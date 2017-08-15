package com.raffler.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.raffler.app.country.Country;
import com.raffler.app.country.CountryPicker;
import com.raffler.app.country.CountryPickerListener;
import com.raycoarana.codeinputview.CodeInputView;
import com.raycoarana.codeinputview.OnCodeCompleteListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class RegisterPhoneActivity extends AppCompatActivity {

    private static final String TAG = "RegisterPhoneActivity";

    private long nationalMobileNumber;
    private String formatMobileNumber;
    private boolean isValid = false;

    private Timer timer;
    private int timeRemaining = 60;
    private boolean isWaiting = false;

    private KProgressHUD hud;
    private TextView txtCountryCode, txtTimer;
    private EditText etPhoneNumber;
    private ImageView imgCheck;
    private AppCompatButton btnNext;
    private RelativeLayout layout_middle;
    private CodeInputView codeInputView;

    private CountryPicker countryPicker;
    private Country selectedCountry;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String phoneVerficationID;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_phone);

        hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setWindowColor(ContextCompat.getColor(this,R.color.colorTransparency))
                .setDimAmount(0.5f);

        imgCheck = (ImageView) findViewById(R.id.imgCheck);
        txtCountryCode = (TextView) findViewById(R.id.txtCountryCode);
        txtCountryCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countryPicker.show(getSupportFragmentManager(), "COUNTRY_PICKER");
            }
        });

        txtTimer = (TextView) findViewById(R.id.txtTimer);

        countryPicker = CountryPicker.newInstance("Select Country");
        List<Country> countryList = new ArrayList<>(Country.getAllCountries());
        countryPicker.setCountriesList(countryList);
        countryPicker.setListener(new CountryPickerListener() {
            @Override
            public void onSelectCountry(String name, String code, String dialCode, int flagDrawableResID) {
                String countryCode = " " + code + " " + dialCode + " ▾ ";
                txtCountryCode.setText(countryCode);
                selectedCountry = new Country(code, name, dialCode, flagDrawableResID);

                countryPicker.dismiss();
            }
        });
        Country country = Country.getCountryFromSIM(this);
        String countryCode = " " + country.getCode() + " " + country.getDialCode() + " ▾ ";
        txtCountryCode.setText(countryCode);
        selectedCountry = country;

        etPhoneNumber = (EditText) findViewById(R.id.etPhoneNumber);
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0){
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    long inputMobileNumber = 0;
                    try {
                        String result = s.toString();
                        result = result.replaceAll("[\\-\\+\\s\\(\\)]","");
                        inputMobileNumber = Long.valueOf(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String dialCode = selectedCountry.getDialCode().replace("+", "");
                    int countryDial = Integer.valueOf(dialCode);
                    Phonenumber.PhoneNumber mobileNumber = new Phonenumber.PhoneNumber().setCountryCode(countryDial).setNationalNumber(inputMobileNumber);
                    String strPhoneNumber = phoneUtil.format(mobileNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);

                    isValid = phoneUtil.isValidNumber(mobileNumber);

                    imgCheck.setVisibility(View.VISIBLE);
                    if (isValid == true){
                        imgCheck.setImageResource(R.drawable.ic_true);
                        nationalMobileNumber = inputMobileNumber;
                        etPhoneNumber.removeTextChangedListener(this);
                        etPhoneNumber.setText(strPhoneNumber);
                        etPhoneNumber.addTextChangedListener(this);
                        etPhoneNumber.setSelection(strPhoneNumber.length());
                        formatMobileNumber = selectedCountry.getDialCode()+String.valueOf(nationalMobileNumber);
                        btnNext.setEnabled(true);
                    }else{
                        imgCheck.setImageResource(R.drawable.ic_false);
                        nationalMobileNumber = inputMobileNumber;
                        btnNext.setEnabled(false);
                    }
                }
            }
        });
        etPhoneNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    verifyPhoneNumber(formatMobileNumber);
                }
                return false;
            }
        });

        btnNext = (AppCompatButton) findViewById(R.id.btnNext);
        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{0xffff4081});
        btnNext.setSupportBackgroundTintList(csl);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPhoneNumber(formatMobileNumber);
            }
        });

        layout_middle = (RelativeLayout) findViewById(R.id.layout_middle);

        codeInputView = (CodeInputView) findViewById(R.id.codeInputView);
        codeInputView.addOnCompleteListener(new OnCodeCompleteListener() {
            @Override
            public void onCompleted(String code) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(phoneVerficationID, code);
                signInWithPhoneAuthCredential(credential);
            }
        });
        codeInputView.setVisibility(View.INVISIBLE);

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
    }

    private void verifyPhoneNumber(String phoneNumber){

        if (!isValid){
            Toast.makeText(this, "Invalid Phone number!", Toast.LENGTH_SHORT).show();
            return;
        }

        hud.show();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);

                hud.dismiss();
                //signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Toast.makeText(RegisterPhoneActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(RegisterPhoneActivity.this, "Invalid mobile number", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Toast.makeText(RegisterPhoneActivity.this, "The SMS quota for the project has been exceeded", Toast.LENGTH_SHORT).show();
                }

                // Show a message and update the UI

                hud.dismiss();
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                phoneVerficationID = verificationId;
                resendToken = token;
                Toast.makeText(RegisterPhoneActivity.this, getString(R.string.register_verify_sent), Toast.LENGTH_SHORT).show();
                hud.dismiss();

                startResendTimer();
            }
        };

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks);

    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        hud.show();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        hud.dismiss();
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // ...
                            startActivity(new Intent(RegisterPhoneActivity.this, RegisterUserActivity.class));
                            RegisterPhoneActivity.this.finish();
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(RegisterPhoneActivity.this, "The verification code entered was invalid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void startResendTimer(){
        if (timer != null){
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new WaitingTask(), 0, 1000);

        btnNext.setText(getString(R.string.button_resend));
        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{0xffd3d3d3}); // Gray
        btnNext.setSupportBackgroundTintList(csl);
        btnNext.setEnabled(false);

        layout_middle.setVisibility(View.GONE);
        codeInputView.setVisibility(View.VISIBLE);
        txtTimer.setVisibility(View.VISIBLE);
        isWaiting = true;
    }

    private void stopResendTimer(){
        timer.cancel();

        btnNext.setText(getString(R.string.button_next));
        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{0xffff4081}); // Ancient
        btnNext.setSupportBackgroundTintList(csl);
        btnNext.setEnabled(true);

        layout_middle.setVisibility(View.VISIBLE);
        txtTimer.setVisibility(View.GONE);
        codeInputView.setVisibility(View.GONE);
        timeRemaining = 60;

        isWaiting = false;
    }

    class WaitingTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timeRemaining = timeRemaining - 1;
                    if (timeRemaining == 0){
                        stopResendTimer();
                    }else{
                        String strTimeRemaining = String.format("Resend in %02d:%02d",timeRemaining/60,timeRemaining%60);
                        txtTimer.setText(strTimeRemaining);
                    }
                }
            });
        }
    }
}

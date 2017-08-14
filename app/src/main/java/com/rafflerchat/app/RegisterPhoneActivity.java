package com.rafflerchat.app;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.rafflerchat.app.country.Country;
import com.rafflerchat.app.country.CountryPicker;
import com.rafflerchat.app.country.CountryPickerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RegisterPhoneActivity extends AppCompatActivity {

    private static final String TAG = "RegisterPhoneActivity";

    private long nationalMobileNumber;
    private String formatMobileNumber;
    private boolean isValid = false;

    private KProgressHUD hud;
    private TextView txtCountryCode;
    private EditText etPhoneNumber;
    private ImageView imgCheck;
    private Button btnNext;

    private CountryPicker countryPicker;
    private Country selectedCountry;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

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

        countryPicker = CountryPicker.newInstance("Select Country");
        List<Country> countryList = new ArrayList<>(Country.getAllCountries());
        /*Collections.sort(countryList, new Comparator<Country>() {
            @Override
            public int compare(Country o1, Country o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });*/
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

                    int countryDial = Integer.valueOf(selectedCountry.getDialCode());
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

        btnNext = (Button) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPhoneNumber(formatMobileNumber);
            }
        });
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
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);

                //signInWithPhoneAuthCredential(credential);
                hud.dismiss();
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                }

                // Show a message and update the UI
                // ...

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
//                mVerificationId = verificationId;
//                mResendToken = token;

                //resendVerificationCode(formatMobileNumber, token);
                hud.dismiss();

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
}

package com.raffler.app;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.alertView.OnItemClickListener;
import com.raffler.app.classes.AppConsts;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.User;
import com.raffler.app.utils.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RegisterUserActivity extends AppCompatActivity {

    private static final String TAG = "RegisterUserActivity";

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String userId;

    private EditText etName;
    private ImageView imgProfile;

    private KProgressHUD hud;

    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.

    String[] permissions= new String[]{
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        hud = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setWindowColor(ContextCompat.getColor(this,R.color.colorTransparency))
                .setDimAmount(0.5f);

        etName = (EditText) findViewById(R.id.etName);
        etName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    String name = etName.getText().toString();
                    userRef.child(userId).child("name").setValue(name);
                }
                return false;
            }
        });
        etName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                    String name = etName.getText().toString();
                    userRef.child(userId).child("name").setValue(name);
                }
            }
        });

        imgProfile = (ImageView) findViewById(R.id.imgProfile);
        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickProfile();
            }
        });

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = firebaseUser.getUid();
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Users");
        userRef.child(userId).child("uid").setValue(userId);

        // get owner name as dummy data
        loadUserInfoFromPhone();
    }

    private void loadUserInfoFromPhone(){
        User user = AppManager.getSession(this);
        if (user != null) {
            etName.setText(user.getName());
            Util.setProfileImage(user.getPhoto(), imgProfile);
        } else {
            if (checkPermissions()) {
                Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
                c.moveToFirst();
                etName.setText(c.getString(c.getColumnIndex("display_name")));
                c.close();
            }
        }
    }

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permissions granted.
                    loadUserInfoFromPhone();
                } else {
                    String permissionList = "";
                    for (String per : permissions) {
                        permissionList += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Toast.makeText(this, permissionList + "not granted.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY){
                mCurrentPhotoPath = data.getData();
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mCurrentPhotoPath, "image/*");
                startActivityForResult(getCropIntent(intent), RESULT_CROP);
            }
            else if (requestCode == REQUEST_CAMERA) {
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mCurrentPhotoPath, "image/*");
                startActivityForResult(getCropIntent(intent), RESULT_CROP);
            }
            else if (requestCode == RESULT_CROP){
                Bitmap bitmap = getBitmapFromData(data);
                Bitmap resizedBitmap =  Bitmap.createScaledBitmap(bitmap, AppConsts.profile_size, AppConsts.profile_size, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos);
                final byte[] imgData = baos.toByteArray();
                try{
                    File profile = new File(this.getCacheDir(), "profile.png");
                    profile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(profile);
                    fos.write(imgData);
                    fos.flush();
                    fos.close();

                    // upload user profile to Storage Bucket
                    uploadProfilePhoto(profile);

                }catch (IOException e){
                    e.printStackTrace();
                }

            }
        }
    }

    private void onClickProfile(){
        new AlertView.Builder().setContext(this)
                .setStyle(AlertView.Style.ActionSheet)
                .setTitle("Take your photo from")
                .setMessage(null)
                .setCancelText("Cancel")
                .setDestructive("Camera", "Gallery")
                .setOthers(null)
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(Object o, int position) {
                        if (position == 0){
                            // camera
                            RegisterUserActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        cameraIntent();
                                    }catch (IOException e){
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }else if (position == 1){
                            // gallery
                            RegisterUserActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    galleryIntent();
                                }
                            });
                        }
                    }
                })
                .build()
                .show();
    }

    private static final int REQUEST_CAMERA = 982;
    private static final int REQUEST_GALLERY = 983;
    private static final int RESULT_CROP = 985;
    public static Bitmap getBitmapFromData(Intent data) {
        Bitmap photo = null;
        Uri photoUri = data.getData();
        if (photoUri != null) {
            photo = BitmapFactory.decodeFile(photoUri.getPath());
        }
        if (photo == null) {
            Bundle extra = data.getExtras();
            if (extra != null) {
                photo = (Bitmap) extra.get("data");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }
        }

        return photo;
    }
    private Intent getCropIntent(Intent intent) {
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", true);
        return intent;
    }

    private Uri mCurrentPhotoPath;
    private void cameraIntent() throws IOException
    {
        if (checkPermissions()){

            final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/picFolder/";
            File newdir = new File(dir);
            newdir.mkdirs();
            String file = dir+"profile.jpg";
            File newfile = new File(file);
            try {
                newfile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            mCurrentPhotoPath = Uri.fromFile(newfile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);
            startActivityForResult(takePictureIntent, REQUEST_CAMERA);
        }
    }

    private void galleryIntent()
    {

        if (checkPermissions()){
            if (Build.VERSION.SDK_INT <= 19) {
                /*Intent intent = new Intent();
                intent.setType("image");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_GALLERY);*/
                Intent intent = new Intent();
                intent.setType("image/jpeg");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_GALLERY);
            } else if (Build.VERSION.SDK_INT > 19) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_GALLERY);
            }
        }
    }

    private void uploadProfilePhoto(File file){

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference profileRef = storageRef.child("profile");
        final String fileName = userId+".jpg";
        StorageReference imageRef = profileRef.child(fileName);

        try{
            InputStream stream = new FileInputStream(file);
            UploadTask uploadTask = imageRef.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }

            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    final Uri profileURL = taskSnapshot.getDownloadUrl();
                    userRef.child(userId).child("photo").setValue(profileURL.toString());

                    Util.setProfileImage(profileURL.toString(), imgProfile);
                }
            });
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }

    }
}

package com.raffler.app.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.raffler.app.utils.References;

/**
 * Created by Shiburagi on 11/09/2016.
 */
public class BaseFragment extends Fragment {
    //    protected FirebaseDatabase database;
    protected FirebaseAuth auth;
    protected References ref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        database = FirebaseDatabase.getInstance();
        ref = References.getInstance();
        auth = FirebaseAuth.getInstance();

    }

    public void search(String text) {

    }
}

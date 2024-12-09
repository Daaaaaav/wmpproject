package com.example.wmpprojectfireaicamera;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class FirebaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}

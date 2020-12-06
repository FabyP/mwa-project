package com.example.mwaproject;

import android.app.Application;
import android.content.Context;

public class MwaApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MwaApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MwaApplication.context;
    }
}
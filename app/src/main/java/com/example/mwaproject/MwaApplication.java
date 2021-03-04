package com.example.mwaproject;

import android.app.Application;
import android.content.Context;

/**
 * Application to detect objects and their distance in a taken image
 */
public class MwaApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MwaApplication.context = getApplicationContext();
    }

}
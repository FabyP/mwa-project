package com.example.mwaproject;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Activity to change the model for object detection in the app
 */
public class SettingsActivity extends AppCompatActivity {
    public static final String KEY_PREF_EVALUATION_MODEL= "model_preference_1";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
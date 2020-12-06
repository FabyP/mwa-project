package com.example.mwaproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class EvaluationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.verifyStoragePermissions(this);
        setContentView(R.layout.activity_evaluation);
        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imagePath");
        String imageName = intent.getStringExtra("imageName");

        ImageView imageView = findViewById(R.id.imageView);

        Bitmap myBitmap = BitmapFactory.decodeFile(imagePath + "/" + imageName);
        imageView.setImageBitmap(myBitmap);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        imageView.setRotation(MainActivity.ORIENTATIONS.get(rotation));
        imageView.setVisibility(View.VISIBLE);
    }
}
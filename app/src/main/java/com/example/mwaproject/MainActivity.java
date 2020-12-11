package com.example.mwaproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//LinearLayOut Setup
        LinearLayout linearLayout= new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));        super.onCreate(savedInstanceState);
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();
        ObjectDetector objectDetector = ObjectDetection.getClient(options);
        InputImage image;
        String name = "cat_dog_2";
        try {
            image = InputImage.fromFilePath(this, Uri.parse("android.resource://com.example.mwaproject/drawable/"+ name));
            objectDetector.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<List<DetectedObject>>() {
                                @Override
                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                    List<Rect> rects = new ArrayList<>();
                                    for (final DetectedObject detectedObject : detectedObjects) {
                                        Rect boundingBox = detectedObject.getBoundingBox();
                                        rects.add(boundingBox);
                                        Integer trackingId = detectedObject.getTrackingId();
                                        Log.i(TAG, "Detected " + trackingId);
                                        for (DetectedObject.Label label : detectedObject.getLabels()) {
                                            String text = label.getText();
                                            Log.i(TAG, "Detected " + text);
                                            float confidence = label.getConfidence();
                                            Log.i(TAG, "Confidence " + confidence);
                                        }
                                    }
                                    DrawView drawView = new DrawView(getApplicationContext(), rects);

                                    drawView.setImageResource(getResources().getIdentifier(name, "drawable", getPackageName()));
                                    drawView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));

                                    linearLayout.addView(drawView);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...Log.i(TAG, "Detected " + text);
                                    Log.i(TAG, "Detected " + e);
                                }
                            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        setContentView(linearLayout);
    }
}
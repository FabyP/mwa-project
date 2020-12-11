package com.example.mwaproject;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MobileNetActivity extends AppCompatActivity {
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//LinearLayOut Setup
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout= new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
                        // or .setAbsoluteFilePath(absoluteFilePathToTfliteModel)
                        .build();
        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(3)
                        .build();
        ObjectDetector objectDetector =
                ObjectDetection.getClient(customObjectDetectorOptions);
        InputImage image;
        String name = "apple";

        try {
            image = InputImage.fromFilePath(this, Uri.parse("android.resource://com.example.mwaproject/drawable/"+name));
            objectDetector
                    .process(image)
                    .addOnFailureListener(e -> {})
                    .addOnSuccessListener(results -> {

                        List<Rect> rects = new ArrayList<>();
                        for (final DetectedObject detectedObject : results) {
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
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        setContentView(linearLayout);
    }
}
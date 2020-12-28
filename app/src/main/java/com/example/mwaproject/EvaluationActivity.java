package com.example.mwaproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TableLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class EvaluationActivity extends AppCompatActivity {
    private static final String TAG = "MwaApplication";
    private ObjectDetector objectDetector;

    public enum ModelType {
        DEFAULT,
        OBJECT_LABELER_V1_1
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        MainActivity.verifyStoragePermissions(this);
        setContentView(R.layout.activity_evaluation);
        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imagePath");
        String imageName = intent.getStringExtra("imageName");

        boolean isDepth16FormatSupported = intent.getBooleanExtra("isDepth16FormatSupported",false);
        String imageDepth16Name;
        // New image with depth information in PNG format
        if(isDepth16FormatSupported) {
            imageDepth16Name = intent.getStringExtra("imageNameDepth16");
        }
        ModelType modelType = (ModelType) intent.getSerializableExtra("modelType");

        Bitmap myBitmap = BitmapFactory.decodeFile(imagePath + "/" + imageName);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();


        switch(modelType) {
            case DEFAULT:
                System.out.println("DEFAULT");
                ObjectDetectorOptions objectDetectorOptions = new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();
                 objectDetector = ObjectDetection.getClient(objectDetectorOptions);
//Settings
                break;
            case OBJECT_LABELER_V1_1:
                System.out.println("OBJECT_LABELER_V1_1");
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
                 objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);

                break;
        }

        InputImage image = InputImage.fromBitmap(myBitmap, rotation);
        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                ImageView imageView = findViewById(R.id.imageView);

                                EvaluationImageView evaluationView = new EvaluationImageView(imageView, myBitmap, rotation);
                                evaluationView.setDetectedObjetcs(detectedObjects);
                                evaluationView.drawEvalRectsOnImageView();

                                TableLayout tableLayout = findViewById(R.id.tableLayout);
                                EvaluationTableView evaluationTableView = new EvaluationTableView(tableLayout);
                                evaluationTableView.drawDetectedObjectInformations(getApplicationContext(), detectedObjects);
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

    }
}
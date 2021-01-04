package com.example.mwaproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import org.tensorflow.lite.task.vision.detector.Detection;


import java.util.ArrayList;
import java.util.List;

public class EvaluationActivity extends AppCompatActivity {
    private static final String TAG = "MwaApplication";
    private ObjectDetector objectDetector;

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private enum DetectorMode {
        TF_OD_API;
    }

    /*public enum ModelType {
        DEFAULT,
        OBJECT_LABELER_V1_1
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        MainActivity.verifyStoragePermissions(this);
        setContentView(R.layout.activity_evaluation);
        Intent intent = getIntent();
        //ModelType modelType = (ModelType) intent.getSerializableExtra("modelType");

        byte[] byteArray = getIntent().getByteArrayExtra("imageByte");

        Bitmap myBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        //TODO: Ausgabe löschen, wenn keine Fehler mehr auftreten.
        /*
        if(byteArray != null) {
            Log.e("EVA: Daten: ", String.valueOf(byteArray.length));
            Log.e("EVA: Bitmap ist da: ", String.valueOf(myBitmap != null));
            if(myBitmap != null) {
                Log.e("EVA: Daten: ", myBitmap.getHeight() + " " + myBitmap.getWidth());
            }
        } else {
            Log.e("EVA: Bild ist da: ", String.valueOf(byteArray != null));
        }
        */
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        SharedPreferences sharedPref =
                PreferenceManager
                        .getDefaultSharedPreferences(this);
        String modelType = sharedPref.getString
                (SettingsActivity.KEY_PREF_EVALUATION_MODEL,"DEFAULT");
        Toast.makeText(this, modelType.toString(),
                Toast.LENGTH_SHORT).show();
        Log.e("EVA", "Modeltype: " + modelType);
        switch(modelType) {
            default:
                ObjectDetectorOptions objectDetectorOptions = new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();
                 objectDetector = ObjectDetection.getClient(objectDetectorOptions);
                break;
            case "OBJECT_LABELER_V1_1":
                Log.e("EVA: Detector", "OBJECT_LABELER_V1_1");
                objectDetector=  getCustomObjectDetector("lite-model_object_detection_mobile_object_labeler_v1_1.tflite");
                break;
            case "MOBILENET_V2_1":
                Log.e("EVA: Detector", "MOBILENET_V2_1");
                objectDetector=  getCustomObjectDetector("mobilenet_v2_1.0_224_1_metadata_1.tflite");
                break;
        }

        InputImage image = InputImage.fromBitmap(myBitmap, rotation);
        Log.e("EVA", "ObjectDetector da?:" + String.valueOf(objectDetector != null));

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

    private ObjectDetector getCustomObjectDetector(String modelName) {
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath(modelName)
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
        return ObjectDetection.getClient(customObjectDetectorOptions);
    }
}
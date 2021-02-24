package com.example.mwaproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
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
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class EvaluationActivity extends AppCompatActivity {
    private static final String TAG = "MwaApplication";

    // Camera
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    public boolean isDepth16FormatSupported = true;

    public static int orientation = 90;

    // Image
    private ImageReader reader;
    ImageReader.OnImageAvailableListener readerListener;
    private boolean isReadyForNextPicture = true;


    private Size imageDimension;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    byte[] imageByteJPG;
    Image imageDepth16;

    // Handler
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    // Permission requests
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private ArrayList<DirectionInfoRect> directionInfoGrid;
    private List<DetectedObject> detectedObjectList;

    /*public enum ModelType {
        DEFAULT,
        OBJECT_LABELER_V1_1
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions(this);
        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);
        setContentView(R.layout.activity_evaluation);
        textureView = findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        if(imageByteJPG != null) {
            textureView.setVisibility(View.GONE);
        } else {
            textureView.setVisibility(View.VISIBLE);
        }

        // Recognize double tap on screen, take a picture and change activity
        findViewById(R.id.cameraLayoutId).setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector gestureDetector = new GestureDetector(EvaluationActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if(isReadyForNextPicture) {
                        Log.e("MWA", "onDoubleTap");
                        takePicture();
                    } else {
                        Log.e("MWA", "Reset camera - try it again");
                        closeCamera();
                        stopBackgroundThread();
                        onResume();
                    }
                    return super.onDoubleTap(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Recognize touches
                gestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return true;
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

    private void evaluateImage() {
        // Log.e("EvaluateImage", "Evaluation Start");
        if(imageByteJPG != null) {
            textureView.setVisibility(View.GONE);
        } else {
            textureView.setVisibility(View.VISIBLE);
        }

        //ModelType modelType = (ModelType) intent.getSerializableExtra("modelType");

        byte[] byteArray = imageByteJPG;

        Bitmap myBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        SharedPreferences sharedPref =
                PreferenceManager
                        .getDefaultSharedPreferences(this);
        String modelType = sharedPref.getString
                (SettingsActivity.KEY_PREF_EVALUATION_MODEL,"DEFAULT");
        Toast.makeText(this, modelType.toString(),
                Toast.LENGTH_SHORT).show();

        ObjectDetector objectDetector;
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
                objectDetector =  getCustomObjectDetector("lite-model_object_detection_mobile_object_labeler_v1_1.tflite");
                break;
            case "MOBILENET_V2_1":
                Log.e("EVA: Detector", "MOBILENET_V2_1");
                objectDetector =  getCustomObjectDetector("mobilenet_v2_1.0_224_1_metadata_1.tflite");
                break;
        }

        InputImage image = InputImage.fromBitmap(myBitmap, rotation);
        int w = myBitmap.getWidth();
        int h = myBitmap.getHeight();
        Log.e("EVA: w", Integer.toString(w));
        Log.e("EVA: h", Integer.toString(h));
        Log.e("EVA: rotation", Integer.toString(rotation));
        String[] verticalDirections = {"Rechts", "Mitte", "Links", };
        String[] horizontalDirections = {"Oben", "Mitte", "Unten", };
        int verticalCounter = 0;
        int horizontalCounter = 0;
        directionInfoGrid = new ArrayList<>();

        for(int x = 0; x < w; x = x + (int) Math.ceil((double)w / 3)){
            for(int y = 0; y < h; y = y + (h/3)){
                Rect rect = new Rect(x, y, x +  (w/3), y +  (h/3));
                DirectionInfoRect directionInfoRect = new DirectionInfoRect(horizontalDirections[horizontalCounter % 3], verticalDirections[verticalCounter % 3], rect);
                directionInfoGrid.add(directionInfoRect);
                verticalCounter++;
            }
            horizontalCounter++;
        }

        //TODO: Ist berechnung korrekt ? (Es kann auch die Confidence für die Entfernung ergänzt werden)
        if(isDepth16FormatSupported && imageDepth16 != null) {
            for(DirectionInfoRect infoRect: directionInfoGrid) {
                double distance = getMillimetersDepth(imageDepth16, infoRect.rect.centerX(), infoRect.rect.centerY());
                infoRect.distance = distance;
            }
        }

        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                detectedObjectList = detectedObjects;

                                ImageView imageView = findViewById(R.id.imageView);
                                EvaluationImageView evaluationView = new EvaluationImageView(imageView, myBitmap, rotation);
                                //evaluationView.setDirectionInfoObjects(directionInfoGrid); useful for debugging the grid
                                evaluationView.setDetectedObjects(detectedObjectList);
                                evaluationView.drawEvalRectsOnImageView();

                                TableLayout tableLayout = findViewById(R.id.tableLayout);
                                tableLayout.removeAllViews();
                                EvaluationTableView evaluationTableView = new EvaluationTableView(tableLayout);
                                evaluationTableView.drawDetectedObjectInformations(getApplicationContext(), detectedObjects, directionInfoGrid);
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
        // Log.e("MWA", "evaluation is finished");
        if (imageDepth16 != null) {
            imageDepth16.close();
            imageDepth16 = null;
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    // A callback object for receiving updates about the state of a camera device.
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            mCameraOpenCloseLock.release();
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            if(cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            } else {
                Log.e("Error", "cameraDevice is null: " + error);
            }
        }
    };

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Take a picture in jpeg format
    private synchronized void takePicture() {
        isReadyForNextPicture = false;
        imageDepth16 = null;
        verifyStoragePermissions(this);
        if(null == cameraDevice) {
            return;
        }
        // The android CameraManager class is used to manage all the camera devices in the android device
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            // Default values

            int width = 800;
            int height = 480;
            // set size

            if (jpegSizes != null && 0 < jpegSizes.length) {
                if(jpegSizes[0].getWidth() < width || jpegSizes[0].getHeight() < height) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                } else if(jpegSizes[0].getWidth() < jpegSizes[0].getHeight()) {
                    width = 480;
                    height = 800;
                }
            }


            reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            Log.e("MWA-READER", "w,h: " +reader.getWidth() + " " + reader.getHeight());

            // The camera capture needs a surface to output what has been captured or being previewed
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);

            readerListener = reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    imageByteJPG = bytes;

                    if(imageByteJPG != null && imageByteJPG.length > 0) {
                        buffer.clear();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                textureView = findViewById(R.id.texture);
                                textureView.setVisibility(View.GONE);
                                if(isDepth16FormatSupported) {
                                    takeDepthPicture();
                                } else {
                                    Log.e("MWA", "Depth16 is not supported - no distance calculation possible");
                                    evaluateImage();
                                }

                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            // To capture or stream images from a camera device, the application must first create a camera capture session
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    if(cameraDevice != null) {
                        createCameraPreview();
                    }
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Take image in depth16 format
    private synchronized void takeDepthPicture() {
        verifyStoragePermissions(this);
        if(null == cameraDevice) {
            return;
        }
        // The android CameraManager class is used to manage all the camera devices in the android device
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.DEPTH16);
            }
            // Default values
            int width = 800;
            int height = 480;
            // set size

            if (jpegSizes != null && 0 < jpegSizes.length) {
                if(jpegSizes[0].getWidth() < width || jpegSizes[0].getHeight() < height) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                } else if(jpegSizes[0].getWidth() < jpegSizes[0].getHeight()) {
                    width = 480;
                    height = 800;
                }
            }


            reader = ImageReader.newInstance(width, height, ImageFormat.DEPTH16, 2);

            // The camera capture needs a surface to output what has been captured or being previewed
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);

            readerListener = reader -> {
                // Image image = null;
                try {
                    imageDepth16 = reader.acquireLatestImage();
                    /*ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    image.getPlanes()[0].getBuffer().order(ByteOrder.nativeOrder());
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);*/
                    // imageByteDepth16 = bytes;

                    if(imageDepth16 != null) {
                        // buffer.clear();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                // TODO: Do something with data of imageByteDepth16
                                Log.e("MWA", "READY TO DO STH WITH DEPTH16");
                                // ShortBuffer shortBuffer = imageByteDepth16.getPlanes()

                                // int distance = getMillimetersDepth(imageDepth16, 50,50);
                                // Log.d("Distanz", String.valueOf(distance));
                                evaluateImage();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            // To capture or stream images from a camera device, the application must first create a camera capture session
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    if(cameraDevice != null) {
                        createCameraPreview();
                    }
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        Log.e("MWA", "HARD ERROR 3");
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try {
            Log.e("MWA", "createCameraPreview");
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);
                cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return;
                        }
                        // When the session is ready, start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession;
                        updatePreview();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(EvaluationActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        Log.e("MWA", "openCamera");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            for (OutputFormat format : OutputFormat.values()) {
                try {
                    if (!map.isOutputSupportedFor(format.imageFormat)) {
                        isDepth16FormatSupported = false;
                        Log.e("ATTENTION", "Format " + format + " not supported - no depth information possible");
                    }
                } catch(IllegalArgumentException e) {
                    isDepth16FormatSupported = false;
                }
            }

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(EvaluationActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void updatePreview() {
        Log.e("MWA", "updatePreview");
        if (null == cameraDevice) {
            Log.e("MWA", "updatePreview error, return");
            return;
        }
        try {
            cameraCaptureSessions.abortCaptures();
            cameraCaptureSessions.stopRepeating();
            isReadyForNextPicture = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("MWA", "HARD ERROR 2");
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch(Exception e) {
            Log.e("MWA", "HARD ERROR");
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != reader) {
            reader.close();
            reader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(EvaluationActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        imageDepth16 = null;
        imageByteJPG = null;
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        Log.e("MWA", "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(EvaluationActivity.this,
                        SettingsActivity.class);
                startActivity(intent);
                return true;
            // Code for action_status and other cases...
        }

        return super.onOptionsItemSelected(item);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    enum OutputFormat {
        JPEG(ImageFormat.JPEG),
        DEPTH16(ImageFormat.DEPTH16);
        public final int imageFormat;
        OutputFormat(int imageFormat) {
            this.imageFormat = imageFormat;
        }
    }

//    Distance Calculation
    public int getMillimetersDepth(Image depthImage, int x, int y){
        Log.e("MWA-Daten - x,y:", x + " " + y);
        //Log.e("MWA-Daten - w,h:", depthImage.getWidth() + " " + depthImage.getHeight());
        if(x >= depthImage.getWidth()) {
            x = depthImage.getWidth();
        }
        if(y >= depthImage.getHeight()) {
            y = depthImage.getHeight();
        }
        Image.Plane plane = depthImage.getPlanes()[0];
        int byteIndex = 120 * plane.getPixelStride() + 213 * plane.getRowStride();
        ByteBuffer buffer = plane.getBuffer().order(ByteOrder.nativeOrder());
        short depthSample = buffer.getShort(byteIndex);

        /*
        short depthRange = (short) (depthSample & 0x1FFF);
        short depthConfidence = (short) ((depthSample >> 13) & 0x7);
        float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1)/7f;
         */
        return (depthSample & 0x1FFF);
    }
}
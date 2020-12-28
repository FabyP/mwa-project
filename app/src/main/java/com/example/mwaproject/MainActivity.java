package com.example.mwaproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Camera
    private TextureView textureView;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;

    // Orientations for camera
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // Image
    private ImageReader imageReader;
    private Size imageDimension;

    public final String imageNameJpg = "imageJPG.jpg";
    public final String imageNameDepth16 = "imageDEPTH16.PNG";

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private String imagePath;

    private boolean pictureAlreadyTaken = false;

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

    public final int jpegFormat = ImageFormat.JPEG;
    public final int depth16Format = ImageFormat.DEPTH16;
    public boolean isDepth16FormatSupported = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // check permissions
        verifyStoragePermissions(this);

        // UIElements
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        // Intent for image evaluation
        Intent intent = new Intent(this, EvaluationActivity.class);
        imagePath = MwaApplication.getAppContext().getExternalFilesDir(null).toString();

        // Recognize double tap on screen, take a picture and change activity
        findViewById(R.id.cameraLayoutId).setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector gestureDetector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    Log.d("MWA", "onDoubleTap");
                    takePicture();
                    pictureAlreadyTaken = true;
                    intent.putExtra("imagePath", imagePath);
                    intent.putExtra("imageName", imageNameJpg);
                    intent.putExtra("isDepth16FormatSupported", isDepth16FormatSupported);
                    if(isDepth16FormatSupported) {
                        intent.putExtra("imageNameDepth16", imageNameDepth16);
                    }
                    intent.putExtra("modelType", EvaluationActivity.ModelType.OBJECT_LABELER_V1_1);
                    startActivity(intent);
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

    // This listener is notified when the surface texture associated with this texture view is available.
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
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            if(cameraDevice != null) {
                cameraDevice.close();
            } else {
                Log.e("Error", "cameraDevice is null");
            }
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ImageReader for depth16 image
    ImageReader.OnImageAvailableListener readerListenerForDepth16 = reader -> {
        try (Image img = reader.acquireLatestImage()) {
            File path = MwaApplication.getAppContext().getExternalFilesDir(null);
            ShortBuffer shortDepthBuffer = img.getPlanes()[0].getBuffer().asShortBuffer();
            shortDepthBuffer.rewind();

            int w = img.getWidth();
            int h = img.getHeight();
            int rowStride = img.getPlanes()[0].getRowStride();
            short[] yRow = new short[w];
            int[] rgbData = new int[w * h];
            int rgbIndex = 0;
            for (int y = 0; y < h; y++) {
                shortDepthBuffer.position(y * rowStride);
                shortDepthBuffer.get(yRow, 0, w);
                for (int x = 0; x < w; x++) {
                    short y16 = yRow[x];
                    rgbData[rgbIndex++] = Color.rgb(y16 & 0x00FF, (y16 >> 8) & 0x00FF, 0);
                }
            }
            Bitmap rgbImage = Bitmap.createBitmap(rgbData, w, h, Bitmap.Config.ARGB_8888);
            try (OutputStream output = new FileOutputStream(path + "/" + imageNameDepth16)) {
                rgbImage.compress(Bitmap.CompressFormat.PNG, 100, output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    // Take a picture of the current scene
    protected void takePicture() {
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
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(jpegFormat);
            }
            // Default values
            int width = 640;
            int height = 480;
            // set size
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader readerJpeg = ImageReader.newInstance(width, height, jpegFormat, 2);

            // if depth16 is supported with camera
            ImageReader readerDepth16;
            if (isDepth16FormatSupported) {
                readerDepth16 = ImageReader.newInstance(width, height, depth16Format, 2);
                readerDepth16.setOnImageAvailableListener(readerListenerForDepth16, mBackgroundHandler);
            }

            // The camera capture needs a surface to output what has been captured or being previewed
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(readerJpeg.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(readerJpeg.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            File path = MwaApplication.getAppContext().getExternalFilesDir(null);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    try (OutputStream output = new FileOutputStream(path + "/" + imageNameJpg)) {
                        output.write(bytes);
                    }
                }
            };
            readerJpeg.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            // To capture or stream images from a camera device, the application must first create a camera capture session
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + path+ "/" + imageNameJpg, Toast.LENGTH_SHORT).show();
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

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            if (!pictureAlreadyTaken) {
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
                        Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Check if depth16 format is with camera possible
            StreamConfigurationMap streamConfigMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            for (OutputFormat format : OutputFormat.values()) {
                if (!streamConfigMap.isOutputSupportedFor(format.imageFormat)) {
                    isDepth16FormatSupported = false;
                    Log.e("ATTENTION","Format " + format + " not supported - no depth information possible");
                }
            }

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "You can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MWA", "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.d("MWA", "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
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

    // Relevant output formats for application
    enum OutputFormat {
        JPEG(ImageFormat.JPEG),
        DEPTH16(ImageFormat.DEPTH16);
        public final int imageFormat;
        OutputFormat(int imageFormat) {
            this.imageFormat = imageFormat;
        }
    }
}


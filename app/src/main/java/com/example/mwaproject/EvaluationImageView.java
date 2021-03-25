package com.example.mwaproject;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.ImageView;
import com.google.mlkit.vision.objects.DetectedObject;
import java.util.ArrayList;
import java.util.List;

/**
 * View to show the taken image
 */
public class EvaluationImageView {

    public List<Rect> detectedObjectRects = new ArrayList<>();
    public ImageView imageView;
    public Bitmap myBitmap;
    public int rotation;

    public EvaluationImageView(ImageView imageView, Bitmap myBitmap, int rotation) {
        this.imageView = imageView;
        this.myBitmap = myBitmap;
        this.rotation = rotation;
    }

    /**
     * Draws the bounding box of detected objects in the image
     */
    public void drawEvalRectsOnImageView() {
        // Create a new image bitmap and attach a brand new canvas to it
        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);

        // Draw the image bitmap into the canvas
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        // Draw everything else you want into the canvas, in this example a rectangle with rounded edges
        Paint myPaint = new Paint();
        myPaint.setColor(Color.rgb(255, 0, 0));
        myPaint.setStrokeWidth(9);
        myPaint.setStyle(Paint.Style.STROKE);

        for (Rect rect : detectedObjectRects) {
            tempCanvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, myPaint);
        }


        // Attach the canvas to the ImageView
        imageView.setImageBitmap(tempBitmap);
        // imageView.setRotation(rotation);
        if(tempBitmap.getWidth() > tempBitmap.getHeight()) {
            imageView.setRotation(EvaluationActivity.orientation);
        }


    }

    public void setDetectedObjects(List<DetectedObjectWithDistance> detectedObjects) {
        for (final DetectedObject detectedObject : detectedObjects) {
            Rect boundingBox = detectedObject.getBoundingBox();
            detectedObjectRects.add(boundingBox);
        }
    }

}

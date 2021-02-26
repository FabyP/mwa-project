package com.example.mwaproject;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;

public class DetectedObjectWithDistance extends DetectedObject {
    private double distance;

    public DetectedObjectWithDistance(@NonNull Rect rect, @Nullable Integer integer, @NonNull List<Label> list, double distance) {
        super(rect, integer, list);
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }
}

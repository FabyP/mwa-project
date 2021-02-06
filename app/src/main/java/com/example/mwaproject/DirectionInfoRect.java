package com.example.mwaproject;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.shapes.Shape;

public class DirectionInfoRect {

   public String horizontal;
   public  String vertikal;
   public Rect rect;

    @Override
    public String toString() {
        return  horizontal + '-' + vertikal;
    }

    public DirectionInfoRect(String horizontal, String vertikal, Rect rect) {
        this.horizontal = horizontal;
        this.vertikal = vertikal;
        this.rect = rect;
    }


}

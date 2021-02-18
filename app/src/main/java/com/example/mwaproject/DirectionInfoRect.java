package com.example.mwaproject;

import android.graphics.Rect;

public class DirectionInfoRect {

   public String horizontal;
   public  String vertical;
   public Rect rect;
   public double distance;

    @Override
    public String toString() {
        return  horizontal + '-' + vertical;
    }

    public DirectionInfoRect(String horizontal, String vertical, Rect rect) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.rect = rect;
        this.distance = 0;
    }

}

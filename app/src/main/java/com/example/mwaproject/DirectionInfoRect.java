package com.example.mwaproject;

import android.graphics.Rect;

/**
 * DirectionInfoGrid contains a rectangle (a part of an image)
 * and describes the position with horizontal and vertical description.
 * It's used in a list with 9 objects of this class, to divide the
 * picture into 9 parts.
 */
public class DirectionInfoRect {

   public String horizontal;
   public  String vertical;
   public Rect rect;

    @Override
    public String toString() {
        return  horizontal + '-' + vertical;
    }

    public DirectionInfoRect(String horizontal, String vertical, Rect rect) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.rect = rect;
    }

}

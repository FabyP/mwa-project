package com.example.mwaproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.List;

class DrawView extends androidx.appcompat.widget.AppCompatImageView {
    public  List<Rect> rects;
    public DrawView(Context context, List<Rect> rects) {
        super(context);
        this.rects = rects;
    }

    DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        Paint myPaint = new Paint();
        myPaint.setColor(Color.rgb(0, 0, 0));
        myPaint.setStrokeWidth(10);
        myPaint.setStyle(Paint.Style.STROKE);
        for(Rect rect: rects){
            canvas.drawRect(rect.left +100 , rect.top + 500, rect.right + 100, rect.bottom +500, myPaint);
        }
    }



}
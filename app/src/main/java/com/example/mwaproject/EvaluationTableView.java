package com.example.mwaproject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class EvaluationTableView {
    private static final String TAG = "MwaApplication";

    public TableLayout tableLayout;
    private int rowIndex;

    public EvaluationTableView(TableLayout tableLayout) {
        this.tableLayout = tableLayout;
    }

    public void drawDetectedObjectInformations(Context applicationContext, List<DetectedObjectWithDistance> detectedObjects, ArrayList<DirectionInfoRect> directionInfoGrid) {
        String[] headLabels = {"Objekt", "Confidence", "Position", "Distanz"};
        TableRow headRow = new TableRow(applicationContext);
        headRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.setStretchAllColumns(true);
        headRow.setId(0);
        for (String headLabel : headLabels) {
            TextView labelTextView = new TextView(applicationContext);

            labelTextView.setText(headLabel);
            labelTextView.setTextColor(Color.BLACK);
            labelTextView.setTextSize(16f);
            labelTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            headRow.addView(labelTextView);
        }
        tableLayout.addView(headRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        for (DetectedObjectWithDistance detectedObject : detectedObjects) {
            rowIndex = 1;
            List<DetectedObject.Label> objectLabels = detectedObject.getLabels();
            String position = getPosition(directionInfoGrid, detectedObject);
            double distance = detectedObject.getDistance();
            distance = distance / 1000;
            if (objectLabels.isEmpty()) {
                addEvaluationRow(applicationContext, "Unbekannt", 0, 0, position);
            } else {
                for (DetectedObject.Label label : detectedObject.getLabels()) {
                    String labelText = label.getText();
                    float confidence = label.getConfidence();
                    Log.i(TAG, "Objekt " + labelText.toString());

                    addEvaluationRow(applicationContext, labelText, confidence, distance, position);
                }
            }
        }
    }

    private void addEvaluationRow(Context applicationContext, String labelText, float confidence, double distance, String position) {
        TableRow newRow = new TableRow(applicationContext);
        newRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        newRow.setId(rowIndex);
        rowIndex++;

        // Objekt
        TextView label = new TextView(applicationContext);
        label.setText(labelText);
        label.setTextColor(Color.BLACK);
        label.setTextSize(16f);
        label.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        newRow.addView(label);

        // Confidence
        String confidenceString = String.format("%.2f", confidence * 100) + "%";
        TextView confidenceTextView = new TextView(applicationContext);
        confidenceTextView.setText(confidenceString);
        confidenceTextView.setTextColor(Color.BLACK);
        confidenceTextView.setTextSize(16f);
        confidenceTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        newRow.addView(confidenceTextView);

        // Position
        TextView positionTextView = new TextView(applicationContext);
        positionTextView.setText(position);
        positionTextView.setTextColor(Color.BLACK);
        positionTextView.setTextSize(16f);
        positionTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        newRow.addView(positionTextView);

        // Distanz
        String distanceString = String.format("%.2f", distance / 1000) + " m";
        TextView distanceTextView = new TextView(applicationContext);
        distanceTextView.setText(distanceString);
        distanceTextView.setTextColor(Color.BLACK);
        distanceTextView.setTextSize(16f);
        distanceTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        newRow.addView(distanceTextView);

        tableLayout.addView(newRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }

    private String getPosition(ArrayList<DirectionInfoRect> directionInfoGrid, DetectedObject detectedObject) {
        String position = "undefined";
        Double max = 0.0;
        for (DirectionInfoRect directionInfoRect : directionInfoGrid) {
            Double areaOverlap = overLappingAreaPercentage(directionInfoRect.rect, detectedObject.getBoundingBox());
            Double overlapSelf = overLappingAreaPercentage(directionInfoRect.rect, directionInfoRect.rect);
            Double overlapPercentage = areaOverlap / overlapSelf;
            if (overlapPercentage > max) {
                max = overlapPercentage;
                position = directionInfoRect.toString();
            }

           /* Log.i(TAG, "position " + directionInfoRect.toString());
            Log.i(TAG, "overlapPercentage " + overlapPercentage);*/
        }
        return position;
    }

    private double overLappingAreaPercentage(Rect rect1, Rect rect2) {
        int x_overlap = Math.max(0, Math.min(rect1.right, rect2.right) - Math.max(rect1.left, rect2.left));
        int y_overlap = Math.max(0, Math.min(rect1.bottom, rect2.bottom) - Math.max(rect1.top, rect2.top));
        return x_overlap * y_overlap;
    }
}

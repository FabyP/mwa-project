package com.example.mwaproject;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.ArrayList;
import java.util.List;

public class EvaluationTableView {

    public TableLayout tableLayout;

    public EvaluationTableView(TableLayout tableLayout) {
        this.tableLayout = tableLayout;
    }

    public void drawDetectedObjectInformations(Context applicationContext, List<DetectedObject> detectedObjects, ArrayList<DirectionInfoRect> directionInfoGrid) {
        String[] headLabels = {"Objekt", "Confidence", "Position", "Distanz"};
        TableRow headRow = new TableRow(applicationContext);
        headRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.setStretchAllColumns(true);
        headRow.setId(0);
        for (String headLabel : headLabels){
            TextView labelTextView = new TextView(applicationContext);

            labelTextView.setText(headLabel);
            labelTextView.setTextColor(Color.BLACK);
            labelTextView.setTextSize(16f);
            labelTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            headRow.addView(labelTextView);
        }
        tableLayout.addView(headRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
          for ( DetectedObject detectedObject : detectedObjects) {

                int rowIndex = 1;

                for (DetectedObject.Label label : detectedObject.getLabels()) {
                    String labelText = label.getText();

                    float confidence = label.getConfidence();
                    float distance = 0; // TODO distance calculation
                    String position = "undefined"; // TODO position
                    for( DirectionInfoRect directionInfoRect : directionInfoGrid){
                        if(directionInfoRect.rect.intersect(detectedObject.getBoundingBox())){
                           position = directionInfoRect.toString();
                        }
                    }

                    TableRow newRow = new TableRow(applicationContext);
                    newRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    newRow.setId(rowIndex);
                    rowIndex++;

                    // Objekt
                    TextView labelTextView = new TextView(applicationContext);
                    labelTextView.setText(labelText);
                    labelTextView.setTextColor(Color.BLACK);
                    labelTextView.setTextSize(16f);
                    labelTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    newRow.addView(labelTextView);

                    // Confidence
                    TextView confidenceTextView = new TextView(applicationContext);
                    confidenceTextView.setText(String.valueOf(confidence));
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
                    TextView distanceTextView = new TextView(applicationContext);
                    distanceTextView.setText(String.valueOf(distance));
                    distanceTextView.setTextColor(Color.BLACK);
                    distanceTextView.setTextSize(16f);
                    distanceTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    newRow.addView(distanceTextView);

                    tableLayout.addView(newRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                }
            }
    }
}

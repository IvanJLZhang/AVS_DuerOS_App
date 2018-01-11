/*
 * =========================================================================
 * Copyright (c) 2014 Qualcomm Technologies, Inc. All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
 * =========================================================================
 * @file: DrawView.java
 */

package com.wistron.demo.tool.teddybear.face_recognition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceView;

import com.qualcomm.snapdragon.sdk.face.FaceData;

import java.util.HashMap;
import java.util.Iterator;

public class DrawView extends SurfaceView {

    private Paint paintForTextBackground = new Paint(); // Draw the black background
    // behind the text
    private Paint paintForText = new Paint(); // Draw the text
    private FaceData[] mFaceArray;
    private boolean _inFrame; // Boolean to see if there is any faces in the frame
    private HashMap<String, String> hash;
    private FacialRecognitionActivity faceRecog;
    private Context mContext;

    public DrawView(Context context, FaceData[] faceArray, boolean inFrame) {
        super(context);
        mContext = context;
        setWillNotDraw(false); // This call is necessary, or else the draw
        // method will not be called.
        mFaceArray = faceArray;
        _inFrame = inFrame;
        faceRecog = new FacialRecognitionActivity();
        hash = faceRecog.retrieveHash(getContext());
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (_inFrame) // If the face detected is in frame.
        {
            int MaxX = canvas.getWidth();
            int MaxY = canvas.getHeight();
            Log.i("t", " " + MaxX + " " + MaxY);

            for (int i = 0; i < mFaceArray.length; i++) {

                String selectedPersonId = Integer.toString(mFaceArray[i]
                        .getPersonId());
                String personName = null;
                Iterator<HashMap.Entry<String, String>> iter = hash.entrySet()
                        .iterator();
                while (iter.hasNext()) {
                    HashMap.Entry<String, String> entry = iter.next();
                    if (entry.getValue().equals(selectedPersonId)) {
                        personName = entry.getKey();
                    }
                }
                Rect rect = mFaceArray[i].rect;
                //aaron 180 rotation
                rect = new Rect(MaxX - rect.right, MaxY - rect.bottom, MaxX - rect.left, MaxY - rect.top);
                //end
                float pixelDensity = getResources().getDisplayMetrics().density;
                int textSize = (int) (rect.width() / 25 * pixelDensity);

                paintForText.setColor(Color.WHITE);
                paintForText.setTextSize(textSize);
                Typeface tp = Typeface.SERIF;
                Rect backgroundRect = new Rect(rect.left - 20, rect.bottom + 20,
                        rect.right + 20, (rect.bottom + textSize) + 20);

                paintForTextBackground.setStyle(Paint.Style.FILL);
                paintForTextBackground.setColor(Color.BLACK);
                paintForText.setTypeface(tp);
                paintForTextBackground.setAlpha(80);
                if (personName != null) {
                    canvas.drawRect(backgroundRect, paintForTextBackground);
                    canvas.drawText(personName, rect.left - 20, rect.bottom + 20
                            + (textSize), paintForText);
                }

                //Aaron draw rectangle
                Rect faceRect = new Rect(rect.left - 20, rect.top - 20,
                        rect.right + 20, rect.bottom + 20);
                Paint paintForRectStroke = new Paint();
                paintForRectStroke.setStyle(Paint.Style.STROKE);
                paintForRectStroke.setColor(Color.GREEN);
                paintForRectStroke.setStrokeWidth(5);
                canvas.drawRect(faceRect, paintForRectStroke);
                //end
            }
        } else {
            canvas.drawColor(0, Mode.CLEAR);
        }
    }

}

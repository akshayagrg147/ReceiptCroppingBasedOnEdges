/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meetsuccess.scannerreceipt;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;


import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.meetsuccess.scannerreceipt.ui.camera.GraphicOverlay;

/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private Context context;
    int height,width;
OcrDetectorProcessor.SendingMessage obj;
    private int []faceLimitPointsArray;




    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay,int []faceLimitPointsArray,Context cnt,OcrDetectorProcessor.SendingMessage obj1) {
        obj=obj1;
       this.faceLimitPointsArray=faceLimitPointsArray;
        obj.sendMessage(true, "BlankImage");
        mGraphicOverlay = ocrGraphicOverlay;
        context=cnt;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
         height = displayMetrics.heightPixels;
         width = displayMetrics.widthPixels;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        obj.sendMessage(true, "BlankImage");
        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();


        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);

            final int finalI = i;
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay,faceLimitPointsArray,height,width, item, new OcrGraphic.getResultText() {
                @Override
                public void checkReceiptNotInBox(Boolean NotReceiptInBox,String String) {

                    obj.sendMessage(NotReceiptInBox,String);
//                    if(NotReceiptInBox) {
//
//
//                       // Toast.makeText(context, "please put in box"+ ss, Toast.LENGTH_SHORT).show();
//                    }
//                    else
//                    {
//                        Log.d("callleee","toast called"+NotReceiptInBox+ finalI);
//
//                        obj.sendMessage(false);
//                    }
                }

                @Override
                public void CheckReceiptHavingLongDistance(Boolean longDistance) {
                    obj.CheckingLongDistance(longDistance);

                }
            });
            mGraphicOverlay.add(graphic);
        }

    }

    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }

    interface SendingMessage{
        public void sendMessage(Boolean str,String CheckBlankImage);
        public void CheckingLongDistance(Boolean str);
    }
}

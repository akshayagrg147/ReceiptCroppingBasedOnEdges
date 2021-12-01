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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;


import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.meetsuccess.scannerreceipt.ui.camera.GraphicOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class OcrGraphic extends GraphicOverlay.Graphic {

    private int mId;


    private static final int TEXT_COLOR = Color.WHITE;

    private static Paint sRectPaint;
    private static Paint sTextPaint;
    private final TextBlock mText;
    int h,w;
    private OcrGraphic.getResultText obj;
    int []gettingPoints;

    OcrGraphic(GraphicOverlay overlay,int []GettingPoints,int height,int width, TextBlock text,OcrGraphic.getResultText bb) {

        super(overlay);
        obj=bb;
        this.gettingPoints=GettingPoints;
        h=height;
        w=width;


        mText = text;

        if (sRectPaint == null) {
            sRectPaint = new Paint();
            sRectPaint.setColor(TEXT_COLOR);
            sRectPaint.setStyle(Paint.Style.STROKE);
            sRectPaint.setStrokeWidth(4.0f);
        }

        if (sTextPaint == null) {
            sTextPaint = new Paint();
            sTextPaint.setColor(TEXT_COLOR);
            sTextPaint.setTextSize(54.0f);
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public TextBlock getTextBlock() {
        return mText;
    }

    /**
     * Checks whether a point is within the bounding box of this graphic.
     * The provided point should be relative to this graphic's containing overlay.
     * @param x An x parameter in the relative context of the canvas.
     * @param y A y parameter in the relative context of the canvas.
     * @return True if the provided point is contained within this graphic's bounding box.
     */
    public boolean contains(float x, float y) {
        TextBlock text = mText;
        if (text == null) {
            return false;
        }
        RectF rect = new RectF(text.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);
        return (rect.left < x && rect.right > x && rect.top < y && rect.bottom > y);
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        TextBlock text = mText;
        if (text == null) {
            return;
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(text.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);
        canvas.drawRect(rect, sRectPaint);

        // Break the text into multiple lines and draw each one according to its own bounding box.
        List<? extends Text> textComponents = text.getComponents();
        List<Integer> leftside = new ArrayList<>();
        List<Integer> Rightside = new ArrayList<>();
        List<Integer> Topside = new ArrayList<>();
        List<Integer> Bottomside = new ArrayList<>();


        for (Text currentText : textComponents) {


            leftside.add(currentText.getBoundingBox().left);
            Rightside.add(currentText.getBoundingBox().right);
            Topside.add(currentText.getBoundingBox().top);
            Bottomside.add(currentText.getBoundingBox().bottom);

            float left = translateX(currentText.getBoundingBox().left);
            float bottom = translateY(currentText.getBoundingBox().bottom);
            canvas.drawText(currentText.getValue(), left, bottom, sTextPaint);

        }
        Collections.sort(leftside);
        Collections.sort(Rightside);
        Collections.sort(Topside);
        Collections.sort(Bottomside);


        //  Log.d("gettingtesxt","left"+leftside.get(0)+"--"+currentText.getBoundingBox().right+"--"+currentText.getBoundingBox().top+"--"+currentText.getBoundingBox().bottom);
        // Log.d("gettingtesxt","please put in box"+currentText.getBoundingBox().top);


//170  744  20.04
        //   100>300


        float translatex = (leftside.get(0) + (Rightside.get(Rightside.size() - 1) - leftside.get(0)));
        float translatey = (Topside.get(0) + (Bottomside.get(Bottomside.size() - 1) - Topside.get(0)));


        float x = translateX(translatex / 2);
        float y = translateY(translatey / 2);

        float xOffset = scaleX((Rightside.get(Rightside.size() - 1) - leftside.get(0)) / 2.0f);
        float yOffset = scaleY(Bottomside.get(Bottomside.size() - 1) - Topside.get(0) / 2.0f);
        float left = x - xOffset + 50;
        float top = y - yOffset + 50;
        float right = x + xOffset - 50;
        float bottom = y + yOffset - 50;
        Log.d("ddddddddddddddddddd", Rightside.get(Rightside.size() - 1) + "--" + leftside.get(0) + "--" + xOffset + "--" + yOffset + "--" + left + "--" + top + "--" + right + "--" + bottom);


//        if (left < gettingPoints[0] && right > gettingPoints[2] && top > gettingPoints[1] && bottom < gettingPoints[3]) {
//            //Place your face inside square box
//
//            obj.checkReceiptNotInBox(false, "NotBlank");
//        } else if (left + 20 > gettingPoints[0] && right - left < gettingPoints[2]) {// && right-100<faceLimitPointsArray[2]
//            //Please come closer to capture the face
//
//            obj.CheckReceiptHavingLongDistance(true);
////            isFaceClickable=true;
////            attendanceFaceObject.setClickable(true);
////            attendanceFaceObject.setMessage("Place your face inside square box");//Tap on face
//        } else {
//            //Place your face inside square box
//
//            obj.checkReceiptNotInBox(false, "NotBlank");
//        }



          if (left < gettingPoints[0] && right > gettingPoints[2] && top > gettingPoints[1] && bottom < gettingPoints[3]) {

              //Place your face inside square box

              obj.checkReceiptNotInBox(false, "NotBlank");
            if (right-left > (gettingPoints[2] - gettingPoints[0])){

                //Please wait we are capturing your face.
                obj.checkReceiptNotInBox(true, "BlankImage");
            }else {

                //Please come closer to capture the face

                obj.CheckReceiptHavingLongDistance(true);
            }
        }
          else if (left > gettingPoints[0] && right < gettingPoints[2] + gettingPoints[0]) {
            if (right-left > (gettingPoints[2] - gettingPoints[0])){

                //Please wait we are capturing your face.
                obj.checkReceiptNotInBox(true, "BlankImage");
            }else {
                //Please come closer to capture the face

                obj.CheckReceiptHavingLongDistance(true);
            }
        }
//            else if (right-left > (faceLimitPointsArray[2] - faceLimitPointsArray[0])){
//                isFaceClickable = true;
//                attendanceFaceObject.setClickable(true);
//                attendanceFaceObject.setMessage("Please wait we are capturing your face.");//Tap on face
//            }
        else {


            //Place your face inside square box

              obj.checkReceiptNotInBox(false, "NotBlank");
        }






//        Log.d("dmdmmdmddm",text.getValue().length()+"--"+text.getValue()+"image is null");
//
//        if(text.getValue().length()>0)
//        {
//
//            if((Rightside.get(Rightside.size()-1)-leftside.get(0)<w/35))
//            {
//                obj.CheckReceiptHavingLongDistance(true);
//
//            }
//            else if(leftside.get(0)+10<(w/6.35)||Rightside.get(Rightside.size()-1)-10>(w/1.45)||Topside.get(0)-10<(h/103.75))
//            {
//
//                obj.checkReceiptNotInBox(true,"NotBlank");
//
//              //  obj.checkReceiptNotInBox(true," left "+leftside.get(0)+"--"+(w/6.35)+" right "+Rightside.get(0)+"---"+(w/1.45)+" top "+Topside.get(0)+"--"+h/103.75);
//
//
//            }
//            else {
//             //   Log.d("dmdmmdmddm",text.getValue().length()+"--"+text.getValue());
//               // Log.d("rirkifmkfm",Rightside.get(Rightside.size()-1)+"--"+leftside.get(0)+"--"+w/35+"--"+(Rightside.get(Rightside.size()-1)-leftside.get(0)));
//
//
//                obj.checkReceiptNotInBox(false, "NotBlank");
//
//            }
//        }
//        else
//        { Log.d("dmdmmdmddm",text.getValue().length()+"--"+text.getValue()+"image is null");
//
//            obj.checkReceiptNotInBox(true, "BlankImage");
//        }



    }
    interface getResultText{
        public void checkReceiptNotInBox(Boolean NotReceiptInBox,String String);
        public void CheckReceiptHavingLongDistance(Boolean distnaceReceipt);
    }
}

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
 package com.meetsuccess.scannerreceipt;;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;

import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.meetsuccess.scannerreceipt.ui.camera.CameraSource;
import com.meetsuccess.scannerreceipt.ui.camera.CameraSourcePreview;
import com.meetsuccess.scannerreceipt.ui.camera.GraphicOverlay;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity for the multi-tracker app.  This app detects text and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and contents of each TextBlock.
 */
public final class OcrCaptureActivity extends AppCompatActivity {
    private static final String TAG = "OcrCaptureActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    Timer myTimer = new Timer();
    Boolean activeTimer = false;



    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    TextView textView;

    String textViewValues ="";
    boolean firstTime = true;
     TextRecognizer textRecognizer;

    MyTimer chatTimerTask;
    Timer time;
    View camera_face_limit_view;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_ocr_capture);
        textView = findViewById(R.id.textViewMessage);
        camera_face_limit_view=findViewById(R.id.camera_face_limit_view);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                canCaptureImage = true;
//            }
//        },15000);

//        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
//        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);


        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = true;
        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int storage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
            requestStoragePermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());


        findViewById(R.id.textViewCameraCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialogShowing();
               // mCameraSource.takePicture(null, mPicture);
            }
        });
    }

    private void requestStoragePermission() {
    }

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        } else {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA) || (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);

            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };


    }

   CameraSource.PictureCallback mPicture = new
          CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data) {

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int height = displayMetrics.heightPixels;
                    int width = displayMetrics.widthPixels;
//                    Log.d("Dddddddddddddddddd", height + "--" + width+"---"+width * 2+"--"+(height + (int)(height / 2.7)));
//                    Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//
//                    myBitmap = Bitmap.createBitmap(myBitmap, (int) (width / 2.5), (int) height / 10, width * 2, (height + (int)(height / 2.7)));

                    Rect rectf = new Rect();

//For coordinates location relative to the parent
                    camera_face_limit_view.getLocalVisibleRect(rectf);

//For coordinates location relative to the screen/display
                    camera_face_limit_view.getGlobalVisibleRect(rectf);



                   // Log.d("gettingTakenImageSize", height + "--" + width+"---"+(int) (width / 2.5)+"--"+(int) height / 10+"--"+ ((width )+"--"+(height -(int)(height / 2.7))));
                    Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                  //  myBitmap = Bitmap.createBitmap(myBitmap, (int) (width / 2.5), (int) height / 10, width , (height -(int)(height / 2.7)));
                    Log.d("printedreactvalue",rectf.left+"--"+ (int) (rectf.top - 50)+"--"+ (int) rectf.width()+"--"+ (int) rectf.height()+"--"+width+"--"+height);

                    myBitmap= Bitmap.createBitmap(myBitmap, rectf.left, (int) rectf.top - 50, (int) width, (int) height);
                   // Bitmap croppedBmp = Bitmap.createBitmap(tempBitmap, xValue, (int) face.getPosition().y - 50, (int) face.getWidth(), (int) face.getHeight());



                    byte[] croppedBitMap = getByteArrayFromBitMap(myBitmap);
                    saveTemplateFromByteArray(croppedBitMap, "croppedfile", OcrCaptureActivity.this);

                }
            };

    public static void saveTemplateFromByteArray(byte[] data, String name, Context context) {

        if (data != null) {
            final File file = Utility.getMediaFile(context, name);
            try {
                FileOutputStream fos = new FileOutputStream(file.getPath());
                Log.d("filepathgetting", file.getAbsolutePath());
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (IOException exception) {

            }
        }
    }


    public byte[] getByteArrayFromBitMap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {


        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated processor instance
        // is set to receive the text recognition results and display graphics for each text block
        // on screen.
        textRecognizer = new TextRecognizer.Builder(context).build();
        final Boolean[] response = new Boolean[1];
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        int[] faceLimitPointsArray = new int[4];
        faceLimitPointsArray[0] = width / 5;
        //faceLimitPointsArray[1]=height/4;
        faceLimitPointsArray[1] = width / 5;
        faceLimitPointsArray[2] = 3 * (width / 5);
        faceLimitPointsArray[3] = height - width / 3/*3*(width/4)*/;
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay,faceLimitPointsArray, this, new OcrDetectorProcessor.SendingMessage() {
            @Override
            public void sendMessage(Boolean str,String blankImage) {

                //Toast.makeText(OcrCaptureActivity.this,str+"--",Toast.LENGTH_SHORT).show();
                if (str) {
                    if (!textView.getText().toString().equalsIgnoreCase("Please put receipt into box"))
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (blankImage.contains("BlankImage")) {
                                    textView.setText("Not able to find Receipt");
                                    textViewValues = "Not able to find Receipt";
                                    Log.e("capture", "-- Not able to find Receipt");


                                } else if (blankImage.contains("NotBlank")) {
                                    textView.setText("Please put receipt into box");
                                    textViewValues = "please put receipt into box";
                                    Log.e("capture", "-- please put inside");


                                }
                            }
                        });


                } else {
                    Log.e("capture","Ready to analysing....");
//                    if(time!=null)
//                    {
//                        time.cancel();
//                        firstTime=true;
//                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("");

                        }
                    });
                    textViewValues ="";

                    if (firstTime)
                    {


                        if (mCameraSource != null) {
                            Log.e("capture","mCameraSource not null....");
                            firstTime = false;
                            time = new Timer();
                            chatTimerTask = new MyTimer();
                            time.scheduleAtFixedRate(chatTimerTask, 3000, 3000);




                        }
                        else {
                            Log.e("capture","mCameraSource null....");
                        }
                    }


//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            textViewValues ="";
//
//                            Handler handler = new Handler(Looper.getMainLooper());
//                            handler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//
//
//                                    if (textViewValues.isEmpty()) {
//
//
//                                        if (mCameraSource != null) {
//                                            mCameraSource.takePicture(null, mPicture);
//
//                                            textRecognizer.release();
//
//
//                                        }
//
//
////Do something
//
//
//
//                                        // do your work
//
//
//                                    }
//
//
//                                }
//                            }, 5000);
//
//                        }
//                    });














//
//                            handler[0] = new Handler();
//
//                            if (activeTimer) {
//                                activeTimer = false;
//
//                                myTimer.cancel();
//                                myTimer = null;
//                                myTimer = new Timer();
//
//
//
//
//                            }

//
//                            final int[] i = {0};
//
//                            myTimer.schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//
//
//                                    runnable[0] = new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            activeTimer = true;
//                                          //  System.out.println(i[0]+"-");
//                                            Log.d("timegetting",i[0]+"------"+System.currentTimeMillis());
//
////                                            Toast.makeText(OcrCaptureActivity.this, "smmma" + i[0], Toast.LENGTH_SHORT).show();
//
//
//
//
//                                            i[0]++;
//
//                                        }
//                                    };
//                                    handler[0].postDelayed(runnable[0], 1000);
//
//
//                                }
//
//
//                            }, 0, 1000);





                }
            }

            @Override
            public void CheckingLongDistance(Boolean str) {
                if (str)

                    if (!textView.getText().toString().equalsIgnoreCase("Please put closer receipt"))
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                textView.setText("Please put closer receipt");
                                textViewValues = "please put closer receipt";
                                Log.e("capture","-- please put inside");
                            }
                        });


            }
        }));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    private void alertDialogShowing() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View view = layoutInflaterAndroid.inflate(R.layout.cancel_dialog, null);


        builder.setView(view);
        builder.setCancelable(true);

        final File file = Utility.getMediaFile(this, "croppedfile");
        if(file!=null &&file.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            ImageView imageView = view.findViewById(R.id.imageview);

            imageView.setImageBitmap(myBitmap);

            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap is called to capture the first TextBlock under the tap location and return it to
     * the Initializing Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {
        OcrGraphic graphic = mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
        TextBlock text = null;
        if (graphic != null) {
            text = graphic.getTextBlock();
            if (text != null && text.getValue() != null) {
                Intent data = new Intent();
                data.putExtra(TextBlockObject, text.getValue());
                setResult(CommonStatusCodes.SUCCESS, data);
                finish();
            } else {
                Log.d(TAG, "text data is null");
            }
        } else {
            Log.d(TAG, "no text detected");
        }
        return text != null;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }


    private void captureTheImage()
    {
        Log.e("capture","-- values: "+textViewValues);
        if (textViewValues.trim().equals(""))
        {
            mCameraSource.takePicture(null, mPicture);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
            if (textRecognizer!=null)
            {
                Log.e("capture","recognizer not null releasing..");
                textRecognizer.release();





//               Timer time1 = new Timer();
//                MyTimer1  chatTimerTask1 = new MyTimer1();
//                time1.scheduleAtFixedRate(chatTimerTask1, 5000, 3000);
//                time1.cancel();


















            }
            else {
                Log.e("capture","recognizer null");
            }
            time.cancel();




//            }
//        },2000);
        }
        else {
            Log.e("capture","can able to capture");
        }

    }


    public class MyTimer1 extends TimerTask {
        @Override
        public void run() {
       Intent ab=new Intent(OcrCaptureActivity.this,MainActivity.class);
               startActivity(ab);
               finish();
//        }
    }


}
    public class MyTimer extends TimerTask {
        @Override
        public void run() {
            captureTheImage();
        }
    }
}

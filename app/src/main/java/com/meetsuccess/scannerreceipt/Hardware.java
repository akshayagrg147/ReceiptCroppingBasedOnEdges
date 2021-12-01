package com.meetsuccess.scannerreceipt;


import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Hardware {
    public static class Camera {
        private String TAG = getClass().getSimpleName();

        public static boolean checkCameraHardware(Context context) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        }

        public static android.hardware.Camera.Size determineBestPreviewSize(android.hardware.Camera.Parameters parameters, int IMAGE_SIZE) {
            List<android.hardware.Camera.Size> sizes = parameters.getSupportedPreviewSizes();

            return determineBestSize(sizes, IMAGE_SIZE);
        }
        public static android.hardware.Camera.Size determineBestPictureSize(android.hardware.Camera.Parameters parameters, int IMAGE_SIZE) {
            List<android.hardware.Camera.Size> sizes = parameters.getSupportedPictureSizes();

            return determineBestSize(sizes, IMAGE_SIZE);
        }

        private static android.hardware.Camera.Size determineBestSize(List<android.hardware.Camera.Size> sizes, int IMAGE_SIZE) {
            android.hardware.Camera.Size bestSize = null;

            for (android.hardware.Camera.Size currentSize : sizes) {
                boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
                boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
                boolean isInBounds = currentSize.width <= IMAGE_SIZE;

                if (isDesiredRatio && isInBounds && isBetterSize) {
                    bestSize = currentSize;
                }
            }

            if (bestSize == null) {
                return sizes.get(0);
            }

            return bestSize;
        }

        public static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
            private String TAG = getClass().getSimpleName();

            private static final double ASPECT_RATIO = 3.0 / 4.0;

            private SurfaceHolder mHolder;
            private android.hardware.Camera mCamera;
            public boolean safeToTakePicture = false;
            public CameraPreview(Context context, android.hardware.Camera camera) {
                super(context);
                mCamera = camera;

                // Install a SurfaceHolder.Callback so we get notified when the
                // underlying surface is created and destroyed.
                mHolder = getHolder();
                mHolder.addCallback(this);
                // deprecated setting, but required on Android versions prior to 3.0
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            public void surfaceCreated(SurfaceHolder holder) {
                // The Surface has been created, now tell the camera where to draw the preview.
                try {
                    if(mCamera!= null) {
                        mCamera.setPreviewDisplay(holder);
                        mCamera.startPreview();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Error setting camera preview: " + e.getMessage());
                }
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.release();
                // empty. Take care of releasing the Camera preview in your activity.
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                // If your preview can change or rotate, take care of those events here.
                // Make sure to stop the preview before resizing or reformatting it.

                if (mHolder.getSurface() == null) {
                    // preview surface does not exist
                    return;
                }

                // stop preview before making changes
                try {
                    mCamera.stopPreview();
                    safeToTakePicture = true;
                } catch (Exception e) {
                    // ignore: tried to stop a non-existent preview
                }

                // set preview size and make any resize, rotate or
                // reformatting changes here

                // start preview with new settings
                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();

                } catch (Exception e) {
                    Log.d(TAG, "Error starting camera preview: " + e.getMessage());
                }
            }

            /**
             * Measure the view and its content to determine the measured width and the
             * measured height.
             */
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int height = MeasureSpec.getSize(heightMeasureSpec);
                int width = MeasureSpec.getSize(widthMeasureSpec);

                if (width > height * ASPECT_RATIO) {
                    width = (int) (height * ASPECT_RATIO + .5);
                } else {
                    height = (int) (width / ASPECT_RATIO + .5);
                }

                setMeasuredDimension(width, height);
            }
        }

        public static File getLeaveMediaFile(Context context) {

            File mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(new Date());
            return new File(mediaStorageDir.getPath() + File.separator + "INFOTECH_LEAVE_" + timeStamp + ".jpg");
        }

        public static File getAttendanceMediaFile(Context context) {
            ContextWrapper cw = new ContextWrapper(context);
            File mediaStorageDir = cw.getDir("imageDir", Context.MODE_PRIVATE);
            //File mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            try {
                if (!mediaStorageDir.exists()) {
                    if (!mediaStorageDir.mkdirs()) {
                        Log.d("MyCameraApp", "failed to create directory");
                        return null;
                    }
                }
                // Create a media file name
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(new Date());
                return new File(mediaStorageDir.getPath() + File.separator + "INFOTECH_ATTENDANCE_" + timeStamp + ".jpg");
            } catch (Exception e){
                return null;
            }
        }
    }
}

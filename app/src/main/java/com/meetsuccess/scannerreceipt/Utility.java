package com.meetsuccess.scannerreceipt;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public abstract class Utility {

    public static File getMediaFile(Context context, String name) {
        ContextWrapper cw = new ContextWrapper(context);
        File mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);;//context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
            return new File(mediaStorageDir.getPath() + File.separator + name + ".jpg");
        } catch (Exception e) {
            return null;
        }
    }
        }


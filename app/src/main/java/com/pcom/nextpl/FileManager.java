package com.pcom.nextpl;

import android.content.Context;
import android.os.Environment;
import android.view.View;

import java.io.File;

public class FileManager {
    static File getRootDirectory(Context context)
    {
        return new File(Environment.getExternalStorageDirectory() + "/" + context.getResources().getString(R.string.app_name));
    }
}

package com.muzima.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;


import com.muzima.api.model.Media;

import java.io.File;
import java.util.List;

import com.muzima.R;

public class MemoryUtil {
    public static long getTotalMediaFileSize(List<Media> mediaList){
        long fileSize = 0;
        for(Media media : mediaList){
            fileSize+=media.getFileSize();
        }
        return fileSize;
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    public static void showAlertDialog(long availableSpace, long totalFileSize, Context context) {
        long requiredSpace = totalFileSize - availableSpace;
        String formattedMemory = MemoryUtil.getFormattedMemory(requiredSpace);
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(context))
                .setTitle(context.getResources().getString(R.string.general_low_available_space))
                .setMessage(context.getResources().getString(R.string.warning_new_no_enough_space, formattedMemory))
                .setNegativeButton(context.getString(R.string.general_ok), null)
                .create()
                .show();
    }

    public static String getFormattedMemory(long size){
        String suffix = " ";
        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
                if (size >= 1024) {
                    suffix = "GB";
                    size /= 1024;
                }
            }
        }
        String memorySize = Long.toString(size);
        return  memorySize.concat(suffix);
    }
}

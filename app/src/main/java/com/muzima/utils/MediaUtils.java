/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MediaUtils {

	public static boolean folderExists(String path) {
		boolean made = true;
		File dir = new File(path);
		if (!dir.exists())
			made = dir.mkdirs();

		return made;
	}

    public static String toBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

	public static Bitmap getBitmapScaledToDisplay(File f, int screenHeight,
			int screenWidth) {
		// Determine image size of f
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(f.getAbsolutePath(), o);

		int heightScale = o.outHeight / screenHeight;
		int widthScale = o.outWidth / screenWidth;

		// Powers of 2 work faster, sometimes, according to the doc.
		// We're just doing closest size that still fills the screen.
		int scale = Math.max(widthScale, heightScale);

		// get bitmap with scale ( < 1 is the same as 1)
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = scale;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), options);
	}

    public static void copyFile(File sourceFile, File destFile) {
        if (sourceFile.exists()) {
            FileChannel src;
            try {
                src = new FileInputStream(sourceFile).getChannel();
                FileChannel dst = new FileOutputStream(destFile).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            } catch (FileNotFoundException e) {
                Log.e("Media Utils", "FileNotFoundException while copying file", e);
            } catch (IOException e) {
                Log.e("Media Utils", "IOException while copying file", e);
            }
        } else
            Log.e("Media Utils", "Source file does not exist: " + sourceFile.getAbsolutePath());
    }

    public static int deleteVideoFileFromMediaProvider(Context context, String videoFile) {
        ContentResolver cr = context.getContentResolver();
        // video
        int count = 0;
        Cursor videoCursor = null;
        try {
            String select = MediaStore.Video.Media.DATA + "=?";
            String[] selectArgs = { videoFile };

            String[] projection = {MediaStore.Video.VideoColumns._ID};
            videoCursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection, select, selectArgs, null);
            if (videoCursor.getCount() > 0) {
                videoCursor.moveToFirst();
                List<Uri> videoToDelete = new ArrayList<>();
                do {
                    String id = videoCursor.getString(videoCursor
                            .getColumnIndex(MediaStore.Video.VideoColumns._ID));

                    videoToDelete.add(Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id));
                } while ( videoCursor.moveToNext());

                for ( Uri uri : videoToDelete ) {
                    Log.i("Media Utils","attempting to delete: " + uri );
                    count += cr.delete(uri, null, null);
                }
            }
        } catch ( Exception e ) {
            Log.e("Media Utils", e.toString());
        } finally {
            if ( videoCursor != null )
                videoCursor.close();
        }
        File f = new File(videoFile);
        if ( f.exists() )
            f.delete();

        return count;
    }

    public static final int deleteAudioFileFromMediaProvider(Context context, String audioFile) {
        ContentResolver cr = context.getContentResolver();
        // audio
        int count = 0;
        Cursor audioCursor = null;
        try {
            String select = MediaStore.Audio.Media.DATA + "=?";
            String[] selectArgs = { audioFile };

            String[] projection = {MediaStore.Audio.AudioColumns._ID};
            audioCursor = cr.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, select, selectArgs, null);
            if (audioCursor.getCount() > 0) {
                audioCursor.moveToFirst();
                List<Uri> audioToDelete = new ArrayList<>();
                do {
                    String id = audioCursor.getString(audioCursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID));

                    audioToDelete.add(Uri.withAppendedPath(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id));
                } while ( audioCursor.moveToNext());

                for ( Uri uri : audioToDelete ) {
                    Log.i("Media Utils","attempting to delete: " + uri );
                    count += cr.delete(uri, null, null);
                }
            }
        } catch ( Exception e ) {
            Log.e("Media Utils", e.toString(), e);
        } finally {
            if ( audioCursor != null )
                audioCursor.close();
        }
        File f = new File(audioFile);
        if ( f.exists() )
            f.delete();
        return count;
    }
}

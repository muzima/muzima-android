package com.muzima.utils;

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

public class ImageUtils {
	private final static String TAG = "ImageUtils";

	public static boolean folderExists(String path) {
		boolean made = true;
		File dir = new File(path);
		if (!dir.exists()) {
			made = dir.mkdirs();
		}
		return made;
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
		Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
		if (b != null) {
			Log.i(TAG,
					"Screen is " + screenHeight + "x" + screenWidth
							+ ".  Image has been scaled down by " + scale
							+ " to " + b.getHeight() + "x" + b.getWidth());
		}
		return b;
	}

	/**
	 * Converts Bitmap picture to a string which can be JSONified.
	 * 
	 * @param bitmapPicture
	 * @return
	 */
	public static String getStringFromBitmap(Bitmap bitmapPicture) {
		if (bitmapPicture != null ){
			final int COMPRESSION_QUALITY = 100;
			String encodedImage;
			ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
			bitmapPicture.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY,
					byteArrayBitmapStream);
			byte[] b = byteArrayBitmapStream.toByteArray();
			encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
			return encodedImage;
		}
		return null;
	}
	
	/**
	 * Convert an @Base64 stringified image back to a bitmap
	 * @param jsonString
	 * @return
	 */
	public Bitmap getBitmapFromString(String jsonString) {
		byte[] decodedString = Base64.decode(jsonString, Base64.DEFAULT);
		Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,
				decodedString.length);
		return decodedByte;
	}

    public static void deleteImage(Context context, String imageUri) {
        // get the file path and delete the file

        String[] projection = {MediaStore.Images.ImageColumns._ID};

        Cursor c = context.getContentResolver().query(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                        "_data='" + imageUri + "'", null, null);
        int del = 0;
        if (c.getCount() > 0) {
            c.moveToFirst();
            String id = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns._ID));

            Log.i(TAG,"attempting to delete: " + Uri.withAppendedPath(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
            del = context.getContentResolver().delete(Uri.withAppendedPath(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id), null, null);
        }
        c.close();

        Log.i(TAG, "Deleted " + del + " rows from media content provider");
    }

}
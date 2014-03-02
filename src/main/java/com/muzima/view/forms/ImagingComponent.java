package com.muzima.view.forms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.webkit.JavascriptInterface;
import com.muzima.utils.ImageUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.imaging.ImagingIntent;

import java.io.File;

public class ImagingComponent {
    private final static String TAG = "ImagingComponent";

    public static final int REQUEST_CODE = 0x0000c0de;
    private final Activity activity;
    private String fieldName;
    private static Context ctx;

    public ImagingComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startImageIntent(String fieldName) {
        this.fieldName = fieldName;
        Intent imagingIntent = new Intent(activity.getApplication(), ImagingIntent.class) ;
        activity.startActivityForResult(imagingIntent, REQUEST_CODE);
        ctx=activity.getApplicationContext();
    }

    public String getFieldName() {
        return fieldName;
    }

    public static String parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String imageUri = intent.getStringExtra("IMAGE_URI");
                String imageString = null;

                if (!StringUtils.isEmpty(imageUri))  {
                    //fetch the image and convert it to @Base64 encoded string. Delete the image
                    File f = new File(imageUri) ;
                    if (f.exists()){
                        // convert image to Bitmap
                        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                        imageString = ImageUtils.getStringFromBitmap(bmp);
                        ImageUtils.deleteImage(ctx, imageUri);
                    }
                }

                return imageString;
            }
        }
        return null;
    }
}

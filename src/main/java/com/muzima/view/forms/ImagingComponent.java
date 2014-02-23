package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import com.muzima.utils.imaging.ImagingIntent;

public class ImagingComponent {


    private final Activity activity;
    private String fieldName;

    public ImagingComponent(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void startImageIntent(String fieldName) {
        this.fieldName = fieldName;
        Intent imagingIntent = new Intent(activity.getApplication(), ImagingIntent.class) ;
        activity.startActivityForResult(imagingIntent, 1);
    }

    public String getFieldName() {
        return fieldName;
    }
}

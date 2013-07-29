package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class FormDataRepository {

    @JavascriptInterface
    public String saveFormSubmission(String paramsJSON, String data) {
        Log.e("DEBUG", "form submit button clicked");
        return "";
    }

}

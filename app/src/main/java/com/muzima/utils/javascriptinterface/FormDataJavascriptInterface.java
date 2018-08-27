package com.muzima.utils.javascriptinterface;

import android.util.Log;
import android.webkit.JavascriptInterface;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;
import com.muzima.utils.Constants;
import net.minidev.json.JSONValue;

import java.util.ArrayList;
import java.util.List;

public class FormDataJavascriptInterface {
    private final MuzimaApplication muzimaApplication;
    public FormDataJavascriptInterface(MuzimaApplication muzimaApplication){
        this.muzimaApplication = muzimaApplication;
    }

    @JavascriptInterface
    public String getCompleteFormData(){
        List<FormData> completeFormData = new ArrayList<>();
        try {
            completeFormData = muzimaApplication.getFormController().getAllFormData(Constants.STATUS_COMPLETE);
        } catch (FormController.FormDataFetchException e) {
            Log.e(getClass().getSimpleName(),"Error while fetching form data",e);
        }
        return JSONValue.toJSONString(completeFormData);
    }

}

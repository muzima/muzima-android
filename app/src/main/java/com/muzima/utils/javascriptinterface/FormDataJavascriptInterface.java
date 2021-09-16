/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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

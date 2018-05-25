/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;
import android.content.Intent;
import android.os.Bundle;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Provider;
import com.muzima.api.service.FormService;
import com.muzima.utils.Constants;
import com.muzima.controller.FormController.FormDataFetchException;
import com.muzima.controller.FormController;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_REGISTRATION;
import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_GENERIC_REGISTRATION;
import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION;


public class ProviderReportsFormActivity extends FormsActivityBase {
    public static final String TAG = "FormsActivity";
    public Provider provider;
    public FormController formController;
    public FormService formService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        initPager();
        initPagerIndicator();

    }

    @Override
    protected MuzimaPagerAdapter createFormsPagerAdapter() {
        try {
            totalTestedCount();
            totalPositiveCount();
            totalLinkedCount();
            totalContactsCount();
        } catch (IOException e) {

        } catch (JSONException e) {

        }



        List<FormData> allNonRegForms = new ArrayList<FormData>();
        return (MuzimaPagerAdapter) allNonRegForms;

    }
    public Integer totalTestedCount() throws IOException, JSONException {                                     //All tested
        Integer count = 0;
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        FormController formController = muzimaApplication.getFormController();

        List<FormData> htsFormData = new ArrayList<FormData>();
        List<FormData> allFormData = null;


        try {
            allFormData = muzimaApplication.getFormController().getAllFormData(Constants.STATUS_COMPLETE);
        } catch (FormDataFetchException e) {
        }
        for (FormData formData : allFormData) {
            if (!isRegistrationFormData(formData)) {
                org.json.JSONObject object = new org.json.JSONObject(formData.getJsonPayload());
                String formUuid = ((org.json.JSONObject) object.get("encounter")).get("encounter.form_uuid").toString();
               if(formUuid.equals("84c21c60-1910-4e0a-8579-a303d94b6c7b")){                                                 // Selects HTS forms by UUID
                   htsFormData.add(formData);
                   count = htsFormData.size();
               }
            }
        }
        System.out.println("Total tested"+count);
        return count;
    }
    public Integer totalPositiveCount() throws IOException, JSONException {                                //Total positive
        Integer count = 0;
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        FormController formController = muzimaApplication.getFormController();

        List<FormData> htsFormData = new ArrayList<FormData>();
        List<FormData> allFormData = null;


        try {
            allFormData = muzimaApplication.getFormController().getAllFormData(Constants.STATUS_COMPLETE);
        } catch (FormDataFetchException e) {
        }
        for (FormData formData : allFormData) {
            if (!isRegistrationFormData(formData)) {
                org.json.JSONObject object = new org.json.JSONObject(formData.getJsonPayload());
                String formUuid = ((org.json.JSONObject) object.get("encounter")).get("encounter.form_uuid").toString();
                if (formUuid.equals("84c21c60-1910-4e0a-8579-a303d94b6c7b")) {                                                 // Selects HTS forms by UUID
                    String finalHIVResult = ((org.json.JSONObject) object.get("observation")).get("159427^FINAL HIV RESULTS^99DCT").toString();
                    if (finalHIVResult.equals("703^POSITIVE^99DCT")) {
                        htsFormData.add(formData);
                        count = htsFormData.size();
                    }
                }

            }
        }
        System.out.println("Total positive"+count);
        return count;
    }
    public Integer totalLinkedCount() throws IOException, JSONException {                                //Total linked
        Integer count = 0;
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        FormController formController = muzimaApplication.getFormController();

        List<FormData> linkageFormData = new ArrayList<FormData>();
        List<FormData> allFormData = null;


        try {
            allFormData = muzimaApplication.getFormController().getAllFormData(Constants.STATUS_COMPLETE);
        } catch (FormDataFetchException e) {
        }
        for (FormData formData : allFormData) {
            if (!isRegistrationFormData(formData)) {
                org.json.JSONObject object = new org.json.JSONObject(formData.getJsonPayload());
                String formUuid = ((org.json.JSONObject) object.get("encounter")).get("encounter.form_uuid").toString();
                if (formUuid.equals("25a541fb-9126-4616-994e-e2b2b4fd4ca1")) {                                                 // Selects Linkage and referral forms by UUID
                    String linkedInCare = ((org.json.JSONObject) object.get("observation")).get("159811^ENROLLED IN HIV CARE^99DCT").toString();
                    if (linkedInCare.equals("1065^YES^99DCT")){
                        linkageFormData.add(formData);
                        count = linkageFormData.size();
                    }
                }

            }
        }
        System.out.println("Total linked"+count);
        return count;
    }
    public Integer totalContactsCount() throws IOException, JSONException {                                //Total linked
        Integer count = 0;
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        FormController formController = muzimaApplication.getFormController();

        List<FormData> contactsFormData = new ArrayList<FormData>();
        List<FormData> allFormData = null;
       // String[] contacts ={};
        try {
            allFormData = muzimaApplication.getFormController().getAllFormData(Constants.STATUS_COMPLETE);
        } catch (FormDataFetchException e) {
        }
        for (FormData formData : allFormData) {
            if (!isRegistrationFormData(formData)) {
                org.json.JSONObject object = new org.json.JSONObject(formData.getJsonPayload());
                String formUuid = ((org.json.JSONObject) object.get("encounter")).get("encounter.form_uuid").toString();
                if (formUuid.equals("8ba71e39-bc0f-408a-9dbc-19982ffd0f14")) {                                                 // Selects contact listing forms by UUID
                   String contactsObservation = ((org.json.JSONObject) object.get("observation")).get("160593^Expected^99DCT").toString();
                    JSONArray jsonArray = new JSONArray(contactsObservation);
                       count = jsonArray.getJSONObject(0).length();
                        //System.out.println("array length is: "+count);/*the result is 1! */
                    }
            }
        }

        return count;
    }
    public boolean isRegistrationFormData(FormData formData){
        return formData.getDiscriminator().equalsIgnoreCase(FORM_DISCRIMINATOR_REGISTRATION)
                || formData.getDiscriminator().equalsIgnoreCase(FORM_JSON_DISCRIMINATOR_REGISTRATION)
                || formData.getDiscriminator().equalsIgnoreCase(FORM_JSON_DISCRIMINATOR_GENERIC_REGISTRATION);
    }

}




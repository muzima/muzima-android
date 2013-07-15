
package com.muzima;

import android.app.Application;

import com.muzima.api.context.Context;
import com.muzima.api.context.ContextFactory;
import com.muzima.controller.FormController;
import com.muzima.util.Constants;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@ReportsCrashes(formKey = "ACRA_FORM_KEY")
public class MuzimaApplication extends Application{
    private Context muzimaContext;
    private FormController formController;

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
        try {
            ContextFactory.setProperty(Constants.RESOURCE_CONFIGURATION_STRING, getConfigurationString());
            muzimaContext = ContextFactory.createContext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Context getMuzimaContext(){
        return muzimaContext;
    }

    public FormController getFormController(){
        if(formController == null){
            try {
                formController = new FormController(muzimaContext.getFormService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return formController;
    }

    private String getConfigurationString() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.configuration);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }
}

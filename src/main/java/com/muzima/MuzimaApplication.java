
package com.muzima;

import android.app.Application;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "YOUR_FORM_KEY")
public class MuzimaApplication extends Application{

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }

}

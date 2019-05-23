package com.muzima.messaging.jobs.requirements;

import android.content.Context;

import com.muzima.messaging.jobmanager.dependencies.ContextDependent;
import com.muzima.messaging.jobmanager.requirements.SimpleRequirement;
import com.muzima.messaging.sms.TelephonyServiceState;

public class ServiceRequirement extends SimpleRequirement implements ContextDependent {

    private static final String TAG = ServiceRequirement.class.getSimpleName();

    private transient Context context;

    public ServiceRequirement(Context context) {
        this.context  = context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public boolean isPresent() {
        TelephonyServiceState telephonyServiceState = new TelephonyServiceState();
        return telephonyServiceState.isConnected(context);
    }
}

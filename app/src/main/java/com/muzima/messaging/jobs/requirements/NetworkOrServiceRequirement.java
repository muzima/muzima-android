package com.muzima.messaging.jobs.requirements;

import android.content.Context;

import com.muzima.messaging.jobmanager.dependencies.ContextDependent;
import com.muzima.messaging.jobmanager.requirements.NetworkRequirement;
import com.muzima.messaging.jobmanager.requirements.SimpleRequirement;

public class NetworkOrServiceRequirement extends SimpleRequirement implements ContextDependent {

    private transient Context context;

    public NetworkOrServiceRequirement(Context context) {
        this.context = context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public boolean isPresent() {
        NetworkRequirement networkRequirement = new NetworkRequirement(context);
        ServiceRequirement serviceRequirement = new ServiceRequirement(context);

        return networkRequirement.isPresent() || serviceRequirement.isPresent();
    }
}

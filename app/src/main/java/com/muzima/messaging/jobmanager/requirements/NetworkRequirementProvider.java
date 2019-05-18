package com.muzima.messaging.jobmanager.requirements;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class NetworkRequirementProvider implements RequirementProvider {

    private RequirementListener listener;

    private final NetworkRequirement requirement;

    public NetworkRequirementProvider(Context context) {
        this.requirement = new NetworkRequirement(context);

        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listener == null) {
                    return;
                }

                if (requirement.isPresent()) {
                    listener.onRequirementStatusChanged();
                }
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void setListener(RequirementListener listener) {
        this.listener = listener;
    }
}

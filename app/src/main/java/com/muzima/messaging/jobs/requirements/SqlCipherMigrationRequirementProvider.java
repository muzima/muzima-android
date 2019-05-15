package com.muzima.messaging.jobs.requirements;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.muzima.messaging.jobmanager.requirements.RequirementListener;
import com.muzima.messaging.jobmanager.requirements.RequirementProvider;

public class SqlCipherMigrationRequirementProvider implements RequirementProvider {

    private RequirementListener listener;

    public SqlCipherMigrationRequirementProvider() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void setListener(RequirementListener listener) {
        this.listener = listener;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SqlCipherNeedsMigrationEvent event) {
        if (listener != null) listener.onRequirementStatusChanged();
    }

    public static class SqlCipherNeedsMigrationEvent {

    }
}

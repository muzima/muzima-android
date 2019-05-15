package com.muzima.messaging.jobmanager.requirements;

import android.support.annotation.NonNull;

import com.muzima.messaging.jobmanager.Job;

import java.io.Serializable;

public interface Requirement extends Serializable {
    /**
     * @return true if the requirement is satisfied, false otherwise.
     */
    boolean isPresent(@NonNull Job job);

    void onRetry(@NonNull Job job);
}

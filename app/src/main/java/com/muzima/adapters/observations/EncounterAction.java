/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import com.muzima.api.model.Encounter;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Encounters;

import java.util.List;

abstract class EncounterAction {
    abstract Encounters get() throws ObservationController.LoadObservationException;
    abstract Encounters get(Encounter encounter) throws ObservationController.LoadObservationException;
    abstract List<Encounter> getEncounters() throws ObservationController.LoadObservationException;
}

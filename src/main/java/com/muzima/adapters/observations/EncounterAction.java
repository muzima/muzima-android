package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Encounters;

public abstract class EncounterAction {
    abstract Encounters get() throws ObservationController.LoadObservationException;
}

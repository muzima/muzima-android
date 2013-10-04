package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Concepts;

public abstract class ConceptAction {
    abstract Concepts get() throws ObservationController.LoadObservationException;
}

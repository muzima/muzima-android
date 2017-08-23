/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.builders;

import com.muzima.model.CompleteForm;

public class CompleteFormBuilder extends FormWithDataBuilder<CompleteFormBuilder, CompleteForm>{

    public CompleteFormBuilder() {
        formWithData = new CompleteForm();
    }
}

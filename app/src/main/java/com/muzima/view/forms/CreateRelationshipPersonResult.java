/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

public class CreateRelationshipPersonResult {
    private String personUuid;

    public CreateRelationshipPersonResult(String personUuid){
        this.personUuid = personUuid;
    }

    public String getPersonUuid() {
        return personUuid;
    }
}

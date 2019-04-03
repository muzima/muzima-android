/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StringUtilsTest {

    @Test
    public void shouldReturnCommaSeparatedList(){
        ArrayList<String> listOfStrings = new ArrayList<String>() {{
            add("Patient");
            add("Registration");
            add("New Tag");
        }};
        String commaSeparatedValues = StringUtils.getCommaSeparatedStringFromList(listOfStrings);
        assertThat(commaSeparatedValues, is("Patient,Registration,New Tag"));
    }
}

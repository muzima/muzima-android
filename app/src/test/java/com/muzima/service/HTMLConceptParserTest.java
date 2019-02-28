/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class HTMLConceptParserTest {

    @Test
    public void shouldReturnListOfConcepts() {
        String html = readFile();
        List<String> concepts = new HTMLConceptParser().parse(html);
        assertThat(concepts.size(),is(7));

        assertThat(concepts,hasItem("BODY PART"));
        assertThat(concepts,hasItem("PROCEDURES DONE THIS VISIT"));
        assertThat(concepts,hasItem("ANATOMIC LOCATION DESCRIPTION"));
        assertThat(concepts,hasItem("CLOCK FACE CERVICAL BIOPSY LOCATION "));
        assertThat(concepts,hasItem("PATHOLOGICAL DIAGNOSIS ADDED"));
        assertThat(concepts,hasItem("FREETEXT GENERAL"));
        assertThat(concepts,hasItem("RETURN VISIT DATE"));
    }

    private String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/histo_form.html");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}

/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class ConceptParserTest {

    private ConceptParser utils;

    @Before
    public void setUp() {
        utils = new ConceptParser(new MXParser());
    }

    @Test
    public void shouldParseConcept() {
        List<String> conceptNames = utils.parse(getModel("concept.xml"));
        assertThat(conceptNames, hasItem("PULSE"));
    }

    @Test
    public void shouldParseConceptInObs() {
        List<String> conceptNames = utils.parse(getModel("concept_in_obs.xml"));
        assertThat(conceptNames, hasItem("WEIGHT (KG)"));
    }

    @Test
    public void shouldNotAddItToConceptUnLessBothDateAndTimeArePresentInChildren() {
        List<String> conceptNames = utils.parse(getModel("concepts_in_concept.xml"));
        assertThat(conceptNames.size(), is(2));
        assertThat(conceptNames, hasItem("PROBLEM ADDED"));
        assertThat(conceptNames, hasItem("PROBLEM RESOLVED"));
    }

    @Test
    public void shouldNotConsiderOptionsAsConcepts() {
        List<String> conceptNames = utils.parse(getModel("concepts_with_options.xml"));
        assertThat(conceptNames.size(), is(1));
        assertThat(conceptNames, hasItem("MOST RECENT PAPANICOLAOU SMEAR RESULT"));
    }

    @Test(expected = ConceptParser.ParseConceptException.class)
    public void shouldThrowParseConceptExceptionWhenTheModelHasNoEndTag() {
        utils.parse(getModel("concept_no_end_tag.xml"));
    }

    @Test
    public void shouldParseConceptTM() {
        List<String> conceptNames = utils.parse(getModel("dispensary_concept_in_obs.xml"));
        assertThat(conceptNames, hasItem("START TIME"));
        assertThat(conceptNames, hasItem("END TIME"));
    }

    private String getModel(String modelFileName) {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("xml/" + modelFileName);
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}

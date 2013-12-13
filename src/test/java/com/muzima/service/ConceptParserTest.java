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
    public void setUp() throws Exception {
        utils = new ConceptParser(new MXParser());
    }

    @Test
    public void shouldParseConcept() throws Exception {
        List<String> conceptNames = utils.parse(getModel("concept.xml"));
        assertThat(conceptNames, hasItem("PULSE"));
    }

    @Test
    public void shouldParseConceptInObs() throws Exception {
        List<String> conceptNames = utils.parse(getModel("concept_in_obs.xml"));
        assertThat(conceptNames, hasItem("WEIGHT (KG)"));
    }

    @Test
    public void shouldNotAddItToConceptUnLessBothDateAndTimeArePresentInChildren() throws Exception {
        List<String> conceptNames = utils.parse(getModel("concepts_in_concept.xml"));
        assertThat(conceptNames.size(), is(2));
        assertThat(conceptNames, hasItem("PROBLEM ADDED"));
        assertThat(conceptNames, hasItem("PROBLEM RESOLVED"));
    }

    @Test
    public void shouldNotConsiderOptionsAsConcepts() throws Exception {
        List<String> conceptNames = utils.parse(getModel("concepts_with_options.xml"));
        assertThat(conceptNames.size(), is(1));
        assertThat(conceptNames, hasItem("MOST RECENT PAPANICOLAOU SMEAR RESULT"));
    }

    @Test(expected = ConceptParser.ParseConceptException.class)
    public void shouldThrowParseConceptExceptionWhenTheModelHasNoEndTag() throws Exception {
        utils.parse(getModel("concept_no_end_tag.xml"));
    }

    @Test
    public void shouldParseConceptTM() throws Exception {
        List<String> conceptNames = utils.parse(getModel("dispensary_concept_in_obs.xml"));
        assertThat(conceptNames, hasItem("START TIME"));
        assertThat(conceptNames, hasItem("END TIME"));
    }

    public String getModel(String modelFileName) {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("xml/" + modelFileName);
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}

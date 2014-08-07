/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.ObservationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConceptControllerTest {

    @Mock
    private ConceptService service;

    @Mock
    private ObservationService observationService;

    private ConceptController controller;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        controller = new ConceptController(service,observationService);
    }

    @Test
    public void shouldDownloadConceptUsingNonPreferredName() throws Exception, ConceptController.ConceptDownloadException {
        List<Concept> concepts = new ArrayList<Concept>();
        final String nonPreferredName = "NonPreferredName";
        Concept aConcept = new Concept() {{
            setConceptNames(new ArrayList<ConceptName>() {{
                add(new ConceptName() {{
                    setName("PreferredName");
                    setPreferred(true);
                }});
                add(new ConceptName() {{
                    setName(nonPreferredName);
                    setPreferred(false);
                }});
            }});
        }};
        concepts.add(aConcept);
        when(service.downloadConceptsByName(nonPreferredName)).thenReturn(concepts);
        List<String> listOfName = new ArrayList<String>() {{
            add(nonPreferredName);
        }};
        List<Concept> expectedResult = new ArrayList<Concept>();
        expectedResult.add(aConcept);
        assertThat(controller.downloadConceptsByNames(listOfName), is(expectedResult));
    }

    @Test
    public void shouldReturnAConceptThatMatchesNameExactly() throws Exception, ConceptController.ConceptFetchException {
        String conceptName = "conceptName";
        List<Concept> conceptList = asList(createConceptByName("someName"),createConceptByName(conceptName));
        when(service.getConceptsByName(conceptName)).thenReturn(conceptList);

        Concept conceptByName = controller.getConceptByName(conceptName);
        assertThat(conceptByName.getName(),is(conceptName));
    }

    @Test
    public void shouldReturnNullIfNoConceptMatchesTheName() throws Exception, ConceptController.ConceptFetchException {
        String conceptName = "conceptName";
        List<Concept> conceptList = asList(createConceptByName("someName"),createConceptByName("someOtherName"));
        when(service.getConceptsByName(conceptName)).thenReturn(conceptList);

        Concept conceptByName = controller.getConceptByName(conceptName);
        assertThat(conceptByName,nullValue());
    }

    private Concept createConceptByName(String name) {
        Concept concept = new Concept();
        ConceptName conceptName = new ConceptName();
        conceptName.setName(name);
        conceptName.setPreferred(true);
        List<ConceptName> conceptNames = asList(conceptName);
        concept.setConceptNames(conceptNames);
        return concept;
    }
}

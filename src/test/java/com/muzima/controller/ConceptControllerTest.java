package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.service.ConceptService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConceptControllerTest {

    private ConceptService service;
    private ConceptController controller;

    @Before
    public void setUp() throws Exception {
        service = mock(ConceptService.class);
        controller = new ConceptController(service);
    }

    @Test
    public void shouldDownloadConceptUsingNonPreferredName() throws Exception, ConceptController.ConceptDownloadException {
        List<Concept> concepts = new ArrayList<Concept>();
        final String nonPreferredName = "NonPreferredName";
        Concept aConcept= new Concept(){{
            setConceptNames(new ArrayList<ConceptName>(){{
                add(new ConceptName(){{
                    setName("PreferredName");
                    setPreferred(true);
                }});
                add(new ConceptName(){{
                    setName(nonPreferredName);
                    setPreferred(false);
                }});
            }});
        }};
        concepts.add(aConcept);
        when(service.downloadConceptsByName(nonPreferredName)).thenReturn(concepts);
        List<String> listOfName = new ArrayList<String>(){{
            add(nonPreferredName);
        }};
        List<Concept> expectedResult = new ArrayList<Concept>();
        expectedResult.add(aConcept);
        assertThat(controller.downloadConceptsByNames(listOfName), is(expectedResult));
    }
}

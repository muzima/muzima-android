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

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/histo_form.html");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}

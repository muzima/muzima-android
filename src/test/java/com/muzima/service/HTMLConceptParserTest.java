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
        assertThat(concepts,hasItem("8265^BODY PART^99DCT"));
        assertThat(concepts,hasItem("7479^PROCEDURES DONE THIS VISIT^99DCT"));
        assertThat(concepts,hasItem("8268^ANATOMIC LOCATION DESCRIPTION^99DCT"));
        assertThat(concepts,hasItem("7481^CLOCK FACE CERVICAL BIOPSY LOCATION ^99DCT"));
        assertThat(concepts,hasItem("8278^PATHOLOGICAL DIAGNOSIS ADDED^99DCT"));
        assertThat(concepts,hasItem("1915^FREETEXT GENERAL^99DCT"));
        assertThat(concepts,hasItem("5096^RETURN VISIT DATE^99DCT"));
    }

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/histo_form.html");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}

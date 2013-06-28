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

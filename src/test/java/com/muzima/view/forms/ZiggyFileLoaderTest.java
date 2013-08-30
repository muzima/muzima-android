package com.muzima.view.forms;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ZiggyFileLoaderTest {


    private ZiggyFileLoader json;

    @Before
    public void setUp() {
        json = new ZiggyFileLoader(null, null, null, "json");
    }

    @Test
    public void loadAppData_shouldReturnEmptyArrayForEntityRelationships(){
        assertThat(json.loadAppData("entity_relationship.json"), is("[]"));
    }

    @Test
    public void loadAppData_shouldReturnModelJSONIfAnyOtherFileIsAskedToBeLoaded(){
        assertThat(json.loadAppData("form"), is("json"));
    }
}

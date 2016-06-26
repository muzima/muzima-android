/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Location;
import com.muzima.api.service.LocationService;
import org.apache.lucene.queryParser.ParseException;
import org.aspectj.lang.annotation.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocationControllerTest {

    LocationController locationController;
    LocationService locationService;

    @org.junit.Before
    public void setup() {
        locationService = mock(LocationService.class);
        locationController = new LocationController(locationService);
    }

    @Test
    public void shouldSearchOnServerForLocationsByNames() throws Exception, LocationController.LocationDownloadException {
        String name = "name";
        List<Location> locations = new ArrayList<Location>();

        when(locationService.downloadLocationsByName(name)).thenReturn(locations);
        assertThat(locationController.downloadLocationFromServerByName(name), is(locations));
        verify(locationService).downloadLocationsByName(name);
    }

    @Test
    public void shouldSearchOnServerForLocationByUuid() throws Exception, LocationController.LocationDownloadException {
        String uuid = "uuid";
        Location location = new Location();

        when(locationService.downloadLocationByUuid(uuid)).thenReturn(location);
        assertThat(locationController.downloadLocationFromServerByUuid(uuid), is(location));
        verify(locationService).downloadLocationByUuid(uuid);
    }

    @Test
    public void getAllLocations_shouldReturnAllAvailableLocations() throws IOException, ParseException, LocationController.LocationLoadException {
        List<Location> locations = new ArrayList<Location>();
        when(locationService.getAllLocations()).thenReturn(locations);

        assertThat(locationController.getAllLocations(), is(locations));
    }

    @Test(expected = LocationController.LocationLoadException.class)
    public void getAllLocations_shouldThrowLoLocationFetchExceptionIfExceptionThrownByLocationService() throws IOException, ParseException, LocationController.LocationLoadException {
        doThrow(new IOException()).when(locationService).getAllLocations();
        locationController.getAllLocations();
    }

    @Test
    public void getLocationByUuid_shouldReturnLocationForId() throws Exception, LocationController.LocationLoadException {
        Location location = new Location();
        String uuid = "uuid";

        when(locationService.getLocationByUuid(uuid)).thenReturn(location);

        assertThat(locationController.getLocationByUuid(uuid), is(location));
    }

    @Test
    @Ignore("Need to fix the Android Log class util")
    public void shouldReturnEmptyListIsExceptionThrown() throws Exception, LocationController.LocationDownloadException {
        String searchString = "name";
        doThrow(new IOException()).when(locationService).downloadLocationsByName(searchString);

        assertThat(locationController.downloadLocationFromServerByName(searchString).size(), is(0));
    }
}

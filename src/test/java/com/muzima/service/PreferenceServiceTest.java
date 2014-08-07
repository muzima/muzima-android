/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.content.SharedPreferences;
import com.muzima.MuzimaApplication;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class PreferenceServiceTest {

    private CohortPrefixPreferenceService preferenceService;
    private MuzimaApplication muzimaApplication;
    private SharedPreferences sharedPref;


    @Before
    public void setUp() throws Exception {
        muzimaApplication = mock(MuzimaApplication.class);
        sharedPref = mock(SharedPreferences.class);
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE)).thenReturn(sharedPref);
        when(muzimaApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPref);
        preferenceService = new CohortPrefixPreferenceService(muzimaApplication);
    }

    @Test
    public void getCohorts_shouldReturnEmptyListWhenNoCohortsInPreferences() throws Exception {
        assertThat(preferenceService.getCohortPrefixes().isEmpty(), is(true));
    }

    @Test
    public void getCohorts_shouldReturnCohortPrefixesInListWhenCohortsPrefixesDefinedInPreferences() throws Exception {
        when(sharedPref.getString(COHORT_PREFIX_PREF_KEY, "")).thenReturn("[\"Prefix1\",\"Prefix2\",\"Prefix3\"]");

        assertThat(preferenceService.getCohortPrefixes().size(), is(3));
    }
}

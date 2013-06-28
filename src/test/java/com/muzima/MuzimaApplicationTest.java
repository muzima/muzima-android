package com.muzima;

import com.muzima.db.Html5FormDataSource;
import com.muzima.service.FormsService;
import com.muzima.testSupport.CustomTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(CustomTestRunner.class)
public class MuzimaApplicationTest {

    private MuzimaApplication muzimaApplication;

    @Before
    public void setup(){
        muzimaApplication = new MuzimaApplication();
    }

    @Test
    public void getHtml5FormDataSource_shouldCreateInstanceOnlyOnce(){
        Html5FormDataSource html5FormDataSource = muzimaApplication.getHtml5FormDataSource();
        assertThat(muzimaApplication.getHtml5FormDataSource(), is(html5FormDataSource));
    }

    @Test
    public void getFormsService_shouldCreateInstanceOnlyOnce(){
        FormsService formService = muzimaApplication.getFormsService();
        assertThat(muzimaApplication.getFormsService(), is(formService));
    }

}

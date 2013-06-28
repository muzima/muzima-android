package com.muzima.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.muzima.Urls;
import com.muzima.db.Html5FormDataSource;
import com.muzima.domain.Html5Form;
import com.muzima.testSupport.CustomTestRunner;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.HttpRequestInfo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static com.muzima.service.FormsService.FETCHING;
import static com.muzima.service.FormsService.NOT_FETCHING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class FormsServiceTest {

    private Context context;
    private Html5FormDataSource html5FormDataSource;
    private FormsService formsService;
    private NetworkInfo networkInfo;
    private HttpService httpService;
    private FormsService.OnDataFetchComplete dataFetchCompleteListener;

    @Before
    public void setup(){
        context = mock(Context.class);
        html5FormDataSource = mock(Html5FormDataSource.class);
        dataFetchCompleteListener = mock(FormsService.OnDataFetchComplete.class);
        httpService = new HttpService();
        formsService = new FormsService(context, html5FormDataSource, httpService);
        formsService.setDataFetchListener(dataFetchCompleteListener);
    }

    @Test
    public void shouldHaveFetchingStatusAsNotFetchingByDefault(){
        assertThat(formsService.getCurrentState(), is(NOT_FETCHING));
    }

    @Test
    public void fetchForms_shouldReturnNoConnectionStatusIfNoInternetConnectionIsAvailable(){
        setupNotConnectedNetworkExpectation();

        formsService.fetchForms();

        verify(dataFetchCompleteListener).onFetch(FormsService.NO_NETWORK_CONNECTIVITY);
    }

    @Test
    public void fetchForms_shouldReturnAlreadyFetchingStatusIfAlreadyFetchingForms(){
        setupConnectedNetworkExpectation();

        makeBlockingFetchFormsRequest();
        assertThat(formsService.getCurrentState(), is(FETCHING));

        formsService.fetchForms();
        verify(dataFetchCompleteListener).onFetch(FormsService.ALREADY_FETCHING);
    }

    @Test
    public void fetchForms_shouldMakeAGETRequest(){
        setupConnectedNetworkExpectation();
        Robolectric.getBackgroundScheduler().pause();
        
        formsService.fetchForms();
        Robolectric.addPendingHttpResponse(200, "");
        Robolectric.getBackgroundScheduler().runOneTask();

        HttpRequestInfo sentHttpRequestData = Robolectric.getSentHttpRequestInfo(0);
        HttpRequest sentHttpRequest = sentHttpRequestData.getHttpRequest();
        assertThat(sentHttpRequest.getRequestLine().getUri(), equalTo(Urls.FORMS_GET_URL));
        assertThat(sentHttpRequest.getRequestLine().getMethod(), equalTo(HttpGet.METHOD_NAME));
    }

    @Test
    public void fetchForms_shouldNotifyIOException() throws IOException, URISyntaxException {
        httpService = mock(HttpService.class);
        formsService = new FormsService(context, html5FormDataSource, httpService);
        formsService.setDataFetchListener(dataFetchCompleteListener);

        setupConnectedNetworkExpectation();
        when(httpService.get(Urls.FORMS_GET_URL, null)).thenThrow(new IOException());

        formsService.fetchForms();

        verify(dataFetchCompleteListener).onFetch(FormsService.IO_EXCEPTION);
    }

    @Test
    public void fetchForms_shouldNotifyJSONException() throws IOException, URISyntaxException {
        setupConnectedNetworkExpectation();
        Robolectric.getBackgroundScheduler().pause();

        formsService.fetchForms();
        Robolectric.addPendingHttpResponse(200, INVALID_JSON_RESPONSE);
        Robolectric.getBackgroundScheduler().runOneTask();

        verify(dataFetchCompleteListener).onFetch(FormsService.JSON_EXCEPTION);
    }

    @Test
    public void fetchForms_shouldNotifyFetchSuccessful() throws IOException, URISyntaxException {
        setupConnectedNetworkExpectation();
        Robolectric.getBackgroundScheduler().pause();

        formsService.fetchForms();
        Robolectric.addPendingHttpResponse(200, VALID_JSON_RESPONSE);
        Robolectric.getBackgroundScheduler().runOneTask();

        verify(dataFetchCompleteListener).onFetch(FormsService.FETCH_SUCCESSFUL);
    }

    @Test
    public void fetchForms_shouldSaveForms() throws IOException, URISyntaxException {
        setupConnectedNetworkExpectation();
        Robolectric.getBackgroundScheduler().pause();

        formsService.fetchForms();
        Robolectric.addPendingHttpResponse(200, VALID_JSON_RESPONSE);
        Robolectric.getBackgroundScheduler().runOneTask();

        ArrayList<Html5Form> forms = new ArrayList<Html5Form>() {{
            add(new Html5Form("1", "Patient Registration Form", "Form for registering patients", new ArrayList<String>(){{
                add("Registration");
                add("Patient");
            }}));
            add(new Html5Form("2", "PMTCT Ante-Natal Care Form", "", new ArrayList<String>(){{
                add("Registration");
            }}));
        }};
        verify(html5FormDataSource).deleteAllForms();
        verify(html5FormDataSource).saveForms(forms);
        verify(dataFetchCompleteListener).onFetch(FormsService.FETCH_SUCCESSFUL);
    }

    private void makeBlockingFetchFormsRequest() {
        Robolectric.getBackgroundScheduler().pause();
        formsService.fetchForms();
    }

    private void setupNetworkExpectations() {
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        networkInfo = mock(NetworkInfo.class);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    }

    private void setupNotConnectedNetworkExpectation(){
        setupNetworkExpectations();
        when(networkInfo.isConnected()).thenReturn(false);
    }

    private void setupConnectedNetworkExpectation(){
        setupNetworkExpectations();
        when(networkInfo.isConnected()).thenReturn(true);
    }

    private String INVALID_JSON_RESPONSE = "{name:'Patient'}";
    private String VALID_JSON_RESPONSE = "[\n" +
            "    {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Patient Registration Form\",\n" +
            "        \"description\": \"Form for registering patients\",\n" +
            "        \"selected\": false,\n" +
            "        \"tags\": [\n" +
            "            {\n" +
            "                \"id\": 1,\n" +
            "                \"name\": \"Registration\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": 2,\n" +
            "                \"name\": \"Patient\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"PMTCT Ante-Natal Care Form\",\n" +
            "        \"description\": \"\",\n" +
            "        \"selected\": false,\n" +
            "        \"tags\": [\n" +
            "            {\n" +
            "                \"id\": 1,\n" +
            "                \"name\": \"Registration\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "]";
}

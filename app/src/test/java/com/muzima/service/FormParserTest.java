/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

class FormParserTest {
//
//    private FormParser formParser;
//    private String formDataUuid;
//
//    @Mock
//    private PatientController patientController;
//    @Mock
//    private ConceptController conceptController;
//    @Mock
//    private EncounterController encounterController;
//    @Mock
//    private ObservationController observationController;
//
//    @Before
//    public void setUp() {
//        initMocks(this);
//        formDataUuid = "formDataUuid";
//    }
//
//    @Test
//    public void shouldCreateMultipleObservations() throws IOException, XmlPullParserException, ParseException,
//            PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
////        String xml = readFile("xml/histo_xml_payload.xml");
////        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
////
////        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
////        assertThat(observations.size(), is(6));
//    }
//
//    @Test
//    public void shouldAssociateCorrectConceptForObservation() throws IOException, XmlPullParserException,
//            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
////        String xml = readFile("xml/one_date_observation.xml");
////        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
////        Concept aConcept = mock(Concept.class);
////        String conceptName = "RETURN VISIT DATE";
////        when(conceptController.getConceptByName(conceptName)).thenReturn(aConcept);
////
////        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
////        verify(conceptController).getConceptByName(conceptName);
////        assertThat(observations.get(0).getConcept(), is(aConcept));
//    }
//
//    @Test
//    public void shouldParseObservationOfTypeConcept() throws IOException, XmlPullParserException, ParseException,
//            PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/value_concept_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        Concept observedConcept = mock(Concept.class);
//        when(conceptController.getConceptByName("BODY PART")).thenReturn(aConcept);
//        when(conceptController.getConceptByName("CERVIX")).thenReturn(observedConcept);
//        when(aConcept.isCoded()).thenReturn(true);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        Observation observation = observations.get(0);
//        assertThat(observation.getValueCoded(), is(observedConcept));
//    }
//
//    @Test
//    public void shouldParsePayloadWithMultipleSelectObservations() throws IOException, XmlPullParserException,
//            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/multiple_select_value_concept_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        when(conceptController.getConceptByName(anyString())).thenReturn(aConcept);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        assertThat(observations.size(), is(6));
//        assertThat(observations.get(0).getValueText(), is("testing"));
//        assertThat(observations.get(1).getValueText(), is("5963^SHORTNESS OF BREATH WITH EXERTION^99DCT"));
//        assertThat(observations.get(4).getValueText(), is("1470^CHRONIC COUGH^99DCT"));
//        assertThat(observations.get(5).getValueText(), is("2014-03-01"));
//    }
//
//    @Test
//    public void shouldParsePayloadWithMultipleSelectObservationsAndNoneSelected() throws IOException,
//            XmlPullParserException, ParseException, PatientController.PatientLoadException,
//            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException,
//            ConceptController.ConceptParseException, ObservationController.ParseObservationException{
//        String xml = readFile("xml/multiple_select_value_concept_observation_with_no_selection.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        when(conceptController.getConceptByName(anyString())).thenReturn(aConcept);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        assertThat(observations.size(), is(0));
//    }
//
//    @Test(expected = ObservationController.ParseObservationException.class)
//    public void shouldNotCreateObservationWithEmptyValue() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/observation_with_empty_value.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        when(conceptController.getConceptByName("BODY PART")).thenReturn(aConcept);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        assertThat(observations.size(), is(0));
//    }
//
//    @Test
//    public void shouldParseNonPreciseNumericObservation() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/numeric_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        when(conceptController.getConceptByName("WEIGHT (KG)")).thenReturn(aConcept);
//        when(aConcept.isNumeric()).thenReturn(true);
//        when(aConcept.isPrecise()).thenReturn(false);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        Observation observation = observations.get(0);
//        assertThat(observation.getValueNumeric().intValue(), is(42));
//    }
//
//    @Test
//    public void shouldParsePreciseNumericObservationToTwoDecimalPlaces() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/numeric_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        when(conceptController.getConceptByName("BODY SURFACE AREA")).thenReturn(aConcept);
//        when(aConcept.isNumeric()).thenReturn(true);
//        when(aConcept.isPrecise()).thenReturn(false);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        Observation observation = observations.get(1);
//        assertThat(observation.getValueNumeric(), is(1.45));
//    }
//
//
//
//    @Test
//    public void shouldBuildDummyConceptForObservationOfTypeConcept() throws IOException, XmlPullParserException,
//            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/value_concept_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Concept aConcept = mock(Concept.class);
//        when(conceptController.getConceptByName("BODY PART")).thenReturn(aConcept);
//        String observedConceptName = "CERVIX";
//        when(conceptController.getConceptByName(observedConceptName)).thenReturn(null);
//        when(aConcept.isCoded()).thenReturn(true);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        Observation observation = observations.get(0);
//        Concept actuallyObservedConcept = observation.getValueCoded();
//        assertThat(actuallyObservedConcept.getName(), is(observedConceptName));
//        assertThat(actuallyObservedConcept.getConceptType() != null, is(true));
//    }
//
//    @Test
//    public void shouldPrefixCreatedObservationsUuidWithCustomPrefix() throws IOException, XmlPullParserException,
//            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        assertThat(observations.get(0).getUuid(), containsString("observationFromPhoneUuid"));
//    }
//
//    @Test
//    public void shouldAssociateCorrectEncounterForObservation() throws IOException, XmlPullParserException,
//            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException {
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        assertThat(observations.get(0).getEncounter().getEncounterDatetime(), is(DateUtils.parse("2014-02-01")));
//    }
//
//    @Test
//    public void shouldSetAssociateEncounterTimeAsObservationDateTime() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException {
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        assertThat(observations.get(0).getObservationDatetime(), is(DateUtils.parse("2014-02-01")));
//    }
//
//    @Test
//    public void shouldAssociateEncountersToDummyProvider() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException {
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        Person provider = observations.get(0).getEncounter().getProvider();
//        assertThat(provider.getUuid(), is("providerForObservationsCreatedOnPhone"));
//        assertThat(provider.getGender(), is("NA"));
//        assertThat(provider.getFamilyName(), is("Taken"));
//        assertThat(provider.getGivenName(), is(" on"));
//        assertThat(provider.getMiddleName(), is("phone"));
//    }
//
//    @Test
//    public void shouldAssociateEncountersToDummyLocation() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException{
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        Location location = observations.get(0).getEncounter().getLocation();
//        assertThat(location.getUuid(), is("locationForObservationsCreatedOnPhone"));
//        assertThat(location.getName(), is("Created On Phone"));
//    }
//
//    @Test
//    public void shouldAssociateEncountersToDummyEncounterType() throws ConceptController.ConceptFetchException,
//            XmlPullParserException, PatientController.PatientLoadException, ParseException, IOException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException {
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        EncounterType encounterType = observations.get(0).getEncounter().getEncounterType();
//        assertThat(encounterType.getUuid(), is("encounterTypeForObservationsCreatedOnPhone"));
//        assertThat(encounterType.getName(), is("encounterTypeForObservationsCreatedOnPhone"));
//    }
//
//    @Test
//    public void shouldSaveAssociateCorrectEncounterForObservation() throws IOException, XmlPullParserException,
//            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            EncounterController.SaveEncounterException, ConceptController.ConceptSaveException,
//            ConceptController.ConceptParseException, ObservationController.ParseObservationException{
//        String xml = readFile("xml/one_date_observation.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        final Encounter encounter = observations.get(0).getEncounter();
//        assertThat(encounter.getEncounterDatetime(), is(DateUtils.parse("2014-02-01")));
//        verify(encounterController).saveEncounter(encounter);
//    }
//
//    @Test
//    public void shouldSaveCreatedObservation() throws IOException, XmlPullParserException, ParseException,
//            ObservationController.SaveObservationException, PatientController.PatientLoadException,
//            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException,
//            ConceptController.ConceptParseException, ObservationController.ParseObservationException{
//        String xml = readFile("xml/histo_xml_payload.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        verify(observationController).saveObservations(observations);
//    }
//
//    @Test
//    public void shouldAssociateCorrectPatient() throws IOException, XmlPullParserException, ParseException,
//            PatientController.PatientLoadException, ConceptController.ConceptFetchException,
//            ConceptController.ConceptSaveException, ConceptController.ConceptParseException,
//            ObservationController.ParseObservationException {
//        String xml = readFile("xml/histo_xml_payload.xml");
//        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
//        Patient patient = new Patient();
//        String patientUuid = "dd7963a8-1691-11df-97a5-7038c432aabf";
//        when(patientController.getPatientByUuid(patientUuid)).thenReturn(patient);
//
//        List<Observation> observations = formParser.parseAndSaveObservations(xml,formDataUuid);
//        verify(patientController).getPatientByUuid(patientUuid);
//        assertThat(observations.get(0).getEncounter().getPatient(), is(patient));
//        assertThat(observations.get(0).getPerson(), is((Person)patient));
//    }
//
//
//    private String readFile(String fileName) {
//        InputStream fileStream = getClass().getClassLoader().getResourceAsStream(fileName);
//        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
//        return s.hasNext() ? s.next() : "{}";
//    }
}

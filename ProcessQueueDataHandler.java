//Process the request to Edited Obs
package org.openmrs.module.muzimaregistration.handler;

import com1.jayway.jsonpath.JsonPath;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.muzima.exception.QueueProcessorException;
import org.openmrs.module.muzima.model.QueueData;
import org.openmrs.module.muzima.model.handler.QueueDataHandler;
import org.openmrs.module.muzimaregistration.api.RegistrationDataService;
import org.openmrs.module.muzimaregistration.api.model.RegistrationData;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 */
@Component
@Handler(supports = QueueData.class, order = 50)
public class ProcessQueueDataHandler implements QueueDataHandler {

    private static final String DISCRIMINATOR_VALUE = "encounter";

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Log log = LogFactory.getLog(EncounterQueueDataHandler.class);
	
	private static String Provider;
	private static String Location;
	private static String FormName;
	public void process(final QueueData queueData) throws QueueProcessorException {
        log.info("Processing registration form data: " + queueData.getUuid());
        String payload = queueData.getPayload();

        RegistrationDataService registrationDataService = Context.getService(RegistrationDataService.class);

        String temporaryUuid = JsonPath.read(payload, "$['patient.uuid']");
        RegistrationData registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
        if (registrationData == null) {
            Patient unsavedPatient = new Patient();

            PatientService patientService = Context.getPatientService();
            LocationService locationService = Context.getLocationService();

            String identifier = JsonPath.read(payload, "$['patient.identifier']");
            String identifierTypeUuid = JsonPath.read(payload, "$['patient.identifier_type']");
            String locationUuid = JsonPath.read(payload, "$['patient.identifier_location']");

            PatientIdentifier patientIdentifier = new PatientIdentifier();
            patientIdentifier.setLocation(locationService.getLocationByUuid(locationUuid));
            patientIdentifier.setIdentifierType(patientService.getPatientIdentifierTypeByUuid(identifierTypeUuid));
            patientIdentifier.setIdentifier(identifier);
            unsavedPatient.addIdentifier(patientIdentifier);

            String birthdate = JsonPath.read(payload, "$['patient.birthdate']");
            String birthdateEstimated = JsonPath.read(payload, "$['patient.birthdate_estimated']");
            String gender = JsonPath.read(payload, "$['patient.gender']");

            unsavedPatient.setBirthdate(parseDate(birthdate));
            unsavedPatient.setBirthdateEstimated(Boolean.parseBoolean(birthdateEstimated));
            unsavedPatient.setGender(gender);

            String givenName = JsonPath.read(payload, "$['patient.given_name']");
            String middleName = JsonPath.read(payload, "$['patient.middle_name']");
            String familyName = JsonPath.read(payload, "$['patient.family_name']");

            PersonName personName = new PersonName();
            personName.setGivenName(givenName);
            personName.setMiddleName(middleName);
            personName.setFamilyName(familyName);

+            unsavedPatient.addName(personName);

            String address1 = JsonPath.read(payload, "$['person_address.address1']");
            String address2 = JsonPath.read(payload, "$['person_address.address2']");

            PersonAddress personAddress = new PersonAddress();
            personAddress.setAddress1(address1);
            personAddress.setAddress2(address2);
            unsavedPatient.addAddress(personAddress);

            Patient savedPatient;
            // check whether we already have similar patients!
            if (!StringUtils.isBlank(identifier)) {
                List<Patient> patients = patientService.getPatients(identifier);
                savedPatient = findPatient(patients, unsavedPatient);
            } else {
                List<Patient> patients = patientService.getPatients(personName.getFullName());
                savedPatient = findPatient(patients, unsavedPatient);
            }

            registrationData = new RegistrationData();
            registrationData.setTemporaryUuid(temporaryUuid);
            // for a new patient we will create mapping:
            // * temporary uuid --> temporary uuid
            // for existing patient we will create mapping:
            // * temporary uuid --> existing uuid
            // we can't find registration data for this uuid, process the registration form.
            String assignedUuid = temporaryUuid;
            if (savedPatient != null) {
                // if we have a patient already saved with the characteristic found in the registration form:
                // * we will map the temporary uuid to the existing uuid.
                assignedUuid = savedPatient.getUuid();
            }
            registrationData.setAssignedUuid(assignedUuid);
            registrationDataService.saveRegistrationData(registrationData);

            if (savedPatient == null) {
                unsavedPatient.setUuid(assignedUuid);
                Context.getPatientService().savePatient(unsavedPatient);
            }
        }
    }

    private Patient findPatient(final List<Patient> patients, final Patient unsavedPatient) {
        for (Patient patient : patients) {
            PatientIdentifier savedIdentifier = patient.getPatientIdentifier();
            PatientIdentifier unsavedIdentifier = unsavedPatient.getPatientIdentifier();
            if (StringUtils.isNotBlank(savedIdentifier.getIdentifier())
                    && StringUtils.isNotBlank(unsavedIdentifier.getIdentifier())) {
                int editDistance = StringUtils.getLevenshteinDistance(
                        StringUtils.lowerCase(savedIdentifier.getIdentifier()),
                        StringUtils.lowerCase(unsavedIdentifier.getIdentifier()));
                // exact match on the patient identifier, they are the same patient.
                if (editDistance == 0) {
                    return patient;
                }
            }
            // match it using the person name and gender, what about the dob?
            PersonName savedPersonName = patient.getPersonName();
            PersonName unsavedPersonName = unsavedPatient.getPersonName();
            if (StringUtils.isNotBlank(savedPersonName.getFullName())
                    && StringUtils.isNotBlank(unsavedPersonName.getFullName())) {
                if (StringUtils.equalsIgnoreCase(patient.getGender(), unsavedPatient.getGender())) {
                    String savedGivenName = savedPersonName.getGivenName();
                    String unsavedGivenName = unsavedPersonName.getGivenName();
                    int givenNameEditDistance = StringUtils.getLevenshteinDistance(
                            StringUtils.lowerCase(savedGivenName),
                            StringUtils.lowerCase(unsavedGivenName));
                    String savedFamilyName = savedPersonName.getFamilyName();
                    String unsavedFamilyName = unsavedPersonName.getFamilyName();
                    int familyNameEditDistance = StringUtils.getLevenshteinDistance(
                            StringUtils.lowerCase(savedFamilyName),
                            StringUtils.lowerCase(unsavedFamilyName));
                    if (givenNameEditDistance < 3 && familyNameEditDistance < 3) {
                        return patient;
                    }
                }
            }
        }
        return null;
    }
	public static String POST(String url,obsObject Obs,Date date){
	InputStream inputStream= null;
	String ObsResultString="";
	try{
	HttpClient httpclient=new DefaultHttpClient();
	HttpPost httpost=new HttpPost();
	String json="";
	JSONObject jsonobject=new JSONObject();
	jsonobject.accumulate("client",obs.getName());
	jsonobject.acumulate("weight",obs.getCountry());
	jsonobject.accumulate("height",obs.get());
	json=jsonObject.toString();
	StringEntity stringentity=new StringEntity(json);
	httPost.setEntity(stringEntity);
	httpPost.setHeader("accept","application/json");
	httpPost.setHeader("Content-type","application/json");
	HttpResponse httpResponse=httpClient.execute(httpPost);
	inputStream=httpResponse.getEntity().getContent();
	if(inputStream!=null){
	
	if(date.equalsTo(httpResponse.getEntity().getContent().get("date"))){
	return  "No Changes Made On Client data";
	}else if(!date.equalsTo(httpResponse.getEntity().getContent().get("date"))) {
	ObsResultString obsResultString = convertInputStreamToString(inputStream);
	}

	}
	else{
	ObsResultString="";
	Setting
	}
	}catch(Exception e){
		List<ErroroDate> errorMessage = Arrays.asList(
			e.getMessage();
		);
		Set<ErrorMessages> errorCodes = Arrays.asList(
			e.getMessage();
		); 
		String uSerDefinedMessage = "Could not Load Values";
		Settings.addErrorMessage(errorMessage,errorCodes,uSerDefinedMessage);
	}
	}

    private Date parseDate(final String dateValue) {
        Date date = null;
        try {
            date = dateFormat.parse(dateValue);
     
	 } catch (ParseException e) {
            log.error("Unable to parse date data for dataProcessing!", e);
        }
        return date;
    }

    /**
     * Flag whether the current queue data handler can handle the queue data.
     *
     * @param queueData the queue data.
     * @return true when the handler can handle the queue data.
     */
    @Override
    public boolean accept(final QueueData queueData) {
        return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
    }
}

	

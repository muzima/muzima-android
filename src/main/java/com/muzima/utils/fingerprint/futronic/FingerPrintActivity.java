package com.muzima.utils.fingerprint.futronic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.futronictech.SDKHelper.FTR_PROGRESS;
import com.futronictech.SDKHelper.FtrIdentifyRecord;
import com.futronictech.SDKHelper.FtrIdentifyResult;
import com.futronictech.SDKHelper.FutronicEnrollment;
import com.futronictech.SDKHelper.FutronicException;
import com.futronictech.SDKHelper.FutronicIdentification;
import com.futronictech.SDKHelper.FutronicSdkBase;
import com.futronictech.SDKHelper.FutronicVerification;
import com.futronictech.SDKHelper.IEnrollmentCallBack;
import com.futronictech.SDKHelper.IIdentificationCallBack;
import com.futronictech.SDKHelper.IVerificationCallBack;
import com.futronictech.SDKHelper.UsbDeviceDataExchangeImpl;
import com.futronictech.SDKHelper.VersionCompatible;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAttribute;
import com.muzima.controller.PatientController;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.List;
import java.util.Vector;

public class FingerPrintActivity
        extends Activity
        implements IEnrollmentCallBack, IVerificationCallBack, IIdentificationCallBack {
    public static final int SHOW_MESSAGE = 1;
    public static final int SHOW_IMAGE = 2;
    public static final int ENROLL_FINGER = 3;
    public static final int ENABLE_CONTROLS = 4;
    public static final String FINGERPRINT_DATA = "fingerprintData";
    private static final int OPERATION_ENROLL = 1;
    private static final int OPERATION_IDENTIFY = 2;
    private static final int OPERATION_VERIFY = 3;
    private static final String TAG = "PatientsLocalSearchAdapter";
    private static Bitmap bitmap = null;
    private static String attributeName = "fingerprint";
    private List<Patient> patients;
    private byte[] fingerBytes;
    private Button mButtonExit;
    private TextView textView;
    private ImageView imageView;
    private Bundle extras;
    private FutronicSdkBase scanningOperation;
    private Object scanningOperationObject;
    private int mPendingOperation = 0;
    private UsbDeviceDataExchangeImpl usbDeviceDataExchange = null;
    private Button toggleButton;
    private Vector<PatientFingerPrints> patientsFingerprintsRecords;
    private PatientFingerPrints patientRecord;
    private int currentViewAction;
    private boolean processCompletedState;
    private Patient foundPatient;
    private String fingerPrint;
    private boolean stopProcessOn = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_MESSAGE:
                    String showMsg = (String) msg.obj;
                    textView.setText(showMsg);
                    break;
                case SHOW_IMAGE:
                    imageView.setImageBitmap(bitmap);
                    break;
                case ENROLL_FINGER:
                    startEnrollment();
                    break;
                case ENABLE_CONTROLS:
                    enableControls(true);
                    break;
                case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE: {
                    if (usbDeviceDataExchange.ValidateContext()) {
                        switch (mPendingOperation) {
                            case OPERATION_ENROLL:
                                startEnrollment();
                                break;
                            case OPERATION_IDENTIFY:
                                loadAllFingerprints();
                                break;
                            case OPERATION_VERIFY:
                                startVerification();
                                break;
                        }
                    } else {
                        showMessageDialog("Can't open scanner device");
                    }
                    break;
                }
                case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE: {
                    showMessageDialog("User deny scanner device");
                    break;
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        extras = getIntent().getExtras();
        toggleButton = (Button) findViewById(R.id.proceed);
        mButtonExit = (Button) findViewById(R.id.buttonExit);
        processCompletedState = false;
        if (extras.getInt("action") == 0) {
            toggleButton.setText("Scan Finger");
            toggleButton.setTag(0);
            currentViewAction = 0;
        } else if (extras.getInt("action") == 1) {
            toggleButton.setText("Verify Patient");
            toggleButton.setTag(1);
            currentViewAction = 1;
        } else if (extras.getInt("action") == 2) {
            toggleButton.setText("Identify Patient");
            toggleButton.setTag(2);
            currentViewAction = 2;
        }
        toggleButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                switch (currentViewAction) {
                    case 0:
                        if (processCompletedState) {
                            Intent i = new Intent();
                            i.putExtra(FINGERPRINT_DATA, fingerPrint);
                            setResult(RESULT_OK, i);
                            finish();
                        } else if(!processCompletedState)
                            toggleButtonFunction();
                        else
                            showMessageDialog("No Fingerprint captured, please scan first");
                        break;
                    case 1:
                        if (!processCompletedState)
                            showMessageDialog("No verification completed, please tap verify first");
                        break;
                    case 2:
                        if (processCompletedState && foundPatient != null) {
                            Intent intent = new Intent(FingerPrintActivity.this, PatientSummaryActivity.class);
                            intent.putExtra(PatientSummaryActivity.PATIENT, foundPatient);
                            startActivity(intent);
                        }else if (!processCompletedState)
                            toggleButtonFunction();
                        else
                            showMessageDialog("No identification completed, please tap identify first");
                        break;
                }
            }
        });
        textView = (TextView) findViewById(R.id.txtMessage);
        imageView = (ImageView) findViewById(R.id.imageFinger);
        usbDeviceDataExchange = new UsbDeviceDataExchangeImpl(this, mHandler);
        mButtonExit.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                exitActivity();
            }
        });
    }

    public void startPatientEnrollment() {
        try {
            if (usbDeviceDataExchange.OpenDevice(0, true)) {
                // send message to start enrollment
                mHandler.obtainMessage(ENROLL_FINGER).sendToTarget();
            } else {
                if (usbDeviceDataExchange.IsPendingOpen()) {
                    mPendingOperation = OPERATION_ENROLL;
                } else {
                    showMessageDialog("Cannot start enrollment operation.\n" +
                            "Scanner device is not connected, please connect");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPatientVerification() {
        try {
            if (usbDeviceDataExchange.OpenDevice(0, true)) {
                startVerification();
            } else {
                if (usbDeviceDataExchange.IsPendingOpen()) {
                    mPendingOperation = OPERATION_VERIFY;
                } else {
                    showMessageDialog("Cannot start enrollment operation.\n" +
                            "Can't open scanner device");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPatientIdentification() {
        try {
            if (usbDeviceDataExchange.OpenDevice(0, true)) {
                loadAllFingerprints();
            } else {
                if (usbDeviceDataExchange.IsPendingOpen()) {
                    mPendingOperation = OPERATION_IDENTIFY;
                } else {
                    showMessageDialog("Cannot start enrollment operation.\n" +
                            "Can't open scanner device");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * The "Put your finger on the scanner" event.
     *
     * @param Progress the current progress data structure.
     */
    public void OnPutOn(FTR_PROGRESS Progress) {
        mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, "Put finger into device, please ...").sendToTarget();
    }

    /**
     * The "Take off your finger from the scanner" event.
     *
     * @param Progress the current progress data structure.
     */
    public void OnTakeOff(FTR_PROGRESS Progress) {
        mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, "Take off finger from device, please ...").sendToTarget();
    }

    /**
     * The "Show the current fingerprint image" event.
     *
     * @param Image the instance of Bitmap class with fingerprint image.
     */
    public void UpdateScreenImage(Bitmap Image) {
        bitmap = Image;
        mHandler.obtainMessage(SHOW_IMAGE).sendToTarget();
    }

    /**
     * The "Fake finger detected" event.
     *
     * @param Progress the fingerprint image.
     * @return <code>true</code> if the current indetntification operation
     *         should be aborted, otherwise is <code>false</code>
     */
    public boolean OnFakeSource(FTR_PROGRESS Progress) {
        mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, "Fake source detected").sendToTarget();
        return false;
        // if want to cancel, return true
    }

    public void setFingerPrintData(String encodeBytes) {
        fingerPrint = encodeBytes;
    }
    // //////////////////////////////////////////////////////////////////
    // ICallBack interface implementation
    // //////////////////////////////////////////////////////////////////

    /**
     * The "Enrollment operation complete" event.
     *
     * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
     *                 <code>false</code>.
     * @param nResult  Futronic SDK return code (see FTRAPI.h).
     */
    public void OnEnrollmentComplete(boolean bSuccess, int nResult) {
        if (bSuccess) {
            processCompletedState = true;
            setFingerPrintData(Base64.encodeToString(((FutronicEnrollment) scanningOperation).getTemplate(), Base64.DEFAULT));
            mHandler.obtainMessage(
                    SHOW_MESSAGE,
                    -1,
                    -1,
                    "Finger print data captured successfully. Quality: " + ((FutronicEnrollment) scanningOperation).getQuality()).sendToTarget();
        } else {
            mHandler.obtainMessage(SHOW_MESSAGE, -1, -1,
                    "Enrollment failed. Error description: " + FutronicSdkBase.SdkRetCode2Message(nResult))
                    .sendToTarget();
        }
        scanningOperation = null;
        scanningOperationObject = null;
        usbDeviceDataExchange.CloseDevice();
        mPendingOperation = 0;
        mHandler.obtainMessage(ENABLE_CONTROLS).sendToTarget();
    }

    /**
     * The "Verification operation complete" event.
     *
     * @param bSuccess             <code>true</code> if the operation succeeds, otherwise is
     *                             <code>false</code>
     * @param nResult              the Futronic SDK return code.
     * @param bVerificationSuccess if the operation succeeds (bSuccess is
     *                             <code>true</code>), this parameters shows the verification
     *                             operation result. <code>true</code> if the captured from the
     *                             attached scanner template is matched, otherwise is
     *                             <code>false</code>.
     */
    public void OnVerificationComplete(boolean bSuccess, int nResult, boolean bVerificationSuccess) {
        StringBuffer stringBuffer = new StringBuffer();
        if (bSuccess) {
            if (bVerificationSuccess) {
                stringBuffer.append("Verification is successful.");
                stringBuffer.append("Patient Name: ");
                processCompletedState = true;
            } else
                stringBuffer.append("Verification failed.");
        } else {
            stringBuffer.append("Verification failed.");
            stringBuffer.append("Error description: ");
            stringBuffer.append(FutronicSdkBase.SdkRetCode2Message(nResult));
        }
        mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, stringBuffer.toString()).sendToTarget();
        scanningOperation = null;
        scanningOperationObject = null;
        usbDeviceDataExchange.CloseDevice();
        mPendingOperation = 0;
        mHandler.obtainMessage(ENABLE_CONTROLS).sendToTarget();
    }

    /**
     * The "Get base template operation complete" event.
     *
     * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
     *                 <code>false</code>.
     * @param nResult  The Futronic SDK return code.
     */
    public void OnGetBaseTemplateComplete(boolean bSuccess, int nResult) {
        StringBuffer stringBuffer = new StringBuffer();
        if (bSuccess) {
            mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, "Starting identification...").sendToTarget();
            Vector<PatientFingerPrints> fingerPrintsVector = patientsFingerprintsRecords;
            FtrIdentifyRecord[] rgRecords = new FtrIdentifyRecord[fingerPrintsVector.size()];
            for (int iPatients = 0; iPatients < fingerPrintsVector.size(); iPatients++)
                rgRecords[iPatients] = fingerPrintsVector.get(iPatients).getFtrIdentifyRecord();
            FtrIdentifyResult result = new FtrIdentifyResult();
            nResult = ((FutronicIdentification) scanningOperation).Identification(rgRecords, result);
            if (nResult == FutronicSdkBase.RETCODE_OK) {
                stringBuffer.append("Identification complete : ");
                if (result.m_Index != -1) {
                    processCompletedState = true;
                    PatientFingerPrints patientsFingerprints = patientsFingerprintsRecords.get(result.m_Index);
                    foundPatient = patientsFingerprints.getPatient();
                    stringBuffer.append(foundPatient.getDisplayName());
                } else {
                    stringBuffer.append("No Match found");
                }
            } else {
                stringBuffer.append("Identification failed.");
                stringBuffer.append(FutronicSdkBase.SdkRetCode2Message(nResult));
            }
            mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, "Fingerprint Captured").sendToTarget();
        } else {
            stringBuffer.append("Cannot retrieve base template.");
            stringBuffer.append("Error description: ");
            stringBuffer.append(FutronicSdkBase.SdkRetCode2Message(nResult));
        }
        mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, stringBuffer.toString()).sendToTarget();
        scanningOperation = null;
        scanningOperationObject = null;
        usbDeviceDataExchange.CloseDevice();
        mPendingOperation = 0;
        mHandler.obtainMessage(ENABLE_CONTROLS).sendToTarget();
    }

    // enrollment - start enrollment
    private void startEnrollment() {
        try {
            if (!usbDeviceDataExchange.ValidateContext()) {
                throw new Exception("Can't open USB device");
            }
            // createFile( szFingerName );
            scanningOperationObject = new PatientFingerPrints();
            scanningOperation = new FutronicEnrollment((Object) usbDeviceDataExchange);
            // Set control properties
            scanningOperation.setFakeDetection(false);
            scanningOperation.setFFDControl(true);
            scanningOperation.setFARN(345);
            ((FutronicEnrollment) scanningOperation).setMIOTControlOff(false);
            ((FutronicEnrollment) scanningOperation).setMaxModels(3);
            scanningOperation.setVersion(VersionCompatible.ftr_version_compatible);
            enableControls(false);
            // start enrollment process
            ((FutronicEnrollment) scanningOperation).Enrollment(this);
        } catch (Exception e) {
            showMessageDialog("Cannot start enrollment operation.\n" +
                    "Error description:");
            scanningOperation = null;
            scanningOperationObject = null;
            usbDeviceDataExchange.CloseDevice();
        }
    }

    // verify - start verification with selected user
    private void startVerification() {
        try {
            fingerBytes = Base64.decode("", Base64.DEFAULT); // extras.getString("fingerString").getBytes("UTF-8");
            if (!usbDeviceDataExchange.ValidateContext()) {
                throw new Exception("Can't open USB device");
            }
            // Toast.makeText(FingerPrintActivity.this, "1<<",
            // Toast.LENGTH_SHORT).show();
            scanningOperation = new FutronicVerification(fingerBytes, usbDeviceDataExchange);
            // Toast.makeText(FingerPrintActivity.this, "2<<",
            // Toast.LENGTH_SHORT).show();
            // Set control properties
            scanningOperation.setFakeDetection(false);
            scanningOperation.setFFDControl(true);
            scanningOperation.setFARN(3);
            scanningOperation.setVersion(VersionCompatible.ftr_version_compatible);
            enableControls(false);
            // start verification process
            ((FutronicVerification) scanningOperation).Verification(this);
        } catch (Exception e) {
            usbDeviceDataExchange.CloseDevice();
            mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, e.getMessage()).sendToTarget();
        }
    }

    /*
     * Start identify
     */
    private void startIdentify() {
        scanningOperationObject = patientsFingerprintsRecords;
        try {
            if (!usbDeviceDataExchange.ValidateContext()) {
                throw new Exception("Can't open USB device");
            }
            scanningOperation = new FutronicIdentification(usbDeviceDataExchange);
            // Set control properties
            scanningOperation.setFakeDetection(false);
            scanningOperation.setFFDControl(true);
            scanningOperation.setFARN(3);
            scanningOperation.setVersion(VersionCompatible.ftr_version_compatible);
            enableControls(false);
            // start verification process
            ((FutronicIdentification) scanningOperation).GetBaseTemplate(this);
        } catch (FutronicException e) {
            showMessageDialog("Cannot start identification operation.\nError description: ");
            usbDeviceDataExchange.CloseDevice();
            scanningOperation = null;
            scanningOperationObject = null;
        } catch (Exception e) {
            usbDeviceDataExchange.CloseDevice();
            mHandler.obtainMessage(SHOW_MESSAGE, -1, -1, e.getMessage()).sendToTarget();
        }
    }

    /*
     * Stop button pressed
     */
    private void stopOperation() {
        if (scanningOperation != null)
            scanningOperation.OnCancel();
    }

    /*
     * Exit button pressed
     */
    private void exitActivity() {
        if (scanningOperation != null) {
            scanningOperation.Dispose();
        }
        finish();
    }

    /*
     * ENABLE_CONTROLS
     */
    private void enableControls(boolean bEnable) {
        mButtonExit.setEnabled(bEnable);
        if (bEnable) {
            if (processCompletedState) {
                toggleButton.setText("Continue");
                stopProcessOn = false;
            } else {
                toggleButtonState();
                stopProcessOn = false;
            }
        } else {
            toggleButton.setText("Stop Process");
            stopProcessOn = true;
        }
    }

    protected void toggleButtonFunction() {
        if (stopProcessOn) {
            stopOperation();
            stopProcessOn = false;
        } else {
            switch (Integer.parseInt("" + toggleButton.getTag())) {
                case 0:
                    startPatientEnrollment();
                    break;
                case 1:
                    startPatientVerification();
                    break;
                case 2:
                    startPatientIdentification();
                    break;
            }
        }
    }

    protected void toggleButtonState() {
        switch (Integer.parseInt("" + toggleButton.getTag())) {
            case 0:
                toggleButton.setText("Scan Finger");
                break;
            case 1:
                toggleButton.setText("Verify Patient");
                break;
            case 2:
                toggleButton.setText("Identify Patient");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (scanningOperation != null) {
            scanningOperation.Dispose();
        }
        super.onDestroy();
    }

    private void loadAllFingerprints() {
        patientsFingerprintsRecords = new Vector<PatientFingerPrints>();
        PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
        try {
            patients = patientController.getAllPatients();
            for (Patient patient : patients) {
                List<PersonAttribute> personAttribute = patient.getAtributes();
                for (PersonAttribute personAttribute1 : personAttribute) {
                    if (personAttribute1.getAttributeType().getName().equalsIgnoreCase(attributeName)) {
                        patientRecord = new PatientFingerPrints();
                        patientRecord.setTemplate(Base64.decode(personAttribute1.getValue(), Base64.DEFAULT));
                        patientRecord.setPatient(patient);
                        patientsFingerprintsRecords.add(patientRecord);
                    }
                }
            }
        } catch (PatientController.PatientLoadException e) {
            Log.w(TAG, "Exception occurred while fetching patients", e);
        }
        if (patientsFingerprintsRecords.isEmpty()) {
            showMessageDialog("Sorry, No patients with fingerprint.");
        } else {
            startIdentify();
        }
    }

    private void showMessageDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(FingerPrintActivity.this);
        builder
                .setCancelable(true)
                .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                .setTitle("Information")
                .setMessage("" + msg)
                .setNegativeButton("Ok", null).create().show();
    }
}

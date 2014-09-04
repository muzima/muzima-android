package com.muzima.utils.fingerprint.futronic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.futronictech.SDKHelper.*;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAttribute;
import com.muzima.controller.PatientController;
import com.muzima.view.patients.PatientSummaryActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;


public class FingerprintIntent
    extends Activity
    implements IEnrollmentCallBack, IVerificationCallBack, IIdentificationCallBack
{
    /** Called when the activity is first created. */
    private Button mButtonEnroll;

    private Button mButtonVerify;

    private Button mButtonIdentify;

    private Button mButtonStop;

    private Button mButtonExit;

    private TextView mTxtMessage;

    private ImageView mFingerImage;
    private Bundle extras;
    List<Patient> patients;

    byte[] fingerbyte;

    String m[][];


    public static final int MESSAGE_SHOW_MSG = 1;

    public static final int MESSAGE_SHOW_IMAGE = 2;

    public static final int MESSAGE_ENROLL_FINGER = 3;

    public static final int MESSAGE_ENABLE_CONTROLS = 4;

    // Intent request codes
    private static final int REQUEST_INPUT_USERNAME = 1;

    private static final int REQUEST_SELECT_USERNAME = 2;

    // Pending operations
    private static final int OPERATION_ENROLL = 1;

    private static final int OPERATION_IDENTIFY = 2;

    private static final int OPERATION_VERIFY = 3;

    private static Bitmap mBitmapFP = null;
    private static String ATTRIBUTE_NAME = "fingerprint";


    private static final String TAG = "PatientsLocalSearchAdapter";

    /**
     * A database directory name.
     */
    public static String m_DbDir;

    private String mStrFingerName = null;

    /**
     * Contain reference for current operation object
     */
    private FutronicSdkBase m_Operation;

    /**
     * The type of this parameter is depending from current operation. For
     * enrollment operation this is PatientsFingerprints.
     */
    private Object m_OperationObj;
    private int mPendingOperation = 0;
    private UsbDeviceDataExchangeImpl usb_host_ctx = null;
    SharedPreferences app_preference;
    private Button proceed;
    private String uuidSelected="";
    private Vector<PatientsFingerprints> patientsFingerprintsRecords;
    private PatientsFingerprints patientRecord;
    private int currentViewAction;
    private boolean processCompletedState;

    public static final String FINGERPRINTDATA = "fingerprintData";
    private Patient foundPatient;
    private String fingerPrint;


    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_fingerprint);

        extras = getIntent().getExtras();
        app_preference = PreferenceManager.getDefaultSharedPreferences( this );

        proceed = (Button) findViewById( R.id.proceed );

        mButtonEnroll = (Button) findViewById( R.id.buttonEnroll );
        mButtonVerify = (Button) findViewById( R.id.buttonVerify );
        mButtonIdentify = (Button) findViewById( R.id.buttonIdentify );
        mButtonStop = (Button) findViewById( R.id.buttonStop );
        mButtonExit = (Button) findViewById( R.id.buttonExit );

        processCompletedState=false;



        if ( extras.getInt( "action" ) == 0 )
        {
            mButtonVerify.setVisibility( View.INVISIBLE );
            mButtonIdentify.setVisibility( View.INVISIBLE );
            currentViewAction=0;
        }
        else if ( extras.getInt( "action" ) == 1 )
        {
            mButtonEnroll.setVisibility( View.INVISIBLE );
            mButtonIdentify.setVisibility( View.INVISIBLE );
            currentViewAction=1;
        }

        else if ( extras.getInt( "action" ) == 2 )
        {
            mButtonEnroll.setVisibility( View.INVISIBLE );
            mButtonVerify.setVisibility( View.INVISIBLE );
            currentViewAction=2;
        }




        proceed.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {

            switch ( currentViewAction )
            {
                case 0:
                    if(processCompletedState)
                    {
                        Intent i = new Intent();
                        i.putExtra(FINGERPRINTDATA,  fingerPrint);
                        setResult(RESULT_OK, i);
                        finish();
                    }
                    else
                        showMessageDialog("No Fingerprint captured, please scan first");
                    break;
                case 1:
                    if(processCompletedState)
                {

                }
                else
                showMessageDialog("No verification completed, please tap verify first");

                break;
                case 2:
                    if(processCompletedState && foundPatient !=null)
                    {
                        Intent intent = new Intent(FingerprintIntent.this, PatientSummaryActivity.class);
                        intent.putExtra(PatientSummaryActivity.PATIENT, foundPatient);
                        startActivity(intent);

                    }
                    else
                        showMessageDialog("No identification completed, please tap identify first");

                    break;
            }

            }
        } );

        mTxtMessage = (TextView) findViewById( R.id.txtMessage );
        mFingerImage = (ImageView) findViewById( R.id.imageFinger );


        usb_host_ctx = new UsbDeviceDataExchangeImpl( this, mHandler );

        mButtonEnroll.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {
                try
                {
                    if ( usb_host_ctx.OpenDevice( 0, true ) )
                    {
                        // send message to start enrollment
                        mHandler.obtainMessage( MESSAGE_ENROLL_FINGER ).sendToTarget();
                    }
                    else
                    {
                        if ( usb_host_ctx.IsPendingOpen() )
                        {
                            mPendingOperation = OPERATION_ENROLL;
                        }
                        else
                        {
                            showMessageDialog("Cannot start enrollment operation.\n" +
                                    "Can't open scanner device");
                        }
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        } );

        mButtonVerify.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {
                try
                {
                    if ( usb_host_ctx.OpenDevice( 0, true ) )
                    {
                        StartVerification();
                    }
                    else
                    {
                        if ( usb_host_ctx.IsPendingOpen() )
                        {
                            mPendingOperation = OPERATION_VERIFY;
                        }
                        else
                        {

                            showMessageDialog("Cannot start enrollment operation.\n" +
                                    "Can't open scanner device");
                        }
                    }
                }
                catch ( Exception e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } );

        mButtonIdentify.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {
                try
                {
                    if (usb_host_ctx.OpenDevice( 0, true ) )
                    {
                        LoadAllFingerprints();
                    }
                    else
                    {
                        if ( usb_host_ctx.IsPendingOpen() )
                        {
                            mPendingOperation = OPERATION_IDENTIFY;
                        }
                        else
                        {

                            showMessageDialog("Cannot start enrollment operation.\n" +
                                    "Can't open scanner device");
                        }
                    }
                }
                catch ( Exception e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } );

        mButtonStop.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {
                StopOperation();
            }
        } );

        mButtonExit.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {
                ExitActivity();
            }
        } );
    }

    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage( Message msg )
        {
            switch ( msg.what )
            {
            case MESSAGE_SHOW_MSG:
                String showMsg = (String) msg.obj;
                mTxtMessage.setText( showMsg );
                break;
            case MESSAGE_SHOW_IMAGE:
                mFingerImage.setImageBitmap( mBitmapFP );
                break;
            case MESSAGE_ENROLL_FINGER:
                StartEnrollment();
                break;
            case MESSAGE_ENABLE_CONTROLS:
                EnableControls( true );
                break;

            case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE:
            {
                if ( usb_host_ctx.ValidateContext() )
                {
                    switch ( mPendingOperation )
                    {
                    case OPERATION_ENROLL:
                        StartEnrollment();
                        break;

                    case OPERATION_IDENTIFY:
                        LoadAllFingerprints();
                        break;

                    case OPERATION_VERIFY:
                        StartVerification();
                        break;
                    }
                }
                else
                {
                    showMessageDialog("Can't open scanner device");
                }

                break;
            }

            case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE:
            {
                showMessageDialog("User deny scanner device");
                break;
            }

            }
        }
    };

    static private String GetDatabaseDir()
        throws AppException
    {
        String szDbDir;
        File extStorageDirectory = Environment.getExternalStorageDirectory();
        File Dir = new File( extStorageDirectory, "Android//FtrSdkDb" );
        if ( Dir.exists() )
        {
            if ( !Dir.isDirectory() )
                throw new AppException( "Can not create database directory " + Dir.getAbsolutePath()
                    + ". File with the same name already exist." );
        }
        else
        {
            try
            {
                Dir.mkdirs();
            }
            catch ( SecurityException e )
            {
                throw new AppException( "Can not create database directory " + Dir.getAbsolutePath()
                    + ". Access denied." );
            }
        }
        szDbDir = Dir.getAbsolutePath();
        return szDbDir;
    }

    /**
     * The "Put your finger on the scanner" event.
     * 
     * @param Progress the current progress data structure.
     */
    public void OnPutOn( FTR_PROGRESS Progress )
    {
        mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, "Put finger into device, please ..." ).sendToTarget();
    }

    /**
     * The "Take off your finger from the scanner" event.
     * 
     * @param Progress the current progress data structure.
     */
    public void OnTakeOff( FTR_PROGRESS Progress )
    {
        mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, "Take off finger from device, please ..." ).sendToTarget();
    }

    /**
     * The "Show the current fingerprint image" event.
     * 
     * @param Image the instance of Bitmap class with fingerprint image.
     */
    public void UpdateScreenImage( Bitmap Image )
    {
        mBitmapFP = Image;
        mHandler.obtainMessage( MESSAGE_SHOW_IMAGE ).sendToTarget();
    }

    /**
     * The "Fake finger detected" event.
     * 
     * @param Progress the fingerprint image.
     * 
     * @return <code>true</code> if the current indetntification operation
     *         should be aborted, otherwise is <code>false</code>
     */
    public boolean OnFakeSource( FTR_PROGRESS Progress )
    {
        mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, "Fake source detected" ).sendToTarget();
        return false;
        // if want to cancel, return true
    }

    public void setFingerPrintData( String encodeBytes )
    {

        fingerPrint = encodeBytes;
    }

    // //////////////////////////////////////////////////////////////////
    // ICallBack interface implementation
    // //////////////////////////////////////////////////////////////////

    /**
     * The "Enrollment operation complete" event.
     * 
     * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
     *        <code>false</code>.
     * @param nResult Futronic SDK return code (see FTRAPI.h).
     */
    public void OnEnrollmentComplete( boolean bSuccess, int nResult )
    {
        if ( bSuccess )
        {

            processCompletedState=true ;

            setFingerPrintData(Base64.encodeToString(((FutronicEnrollment) m_Operation).getTemplate(), Base64.DEFAULT));

            mHandler.obtainMessage(
                MESSAGE_SHOW_MSG,
                -1,
                -1,
                "Finger print data captured successfully. Quality: " + ((FutronicEnrollment) m_Operation).getQuality()
                    + "\n Tap Continue to proceed" ).sendToTarget();

        }
        else
        {
            mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1,
                "Enrollment failed. Error description: " + FutronicSdkBase.SdkRetCode2Message( nResult ) )
                .sendToTarget();
        }

        m_Operation = null;
        m_OperationObj = null;

        usb_host_ctx.CloseDevice();
        mPendingOperation = 0;

        mHandler.obtainMessage( MESSAGE_ENABLE_CONTROLS ).sendToTarget();
    }


    /**
     * The "Verification operation complete" event.
     * 
     * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
     *        <code>false</code>
     * @param nResult the Futronic SDK return code.
     * @param bVerificationSuccess if the operation succeeds (bSuccess is
     *        <code>true</code>), this parameters shows the verification
     *        operation result. <code>true</code> if the captured from the
     *        attached scanner template is matched, otherwise is
     *        <code>false</code>.
     */
    public void OnVerificationComplete( boolean bSuccess, int nResult, boolean bVerificationSuccess )
    {
        StringBuffer szResult = new StringBuffer();
        if ( bSuccess )
        {
            if ( bVerificationSuccess )
            {
                szResult.append( "Verification is successful." );
                szResult.append( "Patient Name: " );

                processCompletedState=true ;

            }
            else
                szResult.append( "Verification failed." );
        }
        else
        {
            szResult.append( "Verification failed." );
            szResult.append( "Error description: " );
            szResult.append( FutronicSdkBase.SdkRetCode2Message(nResult) );
        }
        mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, szResult.toString() ).sendToTarget();
        m_Operation = null;
        m_OperationObj = null;

        usb_host_ctx.CloseDevice();
        mPendingOperation = 0;

        mHandler.obtainMessage( MESSAGE_ENABLE_CONTROLS ).sendToTarget();

    }

    /**
     * The "Get base template operation complete" event.
     * 
     * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
     *        <code>false</code>.
     * @param nResult The Futronic SDK return code.
     */
    public void OnGetBaseTemplateComplete( boolean bSuccess, int nResult )
    {
        StringBuffer szMessage = new StringBuffer();
        if ( bSuccess )
        {
            mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, "Starting identification..." ).sendToTarget();

            Vector<PatientsFingerprints> vpatients = patientsFingerprintsRecords;
            FtrIdentifyRecord[] rgRecords = new FtrIdentifyRecord[vpatients.size()];
            for ( int iPatients = 0; iPatients < vpatients.size(); iPatients++ )
                rgRecords[iPatients] = vpatients.get( iPatients ).getFtrIdentifyRecord();
//
            FtrIdentifyResult result = new FtrIdentifyResult();
            nResult = ((FutronicIdentification) m_Operation).Identification( rgRecords, result );

            if ( nResult == FutronicSdkBase.RETCODE_OK )
            {
                szMessage.append( "Identification complete : " );
                if ( result.m_Index != -1 )
                {
                   processCompletedState=true ;
                   PatientsFingerprints patientsFingerprints = patientsFingerprintsRecords.get( result.m_Index );
                    foundPatient=  patientsFingerprints.getPatient();
                    szMessage.append( foundPatient.getDisplayName());
                    szMessage.append(" Tap Continue to Proceed");

                }
                else  {
                    szMessage.append( "No Match found" );
                }
            }
            else
            {
                szMessage.append( "Identification failed." );
                szMessage.append( FutronicSdkBase.SdkRetCode2Message( nResult ) );
            }

            mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, "Fingerprint Captured Tap Continue" ).sendToTarget();

        }
        else
        {
            szMessage.append( "Cannot retrieve base template." );
            szMessage.append( "Error description: " );
            szMessage.append( FutronicSdkBase.SdkRetCode2Message( nResult ) );
        }
        mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, szMessage.toString() ).sendToTarget();
        m_Operation = null;

        m_OperationObj = null;
        usb_host_ctx.CloseDevice();
        mPendingOperation = 0;
        mHandler.obtainMessage( MESSAGE_ENABLE_CONTROLS ).sendToTarget();
    }


    // enrollment - start enrollment
    private void StartEnrollment( )
    {
        try
        {
            if ( !usb_host_ctx.ValidateContext() )
            {
                throw new Exception( "Can't open USB device" );
            }

            // CreateFile( szFingerName );

            m_OperationObj = new PatientsFingerprints();

            m_Operation = new FutronicEnrollment( (Object) usb_host_ctx );
            // Set control properties
            m_Operation.setFakeDetection(false );
            m_Operation.setFFDControl( true );
            m_Operation.setFARN(345 );
            ((FutronicEnrollment) m_Operation).setMIOTControlOff(false);
            ((FutronicEnrollment) m_Operation).setMaxModels( 3);

                m_Operation.setVersion(VersionCompatible.ftr_version_compatible);
            EnableControls( false );
            // start enrollment process
            ((FutronicEnrollment) m_Operation).Enrollment( this );

        }
        catch ( Exception e )
        {
            showMessageDialog("Cannot start enrollment operation.\n" +
                    "Error description:");
            m_Operation = null;
            m_OperationObj = null;
            usb_host_ctx.CloseDevice();
        }
    }


    // verify - start verification with selected user
    private void StartVerification()
    {
        try
        {
            fingerbyte = Base64.decode("",Base64.DEFAULT); // extras.getString("fingerString").getBytes("UTF-8");

            if ( !usb_host_ctx.ValidateContext() )
            {
                throw new Exception( "Can't open USB device" );
            }
            // Toast.makeText(FingerprintIntent.this, "1<<",
            // Toast.LENGTH_SHORT).show();
            m_Operation = new FutronicVerification( fingerbyte, usb_host_ctx );
            // Toast.makeText(FingerprintIntent.this, "2<<",
            // Toast.LENGTH_SHORT).show();
            // Set control properties
            m_Operation.setFakeDetection(false );
            m_Operation.setFFDControl( true );
            m_Operation.setFARN( 3 );

                m_Operation.setVersion( VersionCompatible.ftr_version_compatible );
            EnableControls( false );
            // start verification process
            ((FutronicVerification) m_Operation).Verification( this );
        }
        catch ( Exception e )
        {
            usb_host_ctx.CloseDevice();
            mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, e.getMessage() ).sendToTarget();
        }
    }

    /*
     * Start identify
     */
    private void StartIdentify()
    {

        m_OperationObj = patientsFingerprintsRecords;

        try
        {
            if ( !usb_host_ctx.ValidateContext() )
            {
                throw new Exception( "Can't open USB device" );
            }

            m_Operation = new FutronicIdentification( usb_host_ctx );
            // Set control properties
            m_Operation.setFakeDetection( false );
            m_Operation.setFFDControl( true );
            m_Operation.setFARN(3 );
            m_Operation.setVersion(VersionCompatible.ftr_version_compatible);

            EnableControls( false );
            // start verification process
            ((FutronicIdentification) m_Operation).GetBaseTemplate( this );

        }
        catch ( FutronicException e )
        {
            showMessageDialog("Cannot start identification operation.\nError description: ");
            usb_host_ctx.CloseDevice();
            m_Operation = null;
            m_OperationObj = null;
        }
        catch ( Exception e )
        {
            usb_host_ctx.CloseDevice();
            mHandler.obtainMessage( MESSAGE_SHOW_MSG, -1, -1, e.getMessage() ).sendToTarget();
        }
    }

    /*
     * Stop button pressed
     */
    private void StopOperation()
    {
        if ( m_Operation != null )
            m_Operation.OnCancel();
    }

    /*
     * Exit button pressed
     */
    private void ExitActivity()
    {
        if ( m_Operation != null )
        {
            m_Operation.Dispose();
        }
        finish();
    }

    /*
     * try to create file before enrollment.
     */
    private void CreateFile( String szFileName )
        throws AppException
    {
        File f = new File( m_DbDir, szFileName );
        try
        {
            f.createNewFile();
            f.delete();
        }
        catch ( IOException e )
        {
            throw new AppException( "Can not create file " + szFileName + " in database." );
        }
        catch ( SecurityException e )
        {
            throw new AppException( "Can not create file " + szFileName + " in database. Access denied" );
        }
    }

    /*
     * EnableControls
     */
    private void EnableControls( boolean bEnable )
    {
        mButtonEnroll.setEnabled( bEnable );
        mButtonIdentify.setEnabled( bEnable );
        mButtonVerify.setEnabled( bEnable );
        mButtonExit.setEnabled( bEnable );
        mButtonStop.setEnabled( !bEnable );
    }


    @Override
    protected void onDestroy()
    {
        if ( m_Operation != null )
        {
            m_Operation.Dispose();
        }
        super.onDestroy();
    }

    private void LoadAllFingerprints()
    {
        patientsFingerprintsRecords = new Vector<PatientsFingerprints>();

        PatientController patientController=   ((MuzimaApplication) getApplicationContext()).getPatientController();


        try {

                patients = patientController.getAllPatients();
            for(Patient patient: patients){

                List<PersonAttribute> personAttribute =patient.getAtributes();

                for(PersonAttribute personAttribute1: personAttribute){
                   if(personAttribute1.getAttributeType().getName().equalsIgnoreCase(ATTRIBUTE_NAME))
                    {
                        patientRecord = new PatientsFingerprints();
                        patientRecord.setTemplate( Base64.decode( personAttribute1.getValue(),Base64.DEFAULT ) );
                        patientRecord.setPatient(patient);
                        patientsFingerprintsRecords.add(patientRecord);
                    }

                }

            }

        } catch (PatientController.PatientLoadException e) {
            Log.w(TAG, "Exception occurred while fetching patients", e);
        }



        Log.d(TAG, "patientsFingerprintsRecords--"+patientsFingerprintsRecords.size());

        if ( patientsFingerprintsRecords.isEmpty())
        {
         showMessageDialog("Sorry, No patients with fingerprint.");
        }
        else
        {
           StartIdentify();
        }

    }


    private void showMessageDialog(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(FingerprintIntent.this);
        builder
                .setCancelable(true)
                .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                .setTitle("Information")
                .setMessage("" + msg)
                .setNegativeButton("Ok", noClickListener()).create().show();


    }

    private Dialog.OnClickListener yesClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

//                searchView.setIconified(false);
//                searchView.requestFocus();
            }
        };
    }

    private Dialog.OnClickListener noClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                startActivity(new. Intent(FingerprintIntent.this, RegistrationFormsActivity.class));
            }
        };
    }


}

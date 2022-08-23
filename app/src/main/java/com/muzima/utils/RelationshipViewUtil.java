package com.muzima.utils;

import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Relationship;
import com.muzima.controller.PatientController;
import com.muzima.view.forms.PersonDemographicsUpdateFormsActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.relationship.RelationshipsListActivity;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class RelationshipViewUtil {
    private static Person selectedRelatedPerson;
    private static Patient patients;
    private static Activity callingActivity;
    private static ListView lvwPatientRelationships;
    private static ActionMode actionMode;
    private static MuzimaApplication mApplication;

    public static AdapterView.OnItemClickListener listOnClickListener(Activity activity, MuzimaApplication muzimaApplication, Patient patient, boolean actionModeActive, ListView listView) {
        mApplication = muzimaApplication;
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Relationship relationship = (Relationship) parent.getItemAtPosition(position);
                patients = patient;
                callingActivity = activity;
                lvwPatientRelationships = listView;
                if (actionModeActive) {
                    if (!relationship.getSynced()) {
                        TypedValue typedValue = new TypedValue();
                        Resources.Theme theme = activity.getTheme();
                        theme.resolveAttribute(R.attr.primaryBackgroundColor, typedValue, true);

                        int selectedRelationshipsCount = getSelectedRelationships().size();
                        if (selectedRelationshipsCount == 0 && actionModeActive) {
                            actionMode.finish();
                            view.setBackgroundResource(typedValue.resourceId);
                        } else {
                            if(view.isActivated()){
                                view.setBackgroundResource(R.color.hint_blue_opaque);
                            } else {
                                view.setBackgroundResource(typedValue.resourceId);
                            }
                            actionMode.setTitle(String.valueOf(selectedRelationshipsCount));
                        }
                    } else {
                        Toasty.warning(callingActivity, callingActivity.getApplicationContext().getString(R.string.relationship_delete_fail), Toast.LENGTH_SHORT, true).show();
                        lvwPatientRelationships.setItemChecked(position, false);
                    }
                } else {

                    Patient relatedPerson;
                    try {
                        selectedRelatedPerson = null;
                        if (StringUtils.equals(relationship.getPersonA().getUuid(), patient.getUuid()))
                            relatedPerson = muzimaApplication.getPatientController().getPatientByUuid(relationship.getPersonB().getUuid());
                        else
                            relatedPerson = muzimaApplication.getPatientController().getPatientByUuid(relationship.getPersonA().getUuid());

                        if (relatedPerson != null) {
                            Intent intent = new Intent(callingActivity, PatientSummaryActivity.class);

                            intent.putExtra(PatientSummaryActivity.PATIENT_UUID, relatedPerson.getUuid());
                            callingActivity.startActivity(intent);
                        } else {
                            // We pick the right related person and create them as a patient
                            if (StringUtils.equalsIgnoreCase(patient.getUuid(), relationship.getPersonA().getUuid())) {
                                selectedRelatedPerson = relationship.getPersonB();
                            } else {
                                selectedRelatedPerson = relationship.getPersonA();
                            }
                            selectAction();
                        }
                    } catch (PatientController.PatientLoadException e) {
                        Log.e(getClass().getSimpleName(),"Encountered an exception",e);
                    }
                }
            }
        };
    }

    private static List<Relationship> getSelectedRelationships() {
        List<Relationship> relationships = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = lvwPatientRelationships.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                relationships.add(((Relationship) lvwPatientRelationships.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return relationships;
    }

    private static void createPatientFromRelatedPerson() {
        Intent intent = new Intent(callingActivity, RegistrationFormsActivity.class);
        Patient pat = new Patient();
        pat.setUuid(selectedRelatedPerson.getUuid());
        pat.setBirthdate(selectedRelatedPerson.getBirthdate());
        pat.setBirthdateEstimated(selectedRelatedPerson.getBirthdateEstimated());
        pat.setGender(selectedRelatedPerson.getGender());
        pat.setNames(selectedRelatedPerson.getNames());

        intent.putExtra(PatientSummaryActivity.PATIENT, pat);
        intent.putExtra(INDEX_PATIENT, patients);
        callingActivity.startActivity(intent);
    }
//
    private static void OpenUpdatePersonDemographicsForm() {
        Intent intent = new Intent(callingActivity, PersonDemographicsUpdateFormsActivity.class);
        intent.putExtra(PersonDemographicsUpdateFormsActivity.PERSON, selectedRelatedPerson);
        intent.putExtra(INDEX_PATIENT, patients);
        callingActivity.startActivity(intent);
    }

    private static void selectAction(){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(callingActivity);
        builderSingle.setIcon(R.drawable.ic_accept);
        builderSingle.setTitle(R.string.hint_person_action_prompt);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(callingActivity, android.R.layout.simple_selectable_list_item);

        if(mApplication.getMuzimaSettingController().isPatientRegistrationEnabled()) {
            arrayAdapter.add(callingActivity.getString(R.string.info_convert_person_to_patient));
        }
        arrayAdapter.add(callingActivity.getString(R.string.info_update_person_demographics));

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                if(callingActivity.getString(R.string.info_convert_person_to_patient).equals(strName)){
                    showAlertDialog();
                } else {
                    OpenUpdatePersonDemographicsForm();
                }
            }
        });
        builderSingle.show();
    }

    private static void showAlertDialog() {
        new AlertDialog.Builder(callingActivity)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(callingActivity))
                .setTitle(callingActivity.getResources().getString(R.string.title_logout_confirm))
                .setMessage(callingActivity.getResources().getString(R.string.confirm_create_patient_from_person))
                .setPositiveButton(callingActivity.getString(R.string.general_yes), positiveClickListener())
                .setNegativeButton(callingActivity.getString(R.string.general_no), null)
                .create()
                .show();
    }

    private static Dialog.OnClickListener positiveClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createPatientFromRelatedPerson();
            }
        };
    }
}

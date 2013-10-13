package com.muzima.view.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.cohort.SettingsBaseAdapter;
import com.muzima.view.cohort.CohortPrefixFragment;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class CohortPrefActivity extends FragmentActivity implements SettingsBaseAdapter.PreferenceClickListener {

    protected CohortPrefixFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_pref_settings);

        fragment = (CohortPrefixFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fragment.reloadData();
    }

    public void addPrefix(View view){
        String newPrefix = fragment.getPrefixText();
        SharedPreferences cohortSharedPref = getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
        Set<String> originalPrefixesSet = cohortSharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>());
        Set<String> copiedPrefixesSet = new TreeSet<String>(new CaseInsensitiveComparator());
        copiedPrefixesSet.addAll(originalPrefixesSet);

        if(validPrefix(copiedPrefixesSet, newPrefix)){
            copiedPrefixesSet.add(newPrefix);
            SharedPreferences.Editor editor = cohortSharedPref.edit();
            editor.putStringSet(COHORT_PREFIX_PREF_KEY, copiedPrefixesSet);
            editor.commit();
        }else{
            Toast.makeText(this, "Prefix already exists", Toast.LENGTH_SHORT).show();
        }

        fragment.updateView();
    }

    @Override
    public void onDeletePreferenceClick(String pref) {
        SharedPreferences cohortSharedPref = getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
        Set<String> prefixes = new HashSet<String>(cohortSharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>()));
        prefixes.remove(pref);

        SharedPreferences.Editor editor = cohortSharedPref.edit();
        editor.putStringSet(COHORT_PREFIX_PREF_KEY, prefixes);
        editor.commit();

        fragment.reloadData();
    }

    @Override
    public void onChangePreferenceClick(String pref) {
        //save prefix
        //notify list
    }

    private boolean validPrefix(Set<String> prefixes, String newPrefix) {
        return !prefixes.contains(newPrefix);
    }

    private static class CaseInsensitiveComparator implements Comparator<String>{

        @Override
        public int compare(String lhs, String rhs) {
            return lhs.toLowerCase().compareTo(rhs.toLowerCase());
        }
    }

}

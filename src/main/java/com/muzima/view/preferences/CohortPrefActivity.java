package com.muzima.view.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPrefixPrefAdapter;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class CohortPrefActivity extends SherlockActivity implements CohortPrefixPrefAdapter.PrefixClickListener{

    private CohortPrefixPrefAdapter prefAdapter;
    private EditText addPrefixEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_pref);

        ListView cohortPrefList = (ListView) findViewById(R.id.cohort_pref_list);
        prefAdapter = new CohortPrefixPrefAdapter(this, R.layout.item_cohorts_list_pref);
        prefAdapter.setPrefixClickListener(this);
        cohortPrefList.setEmptyView(findViewById(R.id.no_data_msg));
        cohortPrefList.setAdapter(prefAdapter);

        addPrefixEditText = (EditText) findViewById(R.id.prefix_edit_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefAdapter.reloadData();
    }

    public void addPrefix(View view){
        String newPrefix = addPrefixEditText.getText().toString();
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

        prefAdapter.reloadData();
        addPrefixEditText.setText("");
    }

    @Override
    public void onDeletePrefixClick(String prefix) {
        SharedPreferences cohortSharedPref = getSharedPreferences(COHORT_PREFIX_PREF, MODE_PRIVATE);
        Set<String> prefixes = new HashSet<String>(cohortSharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>()));
        prefixes.remove(prefix);

        SharedPreferences.Editor editor = cohortSharedPref.edit();
        editor.putStringSet(COHORT_PREFIX_PREF_KEY, prefixes);
        editor.commit();

        prefAdapter.reloadData();
    }

    @Override
    public void onChangePrefixClick(String prefix) {
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

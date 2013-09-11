package com.muzima.view.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.muzima.R;
import com.muzima.adapters.cohort.FormTagPrefAdapter;
import com.muzima.adapters.cohort.SettingsBaseAdapter;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static com.muzima.utils.Constants.FORM_TAG_PREF;
import static com.muzima.utils.Constants.FORM_TAG_PREF_KEY;

public class FormTagActivity extends SherlockActivity implements SettingsBaseAdapter.PreferenceClickListener {

    private FormTagPrefAdapter prefAdapter;
    private EditText addTagEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_tags);

        ListView tagsList = (ListView) findViewById(R.id.tags_list);
        prefAdapter = new FormTagPrefAdapter(this, R.layout.item_preference);
        prefAdapter.setPreferenceClickListener(this);
        tagsList.setEmptyView(findViewById(R.id.no_data_msg));
        tagsList.setAdapter(prefAdapter);

        addTagEditText = (EditText) findViewById(R.id.tag_edit_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefAdapter.reloadData();
    }

    public void addTag(View view){
        String newTag = addTagEditText.getText().toString();
        SharedPreferences cohortSharedPref = getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
        Set<String> originalTags = cohortSharedPref.getStringSet(FORM_TAG_PREF_KEY, new HashSet<String>());
        Set<String> copiedTagsSet = new TreeSet<String>(new CaseInsensitiveComparator());
        copiedTagsSet.addAll(originalTags);

        if(validPrefix(copiedTagsSet, newTag)){
            copiedTagsSet.add(newTag);
            SharedPreferences.Editor editor = cohortSharedPref.edit();
            editor.putStringSet(FORM_TAG_PREF_KEY, copiedTagsSet);
            editor.commit();
        }else{
            Toast.makeText(this, "Tag already exists", Toast.LENGTH_SHORT).show();
        }

        prefAdapter.reloadData();
        addTagEditText.setText("");
    }

    @Override
    public void onDeletePreferenceClick(String tag) {
        SharedPreferences sharedPreferences = getSharedPreferences(FORM_TAG_PREF, MODE_PRIVATE);
        Set<String> tags = new HashSet<String>(sharedPreferences.getStringSet(FORM_TAG_PREF_KEY, new HashSet<String>()));
        tags.remove(tag);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(FORM_TAG_PREF_KEY, tags);
        editor.commit();

        prefAdapter.reloadData();
    }

    @Override
    public void onChangePreferenceClick(String tag) {
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

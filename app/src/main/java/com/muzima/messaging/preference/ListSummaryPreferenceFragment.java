package com.muzima.messaging.preference;

import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import com.muzima.R;
import com.muzima.messaging.fragments.CorrectedPreferenceFragment;

import java.util.Arrays;

public abstract class ListSummaryPreferenceFragment extends CorrectedPreferenceFragment {

    protected class ListSummaryListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            ListPreference listPref = (ListPreference) preference;
            int entryIndex = Arrays.asList(listPref.getEntryValues()).indexOf(value);

            listPref.setSummary(entryIndex >= 0 && entryIndex < listPref.getEntries().length
                    ? listPref.getEntries()[entryIndex]
                    : getString(R.string.general_unknown));
            return true;
        }
    }

    protected void initializeListSummary(ListPreference pref) {
        pref.setSummary(pref.getEntry());
    }
}

package com.muzima.utils;

import java.util.Set;

public class PreAndroidHoneycomb {

    public static class SharedPreferences{

        public static final int START_INDEX = 1;

        public static Set<String> getStringSet(String key, Set<String> defValues, android.content.SharedPreferences cohortSharedPref) {
            int index = START_INDEX;
            String cohortPrefix = cohortSharedPref.getString(key + index, null);
            while (cohortPrefix != null){
                defValues.add(cohortPrefix);
                index++;
                cohortPrefix = cohortSharedPref.getString(key + index, null);
            }
            return defValues;
        }

        public static void putStringSet(String key, Set<String> copiedPrefixesSet, android.content.SharedPreferences.Editor editor) {
            int index = START_INDEX;
            for(String aCohortPrefix: copiedPrefixesSet) {
                editor.putString(key + index, aCohortPrefix);
                index ++;
            }
        }
    }
}

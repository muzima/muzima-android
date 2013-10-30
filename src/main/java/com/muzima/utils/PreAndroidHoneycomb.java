package com.muzima.utils;

import java.util.Set;

public class PreAndroidHoneycomb {

    public static class SharedPreferences{
        public static Set<String> getStringSet(String key, Set<String> defValues, android.content.SharedPreferences cohortSharedPref) {
            int index = 1;
            String cohortPrefix = cohortSharedPref.getString(key + index, null);
            while (cohortPrefix != null){
                defValues.add(cohortPrefix);
                index++;
                cohortPrefix = cohortSharedPref.getString(key + index, null);
            }
            return defValues;
        }
    }
}

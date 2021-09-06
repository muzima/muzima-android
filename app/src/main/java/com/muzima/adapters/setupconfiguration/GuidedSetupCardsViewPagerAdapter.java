package com.muzima.adapters.setupconfiguration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.muzima.R;
import com.muzima.view.fragments.GuidedSetupImageCardFragment;

public class GuidedSetupCardsViewPagerAdapter extends FragmentStatePagerAdapter {
    private final Context context;

    public GuidedSetupCardsViewPagerAdapter(@NonNull FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position > 13 && position <= 27) {
            position = position - 14;
        } else if (position > 27 && position <= 41) {
            position = position - 28;
        } else if (position > 41 && position <= 55) {
            position = position - 42;
        }
        switch (position) {
            case 0:
                return new GuidedSetupImageCardFragment(R.drawable.security, context.getResources().getString(R.string.general_security), context.getResources().getString(R.string.general_security_description));
            case 1:
                return new GuidedSetupImageCardFragment(R.drawable.multiple_use_cases_icon, context.getResources().getString(R.string.general_multiple_use_cases), context.getResources().getString(R.string.general_multiple_cases_description));
            case 2:
                return new GuidedSetupImageCardFragment(R.drawable.openmrs_logo, context.getResources().getString(R.string.general_openmrs_compatibility), context.getResources().getString(R.string.general_openmrs_compatibility_description));
            case 3:
                return new GuidedSetupImageCardFragment(R.drawable.data_collection_tools, context.getResources().getString(R.string.general_data_collection_tools), context.getResources().getString(R.string.general_data_collection_tools_description));
            case 4:
                return new GuidedSetupImageCardFragment(R.drawable.works_offline, context.getResources().getString(R.string.general_works_offline), context.getResources().getString(R.string.general_works_offline_description));
            case 5:
                return new GuidedSetupImageCardFragment(R.drawable.error_resolution, context.getResources().getString(R.string.general_error_resolutions), context.getResources().getString(R.string.general_error_resolutions_description));
            case 6:
                return new GuidedSetupImageCardFragment(R.drawable.form_management, context.getResources().getString(R.string.general_form_management), context.getResources().getString(R.string.general_form_management_description));
            case 7:
                return new GuidedSetupImageCardFragment(R.drawable.cohort_management, context.getResources().getString(R.string.general_cohort_management), context.getResources().getString(R.string.general_cohort_management_description));
            case 8:
                return new GuidedSetupImageCardFragment(R.drawable.multiple_languages, context.getResources().getString(R.string.general_multiple_languages), context.getResources().getString(R.string.general_multiple_languages_description));
            case 9:
                return new GuidedSetupImageCardFragment(R.drawable.historical_data_view, context.getResources().getString(R.string.general_historical_data_view), context.getResources().getString(R.string.general_historical_data_view_description));
            case 10:
                return new GuidedSetupImageCardFragment(R.drawable.multiple_themes, context.getResources().getString(R.string.general_multiple_themes), context.getResources().getString(R.string.general_multiple_themes_description));
            case 11:
                return new GuidedSetupImageCardFragment(R.drawable.relationship_management , context.getResources().getString(R.string.general_relationship_management), context.getResources().getString(R.string.general_relationship_management_description));
            case 12:
                return new GuidedSetupImageCardFragment(R.drawable.clinical_summary , context.getResources().getString(R.string.general_clinical_summary), context.getResources().getString(R.string.general_clinical_summary_description));
            case 13:
                return new GuidedSetupImageCardFragment(R.drawable.geomapping , context.getResources().getString(R.string.general_geomapping), context.getResources().getString(R.string.general_geomapping_description));
        }
        return null;
    }

    @Override
    public int getCount() {
        return 55;
    }
}

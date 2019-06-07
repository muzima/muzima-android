package com.muzima.messaging.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.preference.Preference;

import com.muzima.R;
import com.muzima.messaging.ConversationListActivity;
import com.muzima.messaging.CreateProfileActivity;
import com.muzima.messaging.PassphraseRequiredActionBarActivity;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.fragments.CorrectedPreferenceFragment;
import com.muzima.service.KeyCachingService;
import com.muzima.utils.ThemeUtils;

public class ApplicationPreferencesActivity extends PassphraseRequiredActionBarActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @SuppressWarnings("unused")
    private static final String TAG = ApplicationPreferencesActivity.class.getSimpleName();

    private static final String PREFERENCE_CATEGORY_PROFILE = "preference_category_profile";
    private static final String PREFERENCE_CATEGORY_SMS_MMS = "preference_category_sms_mms";
    private static final String PREFERENCE_CATEGORY_NOTIFICATIONS = "preference_category_notifications";
    private static final String PREFERENCE_CATEGORY_APPEARANCE = "preference_category_appearance";
    private static final String PREFERENCE_CATEGORY_CHATS = "preference_category_chats";
    private static final String PREFERENCE_CATEGORY_DEVICES = "preference_category_devices";
    private static final String PREFERENCE_CATEGORY_ADVANCED = "preference_category_advanced";
    private ThemeUtils themeUtils = new ThemeUtils();

    @Override
    protected void onPreCreate() {
        themeUtils.onCreate(this);
    }


    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
        //noinspection ConstantConditions
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null && getIntent().getCategories() != null && getIntent().getCategories().contains("android.intent.category.NOTIFICATION_PREFERENCES")) {
            initFragment(android.R.id.content, new NotificationsPreferenceFragment());
        } else if (icicle == null) {
            initFragment(android.R.id.content, new ApplicationPreferenceFragment());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        themeUtils.onCreate(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
        fragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            Intent intent = new Intent(this, ConversationListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(TextSecurePreferences.THEME_PREF)) {
            recreate();
        } else if (key.equals(TextSecurePreferences.LANGUAGE_PREF)) {
            recreate();

            Intent intent = new Intent(this, KeyCachingService.class);
            intent.setAction(KeyCachingService.LOCALE_CHANGE_EVENT);
            startService(intent);
        }
    }

    public static class ApplicationPreferenceFragment extends CorrectedPreferenceFragment {

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            this.findPreference(PREFERENCE_CATEGORY_PROFILE)
                    .setOnPreferenceClickListener(new ProfileClickListener());
            this.findPreference(PREFERENCE_CATEGORY_SMS_MMS)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_SMS_MMS));
            this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_NOTIFICATIONS));
            this.findPreference(PREFERENCE_CATEGORY_CHATS)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_CHATS));
            this.findPreference(PREFERENCE_CATEGORY_DEVICES)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_DEVICES));
            this.findPreference(PREFERENCE_CATEGORY_ADVANCED)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_ADVANCED));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                tintIcons(getActivity());
            }
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            //noinspection ConstantConditions
            ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.text_secure_normal__menu_settings);
            setCategorySummaries();
            setCategoryVisibility();
        }

        private void setCategorySummaries() {
            ((ProfilePreference) this.findPreference(PREFERENCE_CATEGORY_PROFILE)).refresh();

            this.findPreference(PREFERENCE_CATEGORY_SMS_MMS)
                    .setSummary(SmsMmsPreferenceFragment.getSummary(getActivity()));
            this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS)
                    .setSummary(NotificationsPreferenceFragment.getSummary(getActivity()));
            this.findPreference(PREFERENCE_CATEGORY_CHATS)
                    .setSummary(ChatsPreferenceFragment.getSummary(getActivity()));
        }

        private void setCategoryVisibility() {
            Preference devicePreference = this.findPreference(PREFERENCE_CATEGORY_DEVICES);
            if (devicePreference != null && !TextSecurePreferences.isPushRegistered(getActivity())) {
                getPreferenceScreen().removePreference(devicePreference);
            }
        }

        @TargetApi(11)
        private void tintIcons(Context context) {
            Drawable sms = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_textsms_white_24dp));
            Drawable notifications = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_notifications_white_24dp));
            Drawable chats = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_forum_white_24dp));
            Drawable devices = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_laptop_white_24dp));
            Drawable advanced = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_advanced_white_24dp));

            int[] tintAttr = new int[]{R.attr.pref_icon_tint};
            TypedArray typedArray = context.obtainStyledAttributes(tintAttr);
            int color = typedArray.getColor(0, 0x0);
            typedArray.recycle();

            DrawableCompat.setTint(sms, color);
            DrawableCompat.setTint(notifications, color);
            DrawableCompat.setTint(chats, color);
            DrawableCompat.setTint(devices, color);
            DrawableCompat.setTint(advanced, color);

            this.findPreference(PREFERENCE_CATEGORY_SMS_MMS).setIcon(sms);
            this.findPreference(PREFERENCE_CATEGORY_NOTIFICATIONS).setIcon(notifications);
            this.findPreference(PREFERENCE_CATEGORY_CHATS).setIcon(chats);
            this.findPreference(PREFERENCE_CATEGORY_DEVICES).setIcon(devices);
            this.findPreference(PREFERENCE_CATEGORY_ADVANCED).setIcon(advanced);
        }

        private class CategoryClickListener implements Preference.OnPreferenceClickListener {
            private String category;

            CategoryClickListener(String category) {
                this.category = category;
            }

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment fragment = null;

                switch (category) {
                    case PREFERENCE_CATEGORY_SMS_MMS:
                        fragment = new SmsMmsPreferenceFragment();
                        break;
                    case PREFERENCE_CATEGORY_NOTIFICATIONS:
                        fragment = new NotificationsPreferenceFragment();
                        break;
                    case PREFERENCE_CATEGORY_CHATS:
                        fragment = new ChatsPreferenceFragment();
                        break;
                    case PREFERENCE_CATEGORY_DEVICES:
                        Intent intent = new Intent(getActivity(), DeviceActivity.class);
                        startActivity(intent);
                        break;
                    case PREFERENCE_CATEGORY_ADVANCED:
                        fragment = new AdvancedPreferenceFragment();
                        break;
                    default:
                        throw new AssertionError();
                }

                if (fragment != null) {
                    Bundle args = new Bundle();
                    fragment.setArguments(args);

                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(android.R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }

                return true;
            }
        }

        private class ProfileClickListener implements Preference.OnPreferenceClickListener {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(preference.getContext(), CreateProfileActivity.class);
                intent.putExtra(CreateProfileActivity.EXCLUDE_SYSTEM, true);

                getActivity().startActivity(intent);
                return true;
            }
        }
    }
}

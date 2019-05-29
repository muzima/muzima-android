package com.muzima.messaging.reminder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.view.View;

import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.utils.Util;
import com.muzima.model.Reminder;

public class DefaultSmsReminder extends Reminder {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public DefaultSmsReminder(final Context context) {
        super(context.getString(R.string.reminder_header_sms_default_title),
                context.getString(R.string.reminder_header_sms_default_text));

        final View.OnClickListener okListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextSecurePreferences.setPromptedDefaultSmsProvider(context, true);
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
                context.startActivity(intent);
            }
        };
        final View.OnClickListener dismissListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextSecurePreferences.setPromptedDefaultSmsProvider(context, true);
            }
        };
        setOkListener(okListener);
        setDismissListener(dismissListener);
    }

    public static boolean isEligible(Context context) {
        final boolean isDefault = Util.isDefaultSmsProvider(context);
        if (isDefault) {
            TextSecurePreferences.setPromptedDefaultSmsProvider(context, false);
        }

        return !isDefault && !TextSecurePreferences.hasPromptedDefaultSmsProvider(context);
    }
}

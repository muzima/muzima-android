package com.muzima.messaging.reminder;

import android.content.Context;
import android.content.Intent;

import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.twofactoraunthentication.RegistrationActivity;
import com.muzima.model.Reminder;

public class UnauthorizedReminder extends Reminder {

    public UnauthorizedReminder(final Context context) {
        super(context.getString(R.string.warning_device_no_longer_registered),
                context.getString(R.string.warning_number_may_have_been_registered_on_another_device));

        setOkListener(v -> {
            Intent intent = new Intent(context, RegistrationActivity.class);
            intent.putExtra(RegistrationActivity.RE_REGISTRATION_EXTRA, true);
            context.startActivity(intent);
        });
    }

    @Override
    public boolean isDismissable() {
        return false;
    }

    public static boolean isEligible(Context context) {
        return TextSecurePreferences.isUnauthorizedRecieved(context);
    }
}

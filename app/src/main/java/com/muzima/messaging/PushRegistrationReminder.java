package com.muzima.messaging;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.muzima.R;
import com.muzima.messaging.twofactoraunthentication.RegistrationActivity;
import com.muzima.model.Reminder;

public class PushRegistrationReminder extends Reminder {

    public PushRegistrationReminder(final Context context) {
        super(context.getString(R.string.title_push),
                context.getString(R.string.hint_push));

        final View.OnClickListener okListener = v -> {
            Intent intent = new Intent(context, RegistrationActivity.class);
            intent.putExtra(RegistrationActivity.RE_REGISTRATION_EXTRA, true);
            context.startActivity(intent);
        };

        setOkListener(okListener);
    }

    @Override
    public boolean isDismissable() {
        return false;
    }

    public static boolean isEligible(Context context) {
        return !TextSecurePreferences.isPushRegistered(context);
    }
}

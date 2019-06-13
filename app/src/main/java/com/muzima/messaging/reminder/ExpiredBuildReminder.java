package com.muzima.messaging.reminder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.messaging.utils.Util;
import com.muzima.model.Reminder;

public class ExpiredBuildReminder extends Reminder {
    @SuppressWarnings("unused")
    private static final String TAG = ExpiredBuildReminder.class.getSimpleName();

    public ExpiredBuildReminder(final Context context) {
        super(context.getString(R.string.warning_expired_build),
                context.getString(R.string.warning_expired_build_details));
        setOkListener(v -> {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
            } catch (android.content.ActivityNotFoundException anfe) {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName())));
                } catch (android.content.ActivityNotFoundException anfe2) {
                    Log.w(TAG, anfe2);
                    Toast.makeText(context, R.string.warning_no_web_browser_installed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean isDismissable() {
        return false;
    }

    public static boolean isEligible() {
        return Util.getDaysTillBuildExpiry() <= 0;
    }
}

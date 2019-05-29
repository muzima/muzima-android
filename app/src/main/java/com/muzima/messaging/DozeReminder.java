package com.muzima.messaging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;

import com.muzima.R;
import com.muzima.model.Reminder;

@SuppressLint("BatteryLife")
public class DozeReminder extends Reminder {

    @RequiresApi(api = Build.VERSION_CODES.M)
    public DozeReminder(@NonNull final Context context) {
        super(context.getString(R.string.DozeReminder_optimize_for_missing_play_services),
                context.getString(R.string.DozeReminder_this_device_does_not_support_play_services_tap_to_disable_system_battery));

        setOkListener(v -> {
            TextSecurePreferences.setPromptedOptimizeDoze(context.getApplicationContext(), true);
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        });

        setDismissListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextSecurePreferences.setPromptedOptimizeDoze(context.getApplicationContext(), true);
            }
        });
    }

    public static boolean isEligible(Context context) {
        return TextSecurePreferences.isGcmDisabled(context) &&
                !TextSecurePreferences.hasPromptedOptimizeDoze(context) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(context.getPackageName());
    }

}


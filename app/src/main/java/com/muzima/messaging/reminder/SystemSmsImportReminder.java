package com.muzima.messaging.reminder;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.muzima.R;
import com.muzima.messaging.ConversationListActivity;
import com.muzima.messaging.DatabaseMigrationActivity;
import com.muzima.model.Reminder;
import com.muzima.service.ApplicationMigrationService;

public class SystemSmsImportReminder extends Reminder {

    public SystemSmsImportReminder(final Context context) {
        super(context.getString(R.string.reminder_header_sms_import_title),
                context.getString(R.string.reminder_header_sms_import_text));

        final View.OnClickListener okListener = v -> {
            Intent intent = new Intent(context, ApplicationMigrationService.class);
            intent.setAction(ApplicationMigrationService.MIGRATE_DATABASE);
            context.startService(intent);

            Intent nextIntent = new Intent(context, ConversationListActivity.class);
            Intent activityIntent = new Intent(context, DatabaseMigrationActivity.class);
            activityIntent.putExtra("next_intent", nextIntent);
            context.startActivity(activityIntent);
        };
        final View.OnClickListener cancelListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApplicationMigrationService.setDatabaseImported(context);
            }
        };
        setOkListener(okListener);
        setDismissListener(cancelListener);
    }

    public static boolean isEligible(Context context) {
        return !ApplicationMigrationService.isDatabaseImported(context);
    }
}

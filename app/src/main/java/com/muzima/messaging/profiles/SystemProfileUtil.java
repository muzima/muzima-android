package com.muzima.messaging.profiles;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.whispersystems.signalservice.internal.util.concurrent.ListenableFuture;
import org.whispersystems.signalservice.internal.util.concurrent.SettableFuture;

public class SystemProfileUtil {
    private static final String TAG = SystemProfileUtil.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    public static ListenableFuture<String> getSystemProfileName(final @NonNull Context context) {
        SettableFuture<String> future = new SettableFuture<>();

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String name = null;

                if (Build.VERSION.SDK_INT >= 14) {
                    try (Cursor cursor =  context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null)) {
                        if (cursor != null && cursor.moveToNext()) {
                            name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Profile.DISPLAY_NAME));
                        }
                    } catch (SecurityException se) {
                        Log.w(TAG, se);
                    }
                }

                if (name == null) {
                    AccountManager accountManager = AccountManager.get(context);
                    Account[]      accounts       = accountManager.getAccountsByType("com.google");

                    for (Account account : accounts) {
                        if (!TextUtils.isEmpty(account.name)) {
                            if (account.name.contains("@")) {
                                name = account.name.substring(0, account.name.indexOf("@")).replace('.', ' ');
                            } else {
                                name = account.name.replace('.', ' ');
                            }

                            break;
                        }
                    }
                }

                return name;
            }

            @Override
            protected void onPostExecute(@Nullable String result) {
                future.set(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return future;
    }
}

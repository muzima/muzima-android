package com.muzima.service;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.muzima.messaging.ShareActivity;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.BitmapUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiresApi(api = Build.VERSION_CODES.M)
public class DirectShareService extends ChooserTargetService {

    private static final String TAG = DirectShareService.class.getSimpleName();

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                   IntentFilter matchedFilter) {
        List<ChooserTarget> results = new LinkedList<>();
        ComponentName componentName = new ComponentName(this, ShareActivity.class);
        ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(this);
        Cursor cursor = threadDatabase.getDirectShareList();

        try {
            ThreadDatabase.Reader reader = threadDatabase.readerFor(cursor);
            ThreadRecord record;

            while ((record = reader.getNext()) != null && results.size() < 10) {
                SignalRecipient recipient = SignalRecipient.from(this, record.getRecipient().getAddress(), false);
                String name = recipient.toShortString();

                Bitmap avatar;

                if (recipient.getContactPhoto() != null) {
                    try {
                        avatar = GlideApp.with(this)
                                .asBitmap()
                                .load(recipient.getContactPhoto())
                                .circleCrop()
                                .submit(getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                        getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width))
                                .get();
                    } catch (InterruptedException | ExecutionException e) {
                        Log.w(TAG, e);
                        avatar = getFallbackDrawable(recipient);
                    }
                } else {
                    avatar = getFallbackDrawable(recipient);
                }

                Parcel parcel = Parcel.obtain();
                parcel.writeParcelable(recipient.getAddress(), 0);

                Bundle bundle = new Bundle();
                bundle.putLong(ShareActivity.EXTRA_THREAD_ID, record.getThreadId());
                bundle.putByteArray(ShareActivity.EXTRA_ADDRESS_MARSHALLED, parcel.marshall());
                bundle.putInt(ShareActivity.EXTRA_DISTRIBUTION_TYPE, record.getDistributionType());
                bundle.setClassLoader(getClassLoader());

                results.add(new ChooserTarget(name, Icon.createWithBitmap(avatar), 1.0f, componentName, bundle));
                parcel.recycle();

            }

            return results;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private Bitmap getFallbackDrawable(@NonNull SignalRecipient recipient) {
        return BitmapUtil.createFromDrawable(recipient.getFallbackContactPhotoDrawable(this, false),
                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
    }
}

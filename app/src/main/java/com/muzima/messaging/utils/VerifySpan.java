package com.muzima.messaging.utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.style.ClickableSpan;
import android.view.View;

import com.muzima.messaging.VerifyIdentityActivity;
import com.muzima.messaging.crypto.IdentityKeyParcelable;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;

import org.whispersystems.libsignal.IdentityKey;

public class VerifySpan extends ClickableSpan {

    private final Context context;
    private final SignalAddress address;
    private final IdentityKey identityKey;

    public VerifySpan(@NonNull Context context, @NonNull IdentityKeyMismatch mismatch) {
        this.context     = context;
        this.address     = mismatch.getAddress();
        this.identityKey = mismatch.getIdentityKey();
    }

    public VerifySpan(@NonNull Context context, @NonNull SignalAddress address, @NonNull IdentityKey identityKey) {
        this.context     = context;
        this.address     = address;
        this.identityKey = identityKey;
    }

    @Override
    public void onClick(View widget) {
        Intent intent = new Intent(context, VerifyIdentityActivity.class);
        intent.putExtra(VerifyIdentityActivity.ADDRESS_EXTRA, address);
        intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(identityKey));
        intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);
        context.startActivity(intent);
    }
}

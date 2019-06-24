package com.muzima.messaging.contacts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.messaging.RecipientModifiedListener;
import com.muzima.messaging.customcomponents.AvatarImageView;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.ContactsDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.ViewUtil;

public class ContactSelectionListItem extends LinearLayout implements RecipientModifiedListener {

    @SuppressWarnings("unused")
    private static final String TAG = ContactSelectionListItem.class.getSimpleName();

    private AvatarImageView contactPhotoImage;
    private TextView numberView;
    private TextView nameView;
    private TextView labelView;
    private CheckBox checkBox;

    private String number;
    private SignalRecipient recipient;
    private GlideRequests glideRequests;

    public ContactSelectionListItem(Context context) {
        super(context);
    }

    public ContactSelectionListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.contactPhotoImage = findViewById(R.id.contact_photo_image);
        this.numberView = findViewById(R.id.number);
        this.labelView = findViewById(R.id.label);
        this.nameView = findViewById(R.id.name);
        this.checkBox = findViewById(R.id.check_box);

        ViewUtil.setTextViewGravityStart(this.nameView, getContext());
    }

    public void set(@NonNull GlideRequests glideRequests, int type, String name, String number, String label, int color, boolean multiSelect) {
        this.glideRequests = glideRequests;
        this.number = number;

        if (type == ContactsDatabase.NEW_TYPE) {
            this.recipient = null;
            this.contactPhotoImage.setAvatar(glideRequests, SignalRecipient.from(getContext(), SignalAddress.UNKNOWN, true), false);
        } else if (!TextUtils.isEmpty(number)) {
            SignalAddress address = SignalAddress.fromExternal(getContext(), number);
            this.recipient = SignalRecipient.from(getContext(), address, true);
            this.recipient.addListener(this);

            if (this.recipient.getName() != null) {
                name = this.recipient.getName();
            }
        }

        this.nameView.setTextColor(color);
        this.numberView.setTextColor(color);
        this.contactPhotoImage.setAvatar(glideRequests, recipient, false);

        setText(type, name, number, label);

        if (multiSelect) this.checkBox.setVisibility(View.VISIBLE);
        else this.checkBox.setVisibility(View.GONE);
    }

    public void setChecked(boolean selected) {
        this.checkBox.setChecked(selected);
    }

    public void unbind(GlideRequests glideRequests) {
        if (recipient != null) {
            recipient.removeListener(this);
            recipient = null;
        }

        contactPhotoImage.clear(glideRequests);
    }

    private void setText(int type, String name, String number, String label) {
        if (number == null || number.isEmpty() || GroupUtil.isEncodedGroup(number)) {
            this.nameView.setEnabled(false);
            this.numberView.setText("");
            this.labelView.setVisibility(View.GONE);
        } else if (type == ContactsDatabase.PUSH_TYPE) {
            this.numberView.setText(number);
            this.nameView.setEnabled(true);
            this.labelView.setVisibility(View.GONE);
        } else {
            this.numberView.setText(number);
            this.nameView.setEnabled(true);
            this.labelView.setText(label);
            this.labelView.setVisibility(View.VISIBLE);
        }

        this.nameView.setText(name);
    }

    public String getNumber() {
        return number;
    }

    @Override
    public void onModified(final SignalRecipient recipient) {
        if (this.recipient == recipient) {
            Util.runOnMain(() -> {
                contactPhotoImage.setAvatar(glideRequests, recipient, false);
                nameView.setText(recipient.toShortString());
            });
        }
    }
}

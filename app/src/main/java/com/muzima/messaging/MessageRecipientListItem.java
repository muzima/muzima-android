package com.muzima.messaging;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.messaging.customcomponents.AvatarImageView;
import com.muzima.messaging.customcomponents.DeliveryStatusView;
import com.muzima.messaging.customcomponents.FromTextView;
import com.muzima.messaging.dialogs.ConfirmIdentityDialog;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;
import com.muzima.messaging.sqlite.database.documents.NetworkFailure;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.MessageDetailsRecipientAdapter.RecipientDeliveryStatus;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;

public class MessageRecipientListItem extends RelativeLayout
        implements RecipientModifiedListener {
    @SuppressWarnings("unused")
    private final static String TAG = MessageRecipientListItem.class.getSimpleName();

    private RecipientDeliveryStatus member;
    private GlideRequests glideRequests;
    private FromTextView fromView;
    private TextView errorDescription;
    private TextView actionDescription;
    private Button conflictButton;
    private AvatarImageView contactPhotoImage;
    private ImageView unidentifiedDeliveryIcon;
    private DeliveryStatusView deliveryStatusView;

    public MessageRecipientListItem(Context context) {
        super(context);
    }

    public MessageRecipientListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.fromView = findViewById(R.id.from);
        this.errorDescription = findViewById(R.id.error_description);
        this.actionDescription = findViewById(R.id.action_description);
        this.contactPhotoImage = findViewById(R.id.contact_photo_image);
        this.conflictButton = findViewById(R.id.conflict_button);
        this.unidentifiedDeliveryIcon = findViewById(R.id.ud_indicator);
        this.deliveryStatusView = findViewById(R.id.delivery_status);
    }

    public void set(final GlideRequests glideRequests,
                    final MessageRecord record,
                    final RecipientDeliveryStatus member,
                    final boolean isPushGroup) {
        this.glideRequests = glideRequests;
        this.member = member;

        member.getRecipient().addListener(this);
        fromView.setText(member.getRecipient());
        contactPhotoImage.setAvatar(glideRequests, member.getRecipient(), false);
        setIssueIndicators(record, isPushGroup);
        unidentifiedDeliveryIcon.setVisibility(TextSecurePreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(getContext()) && member.isUnidentified() ? VISIBLE : GONE);
    }

    private void setIssueIndicators(final MessageRecord record,
                                    final boolean isPushGroup) {
        final NetworkFailure networkFailure = getNetworkFailure(record);
        final IdentityKeyMismatch keyMismatch = networkFailure == null ? getKeyMismatch(record) : null;

        String errorText = "";

        if (keyMismatch != null) {
            conflictButton.setVisibility(View.VISIBLE);

            errorText = getContext().getString(R.string.title_new_safety_number);
            conflictButton.setOnClickListener(v -> new ConfirmIdentityDialog(getContext(), record, keyMismatch).show());
        } else if ((networkFailure != null && !record.isPending()) || (!isPushGroup && record.isFailed())) {
            conflictButton.setVisibility(View.GONE);
            errorText = getContext().getString(R.string.warning_failed_to_send);
        } else {
            if (record.isOutgoing()) {
                if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.PENDING || member.getDeliveryStatus() == RecipientDeliveryStatus.Status.UNKNOWN) {
                    deliveryStatusView.setVisibility(View.GONE);
                } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.READ) {
                    deliveryStatusView.setRead();
                    deliveryStatusView.setVisibility(View.VISIBLE);
                } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.DELIVERED) {
                    deliveryStatusView.setDelivered();
                    deliveryStatusView.setVisibility(View.VISIBLE);
                } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.SENT) {
                    deliveryStatusView.setSent();
                    deliveryStatusView.setVisibility(View.VISIBLE);
                }
            } else {
                deliveryStatusView.setVisibility(View.GONE);
            }

            conflictButton.setVisibility(View.GONE);
        }

        errorDescription.setText(errorText);
        errorDescription.setVisibility(TextUtils.isEmpty(errorText) ? View.GONE : View.VISIBLE);
    }

    private NetworkFailure getNetworkFailure(final MessageRecord record) {
        if (record.hasNetworkFailures()) {
            for (final NetworkFailure failure : record.getNetworkFailures()) {
                if (failure.getAddress().equals(member.getRecipient().getAddress())) {
                    return failure;
                }
            }
        }
        return null;
    }

    private IdentityKeyMismatch getKeyMismatch(final MessageRecord record) {
        if (record.isIdentityMismatchFailure()) {
            for (final IdentityKeyMismatch mismatch : record.getIdentityKeyMismatches()) {
                if (mismatch.getAddress().equals(member.getRecipient().getAddress())) {
                    return mismatch;
                }
            }
        }
        return null;
    }

    public void unbind() {
        if (this.member != null && this.member.getRecipient() != null)
            this.member.getRecipient().removeListener(this);
    }

    @Override
    public void onModified(final SignalRecipient recipient) {
        Util.runOnMain(() -> {
            fromView.setText(recipient);
            contactPhotoImage.setAvatar(glideRequests, recipient, false);
        });
    }
}

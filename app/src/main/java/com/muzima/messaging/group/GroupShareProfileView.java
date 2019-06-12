package com.muzima.messaging.group;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.ViewUtil;

public class GroupShareProfileView extends FrameLayout {

    private View container;
    private @Nullable SignalRecipient recipient;

    public GroupShareProfileView(@NonNull Context context) {
        super(context);
        initialize();
    }

    public GroupShareProfileView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public GroupShareProfileView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GroupShareProfileView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        inflate(getContext(), R.layout.profile_group_share_view, this);

        this.container = ViewUtil.findById(this, R.id.container);
        this.container.setOnClickListener(view -> {
            if (this.recipient != null) {
                new AlertDialog.Builder(getContext())
                        .setIconAttribute(R.attr.dialog_info_icon)
                        .setTitle(R.string.general_share_profile)
                        .setMessage(R.string.general_make_profile_visible)
                        .setPositiveButton(R.string.general_make_visible, (dialog, which) -> {
                            DatabaseFactory.getRecipientDatabase(getContext()).setProfileSharing(recipient, true);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
    }

    public void setRecipient(@NonNull SignalRecipient recipient) {
        this.recipient = recipient;
    }

}

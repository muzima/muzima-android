package com.muzima.messaging.customcomponents.actionmode;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.messaging.BindableConversationListItem;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.utils.ViewUtil;

import java.util.Locale;
import java.util.Set;

public class ConversationListItemAction extends LinearLayout implements BindableConversationListItem {

    private TextView description;

    public ConversationListItemAction(Context context) {
        super(context);
    }

    public ConversationListItemAction(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ConversationListItemAction(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.description = ViewUtil.findById(this, R.id.description);
    }

    @Override
    public void bind(@NonNull ThreadRecord thread,
                     @NonNull GlideRequests glideRequests,
                     @NonNull Locale locale,
                     @NonNull Set<Long> typingThreads,
                     @NonNull Set<Long> selectedThreads,
                     boolean batchMode) {
        this.description.setText(getContext().getString(R.string.ConversationListItemAction_archived_conversations_d, thread.getCount()));
    }

    @Override
    public void unbind() {

    }
}

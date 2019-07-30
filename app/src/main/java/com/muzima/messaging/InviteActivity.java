package com.muzima.messaging;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.messaging.contacts.ContactsCursorLoader.DisplayMode;
import com.muzima.messaging.customcomponents.ContactFilterToolbar;
import com.muzima.messaging.fragments.ContactSelectionListFragment;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sms.OutgoingTextMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.tasks.ProgressDialogAsyncTask;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.ViewUtil;
import com.muzima.utils.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class InviteActivity extends PassphraseRequiredActionBarActivity implements ContactSelectionListFragment.OnContactSelectedListener {

    private ContactSelectionListFragment contactsFragment;
    private EditText inviteText;
    private ViewGroup smsSendFrame;
    private Button smsSendButton;
    private Animation slideInAnimation;
    private Animation slideOutAnimation;
    private ImageView heart;

    @Override
    protected void onCreate(Bundle savedInstanceState, boolean ready) {
        getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_SMS);
        getIntent().putExtra(ContactSelectionListFragment.MULTI_SELECT, true);
        getIntent().putExtra(ContactSelectionListFragment.REFRESHABLE, false);

        setContentView(R.layout.invite_activity);
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(R.string.general_invite_friends);

        initializeResources();
    }

    private void initializeResources() {
        slideInAnimation = loadAnimation(R.anim.slide_from_bottom);
        slideOutAnimation = loadAnimation(R.anim.slide_to_bottom);

        View shareButton = ViewUtil.findById(this, R.id.share_button);
        View smsButton = ViewUtil.findById(this, R.id.sms_button);
        Button smsCancelButton = ViewUtil.findById(this, R.id.cancel_sms_button);
        ContactFilterToolbar contactFilter   = ViewUtil.findById(this, R.id.contact_filter);

        inviteText = ViewUtil.findById(this, R.id.invite_text);
        smsSendFrame = ViewUtil.findById(this, R.id.sms_send_frame);
        smsSendButton = ViewUtil.findById(this, R.id.send_sms_button);
        heart = ViewUtil.findById(this, R.id.heart);
        contactsFragment = (ContactSelectionListFragment)getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);

        inviteText.setText(getString(R.string.general_lets_switch_to_muzima, "https://play.google.com/store/apps/details?id=com.muzima"));
        updateSmsButtonText();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            heart.getViewTreeObserver().addOnPreDrawListener(new HeartPreDrawListener());
        }
        contactsFragment.setOnContactSelectedListener(this);
        shareButton.setOnClickListener(new ShareClickListener());
        smsButton.setOnClickListener(new SmsClickListener());
        smsCancelButton.setOnClickListener(new SmsCancelClickListener());
        smsSendButton.setOnClickListener(new SmsSendClickListener());
        contactFilter.setOnFilterChangedListener(new ContactFilterChangedListener());
        contactFilter.setNavigationIcon(R.drawable.ic_search_white_24dp);
    }

    private Animation loadAnimation(@AnimRes int animResId) {
        final Animation animation = AnimationUtils.loadAnimation(this, animResId);
        animation.setInterpolator(new FastOutSlowInInterpolator());
        return animation;
    }

    @Override
    public void onContactSelected(String number) {
        updateSmsButtonText();
    }

    @Override
    public void onContactDeselected(String number) {
        updateSmsButtonText();
    }

    private void sendSmsInvites() {
        new SendSmsInvitesAsyncTask(this, inviteText.getText().toString())
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        contactsFragment.getSelectedContacts()
                                .toArray(new String[contactsFragment.getSelectedContacts().size()]));
    }

    private void updateSmsButtonText() {
        smsSendButton.setText(getResources().getQuantityString(R.plurals.plurals_send_sms_to_friends,
                contactsFragment.getSelectedContacts().size(),
                contactsFragment.getSelectedContacts().size()));
        smsSendButton.setEnabled(!contactsFragment.getSelectedContacts().isEmpty());
    }

    @Override
    public void onBackPressed() {
        if (smsSendFrame.getVisibility() == View.VISIBLE) {
            cancelSmsSelection();
        } else {
            super.onBackPressed();
        }
    }

    private void cancelSmsSelection() {
        contactsFragment.reset();
        updateSmsButtonText();
        ViewUtil.animateOut(smsSendFrame, slideOutAnimation, View.GONE);
    }

    private class ShareClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, inviteText.getText().toString());
            sendIntent.setType("text/plain");
            if (sendIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(sendIntent, getString(R.string.hint_invite_to_muzima)));
            } else {
                Toast.makeText(InviteActivity.this, R.string.hint_no_app_to_share_to, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SmsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ViewUtil.animateIn(smsSendFrame, slideInAnimation);
        }
    }

    private class SmsCancelClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            cancelSmsSelection();
        }
    }

    private class SmsSendClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(InviteActivity.this)
                    .setTitle(getResources().getQuantityString(R.plurals.plurals_send_sms_invites,
                            contactsFragment.getSelectedContacts().size(),
                            contactsFragment.getSelectedContacts().size()))
                    .setMessage(inviteText.getText().toString())
                    .setPositiveButton(R.string.general_yes, (dialog, which) -> sendSmsInvites())
                    .setNegativeButton(R.string.general_no, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private class ContactFilterChangedListener implements ContactFilterToolbar.OnFilterChangedListener {
        @Override
        public void onFilterChanged(String filter) {
            contactsFragment.setQueryFilter(filter);
        }
    }

    private class HeartPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onPreDraw() {
            heart.getViewTreeObserver().removeOnPreDrawListener(this);
            final int w = heart.getWidth();
            final int h = heart.getHeight();
            Animator reveal = ViewAnimationUtils.createCircularReveal(heart,
                    w / 2, h,
                    0, (float)Math.sqrt(h*h + (w*w/4)));
            reveal.setInterpolator(new FastOutSlowInInterpolator());
            reveal.setDuration(800);
            reveal.start();
            return false;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SendSmsInvitesAsyncTask extends ProgressDialogAsyncTask<String,Void,Void> {
        private final String message;

        SendSmsInvitesAsyncTask(Context context, String message) {
            super(context, R.string.general_sending, R.string.general_sending);
            this.message = message;
        }

        @Override
        protected Void doInBackground(String... numbers) {
            final Context context = getContext();
            if (context == null) return null;
            String phoneNumbers = "";
            for (String number : numbers) {
                if (!phoneNumbers.isEmpty()) {
                    phoneNumbers = phoneNumbers.concat(";").concat(number);
                } else {
                    phoneNumbers = number;
                }
            }

            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + phoneNumbers));
            smsIntent.putExtra("sms_body",message);
            startActivityForResult(smsIntent,100);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            final Context context = getContext();
            if (context == null) return;

            ViewUtil.animateOut(smsSendFrame, slideOutAnimation, View.GONE).addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    contactsFragment.reset();
                }

                @Override
                public void onFailure(ExecutionException e) {}
            });
        }
    }

    @Override
    public void onActivityResult(final int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if ((reqCode == 100) || (resultCode == RESULT_OK && reqCode == 100)) {
            Intent conversationListActivity = new Intent(this, ConversationListActivity.class);
            conversationListActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(conversationListActivity);
        }
    }
}

package com.muzima.messaging;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.animations.DeleteItemAnimator;
import com.muzima.messaging.customcomponents.PulsingFloatingActionButton;
import com.muzima.messaging.customcomponents.ReminderView;
import com.muzima.messaging.customcomponents.actionmode.ConversationListItemAction;
import com.muzima.messaging.customcomponents.actionmode.ConversationListItemInboxZero;
import com.muzima.messaging.events.ReminderUpdateEvent;
import com.muzima.messaging.jobs.ServiceOutageDetectionJob;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.reminder.DefaultSmsReminder;
import com.muzima.messaging.reminder.ExpiredBuildReminder;
import com.muzima.messaging.reminder.OutdatedBuildReminder;
import com.muzima.messaging.reminder.ServiceOutageReminder;
import com.muzima.messaging.reminder.SystemSmsImportReminder;
import com.muzima.messaging.reminder.UnauthorizedReminder;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.MarkedMessageInfo;
import com.muzima.messaging.tasks.ConversationListLoader;
import com.muzima.messaging.utils.Util;
import com.muzima.model.Reminder;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MarkReadReceiver;
import com.muzima.notifications.MessageNotifier;
import com.muzima.tasks.SnackbarAsyncTask;
import com.muzima.utils.ViewUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback, ConversationListAdapter.ItemClickListener {
    public static final String ARCHIVE = "archive";

    @SuppressWarnings("unused")
    private static final String TAG = ConversationListFragment.class.getSimpleName();

    private ActionMode actionMode;
    private RecyclerView list;
    private ReminderView reminderView;
    private View emptyState;
    private TextView emptySearch;
    private PulsingFloatingActionButton fab;
    private Locale locale;
    private String queryFilter = "";
    private boolean archive;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        locale = (Locale) getArguments().getSerializable(PassphraseRequiredActionBarActivity.LOCALE_EXTRA);
        archive = getArguments().getBoolean(ARCHIVE, false);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        final View view = inflater.inflate(R.layout.conversation_list_fragment, container, false);

        reminderView = ViewUtil.findById(view, R.id.reminder);
        list = ViewUtil.findById(view, R.id.list);
        fab = ViewUtil.findById(view, R.id.fab);
        emptyState = ViewUtil.findById(view, R.id.empty_state);
        emptySearch = ViewUtil.findById(view, R.id.empty_search);

        if (archive) fab.setVisibility(View.GONE);
        else fab.setVisibility(View.VISIBLE);

        reminderView.setOnDismissListener(() -> updateReminders(true));

        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setItemAnimator(new DeleteItemAnimator());

        new ItemTouchHelper(new ArchiveListenerCallback()).attachToRecyclerView(list);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        setHasOptionsMenu(true);
        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), NewConversationActivity.class)));
        initializeListAdapter();
        initializeTypingObserver();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateReminders(true);
        list.getAdapter().notifyDataSetChanged();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        fab.stopPulse();
        EventBus.getDefault().unregister(this);
    }

    public ConversationListAdapter getListAdapter() {
        return (ConversationListAdapter) list.getAdapter();
    }

    public void setQueryFilter(String query) {
        this.queryFilter = query;
        getLoaderManager().restartLoader(0, null, this);
    }

    public void resetQueryFilter() {
        if (!TextUtils.isEmpty(this.queryFilter)) {
            setQueryFilter("");
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void updateReminders(boolean hide) {
        new AsyncTask<Context, Void, Optional<? extends Reminder>>() {
            @Override
            protected Optional<? extends Reminder> doInBackground(Context... params) {
                final Context context = params[0];
                if (UnauthorizedReminder.isEligible(context)) {
                    return Optional.of(new UnauthorizedReminder(context));
                } else if (ExpiredBuildReminder.isEligible()) {
                    return Optional.of(new ExpiredBuildReminder(context));
                } else if (ServiceOutageReminder.isEligible(context)) {
                    MuzimaApplication.getInstance(context).getJobManager().add(new ServiceOutageDetectionJob(context));
                    return Optional.of(new ServiceOutageReminder(context));
                } else if (OutdatedBuildReminder.isEligible()) {
                    return Optional.of(new OutdatedBuildReminder(context));
                } else if (DefaultSmsReminder.isEligible(context)) {
                    return Optional.of(new DefaultSmsReminder(context));
                } else if (Util.isDefaultSmsProvider(context) && SystemSmsImportReminder.isEligible(context)) {
                    return Optional.of((new SystemSmsImportReminder(context)));
                } else if (PushRegistrationReminder.isEligible(context)) {
                    return Optional.of((new PushRegistrationReminder(context)));
                } else if (ShareReminder.isEligible(context)) {
                    return Optional.of(new ShareReminder(context));
                } else if (DozeReminder.isEligible(context)) {
                    return Optional.of(new DozeReminder(context));
                } else {
                    return Optional.absent();
                }
            }

            @Override
            protected void onPostExecute(Optional<? extends Reminder> reminder) {
                if (reminder.isPresent() && getActivity() != null && !isRemoving()) {
                    reminderView.showReminder(reminder.get());
                } else if (!reminder.isPresent()) {
                    reminderView.hide();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());
    }

    private void initializeListAdapter() {
        list.setAdapter(new ConversationListAdapter(getActivity(), GlideApp.with(this), locale, null, this));
        getLoaderManager().restartLoader(0, null, this);
    }

    private void initializeTypingObserver() {
        MuzimaApplication.getInstance(requireContext()).getTypingStatusRepository().getTypingThreads().observe(this, threadIds -> {
            if (threadIds == null) {
                threadIds = Collections.emptySet();
            }

            getListAdapter().setTypingThreads(threadIds);
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void handleArchiveAllSelected() {
        final Set<Long> selectedConversations = new HashSet<>(getListAdapter().getBatchSelections());
        final boolean archive = this.archive;

        int snackBarTitleId;

        if (archive)
            snackBarTitleId = R.plurals.ConversationListFragment_moved_conversations_to_inbox;
        else snackBarTitleId = R.plurals.ConversationListFragment_conversations_archived;

        int count = selectedConversations.size();
        String snackBarTitle = getResources().getQuantityString(snackBarTitleId, count, count);

        new SnackbarAsyncTask<Void>(getView(), snackBarTitle,
                getString(R.string.ConversationListFragment_undo),
                getResources().getColor(R.color.amber_500),
                Snackbar.LENGTH_LONG, true) {

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            }

            @Override
            protected void executeAction(@Nullable Void parameter) {
                for (long threadId : selectedConversations) {
                    if (!archive)
                        DatabaseFactory.getThreadDatabase(getActivity()).archiveConversation(threadId);
                    else
                        DatabaseFactory.getThreadDatabase(getActivity()).unarchiveConversation(threadId);
                }
            }

            @Override
            protected void reverseAction(@Nullable Void parameter) {
                for (long threadId : selectedConversations) {
                    if (!archive)
                        DatabaseFactory.getThreadDatabase(getActivity()).unarchiveConversation(threadId);
                    else
                        DatabaseFactory.getThreadDatabase(getActivity()).archiveConversation(threadId);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleDeleteAllSelected() {
        int conversationsCount = getListAdapter().getBatchSelections().size();
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setIconAttribute(R.attr.dialog_alert_icon);
        alert.setTitle(getActivity().getResources().getQuantityString(R.plurals.ConversationListFragment_delete_selected_conversations,
                conversationsCount, conversationsCount));
        alert.setMessage(getActivity().getResources().getQuantityString(R.plurals.ConversationListFragment_this_will_permanently_delete_all_n_selected_conversations,
                conversationsCount, conversationsCount));
        alert.setCancelable(true);

        alert.setPositiveButton(R.string.delete, (dialog, which) -> {
            final Set<Long> selectedConversations = (getListAdapter())
                    .getBatchSelections();

            if (!selectedConversations.isEmpty()) {
                new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog dialog;

                    @Override
                    protected void onPreExecute() {
                        dialog = ProgressDialog.show(getActivity(),
                                getActivity().getString(R.string.ConversationListFragment_deleting),
                                getActivity().getString(R.string.ConversationListFragment_deleting_selected_conversations),
                                true, false);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        DatabaseFactory.getThreadDatabase(getActivity()).deleteConversations(selectedConversations);
                        MessageNotifier.updateNotification(getActivity());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        dialog.dismiss();
                        if (actionMode != null) {
                            actionMode.finish();
                            actionMode = null;
                        }
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }



    private void handleSelectAllThreads() {
        getListAdapter().selectAllThreads();
        actionMode.setTitle(String.valueOf(getListAdapter().getBatchSelections().size()));
    }

    private void handleCreateConversation(long threadId, SignalRecipient recipient, int distributionType, long lastSeen) {
        ((ConversationSelectedListener) getActivity()).onCreateConversation(threadId, recipient, distributionType, lastSeen);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new ConversationListLoader(getActivity(), queryFilter, archive);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        if ((cursor == null || cursor.getCount() <= 0) && TextUtils.isEmpty(queryFilter) && !archive) {
            list.setVisibility(View.INVISIBLE);
            emptyState.setVisibility(View.VISIBLE);
            emptySearch.setVisibility(View.INVISIBLE);
            fab.startPulse(3 * 1000);
        } else if ((cursor == null || cursor.getCount() <= 0) && !TextUtils.isEmpty(queryFilter)) {
            list.setVisibility(View.INVISIBLE);
            emptyState.setVisibility(View.GONE);
            emptySearch.setVisibility(View.VISIBLE);
            emptySearch.setText(getString(R.string.ConversationListFragment_no_results_found_for_s_, queryFilter));
        } else {
            list.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            emptySearch.setVisibility(View.INVISIBLE);
            fab.stopPulse();
        }

        getListAdapter().changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        getListAdapter().changeCursor(null);
    }

    @Override
    public void onItemClick(ConversationListItem item) {
        if (actionMode == null) {
            handleCreateConversation(item.getThreadId(), item.getRecipient(),
                    item.getDistributionType(), item.getLastSeen());
        } else {
            ConversationListAdapter adapter = (ConversationListAdapter) list.getAdapter();
            adapter.toggleThreadInBatchSet(item.getThreadId());

            if (adapter.getBatchSelections().size() == 0) {
                actionMode.finish();
            } else {
                actionMode.setTitle(String.valueOf(getListAdapter().getBatchSelections().size()));
            }

            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemLongClick(ConversationListItem item) {
        actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(ConversationListFragment.this);

        getListAdapter().initializeBatchMode(true);
        getListAdapter().toggleThreadInBatchSet(item.getThreadId());
        getListAdapter().notifyDataSetChanged();
    }

    @Override
    public void onSwitchToArchive() {
        ((ConversationSelectedListener) getActivity()).onSwitchToArchive();
    }

    public interface ConversationSelectedListener {
        void onCreateConversation(long threadId, SignalRecipient recipient, int distributionType, long lastSeen);

        void onSwitchToArchive();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = getActivity().getMenuInflater();

        if (archive) inflater.inflate(R.menu.conversation_list_batch_unarchive, menu);
        else inflater.inflate(R.menu.conversation_list_batch_archive, menu);

        inflater.inflate(R.menu.conversation_list_batch, menu);

        mode.setTitle("1");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
        }

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select_all:
                handleSelectAllThreads();
                return true;
            case R.id.menu_delete_selected:
                handleDeleteAllSelected();
                return true;
            case R.id.menu_archive_selected:
                handleArchiveAllSelected();
                return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        getListAdapter().initializeBatchMode(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedArray color = getActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.statusBarColor});
            getActivity().getWindow().setStatusBarColor(color.getColor(0, Color.BLACK));
            color.recycle();
        }

        actionMode = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ReminderUpdateEvent event) {
        updateReminders(false);
    }

    private class ArchiveListenerCallback extends ItemTouchHelper.SimpleCallback {

        ArchiveListenerCallback() {
            super(0, ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.itemView instanceof ConversationListItemAction) {
                return 0;
            }

            if (actionMode != null) {
                return 0;
            }

            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if (viewHolder.itemView instanceof ConversationListItemInboxZero) return;
            final long threadId = ((ConversationListItem) viewHolder.itemView).getThreadId();
            final int unreadCount = ((ConversationListItem) viewHolder.itemView).getUnreadCount();

            if (archive) {
                new SnackbarAsyncTask<Long>(getView(),
                        getResources().getQuantityString(R.plurals.ConversationListFragment_moved_conversations_to_inbox, 1, 1),
                        getString(R.string.ConversationListFragment_undo),
                        getResources().getColor(R.color.amber_500),
                        Snackbar.LENGTH_LONG, false) {
                    @Override
                    protected void executeAction(@Nullable Long parameter) {
                        DatabaseFactory.getThreadDatabase(getActivity()).unarchiveConversation(threadId);
                    }

                    @Override
                    protected void reverseAction(@Nullable Long parameter) {
                        DatabaseFactory.getThreadDatabase(getActivity()).archiveConversation(threadId);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
            } else {
                new SnackbarAsyncTask<Long>(getView(),
                        getResources().getQuantityString(R.plurals.ConversationListFragment_conversations_archived, 1, 1),
                        getString(R.string.ConversationListFragment_undo),
                        getResources().getColor(R.color.amber_500),
                        Snackbar.LENGTH_LONG, false) {
                    @Override
                    protected void executeAction(@Nullable Long parameter) {
                        DatabaseFactory.getThreadDatabase(getActivity()).archiveConversation(threadId);

                        if (unreadCount > 0) {
                            List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(getActivity()).setRead(threadId, false);
                            MessageNotifier.updateNotification(getActivity());
                            MarkReadReceiver.process(getActivity(), messageIds);
                        }
                    }

                    @Override
                    protected void reverseAction(@Nullable Long parameter) {
                        DatabaseFactory.getThreadDatabase(getActivity()).unarchiveConversation(threadId);

                        if (unreadCount > 0) {
                            DatabaseFactory.getThreadDatabase(getActivity()).incrementUnread(threadId, unreadCount);
                            MessageNotifier.updateNotification(getActivity());
                        }
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
            }
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState,
                                boolean isCurrentlyActive) {
            if (viewHolder.itemView instanceof ConversationListItemInboxZero) return;
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View itemView = viewHolder.itemView;
                Paint p = new Paint();
                float alpha = 1.0f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();

                if (dX > 0) {
                    Bitmap icon;

                    if (archive)
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_unarchive_white_36dp);
                    else
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_archive_white_36dp);

                    if (alpha > 0) p.setColor(getResources().getColor(R.color.green_500));
                    else p.setColor(Color.WHITE);

                    c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                            (float) itemView.getBottom(), p);

                    c.drawBitmap(icon,
                            (float) itemView.getLeft() + getResources().getDimension(R.dimen.conversation_list_fragment_archive_padding),
                            (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight()) / 2,
                            p);
                }

                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);
            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }
    }
}

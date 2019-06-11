package com.muzima.messaging.fragments;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.SearchListAdapter;
import com.muzima.messaging.ConversationActivity;
import com.muzima.messaging.ConversationListActivity;
import com.muzima.messaging.SearchRepository;
import com.muzima.messaging.contacts.ContactAccessor;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.messaging.utils.StickyHeaderDecoration;
import com.muzima.model.MessageResult;
import com.muzima.model.SearchResult;
import com.muzima.model.SearchViewModel;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.ThemeUtils;

import java.util.Locale;
import java.util.concurrent.Executors;

public class SearchFragment extends Fragment implements SearchListAdapter.EventListener{

    public static final String TAG          = "SearchFragment";
    public static final String EXTRA_LOCALE = "locale";

    private TextView noResultsView;
    private RecyclerView listView;
    private StickyHeaderDecoration listDecoration;

    private SearchViewModel viewModel;
    private SearchListAdapter listAdapter;
    private String            pendingQuery;
    private Locale locale;

    public static SearchFragment newInstance(@NonNull Locale locale) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_LOCALE, locale);

        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.locale = (Locale) getArguments().getSerializable(EXTRA_LOCALE);

        SearchRepository searchRepository = new SearchRepository(getContext(),
                DatabaseFactory.getSearchDatabase(getContext()),
                DatabaseFactory.getContactsDatabase(getContext()),
                DatabaseFactory.getThreadDatabase(getContext()),
                ContactAccessor.getInstance(),
                Executors.newSingleThreadExecutor());
        viewModel = ViewModelProviders.of(this, new SearchViewModel.Factory(searchRepository)).get(SearchViewModel.class);

        if (pendingQuery != null) {
            viewModel.updateQuery(pendingQuery);
            pendingQuery = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        noResultsView = view.findViewById(R.id.search_no_results);
        listView      = view.findViewById(R.id.search_list);

        listAdapter    = new SearchListAdapter(GlideApp.with(this), this, locale);
        listDecoration = new StickyHeaderDecoration(listAdapter, false, false);

        listView.setAdapter(listAdapter);
        listView.addItemDecoration(listDecoration);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.getSearchResult().observe(this, result -> {
            result = result != null ? result : SearchResult.EMPTY;

            listAdapter.updateResults(result);
            int backgroundColor = ThemeUtils.getPrimaryThemeColor(getContext());
            noResultsView.setBackgroundColor(backgroundColor);

            if (result.isEmpty()) {
                if (TextUtils.isEmpty(viewModel.getLastQuery().trim())) {
                    noResultsView.setVisibility(View.GONE);
                } else {
                    noResultsView.setVisibility(View.VISIBLE);
                    noResultsView.setText(getString(R.string.SearchFragment_no_results, viewModel.getLastQuery()));
                }
            } else {
                noResultsView.setVisibility(View.VISIBLE);
                noResultsView.setText("");
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (listDecoration != null) {
            listDecoration.invalidateLayouts();
        }
    }


    @Override
    public void onConversationClicked(@NonNull ThreadRecord threadRecord) {
        ConversationListActivity conversationList = (ConversationListActivity) getActivity();

        if (conversationList != null) {
            conversationList.onCreateConversation(threadRecord.getThreadId(),
                    threadRecord.getRecipient(),
                    threadRecord.getDistributionType(),
                    threadRecord.getLastSeen());
        }
    }

    @Override
    public void onContactClicked(@NonNull SignalRecipient contact) {
        Intent intent = new Intent(getContext(), ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, contact.getAddress());

        long existingThread = DatabaseFactory.getThreadDatabase(getContext()).getThreadIdIfExistsFor(contact);

        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        startActivity(intent);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onMessageClicked(@NonNull MessageResult message) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                int  startingPosition = DatabaseFactory.getMmsSmsDatabase(getContext()).getMessagePositionInConversation(message.threadId, message.receivedTimestampMs);
                startingPosition = Math.max(0, startingPosition);

                return startingPosition;
            }

            @Override
            protected void onPostExecute(Integer startingPosition) {
                ConversationListActivity conversationList = (ConversationListActivity) getActivity();
                if (conversationList != null) {
                    conversationList.openConversation(message.threadId,
                            message.recipient,
                            ThreadDatabase.DistributionTypes.DEFAULT,
                            -1,
                            startingPosition);
                }
            }
        }.execute();
    }


    public void updateSearchQuery(@NonNull String query) {
        if (viewModel != null) {
            viewModel.updateQuery(query);
        } else {
            pendingQuery = query;
        }
    }
}
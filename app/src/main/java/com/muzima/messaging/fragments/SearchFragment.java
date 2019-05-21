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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.messaging.ConversationActivity;
import com.muzima.messaging.ConversationListActivity;
import com.muzima.messaging.contacts.ContactAccessor;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.model.SignalRecipient;

import java.util.Locale;
import java.util.concurrent.Executors;

public class SearchFragment extends Fragment {

    public static final String TAG          = "SearchFragment";
    public static final String EXTRA_LOCALE = "locale";

    private TextView noResultsView;
    private RecyclerView listView;
//    private StickyHeaderDecoration listDecoration;
//
//    private SearchViewModel   viewModel;
//    private SearchListAdapter listAdapter;
    private String            pendingQuery;
    private Locale locale;

    public static SearchFragment newInstance(@NonNull Locale locale) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_LOCALE, locale);

        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static SearchFragment newInstance() {
        Bundle args = new Bundle();

        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);

        return fragment;
    }



    public void updateSearchQuery(@NonNull String query) {
//        if (viewModel != null) {
//            viewModel.updateQuery(query);
//        } else {
//            pendingQuery = query;
//        }
    }
}

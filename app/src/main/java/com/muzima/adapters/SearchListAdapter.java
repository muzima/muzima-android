package com.muzima.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.messaging.ConversationListItem;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.messaging.utils.StickyHeaderDecoration;
import com.muzima.model.MessageResult;
import com.muzima.model.SearchResult;
import com.muzima.model.SignalRecipient;

import java.util.Collections;
import java.util.Locale;

public class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.SearchResultViewHolder>
        implements StickyHeaderDecoration.StickyHeaderAdapter<SearchListAdapter.HeaderViewHolder> {
    private static final int TYPE_CONVERSATIONS = 1;
    private static final int TYPE_CONTACTS = 2;
    private static final int TYPE_MESSAGES = 3;

    private final GlideRequests glideRequests;
    private final EventListener eventListener;
    private final Locale locale;

    @NonNull private SearchResult searchResult = SearchResult.EMPTY;

    public SearchListAdapter(@NonNull GlideRequests glideRequests,
                             @NonNull EventListener eventListener,
                             @NonNull Locale locale) {
        this.glideRequests = glideRequests;
        this.eventListener = eventListener;
        this.locale = locale;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SearchResultViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conversation_list_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        ThreadRecord conversationResult = getConversationResult(position);

        if (conversationResult != null) {
            holder.bind(conversationResult, glideRequests, eventListener, locale, searchResult.getQuery());
            return;
        }

        SignalRecipient contactResult = getContactResult(position);

        if (contactResult != null) {
            holder.bind(contactResult, glideRequests, eventListener, locale, searchResult.getQuery());
            return;
        }

        MessageResult messageResult = getMessageResult(position);

        if (messageResult != null) {
            holder.bind(messageResult, glideRequests, eventListener, locale, searchResult.getQuery());
        }
    }

    @Override
    public void onViewRecycled(SearchResultViewHolder holder) {
        holder.recycle();
    }

    @Override
    public int getItemCount() {
        return searchResult.size();
    }

    @Override
    public long getHeaderId(int position) {
        if (getConversationResult(position) != null) {
            return TYPE_CONVERSATIONS;
        } else if (getContactResult(position) != null) {
            return TYPE_CONTACTS;
        } else {
            return TYPE_MESSAGES;
        }
    }

    @Override
    public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        return new HeaderViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_selection_list_divider, parent, false));
    }

    @Override
    public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
        viewHolder.bind((int) getHeaderId(position));
    }

    public void updateResults(@NonNull SearchResult result) {
        this.searchResult = result;
        notifyDataSetChanged();
    }

    @Nullable
    private ThreadRecord getConversationResult(int position) {
        if (position < searchResult.getConversations().size()) {
            return searchResult.getConversations().get(position);
        }
        return null;
    }

    @Nullable
    private SignalRecipient getContactResult(int position) {
        if (position >= getFirstContactIndex() && position < getFirstMessageIndex()) {
            return searchResult.getContacts().get(position - getFirstContactIndex());
        }
        return null;
    }

    @Nullable
    private MessageResult getMessageResult(int position) {
        if (position >= getFirstMessageIndex() && position < searchResult.size()) {
            return searchResult.getMessages().get(position - getFirstMessageIndex());
        }
        return null;
    }

    private int getFirstContactIndex() {
        return searchResult.getConversations().size();
    }

    private int getFirstMessageIndex() {
        return getFirstContactIndex() + searchResult.getContacts().size();
    }

    public interface EventListener {
        void onConversationClicked(@NonNull ThreadRecord threadRecord);

        void onContactClicked(@NonNull SignalRecipient contact);

        void onMessageClicked(@NonNull MessageResult message);
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {

        private final ConversationListItem root;

        SearchResultViewHolder(View itemView) {
            super(itemView);
            root = (ConversationListItem) itemView;
        }

        public void bind(@NonNull ThreadRecord conversationResult,
                         @NonNull GlideRequests glideRequests,
                         @NonNull EventListener eventListener,
                         @NonNull Locale locale,
                         @Nullable String query) {

            root.bind(conversationResult, glideRequests, locale, Collections.emptySet(), Collections.emptySet(), false, query);
            root.setOnClickListener(view -> eventListener.onConversationClicked(conversationResult));
        }

        public void bind(@NonNull SignalRecipient contactResult,
                         @NonNull GlideRequests glideRequests,
                         @NonNull EventListener eventListener,
                         @NonNull Locale locale,
                         @Nullable String query) {
            root.bind(contactResult, glideRequests, locale, query);
            root.setOnClickListener(view -> eventListener.onContactClicked(contactResult));
        }

        public void bind(@NonNull MessageResult messageResult,
                         @NonNull GlideRequests glideRequests,
                         @NonNull EventListener eventListener,
                         @NonNull Locale locale,
                         @Nullable String query) {
            root.bind(messageResult, glideRequests, locale, query);
            root.setOnClickListener(view -> eventListener.onMessageClicked(messageResult));
        }

        public void recycle() {
            root.unbind();
            root.setOnClickListener(null);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView titleView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.label);
        }

        public void bind(int headerType) {
            switch (headerType) {
                case TYPE_CONVERSATIONS:
                    titleView.setText(R.string.SearchFragment_header_conversations);
                    break;
                case TYPE_CONTACTS:
                    titleView.setText(R.string.SearchFragment_header_contacts);
                    break;
                case TYPE_MESSAGES:
                    titleView.setText(R.string.SearchFragment_header_messages);
                    break;
            }
        }
    }
}

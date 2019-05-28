package com.muzima.messaging;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MergeCursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.annimon.stream.Stream;
import com.muzima.messaging.contacts.ContactAccessor;
import com.muzima.messaging.sqlite.database.ContactsDatabase;
import com.muzima.messaging.sqlite.database.CursorList;
import com.muzima.messaging.sqlite.database.MmsSmsColumns;
import com.muzima.messaging.sqlite.database.SearchDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.model.MessageResult;
import com.muzima.model.SearchResult;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.Permissions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class SearchRepository {
    private static final Set<Character> BANNED_CHARACTERS = new HashSet<>();

    static {
        // Several ranges of invalid ASCII characters
        for (int i = 33; i <= 47; i++) {
            BANNED_CHARACTERS.add((char) i);
        }
        for (int i = 58; i <= 64; i++) {
            BANNED_CHARACTERS.add((char) i);
        }
        for (int i = 91; i <= 96; i++) {
            BANNED_CHARACTERS.add((char) i);
        }
        for (int i = 123; i <= 126; i++) {
            BANNED_CHARACTERS.add((char) i);
        }
    }

    private final Context context;
    private final SearchDatabase searchDatabase;
    private final ContactsDatabase contactsDatabase;
    private final ThreadDatabase threadDatabase;
    private final ContactAccessor contactAccessor;
    private final Executor executor;

    public SearchRepository(@NonNull Context context,
                            @NonNull SearchDatabase searchDatabase,
                            @NonNull ContactsDatabase contactsDatabase,
                            @NonNull ThreadDatabase threadDatabase,
                            @NonNull ContactAccessor contactAccessor,
                            @NonNull Executor executor) {
        this.context = context.getApplicationContext();
        this.searchDatabase = searchDatabase;
        this.contactsDatabase = contactsDatabase;
        this.threadDatabase = threadDatabase;
        this.contactAccessor = contactAccessor;
        this.executor = executor;
    }

    public void query(@NonNull String query, @NonNull Callback callback) {
        if (TextUtils.isEmpty(query)) {
            callback.onResult(SearchResult.EMPTY);
            return;
        }

        executor.execute(() -> {
            String cleanQuery = sanitizeQuery(query);
            CursorList<SignalRecipient> contacts = queryContacts(cleanQuery);
            CursorList<ThreadRecord> conversations = queryConversations(cleanQuery);
            CursorList<MessageResult> messages = queryMessages(cleanQuery);

            callback.onResult(new SearchResult(cleanQuery, contacts, conversations, messages));
        });
    }

    private CursorList<SignalRecipient> queryContacts(String query) {
        if (!Permissions.hasAny(context, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) {
            return CursorList.emptyList();
        }

        Cursor textSecureContacts = contactsDatabase.queryTextSecureContacts(query);
        Cursor systemContacts = contactsDatabase.querySystemContacts(query);
        MergeCursor contacts = new MergeCursor(new Cursor[]{textSecureContacts, systemContacts});

        return new CursorList<>(contacts, new RecipientModelBuilder(context));
    }

    private CursorList<ThreadRecord> queryConversations(@NonNull String query) {
        List<String> numbers = contactAccessor.getNumbersForThreadSearchFilter(context, query);
        List<SignalAddress> addresses = Stream.of(numbers).map(number -> SignalAddress.fromExternal(context, number)).toList();

        Cursor conversations = threadDatabase.getFilteredConversationList(addresses);
        return conversations != null ? new CursorList<>(conversations, new ThreadModelBuilder(threadDatabase))
                : CursorList.emptyList();
    }

    private CursorList<MessageResult> queryMessages(@NonNull String query) {
        Cursor messages = searchDatabase.queryMessages(query);
        return messages != null ? new CursorList<>(messages, new MessageModelBuilder(context))
                : CursorList.emptyList();
    }

    /**
     * Unfortunately {@link DatabaseUtils#sqlEscapeString(String)} is not sufficient for our purposes.
     * MATCH queries have a separate format of their own that disallow most "special" characters.
     * <p>
     * Also, SQLite can't search for apostrophes, meaning we can't normally find words like "I'm".
     * However, if we replace the apostrophe with a space, then the query will find the match.
     */
    private String sanitizeQuery(@NonNull String query) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (!BANNED_CHARACTERS.contains(c)) {
                out.append(c);
            } else if (c == '\'') {
                out.append(' ');
            }
        }

        return out.toString();
    }

    private static class RecipientModelBuilder implements CursorList.ModelBuilder<SignalRecipient> {

        private final Context context;

        RecipientModelBuilder(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public SignalRecipient build(@NonNull Cursor cursor) {
            SignalAddress address = SignalAddress.fromExternal(context, cursor.getString(1));
            return SignalRecipient.from(context, address, false);
        }
    }

    private static class ThreadModelBuilder implements CursorList.ModelBuilder<ThreadRecord> {

        private final ThreadDatabase threadDatabase;

        ThreadModelBuilder(@NonNull ThreadDatabase threadDatabase) {
            this.threadDatabase = threadDatabase;
        }

        @Override
        public ThreadRecord build(@NonNull Cursor cursor) {
            return threadDatabase.readerFor(cursor).getCurrent();
        }
    }

    private static class MessageModelBuilder implements CursorList.ModelBuilder<MessageResult> {

        private final Context context;

        MessageModelBuilder(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public MessageResult build(@NonNull Cursor cursor) {
            SignalAddress address = SignalAddress.fromSerialized(cursor.getString(0));
            SignalRecipient recipient = SignalRecipient.from(context, address, false);
            String body = cursor.getString(cursor.getColumnIndexOrThrow(SearchDatabase.SNIPPET));
            long receivedMs = cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.NORMALIZED_DATE_RECEIVED));
            long threadId = cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.THREAD_ID));

            return new MessageResult(recipient, body, threadId, receivedMs);
        }
    }

    public interface Callback {
        void onResult(@NonNull SearchResult result);
    }
}

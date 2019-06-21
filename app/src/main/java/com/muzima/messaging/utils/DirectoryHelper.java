package com.muzima.messaging.utils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.muzima.BuildConfig;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.contacts.ContactAccessor;
import com.muzima.messaging.crypto.SessionUtil;
import com.muzima.messaging.jobs.MultiDeviceContactUpdateJob;
import com.muzima.messaging.push.AccountManagerFactory;
import com.muzima.messaging.push.IasTrustStore;
import com.muzima.messaging.sms.IncomingJoinedMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.InsertResult;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.RecipientDatabase.RegisteredState;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.notifications.NotificationChannels;
import com.muzima.utils.Permissions;
import com.muzima.utils.concurrent.SignalExecutors;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.ContactTokenDetails;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.internal.contacts.crypto.Quote;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedQuoteException;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedResponseException;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DirectoryHelper {
    private static final String TAG = DirectoryHelper.class.getSimpleName();

    private static final int CONTACT_DISCOVERY_BATCH_SIZE = 2048;

    public static void refreshDirectory(@NonNull Context context, boolean notifyOfNewUsers)
            throws IOException {
        if (TextUtils.isEmpty(TextSecurePreferences.getLocalNumber(context))) return;
        if (!Permissions.hasAll(context, Manifest.permission.WRITE_CONTACTS)) return;

        List<SignalAddress> newlyActiveUsers = refreshDirectory(context, AccountManagerFactory.createManager(context));

        if (TextSecurePreferences.isMultiDevice(context)) {
            MuzimaApplication.getInstance(context)
                    .getJobManager()
                    .add(new MultiDeviceContactUpdateJob(context));
        }

        if (notifyOfNewUsers) notifyNewUsers(context, newlyActiveUsers);
    }

    @SuppressLint("CheckResult")
    private static @NonNull
    List<SignalAddress> refreshDirectory(@NonNull Context context, @NonNull SignalServiceAccountManager accountManager)
            throws IOException {
        if (TextUtils.isEmpty(TextSecurePreferences.getLocalNumber(context))) {
            return Collections.emptyList();
        }

        if (!Permissions.hasAll(context, Manifest.permission.WRITE_CONTACTS)) {
            return Collections.emptyList();
        }

        RecipientDatabase recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
        Stream<String> eligibleRecipientDatabaseContactNumbers = Stream.of(recipientDatabase.getAllAddresses()).filter(SignalAddress::isPhone).map(SignalAddress::toPhoneString);
        Stream<String> eligibleSystemDatabaseContactNumbers = Stream.of(ContactAccessor.getInstance().getAllContactsWithNumbers(context)).map(SignalAddress::serialize);
        Set<String> eligibleContactNumbers = Stream.concat(eligibleRecipientDatabaseContactNumbers, eligibleSystemDatabaseContactNumbers).collect(Collectors.toSet());

        Future<DirectoryResult> legacyRequest = getLegacyDirectoryResult(context, accountManager, recipientDatabase, eligibleContactNumbers);
        List<Future<Set<String>>> contactServiceRequest = getContactServiceDirectoryResult(context, accountManager, eligibleContactNumbers);

        try {
            DirectoryResult legacyResult = legacyRequest.get();
            Optional<Set<String>> contactServiceResult = executeAndMergeContactDiscoveryRequests(accountManager, contactServiceRequest);

            if (!contactServiceResult.isPresent()) {
                Log.i(TAG, "[Batch] New contact discovery service failed, so we're skipping the comparison.");
                return legacyResult.getNewlyActiveAddresses();
            }

            if (legacyResult.getNumbers().size() == contactServiceResult.get().size() && legacyResult.getNumbers().containsAll(contactServiceResult.get())) {
                Log.i(TAG, "[Batch] New contact discovery service request matched existing results.");
                accountManager.reportContactDiscoveryServiceMatch();
            } else {
                Log.w(TAG, "[Batch] New contact discovery service request did NOT match existing results.");
                accountManager.reportContactDiscoveryServiceMismatch();
            }

            return legacyResult.getNewlyActiveAddresses();

        } catch (InterruptedException e) {
            throw new IOException("[Batch] Operation was interrupted.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                Log.e(TAG, "[Batch] Experienced an unexpected exception.", e);
                throw new AssertionError(e);
            }
        }
    }

    public static RegisteredState refreshDirectoryFor(@NonNull Context context,
                                                                        @NonNull SignalRecipient recipient)
            throws IOException {
        RecipientDatabase recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
        SignalServiceAccountManager accountManager = AccountManagerFactory.createManager(context);

        Future<RegisteredState> legacyRequest = getLegacyRegisteredState(context, accountManager, recipientDatabase, recipient);
        List<Future<Set<String>>> contactServiceRequest = getContactServiceDirectoryResult(context, accountManager, Collections.singleton(recipient.getAddress().serialize()));

        try {
            RegisteredState legacyState = legacyRequest.get();
            Optional<Set<String>> contactServiceResult = executeAndMergeContactDiscoveryRequests(accountManager, contactServiceRequest);

            if (!contactServiceResult.isPresent()) {
                Log.i(TAG, "[Singular] New contact discovery service failed, so we're skipping the comparison.");
                return legacyState;
            }

            RegisteredState contactServiceState = contactServiceResult.get().size() == 1 ? RegisteredState.REGISTERED : RegisteredState.NOT_REGISTERED;

            if (legacyState == contactServiceState) {
                Log.i(TAG, "[Singular] New contact discovery service request matched existing results.");
                accountManager.reportContactDiscoveryServiceMatch();
            } else {
                Log.w(TAG, "[Singular] New contact discovery service request did NOT match existing results.");
                accountManager.reportContactDiscoveryServiceMismatch();
            }

            return legacyState;

        } catch (InterruptedException e) {
            throw new IOException("[Singular] Operation was interrupted.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                Log.e(TAG, "[Singular] Experienced an unexpected exception.", e);
                throw new AssertionError(e);
            }
        }
    }

    private static void updateContactsDatabase(@NonNull Context context, @NonNull List<SignalAddress> activeAddresses, boolean removeMissing) {
        Optional<AccountHolder> account = getOrCreateAccount(context);

        if (account.isPresent()) {
            try {
                DatabaseFactory.getContactsDatabase(context).removeDeletedRawContacts(account.get().getAccount());
                DatabaseFactory.getContactsDatabase(context).setRegisteredUsers(account.get().getAccount(), activeAddresses, removeMissing);

                Cursor cursor = ContactAccessor.getInstance().getAllSystemContacts(context);
                RecipientDatabase.BulkOperationsHandle handle = DatabaseFactory.getRecipientDatabase(context).resetAllSystemContactInfo();

                try {
                    while (cursor != null && cursor.moveToNext()) {
                        String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        if (!TextUtils.isEmpty(number)) {
                            SignalAddress address = SignalAddress.fromExternal(context, number);
                            String displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String contactPhotoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                            String contactLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL));
                            Uri contactUri = ContactsContract.Contacts.getLookupUri(cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)));

                            handle.setSystemContactInfo(address, displayName, contactPhotoUri, contactLabel, contactUri.toString());
                        }
                    }
                } finally {
                    handle.finish();
                }

                if (NotificationChannels.supported()) {
                    try (RecipientDatabase.RecipientReader recipients = DatabaseFactory.getRecipientDatabase(context).getRecipientsWithNotificationChannels()) {
                        SignalRecipient recipient;
                        while ((recipient = recipients.getNext()) != null) {
                            NotificationChannels.updateContactChannelName(context, recipient);
                        }
                    }
                }
            } catch (RemoteException | OperationApplicationException e) {
                Log.w(TAG, e);
            }
        }
    }

    private static void notifyNewUsers(@NonNull Context context,
                                       @NonNull List<SignalAddress> newUsers) {
        if (!TextSecurePreferences.isNewContactsNotificationEnabled(context)) return;

        for (SignalAddress newUser : newUsers) {
            if (!SessionUtil.hasSession(context, newUser) && !Util.isOwnNumber(context, newUser)) {
                IncomingJoinedMessage message = new IncomingJoinedMessage(newUser);
                Optional<InsertResult> insertResult = DatabaseFactory.getSmsDatabase(context).insertMessageInbox(message);

                if (insertResult.isPresent()) {
                    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    if (hour >= 9 && hour < 23) {
                        MessageNotifier.updateNotification(context, insertResult.get().getThreadId(), true);
                    } else {
                        MessageNotifier.updateNotification(context, insertResult.get().getThreadId(), false);
                    }
                }
            }
        }
    }

    private static Optional<AccountHolder> getOrCreateAccount(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.muzima");

        Optional<AccountHolder> account;

        if (accounts.length == 0) account = createAccount(context);
        else account = Optional.of(new AccountHolder(accounts[0], false));

        if (account.isPresent() && !ContentResolver.getSyncAutomatically(account.get().getAccount(), ContactsContract.AUTHORITY)) {
            ContentResolver.setSyncAutomatically(account.get().getAccount(), ContactsContract.AUTHORITY, true);
        }

        return account;
    }

    private static Optional<AccountHolder> createAccount(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(context.getString(R.string.app_name), "com.muzima");

        if (accountManager.addAccountExplicitly(account, null, null)) {
            Log.i(TAG, "Created new account...");
            ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
            return Optional.of(new AccountHolder(account, true));
        } else {
            Log.w(TAG, "Failed to create account!");
            return Optional.absent();
        }
    }

    private static Future<DirectoryResult> getLegacyDirectoryResult(@NonNull Context context,
                                                                    @NonNull SignalServiceAccountManager accountManager,
                                                                    @NonNull RecipientDatabase recipientDatabase,
                                                                    @NonNull Set<String> eligibleContactNumbers) {
        return SignalExecutors.IO.submit(() -> {
            List<ContactTokenDetails> activeTokens = accountManager.getContacts(eligibleContactNumbers);

            if (activeTokens != null) {
                List<SignalAddress> activeAddresses = new LinkedList<>();
                List<SignalAddress> inactiveAddresses = new LinkedList<>();

                Set<String> inactiveContactNumbers = new HashSet<>(eligibleContactNumbers);

                for (ContactTokenDetails activeToken : activeTokens) {
                    activeAddresses.add(SignalAddress.fromSerialized(activeToken.getNumber()));
                    inactiveContactNumbers.remove(activeToken.getNumber());
                }

                for (String inactiveContactNumber : inactiveContactNumbers) {
                    inactiveAddresses.add(SignalAddress.fromSerialized(inactiveContactNumber));
                }

                Set<SignalAddress> currentActiveAddresses = new HashSet<>(recipientDatabase.getRegistered());
                Set<SignalAddress> contactAddresses = new HashSet<>(recipientDatabase.getSystemContacts());
                List<SignalAddress> newlyActiveAddresses = Stream.of(activeAddresses)
                        .filter(address -> !currentActiveAddresses.contains(address))
                        .filter(contactAddresses::contains)
                        .toList();

                recipientDatabase.setRegistered(activeAddresses, inactiveAddresses);
                updateContactsDatabase(context, activeAddresses, true);

                Set<String> activeContactNumbers = Stream.of(activeAddresses).map(SignalAddress::serialize).collect(Collectors.toSet());

                if (TextSecurePreferences.hasSuccessfullyRetrievedDirectory(context)) {
                    return new DirectoryResult(activeContactNumbers, newlyActiveAddresses);
                } else {
                    TextSecurePreferences.setHasSuccessfullyRetrievedDirectory(context, true);
                    return new DirectoryResult(activeContactNumbers);
                }
            }
            return new DirectoryResult(Collections.emptySet(), Collections.emptyList());
        });
    }

    private static Future<RegisteredState> getLegacyRegisteredState(@NonNull Context context,
                                                                    @NonNull SignalServiceAccountManager accountManager,
                                                                    @NonNull RecipientDatabase recipientDatabase,
                                                                    @NonNull SignalRecipient recipient) {
        return SignalExecutors.IO.submit(() -> {
            boolean activeUser = recipient.resolve().getRegistered() == RegisteredState.REGISTERED;
            boolean systemContact = recipient.isSystemContact();
            String number = recipient.getAddress().serialize();
            Optional<ContactTokenDetails> details = accountManager.getContact(number);

            if (details.isPresent()) {
                recipientDatabase.setRegistered(recipient, RegisteredState.REGISTERED);

                if (Permissions.hasAll(context, Manifest.permission.WRITE_CONTACTS)) {
                    updateContactsDatabase(context, Util.asList(recipient.getAddress()), false);
                }

                if (!activeUser && TextSecurePreferences.isMultiDevice(context)) {
                    MuzimaApplication.getInstance(context).getJobManager().add(new MultiDeviceContactUpdateJob(context));
                }

                if (!activeUser && systemContact && !TextSecurePreferences.getNeedsSqlCipherMigration(context)) {
                    notifyNewUsers(context, Collections.singletonList(recipient.getAddress()));
                }

                return RegisteredState.REGISTERED;
            } else {
                recipientDatabase.setRegistered(recipient, RegisteredState.NOT_REGISTERED);
                return RegisteredState.NOT_REGISTERED;
            }
        });
    }

    private static List<Future<Set<String>>> getContactServiceDirectoryResult(@NonNull Context context,
                                                                              @NonNull SignalServiceAccountManager accountManager,
                                                                              @NonNull Set<String> eligibleContactNumbers) {
        Set<String> sanitizedNumbers = sanitizeNumbers(eligibleContactNumbers);
        List<Set<String>> batches = splitIntoBatches(sanitizedNumbers, CONTACT_DISCOVERY_BATCH_SIZE);
        List<Future<Set<String>>> futures = new ArrayList<>(batches.size());

        for (Set<String> batch : batches) {
            Future<Set<String>> future = SignalExecutors.IO.submit(() -> {
                return new HashSet<>(accountManager.getRegisteredUsers(getIasKeyStore(context), batch, BuildConfig.MRENCLAVE));
            });
            futures.add(future);
        }
        return futures;
    }

    private static Set<String> sanitizeNumbers(@NonNull Set<String> numbers) {
        return Stream.of(numbers).filter(number -> {
            try {
                return number.startsWith("+") && number.length() > 1 && Long.parseLong(number.substring(1)) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }).collect(Collectors.toSet());
    }

    private static List<Set<String>> splitIntoBatches(@NonNull Set<String> numbers, int batchSize) {
        List<String> numberList = new ArrayList<>(numbers);
        List<Set<String>> batches = new LinkedList<>();

        for (int i = 0; i < numberList.size(); i += batchSize) {
            List<String> batch = numberList.subList(i, Math.min(numberList.size(), i + batchSize));
            batches.add(new HashSet<>(batch));
        }

        return batches;
    }

    private static Optional<Set<String>> executeAndMergeContactDiscoveryRequests(@NonNull SignalServiceAccountManager accountManager, @NonNull List<Future<Set<String>>> futures) {
        Set<String> results = new HashSet<>();
        try {
            for (Future<Set<String>> future : futures) {
                results.addAll(future.get());
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Contact discovery batch was interrupted.", e);
            accountManager.reportContactDiscoveryServiceUnexpectedError();
            return Optional.absent();
        } catch (ExecutionException e) {
            if (isAttestationError(e.getCause())) {
                Log.w(TAG, "Failed during attestation.", e);
                accountManager.reportContactDiscoveryServiceAttestationError();
                return Optional.absent();
            } else if (e.getCause() instanceof PushNetworkException) {
                Log.w(TAG, "Failed due to poor network.", e);
                return Optional.absent();
            } else {
                Log.w(TAG, "Failed for an unknown reason.", e);
                accountManager.reportContactDiscoveryServiceUnexpectedError();
                return Optional.absent();
            }
        }

        return Optional.of(results);
    }

    private static boolean isAttestationError(Throwable e) {
        return e instanceof CertificateException ||
                e instanceof SignatureException ||
                e instanceof UnauthenticatedQuoteException ||
                e instanceof UnauthenticatedResponseException ||
                e instanceof Quote.InvalidQuoteFormatException;
    }

    private static KeyStore getIasKeyStore(@NonNull Context context)
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        TrustStore contactTrustStore = new IasTrustStore(context);

        KeyStore keyStore = KeyStore.getInstance("BKS");
        keyStore.load(contactTrustStore.getKeyStoreInputStream(), contactTrustStore.getKeyStorePassword().toCharArray());

        return keyStore;
    }

    private static class DirectoryResult {

        private final Set<String> numbers;
        private final List<SignalAddress> newlyActiveAddresses;

        DirectoryResult(@NonNull Set<String> numbers) {
            this(numbers, Collections.emptyList());
        }

        DirectoryResult(@NonNull Set<String> numbers, @NonNull List<SignalAddress> newlyActiveAddresses) {
            this.numbers = numbers;
            this.newlyActiveAddresses = newlyActiveAddresses;
        }

        Set<String> getNumbers() {
            return numbers;
        }

        List<SignalAddress> getNewlyActiveAddresses() {
            return newlyActiveAddresses;
        }
    }

    private static class AccountHolder {

        private final boolean fresh;
        private final Account account;

        private AccountHolder(Account account, boolean fresh) {
            this.fresh = fresh;
            this.account = account;
        }

        @SuppressWarnings("unused")
        public boolean isFresh() {
            return fresh;
        }

        public Account getAccount() {
            return account;
        }

    }
}

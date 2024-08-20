package com.muzima.api;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.INVALID_CREDENTIALS_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR;

import android.content.Context;

import androidx.annotation.NonNull;

import com.muzima.MuzimaApplication;
import com.muzima.api.retrofit.RetrofitServiceInstance;
import com.muzima.callbacks.AuntenticationCallBack;
import com.muzima.db.MuzimaDatabase;
import com.muzima.db.daos.UserDao;
import com.muzima.db.entities.Credential;
import com.muzima.db.entities.User;
import com.muzima.model.OpenMRSSession;
import com.muzima.search.api.util.DigestUtil;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.Constants;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class APIUtils {
    private static MuzimaDatabase muzimaDatabase;
    private static User authenticatedUser;

    public static MuzimaDatabase getMuzimaDatabase(Context context){
        try {
            muzimaDatabase = ((MuzimaApplication) context).getDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return muzimaDatabase;
    }
    public static void authenticate(Context context, String username, String password, String serverUrl, boolean isUpdatePasswordRequired, @NonNull AuntenticationCallBack callback) {
       getMuzimaDatabase(context);
        if (isUpdatePasswordRequired) {
            authenticateOnlineAndUpdateCredentialsWithNewPassword(context, username, password, serverUrl, callback);
        } else {
            try {
                User user = muzimaDatabase.userDao().getUserByUsername(username);
                if (user != null) {
                    authenticateOffline(username, password, callback);
                } else {
                    authenticateOnline(context, username, password, serverUrl, callback);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void authenticateOffline(String username, String password, @NonNull AuntenticationCallBack callback) {
        Credential credential = muzimaDatabase.credentialDao().getCredentialByUsername(username);

        if (credential != null) {
            String salt = credential.getSalt();
            String hashedPassword = null;
            try {
                hashedPassword = DigestUtil.getSHA1Checksum(salt + ":" + password);
                if (!StringUtil.equals(hashedPassword, credential.getPassword())) {
                    callback.onError(INVALID_CREDENTIALS_ERROR);
                }else{
                    authenticatedUser = muzimaDatabase.userDao().getUserByUsername(username);
                    callback.onAuthenticated(true);
                }
            } catch (IOException e) {
                callback.onError(UNKNOWN_ERROR);
            }
        } else {
            callback.onError(INVALID_CREDENTIALS_ERROR);
        }
    }

    public static void authenticateOnline(Context context, String username, String password, String serverUrl, @NonNull AuntenticationCallBack callback) {
        OpenmrsAPIService openMRSRestApi = RetrofitServiceInstance.createService(context, OpenmrsAPIService.class, username, password, serverUrl);
        if (openMRSRestApi == null) {
            callback.onError(Constants.DataSyncServiceConstants.SyncStatusConstants.LOCAL_CONNECTION_ERROR);
            return;
        }

        Call<OpenMRSSession> call = openMRSRestApi.getSession();

        call.enqueue(new Callback<OpenMRSSession>() {
            @Override
            public void onResponse(@NonNull Call<OpenMRSSession> call, @NonNull Response<OpenMRSSession> response) {

                if (response.isSuccessful()) {
                    OpenMRSSession openMRSSession = response.body();
                    if (openMRSSession.isAuthenticated()) {
                        User user = openMRSSession.getUser();
                        if (user != null) {
                            new InsertUserTask(context, user).execute();
                            String uuid = UUID.randomUUID().toString();
                            String salt = null;
                            try {
                                salt = DigestUtil.getSHA1Checksum(uuid);
                                String hashedPassword = DigestUtil.getSHA1Checksum(salt + ":" + password);
                                Credential credential = new Credential();
                                credential.setUuid(uuid);
                                credential.setSalt(salt);
                                credential.setUsername(username);
                                credential.setPassword(hashedPassword);
                                new InsertCredentialTask(context, credential).execute();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        new GetUserTask(context, username).execute();
                        callback.onAuthenticated(response.isSuccessful());
                    } else {
                        callback.onError(INVALID_CREDENTIALS_ERROR);
                    }
                } else {
                    callback.onError(UNKNOWN_ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OpenMRSSession> call, @NonNull Throwable t) {
                callback.onError(AUTHENTICATION_ERROR);
            }
        });
    }

    public static void authenticateOnlineAndUpdateCredentialsWithNewPassword(Context context, String username, String password, String serverUrl, @NonNull AuntenticationCallBack callback) {
        OpenmrsAPIService openMRSRestApi = RetrofitServiceInstance.createService(context, OpenmrsAPIService.class, username, password, serverUrl);
        if (openMRSRestApi == null) {
            callback.onError(Constants.DataSyncServiceConstants.SyncStatusConstants.LOCAL_CONNECTION_ERROR);
            return;
        }
        Call<OpenMRSSession> call = openMRSRestApi.getSession();
        call.enqueue(new Callback<OpenMRSSession>() {
            @Override
            public void onResponse(@NonNull Call<OpenMRSSession> call, @NonNull Response<OpenMRSSession> response) {
                if (response.isSuccessful()) {
                    OpenMRSSession openMRSSession = response.body();
                    if (openMRSSession.isAuthenticated()) {
                        User user = openMRSSession.getUser();
                        if (user != null) {
                            Credential credential = null;
                            try {
                                credential = muzimaDatabase.credentialDao().getCredentialByUsername(username);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            String salt = credential.getSalt();
                            try {
                                String hashedPassword = DigestUtil.getSHA1Checksum(salt + ":" + password);
                                credential.setPassword(hashedPassword);
                                muzimaDatabase.credentialDao().update(credential);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        new GetUserTask(context, username).execute();
                        callback.onAuthenticated(response.isSuccessful());
                    } else {
                        callback.onError(INVALID_CREDENTIALS_ERROR);
                    }
                } else {
                    callback.onError(UNKNOWN_ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OpenMRSSession> call, @NonNull Throwable t) {
                callback.onError(AUTHENTICATION_ERROR);
            }
        });
    }

    public static User getAuthenticatedUser() {
        return authenticatedUser;
    }

    public static boolean isAuthenticated() {
        return authenticatedUser != null;
    }


    private static class InsertUserTask extends MuzimaAsyncTask<Void, Void, Boolean> {

        private WeakReference<Context> activityReference;
        private User user;

        InsertUserTask(Context context, User user) {
            activityReference = new WeakReference<>(context);
            this.user = user;
        }

        @Override
        protected void onPreExecute() {

        }

        // doInBackground methods runs on a worker thread
        @Override
        protected Boolean doInBackground(Void... objs) {
            muzimaDatabase.userDao().insert(user);
            return true;
        }

        // onPostExecute runs on main thread
        @Override
        protected void onPostExecute(Boolean bool) {

        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }

    private static class InsertCredentialTask extends MuzimaAsyncTask<Void, Void, Boolean> {

        private WeakReference<Context> activityReference;
        private Credential credential;

        InsertCredentialTask(Context context, Credential credential){
            activityReference = new WeakReference<>(context);
            this.credential = credential;
        }

        @Override
        protected void onPreExecute() {

        }

        // doInBackground methods runs on a worker thread
        @Override
        protected Boolean doInBackground(Void... objs) {
            muzimaDatabase.credentialDao().insert(credential);
            return true;
        }

        // onPostExecute runs on main thread
        @Override
        protected void onPostExecute(Boolean bool) {

        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }

    private static class GetUserTask extends MuzimaAsyncTask<Void, Void, User> {

        private WeakReference<Context> activityReference;
        private String username;

        GetUserTask(Context context, String username){
            activityReference = new WeakReference<>(context);
            this.username = username;
        }

        @Override
        protected void onPreExecute() {

        }

        // doInBackground methods runs on a worker thread
        @Override
        protected User doInBackground(Void... objs) {
            authenticatedUser = muzimaDatabase.userDao().getUserByUsername(username);
            return null;
        }

        // onPostExecute runs on main thread
        @Override
        protected void onPostExecute(User user) {

        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }

    public static User getUserByUsername(String username, Context context){
        getMuzimaDatabase(context);
        UserDao userDao = muzimaDatabase.userDao();
        return userDao.getUserByUsername(username);
    }
}

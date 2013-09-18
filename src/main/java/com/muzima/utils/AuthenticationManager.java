package com.muzima.utils;

import com.muzima.api.context.Context;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.*;

public class AuthenticationManager {

    public static int authenticate(String[] credentials, Context context) {
        String username = credentials[0];
        String password = credentials[1];
        String server = credentials[2];

        if (context == null) {
            return CANCELLED;
        }

        try {
            context.openSession();
            if (!context.isAuthenticated()) {
                context.authenticate(username, password, server);
            }

        } catch (ConnectException e) {
            return CONNECTION_ERROR;
        } catch (ParseException e) {
            return PARSING_ERROR;
        } catch (IOException e) {
            return AUTHENTICATION_ERROR;
        } finally {
            if (context != null)
                context.closeSession();
        }

        return AUTHENTICATION_SUCCESS;
    }

}

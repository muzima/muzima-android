package com.muzima.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import com.google.api.services.gmail.GmailScopes;
import com.muzima.api.context.Context;
import com.muzima.domain.Credentials;
import com.muzima.R;
import com.muzima.utils.StringUtils;

import java.util.Properties;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.mail.MessagingException;
import javax.mail.Session;
import com.google.api.services.gmail.model.Message;
import java.io.ByteArrayOutputStream;
import org.apache.commons.codec.binary.Base64;
import java.io.IOException;
import com.google.api.services.gmail.Gmail;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.InputStream;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import java.util.List;
import java.util.Collections;
import java.io.InputStreamReader;
import com.google.api.client.util.store.FileDataStoreFactory;
import java.security.GeneralSecurityException;
import com.google.api.client.http.javanet.NetHttpTransport;


public class FeedbackActivity extends BaseActivity {
    private Button sendButton;
    private Button cancelButton;
    private EditText feedbackText;
    private RatingBar feedbackRatingBar;

    private static final String APPLICATION_NAME = "ZitaGmailAPI";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_feedback);
        initViews();
        setupListeners();
    }

    private void setupListeners() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = feedbackText.getText().toString();
                if (!StringUtils.isEmpty(message)) {
                    //sendEmail();
                    sendGmail();
                } else {
                    feedbackText.setHint(getString(R.string.hint_feedback_prompt));
                    feedbackText.setHintTextColor(getResources().getColor(R.color.error_text_color));
                }
            }

            private String composeMessage(){
                String sender = String.join(" ", "Sender:", getUserName());
                String rating = String.join(" ", "Rating:", String.valueOf(feedbackRatingBar.getRating()));
                String feedback = String.join(" ", "Message:", feedbackText.getText().toString());
                String message = String.join("\n", sender, rating, feedback);
                return message;
            }

            private void sendEmail() {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"test.levelek@gmail.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "feedback");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, composeMessage());
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("message/rfc822");
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(emailIntent, 1);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
                }
            }

            private void sendGmail(){
                try {
                    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                    MimeMessage mimeMessage = createEmail("test.levelek@gmail.com", "test.levelek@gmail.com", "feedback", composeMessage());
                    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                            .setApplicationName(APPLICATION_NAME)
                            .build();
                } catch (MessagingException mex) {
                    Toast.makeText(getApplicationContext(), "MessagingException", Toast.LENGTH_SHORT).show();
                } catch (IOException ioex) {
                    Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_SHORT).show();
                } catch (GeneralSecurityException gse) {
                    Toast.makeText(getApplicationContext(), "GSException", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder builder = new AlertDialog.Builder(FeedbackActivity.this);
                    builder
                            .setCancelable(true)
                            .setTitle(FeedbackActivity.this.getUserName())
                            .setMessage(gse.toString())
                            .setPositiveButton(getResources().getText(R.string.general_ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    FeedbackActivity.this.onBackPressed();
                                }
                            }).create().show();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                FeedbackActivity.this.onBackPressed();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                //When the feedback is sent
                AlertDialog.Builder builder = new AlertDialog.Builder(FeedbackActivity.this);
                builder
                        .setCancelable(true)
                        .setTitle(FeedbackActivity.this.getUserName())
                        .setMessage(R.string.message_feedback_sent)
                        .setPositiveButton(getResources().getText(R.string.general_ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                FeedbackActivity.this.onBackPressed();
                            }
                        }).create().show();
            } else {
                //When the message didn't sent (cancelled by the user)
                AlertDialog.Builder builder = new AlertDialog.Builder(FeedbackActivity.this);
                builder
                        .setCancelable(true)
                        .setTitle(FeedbackActivity.this.getUserName())
                        .setMessage(R.string.message_feedback_halt)
                        .setPositiveButton(getResources().getText(R.string.general_ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                FeedbackActivity.this.onBackPressed();
                            }
                        }).create().show();
            }
        }
    }

    private void initViews() {
        sendButton = findViewById(R.id.send);
        cancelButton = findViewById(R.id.cancel);
        feedbackText = findViewById(R.id.feedback_message);
        feedbackRatingBar = findViewById(R.id.question_rating);
    }

    private String getUserName() {
        Credentials credentials;
        credentials = new Credentials(this);
        return credentials.getUserName();
    }

    public static MimeMessage createEmail(String to,
                                          String from,
                                          String subject,
                                          String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        //String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        String encodedString = new String(Base64.encodeBase64(bytes));
        String encodedEmail = encodedString.replace('+','-').replace('/','_');
        // String encodedEmail = org.apache.commons.codec.binary.StringUtils.newStringsUtf8(
        //          Base64.encodeBase64(bytes, true, false, Integer.MAX_VALUE));
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public static Message sendMessage(Gmail service,
                                      String userId,
                                      MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = FeedbackActivity.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}


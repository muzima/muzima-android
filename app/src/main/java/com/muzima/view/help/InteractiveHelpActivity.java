/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.help;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseActivity;

public class InteractiveHelpActivity extends BaseActivity {
    private static final String EMAIL_TO = "help@muzima.org";
    private static final String SUBJECT = "Asking help - ";

    private Button sendButton;
    private Button cancelButton;
    private EditText helpText;
    private Spinner options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_help);
        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void setupListeners() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = helpText.getText().toString();
                if (!StringUtils.isEmpty(message)) {
                    sendEmail();
                } else {
                    helpText.setHint(getString(R.string.hint_help_prompt));
                    helpText.setHintTextColor(getResources().getColor(R.color.error_text_color));
                }
            }

            private String composeMessage() {
                String sender = "Sender: " + getUserName();
                String helpMsg = "Message: " + helpText.getText().toString();
                String message = sender + "\n" + helpMsg;
                return message;
            }

            private String composeSubject() {
                return SUBJECT + options.getSelectedItem().toString();
            }

            private void sendEmail() {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{EMAIL_TO});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, composeSubject());
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, composeMessage());
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("message/rfc822");
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(emailIntent, 1);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InteractiveHelpActivity.this.onBackPressed();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InteractiveHelpActivity.this);
            builder
                    .setCancelable(true)
                    .setMessage(R.string.message_help_sent)
                    .setPositiveButton(getResources().getText(R.string.general_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            InteractiveHelpActivity.this.onBackPressed();
                        }
                    }).create().show();
        }
    }

    private void initViews() {
        sendButton = findViewById(R.id.send);
        cancelButton = findViewById(R.id.cancel);
        helpText = findViewById(R.id.help_message);
        options = findViewById(R.id.spinner);
    }

    private String getUserName() {
        Credentials credentials;
        credentials = new Credentials(this);
        return credentials.getUserName();
    }
}

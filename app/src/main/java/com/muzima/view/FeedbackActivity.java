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
import com.muzima.domain.Credentials;
import com.muzima.R;
import com.muzima.utils.StringUtils;


public class FeedbackActivity extends BaseActivity {
    private Button sendButton;
    private Button cancelButton;
    private EditText feedbackText;
    private RatingBar feedbackRatingBar;

    private static final String EMAIL_TO = "help@muzima.org";
    private static final String SUBJECT = "Feedback";

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
                    sendEmail();
                } else {
                    feedbackText.setHint(getString(R.string.hint_feedback_prompt));
                    feedbackText.setHintTextColor(getResources().getColor(R.color.error_text_color));
                }
            }

            private String composeMessage() {
                String sender = String.join(" ", "Sender:", getUserName());
                String rating = String.join(" ", "Rating:", String.valueOf(feedbackRatingBar.getRating()));
                String feedback = String.join(" ", "Message:", feedbackText.getText().toString());
                String message = String.join("\n", sender, rating, feedback);
                return message;
            }

            private void sendEmail() {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{EMAIL_TO});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, SUBJECT);
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
        Credentials credentials = new Credentials(this);
        return credentials.getUserName();
    }
}


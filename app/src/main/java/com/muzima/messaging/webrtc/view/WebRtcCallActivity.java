package com.muzima.messaging.webrtc.view;

import android.support.v7.app.AppCompatActivity;

public class WebRtcCallActivity extends AppCompatActivity {

    private static final String TAG = WebRtcCallActivity.class.getSimpleName();

    private static final int STANDARD_DELAY_FINISH    = 1000;
    public  static final int BUSY_SIGNAL_DELAY_FINISH = 5500;

    public static final String ANSWER_ACTION   = WebRtcCallActivity.class.getCanonicalName() + ".ANSWER_ACTION";
    public static final String DENY_ACTION     = WebRtcCallActivity.class.getCanonicalName() + ".DENY_ACTION";
    public static final String END_CALL_ACTION = WebRtcCallActivity.class.getCanonicalName() + ".END_CALL_ACTION";

}

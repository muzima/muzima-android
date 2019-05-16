package com.muzima.messaging.customcomponents.emoji;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;

public class RepeatableImageKey extends ImageButton {
    private KeyEventListener listener;

    public RepeatableImageKey(Context context) {
        super(context);
        init();
    }

    public RepeatableImageKey(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RepeatableImageKey(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RepeatableImageKey(Context context, AttributeSet attrs, int defStyleAttr,
                              int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOnClickListener(new RepeaterClickListener());
        setOnTouchListener(new RepeaterTouchListener());
    }

    public void setOnKeyEventListener(KeyEventListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (this.listener != null) this.listener.onKeyEvent();
    }

    private class RepeaterClickListener implements View.OnClickListener {
        @Override public void onClick(View v) {
            notifyListener();
        }
    }

    private class Repeater implements Runnable {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        public void run() {
            notifyListener();
            postDelayed(this, Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
                    ? ViewConfiguration.getKeyRepeatDelay()
                    : 50);
        }
    }

    private class RepeaterTouchListener implements View.OnTouchListener {
        private Repeater repeater;

        public RepeaterTouchListener() {
            this.repeater = new Repeater();
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.postDelayed(repeater, Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
                            ? ViewConfiguration.getKeyRepeatTimeout()
                            : ViewConfiguration.getLongPressTimeout());
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    view.removeCallbacks(repeater);
                    return false;
                default:
                    return false;
            }
        }
    }

    public interface KeyEventListener {
        void onKeyEvent();
    }
}

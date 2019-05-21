package com.muzima.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public abstract class Reminder {
    private CharSequence title;
    private CharSequence text;

    private View.OnClickListener okListener;
    private View.OnClickListener dismissListener;

    public Reminder(@Nullable CharSequence title,
                    @NonNull CharSequence text) {
        this.title = title;
        this.text = text;
    }

    public @Nullable
    CharSequence getTitle() {
        return title;
    }

    public CharSequence getText() {
        return text;
    }

    public View.OnClickListener getOkListener() {
        return okListener;
    }

    public View.OnClickListener getDismissListener() {
        return dismissListener;
    }

    public void setOkListener(View.OnClickListener okListener) {
        this.okListener = okListener;
    }

    public void setDismissListener(View.OnClickListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public boolean isDismissable() {
        return true;
    }

    public @NonNull
    Importance getImportance() {
        return Importance.NORMAL;
    }


    public enum Importance {
        NORMAL, ERROR
    }
}

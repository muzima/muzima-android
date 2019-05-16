package com.muzima.messaging.customcomponents.emoji;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class AnimatingImageSpan extends ImageSpan {
    public AnimatingImageSpan(Drawable drawable, Drawable.Callback callback) {
        super(drawable, ALIGN_BOTTOM);
        drawable.setCallback(callback);
    }
}

package com.muzima.utils;

import android.text.Html;
import android.text.Spanned;

public class HtmlCompat {
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
    }
}

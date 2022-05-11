package com.muzima.utils;

import android.content.Context;
import android.graphics.Typeface;

public class FontManager {

    public static final String ROOT = "fonts/",
            FONTAWESOME = ROOT + "font_awesome.otf";

    public static Typeface getTypeface(Context context, String font) {
        return Typeface.createFromAsset(context.getAssets(), font);
    }

}

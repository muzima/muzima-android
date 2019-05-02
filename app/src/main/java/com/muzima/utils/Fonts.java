/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.content.Context;
import android.graphics.Typeface;

public class Fonts {
    private static Typeface ROBOTO_LIGHT = null;
    private static Typeface ROBOTO_BOLD_CONDENSED = null;
    private static Typeface ROBOTO_REGULAR = null;
    private static Typeface ROBOTO_ITALIC = null;
    private static Typeface ROBOTO_THIN = null;
    private static Typeface ROBOTO_BOLD = null;
    private static Typeface ROBOTO_MEDIUM = null;
    private static Typeface ROBOTO_BLACK = null;

    public static Typeface roboto_light(Context context) {
        if (ROBOTO_LIGHT == null) {
            ROBOTO_LIGHT = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
        }
        return ROBOTO_LIGHT;
    }

    public static Typeface roboto_bold_condensed(Context context) {
        if (ROBOTO_BOLD_CONDENSED == null) {
            ROBOTO_BOLD_CONDENSED = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-BoldCondensed.ttf");
        }
        return ROBOTO_BOLD_CONDENSED;
    }

    public static Typeface roboto_regular(Context context) {
        if (ROBOTO_REGULAR == null) {
            ROBOTO_REGULAR = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
        }
        return ROBOTO_REGULAR;
    }

    public static Typeface roboto_italic(Context context) {
        if (ROBOTO_ITALIC == null) {
            ROBOTO_ITALIC = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Italic.ttf");
        }
        return ROBOTO_ITALIC;
    }

    public static Typeface roboto_thin(Context context) {
        if (ROBOTO_THIN == null) {
            ROBOTO_THIN = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
        }
        return ROBOTO_THIN;
    }

    public static Typeface roboto_bold(Context context) {
        if (ROBOTO_BOLD == null) {
            ROBOTO_BOLD = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Bold.ttf");
        }
        return ROBOTO_BOLD;
    }

    public static Typeface roboto_medium(Context context) {
        if (ROBOTO_MEDIUM == null) {
            ROBOTO_MEDIUM = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Medium.ttf");
        }
        return ROBOTO_MEDIUM;
    }

    public static Typeface roboto_black(Context context) {
        if (ROBOTO_BLACK == null) {
            ROBOTO_BLACK = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Black.ttf");
        }
        return ROBOTO_BLACK;
    }
}

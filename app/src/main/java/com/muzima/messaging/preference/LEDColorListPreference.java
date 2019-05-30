package com.muzima.messaging.preference;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.muzima.R;

public class LEDColorListPreference extends ListPreference {

    private static final String TAG = LEDColorListPreference.class.getSimpleName();

    private ImageView colorImageView;

    public LEDColorListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.led_color_preference_widget);
    }

    public LEDColorListPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.led_color_preference_widget);
    }

    @Override
    public void setValue(String value) {
        CharSequence oldEntry = getEntry();
        super.setValue(value);
        CharSequence newEntry = getEntry();
        if (oldEntry != newEntry) {
            notifyDependencyChange(shouldDisableDependents());
        }

        if (value != null) setPreviewColor(value);
    }

    @Override
    public boolean shouldDisableDependents() {
        CharSequence newEntry = getValue();
        boolean shouldDisable = newEntry.equals("none");
        return shouldDisable || super.shouldDisableDependents();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        this.colorImageView = (ImageView) view.findViewById(R.id.color_view);
        setPreviewColor(getValue());
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(null);
    }

    private void setPreviewColor(@NonNull String value) {
        int color;

        switch (value) {
            case "green":
                color = getContext().getResources().getColor(R.color.green_500);
                break;
            case "red":
                color = getContext().getResources().getColor(R.color.red_500);
                break;
            case "blue":
                color = getContext().getResources().getColor(R.color.blue_500);
                break;
            case "yellow":
                color = getContext().getResources().getColor(R.color.yellow_500);
                break;
            case "cyan":
                color = getContext().getResources().getColor(R.color.cyan_500);
                break;
            case "magenta":
                color = getContext().getResources().getColor(R.color.pink_500);
                break;
            case "white":
                color = getContext().getResources().getColor(R.color.primary_white);
                break;
            default:
                color = getContext().getResources().getColor(R.color.transparent);
                break;
        }

        if (colorImageView != null) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);

            colorImageView.setImageDrawable(drawable);
        }
    }
}

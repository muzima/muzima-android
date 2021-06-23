package com.muzima.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.muzima.R;

public class GuidedSetupImageCardFragment extends Fragment {

    private int drawableId;
    private ImageView imageView;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private String title;
    private String description;

    public GuidedSetupImageCardFragment(int drawableId, String title, String description) {
        this.drawableId = drawableId;
        this.title = title;
        this.description = description;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guided_setup_cards,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
    }

    private void initializeResources(View view) {
        imageView = view.findViewById(R.id.card_image_view);
        titleTextView = view.findViewById(R.id.fragment_guided_setup_title_text_view);
        descriptionTextView = view.findViewById(R.id.fragment_guided_setup_description_text_view);
        imageView.setImageDrawable(getResources().getDrawable(drawableId));
        titleTextView.setText(title);
        descriptionTextView.setText(description);
    }
}

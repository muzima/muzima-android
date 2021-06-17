package com.muzima.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.muzima.R;

public class GuidedSetupImageCardFragment extends Fragment {

    private int drawableId;

    public GuidedSetupImageCardFragment(int drawableId) {
        this.drawableId = drawableId;
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
        ImageView imageView = view.findViewById(R.id.card_image_view);
        imageView.setImageDrawable(getResources().getDrawable(drawableId));
    }
}

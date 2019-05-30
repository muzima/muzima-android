package com.muzima.messaging.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.customcomponents.TypingIndicatorView;
import com.muzima.messaging.jobs.MultiDeviceConfigurationUpdateJob;

public class TypingIndicatorIntroFragment  extends Fragment {

    private Controller controller;

    public static TypingIndicatorIntroFragment newInstance() {
        TypingIndicatorIntroFragment fragment = new TypingIndicatorIntroFragment();
        Bundle args     = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TypingIndicatorIntroFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(getActivity() instanceof Controller)) {
            throw new IllegalStateException("Parent activity must implement the Controller interface.");
        }

        controller = (Controller) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view      = inflater.inflate(R.layout.experience_upgrade_typing_indicators_fragment, container, false);
        View yesButton = view.findViewById(R.id.experience_yes_button);
        View noButton  = view.findViewById(R.id.experience_no_button);

        ((TypingIndicatorView) view.findViewById(R.id.typing_indicator)).startAnimation();

        yesButton.setOnClickListener(v -> onButtonClicked(true));
        noButton.setOnClickListener(v -> onButtonClicked(false));

        return view;
    }

    private void onButtonClicked(boolean typingEnabled) {
        TextSecurePreferences.setTypingIndicatorsEnabled(getContext(), typingEnabled);
        MuzimaApplication.getInstance(requireContext())
                .getJobManager()
                .add(new MultiDeviceConfigurationUpdateJob(getContext(),
                        TextSecurePreferences.isReadReceiptsEnabled(requireContext()),
                        typingEnabled,
                        TextSecurePreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(getContext())));

        controller.onFinished();
    }

    public interface Controller {
        void onFinished();
    }
}

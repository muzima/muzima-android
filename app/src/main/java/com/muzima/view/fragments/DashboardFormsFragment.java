package com.muzima.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.muzima.R;
import com.muzima.adapters.viewpager.FormsViewPagerAdapter;
import com.muzima.model.events.FormSearchEvent;
import com.muzima.utils.Constants;

import org.greenrobot.eventbus.EventBus;

public class DashboardFormsFragment extends Fragment {
    private EditText searchFormsEditText;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private FormsViewPagerAdapter viewPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_form_pager, container, false);
        initializeResources(view);
        return view;
    }

    private void initializeResources(View view) {
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        viewPagerAdapter = new FormsViewPagerAdapter(getChildFragmentManager(), getActivity().getApplicationContext());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        searchFormsEditText = view.findViewById(R.id.dashboard_forms_search_edit_text);
        searchFormsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                EventBus.getDefault().post(new FormSearchEvent(searchFormsEditText.getText().toString(), viewPager.getCurrentItem()));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                EventBus.getDefault().post( new FormSearchEvent(Constants.EMPTY_STRING,0));
                EventBus.getDefault().post( new FormSearchEvent(Constants.EMPTY_STRING,1));
                EventBus.getDefault().post( new FormSearchEvent(Constants.EMPTY_STRING,2));
                searchFormsEditText.setText(Constants.EMPTY_STRING);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}

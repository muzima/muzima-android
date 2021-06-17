package com.muzima.adapters.viewpager;

import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.R;
import com.muzima.view.fragments.GuidedSetupImageCardFragment;

import java.util.List;

public class GuidedSetupCardsViewPagerAdapter extends FragmentPagerAdapter {

    public GuidedSetupCardsViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new GuidedSetupImageCardFragment(R.drawable.slide_a);
            case 1:
                return new GuidedSetupImageCardFragment(R.drawable.slide_b);
            case 2:
                return new GuidedSetupImageCardFragment(R.drawable.slide_c);
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}

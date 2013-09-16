package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.adapters.forms.SectionedFormsAdapter;

public abstract class FormsFragmentWithSectionedListAdapter extends FormsListFragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ((SectionedFormsAdapter)listAdapter).setListView(list);
        return view;
    }

}

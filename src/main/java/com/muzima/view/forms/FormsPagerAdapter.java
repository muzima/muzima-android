package com.muzima.view.forms;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.service.FormService;
import com.muzima.listeners.DownloadListener;

import java.io.IOException;

public class FormsPagerAdapter extends FragmentPagerAdapter implements DownloadListener<Integer[]>{
    private static final int TAB_NEW = 0;
    private static final int TAB_COMPLETE = 1;
    private static final int TAB_INCOMPLETE = 2;
    private static final int TAB_SYNCED = 3;

    private PagerView[] pagers;

    public FormsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        initPagerViews(context);
    }

    @Override
    public Fragment getItem(int position) {
        return pagers[position].fragment;
    }

    @Override
    public int getCount() {
        return pagers.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return pagers[position].title;
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        pagers[TAB_NEW].fragment.downloadComplete(result);
    }

    private void initPagerViews(Context context) {
        final Resources resources = context.getResources();
        FormService formService = getFormService(context);
        pagers = new PagerView[4];
        pagers[TAB_NEW] = new PagerView("New", FormsListFragment.newInstance(
                new NewFormsAdapter(context, R.layout.item_forms_list, formService),
                resources.getString(R.string.no_new_form_msg),
                resources.getString(R.string.no_new_form_tip)));
        pagers[TAB_COMPLETE] = new PagerView("Complete", FormsListFragment.newInstance(
                null,
                resources.getString(R.string.no_complete_form_msg),
                resources.getString(R.string.no_complete_form_tip)));
        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", FormsListFragment.newInstance(
                null,
                resources.getString(R.string.no_incomplete_form_msg),
                resources.getString(R.string.no_incomplete_form_tip)));
        pagers[TAB_SYNCED] = new PagerView("Synced", FormsListFragment.newInstance(
                null,
                resources.getString(R.string.no_synced_form_msg),
                resources.getString(R.string.no_synced_form_tip)));
    }

    private FormService getFormService(Context context){
        com.muzima.api.context.Context muzimaContext = ((MuzimaApplication) context.getApplicationContext()).getMuzimaContext();
        FormService formService = null;
        try {
            formService = muzimaContext.getFormService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return formService;
    }

    private static class PagerView {
        String title;
        FormsListFragment fragment;

        private PagerView(String title, FormsListFragment fragment) {
            this.title = title;
            this.fragment = fragment;
        }
    }

}

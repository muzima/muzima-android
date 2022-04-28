package com.muzima.adapters.observations;

import android.os.AsyncTask;
import com.muzima.controller.ConceptController;
import java.util.ArrayList;
import java.util.List;

public class ObservationGroupBackgroundTask extends AsyncTask<Void, List<String>, List<String>> {

    private final ObservationGroupAdapter observationGroupAdapter;
    private final ConceptController conceptController;

    public ObservationGroupBackgroundTask(ObservationGroupAdapter observationGroupAdapter,
                                            ConceptController conceptController) {
        this.observationGroupAdapter = observationGroupAdapter;
        this.conceptController = conceptController;
    }

    @Override
    protected void onPreExecute() {
        if (observationGroupAdapter.getBackgroundListQueryTaskListener() != null) {
            observationGroupAdapter.getBackgroundListQueryTaskListener().onQueryTaskStarted();
        }
    }

    @Override
    protected List<String> doInBackground(Void... params) {
        List<String> obsGroups = new ArrayList();
        //Todo: get obsgroup
        obsGroups.add("Vital Signss");
        return obsGroups;
    }


    @Override
    protected void onPostExecute(List<String> obsGroups) {
        if (obsGroups != null) {
            observationGroupAdapter.clear();
            observationGroupAdapter.add(obsGroups);
            observationGroupAdapter.notifyDataSetChanged();
        }

        if (observationGroupAdapter.getBackgroundListQueryTaskListener() != null) {
            observationGroupAdapter.getBackgroundListQueryTaskListener().onQueryTaskFinish();
        }
    }
}
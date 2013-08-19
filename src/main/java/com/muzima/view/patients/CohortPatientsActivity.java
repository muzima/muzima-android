package com.muzima.view.patients;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import com.muzima.R;

/**
 * Created with IntelliJ IDEA.
 * User: twer
 * Date: 8/19/13
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class CohortPatientsActivity extends Activity {
    public static final String COHORT_ID = "cohortId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_list);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.client_list, menu);
//        // Associate searchable configuration with the SearchView
//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView = (SearchView) menu.findItem(R.id.search)
//                .getActionView();
//        searchView.setSearchableInfo(searchManager
//                .getSearchableInfo(getComponentName()));
//
//        if (quickSearch) {
//            searchView.setIconified(false);
//            searchView.requestFocus();
//        } else
//            searchView.setIconified(true);
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_client_add: // icon in action bar clicked
//                Intent intent = new Intent(this, RegisterClientActivity.class);
//                startActivity(intent);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        // overridePendingTransition(R.anim.push_in_from_left,R.anim.do_not_move_bottom);
//        super.onPause();
//    }
//
//    /** Called when the user clicks the Dashboard button in action bar */
//    public void dashboardClick(View view) {
//        toDashboard = true;
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//
//    }
//
//    /** Called when the user clicks on a client */
//    public void clientSummary(View view) {
//        toDashboard = true;
//        Intent intent = new Intent(this, ClientSummaryActivity.class);
//        startActivity(intent);
//
//    }
}

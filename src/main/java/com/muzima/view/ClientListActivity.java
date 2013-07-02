package com.muzima.view;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import com.muzima.R;

public class ClientListActivity extends Activity {
	public boolean toDashboard = false; // set to get the right transition
	public boolean quickSearch = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_list);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.getString("quickSearch").equals("true"))
				quickSearch = true;
		}

		ActionBar actionBar = getActionBar();
		// actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		// actionBar.setDisplayShowCustomEnabled(false);
		// actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		//actionBar.setIcon(R.drawable.ic_action_dashboard);
		// View cView
		// =getLayoutInflater().inflate(R.layout.actionbar_layout,null);
		// actionBar.setCustomView(cView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client_list, menu);
		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search)
				.getActionView();
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));

		if (quickSearch) {
			searchView.setIconified(false);
			searchView.requestFocus();
		} else
			searchView.setIconified(true);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.client_add: // icon in action bar clicked
			Intent intent = new Intent(this, RegisterClientActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		// overridePendingTransition(R.anim.push_in_from_left,R.anim.do_not_move_bottom);
		super.onPause();
	}

	/** Called when the user clicks the Dashboard button in action bar */
	public void dashboardClick(View view) {
		toDashboard = true;
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);

	}

	/** Called when the user clicks on a client */
	public void clientSummary(View view) {
		toDashboard = true;
		Intent intent = new Intent(this, ClientSummaryActivity.class);
		startActivity(intent);

	}

}

package com.muzima.view;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.muzima.R;
import com.muzima.adapters.MainDashboardAdapter;
import com.muzima.utils.ThemeUtils;

public class MainDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ViewPager viewPager;
    private MainDashboardAdapter adapter;
    private BottomNavigationView bottomNavigationView;
    private ActionBarDrawerToggle drawerToggle;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private MenuItem menubarLoadButton;
    private MenuItem menuUpload;
    private MenuItem tagsButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(MainDashboardActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_main_layout);
        initializeResources();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.form_list_menu, menu);
        menubarLoadButton = menu.findItem(R.id.menu_load);
        tagsButton = menu.findItem(R.id.menu_tags);
        menuUpload = menu.findItem(R.id.menu_upload);
        return super.onCreateOptionsMenu(menu);
    }

    public void hideProgressbar() {
        menubarLoadButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarLoadButton.setActionView(R.layout.refresh_menuitem);
    }

    private void initializeResources() {
        viewPager = findViewById(R.id.main_dashboard_view_pager);
        bottomNavigationView = findViewById(R.id.main_dashboard_bottom_navigation);
        toolbar = findViewById(R.id.dashboard_toolbar);
        drawerLayout = findViewById(R.id.main_dashboard_drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(MainDashboardActivity.this, drawerLayout,
                toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        adapter = new MainDashboardAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        setTitle(" ");
    }
}

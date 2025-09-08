package com.grepguru.zenlock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;

import com.grepguru.zenlock.model.*;
import com.grepguru.zenlock.ui.adapter.*;
import com.grepguru.zenlock.utils.AppUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class WhitelistActivity extends AppCompatActivity {

    // Configuration - Easy to modify
    private static final int MAX_ADDITIONAL_APPS = 3;
    
    // UI Components
    private RecyclerView recyclerView;
    private Button saveButton;
    private LinearLayout loadingContainer;
    private TabLayout appTabs;
    
    // Data Collections
    private List<SelectableAppModel> systemApps = new ArrayList<>();
    private List<SelectableAppModel> userApps = new ArrayList<>();
    private List<SelectableAppModel> currentAppList = new ArrayList<>(); // Currently displayed list
    private Set<String> defaultApps = new HashSet<>(); // Phone, Calendar, Clock (excluded from selection)
    private Set<String> selectedApps = new HashSet<>(); // User's additional app selections
    
    // Tab constants
    private static final int TAB_SYSTEM = 0;
    private static final int TAB_USER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        recyclerView = findViewById(R.id.whitelistRecyclerView);
        saveButton = findViewById(R.id.saveButton);
        loadingContainer = findViewById(R.id.loadingContainer);
        appTabs = findViewById(R.id.appTabs);

        defaultApps = AppUtils.getMainDefaultApps(this);
        loadUserSelections();

        // Setup tabs
        setupTabs();

        // Setup RecyclerView first with empty list
        WhitelistAdapter adapter = new WhitelistAdapter(currentAppList, selectedApps, MAX_ADDITIONAL_APPS);
        adapter.setOnSelectionChangeListener(this::updateSaveButtonText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load apps in background to improve responsiveness
        loadAndOrganizeAppsAsync(adapter);

        // Save Button Click Listener
        saveButton.setOnClickListener(v -> {
            saveWhitelist();
        });
        
        // Update save button text with selection count
        updateSaveButtonText();
    }

    private void setupTabs() {
        appTabs.addTab(appTabs.newTab().setText("System"));
        appTabs.addTab(appTabs.newTab().setText("User"));
        
        appTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void switchTab(int position) {
        currentAppList.clear();
        if (position == TAB_SYSTEM) {
            currentAppList.addAll(systemApps);
        } else {
            currentAppList.addAll(userApps);
        }
        
        WhitelistAdapter adapter = (WhitelistAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadUserSelections() {
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        Set<String> savedWhitelist = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        
        // Filter out default apps from saved selections - they don't count toward quota
        for (String packageName : savedWhitelist) {
            if (!defaultApps.contains(packageName)) {
                selectedApps.add(packageName);
            }
        }
    }
    
    private void loadAndOrganizeAppsAsync(WhitelistAdapter adapter) {
        // Show loading animation
        loadingContainer.setVisibility(android.view.View.VISIBLE);
        recyclerView.setVisibility(android.view.View.GONE);
        
        // Load apps in background thread to prevent UI blocking
        new Thread(() -> {
            loadAndOrganizeApps();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                // Hide loading animation and show app list
                loadingContainer.setVisibility(android.view.View.GONE);
                recyclerView.setVisibility(android.view.View.VISIBLE);
                adapter.notifyDataSetChanged();
                
                // Optional: Show completion message
                Toast.makeText(this, "Loaded " + (systemApps.size() + userApps.size()) + " apps", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private void loadAndOrganizeApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolvedApps = pm.queryIntentActivities(mainIntent, 0);
        
        // Clear existing lists
        userApps.clear();
        systemApps.clear();

        for (ResolveInfo resolveInfo : resolvedApps) {
            String packageName = resolveInfo.activityInfo.packageName;
            
            // Skip default apps (Phone, Calendar, Clock) - they don't count toward quota
            if (isDefaultApp(packageName) || "com.grepguru.zenlock".equals(packageName)) {
                continue;
            }

            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String appName = resolveInfo.activityInfo.loadLabel(pm).toString();
                boolean isSelected = selectedApps.contains(packageName);
                
                Drawable icon;
                try {
                    icon = pm.getApplicationIcon(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = getDrawable(R.drawable.default_app_icon);
                }

                SelectableAppModel appModel = new SelectableAppModel(packageName, appName, false, isSelected, icon);
                
                // Categorize: User-installed vs System apps
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    userApps.add(appModel);
                } else {
                    systemApps.add(appModel);
                }
                
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Sort both lists alphabetically
        Comparator<SelectableAppModel> alphabetical = (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName());
        Collections.sort(userApps, alphabetical);
        Collections.sort(systemApps, alphabetical);

        // Initialize current list with system apps (default tab)
        currentAppList.clear();
        currentAppList.addAll(systemApps);
    }
    
    private boolean isDefaultApp(String packageName) {
        return defaultApps.contains(packageName);
    }
    
    private void updateSaveButtonText() {
        int selectedCount = selectedApps.size();
        if (selectedCount == 0) {
            saveButton.setText("Save (0/" + MAX_ADDITIONAL_APPS + ")");
        } else {
            saveButton.setText("Save (" + selectedCount + "/" + MAX_ADDITIONAL_APPS + ")");
        }
    }


    private void saveWhitelist() {
        Set<String> finalWhitelist = new HashSet<>();
        finalWhitelist.addAll(defaultApps);
        finalWhitelist.addAll(selectedApps);
        
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("whitelisted_apps", finalWhitelist);
        editor.apply();

        Toast.makeText(this, "Whitelist Updated! Selected " + selectedApps.size() + " additional apps.", Toast.LENGTH_SHORT).show();
        finish();
    }
}

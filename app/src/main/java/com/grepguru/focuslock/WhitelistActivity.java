package com.grepguru.focuslock;

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
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.grepguru.focuslock.model.*;
import com.grepguru.focuslock.ui.adapter.*;
import com.grepguru.focuslock.utils.AppUtils;

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
    
    // Data Collections
    private List<SelectableAppModel> appList = new ArrayList<>();
    private Set<String> defaultApps = new HashSet<>(); // Phone, SMS, Clock (excluded from selection)
    private Set<String> selectedApps = new HashSet<>(); // User's additional app selections

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        recyclerView = findViewById(R.id.whitelistRecyclerView);
        saveButton = findViewById(R.id.saveButton);
        loadingContainer = findViewById(R.id.loadingContainer);

        defaultApps = AppUtils.getMainDefaultApps(this);
        loadUserSelections();

        // Setup RecyclerView first with empty list
        WhitelistAdapter adapter = new WhitelistAdapter(appList, selectedApps, MAX_ADDITIONAL_APPS);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load apps in background to improve responsiveness
        loadAndOrganizeAppsAsync(adapter);

        // Save Button Click Listener
        saveButton.setOnClickListener(v -> {
            saveWhitelist();
        });
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
                Toast.makeText(this, "Loaded " + appList.size() + " apps", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private void loadAndOrganizeApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolvedApps = pm.queryIntentActivities(mainIntent, 0);
        
        List<SelectableAppModel> userInstalledApps = new ArrayList<>();
        List<SelectableAppModel> systemInstalledApps = new ArrayList<>();

        for (ResolveInfo resolveInfo : resolvedApps) {
            String packageName = resolveInfo.activityInfo.packageName;
            
            // Skip default apps (Phone, SMS, Clock) - they don't count toward quota
            if (isDefaultApp(packageName) || "com.grepguru.focuslock".equals(packageName)) {
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
                    userInstalledApps.add(appModel);
                } else {
                    systemInstalledApps.add(appModel);
                }
                
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        Comparator<SelectableAppModel> alphabetical = (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName());
        Collections.sort(userInstalledApps, alphabetical);
        Collections.sort(systemInstalledApps, alphabetical);

        appList.clear();
        appList.addAll(userInstalledApps);
        appList.addAll(systemInstalledApps);
    }
    
    private boolean isDefaultApp(String packageName) {
        return defaultApps.contains(packageName);
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

package com.grepguru.zenlock;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;

import com.grepguru.zenlock.model.*;
import com.grepguru.zenlock.ui.adapter.*;
import com.grepguru.zenlock.utils.AppUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class WhitelistActivity extends AppCompatActivity {

    // Configuration - Easy to modify
    private static final int MAX_ADDITIONAL_APPS = 3;
    
    // UI Components
    private RecyclerView recyclerView;
    private Button saveButton;
    private LinearLayout loadingContainer;
    private TabLayout appTabs;
    
    // Selected Apps Bar Components
    private ImageView[] appIcons = new ImageView[4];
    private CardView[] appSlots = new CardView[4];
    private String[] selectedAppPackages = new String[4]; // Track which app is in which slot
    
    // Data Collections
    private List<SelectableAppModel> systemApps = new ArrayList<>();
    private List<SelectableAppModel> userApps = new ArrayList<>();
    private List<SelectableAppModel> currentAppList = new ArrayList<>(); // Currently displayed list
    private Set<String> defaultApps = new HashSet<>(); // Phone, Calendar, Clock (excluded from selection)
    private Set<String> selectedApps = new HashSet<>(); // User's additional app selections
    private Map<String, SelectableAppModel> appModelMap = new HashMap<>(); // Quick lookup for app info
    
    // Tab constants
    private static final int TAB_SYSTEM = 0;
    private static final int TAB_USER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        initializeViews();
        setupSelectedAppsBar();

        defaultApps = AppUtils.getMainDefaultApps(this);
        loadUserSelections();

        // Setup tabs
        setupTabs();

        // Setup RecyclerView first with empty list
        WhitelistAdapter adapter = new WhitelistAdapter(currentAppList, selectedApps, MAX_ADDITIONAL_APPS);
        adapter.setOnSelectionChangeListener(() -> {
            updateSaveButtonText();
            updateSelectedAppsBar();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load apps in background to improve responsiveness
        loadAndOrganizeAppsAsync(adapter);

        // Save Button Click Listener
        saveButton.setOnClickListener(v -> saveWhitelist());
        
        // Update UI
        updateSaveButtonText();
        updateSelectedAppsBar();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.whitelistRecyclerView);
        saveButton = findViewById(R.id.saveButton);
        loadingContainer = findViewById(R.id.loadingContainer);
        appTabs = findViewById(R.id.appTabs);
    }

    private void setupSelectedAppsBar() {
        // Initialize app icon views and slots
        appIcons[0] = findViewById(R.id.appIcon1);
        appIcons[1] = findViewById(R.id.appIcon2);
        appIcons[2] = findViewById(R.id.appIcon3);
        appIcons[3] = findViewById(R.id.appIcon4);
        
        appSlots[0] = findViewById(R.id.appSlot1);
        appSlots[1] = findViewById(R.id.appSlot2);
        appSlots[2] = findViewById(R.id.appSlot3);
        appSlots[3] = findViewById(R.id.appSlot4);
        
        // Set click listeners for app slots - clicking removes the app
        for (int i = 0; i < 4; i++) {
            final int index = i;
            appSlots[i].setOnClickListener(v -> removeAppFromSlot(index));
        }
    }

    private void removeAppFromSlot(int slotIndex) {
        String packageName = selectedAppPackages[slotIndex];
        if (packageName != null) {
            // Remove from selected apps
            selectedApps.remove(packageName);
            
            // Clear the slot
            selectedAppPackages[slotIndex] = null;
            
            // Update UI
            updateSelectedAppsBar();
            updateSaveButtonText();
            
            // Update the RecyclerView to reflect the change
            WhitelistAdapter adapter = (WhitelistAdapter) recyclerView.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            
            Toast.makeText(this, "App removed from whitelist", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSelectedAppsBar() {
        View selectedAppsCard = findViewById(R.id.selectedAppsCard);
        
        // Show/hide the container with smooth animation
        if (selectedApps.isEmpty()) {
            if (selectedAppsCard.getVisibility() == View.VISIBLE) {
                // Animate out
                ObjectAnimator.ofFloat(selectedAppsCard, "alpha", 1f, 0f).setDuration(200).start();
                selectedAppsCard.postDelayed(() -> selectedAppsCard.setVisibility(View.GONE), 200);
            }
        } else {
            if (selectedAppsCard.getVisibility() == View.GONE) {
                // Animate in
                selectedAppsCard.setVisibility(View.VISIBLE);
                selectedAppsCard.setAlpha(0f);
                ObjectAnimator.ofFloat(selectedAppsCard, "alpha", 0f, 1f).setDuration(300).start();
            }
        }
        
        // Clear all slots first - reset to placeholder state
        for (int i = 0; i < 4; i++) {
            selectedAppPackages[i] = null;
            appIcons[i].setImageResource(R.drawable.ic_add_placeholder);
            appIcons[i].setPadding(12, 12, 12, 12);
            appIcons[i].setScaleType(ImageView.ScaleType.CENTER);
            appSlots[i].setCardBackgroundColor(getColor(R.color.darkBackground)); // Show placeholder background
            appSlots[i].setClickable(false); // Make non-clickable when empty
        }
        
        // Fill slots with selected apps (max 3 even though we have 4 slots)
        int slotIndex = 0;
        for (String packageName : selectedApps) {
            if (slotIndex >= 4) break; // Safety check (though should be max 3)
            
            SelectableAppModel appModel = appModelMap.get(packageName);
            if (appModel != null) {
                selectedAppPackages[slotIndex] = packageName;
                appIcons[slotIndex].setImageDrawable(appModel.getIcon());
                appIcons[slotIndex].setPadding(4, 4, 4, 4); // Small padding to show full icon
                appIcons[slotIndex].setScaleType(ImageView.ScaleType.FIT_CENTER);
                appSlots[slotIndex].setCardBackgroundColor(android.graphics.Color.TRANSPARENT); // Hide placeholder background
                appSlots[slotIndex].setClickable(true); // Make clickable when has app
                slotIndex++;
            }
        }
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
                
                // Update selected apps bar after loading
                updateSelectedAppsBar();
                
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
        appModelMap.clear();

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
                appModelMap.put(packageName, appModel); // Store for quick lookup
                
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

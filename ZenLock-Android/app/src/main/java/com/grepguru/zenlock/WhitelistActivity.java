package com.grepguru.zenlock;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.grepguru.zenlock.utils.WhitelistManager;
import java.util.Locale;
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
    private static final int MAX_ADDITIONAL_APPS = 8;
    
    // UI Components
    private RecyclerView recyclerView;
    private Button saveButton;
    private LinearLayout loadingContainer;
    private TabLayout appTabs;
    private TextView whitelistTitle;
    
    // Search Components
    private ImageView searchIcon;
    private LinearLayout searchBarContainer;
    private EditText searchEditText;
    private ImageView searchCloseIcon;
    private boolean isSearchVisible = false;
    
    private LinearLayout selectedAppsContainer;
    private boolean appsLoaded;

    // Data Collections
    private List<SelectableAppModel> systemApps = new ArrayList<>();
    private List<SelectableAppModel> userApps = new ArrayList<>();
    private List<SelectableAppModel> currentAppList = new ArrayList<>(); // Currently displayed list
    private List<SelectableAppModel> filteredAppList = new ArrayList<>(); // Filtered list for search
    private Set<String> defaultApps = new HashSet<>(); // Phone, Calendar, Clock (excluded from selection)
    private Set<String> selectedApps = new HashSet<>(); // User's additional app selections
    private Map<String, SelectableAppModel> appModelMap = new HashMap<>(); // Quick lookup for app info
    // Set of device default app package names (Phone, Calendar, Clock) - always excluded from quota
    private Set<String> deviceDefaultAppPackages = new HashSet<>();

    // Tab constants
    private static final int TAB_SYSTEM = 0;
    private static final int TAB_USER = 1;

    private boolean isLockActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLocked", false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isLockActive(this)) {
            Intent lockIntent = new Intent(this, LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(lockIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_whitelist);

        initializeViews();
        setupSelectedAppsBar();
        setupSearch();

        // Build set of device default app package names (regardless of toggle state)
        deviceDefaultAppPackages.clear();
        String phonePkg = AppUtils.findMainDialerApp(this);
        String calendarPkg = AppUtils.findMainCalendarApp(this);
        String clockPkg = AppUtils.findMainClockApp(this);
        if (phonePkg != null) deviceDefaultAppPackages.add(phonePkg);
        if (calendarPkg != null) deviceDefaultAppPackages.add(calendarPkg);
        if (clockPkg != null) deviceDefaultAppPackages.add(clockPkg);

        defaultApps = AppUtils.getMainDefaultApps(this); // This is still used for lock screen logic
        if (savedInstanceState != null && savedInstanceState.containsKey("selected_apps")) {
            selectedApps.addAll(savedInstanceState.getStringArrayList("selected_apps"));
        } else {
            loadUserSelections();
        }

        // Setup tabs
        setupTabs();

        // Setup RecyclerView first with empty list
        WhitelistAdapter adapter = new WhitelistAdapter(filteredAppList, selectedApps, MAX_ADDITIONAL_APPS);
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

    @Override
    protected void onResume() {
        super.onResume();
        // Enforce lock: if locked, redirect to lock screen and prevent access
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);
        if (isLocked) {
            Intent lockIntent = new Intent(this, com.grepguru.zenlock.LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && isSearchVisible) {
            // Check if touch is outside search bar and search field is empty
            if (searchEditText.getText().toString().trim().isEmpty()) {
                // Get search bar coordinates
                int[] searchBarLocation = new int[2];
                searchBarContainer.getLocationOnScreen(searchBarLocation);
                
                float x = ev.getRawX();
                float y = ev.getRawY();
                
                // Check if touch is outside the search bar container
                if (x < searchBarLocation[0] || 
                    x > searchBarLocation[0] + searchBarContainer.getWidth() ||
                    y < searchBarLocation[1] || 
                    y > searchBarLocation[1] + searchBarContainer.getHeight()) {
                    
                    hideSearchBar();
                    return true; // Consume the event
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.whitelistRecyclerView);
        saveButton = findViewById(R.id.saveButton);
        loadingContainer = findViewById(R.id.loadingContainer);
        appTabs = findViewById(R.id.appTabs);
        whitelistTitle = findViewById(R.id.whitelistTitle);
        
        // Search views
        searchIcon = findViewById(R.id.searchIcon);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        searchEditText = findViewById(R.id.searchEditText);
        searchCloseIcon = findViewById(R.id.searchCloseIcon);
    }

    private void setupSelectedAppsBar() {
        selectedAppsContainer = findViewById(R.id.selectedAppsContainer);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("selected_apps", new ArrayList<>(selectedApps));
        super.onSaveInstanceState(outState);
    }

    private void setupSearch() {
        // Search icon click - expand search bar
        searchIcon.setOnClickListener(v -> showSearchBar());
        
        // Close search
        searchCloseIcon.setOnClickListener(v -> hideSearchBar());
        
        // Search text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showSearchBar() {
        if (isSearchVisible) return;
        
        isSearchVisible = true;
        searchBarContainer.setVisibility(View.VISIBLE);
        
        // Animate search bar expansion and hide title
        ObjectAnimator.ofFloat(searchIcon, "alpha", 1f, 0f).setDuration(200).start();
        ObjectAnimator.ofFloat(whitelistTitle, "alpha", 1f, 0f).setDuration(200).start();
        ObjectAnimator.ofFloat(searchBarContainer, "alpha", 0f, 1f).setDuration(300).start();
        
        // Hide search icon and make title invisible (but keep layout space)
        searchIcon.postDelayed(() -> searchIcon.setVisibility(View.GONE), 200);
        whitelistTitle.postDelayed(() -> whitelistTitle.setVisibility(View.INVISIBLE), 200);
        
        // Focus on search input and show keyboard
        searchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearchBar() {
        if (!isSearchVisible) return;
        
        isSearchVisible = false;
        
        // Clear search and reset filter
        searchEditText.setText("");
        filterApps("");
        
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        
        // Animate search bar collapse and show title
        ObjectAnimator.ofFloat(searchBarContainer, "alpha", 1f, 0f).setDuration(200).start();
        searchIcon.setVisibility(View.VISIBLE);
        whitelistTitle.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(searchIcon, "alpha", 0f, 1f).setDuration(300).start();
        ObjectAnimator.ofFloat(whitelistTitle, "alpha", 0f, 1f).setDuration(300).start();
        
        // Hide search bar after animation
        searchBarContainer.postDelayed(() -> searchBarContainer.setVisibility(View.GONE), 200);
    }

    private void filterApps(String query) {
        filteredAppList.clear();
        
        if (query.trim().isEmpty()) {
            // No search - show all apps from current tab
            filteredAppList.addAll(currentAppList);
        } else {
            // Filter apps based on search query
            String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
            for (SelectableAppModel app : currentAppList) {
                if (app.getAppName().toLowerCase(Locale.ROOT).contains(lowerQuery) ||
                    app.getPackageName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    filteredAppList.add(app);
                }
            }
        }
        
        // Update adapter with filtered list
        WhitelistAdapter adapter = (WhitelistAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateAppList(filteredAppList);
        }
    }

    private void updateSelectedAppsBar() {
        findViewById(R.id.selectedAppsCard).setVisibility(selectedApps.isEmpty() ? View.GONE : View.VISIBLE);
        selectedAppsContainer.removeAllViews();
        List<String> packages = new ArrayList<>(selectedApps);
        Collections.sort(packages);
        for (String packageName : packages) {
            SelectableAppModel model = appModelMap.get(packageName);
            Chip chip = new Chip(this);
            chip.setText(model == null ? packageName : model.getAppName());
            chip.setTextColor(getColor(R.color.textPrimary));
            chip.setChipBackgroundColorResource(R.color.surface);
            chip.setCloseIconTintResource(R.color.textSecondary);
            if (model != null) chip.setChipIcon(model.getIcon());
            chip.setChipIconVisible(model != null);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.remove_allowed_app, chip.getText()));
            chip.setOnCloseIconClickListener(v -> {
                selectedApps.remove(packageName);
                updateSelectedAppsBar();
                updateSaveButtonText();
                RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                if (adapter != null) adapter.notifyDataSetChanged();
            });
            selectedAppsContainer.addView(chip);
        }
    }

    private void setupTabs() {
        appTabs.addTab(appTabs.newTab().setText("System"));
        appTabs.addTab(appTabs.newTab().setText("Installed"));
        
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
        
        // Apply current search filter to new tab
        String currentQuery = searchEditText.getText().toString();
        filterApps(currentQuery);
    }

    private void loadUserSelections() {
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        Set<String> savedWhitelist = preferences.getStringSet("whitelisted_apps", new HashSet<>());
        
        // Filter out device default apps from saved selections - they don't count toward quota
        for (String packageName : savedWhitelist) {
            if (!deviceDefaultAppPackages.contains(packageName)) {
                selectedApps.add(packageName);
            }
        }
    }
    
    private void loadAndOrganizeAppsAsync(WhitelistAdapter adapter) {
        // Show loading animation
        loadingContainer.setVisibility(android.view.View.VISIBLE);
        recyclerView.setVisibility(android.view.View.GONE);
        
        saveButton.setEnabled(false);
        new Thread(() -> {
            // Build local collections; the worker never mutates UI-owned lists or selections.
            List<SelectableAppModel> loadedSystem = new ArrayList<>();
            List<SelectableAppModel> loadedUser = new ArrayList<>();
            Map<String, SelectableAppModel> loadedModels = new HashMap<>();
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo info : pm.queryIntentActivities(mainIntent, 0)) {
                String packageName = info.activityInfo.packageName;
                if (isDefaultApp(packageName) || getPackageName().equals(packageName)
                        || WhitelistManager.isSecurityRisk(packageName)
                        || AppUtils.isLauncherPackage(this, packageName)
                        || loadedModels.containsKey(packageName)) continue;
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    SelectableAppModel model = new SelectableAppModel(packageName,
                            pm.getApplicationLabel(appInfo).toString(), false, false,
                            pm.getApplicationIcon(appInfo));
                    loadedModels.put(packageName, model);
                    ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ? loadedUser : loadedSystem).add(model);
                } catch (PackageManager.NameNotFoundException ignored) {
                    // An app may be removed while the picker is loading.
                }
            }
            Comparator<SelectableAppModel> alphabetical = (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName());
            Collections.sort(loadedUser, alphabetical);
            Collections.sort(loadedSystem, alphabetical);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                userApps = loadedUser;
                systemApps = loadedSystem;
                appModelMap = loadedModels;
                selectedApps.retainAll(loadedModels.keySet());
                appsLoaded = true;
                loadingContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                switchTab(appTabs.getSelectedTabPosition());
                updateSelectedAppsBar();
                updateSaveButtonText();
                saveButton.setEnabled(true);
            });
        }, "allowed-app-loader").start();
    }

    private boolean isDefaultApp(String packageName) {
        return deviceDefaultAppPackages.contains(packageName);
    }
    
    private void updateSaveButtonText() {
        saveButton.setText(getString(R.string.save_allowed_apps, selectedApps.size(), MAX_ADDITIONAL_APPS));
    }


    private void saveWhitelist() {
        if (!appsLoaded || selectedApps.size() > MAX_ADDITIONAL_APPS) return;
        if (isLockActive(this)) {
            startActivity(new Intent(this, LockScreenActivity.class));
            finish();
            return;
        }
        Set<String> finalWhitelist = new HashSet<>();
        finalWhitelist.addAll(defaultApps);
        finalWhitelist.addAll(selectedApps);
        
        SharedPreferences preferences = getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("whitelisted_apps", finalWhitelist);
        editor.apply();

        Toast.makeText(this, getString(R.string.allowed_apps_saved), Toast.LENGTH_SHORT).show();
        finish();
    }
}

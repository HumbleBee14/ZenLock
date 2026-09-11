package com.grepguru.zenlock.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.BuildConfig;
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.model.AnalyticsModels;
import com.grepguru.zenlock.data.entities.DailyStatsEntity;
import com.grepguru.zenlock.data.entities.WeeklyStatsEntity;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.utils.UsageStatsPermissionManager;
import com.grepguru.zenlock.utils.DailyMobileUsageManager;

import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import android.util.Log;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

public class AnalyticsFragment extends Fragment {

    // Today's stats views
    private TextView todaySessions, todayTime;
    private TextView todayTrendIndicator, todayMobileUsage, todayTimeSaved;
    private ProgressBar sessionsProgress, timeProgress;

    // Expandable sections
    private LinearLayout recentSessionsHeader, recentSessionsContent;
    private ImageView recentSessionsExpandIcon;
    private boolean isRecentSessionsExpanded = false;

    // Recent sessions views
    private TextView recentSessionsText;
    private LinearLayout recentSessionsContainer;
    private AnalyticsManager analyticsManager;
    private DailyMobileUsageManager dailyMobileUsageManager;
    
    // Usage permission banner
    private TextView usagePermissionBanner;
    
    private TabLayout trendsTabs;
    private TextView currentFocusTime, previousFocusTime, currentMobileUsage, previousMobileUsage;
    private TextView focusChange, mobileChange;
    private TextView currentFocusLabel, previousFocusLabel, currentMobileLabel, previousMobileLabel, chartCaption;
    private CombinedChart trendsChart;
    private boolean showingMonthly = false;
    private final PeriodData weeklyData = new PeriodData();
    private final PeriodData monthlyData = new PeriodData();

    private static class PeriodData {
        long currentFocusMs, previousFocusMs, currentMobileMs, previousMobileMs;
        boolean statsLoaded;
        CombinedData chartData;
        List<String> chartLabels;
    }

    public AnalyticsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize analytics manager
        analyticsManager = new AnalyticsManager(requireContext());
        
        // Initialize daily mobile usage manager
        dailyMobileUsageManager = new DailyMobileUsageManager(requireContext());

        // Initialize UI components
        initializeViews(view);

        // Setup expandable sections
        setupExpandableSections();
        setupTrendsTabs();
        
        // Setup usage permission banner
        setupUsagePermissionBanner();

        // Pre-populate recent mobile usage data and store yesterday's data
        prePopulateMobileUsageData();
        
        // Load and display analytics data
        loadAnalyticsData();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Enforce lock: if locked, redirect to lock screen and prevent access
        SharedPreferences preferences = requireActivity().getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        boolean isLocked = preferences.getBoolean("isLocked", false);
        if (isLocked) {
            Intent lockIntent = new Intent(requireContext(), com.grepguru.zenlock.LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
            requireActivity().finish();
            return;
        }

        // Check permission status when returning from settings (single check)
        checkUsageStatsPermission();
        
        // Check if user granted permission while away
        checkPermissionStatusOnResume();
        
        // Force refresh mobile usage data every time analytics page is opened
        refreshMobileUsageData();
    }

    private void initializeViews(View view) {

        // Today's stats
        todaySessions = view.findViewById(R.id.todaySessions);
        todayTime = view.findViewById(R.id.todayTime);
        todayTrendIndicator = view.findViewById(R.id.todayTrendIndicator);
        todayMobileUsage = view.findViewById(R.id.todayMobileUsage);
        todayTimeSaved = view.findViewById(R.id.todayTimeSaved);

        // Progress bars
        sessionsProgress = view.findViewById(R.id.sessionsProgress);
        timeProgress = view.findViewById(R.id.timeProgress);


        // Recent sessions expandable section
        recentSessionsHeader = view.findViewById(R.id.recentSessionsHeader);
        recentSessionsContent = view.findViewById(R.id.recentSessionsContent);
        recentSessionsExpandIcon = view.findViewById(R.id.recentSessionsExpandIcon);

        // Recent sessions content
        recentSessionsText = view.findViewById(R.id.recentSessionsText);
        recentSessionsContainer = view.findViewById(R.id.recentSessionsContainer);
        
        // Usage permission banner
        usagePermissionBanner = view.findViewById(R.id.usagePermissionBanner);
        
        trendsTabs = view.findViewById(R.id.trendsTabs);
        currentFocusTime = view.findViewById(R.id.currentFocusTime);
        previousFocusTime = view.findViewById(R.id.previousFocusTime);
        currentMobileUsage = view.findViewById(R.id.currentMobileUsage);
        previousMobileUsage = view.findViewById(R.id.previousMobileUsage);
        focusChange = view.findViewById(R.id.focusChange);
        mobileChange = view.findViewById(R.id.mobileChange);
        currentFocusLabel = view.findViewById(R.id.currentFocusLabel);
        previousFocusLabel = view.findViewById(R.id.previousFocusLabel);
        currentMobileLabel = view.findViewById(R.id.currentMobileLabel);
        previousMobileLabel = view.findViewById(R.id.previousMobileLabel);
        chartCaption = view.findViewById(R.id.chartCaption);
        trendsChart = view.findViewById(R.id.trendsChart);

        setupChart(trendsChart, 8);
    }

    private void setupExpandableSections() {
        // Recent Sessions expandable
        recentSessionsHeader.setOnClickListener(v -> toggleRecentSessions());
    }

    private void setupTrendsTabs() {
        trendsTabs.addTab(trendsTabs.newTab().setText("Weekly"));
        trendsTabs.addTab(trendsTabs.newTab().setText("Monthly"));
        trendsTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingMonthly = tab.getPosition() == 1;
                bindActivePeriod();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        bindActivePeriod();
    }

    private void bindActivePeriod() {
        if (!isAdded()) return;
        PeriodData data = showingMonthly ? monthlyData : weeklyData;
        String current = showingMonthly ? "THIS MONTH" : "THIS WEEK";
        String previous = showingMonthly ? "LAST MONTH" : "LAST WEEK";
        currentFocusLabel.setText(current);
        previousFocusLabel.setText(previous);
        currentMobileLabel.setText(current);
        previousMobileLabel.setText(previous);
        chartCaption.setText(showingMonthly ? "Last 30 days" : "Last 7 days");

        if (data.statsLoaded) {
            currentFocusTime.setText(formatTime(data.currentFocusMs / (60 * 1000)));
            previousFocusTime.setText(formatTime(data.previousFocusMs / (60 * 1000)));
            currentMobileUsage.setText(formatTime(data.currentMobileMs / (60 * 1000)));
            previousMobileUsage.setText(formatTime(data.previousMobileMs / (60 * 1000)));
            int currentDays = showingMonthly ? getDaysElapsedThisMonth() : getDaysElapsedThisWeek();
            int previousDays = showingMonthly ? getDaysInLastMonth() : 7;
            applyChangeLabel(focusChange, computeNormalizedChange(data.currentFocusMs, currentDays, data.previousFocusMs, previousDays), true);
            applyChangeLabel(mobileChange, computeNormalizedChange(data.currentMobileMs, currentDays, data.previousMobileMs, previousDays), false);
        } else {
            currentFocusTime.setText("0m");
            previousFocusTime.setText("0m");
            currentMobileUsage.setText("0m");
            previousMobileUsage.setText("0m");
            focusChange.setText("--");
            mobileChange.setText("--");
        }

        if (data.chartData != null) {
            trendsChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(data.chartLabels));
            trendsChart.setData(data.chartData);
            trendsChart.invalidate();
        } else {
            trendsChart.clear();
        }
    }

    private void toggleRecentSessions() {
        isRecentSessionsExpanded = !isRecentSessionsExpanded;

        if (isRecentSessionsExpanded) {
            // Expand
            recentSessionsContent.setVisibility(View.VISIBLE);
            recentSessionsContent.setAlpha(0f);
            recentSessionsContent.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        } else {
            // Collapse
            recentSessionsContent.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> recentSessionsContent.setVisibility(View.GONE))
                    .start();
        }

        // Rotate icon
        ObjectAnimator rotation = ObjectAnimator.ofFloat(recentSessionsExpandIcon, "rotation",
                isRecentSessionsExpanded ? 180f : 0f, isRecentSessionsExpanded ? 0f : 180f);
        rotation.setDuration(300);
        rotation.start();
    }

    private void setupUsagePermissionBanner() {
        usagePermissionBanner.setOnClickListener(v -> {
            // Request usage stats permission
            analyticsManager.requestUsageStatsPermission();
        });
    }

    private void prePopulateMobileUsageData() {
        // Pre-populate recent mobile usage data in background
        new Thread(() -> {
            try {
                dailyMobileUsageManager.prePopulateRecentData();
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error pre-populating mobile usage data", e);
            }
        }).start();
    }
    
    private void loadAnalyticsData() {
        // Check usage stats permission first
        checkUsageStatsPermission();
        
        // Load real analytics data using LiveData
        loadTodayStats();
        loadWeeklyStats();
        loadMonthlyStats();
        loadWeeklyChart();
        loadMonthlyChart();
        loadRecentSessions();
    }

    // ---- Charts ----
    private void setupChart(CombinedChart chart, int maxLabels) {
        if (chart == null) return;
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);
        
        // Set background color for better text visibility
        chart.setBackgroundColor(requireContext().getColor(R.color.surface));
        
        Description d = new Description();
        d.setText("");
        chart.setDescription(d);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        
        // Configure X-axis for better text visibility
        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setLabelCount(Math.min(maxLabels, 8), false);
        x.setTextColor(requireContext().getColor(R.color.textPrimary));
        x.setTextSize(12f);
        
        // Configure Y-axis for better text visibility
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setTextColor(requireContext().getColor(R.color.textPrimary));
        chart.getAxisLeft().setTextSize(12f);
        chart.getAxisLeft().setGridColor(requireContext().getColor(R.color.textSecondary));
        chart.getAxisLeft().setAxisLineColor(requireContext().getColor(R.color.textSecondary));
    }

    private void loadWeeklyChart() {
        if (trendsChart == null) return;
        new Thread(() -> {
            try {
                // Rolling window: today (index 0) back to previous 7 days => total 8 points
                long now = System.currentTimeMillis();
                java.util.Calendar cal = java.util.Calendar.getInstance();
                String endDate = AnalyticsManager.formatDate(now);
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
                String startDate = AnalyticsManager.formatDate(cal.getTimeInMillis());

                List<com.grepguru.zenlock.data.entities.DailyStatsEntity> days = analyticsManager.getDailyStatsRangeSync(startDate, endDate);
                // Build a map date->focusMs for quick lookup
                java.util.Map<String, Long> focusByDate = new java.util.HashMap<>();
                for (com.grepguru.zenlock.data.entities.DailyStatsEntity d : days) {
                    focusByDate.put(d.date, d.totalFocusTime);
                }

                // Prepare entries (X from 0..7 where 0=today)
                List<BarEntry> focusEntries = new java.util.ArrayList<>();
                List<Entry> mobileEntries = new java.util.ArrayList<>();
                List<String> labels = new java.util.ArrayList<>();

                java.util.Calendar walk = java.util.Calendar.getInstance();
                for (int i = 7; i >= 0; i--) {
                    java.util.Calendar c = (java.util.Calendar) walk.clone();
                    c.add(java.util.Calendar.DAY_OF_YEAR, -i);
                    String ds = AnalyticsManager.formatDate(c.getTimeInMillis());
                    long focusMs = focusByDate.getOrDefault(ds, 0L);
                    // Use DailyMobileUsageManager for efficient data retrieval
                    long mobileMs = dailyMobileUsageManager.getMobileUsageForDate(ds);
                    int x = 7 - i; // 0..7
                    focusEntries.add(new BarEntry(x, msToHoursFloat(focusMs))); // bars in hours
                    mobileEntries.add(new Entry(x, msToHoursFloat(mobileMs)));  // line in hours
                    labels.add(shortDayLabel(c));
                }

                BarDataSet barSet = new BarDataSet(focusEntries, "Focus (h)");
                barSet.setColor(requireContext().getColor(R.color.secondary)); // orange-like
                barSet.setDrawValues(false);
                BarData barData = new BarData(barSet);
                barData.setBarWidth(0.45f);

                LineDataSet lineSet = new LineDataSet(mobileEntries, "Mobile (h)");
                lineSet.setColor(requireContext().getColor(R.color.warning));
                lineSet.setCircleColor(requireContext().getColor(R.color.warning));
                lineSet.setLineWidth(1.8f);
                lineSet.setDrawValues(false);
                lineSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                LineData lineData = new LineData(lineSet);

                CombinedData combinedData = new CombinedData();
                combinedData.setData(barData);
                combinedData.setData(lineData);

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        weeklyData.chartData = combinedData;
                        weeklyData.chartLabels = labels;
                        if (!showingMonthly) bindActivePeriod();
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error loading weekly chart", e);
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (trendsChart != null && isAdded()) {
                            trendsChart.setNoDataText("No data yet");
                        }
                    });
                }
            }
        }).start();
    }

    private void loadMonthlyChart() {
        if (trendsChart == null) return;
        new Thread(() -> {
            try {
                // Rolling window: today back 30 days => 31 points
                long now = System.currentTimeMillis();
                java.util.Calendar cal = java.util.Calendar.getInstance();
                String endDate = AnalyticsManager.formatDate(now);
                cal.add(java.util.Calendar.DAY_OF_YEAR, -30);
                String startDate = AnalyticsManager.formatDate(cal.getTimeInMillis());

                List<com.grepguru.zenlock.data.entities.DailyStatsEntity> days = analyticsManager.getDailyStatsRangeSync(startDate, endDate);
                java.util.Map<String, Long> focusByDate = new java.util.HashMap<>();
                for (com.grepguru.zenlock.data.entities.DailyStatsEntity d : days) {
                    focusByDate.put(d.date, d.totalFocusTime);
                }

                List<BarEntry> focusEntries = new java.util.ArrayList<>();
                List<Entry> mobileEntries = new java.util.ArrayList<>();
                List<String> labels = new java.util.ArrayList<>();

                java.util.Calendar walk = java.util.Calendar.getInstance();
                for (int i = 30; i >= 0; i--) {
                    java.util.Calendar c = (java.util.Calendar) walk.clone();
                    c.add(java.util.Calendar.DAY_OF_YEAR, -i);
                    String ds = AnalyticsManager.formatDate(c.getTimeInMillis());
                    long focusMs = focusByDate.getOrDefault(ds, 0L);
                    // Use DailyMobileUsageManager for efficient data retrieval
                    long mobileMs = dailyMobileUsageManager.getMobileUsageForDate(ds);
                    int x = 30 - i; // 0..30
                    
                    
                    focusEntries.add(new BarEntry(x, msToHoursFloat(focusMs)));
                    mobileEntries.add(new Entry(x, msToHoursFloat(mobileMs)));
                    labels.add(dayOfMonthLabel(c));
                }

                BarDataSet barSet = new BarDataSet(focusEntries, "Focus (h)");
                barSet.setColor(requireContext().getColor(R.color.secondary));
                barSet.setDrawValues(false);
                BarData barData = new BarData(barSet);
                barData.setBarWidth(0.4f);

                LineDataSet lineSet = new LineDataSet(mobileEntries, "Mobile (h)");
                lineSet.setColor(requireContext().getColor(R.color.warning));
                lineSet.setCircleColor(requireContext().getColor(R.color.warning));
                lineSet.setLineWidth(1.6f);
                lineSet.setDrawValues(false);
                lineSet.setMode(LineDataSet.Mode.LINEAR);
                LineData lineData = new LineData(lineSet);

                CombinedData combinedData = new CombinedData();
                combinedData.setData(barData);
                combinedData.setData(lineData);

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        monthlyData.chartData = combinedData;
                        monthlyData.chartLabels = labels;
                        if (showingMonthly) bindActivePeriod();
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error loading monthly chart", e);
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (trendsChart != null && isAdded()) {
                            trendsChart.setNoDataText("No data yet");
                        }
                    });
                }
            }
        }).start();
    }

    private static class IndexAxisValueFormatter extends ValueFormatter {
        private final List<String> labels;
        IndexAxisValueFormatter(List<String> labels) { this.labels = labels; }
        @Override public String getFormattedValue(float value) {
            int idx = (int) value;
            return (idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
        }
    }

    private float msToHoursFloat(long ms) { return (float)(ms / 3600000.0); }
    private String shortDayLabel(java.util.Calendar c) {
        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        int dow = c.get(java.util.Calendar.DAY_OF_WEEK); // 1..7
        return days[dow - 1];
    }
    private String dayOfMonthLabel(java.util.Calendar c) { return String.valueOf(c.get(java.util.Calendar.DAY_OF_MONTH)); }
    
    private void checkUsageStatsPermission() {
        // Single permission check to avoid redundant calls
        boolean hasPermission = analyticsManager.hasUsageStatsPermission();
        
        if (usagePermissionBanner != null) {
            usagePermissionBanner.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        }
    }
    
    
    private void loadTodayStats() {
        // Observe today's stats with LiveData
        analyticsManager.getTodayStatsLive().observe(getViewLifecycleOwner(), todayStats -> {
            if (todayStats != null) {
                // Get yesterday's stats for comparison
                DailyStatsEntity yesterdayStats = analyticsManager.getYesterdayStats();
                
                // Update today's stats with real data
        updateTodayStats(
                    todayStats.totalSessions,
                    todayStats.totalFocusTime / (1000 * 60), // Convert to minutes
                    (int) todayStats.avgFocusScore,
                    yesterdayStats
                );
            }
            // Don't show default values - let the UI show existing data until real data loads
        });
    }
    
    private void loadWeeklyStats() {
        new Thread(() -> {
            try {
                long thisWeekFocusMs = analyticsManager.getThisWeekFocusTime();
                long thisWeekMobileMs = analyticsManager.getThisWeekMobileUsage();
                long lastWeekFocusMs = analyticsManager.getLastWeekFocusTime();
                long lastWeekMobileMs = analyticsManager.getLastWeekMobileUsage();

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        weeklyData.currentFocusMs = thisWeekFocusMs;
                        weeklyData.previousFocusMs = lastWeekFocusMs;
                        weeklyData.currentMobileMs = thisWeekMobileMs;
                        weeklyData.previousMobileMs = lastWeekMobileMs;
                        weeklyData.statsLoaded = true;
                        if (!showingMonthly) bindActivePeriod();
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error loading weekly stats", e);
            }
        }).start();
    }

    private void loadMonthlyStats() {
        new Thread(() -> {
            try {
                long thisMonthFocusMs = analyticsManager.getThisMonthFocusTime();
                long thisMonthMobileMs = analyticsManager.getThisMonthMobileUsage();
                long lastMonthFocusMs = analyticsManager.getLastMonthFocusTime();
                long lastMonthMobileMs = analyticsManager.getLastMonthMobileUsage();

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        monthlyData.currentFocusMs = thisMonthFocusMs;
                        monthlyData.previousFocusMs = lastMonthFocusMs;
                        monthlyData.currentMobileMs = thisMonthMobileMs;
                        monthlyData.previousMobileMs = lastMonthMobileMs;
                        monthlyData.statsLoaded = true;
                        if (showingMonthly) bindActivePeriod();
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error loading monthly stats", e);
            }
        }).start();
    }
    
    private void loadRecentSessions() {
        // Observe recent sessions with LiveData
        analyticsManager.getRecentSessionsLive(10).observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null && !sessions.isEmpty()) {
                updateRecentSessionsWithData(sessions);
            } else {
                updateRecentSessionsEmpty();
            }
        });
    }

    private int calculateFocusScore(AnalyticsModels.DailyStats todayStats) {
        // Calculate a focus score based on sessions completed, time focused, and completion rate
        double sessionScore = Math.min(todayStats.getTotalSessions() * 10, 40); // Max 40 points
        double timeScore = Math.min(todayStats.getTotalFocusTime() / (1000 * 60 * 8), 30); // Max 30 points (8 hours = 30 points)
        double completionScore = todayStats.getCompletionRate() * 0.3; // Max 30 points
        
        return (int) Math.min(sessionScore + timeScore + completionScore, 100);
    }

    private int calculateWeeklyGoalProgress(AnalyticsModels.PeriodSummary weekStats) {
        // Assume weekly goal is 20 hours (1200 minutes)
        long weeklyGoalMinutes = 1200;
        long actualMinutes = weekStats.getTotalFocusTime() / (1000 * 60);
        return (int) Math.min((actualMinutes * 100) / weeklyGoalMinutes, 100);
    }

    private void updateTodayStats(int sessions, long focusTimeMinutes, int focusScore, DailyStatsEntity yesterdayStats) {
        if (todaySessions != null) todaySessions.setText(String.valueOf(sessions));

        if (todayTime != null) {
            if (focusTimeMinutes < 60) {
                todayTime.setText(focusTimeMinutes + "m");
            } else {
                int hours = (int) (focusTimeMinutes / 60);
                int minutes = (int) (focusTimeMinutes % 60);
                todayTime.setText(hours + "h " + minutes + "m");
            }
        }



        // Update progress bars
        if (sessionsProgress != null) {
            sessionsProgress.setProgress(Math.min(sessions, 10)); // Goal: 10 sessions
        }
        if (timeProgress != null) {
            timeProgress.setProgress((int) Math.min(focusTimeMinutes, 480)); // Goal: 8 hours
        }


        // Update trend indicator with today vs yesterday comparison
        if (todayTrendIndicator != null) {
            updateTrendIndicator(sessions, focusTimeMinutes, focusScore, yesterdayStats);
        }
        
        // Update mobile usage and time saved
        updateMobileUsageDisplay(focusTimeMinutes);
    }
    
    private void refreshMobileUsageData() {
        // Force refresh mobile usage data every time analytics page is opened
        new Thread(() -> {
            try {
                // Use DailyMobileUsageManager for efficient data retrieval
                String todayDate = AnalyticsManager.formatDate(System.currentTimeMillis());
                long mobileUsageMs = dailyMobileUsageManager.getMobileUsageForDate(todayDate);
                // Log.d("AnalyticsFragment", "Refreshed mobile usage: " + mobileUsageMs + "ms");
                
                // Update UI on main thread
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return; // Double check fragment is still attached
                        if (todayMobileUsage != null) {
                            if (mobileUsageMs > 0) {
                                todayMobileUsage.setText(formatTime(mobileUsageMs / (60 * 1000)));
                            } else {
                                todayMobileUsage.setText("0m");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error refreshing mobile usage", e);
            }
        }).start();
    }
    
    private void updateMobileUsageDisplay(long focusTimeMinutes) {
        // Get real mobile usage data using DailyMobileUsageManager for efficiency
        new Thread(() -> {
            try {
                // Use DailyMobileUsageManager for efficient data retrieval
                String todayDate = AnalyticsManager.formatDate(System.currentTimeMillis());
                long mobileUsageMs = dailyMobileUsageManager.getMobileUsageForDate(todayDate);
                Log.d("AnalyticsFragment", "Mobile usage from tracker: " + mobileUsageMs + "ms");
                
                // Update UI on main thread
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return; // Double check fragment is still attached
                        // Update mobile usage
                        if (todayMobileUsage != null) {
                            if (mobileUsageMs > 0) {
                                todayMobileUsage.setText(formatTime(mobileUsageMs / (60 * 1000)));
                            } else {
                                todayMobileUsage.setText("0m");
                            }
                        }
                        
                        // Calculate and update time saved in hours
                        if (todayTimeSaved != null) {
                            // Time saved = focus time (actual hours focused)
                            // If no sessions, show 0
                            if (focusTimeMinutes > 0) {
                                todayTimeSaved.setText(formatTime(focusTimeMinutes));
                            } else {
                                todayTimeSaved.setText("0m");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error updating mobile usage", e);
                // Show error state
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return; // Double check fragment is still attached
                        if (todayMobileUsage != null) {
                            todayMobileUsage.setText("Error");
                        }
                    });
                }
            }
        }).start();
    }
    
    private void updateTrendIndicator(int todaySessions, long todayFocusTime, int todayFocusScore, DailyStatsEntity yesterdayStats) {
        if (yesterdayStats == null) {
            // No comparison data available
            todayTrendIndicator.setText("📊 First Day");
            todayTrendIndicator.setTextColor(requireContext().getColor(R.color.textSecondary));
            return;
        }
        
        // Calculate percentage change in focus time
        long yesterdayFocusTime = yesterdayStats.totalFocusTime / (1000 * 60); // Convert to minutes
        double changePercentage = 0;
        
        if (yesterdayFocusTime > 0) {
            changePercentage = ((double) (todayFocusTime - yesterdayFocusTime) / yesterdayFocusTime) * 100;
        } else if (todayFocusTime > 0) {
            changePercentage = 100; // 100% increase from 0
        }
        
        // Update trend indicator with proper color coding
        if (changePercentage > 10) {
            // Increase in focus time (good) - show in green
            todayTrendIndicator.setText(String.format("↗️ +%.0f%%", changePercentage));
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.success));
        } else if (changePercentage > -10) {
            // Similar performance
            todayTrendIndicator.setText("→ Similar");
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.textSecondary));
            } else {
            // Decrease in focus time (bad) - show in red
            todayTrendIndicator.setText(String.format("↘️ %.0f%%", changePercentage));
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.warning));
        }
    }

    
    
    // ---- Helpers for normalized comparisons ----
    private int getDaysElapsedThisWeek() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK); // Sun=1..Sat=7
        // Convert to Monday-based index: Mon=1..Sun=7
        int daysFromMon = ((dow - java.util.Calendar.MONDAY + 7) % 7);
        return daysFromMon + 1; // inclusive of today
    }

    private int getDaysElapsedThisMonth() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.DAY_OF_MONTH); // 1..31 (inclusive to today)
    }

    private int getDaysInLastMonth() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -1);
        return cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
    }

    private Double computeNormalizedChange(long currentTotalMs, int currentDays, long previousTotalMs, int previousDays) {
        if (previousDays <= 0) return null;
        double prevAvg = previousDays > 0 ? (previousTotalMs / (double) previousDays) : 0d;
        if (prevAvg == 0d) return null; // show "--" when no baseline
        double currAvg = currentDays > 0 ? (currentTotalMs / (double) currentDays) : 0d;
        return ((currAvg - prevAvg) / prevAvg) * 100.0;
    }

    private void applyChangeLabel(TextView label, Double changePct, boolean higherIsGood) {
        if (changePct == null) {
            label.setText("--");
            label.setTextColor(requireContext().getColor(R.color.textSecondary));
            return;
        }
        double cp = changePct;
        // Round to nearest whole percent for UI
        String text = String.format(Locale.getDefault(), (cp >= 0 ? "+%.0f%%" : "%.0f%%"), cp);
        label.setText(text);

        // Thresholds similar to previous logic
        if (cp > 5) {
            label.setTextColor(requireContext().getColor(higherIsGood ? R.color.success : R.color.warning));
        } else if (cp < -5) {
            label.setTextColor(requireContext().getColor(higherIsGood ? R.color.warning : R.color.success));
        } else {
            label.setText("0%");
            label.setTextColor(requireContext().getColor(R.color.textSecondary));
        }
    }
    
    private void updateRecentSessionsWithData(List<SessionEntity> sessions) {
        if (recentSessionsText != null) {
            recentSessionsText.setVisibility(View.GONE);
        }
        if (recentSessionsContainer != null) {
            recentSessionsContainer.setVisibility(View.VISIBLE);
            recentSessionsContainer.removeAllViews();
            
            // Add real session data (show all up to 10 sessions)
            int displayCount = Math.min(sessions.size(), 10);
            for (int i = 0; i < displayCount; i++) {
                SessionEntity session = sessions.get(i);
                View sessionView = createRealSessionView(session);
                recentSessionsContainer.addView(sessionView);
            }
        }
    }
    
    private void updateRecentSessionsEmpty() {
        if (recentSessionsText != null) {
            recentSessionsText.setText("No sessions yet. Start your first focus session!");
            recentSessionsText.setVisibility(View.VISIBLE);
        }
        if (recentSessionsContainer != null) {
            recentSessionsContainer.setVisibility(View.GONE);
        }
    }
    
    private View createSessionView(SessionEntity session) {
        // Create a simple session item view
        // For now, return null to keep existing mock data
        // This would be implemented with proper session item layout
        return null;
    }

    
    private String formatTime(long minutes) {
        if (minutes < 60) {
            return minutes + "m";
        } else {
            int hours = (int) (minutes / 60);
            int mins = (int) (minutes % 60);
            if (mins == 0) {
                return hours + "h";
            } else {
                return hours + "h " + mins + "m";
            }
        }
    }
    
    private void updateRecentSessions() {
        List<AnalyticsModels.FocusSession> recentSessions = analyticsManager.getRecentSessions(10);
        
        if (recentSessions.isEmpty()) {
            if (recentSessionsText != null) {
                recentSessionsText.setText("No sessions yet. Start your first focus session!");
                recentSessionsText.setVisibility(View.VISIBLE);
            }
            if (recentSessionsContainer != null) {
                recentSessionsContainer.setVisibility(View.GONE);
            }
        } else {
            if (recentSessionsText != null) {
                recentSessionsText.setVisibility(View.GONE);
            }
            if (recentSessionsContainer != null) {
                recentSessionsContainer.setVisibility(View.VISIBLE);
                displayRecentSessions(recentSessions);
            }
        }
    }
    
    private void displayRecentSessions(List<AnalyticsModels.FocusSession> sessions) {
        if (recentSessionsContainer == null) return;
        
        recentSessionsContainer.removeAllViews();
        
        for (AnalyticsModels.FocusSession session : sessions) {
            View sessionView = createSessionView(session);
            recentSessionsContainer.addView(sessionView);
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    private View createSessionView(AnalyticsModels.FocusSession session) {
        LinearLayout sessionItem = new LinearLayout(requireContext());
        sessionItem.setOrientation(LinearLayout.HORIZONTAL);
        sessionItem.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        sessionItem.setBackgroundResource(R.drawable.glass_card_inner);
        
        // Session info
        LinearLayout infoLayout = new LinearLayout(requireContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        
        // Session time
        TextView timeText = new TextView(requireContext());
        timeText.setText(formatSessionTime(session.getStartTime()));
        timeText.setTextSize(16);
        timeText.setTextColor(requireContext().getColor(R.color.textPrimary));
        timeText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Session duration
        TextView durationText = new TextView(requireContext());
        durationText.setText(formatDuration(session.getActualDuration()));
        durationText.setTextSize(14);
        durationText.setTextColor(requireContext().getColor(R.color.textSecondary));
        
        // Status indicator
        TextView statusText = new TextView(requireContext());
        statusText.setText(session.isCompleted() ? "✓ Completed" : "✗ Interrupted");
        statusText.setTextSize(12);
        statusText.setTextColor(requireContext().getColor(session.isCompleted() ? R.color.success : R.color.error));
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);

        infoLayout.addView(timeText);
        infoLayout.addView(durationText);
        infoLayout.addView(statusText);

        sessionItem.addView(infoLayout);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        sessionItem.setLayoutParams(params);

        return sessionItem;
    }
    
    
    private String formatDuration(long milliseconds) {
        long minutes = milliseconds / (1000 * 60);
        if (minutes < 60) {
            return minutes + " minutes";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "m";
        }
    }
    


    // Methods to be called when session data changes
    public void onSessionCompleted(long duration, boolean completed) {
        // This will be called when a focus session ends
        // Refresh the data to show updated stats
        loadAnalyticsData();
    }

    public void refreshAnalytics() {
        // Refresh all analytics data
        loadAnalyticsData();
    }

    
    private void checkPermissionStatusOnResume() {
        if (analyticsManager.hasUsageStatsPermission()) {
            // Permission granted! Reset permission state
            UsageStatsPermissionManager.resetPermissionState(requireContext());
            
            // Mobile usage will be updated automatically by AnalyticsManager
            
        } else if (UsageStatsPermissionManager.shouldShowPermissionRequest(requireContext())) {
            // User came back but didn't grant permission
            UsageStatsPermissionManager.markPermissionDenied(requireContext());
        }
    }
    
    
    private View createRealSessionView(SessionEntity session) {
        // Create a session item view using the existing layout pattern
        LinearLayout sessionItem = new LinearLayout(requireContext());
        sessionItem.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        sessionItem.setOrientation(LinearLayout.HORIZONTAL);
        sessionItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
        sessionItem.setBackgroundResource(R.drawable.glass_card_inner);
        sessionItem.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        LinearLayout.LayoutParams marginParams = (LinearLayout.LayoutParams) sessionItem.getLayoutParams();
        marginParams.bottomMargin = dpToPx(8);
        sessionItem.setLayoutParams(marginParams);
        
        // Session status icon
        TextView statusIcon = new TextView(requireContext());
        statusIcon.setText(session.completed ? "✓" : (session.isPartial() ? "~" : "✗"));
        statusIcon.setTextSize(16);
        statusIcon.setTextColor(requireContext().getColor(
            session.completed ? R.color.success : 
            session.isPartial() ? R.color.secondary : R.color.warning
        ));
        statusIcon.setGravity(android.view.Gravity.CENTER);
        statusIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) statusIcon.getLayoutParams();
        iconParams.rightMargin = dpToPx(12);
        statusIcon.setLayoutParams(iconParams);
        sessionItem.addView(statusIcon);
        
        // Session info section
        LinearLayout infoLayout = new LinearLayout(requireContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        
        TextView timeText = new TextView(requireContext());
        timeText.setText(formatSessionTime(session.startTime));
        timeText.setTextSize(16);
        timeText.setTextColor(requireContext().getColor(R.color.textPrimary));
        infoLayout.addView(timeText);
        
        TextView sourceText = new TextView(requireContext());
        sourceText.setText(session.source.startsWith("schedule:") ? 
            session.source.substring(9) : "Focus Session");
        sourceText.setTextSize(12);
        sourceText.setTextColor(requireContext().getColor(R.color.textSecondary));
        infoLayout.addView(sourceText);
        
        sessionItem.addView(infoLayout);
        
        // Duration and status section
        LinearLayout durationLayout = new LinearLayout(requireContext());
        durationLayout.setOrientation(LinearLayout.VERTICAL);
        durationLayout.setGravity(android.view.Gravity.END);
        durationLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        TextView durationText = new TextView(requireContext());
        durationText.setText(session.getFormattedDuration());
        durationText.setTextSize(16);
        durationText.setTypeface(null, android.graphics.Typeface.BOLD);
        durationText.setTextColor(requireContext().getColor(R.color.textPrimary));
        durationLayout.addView(durationText);
        
        TextView statusText = new TextView(requireContext());
        statusText.setText(session.completed ? "Completed" : 
            session.isPartial() ? "Partial" : "Interrupted");
        statusText.setTextSize(12);
        statusText.setTextColor(requireContext().getColor(
            session.completed ? R.color.success : 
            session.isPartial() ? R.color.secondary : R.color.warning
        ));
        durationLayout.addView(statusText);
        
        sessionItem.addView(durationLayout);
        
        return sessionItem;
    }
    
    private String formatSessionTime(long timestamp) {
        Date date = new Date(timestamp);
        Date today = new Date();
        Date yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        
        if (isSameDay(date, today)) {
            return "Today, " + timeFormat.format(date);
        } else if (isSameDay(date, yesterday)) {
            return "Yesterday, " + timeFormat.format(date);
        } else {
            return dateFormat.format(date) + ", " + timeFormat.format(date);
        }
    }
    
    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return fmt.format(date1).equals(fmt.format(date2));
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup the daily mobile usage manager
        if (dailyMobileUsageManager != null) {
            dailyMobileUsageManager.shutdown();
        }
    }

}

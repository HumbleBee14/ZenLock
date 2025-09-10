package com.grepguru.zenlock.fragments;

import android.animation.ObjectAnimator;
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
import com.grepguru.zenlock.utils.AnalyticsManager;
import com.grepguru.zenlock.model.AnalyticsModels;
import com.grepguru.zenlock.data.entities.DailyStatsEntity;
import com.grepguru.zenlock.data.entities.WeeklyStatsEntity;
import com.grepguru.zenlock.data.entities.SessionEntity;
import com.grepguru.zenlock.utils.UsageStatsPermissionManager;

import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import android.util.Log;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

public class AnalyticsFragment extends Fragment {

    // Today's stats views
    private TextView todaySessions, todayTime, todayFocusScore;
    private TextView todayTrendIndicator, todayMobileUsage, todayTimeSaved;
    private ProgressBar sessionsProgress, timeProgress, focusScoreProgress;

    // Expandable sections
    private LinearLayout focusTrendsHeader, focusTrendsContent;
    private ImageView focusTrendsExpandIcon;
    private boolean isFocusTrendsExpanded = false;

    private LinearLayout monthlyTrendsHeader, monthlyTrendsContent;
    private ImageView monthlyTrendsExpandIcon;
    private boolean isMonthlyTrendsExpanded = false;

    private LinearLayout recentSessionsHeader, recentSessionsContent;
    private ImageView recentSessionsExpandIcon;
    private boolean isRecentSessionsExpanded = false;

    // Recent sessions views
    private TextView recentSessionsText;
    private LinearLayout recentSessionsContainer;
    private AnalyticsManager analyticsManager;
    
    // Usage permission banner
    private TextView usagePermissionBanner;
    
    // Weekly stats UI elements
    private TextView thisWeekFocusTime;
    private TextView lastWeekFocusTime;
    private TextView thisWeekPhoneUsage;
    private TextView lastWeekPhoneUsage;
    private TextView weeklyFocusChange;
    private TextView weeklyMobileChange;
    
    // Monthly stats UI elements
    private TextView thisMonthFocusTime;
    private TextView lastMonthFocusTime;
    private TextView thisMonthPhoneUsage;
    private TextView lastMonthPhoneUsage;
    private TextView monthlyFocusChange;
    private TextView monthlyMobileChange;

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

        // Initialize UI components
        initializeViews(view);

        // Setup expandable sections
        setupExpandableSections();
        
        // Setup usage permission banner
        setupUsagePermissionBanner();

        // Load and display analytics data
        loadAnalyticsData();
    }
    
    @Override
    public void onResume() {
        super.onResume();
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
        todayFocusScore = view.findViewById(R.id.todayFocusScore);
        todayTrendIndicator = view.findViewById(R.id.todayTrendIndicator);
        todayMobileUsage = view.findViewById(R.id.todayMobileUsage);
        todayTimeSaved = view.findViewById(R.id.todayTimeSaved);

        // Progress bars
        sessionsProgress = view.findViewById(R.id.sessionsProgress);
        timeProgress = view.findViewById(R.id.timeProgress);
        focusScoreProgress = view.findViewById(R.id.focusScoreProgress);


        // Focus trends expandable section
        focusTrendsHeader = view.findViewById(R.id.focusTrendsHeader);
        focusTrendsContent = view.findViewById(R.id.focusTrendsContent);
        focusTrendsExpandIcon = view.findViewById(R.id.focusTrendsExpandIcon);

        // Monthly trends expandable section
        monthlyTrendsHeader = view.findViewById(R.id.monthlyTrendsHeader);
        monthlyTrendsContent = view.findViewById(R.id.monthlyTrendsContent);
        monthlyTrendsExpandIcon = view.findViewById(R.id.monthlyTrendsExpandIcon);

        // Recent sessions expandable section
        recentSessionsHeader = view.findViewById(R.id.recentSessionsHeader);
        recentSessionsContent = view.findViewById(R.id.recentSessionsContent);
        recentSessionsExpandIcon = view.findViewById(R.id.recentSessionsExpandIcon);

        // Recent sessions content
        recentSessionsText = view.findViewById(R.id.recentSessionsText);
        recentSessionsContainer = view.findViewById(R.id.recentSessionsContainer);
        
        // Usage permission banner
        usagePermissionBanner = view.findViewById(R.id.usagePermissionBanner);
        
        // Weekly stats views
        thisWeekFocusTime = view.findViewById(R.id.thisWeekFocusTime);
        lastWeekFocusTime = view.findViewById(R.id.lastWeekFocusTime);
        thisWeekPhoneUsage = view.findViewById(R.id.thisWeekMobileUsage);
        lastWeekPhoneUsage = view.findViewById(R.id.lastWeekMobileUsage);
        weeklyFocusChange = view.findViewById(R.id.weeklyFocusChange);
        weeklyMobileChange = view.findViewById(R.id.weeklyMobileChange);
        
        // Monthly stats views
        thisMonthFocusTime = view.findViewById(R.id.thisMonthFocusTime);
        lastMonthFocusTime = view.findViewById(R.id.lastMonthFocusTime);
        thisMonthPhoneUsage = view.findViewById(R.id.thisMonthMobileUsage);
        lastMonthPhoneUsage = view.findViewById(R.id.lastMonthMobileUsage);
        monthlyFocusChange = view.findViewById(R.id.monthlyFocusChange);
        monthlyMobileChange = view.findViewById(R.id.monthlyMobileChange);
    }

    private void setupExpandableSections() {
        // Focus Trends expandable (Weekly Insights)
        focusTrendsHeader.setOnClickListener(v -> toggleFocusTrends());

        // Monthly Trends expandable
        monthlyTrendsHeader.setOnClickListener(v -> toggleMonthlyTrends());

        // Recent Sessions expandable
        recentSessionsHeader.setOnClickListener(v -> toggleRecentSessions());
    }

    private void toggleFocusTrends() {
        isFocusTrendsExpanded = !isFocusTrendsExpanded;

        if (isFocusTrendsExpanded) {
            // Expand
            focusTrendsContent.setVisibility(View.VISIBLE);
            focusTrendsContent.setAlpha(0f);
            focusTrendsContent.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        } else {
            // Collapse
            focusTrendsContent.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> focusTrendsContent.setVisibility(View.GONE))
                    .start();
        }

        // Rotate icon
        ObjectAnimator rotation = ObjectAnimator.ofFloat(focusTrendsExpandIcon, "rotation",
                isFocusTrendsExpanded ? 180f : 0f, isFocusTrendsExpanded ? 0f : 180f);
        rotation.setDuration(300);
        rotation.start();
    }

    private void toggleMonthlyTrends() {
        isMonthlyTrendsExpanded = !isMonthlyTrendsExpanded;

        if (isMonthlyTrendsExpanded) {
            // Expand
            monthlyTrendsContent.setVisibility(View.VISIBLE);
            monthlyTrendsContent.setAlpha(0f);
            monthlyTrendsContent.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        } else {
            // Collapse
            monthlyTrendsContent.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> monthlyTrendsContent.setVisibility(View.GONE))
                    .start();
        }

        // Rotate icon
        ObjectAnimator rotation = ObjectAnimator.ofFloat(monthlyTrendsExpandIcon, "rotation",
                isMonthlyTrendsExpanded ? 180f : 0f, isMonthlyTrendsExpanded ? 0f : 180f);
        rotation.setDuration(300);
        rotation.start();
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

    private void loadAnalyticsData() {
        // Check usage stats permission first
        checkUsageStatsPermission();
        
        // Load real analytics data using LiveData
        loadTodayStats();
        loadWeeklyStats();
        loadMonthlyStats();
        loadRecentSessions();
    }
    
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
        // Load this week's stats
        new Thread(() -> {
            try {
                // Get this week's focus time from database
                long thisWeekFocusMs = analyticsManager.getThisWeekFocusTime();
                
                // Get this week's mobile usage from UsageStatsManager
                long thisWeekMobileMs = analyticsManager.getThisWeekMobileUsage();
                
                // Get last week's stats for comparison
                long lastWeekFocusMs = analyticsManager.getLastWeekFocusTime();
                long lastWeekMobileMs = analyticsManager.getLastWeekMobileUsage();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update this week's focus time
                        if (thisWeekFocusTime != null) {
                            thisWeekFocusTime.setText(formatTime(thisWeekFocusMs / (60 * 1000)));
                        }
                        
                        // Update last week's focus time
                        if (lastWeekFocusTime != null) {
                            lastWeekFocusTime.setText(formatTime(lastWeekFocusMs / (60 * 1000)));
                        }
                        
                        // Update this week's phone usage
                        if (thisWeekPhoneUsage != null) {
                            thisWeekPhoneUsage.setText(formatTime(thisWeekMobileMs / (60 * 1000)));
                        }
                        
                        // Update last week's phone usage
                        if (lastWeekPhoneUsage != null) {
                            lastWeekPhoneUsage.setText(formatTime(lastWeekMobileMs / (60 * 1000)));
                        }
                        
                        // Update weekly comparisons
                        updateWeeklyFocusChange(thisWeekFocusMs, lastWeekFocusMs);
                        updateWeeklyMobileChange(thisWeekMobileMs, lastWeekMobileMs);
                    });
                }
            } catch (Exception e) {
                Log.e("AnalyticsFragment", "Error loading weekly stats", e);
            }
        }).start();
    }
    
    private void loadMonthlyStats() {
        // Load this month's stats
        new Thread(() -> {
            try {
                // Get this month's focus time from database
                long thisMonthFocusMs = analyticsManager.getThisMonthFocusTime();
                
                // Get this month's mobile usage from UsageStatsManager
                long thisMonthMobileMs = analyticsManager.getThisMonthMobileUsage();
                
                // Get last month's stats for comparison
                long lastMonthFocusMs = analyticsManager.getLastMonthFocusTime();
                long lastMonthMobileMs = analyticsManager.getLastMonthMobileUsage();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update this month's focus time
                        if (thisMonthFocusTime != null) {
                            thisMonthFocusTime.setText(formatTime(thisMonthFocusMs / (60 * 1000)));
                        }
                        
                        // Update last month's focus time
                        if (lastMonthFocusTime != null) {
                            lastMonthFocusTime.setText(formatTime(lastMonthFocusMs / (60 * 1000)));
                        }
                        
                        // Update this month's phone usage
                        if (thisMonthPhoneUsage != null) {
                            thisMonthPhoneUsage.setText(formatTime(thisMonthMobileMs / (60 * 1000)));
                        }
                        
                        // Update last month's phone usage
                        if (lastMonthPhoneUsage != null) {
                            lastMonthPhoneUsage.setText(formatTime(lastMonthMobileMs / (60 * 1000)));
                        }
                        
                        // Update monthly comparisons
                        updateMonthlyFocusChange(thisMonthFocusMs, lastMonthFocusMs);
                        updateMonthlyMobileChange(thisMonthMobileMs, lastMonthMobileMs);
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

        if (todayFocusScore != null) {
            // If we have usage stats permission, show calculated focus score
            // Otherwise, show percentage of target reached
            if (analyticsManager.hasUsageStatsPermission()) {
            todayFocusScore.setText(String.valueOf(focusScore));
            } else {
                // Show focus time as percentage of 8-hour goal
                int percentage = (int) Math.min((focusTimeMinutes * 100) / 480, 100);
                todayFocusScore.setText(String.valueOf(percentage));
            }
        }

        // Update progress bars
        if (sessionsProgress != null) {
            sessionsProgress.setProgress(Math.min(sessions, 10)); // Goal: 10 sessions
        }
        if (timeProgress != null) {
            timeProgress.setProgress((int) Math.min(focusTimeMinutes, 480)); // Goal: 8 hours
        }
        if (focusScoreProgress != null) {
            if (analyticsManager.hasUsageStatsPermission()) {
            focusScoreProgress.setProgress(focusScore);
            } else {
                int percentage = (int) Math.min((focusTimeMinutes * 100) / 480, 100);
                focusScoreProgress.setProgress(percentage);
            }
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
                long mobileUsageMs = analyticsManager.getMobileUsageTracker().getTodayMobileUsage();
                // Log.d("AnalyticsFragment", "Refreshed mobile usage: " + mobileUsageMs + "ms");
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
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
        // Get real mobile usage data directly from UsageStatsManager (no database storage)
        new Thread(() -> {
            try {
                long mobileUsageMs = analyticsManager.getMobileUsageTracker().getTodayMobileUsage();
                Log.d("AnalyticsFragment", "Mobile usage from tracker: " + mobileUsageMs + "ms");
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
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

    
    
    private void updateWeeklyFocusChange(long thisWeekFocusMs, long lastWeekFocusMs) {
        if (weeklyFocusChange == null) return;
        
        if (lastWeekFocusMs == 0) {
            weeklyFocusChange.setText("--");
            weeklyFocusChange.setTextColor(requireContext().getColor(R.color.textSecondary));
            return;
        }
        
        double changePercentage = ((double) (thisWeekFocusMs - lastWeekFocusMs) / lastWeekFocusMs) * 100;
        
        if (changePercentage > 5) {
            // Increase in focus time (good) - show in green
            weeklyFocusChange.setText(String.format("+%.0f%%", changePercentage));
            weeklyFocusChange.setTextColor(requireContext().getColor(R.color.success));
        } else if (changePercentage > -5) {
            // Similar performance
            weeklyFocusChange.setText("0%");
            weeklyFocusChange.setTextColor(requireContext().getColor(R.color.textSecondary));
        } else {
            // Decrease in focus time (bad) - show in red
            weeklyFocusChange.setText(String.format("%.0f%%", changePercentage));
            weeklyFocusChange.setTextColor(requireContext().getColor(R.color.warning));
        }
    }
    
    private void updateWeeklyMobileChange(long thisWeekMobileMs, long lastWeekMobileMs) {
        if (weeklyMobileChange == null) return;
        
        if (lastWeekMobileMs == 0) {
            weeklyMobileChange.setText("--");
            weeklyMobileChange.setTextColor(requireContext().getColor(R.color.textSecondary));
            return;
        }
        
        double changePercentage = ((double) (thisWeekMobileMs - lastWeekMobileMs) / lastWeekMobileMs) * 100;
        
        if (changePercentage > 5) {
            // Increase in mobile usage (bad) - show in red
            weeklyMobileChange.setText(String.format("+%.0f%%", changePercentage));
            weeklyMobileChange.setTextColor(requireContext().getColor(R.color.warning));
        } else if (changePercentage > -5) {
            // Similar performance
            weeklyMobileChange.setText("0%");
            weeklyMobileChange.setTextColor(requireContext().getColor(R.color.textSecondary));
        } else {
            // Decrease in mobile usage (good) - show in green
            weeklyMobileChange.setText(String.format("%.0f%%", changePercentage));
            weeklyMobileChange.setTextColor(requireContext().getColor(R.color.success));
        }
    }
    
    private void updateMonthlyFocusChange(long thisMonthFocusMs, long lastMonthFocusMs) {
        if (monthlyFocusChange == null) return;
        
        if (lastMonthFocusMs == 0) {
            monthlyFocusChange.setText("--");
            monthlyFocusChange.setTextColor(requireContext().getColor(R.color.textSecondary));
            return;
        }
        
        double changePercentage = ((double) (thisMonthFocusMs - lastMonthFocusMs) / lastMonthFocusMs) * 100;
        
        if (changePercentage > 5) {
            // Increase in focus time (good) - show in green
            monthlyFocusChange.setText(String.format("+%.0f%%", changePercentage));
            monthlyFocusChange.setTextColor(requireContext().getColor(R.color.success));
        } else if (changePercentage > -5) {
            // Similar performance
            monthlyFocusChange.setText("0%");
            monthlyFocusChange.setTextColor(requireContext().getColor(R.color.textSecondary));
        } else {
            // Decrease in focus time (bad) - show in red
            monthlyFocusChange.setText(String.format("%.0f%%", changePercentage));
            monthlyFocusChange.setTextColor(requireContext().getColor(R.color.warning));
        }
    }
    
    private void updateMonthlyMobileChange(long thisMonthMobileMs, long lastMonthMobileMs) {
        if (monthlyMobileChange == null) return;
        
        if (lastMonthMobileMs == 0) {
            monthlyMobileChange.setText("--");
            monthlyMobileChange.setTextColor(requireContext().getColor(R.color.textSecondary));
            return;
        }
        
        double changePercentage = ((double) (thisMonthMobileMs - lastMonthMobileMs) / lastMonthMobileMs) * 100;
        
        if (changePercentage > 5) {
            // Increase in mobile usage (bad) - show in red
            monthlyMobileChange.setText(String.format("+%.0f%%", changePercentage));
            monthlyMobileChange.setTextColor(requireContext().getColor(R.color.warning));
        } else if (changePercentage > -5) {
            // Similar performance
            monthlyMobileChange.setText("0%");
            monthlyMobileChange.setTextColor(requireContext().getColor(R.color.textSecondary));
        } else {
            // Decrease in mobile usage (good) - show in green
            monthlyMobileChange.setText(String.format("%.0f%%", changePercentage));
            monthlyMobileChange.setTextColor(requireContext().getColor(R.color.success));
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
                // Keep the sample data for now since it looks better
                // displayRecentSessions(recentSessions);
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
    
    private View createSessionView(AnalyticsModels.FocusSession session) {
        // Create a simple session item view
        LinearLayout sessionItem = new LinearLayout(requireContext());
        sessionItem.setOrientation(LinearLayout.HORIZONTAL);
        sessionItem.setPadding(16, 12, 16, 12);
        sessionItem.setBackgroundResource(R.drawable.modern_card_background);
        
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
        
        // Add margin between items
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
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
        sessionItem.setBackgroundResource(R.drawable.modern_card_background);
        sessionItem.setPadding(48, 48, 48, 48); // 16dp in pixels
        
        LinearLayout.LayoutParams marginParams = (LinearLayout.LayoutParams) sessionItem.getLayoutParams();
        marginParams.bottomMargin = 24; // 8dp margin
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
        statusIcon.setLayoutParams(new LinearLayout.LayoutParams(72, 72)); // 24dp
        LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) statusIcon.getLayoutParams();
        iconParams.rightMargin = 36; // 12dp
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

}

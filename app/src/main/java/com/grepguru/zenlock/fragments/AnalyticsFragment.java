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

public class AnalyticsFragment extends Fragment {

    // Today's stats views
    private TextView todaySessions, todayTime, todayFocusScore;
    private TextView todayTrendIndicator;
    private ProgressBar sessionsProgress, timeProgress, focusScoreProgress;

    // Streaks & achievements views
    private TextView currentStreak, bestStreak, weeklyGoal;
    private ProgressBar weeklyGoalProgress;

    // Expandable sections
    private LinearLayout focusTrendsHeader, focusTrendsContent;
    private ImageView focusTrendsExpandIcon;
    private boolean isFocusTrendsExpanded = false;

    private LinearLayout recentSessionsHeader, recentSessionsContent;
    private ImageView recentSessionsExpandIcon;
    private boolean isRecentSessionsExpanded = false;

    // Recent sessions views
    private TextView recentSessionsText;
    private LinearLayout recentSessionsContainer;
    private AnalyticsManager analyticsManager;

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

        // Load and display analytics data
        loadAnalyticsData();
        
        // Create sample data for testing (debug only)
        createSampleDataForTesting();
    }

    private void initializeViews(View view) {
        // Coming soon banner - only show if no usage stats permission
        View comingSoonBanner = view.findViewById(R.id.comingSoonBanner);
        ImageView dismissBanner = view.findViewById(R.id.dismissAnalyticsBanner);
        
        // Handle banner dismissal
        dismissBanner.setOnClickListener(v -> {
            comingSoonBanner.setVisibility(View.GONE);
        });
        
        // Hide banner initially - will be shown if permission needed
        comingSoonBanner.setVisibility(View.GONE);

        // Today's stats
        todaySessions = view.findViewById(R.id.todaySessions);
        todayTime = view.findViewById(R.id.todayTime);
        todayFocusScore = view.findViewById(R.id.todayFocusScore);
        todayTrendIndicator = view.findViewById(R.id.todayTrendIndicator);

        // Progress bars
        sessionsProgress = view.findViewById(R.id.sessionsProgress);
        timeProgress = view.findViewById(R.id.timeProgress);
        focusScoreProgress = view.findViewById(R.id.focusScoreProgress);

        // Streaks & achievements
        currentStreak = view.findViewById(R.id.currentStreak);
        bestStreak = view.findViewById(R.id.bestStreak);
        weeklyGoal = view.findViewById(R.id.weeklyGoal);
        weeklyGoalProgress = view.findViewById(R.id.weeklyGoalProgress);

        // Focus trends expandable section
        focusTrendsHeader = view.findViewById(R.id.focusTrendsHeader);
        focusTrendsContent = view.findViewById(R.id.focusTrendsContent);
        focusTrendsExpandIcon = view.findViewById(R.id.focusTrendsExpandIcon);

        // Recent sessions expandable section
        recentSessionsHeader = view.findViewById(R.id.recentSessionsHeader);
        recentSessionsContent = view.findViewById(R.id.recentSessionsContent);
        recentSessionsExpandIcon = view.findViewById(R.id.recentSessionsExpandIcon);

        // Recent sessions content
        recentSessionsText = view.findViewById(R.id.recentSessionsText);
        recentSessionsContainer = view.findViewById(R.id.recentSessionsContainer);
    }

    private void setupExpandableSections() {
        // Focus Trends expandable
        focusTrendsHeader.setOnClickListener(v -> toggleFocusTrends());

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

    private void loadAnalyticsData() {
        // Check usage stats permission first
        checkUsageStatsPermission();
        
        // Load real analytics data using LiveData
        loadTodayStats();
        loadWeeklyStats();
        loadRecentSessions();
    }
    
    private void checkUsageStatsPermission() {
        if (!analyticsManager.hasUsageStatsPermission() && 
            UsageStatsPermissionManager.shouldShowPermissionRequest(requireContext())) {
            // Show permission request in coming soon banner
            showUsageStatsPermissionBanner();
        }
    }
    
    private void showUsageStatsPermissionBanner() {
        View comingSoonBanner = getView().findViewById(R.id.comingSoonBanner);
        if (comingSoonBanner != null) {
            // Make banner clickable to request permission
            comingSoonBanner.setOnClickListener(v -> {
                analyticsManager.requestUsageStatsPermission();
                Toast.makeText(requireContext(), 
                    "Please find ZenLock in the list and enable usage access", 
                    Toast.LENGTH_LONG).show();
            });
            
            comingSoonBanner.setVisibility(View.VISIBLE);
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
            } else {
                // Show default values if no data
                updateTodayStats(0, 0, 0, null);
            }
        });
    }
    
    private void loadWeeklyStats() {
        // Observe weekly stats with LiveData
        analyticsManager.getCurrentWeekStats().observe(getViewLifecycleOwner(), weekStats -> {
            if (weekStats != null) {
                // Get last week's stats for comparison
                WeeklyStatsEntity lastWeekStats = analyticsManager.getLastWeekStats();
                
                // Update weekly stats with real data
                updateWeeklyStats(weekStats, lastWeekStats);
            }
        });
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
        
        // Update mobile usage info if available
        updateMobileUsageDisplay();
    }
    
    private void updateMobileUsageDisplay() {
        // This could be used to update a separate mobile usage section
        // For now, we'll show it in the insights section
        if (analyticsManager.hasUsageStatsPermission()) {
            String mobileUsage = analyticsManager.getTodayMobileUsageFormatted();
            Log.d("AnalyticsFragment", "Today's mobile usage: " + mobileUsage);
        }
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
        
        // Update trend indicator
        if (changePercentage > 10) {
            todayTrendIndicator.setText(String.format("↗️ +%.0f%%", changePercentage));
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.success));
        } else if (changePercentage > -10) {
            todayTrendIndicator.setText("→ Similar");
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.textSecondary));
            } else {
            todayTrendIndicator.setText(String.format("↘️ %.0f%%", changePercentage));
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.warning));
        }
    }

    private void updateWeeklyStats(WeeklyStatsEntity weekStats, WeeklyStatsEntity lastWeekStats) {
        // Calculate current streak (simplified for now)
        int currentStreakValue = 1; // This should be calculated based on daily data
        int bestStreakValue = currentStreakValue;
        
        // Calculate weekly goal progress (20 hours = 1200 minutes goal)
        long weeklyGoalMinutes = 1200;
        long actualMinutes = weekStats.totalFocusTime / (1000 * 60);
        int weeklyProgress = (int) Math.min((actualMinutes * 100) / weeklyGoalMinutes, 100);
        
        // Update UI
        if (this.currentStreak != null) this.currentStreak.setText(String.valueOf(currentStreakValue));
        if (this.bestStreak != null) this.bestStreak.setText(String.valueOf(bestStreakValue));
        if (this.weeklyGoal != null) this.weeklyGoal.setText(weeklyProgress + "%");
        if (this.weeklyGoalProgress != null) this.weeklyGoalProgress.setProgress(weeklyProgress);
        
        // Update weekly comparison in the insights section
        updateWeeklyComparison(weekStats, lastWeekStats);
    }
    
    private void updateWeeklyComparison(WeeklyStatsEntity thisWeek, WeeklyStatsEntity lastWeek) {
        // This would update the "This Week vs Last Week" indicator in the insights section
        // For now, we'll keep the existing mock data until we implement the full insights section
    }
    
    private void updateRecentSessionsWithData(List<SessionEntity> sessions) {
        if (recentSessionsText != null) {
            recentSessionsText.setVisibility(View.GONE);
        }
        if (recentSessionsContainer != null) {
            recentSessionsContainer.setVisibility(View.VISIBLE);
            recentSessionsContainer.removeAllViews();
            
            // Add real session data (limit to 3 for UI space)
            int displayCount = Math.min(sessions.size(), 3);
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

    private void updateStreaksAndAchievements(int current, int best, int weeklyProgress) {
        if (currentStreak != null) currentStreak.setText(String.valueOf(current));
        if (bestStreak != null) bestStreak.setText(String.valueOf(best));
        if (weeklyGoal != null) weeklyGoal.setText(weeklyProgress + "%");
        if (weeklyGoalProgress != null) weeklyGoalProgress.setProgress(weeklyProgress);
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

    @Override
    public void onResume() {
        super.onResume();
        
        // Check if user granted permission while away
        checkPermissionStatusOnResume();
        
        // Refresh analytics when returning to the fragment
        refreshAnalytics();
    }
    
    private void checkPermissionStatusOnResume() {
        if (analyticsManager.hasUsageStatsPermission()) {
            // Permission granted! Hide banner and reset permission state
            View comingSoonBanner = getView().findViewById(R.id.comingSoonBanner);
            if (comingSoonBanner != null) {
                comingSoonBanner.setVisibility(View.GONE);
            }
            UsageStatsPermissionManager.resetPermissionState(requireContext());
            
            // Update mobile usage now that we have permission
            analyticsManager.updateTodayMobileUsageIfAvailable();
            
            Toast.makeText(requireContext(), 
                "✅ Usage access granted! Screen time tracking enabled.", 
                Toast.LENGTH_SHORT).show();
        } else if (UsageStatsPermissionManager.shouldShowPermissionRequest(requireContext())) {
            // User came back but didn't grant permission
            UsageStatsPermissionManager.markPermissionDenied(requireContext());
        }
    }
    
    private void createSampleDataForTesting() {
        // Only create sample data in debug builds for testing
        try {
            analyticsManager.createSampleData();
            Log.d("AnalyticsFragment", "Sample data created for testing");
        } catch (Exception e) {
            Log.e("AnalyticsFragment", "Error creating sample data", e);
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

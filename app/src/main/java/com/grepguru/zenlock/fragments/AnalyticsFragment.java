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

import java.util.List;

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
    }

    private void initializeViews(View view) {
        // Coming soon banner
        View comingSoonBanner = view.findViewById(R.id.comingSoonBanner);
        ImageView dismissBanner = view.findViewById(R.id.dismissAnalyticsBanner);
        
        // Handle banner dismissal (but always show it when fragment loads)
        dismissBanner.setOnClickListener(v -> {
            comingSoonBanner.setVisibility(View.GONE);
        });

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
        // Load real analytics data
        AnalyticsModels.DailyStats todayStats = analyticsManager.getTodayStats();
        AnalyticsModels.PeriodSummary weekStats = analyticsManager.getWeekStats();

        // Update today's stats with modern enhancements
        updateTodayStats(
            todayStats.getTotalSessions(),
            todayStats.getTotalFocusTime() / (1000 * 60), // Convert to minutes
            calculateFocusScore(todayStats)
        );

        // Update streaks and achievements
        updateStreaksAndAchievements(
            weekStats.getLongestStreak(),
            weekStats.getLongestStreak(),
            calculateWeeklyGoalProgress(weekStats)
        );

        // Update recent sessions
        updateRecentSessions();
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

    private void updateTodayStats(int sessions, long focusTimeMinutes, int focusScore) {
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
            todayFocusScore.setText(String.valueOf(focusScore));
        }

        // Update progress bars
        if (sessionsProgress != null) {
            sessionsProgress.setProgress(Math.min(sessions, 10)); // Goal: 10 sessions
        }
        if (timeProgress != null) {
            timeProgress.setProgress((int) Math.min(focusTimeMinutes, 480)); // Goal: 8 hours
        }
        if (focusScoreProgress != null) {
            focusScoreProgress.setProgress(focusScore);
        }

        // Update trend indicator (mock data for now)
        if (todayTrendIndicator != null) {
            if (focusScore >= 80) {
                todayTrendIndicator.setText("↗️ +15%");
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.success));
            } else if (focusScore >= 60) {
                todayTrendIndicator.setText("→ 0%");
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.textSecondary));
            } else {
                todayTrendIndicator.setText("↘️ -5%");
                todayTrendIndicator.setTextColor(requireContext().getColor(R.color.warning));
            }
        }
    }

    private void updateStreaksAndAchievements(int current, int best, int weeklyProgress) {
        if (currentStreak != null) currentStreak.setText(String.valueOf(current));
        if (bestStreak != null) bestStreak.setText(String.valueOf(best));
        if (weeklyGoal != null) weeklyGoal.setText(weeklyProgress + "%");
        if (weeklyGoalProgress != null) weeklyGoalProgress.setProgress(weeklyProgress);
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
    
    private String formatSessionTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
        return sdf.format(new java.util.Date(timestamp));
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
        // Refresh analytics when returning to the fragment
        refreshAnalytics();
    }


}

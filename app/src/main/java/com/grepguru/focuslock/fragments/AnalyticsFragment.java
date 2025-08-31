package com.grepguru.focuslock.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grepguru.focuslock.R;
import com.grepguru.focuslock.utils.AnalyticsManager;
import com.grepguru.focuslock.model.AnalyticsModels;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalyticsFragment extends Fragment {

    private TextView todaySessions, todayTime, todayCompletion;
    private TextView weekSessions, weekTime, weekStreak;
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

        // Load and display analytics data
        loadAnalyticsData();
    }

    private void initializeViews(View view) {
        // Today's stats
        todaySessions = view.findViewById(R.id.todaySessions);
        todayTime = view.findViewById(R.id.todayTime);
        todayCompletion = view.findViewById(R.id.todayCompletion);
        
        // Week stats
        weekSessions = view.findViewById(R.id.weekSessions);
        weekTime = view.findViewById(R.id.weekTime);
        weekStreak = view.findViewById(R.id.weekStreak);
        
        // Recent sessions
        recentSessionsText = view.findViewById(R.id.recentSessionsText);
        recentSessionsContainer = view.findViewById(R.id.recentSessionsContainer);
    }

    private void loadAnalyticsData() {
        // Load real analytics data
        AnalyticsModels.DailyStats todayStats = analyticsManager.getTodayStats();
        AnalyticsModels.PeriodSummary weekStats = analyticsManager.getWeekStats();

        // Update today's stats
        updateTodayStats(
            todayStats.getTotalSessions(),
            todayStats.getTotalFocusTime() / (1000 * 60), // Convert to minutes
            todayStats.getCompletionRate()
        );

        // Update week stats
        updateWeekStats(
            weekStats.getTotalSessions(),
            weekStats.getTotalFocusTime() / (1000 * 60), // Convert to minutes
            weekStats.getLongestStreak()
        );
        
        // Update recent sessions
        updateRecentSessions();
    }

    private void updateTodayStats(int sessions, long focusTimeMinutes, double completionRate) {
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

        if (todayCompletion != null) {
            todayCompletion.setText((int) completionRate + "%");
        }
    }

    private void updateWeekStats(int sessions, long focusTimeMinutes, int streak) {
        if (weekSessions != null) weekSessions.setText(String.valueOf(sessions));
        if (weekStreak != null) weekStreak.setText(String.valueOf(streak));

        if (weekTime != null) {
            if (focusTimeMinutes < 60) {
                weekTime.setText(focusTimeMinutes + "m");
            } else {
                int hours = (int) (focusTimeMinutes / 60);
                int minutes = (int) (focusTimeMinutes % 60);
                weekTime.setText(hours + "h " + minutes + "m");
            }
        }
    }
    
    private void updateRecentSessions() {
        List<AnalyticsModels.FocusSession> recentSessions = analyticsManager.getRecentSessions(5);
        
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

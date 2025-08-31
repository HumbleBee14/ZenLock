package com.grepguru.focuslock.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grepguru.focuslock.R;

public class AnalyticsFragment extends Fragment {

    private TextView todaySessions, todayTime, todayCompletion;

    public AnalyticsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    private void loadAnalyticsData() {
        // For now, show placeholder data
        // In the future, this will load from SharedPreferences or database

        // Today's data (placeholder)
        updateTodayStats(0, 0, 0);

        // Week data (placeholder)
        updateWeekStats(0, 0, 0);
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
        // TODO: Implement when week stats card is added to layout
        // For now, this method is ready for future implementation
    }

    // Methods to be called when session data changes
    public void onSessionCompleted(long duration, boolean completed) {
        // This will be called when a focus session ends
        // For now, just refresh the data
        loadAnalyticsData();
    }

    public void refreshAnalytics() {
        // Refresh all analytics data
        loadAnalyticsData();
    }
}

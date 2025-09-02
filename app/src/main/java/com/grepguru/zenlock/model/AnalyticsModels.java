package com.grepguru.zenlock.model;

import java.util.List;

public class AnalyticsModels {

    // Session data model
    public static class FocusSession {
        private long id;
        private long startTime;
        private long endTime;
        private long targetDuration; // in milliseconds
        private long actualDuration; // in milliseconds
        private boolean completed;
        private List<String> whitelistedApps;
        private List<String> blockedAttempts;

        public FocusSession() {}

        public FocusSession(long startTime, long targetDuration) {
            this.startTime = startTime;
            this.targetDuration = targetDuration;
            this.completed = false;
        }

        // Getters and setters
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public long getTargetDuration() { return targetDuration; }
        public void setTargetDuration(long targetDuration) { this.targetDuration = targetDuration; }

        public long getActualDuration() { return actualDuration; }
        public void setActualDuration(long actualDuration) { this.actualDuration = actualDuration; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }

        public List<String> getWhitelistedApps() { return whitelistedApps; }
        public void setWhitelistedApps(List<String> whitelistedApps) { this.whitelistedApps = whitelistedApps; }

        public List<String> getBlockedAttempts() { return blockedAttempts; }
        public void setBlockedAttempts(List<String> blockedAttempts) { this.blockedAttempts = blockedAttempts; }

        // Utility methods
        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }

        public double getCompletionRate() {
            if (targetDuration == 0) return 0;
            return Math.min(100.0, (double) getDuration() / targetDuration * 100);
        }
    }

    // Daily statistics
    public static class DailyStats {
        private String date; // YYYY-MM-DD format
        private int totalSessions;
        private long totalFocusTime; // in milliseconds
        private int completedSessions;
        private int blockedAttempts;
        private long averageSessionDuration;

        public DailyStats() {}

        public DailyStats(String date) {
            this.date = date;
            this.totalSessions = 0;
            this.totalFocusTime = 0;
            this.completedSessions = 0;
            this.blockedAttempts = 0;
            this.averageSessionDuration = 0;
        }

        // Getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

        public long getTotalFocusTime() { return totalFocusTime; }
        public void setTotalFocusTime(long totalFocusTime) { this.totalFocusTime = totalFocusTime; }

        public int getCompletedSessions() { return completedSessions; }
        public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }

        public int getBlockedAttempts() { return blockedAttempts; }
        public void setBlockedAttempts(int blockedAttempts) { this.blockedAttempts = blockedAttempts; }

        public long getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(long averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }

        // Utility methods
        public double getCompletionRate() {
            return totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0;
        }

        public void updateAverageDuration() {
            this.averageSessionDuration = totalSessions > 0 ? totalFocusTime / totalSessions : 0;
        }
    }

    // Achievement/badge system
    public static class Achievement {
        private String id;
        private String title;
        private String description;
        private String icon;
        private boolean unlocked;
        private long unlockedAt;
        private int progress;
        private int target;

        public Achievement() {}

        public Achievement(String id, String title, String description, String icon, int target) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.target = target;
            this.progress = 0;
            this.unlocked = false;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public boolean isUnlocked() { return unlocked; }
        public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

        public long getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(long unlockedAt) { this.unlockedAt = unlockedAt; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public int getTarget() { return target; }
        public void setTarget(int target) { this.target = target; }

        // Utility methods
        public void updateProgress(int newProgress) {
            this.progress = newProgress;
            if (this.progress >= this.target && !this.unlocked) {
                this.unlocked = true;
                this.unlockedAt = System.currentTimeMillis();
            }
        }

        public double getProgressPercentage() {
            return target > 0 ? (double) progress / target * 100 : 0;
        }
    }

    // Weekly/Monthly summary
    public static class PeriodSummary {
        private String period; // "week_YYYY_WW" or "month_YYYY_MM"
        private long totalFocusTime;
        private int totalSessions;
        private int completedSessions;
        private int longestStreak;
        private int averageSessionLength;
        private String mostProductiveDay;

        public PeriodSummary() {}

        public PeriodSummary(String period) {
            this.period = period;
        }

        // Getters and setters
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }

        public long getTotalFocusTime() { return totalFocusTime; }
        public void setTotalFocusTime(long totalFocusTime) { this.totalFocusTime = totalFocusTime; }

        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

        public int getCompletedSessions() { return completedSessions; }
        public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }

        public int getLongestStreak() { return longestStreak; }
        public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

        public int getAverageSessionLength() { return averageSessionLength; }
        public void setAverageSessionLength(int averageSessionLength) { this.averageSessionLength = averageSessionLength; }

        public String getMostProductiveDay() { return mostProductiveDay; }
        public void setMostProductiveDay(String mostProductiveDay) { this.mostProductiveDay = mostProductiveDay; }
    }
}
import SwiftUI
import SwiftData
import DeviceActivity

struct AnalyticsView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FocusSession.startedAt, order: .reverse) private var sessions: [FocusSession]
    enum Range: String, CaseIterable, Identifiable {
        case today, week, month
        var id: String { rawValue }
        var label: String {
            switch self {
            case .today: return "Today"
            case .week: return "This week"
            case .month: return "This month"
            }
        }
    }

    @State private var range: Range = .today

    var body: some View {
        ZStack {
            ZenTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: ZenTheme.Spacing.lg) {
                    streakCard
                    rangePicker
                    GlassCard {
                        DeviceActivityReport(
                            DeviceActivityReport.Context("totalUsage"),
                            filter: filter(for: range)
                        )
                        .padding(ZenTheme.Spacing.md)
                    }
                    GlassCard {
                        DeviceActivityReport(
                            DeviceActivityReport.Context("perCategory"),
                            filter: filter(for: range)
                        )
                        .padding(ZenTheme.Spacing.md)
                    }
                    historySection
                    privacyNote
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
        .navigationTitle("Insights")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private var summary: StreakCalculator.Summary {
        StreakCalculator.summary(from: sessions)
    }

    private var streakCard: some View {
        GlassCard {
            VStack(spacing: ZenTheme.Spacing.md) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Focus Score")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                        Text("\(summary.focusScore)")
                            .font(.system(size: 44, weight: .bold))
                            .foregroundStyle(ZenTheme.text)
                        Text("over the last 7 days")
                            .font(ZenTheme.caption2)
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 4) {
                        statBadge(icon: "flame.fill", value: "\(summary.currentStreak)d", label: "streak", tint: .orange)
                        statBadge(icon: "trophy.fill", value: "\(summary.bestStreak)d", label: "best", tint: .yellow)
                    }
                }
                HStack {
                    Label("\(summary.totalFocusMinutes) min focused", systemImage: "hourglass")
                    Spacer()
                    Label("\(summary.completedSessions) completed", systemImage: "checkmark.circle.fill")
                }
                .font(ZenTheme.caption)
                .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func statBadge(icon: String, value: String, label: String, tint: Color) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon).foregroundStyle(tint)
            Text(value).font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
            Text(label).font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
        }
    }

    private var historySection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                Text("Recent Sessions")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                if sessions.isEmpty {
                    Text("Start a focus session and it'll show up here.")
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                } else {
                    ForEach(Array(sessions.prefix(10))) { session in
                        HStack {
                            Image(systemName: session.wasCompleted ? "checkmark.circle.fill" : "circle.dashed")
                                .foregroundStyle(session.wasCompleted ? ZenTheme.success : ZenTheme.textSecondary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(session.groupName).font(ZenTheme.body).foregroundStyle(ZenTheme.text)
                                Text(session.startedAt, format: .relative(presentation: .named))
                                    .font(ZenTheme.caption)
                                    .foregroundStyle(ZenTheme.textSecondary)
                            }
                            Spacer()
                            Text(durationLabel(session.actualDuration))
                                .font(ZenTheme.caption.monospacedDigit())
                                .foregroundStyle(ZenTheme.textSecondary)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func durationLabel(_ seconds: TimeInterval) -> String {
        let m = Int(seconds / 60)
        if m < 60 { return "\(m)m" }
        return "\(m / 60)h \(m % 60)m"
    }

    private var rangePicker: some View {
        Picker("Range", selection: $range) {
            ForEach(Range.allCases) { r in Text(r.label).tag(r) }
        }
        .pickerStyle(.segmented)
    }

    private var privacyNote: some View {
        Text("Usage data is rendered inside Apple's sandboxed extension. ZenLock never sees the raw numbers — they stay on your device.")
            .font(ZenTheme.caption)
            .foregroundStyle(ZenTheme.textSecondary)
            .multilineTextAlignment(.center)
            .padding(.top, ZenTheme.Spacing.sm)
    }

    private func filter(for range: Range) -> DeviceActivityFilter {
        let cal = Calendar.current
        let now = Date()
        let start: Date
        switch range {
        case .today:
            start = cal.startOfDay(for: now)
        case .week:
            start = cal.date(byAdding: .day, value: -7, to: now) ?? now
        case .month:
            start = cal.date(byAdding: .day, value: -30, to: now) ?? now
        }
        return DeviceActivityFilter(
            segment: .daily(during: DateInterval(start: start, end: now))
        )
    }
}

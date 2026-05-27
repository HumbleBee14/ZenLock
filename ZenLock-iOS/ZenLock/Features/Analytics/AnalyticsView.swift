import SwiftUI
import DeviceActivity

struct AnalyticsView: View {
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
                    privacyNote
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
        .navigationTitle("Insights")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
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

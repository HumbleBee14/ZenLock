import SwiftUI
import DeviceActivity

struct ScreenTimeView: View {
    @AppStorage(Constants.Keys.dailyGoalMinutes, store: Constants.sharedDefaults)
    private var dailyGoalMinutes: Int = 180

    @Environment(\.scenePhase) private var scenePhase
    @State private var filterEnd: Date = Date()
    @State private var reportID: UUID = UUID()
    @State private var hasEverRenderedData: Bool = ScreenTimeSnapshot.load() != nil
    @State private var showInitialLoader: Bool = ScreenTimeSnapshot.load() == nil

    var body: some View {
        ZStack {
            ZenTheme.background.ignoresSafeArea()

            DeviceActivityReport(
                DeviceActivityReport.Context("perApp"),
                filter: todayFilter
            )
            .id(reportID)

            if showInitialLoader {
                VStack(spacing: 12) {
                    ProgressView()
                        .tint(ZenTheme.primary)
                    Text("Fetching today's usage…")
                        .font(.callout)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(ZenTheme.background)
            }
        }
        .navigationTitle("Today's Screen Time")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            refresh()
        }
        .onChange(of: dailyGoalMinutes) { _, _ in
            refresh()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refresh() }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    refresh()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .tint(ZenTheme.primary)
            }
        }
    }

    private func refresh() {
        filterEnd = Date()
        reportID = UUID()

        if !hasEverRenderedData {
            showInitialLoader = true
            Task {
                try? await Task.sleep(nanoseconds: 2_500_000_000)
                if ScreenTimeSnapshot.load() == nil {
                    await MainActor.run {
                        filterEnd = Date()
                        reportID = UUID()
                    }
                    try? await Task.sleep(nanoseconds: 2_500_000_000)
                }
                await MainActor.run {
                    if ScreenTimeSnapshot.load() != nil {
                        hasEverRenderedData = true
                    }
                    showInitialLoader = false
                }
            }
        }
    }

    private var todayFilter: DeviceActivityFilter {
        let start = Calendar.current.startOfDay(for: Date())
        return DeviceActivityFilter(
            segment: .daily(during: DateInterval(start: start, end: filterEnd))
        )
    }
}

import SwiftUI
import DeviceActivity

struct ScreenTimeView: View {
    @AppStorage(Constants.Keys.dailyGoalMinutes, store: Constants.sharedDefaults)
    private var dailyGoalMinutes: Int = 180

    @State private var filterEnd: Date = Date()

    var body: some View {
        ZStack {
            ZenTheme.background.ignoresSafeArea()

            DeviceActivityReport(
                DeviceActivityReport.Context("perApp"),
                filter: todayFilter
            )
        }
        .navigationTitle("Today's Screen Time")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: dailyGoalMinutes) { _, _ in
            filterEnd = Date()
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    filterEnd = Date()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .tint(ZenTheme.primary)
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

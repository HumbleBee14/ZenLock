import DeviceActivity
import SwiftUI

struct TotalUsageScene: DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .init("totalUsage")

    let content: (TotalUsageData) -> TotalUsageView

    init(@ViewBuilder content: @escaping (TotalUsageData) -> TotalUsageView = { TotalUsageView(data: $0) }) {
        self.content = content
    }

    func makeConfiguration(representing data: DeviceActivityResults<DeviceActivityData>) async -> TotalUsageData {
        var totalDuration: TimeInterval = 0
        var appCount = 0
        var pickupCount = 0

        for await activity in data {
            for await segment in activity.activitySegments {
                totalDuration += segment.totalActivityDuration
                pickupCount += segment.totalPickupsWithoutApplicationActivity
                for await categoryActivity in segment.categories {
                    for await appActivity in categoryActivity.applications {
                        if appActivity.totalActivityDuration > 0 {
                            appCount += 1
                        }
                    }
                }
            }
        }

        return TotalUsageData(
            totalDuration: totalDuration,
            uniqueAppsUsed: appCount,
            pickups: pickupCount
        )
    }
}

struct TotalUsageData {
    let totalDuration: TimeInterval
    let uniqueAppsUsed: Int
    let pickups: Int
}

struct TotalUsageView: View {
    let data: TotalUsageData

    var body: some View {
        VStack(spacing: 16) {
            statTile(value: formatted(data.totalDuration), label: "Total screen time", icon: "hourglass")
            HStack(spacing: 12) {
                statTile(value: "\(data.uniqueAppsUsed)", label: "Apps used", icon: "square.grid.2x2.fill")
                statTile(value: "\(data.pickups)", label: "Pickups", icon: "hand.tap.fill")
            }
        }
    }

    private func statTile(value: String, label: String, icon: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(.indigo)
                Spacer()
            }
            Text(value)
                .font(.system(size: 28, weight: .bold))
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }

    private func formatted(_ seconds: TimeInterval) -> String {
        let h = Int(seconds) / 3600
        let m = (Int(seconds) % 3600) / 60
        if h > 0 { return "\(h)h \(m)m" }
        return "\(m)m"
    }
}

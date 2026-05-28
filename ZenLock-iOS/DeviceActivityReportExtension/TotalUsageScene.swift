import DeviceActivity
import SwiftUI

struct TotalUsageScene: @preconcurrency DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .init("totalUsage")

    let content: (TotalUsageData) -> TotalUsageView

    init(@ViewBuilder content: @escaping (TotalUsageData) -> TotalUsageView = { TotalUsageView(data: $0) }) {
        self.content = content
    }

    func makeConfiguration(representing data: DeviceActivityResults<DeviceActivityData>) async -> TotalUsageData {
        var totalDuration: TimeInterval = 0
        var pickupCount = 0

        for await activity in data {
            for await segment in activity.activitySegments {
                totalDuration += segment.totalActivityDuration
                pickupCount += segment.totalPickupsWithoutApplicationActivity
            }
        }

        return TotalUsageData(
            totalDuration: totalDuration,
            pickups: pickupCount
        )
    }
}

struct TotalUsageData {
    let totalDuration: TimeInterval
    let pickups: Int
}

struct TotalUsageView: View {
    let data: TotalUsageData

    var body: some View {
        HStack(spacing: 12) {
            statTile(value: formatted(data.totalDuration), label: "Screen time", icon: "hourglass")
            statTile(value: "\(data.pickups)", label: "Pickups", icon: "hand.tap.fill")
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

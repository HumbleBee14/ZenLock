import DeviceActivity
import SwiftUI
import Charts

struct UsageTrendScene: @preconcurrency DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .init("usageTrend")

    let content: (UsageTrendData) -> UsageTrendView

    init(@ViewBuilder content: @escaping (UsageTrendData) -> UsageTrendView = { UsageTrendView(data: $0) }) {
        self.content = content
    }

    func makeConfiguration(representing data: DeviceActivityResults<DeviceActivityData>) async -> UsageTrendData {
        var points: [UsageTrendData.Point] = []
        for await activity in data {
            for await segment in activity.activitySegments {
                points.append(.init(
                    date: segment.dateInterval.start,
                    minutes: segment.totalActivityDuration / 60
                ))
            }
        }
        points.sort { $0.date < $1.date }
        return UsageTrendData(points: points)
    }
}

struct UsageTrendData {
    struct Point: Identifiable {
        let date: Date
        let minutes: Double
        var id: Date { date }
    }
    let points: [Point]
}

struct UsageTrendView: View {
    let data: UsageTrendData

    var body: some View {
        if data.points.isEmpty {
            Text("No usage data yet")
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, minHeight: 120)
        } else {
            Chart(data.points) { point in
                LineMark(
                    x: .value("Time", point.date),
                    y: .value("Minutes", point.minutes)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(.indigo)

                AreaMark(
                    x: .value("Time", point.date),
                    y: .value("Minutes", point.minutes)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(.indigo.opacity(0.15))
            }
            .chartYAxis {
                AxisMarks { value in
                    AxisGridLine()
                    AxisValueLabel {
                        if let m = value.as(Double.self) { Text(label(forMinutes: m)) }
                    }
                }
            }
            .frame(height: 130)
        }
    }

    private func label(forMinutes m: Double) -> String {
        m >= 60 ? "\(Int(m / 60))h" : "\(Int(m))m"
    }
}

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
        var minutesByDate: [Date: Double] = [:]
        for await activity in data {
            for await segment in activity.activitySegments {
                minutesByDate[segment.dateInterval.start, default: 0] += segment.totalActivityDuration / 60
            }
        }
        let points = minutesByDate
            .map { UsageTrendData.Point(date: $0.key, minutes: $0.value) }
            .sorted { $0.date < $1.date }
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
                .interpolationMethod(.monotone)
                .foregroundStyle(.indigo)

                AreaMark(
                    x: .value("Time", point.date),
                    y: .value("Minutes", point.minutes)
                )
                .interpolationMethod(.monotone)
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
        guard m >= 60 else { return "\(Int(m))m" }
        let hours = m / 60
        return hours == hours.rounded() ? "\(Int(hours))h" : String(format: "%.1fh", hours)
    }
}

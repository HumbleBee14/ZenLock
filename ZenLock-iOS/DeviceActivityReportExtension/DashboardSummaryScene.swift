import DeviceActivity
import FamilyControls
import ManagedSettings
import SwiftUI

/// Compact two-card summary rendered on the dashboard. Because raw Screen Time
/// data can only be read inside this sandboxed report extension, the dashboard
/// embeds this scene via `DeviceActivityReport(.init("dashboardSummary"))`.
struct DashboardSummaryScene: @preconcurrency DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .init("dashboardSummary")
    let content: (DashboardSummaryData) -> DashboardSummaryView

    init(@ViewBuilder content: @escaping (DashboardSummaryData) -> DashboardSummaryView = { DashboardSummaryView(data: $0) }) {
        self.content = content
    }

    func makeConfiguration(representing data: DeviceActivityResults<DeviceActivityData>) async -> DashboardSummaryData {
        var totalDuration: TimeInterval = 0
        var perApp: [String: (token: ApplicationToken?, name: String, duration: TimeInterval)] = [:]

        for await activity in data {
            for await segment in activity.activitySegments {
                totalDuration += segment.totalActivityDuration
                for await categoryActivity in segment.categories {
                    for await appActivity in categoryActivity.applications {
                        let app = appActivity.application
                        let name = app.localizedDisplayName ?? app.bundleIdentifier ?? "Unknown"
                        let key = app.bundleIdentifier ?? name
                        let prev = perApp[key]?.duration ?? 0
                        perApp[key] = (
                            token: app.token,
                            name: name,
                            duration: prev + appActivity.totalActivityDuration
                        )
                    }
                }
            }
        }

        let top = perApp
            .filter { $0.value.duration >= 60 }
            .max { $0.value.duration < $1.value.duration }

        let topApp = top.map {
            TopAppInfo(token: $0.value.token, name: $0.value.name, duration: $0.value.duration)
        }

        return DashboardSummaryData(
            totalDuration: totalDuration,
            topApp: topApp
        )
    }
}

struct TopAppInfo {
    let token: ApplicationToken?
    let name: String
    let duration: TimeInterval
}

struct DashboardSummaryData {
    let totalDuration: TimeInterval
    let topApp: TopAppInfo?
}

struct DashboardSummaryView: View {
    let data: DashboardSummaryData

    var body: some View {
        HStack(spacing: 12) {
            screenTimeCard
                .frame(maxWidth: .infinity)
            topAppCard
                .frame(maxWidth: .infinity)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private var screenTimeCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Screen Time")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.secondary)
                .lineLimit(1)

            HStack(spacing: 10) {
                Image(systemName: "hourglass")
                    .font(.system(size: 22))
                    .foregroundStyle(.indigo)
                    .frame(width: 30, height: 30)
                Text(formatted(data.totalDuration))
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Text(dateLabel)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .frame(minHeight: 112)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var topAppCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Most used today")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.secondary)
                .lineLimit(1)

            if let app = data.topApp {
                HStack(spacing: 10) {
                    Group {
                        if let token = app.token {
                            Label(token)
                                .labelStyle(IconOnlyLabelStyle())
                        } else {
                            Image(systemName: "app.fill")
                                .font(.system(size: 28))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .frame(width: 30, height: 30)

                    Text(displayName(for: app.name))
                        .font(.system(size: 18, weight: .bold))
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .minimumScaleFactor(0.6)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Text(formatted(app.duration))
                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            } else {
                HStack(spacing: 10) {
                    Image(systemName: "app.dashed")
                        .font(.system(size: 22))
                        .foregroundStyle(.secondary)
                        .frame(width: 30, height: 30)
                    Text("—")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Text("No usage yet")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .frame(minHeight: 112)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var dateLabel: String {
        let f = DateFormatter()
        f.dateFormat = "MMMM d"
        return f.string(from: Date())
    }

    private func displayName(for name: String) -> String {
        guard name.contains("."), name.split(separator: ".").count >= 2 else { return name }
        if let last = name.split(separator: ".").last {
            return String(last).prefix(1).uppercased() + String(last).dropFirst()
        }
        return name
    }

    private func formatted(_ seconds: TimeInterval) -> String {
        let h = Int(seconds) / 3600
        let m = (Int(seconds) % 3600) / 60
        if h > 0 { return "\(h)h \(m)m" }
        return "\(m)m"
    }
}

private struct IconOnlyLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.icon.font(.system(size: 30))
    }
}

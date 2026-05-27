import DeviceActivity
import FamilyControls
import ManagedSettings
import SwiftUI

struct PerAppScene: @preconcurrency DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .init("perApp")
    let content: (PerAppData) -> PerAppView

    init(@ViewBuilder content: @escaping (PerAppData) -> PerAppView = { PerAppView(data: $0) }) {
        self.content = content
    }

    func makeConfiguration(representing data: DeviceActivityResults<DeviceActivityData>) async -> PerAppData {
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

        let sortedTuples = perApp
            .filter { $0.value.duration > 0 }
            .sorted { $0.value.duration > $1.value.duration }
            .prefix(20)

        let rows = sortedTuples.map {
            PerAppRow(token: $0.value.token, name: $0.value.name, duration: $0.value.duration)
        }

        let snapshotRows = sortedTuples.map {
            ScreenTimeSnapshot.Row(
                bundleID: $0.key,
                name: $0.value.name,
                duration: $0.value.duration
            )
        }
        let snapshot = ScreenTimeSnapshot(
            totalDuration: totalDuration,
            rows: snapshotRows,
            capturedAt: Date()
        )
        ScreenTimeSnapshot.save(snapshot)

        return PerAppData(totalDuration: totalDuration, rows: rows)
    }
}

struct PerAppRow: Identifiable, Hashable {
    let id = UUID()
    let token: ApplicationToken?
    let name: String
    let duration: TimeInterval
}

struct PerAppData {
    let totalDuration: TimeInterval
    let rows: [PerAppRow]
}

struct PerAppView: View {
    let data: PerAppData

    private var goalMinutes: Int {
        let defaults = UserDefaults(suiteName: "group.com.humblebee.zenlock")
        let stored = defaults?.integer(forKey: "zen_daily_goal_minutes") ?? 0
        return stored > 0 ? stored : 180
    }

    private var goalSeconds: TimeInterval { TimeInterval(goalMinutes * 60) }
    private var progress: Double {
        guard goalSeconds > 0 else { return 0 }
        return min(data.totalDuration / goalSeconds, 1.0)
    }
    private var percentUsed: Int { Int(progress * 100) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                goalCard
                if data.rows.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "hourglass")
                            .font(.system(size: 32))
                            .foregroundStyle(.secondary)
                        Text("No usage recorded yet today.")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                } else {
                    ForEach(data.rows) { row in
                        appRow(row)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 20)
            .padding(.bottom, 24)
        }
    }

    private var goalCard: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .trim(from: 0.15, to: 0.85)
                    .stroke(Color.secondary.opacity(0.2), style: StrokeStyle(lineWidth: 20, lineCap: .round))
                    .rotationEffect(.degrees(126))
                Circle()
                    .trim(from: 0.15, to: 0.15 + (0.70 * progress))
                    .stroke(progressColor, style: StrokeStyle(lineWidth: 20, lineCap: .round))
                    .rotationEffect(.degrees(126))
                    .animation(.easeInOut(duration: 0.6), value: progress)
                VStack(spacing: 4) {
                    Text(formatted(data.totalDuration))
                        .font(.system(size: 34, weight: .bold))
                    HStack(spacing: 8) {
                        Text("\(percentLeft)% left")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text("•")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text("\(formatGoal(goalMinutes)) goal")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .offset(y: 10)
            }
            .frame(height: 180)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 18))
    }

    private var percentLeft: Int { max(0, 100 - percentUsed) }
    private var progressColor: Color {
        progress < 0.8 ? .green : (progress < 1.0 ? .orange : .red)
    }

    @ViewBuilder
    private func appRow(_ row: PerAppRow) -> some View {
        HStack(spacing: 12) {
            if let token = row.token {
                Label(token)
                    .labelStyle(IconOnlyLabelStyle())
                    .frame(width: 36, height: 36)
            } else {
                Image(systemName: "app.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(.secondary)
                    .frame(width: 36, height: 36)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(row.name)
                    .font(.callout)
                    .lineLimit(1)
                ProgressView(value: row.duration / max(data.rows.first?.duration ?? 1, 1))
                    .tint(.indigo)
            }
            Spacer()
            Text(formatted(row.duration))
                .font(.callout.monospacedDigit())
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }

    private func formatted(_ seconds: TimeInterval) -> String {
        let h = Int(seconds) / 3600
        let m = (Int(seconds) % 3600) / 60
        if h > 0 { return "\(h)h \(m)m" }
        return "\(m)m"
    }

    private func formatGoal(_ minutes: Int) -> String {
        let h = minutes / 60
        let m = minutes % 60
        if h == 0 { return "\(m)m" }
        if m == 0 { return "\(h)h" }
        return "\(h)h \(m)m"
    }
}

private struct IconOnlyLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.icon.font(.system(size: 28))
    }
}

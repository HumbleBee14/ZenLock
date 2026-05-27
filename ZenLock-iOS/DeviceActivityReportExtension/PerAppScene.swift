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

        let rows = perApp
            .filter { $0.value.duration >= 60 }
            .sorted { $0.value.duration > $1.value.duration }
            .map {
                PerAppRow(token: $0.value.token, name: $0.value.name, duration: $0.value.duration)
            }

        let storedGoal = UserDefaults(suiteName: Constants.appGroupID)?
            .integer(forKey: Constants.Keys.dailyGoalMinutes) ?? 0
        let goalMinutes = storedGoal > 0 ? storedGoal : 180

        return PerAppData(totalDuration: totalDuration, rows: rows, goalMinutes: goalMinutes)
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
    let goalMinutes: Int
}

struct PerAppView: View {
    let data: PerAppData

    private var goalMinutes: Int { data.goalMinutes }
    private var goalSeconds: TimeInterval { TimeInterval(goalMinutes * 60) }
    private var progress: Double {
        guard goalSeconds > 0 else { return 0 }
        return min(data.totalDuration / goalSeconds, 1.0)
    }
    private var rawPercentUsed: Int {
        guard goalSeconds > 0 else { return 0 }
        return Int((data.totalDuration / goalSeconds) * 100)
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                goalCard

                if data.rows.isEmpty {
                    emptyState
                } else {
                    appsCard
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 28)
        }
    }

    private var goalCard: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .trim(from: 0.15, to: 0.85)
                    .stroke(Color.primary.opacity(0.08), style: StrokeStyle(lineWidth: 18, lineCap: .round))
                    .rotationEffect(.degrees(126))

                Circle()
                    .trim(from: 0.15, to: 0.15 + (0.70 * max(progress, 0.001)))
                    .stroke(ringGradient, style: StrokeStyle(lineWidth: 18, lineCap: .round))
                    .rotationEffect(.degrees(126))
                    .shadow(color: progressColor.opacity(0.35), radius: 8, x: 0, y: 0)
                    .animation(.easeInOut(duration: 0.6), value: progress)

                VStack(spacing: 6) {
                    Text(formatted(data.totalDuration))
                        .font(.system(size: 40, weight: .bold, design: .rounded))
                        .monospacedDigit()
                    HStack(spacing: 6) {
                        Circle()
                            .fill(progressColor)
                            .frame(width: 6, height: 6)
                        Text("\(rawPercentUsed)% used of \(formatGoal(goalMinutes)) goal")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.secondary)
                    }
                }
                .offset(y: 8)
            }
            .frame(height: 200)
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .padding(.horizontal, 16)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color.primary.opacity(0.04))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.06), lineWidth: 1)
        )
    }

    private var appsCard: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Top Apps")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                    .tracking(0.5)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 10)

            VStack(spacing: 0) {
                ForEach(Array(data.rows.enumerated()), id: \.element.id) { index, row in
                    appRow(row)
                    if index < data.rows.count - 1 {
                        Divider()
                            .padding(.leading, 64)
                            .opacity(0.4)
                    }
                }
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color.primary.opacity(0.04))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.06), lineWidth: 1)
        )
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "hourglass")
                .font(.system(size: 36, weight: .light))
                .foregroundStyle(.tertiary)
            Text("No usage recorded yet today")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color.primary.opacity(0.04))
        )
    }

    private var progressColor: Color {
        progress < 0.6 ? Color(red: 0.35, green: 0.78, blue: 0.50)
            : progress < 0.9 ? Color(red: 0.98, green: 0.65, blue: 0.20)
            : Color(red: 0.95, green: 0.35, blue: 0.40)
    }
    private var ringGradient: AngularGradient {
        AngularGradient(
            colors: [progressColor.opacity(0.7), progressColor, progressColor.opacity(0.9)],
            center: .center
        )
    }

    @ViewBuilder
    private func appRow(_ row: PerAppRow) -> some View {
        let maxDuration = max(data.rows.first?.duration ?? 1, 1)
        let fraction = row.duration / maxDuration

        HStack(spacing: 14) {
            Group {
                if let token = row.token {
                    Label(token)
                        .labelStyle(IconOnlyLabelStyle())
                        .scaleEffect(1.6)
                } else {
                    Image(systemName: "app.fill")
                        .font(.system(size: 44))
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 56, height: 56)

            VStack(alignment: .leading, spacing: 6) {
                Text(displayName(for: row))
                    .font(.system(size: 15, weight: .semibold))
                    .lineLimit(1)
                    .foregroundStyle(.primary)

                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.primary.opacity(0.08))
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [Color(red: 0.40, green: 0.35, blue: 0.95),
                                             Color(red: 0.55, green: 0.40, blue: 0.95)],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: max(geo.size.width * fraction, 4))
                    }
                }
                .frame(height: 5)
            }

            Text(formatted(row.duration))
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(.secondary)
                .frame(minWidth: 44, alignment: .trailing)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func displayName(for row: PerAppRow) -> String {
        let name = row.name
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
        configuration.icon.font(.system(size: 40))
    }
}

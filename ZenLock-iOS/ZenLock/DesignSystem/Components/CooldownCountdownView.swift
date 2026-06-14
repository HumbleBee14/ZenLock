import SwiftUI

/// Cool-down countdown with time, progress bar, and cancel action.
struct CooldownCountdownView: View {
    let endsAt: Date
    let startedAt: Date
    var compact: Bool = false
    let onCancel: () -> Void

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            let remaining = max(0, Int(endsAt.timeIntervalSince(now)))
            let total = endsAt.timeIntervalSince(startedAt)
            let progress = total > 0 ? min(max(now.timeIntervalSince(startedAt) / total, 0), 1) : 1
            let timeText = String(format: "%02d:%02d", remaining / 60, remaining % 60)

            if compact {
                compactBody(timeText: timeText, progress: progress)
            } else {
                fullBody(timeText: timeText, progress: progress)
            }
        }
    }

    private func compactBody(timeText: String, progress: Double) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "hourglass")
                .font(.caption2)
            Text(timeText)
                .font(ZenTheme.caption.monospacedDigit().weight(.semibold))
            ProgressView(value: progress)
                .progressViewStyle(.linear)
                .tint(ZenTheme.accent)
                .frame(maxWidth: .infinity)
            Button(action: onCancel) {
                Image(systemName: "xmark.circle.fill")
                    .font(.body)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .foregroundStyle(ZenTheme.accent)
    }

    private func fullBody(timeText: String, progress: Double) -> some View {
        GlassCard {
            VStack(spacing: ZenTheme.Spacing.sm) {
                Label("Cooling down", systemImage: "hourglass")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.accent)
                Text(timeText)
                    .font(.system(size: 36, weight: .bold, design: .monospaced))
                    .foregroundStyle(ZenTheme.accent)
                ProgressView(value: progress)
                    .progressViewStyle(.linear)
                    .tint(ZenTheme.accent)
                Text("Apps unlock automatically when this ends.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
                ZenButton(title: "Keep focusing", icon: "arrow.uturn.backward", style: .primary, action: onCancel)
            }
            .frame(maxWidth: .infinity)
            .padding(ZenTheme.Spacing.md)
        }
    }
}

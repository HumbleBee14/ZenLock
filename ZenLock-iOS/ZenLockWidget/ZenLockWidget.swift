import WidgetKit
import SwiftUI

@main
struct ZenLockWidgetBundle: WidgetBundle {
    var body: some Widget {
        ZenLockStatusWidget()
    }
}

struct ZenLockStatusWidget: Widget {
    let kind: String = "ZenLockStatusWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: StatusProvider()) { entry in
            ZenLockWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("ZenLock Focus")
        .description("Active blocking groups + a one-tap path to a Quick Focus session.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct StatusEntry: TimelineEntry {
    let date: Date
    let activeCount: Int
    let totalCount: Int
    let focusScore: Int
    let nextGroupName: String?
}

struct StatusProvider: TimelineProvider {
    private let appGroupID = "group.com.humblebee.zenlock"

    func placeholder(in context: Context) -> StatusEntry {
        StatusEntry(date: .now, activeCount: 2, totalCount: 4, focusScore: 78, nextGroupName: "Evening Wind-down")
    }

    func getSnapshot(in context: Context, completion: @escaping (StatusEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<StatusEntry>) -> Void) {
        let entry = currentEntry()
        let next = Date().addingTimeInterval(30 * 60)
        completion(Timeline(entries: [entry], policy: .after(next)))
    }

    private func currentEntry() -> StatusEntry {
        let defaults = UserDefaults(suiteName: appGroupID)
        let groups: [SharedBlockGroup] = {
            guard let data = defaults?.data(forKey: "zen_block_groups"),
                  let g = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
                return []
            }
            return g
        }()
        let active = groups.filter(\.isActive)
        let nextName = active.first?.name ?? groups.first?.name
        let score = defaults?.integer(forKey: "zen_widget_focus_score") ?? 0
        return StatusEntry(
            date: .now,
            activeCount: active.count,
            totalCount: groups.count,
            focusScore: score,
            nextGroupName: nextName
        )
    }
}

struct ZenLockWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: StatusEntry

    var body: some View {
        switch family {
        case .systemSmall: smallBody
        default: mediumBody
        }
    }

    private var smallBody: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "shield.lefthalf.filled")
                    .foregroundStyle(.indigo)
                Spacer()
                Text("\(entry.activeCount)/\(entry.totalCount)")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text("\(entry.focusScore)")
                .font(.system(size: 38, weight: .bold))
            Text("focus score")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private var mediumBody: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text("ZenLock")
                    .font(.headline)
                Text("\(entry.activeCount) of \(entry.totalCount) groups active")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text("Focus")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text("\(entry.focusScore)")
                    .font(.system(size: 28, weight: .bold))
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                if let next = entry.nextGroupName {
                    Text("Up next")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(next)
                        .font(.subheadline)
                        .lineLimit(2)
                        .multilineTextAlignment(.trailing)
                }
                Spacer()
                Link(destination: URL(string: "zenlock://quick-focus")!) {
                    Label("Quick Focus", systemImage: "timer")
                        .font(.caption.weight(.semibold))
                }
            }
        }
    }
}

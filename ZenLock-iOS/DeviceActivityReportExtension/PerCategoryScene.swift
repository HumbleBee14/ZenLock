import DeviceActivity
import SwiftUI

struct PerCategoryScene: DeviceActivityReportScene {
    let context: DeviceActivityReport.Context = .init("perCategory")
    let content: (PerCategoryData) -> PerCategoryView

    init(@ViewBuilder content: @escaping (PerCategoryData) -> PerCategoryView = { PerCategoryView(data: $0) }) {
        self.content = content
    }

    func makeConfiguration(representing data: DeviceActivityResults<DeviceActivityData>) async -> PerCategoryData {
        var rows: [PerCategoryData.Row] = []
        for await activity in data {
            for await segment in activity.activitySegments {
                for await category in segment.categories {
                    let duration = category.totalActivityDuration
                    let name = category.category.localizedDisplayName ?? "Other"
                    rows.append(.init(name: name, duration: duration))
                }
            }
        }
        rows.sort { $0.duration > $1.duration }
        return PerCategoryData(rows: Array(rows.prefix(8)))
    }
}

struct PerCategoryData {
    struct Row: Identifiable {
        let id = UUID()
        let name: String
        let duration: TimeInterval
    }
    let rows: [Row]
}

struct PerCategoryView: View {
    let data: PerCategoryData

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("By Category")
                .font(.headline)
            if data.rows.isEmpty {
                Text("No usage recorded for this range.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(data.rows) { row in
                    HStack {
                        Text(row.name)
                            .font(.subheadline)
                        Spacer()
                        Text(formatted(row.duration))
                            .font(.subheadline.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
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

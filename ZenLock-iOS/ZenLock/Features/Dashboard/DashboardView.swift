import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = DashboardViewModel()

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        headerSection
                        statsSection
                        groupsSection
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.top, ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { viewModel.showSettings = true } label: {
                        Image(systemName: "gearshape.fill")
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                }
            }
            .sheet(isPresented: $viewModel.showCreateGroup) {
                CreateGroupView { viewModel.loadGroups(context: modelContext) }
            }
            .sheet(isPresented: $viewModel.showSettings) {
                SettingsView()
            }
            .onAppear { viewModel.loadGroups(context: modelContext) }
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: ZenTheme.Spacing.xs) {
            Text("ZenLock")
                .font(ZenTheme.largeTitle)
                .foregroundStyle(ZenTheme.text)
            Text("Stay focused. Stay present.")
                .font(ZenTheme.callout)
                .foregroundStyle(ZenTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var statsSection: some View {
        HStack(spacing: ZenTheme.Spacing.md) {
            StatCard(
                title: "Active",
                value: "\(viewModel.activeGroupCount)",
                icon: "shield.checkered",
                color: ZenTheme.success
            )
            StatCard(
                title: "Groups",
                value: "\(viewModel.totalGroupCount)",
                icon: "square.stack.3d.up",
                color: ZenTheme.primary
            )
        }
    }

    private var groupsSection: some View {
        VStack(spacing: ZenTheme.Spacing.md) {
            HStack {
                Text("Block Groups")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                Spacer()
                Button { viewModel.showCreateGroup = true } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundStyle(ZenTheme.primary)
                }
            }

            if viewModel.groups.isEmpty {
                emptyState
            } else {
                ForEach(viewModel.groups) { group in
                    GroupRow(
                        group: group,
                        onToggle: { viewModel.toggleGroup(group, context: modelContext) },
                        onDelete: { viewModel.deleteGroup(group, context: modelContext) }
                    )
                }
            }
        }
    }

    private var emptyState: some View {
        GlassCard {
            VStack(spacing: ZenTheme.Spacing.md) {
                Image(systemName: "shield.slash")
                    .font(.system(size: 48))
                    .foregroundStyle(ZenTheme.textSecondary)
                Text("No block groups yet")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                Text("Create your first group to start blocking distracting apps.")
                    .font(ZenTheme.callout)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
                ZenButton(title: "Create Group", icon: "plus") {
                    viewModel.showCreateGroup = true
                }
            }
            .padding(ZenTheme.Spacing.xl)
        }
    }
}

private struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                HStack {
                    Image(systemName: icon)
                        .foregroundStyle(color)
                    Spacer()
                }
                Text(value)
                    .font(ZenTheme.title)
                    .foregroundStyle(ZenTheme.text)
                Text(title)
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }
}

private struct GroupRow: View {
    let group: BlockGroup
    let onToggle: () -> Void
    let onDelete: () -> Void

    var body: some View {
        GlassCard {
            HStack(spacing: ZenTheme.Spacing.md) {
                GroupIcon(
                    systemName: group.blockMode.icon,
                    color: group.isActive ? ZenTheme.success : ZenTheme.textSecondary
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text(group.name)
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    Text(group.blockMode.displayName)
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                }

                Spacer()

                Button {
                    withAnimation(ZenTheme.springy) { onToggle() }
                } label: {
                    Image(systemName: group.isActive ? "shield.checkered" : "shield.slash")
                        .font(.title2)
                        .foregroundStyle(group.isActive ? ZenTheme.success : ZenTheme.textSecondary)
                        .contentTransition(.symbolEffect(.replace))
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
        .contextMenu {
            Button(role: .destructive) { onDelete() } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }
}

extension BlockMode {
    var icon: String {
        switch self {
        case .timeBased: "clock.fill"
        case .usageBased: "hourglass"
        case .frictionBased: "hand.raised.fill"
        }
    }

    var displayName: String {
        switch self {
        case .timeBased: "Time-Based"
        case .usageBased: "Usage-Based"
        case .frictionBased: "Friction-Based"
        }
    }
}

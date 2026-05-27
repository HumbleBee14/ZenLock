import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(DeepLinkRouter.self) private var router
    @State private var viewModel = DashboardViewModel()
    @State private var showQuickFocus = false

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        headerSection
                        statsSection
                        quickFocusButton
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
            .sheet(isPresented: $showQuickFocus) {
                QuickFocusSheet()
            }
            .alert("Heads up", isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) { viewModel.errorMessage = nil }
            } message: {
                Text(viewModel.errorMessage ?? "")
            }
            .onAppear {
                viewModel.loadGroups(context: modelContext)
                if router.consume() == .quickFocus { showQuickFocus = true }
            }
            .onChange(of: router.pending) { _, _ in
                if router.consume() == .quickFocus { showQuickFocus = true }
            }
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

    private var quickFocusButton: some View {
        Button { showQuickFocus = true } label: {
            GlassCard {
                HStack(spacing: ZenTheme.Spacing.md) {
                    Image(systemName: "timer")
                        .font(.title2)
                        .foregroundStyle(ZenTheme.accent)
                        .frame(width: 44, height: 44)
                        .background(Circle().fill(ZenTheme.accent.opacity(0.15)))
                    VStack(alignment: .leading) {
                        Text("Quick Focus Session")
                            .font(ZenTheme.headline)
                            .foregroundStyle(ZenTheme.text)
                        Text("Block apps right now for a set amount of time")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
        .buttonStyle(.plain)
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
                    NavigationLink {
                        EditGroupView(group: group)
                    } label: {
                        GroupRow(
                            group: group,
                            onToggle: { viewModel.toggleGroup(group, context: modelContext) },
                            onDelete: { viewModel.deleteGroup(group, context: modelContext) }
                        )
                    }
                    .buttonStyle(.plain)
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
                    systemName: group.icon,
                    color: group.isActive ? Color(hex: group.colorHex) : ZenTheme.textSecondary
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text(group.name)
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    Text(statusLine)
                        .font(ZenTheme.caption)
                        .foregroundStyle(statusColor)
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

    private var statusLine: String {
        if !group.isActive { return group.blockMode.displayName }
        switch group.blockMode {
        case .timeBased:
            return ScheduleEvaluator.isWithinSchedule(group.toShared())
                ? "Blocking now · time-based"
                : "Armed · waiting for window"
        case .usageBased:
            return "Armed · blocks at usage limit"
        }
    }

    private var statusColor: Color {
        guard group.isActive else { return ZenTheme.textSecondary }
        switch group.blockMode {
        case .timeBased:
            return ScheduleEvaluator.isWithinSchedule(group.toShared())
                ? ZenTheme.success
                : ZenTheme.warning
        case .usageBased:
            return ZenTheme.success
        }
    }
}

extension BlockMode {
    var icon: String {
        switch self {
        case .timeBased: "clock.fill"
        case .usageBased: "hourglass"
        }
    }

    var displayName: String {
        switch self {
        case .timeBased: "Time-Based"
        case .usageBased: "Usage-Based"
        }
    }
}

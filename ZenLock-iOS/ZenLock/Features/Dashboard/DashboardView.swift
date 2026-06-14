import SwiftUI
import SwiftData
import DeviceActivity

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(DeepLinkRouter.self) private var router
    @State private var viewModel = DashboardViewModel()
    @State private var showQuickFocus = false
    @State private var activeFocus: ActiveSession?
    @State private var now = Date()
    @State private var summaryFilterEnd = Date()

    private let focusTimer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        headerRow
                        summarySection
                        quickFocusButton
                        groupsSection
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.top, ZenTheme.Spacing.sm)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
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
            .zenToast(Bindable(viewModel).toast)
            .alert(
                "Stop the focus session?",
                isPresented: Binding(
                    get: { viewModel.stopConfirmGroup != nil },
                    set: { if !$0 { viewModel.stopConfirmGroup = nil } }
                )
            ) {
                Button("No", role: .cancel) {}
                Button("Yes", role: .destructive) {
                    viewModel.confirmStop(context: modelContext)
                }
            }
            .onAppear {
                viewModel.loadGroups(context: modelContext)
                activeFocus = ActiveSession.load()
                summaryFilterEnd = Date()
                if router.consume() == .quickFocus { showQuickFocus = true }
            }
            .onChange(of: router.pending) { _, _ in
                if router.consume() == .quickFocus { showQuickFocus = true }
            }
            .onChange(of: showQuickFocus) { _, _ in
                activeFocus = ActiveSession.load()
            }
            .onReceive(focusTimer) { _ in
                now = Date()
                let fresh = ActiveSession.load()
                if let f = fresh, now >= f.endsAt {
                    activeFocus = nil
                } else {
                    activeFocus = fresh
                }
                viewModel.finalizeElapsedCooldowns(context: modelContext)
            }
        }
    }

    private var headerRow: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text("ZenLock")
                    .font(ZenTheme.title.weight(.bold))
                    .foregroundStyle(ZenTheme.text)
                Text("Stay focused. Stay present.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            Spacer(minLength: ZenTheme.Spacing.md)
            Button { viewModel.showSettings = true } label: {
                Image(systemName: "gearshape.fill")
                    .font(.title3)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(ZenTheme.text.opacity(0.06)))
            }
        }
    }

    private var summarySection: some View {
        DeviceActivityReport(
            DeviceActivityReport.Context("dashboardSummary"),
            filter: summaryFilter
        )
        .frame(height: 132)
    }

    private var summaryFilter: DeviceActivityFilter {
        let start = Calendar.current.startOfDay(for: Date())
        return DeviceActivityFilter(
            segment: .daily(during: DateInterval(start: start, end: summaryFilterEnd))
        )
    }

    private var quickFocusButton: some View {
        Button { showQuickFocus = true } label: {
            if let a = activeFocus {
                activeFocusCard(a)
            } else {
                idleFocusCard
            }
        }
        .buttonStyle(.plain)
    }

    private var idleFocusCard: some View {
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

    private func activeFocusCard(_ a: ActiveSession) -> some View {
        let coolingDown = (a.cooldownEndsAt.map { now < $0 }) ?? false
        return GlassCard {
            HStack(spacing: ZenTheme.Spacing.md) {
                ZStack {
                    Circle()
                        .fill(ZenTheme.success.opacity(0.18))
                        .frame(width: 44, height: 44)
                    Image(systemName: "shield.lefthalf.filled")
                        .font(.title2)
                        .foregroundStyle(ZenTheme.success)
                }
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(coolingDown ? ZenTheme.accent : ZenTheme.success)
                            .frame(width: 8, height: 8)
                        Text(coolingDown ? "Cool-down in progress" : "Focus session active")
                            .font(ZenTheme.headline)
                            .foregroundStyle(ZenTheme.text)
                    }
                    Text("\(formattedRemaining(a.endsAt)) until apps unlock")
                        .font(ZenTheme.caption.monospacedDigit())
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func formattedRemaining(_ end: Date) -> String {
        let total = max(0, Int(end.timeIntervalSince(now)))
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 { return String(format: "%d:%02d:%02d", h, m, s) }
        return String(format: "%02d:%02d", m, s)
    }

    private var groupsSection: some View {
        VStack(spacing: ZenTheme.Spacing.md) {
            HStack {
                Text("Focus Sessions")
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
                            now: now,
                            pendingUnlock: viewModel.pendingUnlock(for: group),
                            onToggle: { viewModel.toggleGroup(group, context: modelContext) },
                            onDelete: { viewModel.deleteGroup(group, context: modelContext) },
                            onCancelCooldown: { viewModel.cancelStop(group) }
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
                Text("No focus sessions yet")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                Text("Create your first focus session to start blocking distracting apps.")
                    .font(ZenTheme.callout)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
                ZenButton(title: "Create Focus Session", icon: "plus") {
                    viewModel.showCreateGroup = true
                }
            }
            .padding(ZenTheme.Spacing.xl)
        }
    }
}

private struct GroupRow: View {
    let group: BlockGroup
    let now: Date
    let pendingUnlock: AccountabilityManager.PendingUnlock?
    let onToggle: () -> Void
    let onDelete: () -> Void
    let onCancelCooldown: () -> Void

    var body: some View {
        GlassCard {
            HStack(spacing: ZenTheme.Spacing.md) {
                GroupIcon(
                    systemName: group.icon,
                    color: group.isActive ? Color(hex: group.colorHex) : ZenTheme.textSecondary
                )

                VStack(alignment: .leading, spacing: 4) {
                    Text(group.name)
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    if let pending = pendingUnlock {
                        CooldownCountdownView(
                            endsAt: pending.unlocksAt,
                            startedAt: pending.requestedAt,
                            compact: true,
                            onCancel: onCancelCooldown
                        )
                    } else {
                        statusView
                    }
                }

                Spacer()

                if pendingUnlock == nil {
                    Button {
                        withAnimation(ZenTheme.springy) { onToggle() }
                    } label: {
                        Image(systemName: group.isActive ? "shield.checkered" : "shield.slash")
                            .font(.title2)
                            .foregroundStyle(group.isActive ? ZenTheme.success : ZenTheme.textSecondary)
                            .contentTransition(.symbolEffect(.replace))
                    }
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

    private var statusView: some View {
        HStack(spacing: 5) {
            Text(statusWord)
            Image(systemName: group.blockMode.icon)
            if group.deepFocusEnabled {
                Image(systemName: "lock.fill")
            }
        }
        .font(ZenTheme.caption)
        .foregroundStyle(statusColor)
    }

    private var statusWord: String {
        if !group.isActive { return "Off" }
        switch group.blockMode {
        case .timeBased:
            return ScheduleEvaluator.isWithinSchedule(group.toShared()) ? "Blocking" : "Armed"
        case .usageBased:
            return "Armed"
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

import SwiftUI
import FamilyControls

struct OnboardingView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var page = 0
    @State private var scrollID: Int? = 0
    @State private var screenTimeManager = ScreenTimeManager()
    @State private var isAuthorizing = false
    @State private var authError: String?
    @State private var showOpenSettings = false
    var onComplete: () -> Void

    private let pageCount = 4

    var body: some View {
        ZStack {
            AmbientBackdrop(page: page, animated: !reduceMotion)

            VStack(spacing: 0) {
                ScrollView(.horizontal) {
                    HStack(spacing: 0) {
                        WelcomePage(animated: !reduceMotion)
                            .containerRelativeFrame(.horizontal)
                            .id(0)
                        FactsPage(isCurrent: page == 1, animated: !reduceMotion)
                            .containerRelativeFrame(.horizontal)
                            .id(1)
                        FeaturesPage(isCurrent: page == 2, animated: !reduceMotion)
                            .containerRelativeFrame(.horizontal)
                            .id(2)
                        AccessPage(
                            isAuthorizing: isAuthorizing,
                            error: authError,
                            showOpenSettings: showOpenSettings
                        )
                        .containerRelativeFrame(.horizontal)
                        .id(3)
                    }
                    .scrollTargetLayout()
                }
                .scrollTargetBehavior(.paging)
                .scrollIndicators(.hidden)
                .scrollPosition(id: $scrollID)
                .onChange(of: scrollID) { _, id in
                    if let id, id != page { page = id }
                }
                .onChange(of: page) { _, newPage in
                    if scrollID != newPage {
                        withAnimation(ZenTheme.smooth) { scrollID = newPage }
                    }
                }

                footer
            }
        }
        .sensoryFeedback(.selection, trigger: page)
    }

    private var footer: some View {
        VStack(spacing: ZenTheme.Spacing.lg) {
            HStack(spacing: ZenTheme.Spacing.sm) {
                ForEach(0..<pageCount, id: \.self) { index in
                    Capsule()
                        .fill(index == page ? ZenTheme.primary : ZenTheme.text.opacity(0.15))
                        .frame(width: index == page ? 24 : 8, height: 8)
                }
            }
            .animation(ZenTheme.springy, value: page)

            ZenButton(
                title: buttonTitle,
                icon: page == pageCount - 1 ? "checkmark.shield" : "arrow.right",
                isLoading: isAuthorizing
            ) {
                advance()
            }
            .animation(nil, value: page)
        }
        .padding(.horizontal, ZenTheme.Spacing.xl)
        .padding(.bottom, ZenTheme.Spacing.xl)
        .padding(.top, ZenTheme.Spacing.sm)
    }

    private var buttonTitle: String {
        switch page {
        case 0: "Get started"
        case 1, 2: "Continue"
        default: "Allow Screen Time"
        }
    }

    private func advance() {
        if page < pageCount - 1 {
            page += 1
        } else {
            authorize()
        }
    }

    private func authorize() {
        isAuthorizing = true
        authError = nil
        showOpenSettings = false
        Task {
            do {
                try await screenTimeManager.requestAuthorization()
                _ = await NotificationManager.shared.requestPermission()
                AppGroupStorage().setBool(true, forKey: Constants.Keys.onboardingCompleted)
                await MainActor.run { onComplete() }
            } catch {
                let (message, offerSettings) = describe(error)
                await MainActor.run {
                    authError = message
                    showOpenSettings = offerSettings
                    isAuthorizing = false
                }
            }
        }
    }

    private func describe(_ error: Error) -> (String, Bool) {
        if let fc = error as? FamilyControlsError {
            let code = (fc as NSError).code
            switch fc {
            case .authorizationCanceled:
                return ("You cancelled the request. Tap Allow Screen Time and choose Continue.", false)
            case .authorizationConflict:
                return ("Another app already has Screen Time access. Open Settings → Screen Time → Apps with Screen Time Access and remove the other app.", true)
            case .invalidAccountType:
                return ("Screen Time isn't available on this Apple ID. ZenLock needs a personal Apple ID with Screen Time enabled (not a managed/child account).", true)
            case .restricted:
                return ("Screen Time is restricted on this device — usually by a parent/MDM profile. Remove it in Settings → Screen Time and try again.", true)
            case .unavailable:
                return ("Family Controls isn't available. Make sure Screen Time is turned on in Settings → Screen Time.", true)
            case .networkError:
                return ("Couldn't reach Apple to verify Screen Time. Check your connection and try again.", false)
            case .invalidArgument:
                return ("Invalid request to Screen Time. Restart the app and try again.", false)
            case .authenticationMethodUnavailable:
                return ("Screen Time authentication isn't available. In Settings → Screen Time, enable a passcode or biometric and try again.", true)
            default:
                return ("Couldn't authorize Screen Time right now (code \(code)). In Settings → Screen Time → Apps with Screen Time Access, allow ZenLock, then try again or restart the device.", true)
            }
        }
        let ns = error as NSError
        return ("Authorization failed: \(error.localizedDescription) [\(ns.domain) \(ns.code)]", true)
    }
}

private struct AmbientBackdrop: View {
    let page: Int
    let animated: Bool
    @State private var drift = false

    var body: some View {
        ZStack {
            ZenTheme.background.ignoresSafeArea()

            GeometryReader { geo in
                let w = geo.size.width
                let h = geo.size.height

                Circle()
                    .fill(ZenTheme.primary.opacity(0.32))
                    .frame(width: w * 0.9)
                    .blur(radius: 90)
                    .offset(
                        x: primaryOffset.x * w + (drift ? 18 : -18),
                        y: primaryOffset.y * h + (drift ? -14 : 14)
                    )

                Circle()
                    .fill(Color(hex: "F5C878").opacity(0.22))
                    .frame(width: w * 0.7)
                    .blur(radius: 80)
                    .offset(
                        x: accentOffset.x * w + (drift ? -22 : 22),
                        y: accentOffset.y * h + (drift ? 16 : -16)
                    )
            }
            .ignoresSafeArea()
            .animation(.easeInOut(duration: 1.4), value: page)
        }
        .onAppear {
            guard animated else { return }
            withAnimation(.easeInOut(duration: 8).repeatForever(autoreverses: true)) {
                drift = true
            }
        }
    }

    private var primaryOffset: CGPoint {
        switch page {
        case 0: CGPoint(x: -0.25, y: -0.15)
        case 1: CGPoint(x: 0.3, y: 0.4)
        case 2: CGPoint(x: 0.35, y: -0.3)
        default: CGPoint(x: -0.3, y: 0.35)
        }
    }

    private var accentOffset: CGPoint {
        switch page {
        case 0: CGPoint(x: 0.4, y: 0.45)
        case 1: CGPoint(x: -0.4, y: -0.25)
        case 2: CGPoint(x: -0.35, y: 0.5)
        default: CGPoint(x: 0.4, y: -0.2)
        }
    }
}

private struct WelcomePage: View {
    let animated: Bool
    @State private var floating = false

    var body: some View {
        VStack(spacing: ZenTheme.Spacing.xl) {
            Spacer()

            Image("AppIconArt")
                .resizable()
                .frame(width: 136, height: 136)
                .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                .shadow(color: Color(hex: "F5D9A0").opacity(0.35), radius: 36, y: 18)
                .shadow(color: .black.opacity(0.3), radius: 18, y: 10)
                .offset(y: floating ? -6 : 6)

            VStack(spacing: ZenTheme.Spacing.sm) {
                Text("ZenLock")
                    .font(.system(size: 40, weight: .bold, design: .rounded))
                    .foregroundStyle(ZenTheme.text)
                Text("Put the phone down.\nKeep the day.")
                    .font(.system(size: 20, weight: .medium, design: .rounded))
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }

            Spacer()
            Spacer().frame(maxHeight: 80)
        }
        .padding(ZenTheme.Spacing.lg)
        .onAppear {
            guard animated else { return }
            withAnimation(.easeInOut(duration: 3.4).repeatForever(autoreverses: true)) {
                floating = true
            }
        }
    }
}

private struct FactsPage: View {
    let isCurrent: Bool
    let animated: Bool
    @State private var revealedAt: Date?

    private struct Fact {
        let icon: String
        let value: Int
        let unit: String
        let detail: String
        let color: Color
    }

    private let facts: [Fact] = [
        Fact(
            icon: "iphone.gen3.radiowaves.left.and.right",
            value: 96,
            unit: "checks",
            detail: "a day. Once every ten minutes.",
            color: ZenTheme.primary
        ),
        Fact(
            icon: "eye",
            value: 47,
            unit: "sec",
            detail: "on one screen before switching. It was 2½ minutes in 2004.",
            color: ZenTheme.accent
        ),
        Fact(
            icon: "brain.head.profile",
            value: 23,
            unit: "min",
            detail: "to get back into deep work after one interruption.",
            color: ZenTheme.warning
        ),
        Fact(
            icon: "calendar",
            value: 40,
            unit: "+ days",
            detail: "a year on the phone, at today's three hours a day.",
            color: ZenTheme.error
        )
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: ZenTheme.Spacing.xl) {
            Spacer()

            VStack(alignment: .leading, spacing: ZenTheme.Spacing.xs) {
                Text("Where attention goes")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundStyle(ZenTheme.text)
                Text("A few numbers worth knowing.")
                    .font(ZenTheme.callout)
                    .foregroundStyle(ZenTheme.textSecondary)
            }

            VStack(spacing: ZenTheme.Spacing.md) {
                factRow(0, 1)
                factRow(2, 3)
            }

            Spacer()
            Spacer()
        }
        .padding(.horizontal, ZenTheme.Spacing.xl)
        .onAppear { if !animated { revealedAt = .distantPast } }
        .onChange(of: isCurrent, initial: true) { _, current in
            if current, revealedAt == nil { revealedAt = Date() }
        }
    }

    private func factRow(_ first: Int, _ second: Int) -> some View {
        HStack(alignment: .top, spacing: ZenTheme.Spacing.md) {
            factCard(first)
            factCard(second)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func factCard(_ index: Int) -> some View {
        FactCard(
            fact: facts[index],
            startedAt: revealedAt?.addingTimeInterval(Double(index) * 0.12),
            animated: animated
        )
        .opacity(revealedAt == nil ? 0 : 1)
        .scaleEffect(revealedAt == nil ? 0.92 : 1)
        .offset(y: revealedAt == nil ? 24 : 0)
        .animation(
            .spring(duration: 0.6, bounce: 0.2).delay(Double(index) * 0.1),
            value: revealedAt == nil
        )
    }

    private struct FactCard: View {
        let fact: Fact
        let startedAt: Date?
        let animated: Bool
        @State private var finished = false

        private let countDuration = 1.3

        var body: some View {
            GlassCard(cornerRadius: ZenTheme.CornerRadius.xl) {
                VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                    Image(systemName: fact.icon)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(fact.color)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(fact.color.opacity(0.14)))

                    TimelineView(.animation(minimumInterval: 1 / 30, paused: startedAt == nil || finished || !animated)) { context in
                        HStack(alignment: .firstTextBaseline, spacing: 3) {
                            Text("\(displayValue(at: context.date))")
                                .font(.system(size: 34, weight: .bold, design: .rounded))
                                .foregroundStyle(fact.color)
                                .monospacedDigit()
                                .contentTransition(.numericText(countsDown: false))
                            Text(fact.unit)
                                .font(.system(size: 15, weight: .semibold, design: .rounded))
                                .foregroundStyle(fact.color.opacity(0.85))
                        }
                        .onChange(of: context.date) { _, date in
                            if let startedAt, date.timeIntervalSince(startedAt) >= countDuration { finished = true }
                        }
                    }

                    Text(fact.detail)
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(ZenTheme.Spacing.md)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
        }

        private func displayValue(at date: Date) -> Int {
            guard animated, let startedAt, !finished else { return fact.value }
            let t = max(0, min(1, date.timeIntervalSince(startedAt) / countDuration))
            let eased = 1 - pow(1 - t, 3)
            return Int((Double(fact.value) * eased).rounded())
        }
    }
}

private struct FeaturesPage: View {
    let isCurrent: Bool
    let animated: Bool
    @State private var revealed = false

    private struct Feature {
        let icon: String
        let color: Color
        let title: String
        let detail: String
    }

    private let features: [Feature] = [
        Feature(
            icon: "shield.fill",
            color: ZenTheme.primary,
            title: "Block what pulls you in",
            detail: "Apps or whole categories, blocked by iOS itself."
        ),
        Feature(
            icon: "timer",
            color: ZenTheme.accent,
            title: "Right now, or on a schedule",
            detail: "A Quick Focus for the next hour, or every weekday morning."
        ),
        Feature(
            icon: "lock.fill",
            color: ZenTheme.warning,
            title: "Strict Mode",
            detail: "No stopping, no edits, until the session ends."
        ),
        Feature(
            icon: "heart.fill",
            color: ZenTheme.success,
            title: "Free, always",
            detail: "Open source. No ads, no tracking, no upsell."
        )
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: ZenTheme.Spacing.xl) {
            Spacer()

            VStack(alignment: .leading, spacing: ZenTheme.Spacing.xs) {
                Text("Focus, your way")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundStyle(ZenTheme.text)
                Text("Everything you need. Nothing that pulls you back.")
                    .font(ZenTheme.callout)
                    .foregroundStyle(ZenTheme.textSecondary)
            }

            VStack(alignment: .leading, spacing: ZenTheme.Spacing.lg) {
                ForEach(Array(features.enumerated()), id: \.offset) { index, feature in
                    HStack(alignment: .top, spacing: ZenTheme.Spacing.md) {
                        GroupIcon(systemName: feature.icon, color: feature.color, size: 48)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(feature.title)
                                .font(ZenTheme.headline)
                                .foregroundStyle(ZenTheme.text)
                            Text(feature.detail)
                                .font(ZenTheme.caption)
                                .foregroundStyle(ZenTheme.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .opacity(revealed ? 1 : 0)
                    .offset(y: revealed ? 0 : 28)
                    .animation(
                        .spring(duration: 0.6, bounce: 0.18).delay(Double(index) * 0.09),
                        value: revealed
                    )
                }
            }

            Spacer()
            Spacer()
        }
        .padding(.horizontal, ZenTheme.Spacing.xl)
        .onAppear { if !animated { revealed = true } }
        .onChange(of: isCurrent, initial: true) { _, current in
            if current { revealed = true }
        }
    }
}

private struct AccessPage: View {
    let isAuthorizing: Bool
    let error: String?
    let showOpenSettings: Bool

    var body: some View {
        VStack(spacing: ZenTheme.Spacing.xl) {
            Spacer()

            ZStack {
                Circle()
                    .fill(ZenTheme.accent.opacity(0.14))
                    .frame(width: 120, height: 120)
                Image(systemName: "hourglass")
                    .font(.system(size: 50, weight: .medium))
                    .foregroundStyle(ZenTheme.accent)
                    .symbolEffect(.pulse, isActive: isAuthorizing)
            }

            VStack(spacing: ZenTheme.Spacing.sm) {
                Text("One permission")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundStyle(ZenTheme.text)
                Text("ZenLock uses Apple's Screen Time to block the apps you pick.\nEverything stays on your iPhone.")
                    .font(ZenTheme.callout)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }

            if let error {
                VStack(spacing: ZenTheme.Spacing.sm) {
                    Text(error)
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.error)
                        .multilineTextAlignment(.center)
                    if showOpenSettings {
                        Button("Open Settings") {
                            if let url = URL(string: UIApplication.openSettingsURLString) {
                                UIApplication.shared.open(url)
                            }
                        }
                        .font(ZenTheme.caption.weight(.semibold))
                        .foregroundStyle(ZenTheme.primary)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }

            Spacer()
            Spacer()
        }
        .padding(.horizontal, ZenTheme.Spacing.xl)
        .animation(ZenTheme.smooth, value: error)
    }
}

import Foundation
import FamilyControls
import Combine

@Observable
final class ScreenTimeManager {
    private(set) var authorizationStatus: AuthorizationStatus = .notDetermined

    @ObservationIgnored
    private var statusObserver: AnyCancellable?

    init() {
        refreshStatus()
        startObserving()
    }

    var isAuthorized: Bool { authorizationStatus == .approved }

    var isDenied: Bool { authorizationStatus == .denied }

    func requestAuthorization() async throws {
        try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
        await MainActor.run { refreshStatus() }
    }

    func refreshStatus() {
        authorizationStatus = AuthorizationCenter.shared.authorizationStatus
    }

    private func startObserving() {
        statusObserver = AuthorizationCenter.shared
            .$authorizationStatus
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                self?.authorizationStatus = status
            }
    }
}

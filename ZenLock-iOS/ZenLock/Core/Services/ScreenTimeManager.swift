import Foundation
import FamilyControls

@Observable
final class ScreenTimeManager {
    private(set) var authorizationStatus: AuthorizationStatus = .notDetermined

    var isAuthorized: Bool { authorizationStatus == .approved }

    func requestAuthorization() async throws {
        try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
        authorizationStatus = AuthorizationCenter.shared.authorizationStatus
    }

    func refreshStatus() {
        authorizationStatus = AuthorizationCenter.shared.authorizationStatus
    }
}

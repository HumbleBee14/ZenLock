import Foundation
import LocalAuthentication

/// Gates a sensitive action (like stopping a focus session) behind the device
/// owner's Face ID / Touch ID / passcode, so only the phone's owner can do it.
enum BiometricGate {
    /// Uses Apple's native authentication UI: Face ID / Touch ID first, with an
    /// "Enter Passcode" fallback always available if biometrics fail or are
    /// blocked. If no passcode is set at all, there's nothing to authenticate
    /// against, so the action is allowed rather than trapping the owner out.
    static func authenticate(reason: String) async -> Bool {
        let context = LAContext()

        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            return true
        }

        do {
            return try await context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
        } catch {
            return false
        }
    }
}

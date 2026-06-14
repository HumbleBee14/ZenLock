import Foundation
import LocalAuthentication

/// Gates a sensitive action (like stopping a focus session) behind the device
/// owner's Face ID / Touch ID / passcode, so only the phone's owner can do it.
enum BiometricGate {
    /// Prompts for device-owner authentication. Falls back to passcode when
    /// biometrics are unavailable. Returns true only on successful auth.
    /// If the device has no passcode set at all, authentication can't be
    /// required, so we allow the action through (`true`) rather than trapping
    /// the user out of their own session.
    static func authenticate(reason: String) async -> Bool {
        let context = LAContext()
        context.localizedFallbackTitle = "Enter Passcode"

        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            // No passcode/biometrics configured — nothing to authenticate against.
            return true
        }

        do {
            return try await context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
        } catch {
            return false
        }
    }
}

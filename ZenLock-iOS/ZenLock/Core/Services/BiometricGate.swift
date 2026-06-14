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
        let bioContext = LAContext()
        bioContext.localizedFallbackTitle = ""

        var bioError: NSError?
        if bioContext.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &bioError) {
            do {
                return try await bioContext.evaluatePolicy(
                    .deviceOwnerAuthenticationWithBiometrics,
                    localizedReason: reason
                )
            } catch let error as LAError {
                switch error.code {
                case .userCancel, .appCancel, .systemCancel:
                    return false
                case .biometryNotAvailable, .biometryNotEnrolled, .biometryLockout, .userFallback:
                    break // fall through to passcode
                default:
                    return false
                }
            } catch {
                return false
            }
        }

        let passContext = LAContext()
        var passError: NSError?
        // No passcode set → nothing to authenticate against; allow rather than trap the owner out.
        guard passContext.canEvaluatePolicy(.deviceOwnerAuthentication, error: &passError) else {
            return true
        }
        do {
            return try await passContext.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
        } catch {
            return false
        }
    }
}

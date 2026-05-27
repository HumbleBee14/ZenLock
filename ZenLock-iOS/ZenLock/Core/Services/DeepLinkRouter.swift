import Foundation

@Observable
final class DeepLinkRouter {
    enum Destination: Equatable {
        case quickFocus
    }

    var pending: Destination?

    func handle(_ url: URL) {
        guard url.scheme == "zenlock" else { return }
        switch url.host {
        case "quick-focus":
            pending = .quickFocus
        default:
            break
        }
    }

    func consume() -> Destination? {
        defer { pending = nil }
        return pending
    }
}

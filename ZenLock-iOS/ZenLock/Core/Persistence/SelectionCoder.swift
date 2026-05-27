import Foundation
import FamilyControls

enum SelectionCoder {
    static func encode(_ selection: FamilyActivitySelection?) -> Data? {
        guard let selection else { return nil }
        return try? JSONEncoder().encode(selection)
    }

    static func decode(_ data: Data?) -> FamilyActivitySelection? {
        guard let data else { return nil }
        return try? JSONDecoder().decode(FamilyActivitySelection.self, from: data)
    }

    static func isValid(_ data: Data?) -> Bool {
        decode(data) != nil
    }
}

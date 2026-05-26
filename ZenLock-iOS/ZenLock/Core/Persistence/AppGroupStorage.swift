import Foundation

final class AppGroupStorage {
    private let defaults: UserDefaults?

    init(suiteName: String = Constants.appGroupID) {
        self.defaults = UserDefaults(suiteName: suiteName)
    }

    func saveGroups(_ groups: [SharedBlockGroup]) {
        SharedBlockGroup.save(groups, to: defaults)
    }

    func loadGroups() -> [SharedBlockGroup] {
        SharedBlockGroup.load(from: defaults)
    }

    func saveSelection(_ data: Data, forGroupId id: String) {
        defaults?.set(data, forKey: Constants.Keys.selectionPrefix + id)
    }

    func loadSelection(forGroupId id: String) -> Data? {
        defaults?.data(forKey: Constants.Keys.selectionPrefix + id)
    }

    func setGroupActive(_ id: String, _ active: Bool) {
        defaults?.set(active, forKey: Constants.Keys.activeGroupPrefix + id)
    }

    func isGroupActive(_ id: String) -> Bool {
        defaults?.bool(forKey: Constants.Keys.activeGroupPrefix + id) ?? false
    }

    func incrementOpenCount(_ id: String) -> Int {
        let key = Constants.Keys.openCountPrefix + id
        let count = (defaults?.integer(forKey: key) ?? 0) + 1
        defaults?.set(count, forKey: key)
        return count
    }

    func resetOpenCount(_ id: String) {
        defaults?.removeObject(forKey: Constants.Keys.openCountPrefix + id)
    }

    func setScheduleStartTime(_ date: Date, forGroupId id: String) {
        defaults?.set(date, forKey: "schedule_start_\(id)")
    }

    func set<T: Encodable>(_ value: T, forKey key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults?.set(data, forKey: key)
    }

    func get<T: Decodable>(_ type: T.Type, forKey key: String) -> T? {
        guard let data = defaults?.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    func setBool(_ value: Bool, forKey key: String) {
        defaults?.set(value, forKey: key)
    }

    func bool(forKey key: String) -> Bool {
        defaults?.bool(forKey: key) ?? false
    }
}

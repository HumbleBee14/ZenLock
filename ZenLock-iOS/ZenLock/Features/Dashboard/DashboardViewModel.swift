import SwiftUI
import FamilyControls
import SwiftData

@Observable
final class DashboardViewModel {
    var groups: [BlockGroup] = []
    var showCreateGroup = false
    var showSettings = false
    var errorMessage: String?

    private let blockingService: BlockingService

    init(blockingService: BlockingService = BlockingService()) {
        self.blockingService = blockingService
    }

    func loadGroups(context: ModelContext) {
        let descriptor = FetchDescriptor<BlockGroup>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        groups = (try? context.fetch(descriptor)) ?? []
    }

    func toggleGroup(_ group: BlockGroup, context: ModelContext) {
        do {
            if group.isActive {
                switch blockingService.deactivateGroup(group) {
                case .success:
                    break
                case .failure(let error):
                    errorMessage = error.errorDescription
                    return
                }
            } else {
                try blockingService.activateGroup(group)
            }
            try context.save()
        } catch {
            group.isActive = false
            errorMessage = error.localizedDescription
        }
    }

    func deleteGroup(_ group: BlockGroup, context: ModelContext) {
        blockingService.removeGroupFromAppGroups(group.id.uuidString)
        context.delete(group)
        try? context.save()
        loadGroups(context: context)
    }

    var activeGroupCount: Int { groups.filter(\.isActive).count }
    var totalGroupCount: Int { groups.count }
}

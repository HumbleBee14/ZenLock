import DeviceActivity
import SwiftUI

@main
struct ZenLockReportExtension: @preconcurrency DeviceActivityReportExtension {
    @MainActor
    var body: some DeviceActivityReportScene {
        TotalUsageScene()
        PerCategoryScene()
        PerAppScene()
        DashboardSummaryScene()
    }
}

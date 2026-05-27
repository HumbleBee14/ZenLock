import DeviceActivity
import SwiftUI

@main
struct ZenLockReportExtension: DeviceActivityReportExtension {
    var body: some DeviceActivityReportScene {
        TotalUsageScene()
        PerCategoryScene()
        PerAppScene()
    }
}

import SwiftUI

/// Slider over the standardized cool-down steps (1–10 by 1, then 10s to 60).
/// Binds to a minute value, snapping to the nearest allowed option.
struct CooldownSlider: View {
    @Binding var minutes: Int
    var tint: Color = ZenTheme.primary

    private var indexBinding: Binding<Double> {
        Binding(
            get: {
                let idx = CooldownService.options.firstIndex(of: CooldownService.snap(minutes)) ?? 0
                return Double(idx)
            },
            set: { newIndex in
                let i = Int(newIndex.rounded())
                let clamped = min(max(i, 0), CooldownService.options.count - 1)
                minutes = CooldownService.options[clamped]
            }
        )
    }

    var body: some View {
        Slider(
            value: indexBinding,
            in: 0...Double(CooldownService.options.count - 1),
            step: 1
        )
        .tint(tint)
    }
}

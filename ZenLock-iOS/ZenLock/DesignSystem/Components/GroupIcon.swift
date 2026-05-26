import SwiftUI

struct GroupIcon: View {
    let systemName: String
    var color: Color = ZenTheme.primary
    var size: CGFloat = 44

    var body: some View {
        ZStack {
            Circle()
                .fill(color.opacity(0.15))
            Image(systemName: systemName)
                .font(.system(size: size * 0.4, weight: .semibold))
                .foregroundStyle(color)
        }
        .frame(width: size, height: size)
    }
}

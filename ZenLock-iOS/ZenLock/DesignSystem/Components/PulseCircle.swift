import SwiftUI

struct PulseCircle: View {
    let isActive: Bool
    var color: Color = ZenTheme.accent

    @State private var scale: CGFloat = 1.0

    var body: some View {
        ZStack {
            ForEach(0..<3) { i in
                Circle()
                    .stroke(color.opacity(0.3 - Double(i) * 0.1), lineWidth: 2)
                    .scaleEffect(isActive ? scale + CGFloat(i) * 0.2 : 1.0)
            }
            Circle()
                .fill(color.opacity(0.2))
                .scaleEffect(isActive ? scale : 1.0)
        }
        .frame(width: 100, height: 100)
        .onAppear {
            guard isActive else { return }
            withAnimation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true)) {
                scale = 1.3
            }
        }
    }
}

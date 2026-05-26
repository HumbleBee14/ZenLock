import SwiftUI

struct TimerRing: View {
    let progress: Double
    var lineWidth: CGFloat = 8
    var gradient: LinearGradient = ZenTheme.primaryGradient
    var size: CGFloat = 120

    @State private var animatedProgress: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.1), lineWidth: lineWidth)

            Circle()
                .trim(from: 0, to: animatedProgress)
                .stroke(gradient, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: size, height: size)
        .onAppear { withAnimation(.easeInOut(duration: 0.8)) { animatedProgress = progress } }
        .onChange(of: progress) { _, newValue in
            withAnimation(.easeInOut(duration: 0.3)) { animatedProgress = newValue }
        }
    }
}

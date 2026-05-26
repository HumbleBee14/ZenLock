import SwiftUI

struct ZenToggle: View {
    @Binding var isOn: Bool
    let label: String

    var body: some View {
        HStack {
            Text(label)
                .font(ZenTheme.body)
                .foregroundStyle(ZenTheme.text)

            Spacer()

            Button {
                withAnimation(ZenTheme.springy) { isOn.toggle() }
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } label: {
                Capsule()
                    .fill(isOn ? ZenTheme.accent : ZenTheme.surfaceLight)
                    .frame(width: 52, height: 32)
                    .overlay(alignment: isOn ? .trailing : .leading) {
                        Circle()
                            .fill(.white)
                            .padding(3)
                            .frame(width: 32, height: 32)
                            .shadow(color: .black.opacity(0.15), radius: 2, y: 1)
                    }
            }
        }
    }
}

import SwiftUI
import FamilyControls
import ManagedSettings

/// Grid of selected apps and categories using Apple's Label view.
struct SelectionPreview: View {
    let selection: FamilyActivitySelection
    var maxRowsBeforeScroll: Int = 2

    private let columns = [GridItem(.adaptive(minimum: 80, maximum: 110), spacing: 8)]

    var body: some View {
        if isEmpty {
            EmptyView()
        } else {
            ScrollView(.vertical, showsIndicators: false) {
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(Array(selection.applicationTokens), id: \.self) { token in
                        tile { Label(token).labelStyle(TileLabelStyle()) }
                    }
                    ForEach(Array(selection.categoryTokens), id: \.self) { token in
                        tile { Label(token).labelStyle(TileLabelStyle()) }
                    }
                }
                .padding(.vertical, 4)
            }
            .frame(maxHeight: 200)
        }
    }

    private var isEmpty: Bool {
        selection.applicationTokens.isEmpty
            && selection.categoryTokens.isEmpty
    }

    @ViewBuilder
    private func tile<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .frame(maxWidth: .infinity)
            .frame(height: 78)
            .background(
                RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.md)
                    .fill(ZenTheme.surfaceLight.opacity(0.4))
            )
    }
}

private struct TileLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        VStack(spacing: 4) {
            configuration.icon
                .font(.system(size: 26))
            configuration.title
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(ZenTheme.text)
                .lineLimit(2)
                .multilineTextAlignment(.center)
        }
        .padding(6)
    }
}

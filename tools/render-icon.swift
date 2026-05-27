#!/usr/bin/env swift

// Renders the ZenLock app icon at any size to a PNG file using Core Graphics.
// Single source of truth for both iOS and Android icon assets.
//
// Usage: swift tools/render-icon.swift <size> <output.png> [--transparent-bg]
//
// The icon is a dark navy padlock with a stylized diagonal "Z" cutout,
// drawn on a white background (or transparent for adaptive-icon foreground).

import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
import AppKit

let args = CommandLine.arguments
guard args.count >= 3, let size = Int(args[1]) else {
    print("Usage: render-icon.swift <size> <output.png> [--transparent-bg]")
    exit(1)
}
let output = args[2]
let transparentBg = args.contains("--transparent-bg")

let s = CGFloat(size)
let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!
let ctx = CGContext(
    data: nil,
    width: size, height: size,
    bitsPerComponent: 8, bytesPerRow: 0,
    space: colorSpace,
    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
)!

// Authoring coords use Core Graphics convention: y=0 is BOTTOM.
// Padlock layout (in a 1024 canvas):
//   shackle arc centered at y=720 (toward top)
//   body rect spans y=140..620 (lower-middle)
//   Z cutout sits inside body

// Brand colors
let navy = CGColor(red: 0.137, green: 0.165, blue: 0.275, alpha: 1)        // #232A46 deep navy
let navyLight = CGColor(red: 0.235, green: 0.282, blue: 0.420, alpha: 1)   // accent
let white = CGColor(red: 1, green: 1, blue: 1, alpha: 1)
let bgColor: CGColor = transparentBg
    ? CGColor(red: 1, green: 1, blue: 1, alpha: 0)
    : white

// 1) Background fill (square — iOS will mask corners itself, Android handles adaptive)
ctx.setFillColor(bgColor)
ctx.fill(CGRect(x: 0, y: 0, width: s, height: s))

// 2) Padlock shape — coords normalized to 1024 then scaled
let scale = s / 1024.0
func P(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * scale, y: y * scale) }
func R(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
    CGRect(x: x * scale, y: y * scale, width: w * scale, height: h * scale)
}

// Body rectangle — bottom 2/3 of icon (y-up coordinates)
//   x: 232..792 (560 wide), y: 144..624 (480 tall)
let bodyRect = R(232, 144, 560, 480)
ctx.setFillColor(navy)
ctx.addPath(CGPath(roundedRect: bodyRect, cornerWidth: 80 * scale, cornerHeight: 80 * scale, transform: nil))
ctx.fillPath()

// Shackle — thick stroked half-arc that sits ABOVE the body.
// Arc center sits at the top edge of the body so the arc rises upward.
let shackleCenter = P(512, 624)
let shackleRadius = 175.0 * scale
let shackleThickness = 80.0 * scale

ctx.saveGState()
ctx.setStrokeColor(navy)
ctx.setLineWidth(shackleThickness)
ctx.setLineCap(.round)
let shacklePath = CGMutablePath()
// In y-up coords, a half-circle going "up" is start=0, end=pi, counter-clockwise=false
shacklePath.addArc(
    center: shackleCenter,
    radius: shackleRadius,
    startAngle: 0,
    endAngle: .pi,
    clockwise: false
)
ctx.addPath(shacklePath)
ctx.strokePath()
ctx.restoreGState()

// Re-fill body on top so the shackle's bottom ends are tucked behind it cleanly
ctx.setFillColor(navy)
ctx.addPath(CGPath(roundedRect: bodyRect, cornerWidth: 80 * scale, cornerHeight: 80 * scale, transform: nil))
ctx.fillPath()

// Z cutout in white inside the body.
// In y-up coords: top of Z is HIGHER y than bottom.
//   Body interior: x 232..792, y 144..624. Center the Z within ~340..680 y, ~340..680 x.
ctx.saveGState()
ctx.setStrokeColor(white)
ctx.setLineWidth(72 * scale)
ctx.setLineCap(.round)
ctx.setLineJoin(.round)

let zPath = CGMutablePath()
// Z reads top-left → top-right → bottom-left → bottom-right
// In y-up: top y = 540, bottom y = 260
zPath.move(to: P(370, 540))     // top-left
zPath.addLine(to: P(654, 540))  // top-right
zPath.addLine(to: P(370, 260))  // diagonal down to bottom-left
zPath.addLine(to: P(654, 260))  // bottom-right
ctx.addPath(zPath)
ctx.strokePath()
ctx.restoreGState()

// Subtle highlight on body top edge for polish
ctx.saveGState()
ctx.setFillColor(CGColor(red: 1, green: 1, blue: 1, alpha: 0.06))
ctx.addPath(CGPath(roundedRect: R(232, 564, 560, 60), cornerWidth: 80 * scale, cornerHeight: 80 * scale, transform: nil))
ctx.fillPath()
ctx.restoreGState()

_ = navyLight  // reserved for future shading; silence unused warning

guard let image = ctx.makeImage() else {
    print("Failed to render image")
    exit(1)
}

let url = URL(fileURLWithPath: output)
guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else {
    print("Failed to create PNG destination at \(output)")
    exit(1)
}
CGImageDestinationAddImage(dest, image, nil)
guard CGImageDestinationFinalize(dest) else {
    print("Failed to write PNG")
    exit(1)
}
print("✓ wrote \(output) (\(size)×\(size)\(transparentBg ? ", transparent bg" : ""))")

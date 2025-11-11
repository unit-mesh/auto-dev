// swift-tools-version: 5.9
// This is a REFERENCE file showing how MCP SDK would be configured in a pure Swift Package
// The actual AutoDevApp is an Xcode project and uses Xcode's Package Dependencies UI instead

import PackageDescription

let package = Package(
    name: "AutoDevApp",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "AutoDevApp",
            targets: ["AutoDevApp"]
        ),
    ],
    dependencies: [
        // MCP SDK - Official Swift implementation
        // https://github.com/modelcontextprotocol/swift-sdk
        .package(
            url: "https://github.com/modelcontextprotocol/swift-sdk.git",
            from: "0.10.0"
        )
    ],
    targets: [
        .target(
            name: "AutoDevApp",
            dependencies: [
                .product(name: "MCP", package: "swift-sdk")
            ],
            path: "AutoDevApp"
        )
    ]
)

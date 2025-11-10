# Application Icons

This directory contains the application icons for AutoDev Desktop in various formats for different platforms.

## Files

- **icon.svg** - Original SVG source (40x40px base)
- **icon.icns** - macOS application icon (multiple resolutions: 16-1024px @1x and @2x)
- **icon.ico** - Windows application icon (16, 32, 48, 64, 128, 256px)
- **icon-{size}.png** - Individual PNG exports at various sizes

## Icon Specifications

### macOS (ICNS)
- Contains: 16x16, 32x32, 64x64, 128x128, 256x256, 512x512, 1024x1024
- Each size has @1x and @2x variants
- Format: ICNS (Apple Icon Image)

### Windows (ICO)
- Contains: 16x16, 32x32, 48x48, 64x64, 128x128, 256x256
- Format: ICO (Microsoft Icon)

### Linux (PNG)
- Uses: icon-512.png
- Format: PNG with alpha channel

## Regenerating Icons

If you modify `icon.svg`, regenerate the platform-specific icons:

```bash
# From mpp-ui directory
npm run icon:convert

# Or manually:
bash scripts/convert-icon.sh           # Creates ICNS + PNGs
python3 scripts/convert-icon-windows.py # Creates ICO
```

### Requirements

- **rsvg-convert** (from librsvg): `brew install librsvg`
- **Python 3** with Pillow: `pip3 install pillow`
- **iconutil** (macOS built-in)

## Icon Design

The AutoDev icon features:
- AI copilot theme with green color (#48A178)
- "A_" symbol representing AI assistant
- 40x40px base size, scalable to any resolution
- Transparent background for macOS dock integration

## Build Configuration

Icons are configured in `build.gradle.kts`:

```kotlin
compose.desktop {
    application {
        nativeDistributions {
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon-512.png"))
            }
        }
    }
}
```

## Testing

After regenerating icons, rebuild the distribution:

```bash
./gradlew :mpp-ui:packageDistributionForCurrentOS
```

Check the app icon in Finder (macOS) or verify with:

```bash
sips -g all "build/compose/binaries/main/app/AutoDev Desktop.app/Contents/Resources/AutoDev Desktop.icns"
```

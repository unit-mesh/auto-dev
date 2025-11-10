#!/bin/bash

# Convert SVG icon to ICNS for macOS using native tools
# This script creates an iconset and converts it to ICNS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESOURCES_DIR="$SCRIPT_DIR/../src/jvmMain/resources"
SVG_FILE="$RESOURCES_DIR/icon.svg"
ICONSET_DIR="$RESOURCES_DIR/icon.iconset"

# Check if librsvg is installed
if ! command -v rsvg-convert &> /dev/null; then
    echo "❌ rsvg-convert not found. Installing librsvg..."
    echo "   Run: brew install librsvg"
    exit 1
fi

echo "🎨 Converting SVG to ICNS..."

# Create iconset directory
rm -rf "$ICONSET_DIR"
mkdir -p "$ICONSET_DIR"

# Generate PNG files at different sizes
declare -a sizes=(16 32 64 128 256 512)

for size in "${sizes[@]}"; do
    size2x=$((size * 2))

    echo "  Creating icon_${size}x${size}.png..."
    rsvg-convert -w $size -h $size "$SVG_FILE" -o "$ICONSET_DIR/icon_${size}x${size}.png"

    echo "  Creating icon_${size}x${size}@2x.png..."
    rsvg-convert -w $size2x -h $size2x "$SVG_FILE" -o "$ICONSET_DIR/icon_${size}x${size}@2x.png"
done

# Convert iconset to ICNS
echo "  Creating ICNS file..."
iconutil -c icns "$ICONSET_DIR" -o "$RESOURCES_DIR/icon.icns"

# Also create standalone PNG files for other platforms
echo "  Creating standalone PNG files..."
rsvg-convert -w 512 -h 512 "$SVG_FILE" -o "$RESOURCES_DIR/icon-512.png"
rsvg-convert -w 256 -h 256 "$SVG_FILE" -o "$RESOURCES_DIR/icon-256.png"
rsvg-convert -w 128 -h 128 "$SVG_FILE" -o "$RESOURCES_DIR/icon-128.png"

# Clean up iconset directory
rm -rf "$ICONSET_DIR"

echo "✅ Icon conversion complete!"
echo "   ICNS: $RESOURCES_DIR/icon.icns"
echo "   PNG:  $RESOURCES_DIR/icon-*.png"

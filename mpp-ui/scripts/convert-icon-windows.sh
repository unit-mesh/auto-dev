#!/bin/bash

# Convert PNG to ICO for Windows using ImageMagick
# Falls back to online conversion if ImageMagick is not available

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESOURCES_DIR="$SCRIPT_DIR/../src/jvmMain/resources"
PNG_FILE="$RESOURCES_DIR/icon-256.png"
ICO_FILE="$RESOURCES_DIR/icon.ico"

echo "🪟 Creating Windows ICO file..."

# Check if ImageMagick is installed
if command -v magick &> /dev/null || command -v convert &> /dev/null; then
    # ImageMagick is available
    if command -v magick &> /dev/null; then
        CONVERT_CMD="magick"
    else
        CONVERT_CMD="convert"
    fi
    
    echo "  Using ImageMagick..."
    # Create ICO with multiple sizes (16, 32, 48, 64, 128, 256)
    $CONVERT_CMD "$RESOURCES_DIR/icon-16.png" \
                 "$RESOURCES_DIR/icon-32.png" \
                 "$RESOURCES_DIR/icon-48.png" \
                 "$RESOURCES_DIR/icon-64.png" \
                 "$RESOURCES_DIR/icon-128.png" \
                 "$RESOURCES_DIR/icon-256.png" \
                 "$ICO_FILE" 2>/dev/null || {
        # If specific sizes don't exist, use single size
        echo "  Creating ICO from 256px PNG..."
        $CONVERT_CMD "$PNG_FILE" -define icon:auto-resize=256,128,64,48,32,16 "$ICO_FILE"
    }
    
    echo "✅ ICO file created: $ICO_FILE"
else
    echo "⚠️  ImageMagick not found."
    echo ""
    echo "To create ICO file, either:"
    echo "  1. Install ImageMagick: brew install imagemagick"
    echo "  2. Use online converter:"
    echo "     - Upload: $PNG_FILE"
    echo "     - Convert to ICO at: https://convertico.com/"
    echo "     - Save as: $ICO_FILE"
    echo ""
    echo "For now, copying PNG as fallback..."
    cp "$PNG_FILE" "$ICO_FILE.png"
fi

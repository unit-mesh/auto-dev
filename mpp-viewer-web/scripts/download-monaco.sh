#!/bin/bash

# Script to download Monaco Editor for offline usage
# This downloads the Monaco Editor CDN files to local directory

set -e

MONACO_VERSION="0.52.0"
RESOURCES_DIR="$(cd "$(dirname "$0")/../src/commonMain/resources" && pwd)"
MONACO_DIR="${RESOURCES_DIR}/monaco-editor"

echo "Downloading Monaco Editor v${MONACO_VERSION}..."

# Create directory
mkdir -p "${MONACO_DIR}"

# Download Monaco Editor from NPM
if command -v npm &> /dev/null; then
    echo "Using npm to download monaco-editor..."
    
    # Create temp directory
    TEMP_DIR=$(mktemp -d)
    cd "${TEMP_DIR}"
    
    # Initialize package.json and install monaco-editor
    npm init -y > /dev/null 2>&1
    npm install monaco-editor@${MONACO_VERSION} --no-save > /dev/null 2>&1
    
    # Copy files to resources
    echo "Copying Monaco Editor files..."
    cp -r node_modules/monaco-editor/min/* "${MONACO_DIR}/"
    
    # Cleanup
    cd -
    rm -rf "${TEMP_DIR}"
    
    echo "✅ Monaco Editor downloaded successfully to ${MONACO_DIR}"
    echo ""
    echo "To use the offline version, update viewer.html to use local paths:"
    echo "  paths: { 'vs': './monaco-editor/vs' }"
else
    echo "❌ npm is not installed. Please install Node.js and npm first."
    echo ""
    echo "Alternative: Download Monaco Editor manually from:"
    echo "  https://github.com/microsoft/monaco-editor/releases"
    echo "  Extract to: ${MONACO_DIR}"
    exit 1
fi


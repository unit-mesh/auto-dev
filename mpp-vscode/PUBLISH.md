# AutoDev VSCode Extension - Publishing Guide

## 📦 Package Information

- **Extension ID**: `Phodal.autodev`
- **Version**: 0.5.3
- **Package File**: `autodev-0.5.3.vsix` (3.11 MB)
- **Dependencies**: Uses published npm packages
  - `@autodev/mpp-core@0.3.4`
  - `@autodev/cli@0.3.4`

## ✅ Pre-publish Checklist

All steps completed:
- [x] Version updated in `gradle.properties` (0.3.4)
- [x] mpp-core built and published to npm
- [x] mpp-ui (CLI) built and published to npm
- [x] mpp-vscode updated to use published mpp-core
- [x] Extension built successfully
- [x] Extension packaged as VSIX (3.11 MB)
- [x] Changes committed and pushed to GitHub

## 🚀 Publishing to VS Code Marketplace

### Option 1: Using vsce CLI (Recommended)

1. **Get Personal Access Token (PAT)**
   - Go to https://dev.azure.com/
   - Create a new PAT with `Marketplace (Publish)` scope
   - Save the token securely

2. **Publish**
   ```bash
   cd /Volumes/source/ai/autocrud/mpp-vscode
   vsce publish
   # When prompted, enter your PAT
   ```

3. **Or publish with explicit PAT**
   ```bash
   vsce publish -p <YOUR_PAT>
   ```

### Option 2: Manual Upload via Web

1. Go to https://marketplace.visualstudio.com/manage/publishers/Phodal
2. Click "New Extension"
3. Upload `autodev-0.5.3.vsix`
4. Fill in the marketplace details
5. Publish

## 📝 First-Time Publisher Setup

If this is your first time publishing as "Phodal":

1. **Create Publisher**
   ```bash
   vsce create-publisher Phodal
   ```

2. **Login**
   ```bash
   vsce login Phodal
   # Enter your PAT when prompted
   ```

## 🔍 Verify Installation

After publishing, test the extension:

```bash
# Install from marketplace
code --install-extension Phodal.autodev

# Or install from local VSIX (for testing)
code --install-extension autodev-0.5.3.vsix
```

## 📊 What's Included in the Package

The VSIX contains:
- Extension code (13.6 MB - bundled with esbuild)
- Webview assets (374 KB - React UI)
- Tree-sitter WASM files (10.74 MB - 8 languages)
- Media assets (icons, fonts)
- DevIns language syntax
- Test mocks

**Total package size**: 3.11 MB (compressed)

## 🔄 Updating the Extension

For future updates:

1. Update version in `gradle.properties`
2. Build and publish mpp-core if needed
3. Update mpp-vscode/package.json dependency versions
4. Run `npm run build`
5. Run `vsce package`
6. Run `vsce publish` or upload manually

## 📖 Marketplace Listing

**Display Name**: AutoDev - 🧙the AI-powered coding wizard (KMP Edition).

**Description**: 
> 🧙‍ AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, based on Kotlin Multiplatform. AutoDev provides CodeLens, Chat, and powerful agent features! 🚀

**Categories**:
- Programming Languages
- Education
- Machine Learning
- Snippets

**Keywords**: ai, coding assistant, llm, autodev, kotlin multiplatform, kmp

## 🔗 Links

- Homepage: https://vscode.unitmesh.cc
- Repository: https://github.com/unit-mesh/auto-dev-vscode
- Issues: https://github.com/unit-mesh/auto-dev-vscode/issues
- Marketplace: https://marketplace.visualstudio.com/items?itemName=Phodal.autodev

## ⚠️ Important Notes

1. **Same Publisher ID**: This extension uses `Phodal` as publisher, matching the existing AutoDev extension
2. **Version Coordination**: mpp-core (0.3.4) and CLI (0.3.4) are published separately
3. **File Size**: Extension is 3.11 MB compressed (includes WASM files for Tree-sitter)
4. **Minimum VSCode**: Requires VSCode 1.85.0 or higher

## 🎯 Next Steps

1. Execute: `vsce publish` in the mpp-vscode directory
2. Wait for marketplace processing (usually 5-10 minutes)
3. Verify extension appears in marketplace
4. Test installation and functionality
5. Announce the release!

---

**Ready to publish!** 🚀


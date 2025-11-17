# mpp-viewer

Universal viewer module for AutoDev - provides a platform-agnostic API for displaying various types of content (code, markdown, images, etc.).

## Architecture

This module is split into two parts:

1. **mpp-viewer** (this module): Core API and interfaces
   - `ViewerType`: Enumeration of supported content types
   - `ViewerRequest`: Data class for content display requests
   - `ViewerHost`: Interface for viewer implementations
   - `LanguageDetector`: Utility for detecting programming languages

2. **mpp-viewer-web** (separate module): WebView-based implementation
   - WebView host using `compose-webview-multiplatform`
   - Monaco Editor integration for code viewing
   - Offline-capable viewer HTML

## Usage

### 1. Create a Viewer Request

```kotlin
val request = ViewerRequest(
    type = ViewerType.CODE,
    content = fileContent,
    language = LanguageDetector.detectLanguage(filePath),
    fileName = fileName,
    filePath = filePath,
    readOnly = true
)
```

### 2. Display Content

```kotlin
val viewerHost: ViewerHost = // ... get platform-specific implementation
viewerHost.showContent(request)
```

## Supported Content Types

- **CODE**: Syntax-highlighted code with Monaco Editor
- **MARKDOWN**: Rendered markdown preview
- **IMAGE**: Image display
- **PDF**: PDF document viewer
- **VIDEO**: Video player
- **AUDIO**: Audio player
- **UNSUPPORTED**: Fallback for unknown types

## Dependencies

This module only requires `kotlinx-serialization-json` for data serialization. The actual viewer implementation (WebView, Monaco Editor) is provided by `mpp-viewer-web`.


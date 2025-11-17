# mpp-viewer-web

WebView-based implementation of the viewer API for AutoDev, using `compose-webview-multiplatform` and Monaco Editor.

## Features

- Cross-platform WebView support (JVM, Android, iOS)
- Monaco Editor integration for code syntax highlighting
- Offline-capable viewer HTML
- Support for multiple content types (code, markdown, images, etc.)

## Architecture

- `WebViewerHost`: Implementation of `ViewerHost` that communicates with WebView via JavaScript
- `ViewerWebView`: Composable WebView component for displaying content
- `viewer.html`: Embedded HTML with Monaco Editor for offline viewing

## Usage

```kotlin
@Composable
fun MyFileViewer(filePath: String) {
    val webViewState = rememberWebViewState(url = "")
    val viewerHost = remember { WebViewerHost(webViewState) }
    
    ViewerWebView(
        viewerHost = viewerHost,
        onReady = {
            // WebView is ready, show content
            val request = ViewerRequest(
                type = ViewerType.CODE,
                content = fileContent,
                language = LanguageDetector.detectLanguage(filePath)
            )
            viewerHost.showContent(request)
        }
    )
}
```

## Dependencies

- `mpp-viewer`: Core viewer API
- `compose-webview-multiplatform`: Cross-platform WebView component
- `kotlinx-serialization-json`: JSON serialization for viewer requests


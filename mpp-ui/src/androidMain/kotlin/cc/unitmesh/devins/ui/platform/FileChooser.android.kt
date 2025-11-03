/*
 * Android 平台文件选择器实现
 *
 * 使用方法：
 * 1. 在 MainActivity.onCreate() 中注册 Activity：
 *    AndroidActivityProvider.setActivity(this)
 *
 * 2. 然后就可以在任何地方使用文件选择器了：
 *    val fileChooser = createFileChooser()
 *    val filePath = fileChooser.chooseFile(
 *        title = "选择文件",
 *        fileExtensions = listOf("txt", "md", "devin")
 *    )
 *
 * 3. 或者选择目录：
 *    val dirPath = fileChooser.chooseDirectory(title = "选择目录")
 *
 * 注意事项：
 * - Android 使用 Storage Access Framework (SAF)，返回的是 content:// URI
 * - ActivityResultLauncher 必须在 Activity 创建时注册
 * - 文件路径是 content:// 格式，需要使用 ContentResolver 访问文件内容
 */
package cc.unitmesh.devins.ui.platform

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Android 平台的文件选择器实现
 *
 * 使用 Android Storage Access Framework (SAF) 来选择文件和目录
 *
 * 注意：这个实现需要在 Activity 创建时初始化，因为 ActivityResultLauncher
 * 必须在 Activity onCreate() 之前注册
 */
class AndroidFileChooser(private val activity: ComponentActivity) : FileChooser {
    private var fileContinuation: Continuation<String?>? = null
    private var directoryContinuation: Continuation<String?>? = null

    // 文件选择器启动器
    private val filePickerLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val path = uri?.let { getPathFromUri(it) }
            fileContinuation?.resume(path)
            fileContinuation = null
        }

    // 目录选择器启动器
    private val directoryPickerLauncher: ActivityResultLauncher<Uri?> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            val path = uri?.let { getPathFromUri(it) }
            directoryContinuation?.resume(path)
            directoryContinuation = null
        }

    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? =
        suspendCancellableCoroutine { continuation ->
            fileContinuation = continuation

            // 构建 MIME 类型数组
            val mimeTypes =
                if (fileExtensions.isNullOrEmpty()) {
                    arrayOf("*/*")
                } else {
                    fileExtensions.map { ext ->
                        getMimeTypeForExtension(ext)
                    }.toTypedArray()
                }

            continuation.invokeOnCancellation {
                fileContinuation = null
            }

            try {
                filePickerLauncher.launch(mimeTypes)
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }

    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? =
        suspendCancellableCoroutine { continuation ->
            directoryContinuation = continuation

            continuation.invokeOnCancellation {
                directoryContinuation = null
            }

            try {
                // 可选：设置初始 URI
                val initialUri = initialDirectory?.toUri()
                directoryPickerLauncher.launch(initialUri)
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }

    /**
     * 从 URI 获取文件路径
     * 在 Android 上，通常返回 content:// URI 字符串
     */
    private fun getPathFromUri(uri: Uri): String {
        // 对于 SAF，我们直接返回 URI 字符串
        // 因为实际的文件路径可能无法直接访问（沙盒限制）
        return uri.toString()
    }

    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private fun getMimeTypeForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "devin", "md", "txt" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "html", "htm" -> "text/html"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }
}

/**
 * Android 上的 Activity 提供者
 * 需要在应用启动时设置 Activity 实例并初始化 FileChooser
 */
object AndroidActivityProvider {
    private var activity: ComponentActivity? = null
    private var fileChooser: AndroidFileChooser? = null

    fun setActivity(activity: ComponentActivity) {
        this.activity = activity
        // 在 Activity 创建时立即初始化 FileChooser，此时可以安全地注册 ActivityResultLauncher
        fileChooser = AndroidFileChooser(activity)
    }

    fun getActivity(): ComponentActivity? = activity

    fun getFileChooser(): AndroidFileChooser? = fileChooser
}

/**
 * 创建 Android 文件选择器
 * 注意：需要先通过 AndroidActivityProvider.setActivity() 设置 Activity 实例
 * 通常在 MainActivity.onCreate() 中调用
 *
 * 这个函数会返回在 Activity 创建时已经初始化好的 FileChooser 实例，
 * 避免在运行时重复创建和注册 ActivityResultLauncher
 */
actual fun createFileChooser(): FileChooser {
    val fileChooser = AndroidActivityProvider.getFileChooser()
    return fileChooser ?: run {
        // 如果没有设置 Activity，返回一个占位实现
        // 并在调用时打印警告
        object : FileChooser {
            override suspend fun chooseFile(
                title: String,
                initialDirectory: String?,
                fileExtensions: List<String>?
            ): String? {
                println("⚠️ FileChooser on Android requires Activity context.")
                println("   Please call AndroidActivityProvider.setActivity() in your MainActivity.onCreate()")
                return null
            }

            override suspend fun chooseDirectory(
                title: String,
                initialDirectory: String?
            ): String? {
                println("⚠️ FileChooser on Android requires Activity context.")
                println("   Please call AndroidActivityProvider.setActivity() in your MainActivity.onCreate()")
                return null
            }
        }
    }
}

/**
 * 创建 Android 文件选择器的辅助函数
 * @param activity 当前的 ComponentActivity 实例
 */
@Suppress("unused")
fun createFileChooser(activity: ComponentActivity): FileChooser = AndroidFileChooser(activity)

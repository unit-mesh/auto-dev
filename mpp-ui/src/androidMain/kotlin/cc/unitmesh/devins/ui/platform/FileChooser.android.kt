package cc.unitmesh.devins.ui.platform

import androidx.activity.ComponentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android 平台的文件选择器实现
 *
 * 注意：当前为占位实现，返回 null 以避免崩溃
 *
 * 在 Android 上使用文件选择器的正确方式是在 Compose 代码中使用 rememberLauncherForActivityResult
 *
 * 这是因为 Android 的 Activity Result API 需要在 Activity 创建时注册启动器，
 * 不能在运行时动态注册（会导致崩溃）。
 */
@Suppress("UNUSED_PARAMETER")
class AndroidFileChooser(private val activity: ComponentActivity) : FileChooser {

    override suspend fun chooseFile(
        title: String,
        initialDirectory: String?,
        fileExtensions: List<String>?
    ): String? = suspendCancellableCoroutine { continuation ->
        // 占位实现 - 实际使用时应该在 Compose UI 中使用 rememberLauncherForActivityResult
        // 这里返回 null 以避免崩溃
        continuation.resume(null)
    }
    
    override suspend fun chooseDirectory(
        title: String,
        initialDirectory: String?
    ): String? = suspendCancellableCoroutine { continuation ->
        // 占位实现 - 实际使用时应该在 Compose UI 中使用 rememberLauncherForActivityResult
        // 这里返回 null 以避免崩溃
        continuation.resume(null)
    }
}

/**
 * 创建 Android 文件选择器
 * 注意：返回占位实现，功能受限
 * 在实际项目中建议在 Compose UI 中使用 rememberLauncherForActivityResult
 */
actual fun createFileChooser(): FileChooser {
    // 返回一个占位实例，避免崩溃
    // 由于无法在这里获取 Activity 实例，返回的实例会在调用时返回 null
    return object : FileChooser {
        override suspend fun chooseFile(
            title: String,
            initialDirectory: String?,
            fileExtensions: List<String>?
        ): String? {
            // 占位实现 - 在 Android 上需要 Activity 上下文
            // 实际使用时应该在 Compose UI 中使用 rememberLauncherForActivityResult
            println("⚠️ FileChooser on Android requires Activity context. Returning null.")
            return null
        }

        override suspend fun chooseDirectory(
            title: String,
            initialDirectory: String?
        ): String? {
            // 占位实现 - 在 Android 上需要 Activity 上下文
            println("⚠️ FileChooser on Android requires Activity context. Returning null.")
            return null
        }
    }
}

/**
 * 创建 Android 文件选择器的辅助函数
 * @param activity 当前的 ComponentActivity 实例
 */
@Suppress("unused")
fun createFileChooser(activity: ComponentActivity): FileChooser = AndroidFileChooser(activity)


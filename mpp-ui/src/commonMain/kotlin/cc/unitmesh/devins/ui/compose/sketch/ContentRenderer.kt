package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 内容渲染器统一接口
 * 
 * 所有渲染器（TextBlockRenderer, CodeBlockRenderer, DiffSketchRenderer 等）
 * 都应该实现这个接口，以便：
 * 1. 统一管理渲染逻辑
 * 2. 支持渲染进度回调
 * 3. 支持流式渲染状态
 */
interface ContentRenderer {
    /**
     * 渲染内容
     * 
     * @param content 要渲染的内容
     * @param isComplete 内容是否完整（流式输出时为 false）
     * @param onRenderUpdate 渲染更新回调，传递渲染元数据
     * @param modifier Compose Modifier
     */
    @Composable
    fun Render(
        content: String,
        isComplete: Boolean = true,
        onRenderUpdate: ((RenderMetadata) -> Unit)? = null,
        modifier: Modifier = Modifier
    )
}

/**
 * 渲染元数据
 * 
 * 用于传递渲染器的状态信息给外层组件
 */
data class RenderMetadata(
    /**
     * 渲染的块/元素数量
     * 例如：CodeFence 解析出的块数量
     */
    val blockCount: Int = 0,
    
    /**
     * 估计的内容高度（可选）
     * 单位：像素，用于精确滚动控制
     */
    val estimatedHeight: Int? = null,
    
    /**
     * 最后一个块的类型（可选）
     * 例如："markdown", "code", "diff"
     */
    val lastBlockType: String? = null,
    
    /**
     * 是否包含可展开内容（可选）
     * 例如：长代码块、大型 diff
     */
    val hasExpandableContent: Boolean = false,
    
    /**
     * 自定义元数据（可选）
     * 用于特定渲染器的扩展信息
     */
    val customData: Map<String, Any>? = null
)

/**
 * 简单的渲染器基类
 * 提供默认实现，子类可以选择性覆盖
 */
abstract class BaseContentRenderer : ContentRenderer {
    /**
     * 通知渲染更新的辅助方法
     */
    protected fun notifyRenderUpdate(
        onRenderUpdate: ((RenderMetadata) -> Unit)?,
        blockCount: Int = 0,
        lastBlockType: String? = null
    ) {
        onRenderUpdate?.invoke(
            RenderMetadata(
                blockCount = blockCount,
                lastBlockType = lastBlockType
            )
        )
    }
}


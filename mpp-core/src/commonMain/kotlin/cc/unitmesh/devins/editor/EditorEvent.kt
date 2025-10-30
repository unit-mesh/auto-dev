package cc.unitmesh.devins.editor

/**
 * 编辑器事件
 * 
 * 表示编辑器中发生的各种事件
 */
sealed class EditorEvent {
    /**
     * 提交事件（例如用户按下回车或点击提交按钮）
     */
    data class Submit(val text: String) : EditorEvent()
    
    /**
     * 文本变化事件
     */
    data class TextChanged(val text: String) : EditorEvent()
    
    /**
     * 光标移动事件
     */
    data class CursorMoved(val position: Int) : EditorEvent()
    
    /**
     * 选中变化事件
     */
    data class SelectionChanged(val start: Int, val end: Int) : EditorEvent()
    
    /**
     * 焦点获得事件
     */
    data object FocusGained : EditorEvent()
    
    /**
     * 焦点失去事件
     */
    data object FocusLost : EditorEvent()
}


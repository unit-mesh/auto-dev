package cc.unitmesh.devins.editor

/**
 * 编辑器回调接口
 * 
 * 定义了编辑器的各种回调方法，用于响应编辑器事件
 * 所有方法都有默认空实现，子类只需要重写感兴趣的方法
 */
interface EditorCallbacks {
    /**
     * 当用户提交内容时调用（例如按下 Cmd+Enter）
     */
    fun onSubmit(text: String) {}
    
    /**
     * 当文本内容变化时调用
     */
    fun onTextChanged(text: String) {}
    
    /**
     * 当光标位置移动时调用
     */
    fun onCursorMoved(position: Int) {}
    
    /**
     * 当选中区域变化时调用
     */
    fun onSelectionChanged(start: Int, end: Int) {}
    
    /**
     * 当编辑器获得焦点时调用
     */
    fun onFocusGained() {}
    
    /**
     * 当编辑器失去焦点时调用
     */
    fun onFocusLost() {}
}


# 复选框延时刷新问题修复

## 问题描述

在 `SimplifiedLLMSettingComponent` 中，当用户点击 "Use default model for all categories" 复选框时，会出现延时刷新的问题。

## 原因分析

### 原始实现问题
```kotlin
private fun updateCategoryDropdownsVisibility() {
    // 这会重新构建整个面板，导致延时
    applySettings(settings)
}
```

**问题根源**：
1. 每次复选框状态改变时，都会调用 `applySettings(settings)`
2. `applySettings` 方法会重新构建整个UI面板
3. 重建过程包括：
   - 清空面板 (`panel.removeAll()`)
   - 重新加载所有下拉框数据 (`updateAllDropdowns()`)
   - 重新加载表格数据 (`updateLLMTable()`)
   - 重新构建整个FormBuilder结构
   - 调用 `panel.invalidate()` 和 `panel.repaint()`

这个过程非常耗时，特别是当需要从GitHub Copilot加载模型数据时。

## 解决方案

### 优化后的实现

#### 1. 添加独立的分类面板
```kotlin
// Category section panel for dynamic visibility
private var categoryPanel: JPanel? = null
```

#### 2. 创建独立的分类面板方法
```kotlin
private fun createCategoryPanel(): JPanel {
    val categoryFormBuilder = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Category-Specific Models"), JPanel(), 1, false)
        .addLabeledComponent(JBLabel("Plan:"), planLLMDropdown, 1, false)
        .addLabeledComponent(JBLabel("Act:"), actLLMDropdown, 1, false)
        .addLabeledComponent(JBLabel("Completion:"), completionLLMDropdown, 1, false)
        .addLabeledComponent(JBLabel("Embedding:"), embeddingLLMDropdown, 1, false)
        .addLabeledComponent(JBLabel("FastApply:"), fastApplyLLMDropdown, 1, false)
        .addSeparator()
    
    return categoryFormBuilder.panel
}
```

#### 3. 优化可见性控制
```kotlin
private fun updateCategoryDropdownsVisibility() {
    // 只切换面板可见性，不重建UI
    categoryPanel?.isVisible = !useDefaultForAllCheckbox.isSelected
    
    // 轻量级的布局更新
    panel.revalidate()
    panel.repaint()
}
```

#### 4. 修改主面板构建逻辑
```kotlin
// 在applySettings中
// Create category panel separately for dynamic visibility
categoryPanel = createCategoryPanel()

formBuilder
    // ... 其他组件 ...
    
// Add category panel (visibility controlled dynamically)
formBuilder.addComponent(categoryPanel!!)

// Set initial visibility
updateCategoryDropdownsVisibility()
```

## 性能对比

### 原始方案
- ❌ 重建整个UI面板
- ❌ 重新加载所有数据
- ❌ 重新创建所有组件
- ❌ 延时明显（特别是有网络请求时）

### 优化方案
- ✅ 只切换面板可见性
- ✅ 保持现有数据和组件
- ✅ 轻量级布局更新
- ✅ 即时响应，无延时

## 技术要点

### 1. 组件复用
- 分类下拉框组件只创建一次
- 通过可见性控制显示/隐藏
- 避免重复创建和销毁组件

### 2. 布局更新
- `panel.revalidate()`: 重新计算布局
- `panel.repaint()`: 重绘界面
- 这两个操作比重建整个面板快得多

### 3. 状态保持
- 用户在分类下拉框中的选择得以保持
- 不会因为UI重建而丢失状态

## 适用场景

这种优化方案适用于：
- 需要动态显示/隐藏UI组件的场景
- 包含大量数据加载的复杂面板
- 对用户交互响应速度有要求的界面

## 注意事项

1. **初始化顺序**：确保在设置事件监听器之前创建好所有组件
2. **空指针检查**：使用 `categoryPanel?.isVisible` 进行安全调用
3. **布局更新**：必须调用 `revalidate()` 和 `repaint()` 来更新布局

这个修复彻底解决了复选框点击时的延时问题，提供了流畅的用户体验。

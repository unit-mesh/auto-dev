# LLM模型参数重新设计

## 设计目标

根据您的建议，重新设计LLM模型的参数结构，简化配置并提高灵活性：

### 新的参数结构
- **url**: API端点URL
- **token**: API密钥（可选，因为GitHub Copilot是动态刷新）
- **maxTokens**: 最大Token数量
- **customRequest**: 自定义请求配置（包含headers和body）
- **移除responseResolver**: 根据customRequest的stream参数自动决定

## 实现方案

### 1. 新的数据模型

#### CustomRequest类
```kotlin
@Serializable
data class CustomRequest(
    val headers: Map<String, String> = emptyMap(), // 自定义请求头
    val body: Map<String, @Contextual Any> = emptyMap(), // 自定义请求体
    val stream: Boolean = true // 是否使用流式响应
)
```

#### 更新的LlmConfig类
```kotlin
@Serializable
data class LlmConfig(
    val name: String,
    val description: String = "",
    val url: String,
    val auth: Auth = Auth("Bearer"), // Token可选
    val maxTokens: Int = 4096, // 最大Token数
    val customRequest: CustomRequest = CustomRequest(), // 自定义请求配置
    val modelType: ModelType = ModelType.Default,
    
    // 保留旧字段用于向后兼容
    @Deprecated("Use customRequest instead")
    val requestFormat: String = "",
    @Deprecated("Use customRequest.stream to determine response format")
    val responseFormat: String = ""
)
```

### 2. 智能响应格式处理

#### 自动响应格式决定
```kotlin
fun getResponseFormatByStream(): String {
    return if (customRequest.stream) {
        "\$.choices[0].delta.content" // 流式格式
    } else {
        "\$.choices[0].message.content" // 非流式格式
    }
}
```

#### 向后兼容转换
```kotlin
fun toLegacyRequestFormat(): String {
    return buildJsonObject {
        if (customRequest.headers.isNotEmpty()) {
            put("customHeaders", buildJsonObject {
                customRequest.headers.forEach { (key, value) ->
                    put(key, value)
                }
            })
        }
        put("customFields", buildJsonObject {
            customRequest.body.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, value)
                    else -> put(key, value.toString())
                }
            }
        })
    }.toString()
}
```

### 3. 增强的UI界面

#### 新的配置对话框字段
- **Name**: 模型名称
- **Description**: 模型描述
- **URL**: API端点
- **Token (optional)**: API密钥（标注为可选）
- **Max Tokens**: 最大Token数量
- **Use streaming response**: 流式响应复选框
- **Custom Headers (JSON)**: 自定义请求头（JSON格式）
- **Request Body (JSON)**: 请求体配置（JSON格式）

#### 预设模板支持
为常用服务商提供预设配置：
- **OpenAI**: 标准OpenAI API配置
- **Azure OpenAI**: Azure特定配置
- **Anthropic Claude**: Claude API配置
- **Google Gemini**: Gemini API配置
- **Ollama (Local)**: 本地Ollama配置

### 4. 向后兼容性

#### 自动迁移
```kotlin
companion object {
    fun fromLegacyFormat(requestFormat: String): CustomRequest {
        // 解析旧的requestFormat JSON
        // 转换为新的CustomRequest结构
        // 自动检测stream参数
    }
}
```

#### 废弃字段标记
- 使用`@Deprecated`注解标记旧字段
- 保留字段以确保现有配置不会丢失
- 提供迁移提示和工具

## 主要优势

### 1. 简化配置
- **统一参数**: 所有配置集中在CustomRequest中
- **智能默认**: 根据stream参数自动决定响应格式
- **可选Token**: 支持GitHub Copilot等动态刷新场景

### 2. 增强灵活性
- **自定义Headers**: 支持任意HTTP头部
- **灵活Body**: 支持复杂的请求体结构
- **流式控制**: 简单的布尔值控制流式/非流式

### 3. 更好的用户体验
- **可视化配置**: JSON编辑器替代复杂的文本框
- **实时验证**: 配置时即时验证JSON格式
- **预设模板**: 快速配置常用服务商

### 4. 技术优势
- **类型安全**: 使用Kotlin数据类确保类型安全
- **序列化支持**: 完整的JSON序列化/反序列化
- **向后兼容**: 平滑迁移路径

## 使用示例

### OpenAI配置
```json
{
  "headers": {},
  "body": {
    "model": "gpt-3.5-turbo",
    "temperature": 0.0,
    "max_tokens": 4096
  },
  "stream": true
}
```

### 带自定义Headers的配置
```json
{
  "headers": {
    "X-Custom-Header": "value",
    "Authorization-Extra": "extra-token"
  },
  "body": {
    "model": "custom-model",
    "temperature": 0.7,
    "top_p": 0.9
  },
  "stream": false
}
```

### Ollama本地配置
```json
{
  "headers": {},
  "body": {
    "model": "llama2",
    "temperature": 0.0,
    "stream": true
  },
  "stream": true
}
```

## 迁移指南

### 自动迁移
1. 检测旧的requestFormat配置
2. 解析JSON结构
3. 提取headers和body
4. 检测stream参数
5. 创建新的CustomRequest对象

### 手动迁移
1. 打开LLM配置对话框
2. 查看自动填充的配置
3. 根据需要调整参数
4. 保存新配置

## 编译状态

✅ **编译成功**: 所有新代码已通过编译验证
⚠️ **废弃警告**: 预期的向后兼容警告
🔄 **功能完整**: 支持完整的CRUD操作和测试

这次重新设计彻底简化了LLM模型的参数结构，提供了更好的灵活性和用户体验，同时保持了完整的向后兼容性。

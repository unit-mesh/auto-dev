package cc.unitmesh.indexer.template

/**
 * Simple template engine for variable substitution.
 * Supports basic variable replacement using $variable or ${variable} syntax.
 */
class TemplateEngine {
    
    /**
     * Render a template with the given context variables
     */
    fun render(template: String, context: Map<String, Any>): String {
        var result = template
        
        // Replace ${variable} patterns first (more specific)
        val bracePattern = Regex("""\$\{([^}]+)\}""")
        result = bracePattern.replace(result) { matchResult ->
            val variableName = matchResult.groupValues[1]
            resolveVariable(variableName, context)?.toString() ?: matchResult.value
        }
        
        // Replace $variable patterns (less specific)
        val dollarPattern = Regex("""\$([a-zA-Z_][a-zA-Z0-9_.]*)""")
        result = dollarPattern.replace(result) { matchResult ->
            val variableName = matchResult.groupValues[1]
            resolveVariable(variableName, context)?.toString() ?: matchResult.value
        }
        
        return result
    }
    
    /**
     * Resolve a variable from the context, supporting nested properties
     */
    private fun resolveVariable(variableName: String, context: Map<String, Any>): Any? {
        val parts = variableName.split('.')
        var current: Any? = context
        
        for (part in parts) {
            current = when (current) {
                is Map<*, *> -> current[part]
                is TemplateContext -> current.getProperty(part)
                else -> null
            }
            if (current == null) break
        }
        
        return current
    }
}

/**
 * Interface for template context objects that support property access
 */
interface TemplateContext {
    fun getProperty(name: String): Any?
}

/**
 * Template context for domain dictionary generation
 */
data class DomainDictTemplateContext(
    val code: String,
    val readme: String
) : TemplateContext {
    
    override fun getProperty(name: String): Any? {
        return when (name) {
            "code" -> code
            "readme" -> readme
            else -> null
        }
    }
}

/**
 * Template manager for loading and caching templates
 */
class TemplateManager {
    private val engine = TemplateEngine()
    private val templateCache = mutableMapOf<String, String>()
    
    /**
     * Get template content by name
     */
    fun getTemplate(templateName: String): String {
        return templateCache[templateName] ?: getDefaultTemplate(templateName)
    }
    
    /**
     * Render a template with context
     */
    fun renderTemplate(templateName: String, context: Map<String, Any>): String {
        val template = getTemplate(templateName)
        return engine.render(template, context)
    }
    
    /**
     * Render a template with a context object
     */
    fun renderTemplate(templateName: String, context: TemplateContext): String {
        val template = getTemplate(templateName)
        val contextMap = mapOf("context" to context)
        return engine.render(template, contextMap)
    }
    
    /**
     * Cache a template
     */
    fun cacheTemplate(templateName: String, content: String) {
        templateCache[templateName] = content
    }
    
    /**
     * Get default template content
     */
    private fun getDefaultTemplate(templateName: String): String {
        return when (templateName) {
            "indexer.vm" -> getIndexerTemplate()
            "indexer_en.vm" -> getIndexerEnglishTemplate()
            else -> throw TemplateNotFoundException("Template not found: $templateName")
        }
    }
    
    private fun getIndexerTemplate(): String {
        return """
你是一个 DDD（领域驱动设计）专家，正在从代码库中构建一个业务上的中英字典作为索引。你需要从给定的代码片段中提取出重要的概念，以便于其它人理解和使用。

**提取原则：**

✅ 应该提取的内容：
- 核心业务实体（如：博客、评论、支付、用户等名词）
- 业务概念和领域模型（如：会员、积分、订单等）
- 无法理解的单词或拼音缩写
- 业务中的特定术语

❌ 应该排除的内容：
1. 技术词汇：Controller、Service、Repository、Mapper、DTO、VO、PO、Entity、Request、Response、Config、Filter、Interceptor、Exception、Helper、Utils、Util 等
2. 实现细节和数据传输对象：包含 "Request"、"Response"、"Dto"、"Entity" 等后缀的条目
3. 技术操作动词：validate、check、convert、deserialize、serialize、encode、decode 等
4. 方法名中的技术操作：比如 "checkIfVipAccount" 应只提取 "VIP账户"，"isLimitExceeded" 应只提取 "限制"
5. 常用的库 API（如 Spring、OkHttp、Retrofit 等）和类名（如 List、Map 等）

**处理规则：**
1. 若提取的条目中包含技术后缀（如 "CreateCommentDto"），应该转换为纯业务概念（如 "评论" 而不是 "创建评论数据传输对象"）
2. 若方法名包含技术操作（如 "checkIfVipAccount"），应该提取业务含义（"VIP账户" 而不是 "检查是否为VIP账户"）
3. 若类名包含技术词汇后缀，应该移除后缀后再添加到字典中

项目的 README 文件信息如下：

${'$'}context.readme

**输出格式要求：**

✅ 必须返回 CSV 格式（逗号分隔值）
✅ CSV 头部：中文,代码翻译,描述
✅ 每行一个概念，格式：[中文],[代码翻译],[描述]
✅ 只返回数据，不包含任何其他文字、说明、表格或 markdown 格式
✅ 如果数据包含逗号，请用双引号包围该字段，例如："概念A,概念B",CodeConcept,Description

例子：
```
中文,代码翻译,描述
博客,Blog,核心业务实体，代表一篇博客文章
评论,Comment,核心业务实体，代表博客下的评论
支付,Payment,核心业务实体，代表支付交易
```

请根据以下文件名和代码片段，提取出重要的业务概念，并按照上述 CSV 格式返回：

${'$'}context.code
        """.trimIndent()
    }
    
    private fun getIndexerEnglishTemplate(): String {
        return """
You are a DDD (Domain-Driven Design) expert building a business-oriented English-Chinese dictionary index from a codebase. You need to extract important concepts from the given code snippets to help others understand and use them.

**Extraction Principles:**

✅ Content that should be extracted:
- Core business entities (e.g.: Blog, Comment, Payment, User as nouns)
- Business concepts and domain models (e.g.: Member, Points, Order)
- Incomprehensible words or pinyin abbreviations
- Domain-specific terminology

❌ Content that should be excluded:
1. Technical vocabulary: Controller, Service, Repository, Mapper, DTO, VO, PO, Entity, Request, Response, Config, Filter, Interceptor, Exception, Helper, Utils, Util, etc.
2. Implementation details and data transfer objects: entries containing suffixes like "Request", "Response", "Dto", "Entity"
3. Technical operation verbs: validate, check, convert, deserialize, serialize, encode, decode, etc.
4. Technical operations in method names: e.g., "checkIfVipAccount" should extract only "VIP Account", "isLimitExceeded" should extract only "Limit"
5. Common library APIs (e.g., Spring, OkHttp, Retrofit) and common class names (e.g., List, Map)

**Processing Rules:**
1. If the extracted entry contains technical suffixes (e.g., "CreateCommentDto"), convert it to pure business concepts (e.g., "Comment" not "Create Comment Data Transfer Object")
2. If method names contain technical operations (e.g., "checkIfVipAccount"), extract business meaning ("VIP Account" not "Check If VIP Account")
3. If class names contain technical vocabulary suffixes, remove the suffix before adding to the dictionary

Project README information:

${'$'}context.readme

**Output Format Requirements:**

✅ MUST return CSV format (comma-separated values)
✅ CSV header: Chinese,English,Description
✅ Each line contains one concept: [Chinese],[English],[Description]
✅ Return ONLY data, no other text, explanations, tables, or markdown formatting
✅ If data contains commas, wrap the field in double quotes, e.g.: "Concept A,Concept B",CodeConcept,Description

Example:
```
Chinese,English,Description
博客,Blog,a blog post
评论,Comment,a comment on a blog
支付,Payment,a payment transaction
```

Based on the following filenames and code snippets, extract important business concepts and return them in CSV format:

${'$'}context.code
        """.trimIndent()
    }
}

class TemplateNotFoundException(message: String) : Exception(message)

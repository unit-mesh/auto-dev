# JSON 场景加载器

JSON 场景加载器允许你使用声明式的 JSON 配置文件来定义复杂的测试场景，特别适合需要多工具调用和详细验证的场景。

## 🎯 为什么使用 JSON 配置？

相比编程式定义测试用例，JSON 配置提供了以下优势：

1. **声明式定义**：更清晰、更易读的测试场景描述
2. **易于维护**：非开发人员也可以编辑和创建测试场景
3. **版本控制友好**：JSON 文件更容易进行 diff 和 review
4. **可复用性**：场景配置可以轻松共享和复用
5. **文档集成**：可以直接在配置中引用相关文档链接

## 📋 JSON 配置格式

### 基本结构

```json
{
  "id": "unique-test-id",
  "name": "测试场景名称",
  "description": "详细描述",
  "category": "business-scenario",
  
  "task": {
    "description": "任务描述",
    "context": "额外的上下文信息",
    "documentation": [
      "https://docs.example.com/guide"
    ]
  },
  
  "project": {
    "type": "gradle-spring-boot"
  },
  
  "expectedTools": [...],
  "expectedChanges": [...],
  "quality": {...},
  "config": {...}
}
```

### 字段说明

#### 基本信息

- **id** (必需): 唯一的测试场景 ID
- **name** (必需): 测试场景的名称
- **description** (必需): 详细描述测试场景的目的
- **category** (必需): 测试类别
  - `basic-robustness`: 基础健壮性测试
  - `business-scenario`: 业务场景测试
  - `error-recovery`: 错误恢复测试
  - `performance`: 性能测试
  - `boundary-conditions`: 边界条件测试

#### 任务定义 (task)

- **description** (必需): 任务的详细描述
- **context** (可选): 额外的上下文信息
- **documentation** (可选): 相关文档链接数组

#### 项目配置 (project)

- **type** (必需): 项目类型
  - `gradle-spring-boot`
  - `maven-spring-boot`
  - `npm-node`
  - `empty`

#### 期望的工具调用 (expectedTools)

每个工具调用配置包含：

```json
{
  "tool": "read-file",
  "required": true,
  "minCalls": 2,
  "maxCalls": 10,
  "order": 1,
  "parameters": {},
  "description": "工具调用的说明"
}
```

- **tool** (必需): 工具名称 (`read-file`, `write-file`, `edit-file`, `shell`, `glob`, `grep`, `web-fetch`)
- **required** (必需): 是否必需调用此工具
- **minCalls** (可选): 最小调用次数
- **maxCalls** (可选): 最大调用次数
- **order** (可选): 期望的调用顺序
- **parameters** (可选): 期望的参数
- **description** (可选): 工具调用的说明

#### 期望的文件变更 (expectedChanges)

每个变更配置包含：

```json
{
  "type": "file-created",
  "path": "src/main/java/User.java",
  "pattern": ".*Service\\.java",
  "content": "spring-ai-deepseek",
  "required": true,
  "description": "变更的说明"
}
```

- **type** (必需): 变更类型
  - `file-created`: 文件创建
  - `file-modified`: 文件修改
  - `file-deleted`: 文件删除
  - `dependency-added`: 依赖添加
- **path** (可选): 具体的文件路径
- **pattern** (可选): 文件路径的正则表达式（字符串格式）
- **content** (可选): 期望的内容或正则表达式
- **required** (必需): 是否必需此变更
- **description** (可选): 变更的说明

#### 质量阈值 (quality)

```json
{
  "minToolAccuracy": 0.7,
  "maxExecutionTime": 600000,
  "minTaskCompletion": 0.8,
  "maxCodeIssues": 3
}
```

#### 测试配置 (config)

```json
{
  "timeout": 600000,
  "maxIterations": 15,
  "retryCount": 1,
  "keepTestProject": false,
  "quiet": false
}
```

## 🚀 使用示例

### 1. 创建 JSON 配置文件

创建 `scenarios/my-test.json`:

```json
{
  "id": "my-test-001",
  "name": "My Test Scenario",
  "description": "Test description",
  "category": "business-scenario",
  
  "task": {
    "description": "Implement a feature",
    "documentation": [
      "https://docs.example.com"
    ]
  },
  
  "project": {
    "type": "gradle-spring-boot"
  },
  
  "expectedTools": [
    {
      "tool": "read-file",
      "required": true,
      "minCalls": 1
    },
    {
      "tool": "write-file",
      "required": true,
      "minCalls": 2
    }
  ],
  
  "expectedChanges": [
    {
      "type": "file-created",
      "pattern": ".*Service\\.java",
      "required": true
    }
  ]
}
```

### 2. 在测试中加载场景

```typescript
import { JsonScenarioLoader, TestEngine } from '../framework';

// 加载单个场景
const testCase = await JsonScenarioLoader.loadFromFile('./scenarios/my-test.json');

// 加载目录下所有场景
const testCases = await JsonScenarioLoader.loadFromDirectory('./scenarios');

// 运行测试
const testEngine = new TestEngine({...});
const result = await testEngine.runTest(testCase);
```

### 3. 验证配置有效性

```typescript
import { JsonScenarioLoader } from '../framework';

const config = JSON.parse(jsonString);
const validation = JsonScenarioLoader.validateConfig(config);

if (!validation.valid) {
  console.error('配置错误:', validation.errors);
}
```

## 📝 完整示例

查看以下示例文件：

- `scenarios/spring-ai-deepseek.json` - Spring AI DeepSeek 集成示例
- `scenarios/complex-multi-tool.json` - 复杂多工具调用示例

## 🔧 高级用法

### 多工具调用顺序验证

通过 `order` 字段指定工具调用的期望顺序：

```json
{
  "expectedTools": [
    {
      "tool": "read-file",
      "required": true,
      "order": 1
    },
    {
      "tool": "edit-file",
      "required": true,
      "order": 2
    },
    {
      "tool": "shell",
      "required": true,
      "order": 3
    }
  ]
}
```

### 文档引用

在任务中引用相关文档，Agent 可以使用 `web-fetch` 工具获取文档内容：

```json
{
  "task": {
    "description": "Add Spring AI with DeepSeek",
    "documentation": [
      "https://docs.spring.io/spring-ai/reference/api/chat/deepseek-chat.html"
    ]
  },
  "expectedTools": [
    {
      "tool": "web-fetch",
      "required": false,
      "description": "Fetch documentation if needed"
    }
  ]
}
```

### 正则表达式匹配

使用正则表达式匹配文件路径或内容：

```json
{
  "expectedChanges": [
    {
      "type": "file-created",
      "pattern": ".*DeepSeek.*Service\\.java",
      "required": true
    },
    {
      "type": "file-modified",
      "path": "build.gradle.kts",
      "content": "spring-ai-deepseek",
      "required": true
    }
  ]
}
```

## 🎯 最佳实践

1. **使用描述性的 ID**: 使用有意义的 ID，如 `spring-ai-deepseek-001`
2. **提供详细的描述**: 在 `description` 字段中清晰说明测试目的
3. **引用文档**: 在 `documentation` 中提供相关文档链接
4. **合理设置阈值**: 根据场景复杂度调整质量阈值
5. **使用工具说明**: 在 `expectedTools` 中添加 `description` 说明期望
6. **验证配置**: 使用 `validateConfig` 验证配置有效性

## 🐛 故障排除

### 配置验证失败

使用 `validateConfig` 检查配置：

```typescript
const validation = JsonScenarioLoader.validateConfig(config);
console.log(validation.errors);
```

### 正则表达式不匹配

确保正则表达式字符串格式正确，不需要额外的转义：

```json
{
  "pattern": ".*Service\\.java"  // ✅ 正确
  "pattern": ".*Service\\\\.java" // ❌ 错误（过度转义）
}
```

### 工具调用顺序错误

检查 `order` 字段是否正确设置，并确保期望的顺序合理。


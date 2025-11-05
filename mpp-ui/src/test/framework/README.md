# AI Agent Robustness Testing Framework

## 概述

这是一个专门为测试 AI Agent 健壮性而设计的测试框架，重点关注：

1. **提示词效果验证** - 测试系统提示词是否能正确引导 Agent 行为
2. **工具调用分析** - 跟踪和验证 Agent 调用的工具类型、参数、顺序
3. **结果变更分析** - 检测和评估 Agent 产生的代码变更和质量
4. **场景扩展能力** - 支持不同复杂度和类型的测试场景

## 框架架构

```
framework/
├── core/                    # 核心测试引擎
│   ├── TestEngine.ts       # 主测试执行引擎
│   ├── TestCase.ts         # 测试用例定义
│   └── TestResult.ts       # 测试结果数据结构
├── analyzers/              # 结果分析器
│   ├── PromptAnalyzer.ts   # 提示词效果分析
│   ├── ToolCallAnalyzer.ts # 工具调用分析
│   ├── CodeChangeAnalyzer.ts # 代码变更分析
│   └── QualityAnalyzer.ts  # 代码质量分析
├── scenarios/              # 测试场景定义
│   ├── BasicScenarios.ts   # 基础场景
│   ├── BusinessScenarios.ts # 业务场景
│   └── ErrorScenarios.ts   # 错误处理场景
├── reporters/              # 测试报告生成器
│   ├── ConsoleReporter.ts  # 控制台报告
│   ├── HTMLReporter.ts     # HTML 报告
│   └── JSONReporter.ts     # JSON 数据报告
└── utils/                  # 工具函数
    ├── ProjectGenerator.ts # 测试项目生成
    ├── AgentExecutor.ts    # Agent 执行器
    └── FileAnalyzer.ts     # 文件分析工具
```

## 核心概念

### 1. 测试用例定义 (TestCase)

```typescript
interface TestCase {
  id: string;
  name: string;
  description: string;
  category: TestCategory;
  
  // 测试输入
  task: string;
  projectType: ProjectType;
  initialFiles?: FileDefinition[];
  
  // 期望行为
  expectedPromptBehavior: PromptExpectation;
  expectedToolCalls: ToolCallExpectation[];
  expectedChanges: ChangeExpectation[];
  
  // 测试配置
  timeout: number;
  maxIterations: number;
  retryCount: number;
}
```

### 2. 分析维度

- **提示词效果** - Agent 是否按照系统提示词的指导进行操作
- **工具选择准确性** - 是否选择了正确的工具来完成任务
- **工具调用顺序** - 工具调用的逻辑顺序是否合理
- **参数正确性** - 工具调用的参数是否正确
- **错误处理** - 遇到错误时的处理和恢复能力
- **代码质量** - 生成代码的语法正确性、结构合理性
- **功能完整性** - 是否完成了预期的功能需求

### 3. 测试场景分类

- **基础健壮性** - 基本工具使用和文件操作
- **业务场景** - 复杂的业务功能实现
- **错误恢复** - 异常情况下的处理能力
- **性能压力** - 大型项目和复杂任务处理
- **边界条件** - 极端情况下的行为表现

## 使用示例

```typescript
import { TestEngine, TestCase, BasicScenarios } from './framework';

const testEngine = new TestEngine({
  agentPath: './dist/index.js',
  outputDir: './test-results',
  reporters: ['console', 'html', 'json']
});

// 运行基础场景测试
await testEngine.runScenarios(BasicScenarios.getAllScenarios());

// 运行自定义测试
const customTest: TestCase = {
  id: 'custom-001',
  name: '添加 GraphQL 支持',
  task: 'Add GraphQL support to the Spring Boot project',
  expectedToolCalls: [
    { tool: 'read-file', pattern: /build\.gradle/ },
    { tool: 'write-file', pattern: /GraphQL/ }
  ],
  // ... 其他配置
};

await testEngine.runTest(customTest);
```

## 扩展能力

框架设计为高度可扩展：

1. **自定义分析器** - 可以添加新的结果分析维度
2. **场景模板** - 支持快速创建新的测试场景
3. **报告格式** - 支持多种报告输出格式
4. **集成能力** - 可以集成到 CI/CD 流程中

## 快速开始

### 1. 安装依赖

```bash
cd mpp-ui
npm install
```

### 2. 构建 CodingAgent

```bash
npm run build
```

### 3. 运行测试

```bash
# 运行基础健壮性测试套件
npx ts-node src/test/framework/cli.ts suite basic

# 运行业务场景测试
npx ts-node src/test/framework/cli.ts suite business

# 运行自定义测试
npx ts-node src/test/framework/cli.ts custom -t "Create a simple REST controller"

# 列出所有可用的测试模板
npx ts-node src/test/framework/cli.ts list-templates
```

### 4. 编程方式使用

```typescript
import {
  createTestEngine,
  createBasicTest,
  ConsoleReporter
} from './src/test/framework';

async function runMyTest() {
  // 创建测试引擎
  const testEngine = createTestEngine({
    agentPath: './dist/index.js',
    verbose: true
  });

  // 创建测试用例
  const testCase = createBasicTest({
    name: '我的测试',
    task: 'Create a README.md file',
    expectedTools: ['write-file']
  });

  // 运行测试
  const result = await testEngine.runTest(testCase);

  // 生成报告
  console.log(ConsoleReporter.generateTestReport(result));
}
```

## 测试场景模板

框架提供了多种预定义的测试场景模板：

### 基础操作模板

```bash
# 文件探索
npx ts-node src/test/framework/cli.ts scenario basic-file-operations -p '{"operation": "explore"}'

# 文件读取
npx ts-node src/test/framework/cli.ts scenario basic-file-operations -p '{"operation": "read", "targetFile": "build.gradle.kts"}'

# 文件创建
npx ts-node src/test/framework/cli.ts scenario basic-file-operations -p '{"operation": "write", "targetFile": "CHANGELOG.md"}'
```

### 业务场景模板

```bash
# CRUD 实现
npx ts-node src/test/framework/cli.ts scenario business-feature-implementation -p '{"feature": "crud", "entity": "Product"}'

# 认证系统
npx ts-node src/test/framework/cli.ts scenario business-feature-implementation -p '{"feature": "auth"}'
```

### 错误恢复模板

```bash
# 编译错误恢复
npx ts-node src/test/framework/cli.ts scenario error-recovery -p '{"errorType": "compilation"}'

# 依赖冲突解决
npx ts-node src/test/framework/cli.ts scenario error-recovery -p '{"errorType": "dependency"}'
```

## 测试结果分析

框架提供了多维度的测试结果分析：

### 1. 提示词效果分析
- 是否遵循系统提示词指导
- 是否采用合理的执行顺序
- 是否处理了异常情况

### 2. 工具调用分析
- 工具选择的准确性
- 工具调用的顺序合理性
- 参数使用的正确性

### 3. 代码质量分析
- 语法错误检测
- 结构问题识别
- 最佳实践验证

### 4. 任务完成度分析
- 功能实现完整性
- 向后兼容性检查
- 回归问题识别

## 扩展开发

### 添加自定义分析器

```typescript
import { TestResult, TestCase } from './core/TestResult';

export class CustomAnalyzer {
  static analyze(testCase: TestCase, executionInfo: any): CustomAnalysisResult {
    // 实现自定义分析逻辑
    return {
      customMetric: 0.85,
      issues: []
    };
  }
}
```

### 创建自定义场景模板

```typescript
import { ScenarioBuilder } from './scenarios/ScenarioBuilder';

ScenarioBuilder.registerTemplate({
  id: 'my-custom-scenario',
  name: '我的自定义场景',
  description: '测试特定的业务逻辑',
  category: TestCategory.BUSINESS_SCENARIO,
  difficulty: 'medium',
  estimatedDuration: 180000,
  parameters: {
    businessType: {
      type: 'string',
      description: '业务类型',
      required: true
    }
  },
  generate: (params) => {
    // 生成测试用例逻辑
    return TestCaseBuilder.create('custom-test')
      .withTask(`Implement ${params.businessType} functionality`)
      .build();
  }
});
```

## 最佳实践

### 1. 测试用例设计
- 明确定义期望行为
- 设置合理的超时时间
- 包含必要的验证点

### 2. 场景覆盖
- 覆盖基础功能测试
- 包含复杂业务场景
- 考虑边界条件和异常情况

### 3. 结果分析
- 关注综合得分趋势
- 分析常见问题模式
- 持续优化系统提示词

### 4. 持续集成
- 集成到 CI/CD 流程
- 设置质量门禁
- 定期运行回归测试

## 故障排除

### 常见问题

1. **测试超时**
   - 检查 Agent 是否正常启动
   - 增加超时时间设置
   - 检查系统资源使用情况

2. **工具调用失败**
   - 验证工具注册是否正确
   - 检查工具权限设置
   - 查看详细错误日志

3. **代码质量问题**
   - 检查生成的代码语法
   - 验证项目结构完整性
   - 确认依赖配置正确

### 调试模式

```bash
# 启用详细输出
npx ts-node src/test/framework/cli.ts suite basic -v

# 保留测试项目用于检查
npx ts-node src/test/framework/cli.ts suite basic -k

# 查看测试项目位置
ls /tmp/agent-test-*
```

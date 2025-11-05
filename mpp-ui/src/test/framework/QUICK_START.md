# AI Agent 健壮性测试框架 - 快速开始

## 🎯 框架概述

这是一个专门为测试 AI Agent 健壮性而设计的测试框架，重点关注：

- **提示词效果验证** - 测试系统提示词是否能正确引导 Agent 行为
- **工具调用分析** - 跟踪和验证 Agent 调用的工具类型、参数、顺序  
- **结果变更分析** - 检测和评估 Agent 产生的代码变更和质量
- **场景扩展能力** - 支持不同复杂度和类型的测试场景

## 🚀 快速开始

### 1. 验证框架结构
```bash
npm run test:framework
```

### 2. 编译 TypeScript
```bash
npm run build:ts
```

### 3. 使用编程接口

```typescript
import { 
  TestEngine, 
  TestCaseBuilder, 
  TestCategory, 
  ProjectType,
  ConsoleReporter 
} from './src/test/framework';

// 创建测试引擎
const testEngine = new TestEngine({
  agentPath: './dist/index.js',
  outputDir: './test-results',
  reporters: ['console'],
  verbose: true
});

// 创建测试用例
const testCase = TestCaseBuilder.create('my-test-001')
  .withName('我的第一个测试')
  .withDescription('测试基本的文件操作能力')
  .withCategory(TestCategory.BASIC_ROBUSTNESS)
  .withTask('Create a README.md file with project description')
  .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
  .expectTool('write-file', { required: true })
  .expectChange('file-created', { path: 'README.md', required: true })
  .withTimeout(120000)
  .build();

// 运行测试
const result = await testEngine.runTest(testCase);

// 生成报告
console.log(ConsoleReporter.generateTestReport(result));
```

### 4. 使用场景模板

```typescript
import { ScenarioBuilder } from './src/test/framework';

// 查看所有可用模板
const templates = ScenarioBuilder.getAllTemplates();
console.log(`可用模板: ${templates.length} 个`);

// 使用模板生成测试用例
const testCase = ScenarioBuilder.generateFromTemplate('basic-file-operations', {
  operation: 'write',
  targetFile: 'CHANGELOG.md'
});

// 运行测试
const result = await testEngine.runTest(testCase);
```

## 📊 测试报告示例

框架会生成详细的测试报告，包括：

```
================================================================================
🧪 测试报告: 我的第一个测试
================================================================================
📝 描述: 测试基本的文件操作能力
🏷️  类别: basic-robustness
📋 任务: Create a README.md file with project description
⏱️  执行时间: 5000ms
📊 综合得分: 88.5%
✅ 测试状态: passed

📋 提示词效果分析:
  • 遵循系统提示词: ✅
  • 首先探索项目: ✅
  • 使用合适工具: ✅
  • 优雅处理错误: ✅
  • 有效性得分: 92.3%

🔧 工具调用分析:
  • 总调用次数: 3
  • 使用的工具: glob, read-file, write-file
  • 工具准确率: 100.0%
  • 顺序正确性: 95.0%
  • 参数正确性: 90.0%

📊 代码质量分析:
  • 语法错误: 0
  • 结构问题: 0
  • 最佳实践违规: 1
  • 总问题数: 1
  • 质量得分: 95.0%

✅ 任务完成分析:
  • 任务完成: ✅
  • 完成度: 100.0%
  • 向后兼容: ✅
  ✅ 已实现功能:
    - 文件创建功能

📁 文件变更 (1):
  ➕ created: README.md

💡 改进建议:
  • 建议在代码中添加更多注释以提高可读性
================================================================================
```

## 🔧 自定义测试场景

你可以轻松创建自定义的测试场景：

```typescript
// 注册自定义场景模板
ScenarioBuilder.registerTemplate({
  id: 'my-custom-scenario',
  name: '我的自定义场景',
  description: '测试特定的业务逻辑',
  category: TestCategory.BUSINESS_SCENARIO,
  difficulty: 'medium',
  estimatedDuration: 180000,
  parameters: {
    entityName: {
      type: 'string',
      description: '实体名称',
      required: true
    }
  },
  generate: (params) => {
    return TestCaseBuilder.create(`custom-${Date.now()}`)
      .withTask(`Create CRUD operations for ${params.entityName}`)
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .expectTool('write-file', { required: true, minCalls: 3 })
      .build();
  }
});

// 使用自定义场景
const customTest = ScenarioBuilder.generateFromTemplate('my-custom-scenario', {
  entityName: 'Product'
});
```

## 📚 更多资源

- **详细文档**: [README.md](./README.md)
- **API 参考**: 查看各个模块的 TypeScript 定义
- **示例代码**: [examples/BasicRobustnessTest.ts](./examples/BasicRobustnessTest.ts)

## 🤝 贡献

框架设计为高度可扩展，你可以：

1. 添加新的分析器模块
2. 创建自定义的测试场景模板
3. 扩展报告生成器
4. 集成到 CI/CD 流程中

开始使用这个框架来系统性地测试和改进你的 AI Agent 吧！🚀

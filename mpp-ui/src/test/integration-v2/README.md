# CodingAgent Integration Tests v2 - 基于新测试框架

这是使用新的 AI Agent 健壮性测试框架重新编写的集成测试套件。

## 🎯 框架优势

相比原有的集成测试，新框架提供了：

- **深度分析**: 提示词效果、工具调用、代码质量的多维度分析
- **标准化报告**: 统一的测试报告格式和评分体系
- **场景模板**: 可复用的测试场景模板
- **扩展性**: 易于添加新的测试场景和分析维度
- **可视化**: 详细的测试结果可视化展示

## 📁 测试结构

```
integration-v2/
├── README.md                    # 本文档
├── simple-robustness.test.ts    # 基础健壮性测试
├── business-scenarios.test.ts   # 业务场景测试
├── error-recovery.test.ts       # 错误恢复测试
├── performance.test.ts          # 性能测试
└── custom-scenarios.test.ts     # 自定义场景测试
```

## 🚀 运行测试

### 前置条件

1. 确保测试框架已编译：
   ```bash
   npm run build:ts
   ```

2. 验证框架结构：
   ```bash
   npm run test:framework
   ```

### 运行测试

```bash
# 运行所有集成测试 v2
npm test src/test/integration-v2

# 运行特定测试文件
npm test src/test/integration-v2/simple-robustness.test.ts
npm test src/test/integration-v2/business-scenarios.test.ts

# 运行时显示详细输出
npm test src/test/integration-v2 -- --reporter=verbose
```

## 📊 测试报告

新框架生成的测试报告包含：

### 1. 执行统计
- 通过率、执行时间、综合得分
- 工具使用统计
- 按类别的成功率统计

### 2. 提示词分析
- 系统提示词遵循程度
- 项目探索策略有效性
- 工具选择合理性评估

### 3. 工具调用分析
- 工具使用准确率
- 调用顺序正确性
- 参数使用正确性
- 意外工具识别

### 4. 代码质量分析
- 语法错误检测
- 结构问题识别
- 最佳实践验证
- 质量得分计算

### 5. 任务完成度分析
- 功能实现完整性
- 向后兼容性检查
- 回归问题识别

## 🎭 测试场景

### 基础健壮性测试
- 项目探索能力
- 文件读写操作
- 内容搜索功能
- 基础错误处理

### 业务场景测试
- **视频支持**: 为 BlogPost 实体添加视频功能
- **JWT 认证**: 实现完整的 JWT 认证系统
- **GraphQL API**: 添加 GraphQL 支持
- **Spring 升级**: 处理版本升级和兼容性问题

### 错误恢复测试
- 编译错误恢复
- 依赖冲突解决
- 语法错误修复
- 构建失败处理

### 性能测试
- 简单任务执行效率
- 复杂任务处理能力
- 资源使用优化
- 并发处理能力

## 🔧 自定义测试

你可以轻松添加自定义测试场景：

```typescript
import { TestEngine, TestCaseBuilder, TestCategory } from '../framework';

const customTest = TestCaseBuilder.create('custom-001')
  .withName('我的自定义测试')
  .withTask('实现特定的业务功能')
  .withCategory(TestCategory.BUSINESS_SCENARIO)
  .expectTool('write-file', { required: true })
  .expectChange('file-created', { required: true })
  .build();

const testEngine = new TestEngine({
  agentPath: './dist/index.js',
  outputDir: './test-results'
});

const result = await testEngine.runTest(customTest);
```

## 📈 质量标准

### 性能指标
- **简单测试**: 2分钟内完成
- **业务测试**: 10分钟内完成
- **复杂测试**: 15分钟内完成

### 质量指标
- **工具使用准确率**: ≥70%
- **任务完成率**: 简单测试≥95%，业务测试≥80%
- **错误恢复率**: ≥60%
- **代码质量问题**: ≤3个/项目

### 可靠性指标
- **测试稳定性**: 跨运行一致性
- **错误处理**: 优雅的超时和失败处理
- **资源管理**: 正确的项目清理

## 🔍 调试和故障排除

### 启用调试模式
```bash
# 保留测试项目用于检查
KEEP_TEST_PROJECTS=true npm test src/test/integration-v2

# 启用详细输出
DEBUG=true npm test src/test/integration-v2
```

### 常见问题
1. **超时错误**: 检查系统资源，增加超时设置
2. **工具调用失败**: 验证工具注册和权限
3. **代码质量问题**: 检查生成代码的语法和结构
4. **环境问题**: 确认 Node.js 版本和依赖

## 🤝 贡献指南

添加新测试时：

1. 使用新的测试框架 API
2. 遵循现有的命名约定
3. 包含全面的期望验证
4. 更新本 README 文档
5. 确保测试的确定性和 CI/CD 兼容性

## 📚 相关文档

- [测试框架文档](../framework/README.md)
- [快速开始指南](../framework/QUICK_START.md)
- [API 参考](../framework/index.ts)

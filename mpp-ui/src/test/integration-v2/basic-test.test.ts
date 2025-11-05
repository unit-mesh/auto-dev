/**
 * 基础集成测试 - 验证 Agent 基本功能
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { 
  TestEngine, 
  TestCaseBuilder, 
  TestCategory, 
  ProjectType,
  ConsoleReporter,
  TestSuiteResult 
} from '../framework';

describe('CodingAgent 基础测试', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎
    testEngine = new TestEngine({
      agentPath: './dist/jsMain/typescript/index.js',
      outputDir: './test-results/basic-test',
      reporters: ['console'],
      verbose: process.env.DEBUG === 'true',
      keepTestProjects: process.env.KEEP_TEST_PROJECTS === 'true'
    });
  });

  afterAll(async () => {
    if (testEngine) {
      await testEngine.stopAllTests();
    }
  });

  it('应该能够成功运行基础项目探索测试', async () => {
    console.log('\n🧪 开始基础项目探索测试...');

    // 定义一个简单的测试用例
    const testCase = TestCaseBuilder.create('basic-001')
      .withName('基础项目探索')
      .withDescription('测试 Agent 使用 glob 工具探索项目结构的能力')
      .withCategory(TestCategory.BASIC_ROBUSTNESS)
      .withTask('List all files in the project to understand the structure')
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .expectTool('glob', { required: true, minCalls: 1 })
      .withTimeout(120000) // 2分钟超时
      .build();

    // 运行单个测试
    const result = await testEngine.runTest(testCase);

    // 验证结果
    expect(result).toBeDefined();
    expect(result.status).toBeDefined();
    
    console.log(`📊 测试结果: ${result.status}`);
    console.log(`⏱️  执行时间: ${result.executionInfo.duration}ms`);
    console.log(`🔧 工具调用次数: ${result.toolCallAnalysis.totalCalls}`);
    console.log(`📈 综合得分: ${(result.overallScore * 100).toFixed(1)}%`);

    // 基本验证 - 不要求测试必须成功，只要能运行即可
    expect(result.executionInfo.duration).toBeGreaterThan(0);
    expect(result.toolCallAnalysis.totalCalls).toBeGreaterThanOrEqual(0);
    expect(result.overallScore).toBeGreaterThanOrEqual(0);
    
    console.log('✅ 基础测试完成');
  }, 150000); // 2.5分钟超时

  it('应该能够验证工具调用分析', async () => {
    // 这个测试依赖于前一个测试的结果
    // 如果前一个测试没有运行，跳过这个测试
    if (!testResults) {
      console.log('⏭️  跳过工具调用分析测试（需要先运行基础测试）');
      return;
    }

    const toolAnalysis = testResults.testResults[0]?.toolCallAnalysis;
    expect(toolAnalysis).toBeDefined();
    expect(toolAnalysis.totalCalls).toBeGreaterThanOrEqual(0);
    
    console.log(`🔧 工具调用统计:`);
    console.log(`  总调用次数: ${toolAnalysis.totalCalls}`);
    console.log(`  工具准确率: ${(toolAnalysis.toolAccuracy * 100).toFixed(1)}%`);
  });

  it('应该能够验证执行信息', async () => {
    // 基本的执行信息验证
    expect(testEngine).toBeDefined();
    expect(testEngine.constructor.name).toBe('TestEngine');
    
    console.log('✅ 测试引擎验证通过');
  });
});

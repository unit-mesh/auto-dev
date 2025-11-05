/**
 * 性能测试 - 使用新测试框架
 * 
 * 验证 CodingAgent 在不同复杂度任务下的性能表现
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

describe('CodingAgent 性能测试 v2', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎 - 性能测试使用顺序执行以获得准确的性能数据
    testEngine = new TestEngine({
      agentPath: './dist/index.js',
      outputDir: './test-results/performance',
      reporters: ['console'],
      verbose: false, // 性能测试关闭详细输出以减少干扰
      keepTestProjects: process.env.KEEP_TEST_PROJECTS === 'true',
      parallel: false // 顺序执行以获得准确的性能数据
    });
  });

  afterAll(async () => {
    if (testEngine) {
      await testEngine.stopAllTests();
    }
  });

  it('应该在不同复杂度下表现出合理的性能', async () => {
    console.log('\n⚡ 开始运行性能测试套件...');

    // 定义不同复杂度的性能测试用例
    const testCases = [
      // 1. 简单任务 - 基准性能
      TestCaseBuilder.create('perf-001')
        .withName('简单任务性能测试')
        .withDescription('测试简单文件操作任务的执行性能')
        .withCategory(TestCategory.PERFORMANCE)
        .withTask('List all Java files in the project')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('glob', { required: true })
        .withTimeout(30000) // 30秒
        .build(),

      // 2. 中等复杂度任务
      TestCaseBuilder.create('perf-002')
        .withName('中等复杂度任务性能测试')
        .withDescription('测试中等复杂度任务的执行性能')
        .withCategory(TestCategory.PERFORMANCE)
        .withTask('Create a simple REST controller with basic CRUD operations for a Product entity')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true })
        .expectTool('write-file', { required: true, minCalls: 3 })
        .withTimeout(120000) // 2分钟
        .build(),

      // 3. 复杂任务
      TestCaseBuilder.create('perf-003')
        .withName('复杂任务性能测试')
        .withDescription('测试复杂业务逻辑实现的执行性能')
        .withCategory(TestCategory.PERFORMANCE)
        .withTask('Implement a complete user management system with authentication, authorization, and CRUD operations')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true, minCalls: 6 })
        .withTimeout(300000) // 5分钟
        .build(),

      // 4. 高复杂度任务
      TestCaseBuilder.create('perf-004')
        .withName('高复杂度任务性能测试')
        .withDescription('测试高复杂度系统集成任务的执行性能')
        .withCategory(TestCategory.PERFORMANCE)
        .withTask('Add comprehensive logging, monitoring, and health check endpoints with custom metrics and database integration')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 3 })
        .expectTool('write-file', { required: true, minCalls: 8 })
        .withTimeout(480000) // 8分钟
        .build()
    ];

    // 运行性能测试套件
    testResults = await testEngine.runScenarios(testCases);

    // 生成详细报告
    console.log(ConsoleReporter.generateSuiteReport(testResults));

    // 验证基本测试结果
    expect(testResults.totalTests).toBe(4);
    expect(testResults.passedTests).toBeGreaterThanOrEqual(3); // 至少75%通过率

    // 分析性能数据
    const performanceData = testResults.testResults.map((result, index) => ({
      complexity: ['简单', '中等', '复杂', '高复杂'][index],
      duration: result.executionInfo.duration,
      score: result.overallScore,
      toolCalls: result.toolCallAnalysis.totalCalls,
      fileChanges: result.fileChanges.length
    }));

    console.log('\n📊 性能分析结果:');
    performanceData.forEach(data => {
      console.log(`  ${data.complexity}任务: ${data.duration}ms, 得分: ${(data.score * 100).toFixed(1)}%, 工具调用: ${data.toolCalls}次, 文件变更: ${data.fileChanges}个`);
    });

    // 验证性能指标
    expect(performanceData[0].duration).toBeLessThan(30000); // 简单任务<30秒
    expect(performanceData[1].duration).toBeLessThan(120000); // 中等任务<2分钟
    expect(performanceData[2].duration).toBeLessThan(300000); // 复杂任务<5分钟
    expect(performanceData[3].duration).toBeLessThan(480000); // 高复杂任务<8分钟

    console.log('\n✅ 性能测试套件完成');
    console.log(`⏱️  总执行时间: ${(testResults.duration / 1000 / 60).toFixed(1)}分钟`);
    console.log(`📈 平均得分: ${(testResults.summary.averageScore * 100).toFixed(1)}%`);
  }, 1200000); // 20分钟超时

  it('应该验证性能扩展性', async () => {
    expect(testResults).toBeDefined();
    
    // 验证执行时间与任务复杂度的关系
    const durations = testResults.testResults.map(r => r.executionInfo.duration);
    
    // 简单任务应该明显快于复杂任务
    expect(durations[0]).toBeLessThan(durations[2]); // 简单 < 复杂
    expect(durations[1]).toBeLessThan(durations[3]); // 中等 < 高复杂

    // 验证性能退化不会过于严重
    const performanceRatio = durations[3] / durations[0]; // 最复杂 / 最简单
    expect(performanceRatio).toBeLessThan(20); // 性能退化不超过20倍
  });

  it('应该验证工具调用效率', async () => {
    expect(testResults).toBeDefined();
    
    // 验证工具调用次数与任务复杂度的合理关系
    const toolCallCounts = testResults.testResults.map(r => r.toolCallAnalysis.totalCalls);
    
    // 复杂任务应该有更多的工具调用
    expect(toolCallCounts[2]).toBeGreaterThan(toolCallCounts[0]); // 复杂 > 简单
    expect(toolCallCounts[3]).toBeGreaterThan(toolCallCounts[1]); // 高复杂 > 中等

    // 验证工具调用效率（平均每次调用的时间）
    const avgCallDurations = testResults.testResults.map((result, index) => 
      result.executionInfo.duration / result.toolCallAnalysis.totalCalls
    );

    // 工具调用效率应该相对稳定
    const maxEfficiency = Math.max(...avgCallDurations);
    const minEfficiency = Math.min(...avgCallDurations);
    const efficiencyVariation = maxEfficiency / minEfficiency;
    
    expect(efficiencyVariation).toBeLessThan(5); // 效率变化不超过5倍
  });

  it('应该验证质量与性能的平衡', async () => {
    expect(testResults).toBeDefined();
    
    // 验证在性能压力下质量不会显著下降
    const qualityScores = testResults.testResults.map(r => r.codeQuality.qualityScore);
    const avgQualityScore = qualityScores.reduce((sum, score) => sum + score, 0) / qualityScores.length;

    expect(avgQualityScore).toBeGreaterThanOrEqual(0.7); // 平均质量得分≥70%

    // 验证最复杂任务的质量不会过低
    expect(qualityScores[3]).toBeGreaterThanOrEqual(0.6); // 最复杂任务质量≥60%

    // 验证任务完成度
    const completionScores = testResults.testResults.map(r => r.taskCompletion.completionScore);
    const avgCompletionScore = completionScores.reduce((sum, score) => sum + score, 0) / completionScores.length;

    expect(avgCompletionScore).toBeGreaterThanOrEqual(0.75); // 平均完成度≥75%
  });

  it('应该验证资源使用效率', async () => {
    expect(testResults).toBeDefined();
    
    // 验证文件变更效率（每个文件变更的平均时间）
    const fileChangeEfficiency = testResults.testResults.map(result => {
      const fileChanges = result.fileChanges.length;
      return fileChanges > 0 ? result.executionInfo.duration / fileChanges : 0;
    });

    // 文件变更效率应该在合理范围内
    const avgFileChangeTime = fileChangeEfficiency.reduce((sum, time) => sum + time, 0) / fileChangeEfficiency.length;
    expect(avgFileChangeTime).toBeLessThan(60000); // 平均每个文件变更<1分钟

    // 验证迭代效率
    const iterationCounts = testResults.testResults.map(r => r.executionInfo.iterations);
    const avgIterations = iterationCounts.reduce((sum, count) => sum + count, 0) / iterationCounts.length;
    
    expect(avgIterations).toBeLessThanOrEqual(8); // 平均迭代次数≤8次
  });
});

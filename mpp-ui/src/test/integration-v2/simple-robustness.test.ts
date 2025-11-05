/**
 * 简单健壮性集成测试 - 使用新测试框架
 * 
 * 验证 CodingAgent 的基础功能和工具使用能力
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

describe('CodingAgent 简单健壮性测试 v2', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎
    testEngine = new TestEngine({
      agentPath: './dist/jsMain/typescript/index.js',
      outputDir: './test-results/simple-robustness',
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

  it('应该成功运行所有基础健壮性测试', async () => {
    console.log('\n🧪 开始运行简单健壮性测试套件...');

    // 定义测试用例
    const testCases = [
      // 1. 基础项目探索
      TestCaseBuilder.create('simple-001')
        .withName('基础项目探索')
        .withDescription('测试 Agent 使用 glob 工具探索项目结构的能力')
        .withCategory(TestCategory.BASIC_ROBUSTNESS)
        .withTask('List all files in the project to understand the structure')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('glob', { required: true, minCalls: 1 })
        .expectTool('read-file', { required: false })
        .withTimeout(60000)
        .build(),

      // 2. 文件读取测试
      TestCaseBuilder.create('simple-002')
        .withName('文件读取测试')
        .withDescription('测试 Agent 使用 read-file 工具读取配置文件的能力')
        .withCategory(TestCategory.BASIC_ROBUSTNESS)
        .withTask('Read the build.gradle.kts file to understand the project configuration')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 1 })
        .withTimeout(60000)
        .build(),

      // 3. 文件创建测试
      TestCaseBuilder.create('simple-003')
        .withName('文件创建测试')
        .withDescription('测试 Agent 使用 write-file 工具创建新文件的能力')
        .withCategory(TestCategory.BASIC_ROBUSTNESS)
        .withTask('Create a README.md file with project description and setup instructions')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('write-file', { required: true, minCalls: 1 })
        .expectChange('file-created', { path: 'README.md', required: true })
        .withTimeout(90000)
        .build(),

      // 4. 内容搜索测试
      TestCaseBuilder.create('simple-004')
        .withName('内容搜索测试')
        .withDescription('测试 Agent 使用 grep 工具搜索文件内容的能力')
        .withCategory(TestCategory.BASIC_ROBUSTNESS)
        .withTask('Search for all occurrences of "spring" in the project files')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('grep', { required: true, minCalls: 1 })
        .withTimeout(60000)
        .build(),

      // 5. 综合操作测试
      TestCaseBuilder.create('simple-005')
        .withName('综合操作测试')
        .withDescription('测试 Agent 综合使用多种工具完成复杂任务的能力')
        .withCategory(TestCategory.BASIC_ROBUSTNESS)
        .withTask('Analyze the project structure, read key files, and create a project summary document')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('glob', { required: true })
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true })
        .expectChange('file-created', { required: true })
        .withTimeout(120000)
        .build()
    ];

    // 运行测试套件
    testResults = await testEngine.runScenarios(testCases);

    // 生成详细报告
    console.log(ConsoleReporter.generateSuiteReport(testResults));

    // 验证测试结果
    expect(testResults.totalTests).toBe(5);
    expect(testResults.passedTests).toBeGreaterThanOrEqual(4); // 至少80%通过率
    expect(testResults.summary.averageScore).toBeGreaterThanOrEqual(0.7); // 平均得分≥70%

    // 验证关键指标
    const failedTests = testResults.testResults.filter(r => r.status !== 'passed');
    if (failedTests.length > 0) {
      console.log('\n⚠️  失败的测试:');
      failedTests.forEach(test => {
        console.log(`  - ${test.testCase.name}: ${test.status}`);
        if (test.errors.length > 0) {
          test.errors.forEach(error => console.log(`    错误: ${error}`));
        }
      });
    }

    // 验证工具使用情况
    const toolUsageStats = testResults.summary.toolUsageStats;
    expect(toolUsageStats['glob']).toBeGreaterThanOrEqual(1);
    expect(toolUsageStats['read-file']).toBeGreaterThanOrEqual(1);
    expect(toolUsageStats['write-file']).toBeGreaterThanOrEqual(1);

    console.log('\n✅ 简单健壮性测试套件完成');
    console.log(`📊 通过率: ${((testResults.passedTests / testResults.totalTests) * 100).toFixed(1)}%`);
    console.log(`⏱️  总执行时间: ${testResults.duration}ms`);
    console.log(`📈 平均得分: ${(testResults.summary.averageScore * 100).toFixed(1)}%`);
  }, 600000); // 10分钟超时

  it('应该验证提示词效果', async () => {
    expect(testResults).toBeDefined();
    
    // 验证提示词分析结果
    const promptAnalysisResults = testResults.testResults.map(r => r.promptAnalysis);
    const avgPromptEffectiveness = promptAnalysisResults.reduce(
      (sum, analysis) => sum + analysis.promptEffectivenessScore, 0
    ) / promptAnalysisResults.length;

    expect(avgPromptEffectiveness).toBeGreaterThanOrEqual(0.6); // 提示词有效性≥60%

    // 验证系统提示词遵循情况
    const followedPromptCount = promptAnalysisResults.filter(
      analysis => analysis.followedSystemPrompt
    ).length;
    
    expect(followedPromptCount / promptAnalysisResults.length).toBeGreaterThanOrEqual(0.8); // 80%遵循率
  });

  it('应该验证工具调用准确性', async () => {
    expect(testResults).toBeDefined();
    
    // 验证工具调用分析结果
    const toolAnalysisResults = testResults.testResults.map(r => r.toolCallAnalysis);
    const avgToolAccuracy = toolAnalysisResults.reduce(
      (sum, analysis) => sum + analysis.toolAccuracy, 0
    ) / toolAnalysisResults.length;

    expect(avgToolAccuracy).toBeGreaterThanOrEqual(0.7); // 工具使用准确率≥70%

    // 验证工具调用顺序
    const avgSequenceCorrectness = toolAnalysisResults.reduce(
      (sum, analysis) => sum + analysis.sequenceCorrectness, 0
    ) / toolAnalysisResults.length;

    expect(avgSequenceCorrectness).toBeGreaterThanOrEqual(0.6); // 顺序正确性≥60%
  });

  it('应该验证代码质量', async () => {
    expect(testResults).toBeDefined();
    
    // 验证代码质量分析结果
    const qualityResults = testResults.testResults.map(r => r.codeQuality);
    const avgQualityScore = qualityResults.reduce(
      (sum, quality) => sum + quality.qualityScore, 0
    ) / qualityResults.length;

    expect(avgQualityScore).toBeGreaterThanOrEqual(0.8); // 代码质量得分≥80%

    // 验证错误数量
    const totalIssues = qualityResults.reduce(
      (sum, quality) => sum + quality.totalIssues, 0
    );
    const avgIssuesPerTest = totalIssues / qualityResults.length;

    expect(avgIssuesPerTest).toBeLessThanOrEqual(3); // 平均每个测试≤3个问题
  });
});

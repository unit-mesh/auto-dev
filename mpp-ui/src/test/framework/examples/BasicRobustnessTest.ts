/**
 * 基础健壮性测试示例
 * 
 * 演示如何使用测试框架创建和运行基础的 AI Agent 健壮性测试
 */

import { TestEngine } from '../core/TestEngine';
import { TestCaseBuilder, TestCategory, ProjectType } from '../core/TestCase';
import { ScenarioBuilder } from '../scenarios/ScenarioBuilder';
import { ConsoleReporter } from '../reporters/ConsoleReporter';

/**
 * 运行基础健壮性测试示例
 */
export async function runBasicRobustnessTest(): Promise<void> {
  console.log('🚀 开始运行基础健壮性测试示例');

  // 1. 配置测试引擎
  const testEngine = new TestEngine({
    agentPath: './dist/index.js',
    outputDir: './test-results',
    reporters: ['console'],
    verbose: true,
    keepTestProjects: false
  });

  try {
    // 2. 创建测试用例 - 手动构建
    const manualTestCase = TestCaseBuilder.create('manual-basic-001')
      .withName('手动创建的基础测试')
      .withDescription('测试 Agent 基本的项目探索能力')
      .withCategory(TestCategory.BASIC_ROBUSTNESS)
      .withTask('Explore the project structure and create a README.md file with project description')
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .expectTool('glob', { required: true, minCalls: 1 })
      .expectTool('read-file', { required: false })
      .expectTool('write-file', { required: true, minCalls: 1 })
      .expectChange('file-created', { path: 'README.md', required: true })
      .withTimeout(120000)
      .build();

    // 3. 运行单个测试
    console.log('\n📋 运行手动创建的测试用例...');
    const singleResult = await testEngine.runTest(manualTestCase);
    console.log(ConsoleReporter.generateTestReport(singleResult));

    // 4. 输出总结
    console.log(`\n🎉 测试完成！`);
    console.log(`📊 得分: ${(singleResult.overallScore * 100).toFixed(1)}%`);

  } catch (error) {
    console.error(`❌ 测试执行失败: ${error}`);
  } finally {
    // 清理资源
    await testEngine.stopAllTests();
  }
}

/**
 * 运行特定类别的测试
 */
export async function runCategoryTests(category: TestCategory): Promise<void> {
  console.log(`🎯 运行 ${category} 类别的测试`);

  const testEngine = new TestEngine({
    agentPath: './dist/index.js',
    outputDir: './test-results',
    reporters: ['console'],
    parallel: true,
    maxConcurrency: 2
  });

  try {
    // 获取该类别的所有模板
    const templates = ScenarioBuilder.filterTemplates({ category });
    console.log(`📋 找到 ${templates.length} 个 ${category} 类别的模板`);

    if (templates.length === 0) {
      console.log(`⚠️  没有找到 ${category} 类别的测试用例`);
      return;
    }

    console.log('✅ 类别测试功能已准备就绪');

  } catch (error) {
    console.error(`❌ 类别测试执行失败: ${error}`);
  } finally {
    await testEngine.stopAllTests();
  }
}

/**
 * 运行性能基准测试
 */
export async function runPerformanceBenchmark(): Promise<void> {
  console.log('⚡ 运行性能基准测试');

  const testEngine = new TestEngine({
    agentPath: './dist/index.js',
    outputDir: './test-results',
    reporters: ['console'],
    parallel: false, // 顺序执行以获得准确的性能数据
    verbose: false
  });

  try {
    // 创建简单的性能测试用例
    const performanceTest = TestCaseBuilder.create('perf-simple')
      .withName('性能测试 - 简单任务')
      .withTask('List all Java files in the project')
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .expectTool('glob', { required: true })
      .withTimeout(30000)
      .build();

    console.log('✅ 性能测试功能已准备就绪');

  } catch (error) {
    console.error(`❌ 性能测试执行失败: ${error}`);
  } finally {
    await testEngine.stopAllTests();
  }
}

// 如果直接运行此文件，执行基础测试
if (require.main === module) {
  runBasicRobustnessTest().catch(console.error);
}

/**
 * JSON 场景测试 - 使用 JSON 配置文件定义测试场景
 *
 * 展示如何使用 JSON 配置文件来定义复杂的测试场景，
 * 特别适合需要多工具调用和详细验证的场景
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import {
  TestEngine,
  ConsoleReporter,
  TestSuiteResult
} from '../framework';
import { JsonScenarioLoader } from '../framework/loaders/JsonScenarioLoader';
import * as path from 'path';

describe('CodingAgent JSON 场景测试', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎
    testEngine = new TestEngine({
      agentPath: './dist/jsMain/typescript/index.js',
      outputDir: './test-results/json-scenarios',
      reporters: ['console'],
      verbose: process.env.DEBUG === 'true',
      keepTestProjects: process.env.KEEP_TEST_PROJECTS === 'true',
      parallel: false
    });
  });

  afterAll(async () => {
    if (testEngine) {
      await testEngine.stopAllTests();
    }
  });

  it('应该成功加载并运行 Spring AI DeepSeek 场景', async () => {
    console.log('\n🚀 测试 Spring AI DeepSeek 集成场景...');

    // 从 JSON 文件加载测试场景
    const scenarioPath = path.join(__dirname, 'scenarios', 'spring-ai-deepseek.json');
    const testCase = await JsonScenarioLoader.loadFromFile(scenarioPath);

    console.log(`📋 加载场景: ${testCase.name}`);
    console.log(`📝 任务: ${testCase.task}`);
    console.log(`🔧 期望工具调用: ${testCase.expectedToolCalls.length} 个`);
    console.log(`📁 期望文件变更: ${testCase.expectedChanges.length} 个`);

    // 运行测试
    const result = await testEngine.runTest(testCase);

    // 验证结果
    expect(result.status).toBe('passed');
    expect(result.overallScore).toBeGreaterThanOrEqual(0.6);

    // 验证工具调用
    expect(result.toolCallAnalysis.uniqueTools).toContain('read-file');
    expect(result.toolCallAnalysis.uniqueTools).toContain('edit-file');
    expect(result.toolCallAnalysis.uniqueTools).toContain('write-file');

    // 验证文件变更
    const buildFileModified = result.fileChanges.some(
      change => change.type === 'modified' && change.path.includes('build.gradle.kts')
    );
    expect(buildFileModified).toBe(true);

    const serviceCreated = result.fileChanges.some(
      change => change.type === 'created' && /DeepSeek.*Service\.java/.test(change.path)
    );
    expect(serviceCreated).toBe(true);

    console.log(`✅ Spring AI DeepSeek 场景测试完成`);
    console.log(`📊 综合得分: ${(result.overallScore * 100).toFixed(1)}%`);
  }, 600000); // 10分钟超时
});


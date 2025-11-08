/**
 * 自定义场景测试 - 使用新测试框架的场景模板
 *
 * 展示如何使用场景模板系统创建可复用的测试场景
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import {
  TestEngine,
  ScenarioBuilder,
  TestCategory,
  ConsoleReporter,
  TestSuiteResult
} from '../framework';

describe('CodingAgent 自定义场景测试 v2', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎
    testEngine = new TestEngine({
      agentPath: './dist/jsMain/typescript/index.js',
      outputDir: './test-results/custom-scenarios',
      reporters: ['console'],
      verbose: process.env.DEBUG === 'true',
      keepTestProjects: process.env.KEEP_TEST_PROJECTS === 'true'
    });

    // 注册自定义场景模板
    registerCustomScenarios();
  });

  afterAll(async () => {
    if (testEngine) {
      await testEngine.stopAllTests();
    }
  });

  it('应该成功运行基础文件操作场景', async () => {
    console.log('\n📁 测试基础文件操作场景模板...');

    // 使用预定义的基础文件操作模板
    const testCases = [
      ScenarioBuilder.generateFromTemplate('basic-file-operations', {
        operation: 'explore'
      }),
      ScenarioBuilder.generateFromTemplate('basic-file-operations', {
        operation: 'read',
        targetFile: 'build.gradle.kts'
      }),
      ScenarioBuilder.generateFromTemplate('basic-file-operations', {
        operation: 'write',
        targetFile: 'CHANGELOG.md'
      })
    ];

    const results = await testEngine.runScenarios(testCases);

    expect(results.totalTests).toBe(3);
    expect(results.passedTests).toBeGreaterThanOrEqual(2); // 至少67%通过率

    console.log(`✅ 基础文件操作场景完成: ${results.passedTests}/${results.totalTests} 通过`);
  }, 300000); // 5分钟超时

  it('应该成功运行业务功能实现场景', async () => {
    console.log('\n🏢 测试业务功能实现场景模板...');

    // 使用预定义的业务功能实现模板
    const testCases = [
      ScenarioBuilder.generateFromTemplate('business-feature-implementation', {
        feature: 'crud',
        entity: 'Product',
        includeTests: true
      }),
      ScenarioBuilder.generateFromTemplate('business-feature-implementation', {
        feature: 'auth',
        includeTests: false
      })
    ];

    const results = await testEngine.runScenarios(testCases);

    expect(results.totalTests).toBe(2);
    expect(results.passedTests).toBeGreaterThanOrEqual(1); // 至少50%通过率（业务场景较复杂）

    console.log(`✅ 业务功能实现场景完成: ${results.passedTests}/${results.totalTests} 通过`);
  }, 600000); // 10分钟超时

  it('应该成功运行错误恢复场景', async () => {
    console.log('\n🔧 测试错误恢复场景模板...');

    // 使用预定义的错误恢复模板
    const testCases = [
      ScenarioBuilder.generateFromTemplate('error-recovery', {
        errorType: 'compilation',
        severity: 'medium'
      }),
      ScenarioBuilder.generateFromTemplate('error-recovery', {
        errorType: 'dependency',
        severity: 'low'
      })
    ];

    const results = await testEngine.runScenarios(testCases);

    expect(results.totalTests).toBe(2);
    // 错误恢复场景通过率可能较低，但至少应该能执行
    expect(results.errorTests).toBeLessThan(results.totalTests); // 不是所有测试都出错

    console.log(`✅ 错误恢复场景完成: ${results.passedTests}/${results.totalTests} 通过`);
  }, 480000); // 8分钟超时

  it('应该成功运行自定义场景模板', async () => {
    console.log('\n🎨 测试自定义场景模板...');

    // 使用自定义注册的场景模板
    const testCases = [
      ScenarioBuilder.generateFromTemplate('microservice-setup', {
        serviceName: 'UserService',
        includeDocker: true,
        includeTests: true
      }),
      ScenarioBuilder.generateFromTemplate('api-documentation', {
        apiType: 'rest',
        includeExamples: true
      })
    ];

    const results = await testEngine.runScenarios(testCases);

    expect(results.totalTests).toBe(2);
    expect(results.passedTests).toBeGreaterThanOrEqual(1); // 至少50%通过率

    console.log(`✅ 自定义场景完成: ${results.passedTests}/${results.totalTests} 通过`);
  }, 720000); // 12分钟超时

  it('应该验证场景模板的可扩展性', async () => {
    // 验证所有注册的模板
    const allTemplates = ScenarioBuilder.getAllTemplates();
    expect(allTemplates.length).toBeGreaterThanOrEqual(5); // 至少5个模板

    // 验证模板分类
    const categories = [...new Set(allTemplates.map(t => t.category))];
    expect(categories.length).toBeGreaterThanOrEqual(3); // 至少3个类别

    // 验证模板难度分布
    const difficulties = [...new Set(allTemplates.map(t => t.difficulty))];
    expect(difficulties.length).toBeGreaterThanOrEqual(2); // 至少2个难度级别

    console.log(`📋 发现 ${allTemplates.length} 个场景模板，涵盖 ${categories.length} 个类别`);
  });
});

/**
 * 注册自定义场景模板
 */
function registerCustomScenarios(): void {
  // 微服务设置场景
    ScenarioBuilder.registerTemplate({
    id: 'microservice-setup',
    name: '微服务项目设置',
    description: '创建一个完整的微服务项目结构',
    category: TestCategory.BUSINESS_SCENARIO,
    difficulty: 'hard',
    estimatedDuration: 360000, // 6分钟
    parameters: {
      serviceName: {
        type: 'string',
        description: '服务名称',
        required: true
      },
      includeDocker: {
        type: 'boolean',
        description: '是否包含 Docker 配置',
        default: false
      },
      includeTests: {
        type: 'boolean',
        description: '是否包含测试代码',
        default: true
      }
    },
    // @ts-ignore
    generate: async (params) => {
      const { TestCaseBuilder, TestCategory, ProjectType } = await import('../framework/index.js');

      return TestCaseBuilder.create(`microservice-${Date.now()}`)
        .withName(`微服务设置: ${params.serviceName}`)
        .withDescription(`创建 ${params.serviceName} 微服务的完整项目结构`)
        .withCategory(TestCategory.BUSINESS_SCENARIO)
        .withTask(`Create a microservice project for ${params.serviceName} with proper structure, configuration, and ${params.includeDocker ? 'Docker support' : 'basic setup'}`)
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('write-file', { required: true, minCalls: params.includeDocker ? 6 : 4 })
        .expectChange('file-created', { required: true })
        .withTimeout(360000)
        .build();
    }
  });

  // API 文档生成场景
  ScenarioBuilder.registerTemplate({
    id: 'api-documentation',
    name: 'API 文档生成',
    description: '为现有 API 生成完整的文档',
    category: TestCategory.BUSINESS_SCENARIO,
    difficulty: 'medium',
    estimatedDuration: 240000, // 4分钟
    parameters: {
      apiType: {
        type: 'string',
        description: 'API 类型: rest, graphql',
        default: 'rest',
        required: true
      },
      includeExamples: {
        type: 'boolean',
        description: '是否包含使用示例',
        default: true
      }
    },
    generate: (params) => {
      const { TestCaseBuilder, TestCategory, ProjectType } = require('../framework');

      return TestCaseBuilder.create(`api-docs-${Date.now()}`)
        .withName(`API 文档生成: ${params.apiType.toUpperCase()}`)
        .withDescription(`为 ${params.apiType} API 生成完整的文档和${params.includeExamples ? '使用示例' : '基础说明'}`)
        .withCategory(TestCategory.BUSINESS_SCENARIO)
        .withTask(`Generate comprehensive API documentation for ${params.apiType} endpoints ${params.includeExamples ? 'with usage examples' : 'with basic descriptions'}`)
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true, minCalls: 2 })
        .expectChange('file-created', { pattern: /README|API|docs/, required: true })
        .withTimeout(240000)
        .build();
    }
  });
}

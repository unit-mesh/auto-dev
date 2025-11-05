/**
 * 业务场景集成测试 - 使用新测试框架
 * 
 * 验证 CodingAgent 在复杂业务场景下的表现
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

describe('CodingAgent 业务场景测试 v2', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎
    testEngine = new TestEngine({
      agentPath: './dist/jsMain/typescript/index.js',
      outputDir: './test-results/business-scenarios',
      reporters: ['console'],
      verbose: process.env.DEBUG === 'true',
      keepTestProjects: process.env.KEEP_TEST_PROJECTS === 'true',
      parallel: false // 业务场景测试顺序执行以避免资源冲突
    });
  });

  afterAll(async () => {
    if (testEngine) {
      await testEngine.stopAllTests();
    }
  });

  it('应该成功运行所有业务场景测试', async () => {
    console.log('\n🏢 开始运行业务场景测试套件...');

    // 定义业务场景测试用例
    const testCases = [
      // 1. 视频支持功能
      TestCaseBuilder.create('business-001')
        .withName('BlogPost 视频支持')
        .withDescription('为 BlogPost 实体添加视频支持功能，包括 URL、标题、描述字段')
        .withCategory(TestCategory.BUSINESS_SCENARIO)
        .withTask('Add video support for BlogPost entity including video URL, title, and description fields. Update the entity, DTO, service, and controller accordingly.')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 3 })
        .expectTool('write-file', { required: true, minCalls: 4 })
        .expectChange('file-modified', { pattern: /BlogPost\.java/, required: true })
        .expectChange('file-modified', { pattern: /BlogPostDto\.java/, required: false })
        .expectChange('file-modified', { pattern: /BlogPostService\.java/, required: false })
        .expectChange('file-modified', { pattern: /BlogPostController\.java/, required: false })
        .withTimeout(300000) // 5分钟
        .build(),

      // 2. JWT 认证系统
      TestCaseBuilder.create('business-002')
        .withName('JWT 认证系统')
        .withDescription('实现完整的 JWT 认证系统，包括用户管理、登录、注册功能')
        .withCategory(TestCategory.BUSINESS_SCENARIO)
        .withTask('Implement JWT authentication system with user registration, login, and secure endpoints. Include User entity, UserService, AuthController, and JWT utilities.')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true, minCalls: 6 })
        .expectChange('file-modified', { path: 'build.gradle.kts', required: true }) // 添加依赖
        .expectChange('file-created', { pattern: /User\.java/, required: true })
        .expectChange('file-created', { pattern: /UserService\.java/, required: true })
        .expectChange('file-created', { pattern: /AuthController\.java/, required: true })
        .expectChange('file-created', { pattern: /JwtUtil\.java/, required: true })
        .withTimeout(600000) // 10分钟
        .build(),

      // 3. GraphQL API 支持
      TestCaseBuilder.create('business-003')
        .withName('GraphQL API 支持')
        .withDescription('为现有的 REST API 添加 GraphQL 支持，包括 Schema 和 Resolver')
        .withCategory(TestCategory.BUSINESS_SCENARIO)
        .withTask('Add GraphQL support to the Spring Boot application. Create GraphQL schema, resolvers for existing entities, and configure GraphQL endpoint.')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 3 })
        .expectTool('write-file', { required: true, minCalls: 5 })
        .expectChange('file-modified', { path: 'build.gradle.kts', required: true })
        .expectChange('file-created', { pattern: /\.graphqls/, required: true })
        .expectChange('file-created', { pattern: /GraphQLResolver\.java/, required: true })
        .withTimeout(450000) // 7.5分钟
        .build(),

      // 4. 数据库迁移和实体关系
      TestCaseBuilder.create('business-004')
        .withName('实体关系和数据库设计')
        .withDescription('设计和实现复杂的实体关系，包括一对多、多对多关系')
        .withCategory(TestCategory.BUSINESS_SCENARIO)
        .withTask('Create a blog system with User, BlogPost, Category, and Tag entities. Implement proper JPA relationships and repository interfaces.')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true, minCalls: 8 })
        .expectChange('file-created', { pattern: /User\.java/, required: true })
        .expectChange('file-created', { pattern: /BlogPost\.java/, required: true })
        .expectChange('file-created', { pattern: /Category\.java/, required: true })
        .expectChange('file-created', { pattern: /Tag\.java/, required: true })
        .expectChange('file-created', { pattern: /Repository\.java/, required: true })
        .withTimeout(480000) // 8分钟
        .build()
    ];

    // 运行测试套件
    testResults = await testEngine.runScenarios(testCases);

    // 生成详细报告
    console.log(ConsoleReporter.generateSuiteReport(testResults));

    // 验证测试结果
    expect(testResults.totalTests).toBe(4);
    expect(testResults.passedTests).toBeGreaterThanOrEqual(3); // 至少75%通过率
    expect(testResults.summary.averageScore).toBeGreaterThanOrEqual(0.6); // 平均得分≥60%

    // 验证执行时间合理性
    expect(testResults.duration).toBeLessThan(1800000); // 总时间<30分钟

    console.log('\n✅ 业务场景测试套件完成');
    console.log(`📊 通过率: ${((testResults.passedTests / testResults.totalTests) * 100).toFixed(1)}%`);
    console.log(`⏱️  总执行时间: ${(testResults.duration / 1000 / 60).toFixed(1)}分钟`);
    console.log(`📈 平均得分: ${(testResults.summary.averageScore * 100).toFixed(1)}%`);
  }, 1800000); // 30分钟超时

  it('应该验证复杂任务的完成度', async () => {
    expect(testResults).toBeDefined();
    
    // 验证任务完成度
    const completionResults = testResults.testResults.map(r => r.taskCompletion);
    const avgCompletionScore = completionResults.reduce(
      (sum, completion) => sum + completion.completionScore, 0
    ) / completionResults.length;

    expect(avgCompletionScore).toBeGreaterThanOrEqual(0.7); // 业务场景完成度≥70%

    // 验证功能实现情况
    const totalImplemented = completionResults.reduce(
      (sum, completion) => sum + completion.functionalityImplemented.length, 0
    );
    const totalMissing = completionResults.reduce(
      (sum, completion) => sum + completion.functionalityMissing.length, 0
    );

    expect(totalImplemented).toBeGreaterThan(totalMissing); // 实现的功能多于缺失的功能
  });

  it('应该验证向后兼容性', async () => {
    expect(testResults).toBeDefined();
    
    // 验证向后兼容性
    const compatibilityResults = testResults.testResults.map(r => r.taskCompletion.backwardCompatibility);
    const compatibleCount = compatibilityResults.filter(compatible => compatible).length;

    expect(compatibleCount / compatibilityResults.length).toBeGreaterThanOrEqual(0.8); // 80%保持向后兼容
  });

  it('应该验证业务场景的工具使用模式', async () => {
    expect(testResults).toBeDefined();
    
    // 验证业务场景特有的工具使用模式
    const toolUsageStats = testResults.summary.toolUsageStats;
    
    // 业务场景应该大量使用文件读写
    expect(toolUsageStats['read-file']).toBeGreaterThanOrEqual(8); // 至少8次文件读取
    expect(toolUsageStats['write-file']).toBeGreaterThanOrEqual(15); // 至少15次文件写入

    // 验证工具调用的复杂性
    const avgToolCalls = testResults.testResults.reduce(
      (sum, result) => sum + result.toolCallAnalysis.totalCalls, 0
    ) / testResults.testResults.length;

    expect(avgToolCalls).toBeGreaterThanOrEqual(10); // 业务场景平均≥10次工具调用
  });

  it('应该验证代码结构的复杂性处理', async () => {
    expect(testResults).toBeDefined();
    
    // 验证文件变更的复杂性
    const totalFileChanges = testResults.testResults.reduce(
      (sum, result) => sum + result.fileChanges.length, 0
    );

    expect(totalFileChanges).toBeGreaterThanOrEqual(12); // 至少12个文件变更

    // 验证创建的文件类型多样性
    const createdFiles = testResults.testResults.flatMap(
      result => result.fileChanges.filter(change => change.type === 'created')
    );

    expect(createdFiles.length).toBeGreaterThanOrEqual(8); // 至少创建8个新文件
  });
});

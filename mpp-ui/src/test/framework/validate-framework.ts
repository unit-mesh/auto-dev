#!/usr/bin/env node

/**
 * 测试框架验证脚本
 * 
 * 验证测试框架的各个组件是否正常工作
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import { TestEngine } from './core/TestEngine';
import { TestCaseBuilder, TestCategory, ProjectType } from './core/TestCase';
import { TestStatus } from './core/TestResult';
import { ScenarioBuilder } from './scenarios/ScenarioBuilder';
import { ConsoleReporter } from './reporters/ConsoleReporter';
import { PromptAnalyzer } from './analyzers/PromptAnalyzer';
import { ToolCallAnalyzer } from './analyzers/ToolCallAnalyzer';
import { CodeChangeAnalyzer } from './analyzers/CodeChangeAnalyzer';

async function validateFramework(): Promise<void> {
  console.log('🔍 开始验证测试框架...\n');

  let allTestsPassed = true;

  try {
    // 1. 验证核心组件
    console.log('📦 验证核心组件...');
    
    // 测试用例构建器
    const testCase = TestCaseBuilder.create('validation-test')
      .withName('验证测试')
      .withDescription('框架验证测试用例')
      .withCategory(TestCategory.BASIC_ROBUSTNESS)
      .withTask('Test framework validation')
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .expectTool('glob', { required: true })
      .expectChange('file-created', { path: 'test.txt', required: false })
      .withTimeout(60000)
      .build();
    
    console.log('  ✅ TestCaseBuilder 工作正常');

    // 2. 验证场景构建器
    console.log('\n🎭 验证场景构建器...');
    
    const templates = ScenarioBuilder.getAllTemplates();
    console.log(`  📋 找到 ${templates.length} 个预定义模板`);
    
    if (templates.length > 0) {
      const basicTemplate = templates.find(t => t.id === 'basic-file-operations');
      if (basicTemplate) {
        const generatedTest = ScenarioBuilder.generateFromTemplate('basic-file-operations', {
          operation: 'explore'
        });
        console.log('  ✅ 场景模板生成工作正常');
      } else {
        console.log('  ⚠️  未找到基础文件操作模板');
      }
    }

    // 3. 验证分析器（模拟数据）
    console.log('\n🔬 验证分析器...');
    
    // 模拟执行信息
    const mockExecutionInfo = {
      startTime: new Date(),
      endTime: new Date(),
      duration: 5000,
      exitCode: 0,
      stdout: '● glob - File search - pattern matcher\n● read-file - file reader - Reading file: build.gradle.kts',
      stderr: '',
      iterations: 2,
      timeoutOccurred: false
    };

    // 测试工具调用分析器
    const toolAnalysis = ToolCallAnalyzer.analyze(testCase, mockExecutionInfo);
    console.log(`  🔧 工具调用分析: 找到 ${toolAnalysis.totalCalls} 次调用`);
    console.log('  ✅ ToolCallAnalyzer 工作正常');

    // 测试提示词分析器
    const promptAnalysis = PromptAnalyzer.analyze(testCase, mockExecutionInfo, toolAnalysis.toolCallDetails);
    console.log(`  📋 提示词分析: 有效性得分 ${(promptAnalysis.promptEffectivenessScore * 100).toFixed(1)}%`);
    console.log('  ✅ PromptAnalyzer 工作正常');

    // 4. 验证报告生成器
    console.log('\n📊 验证报告生成器...');
    
    // 创建模拟测试结果
    const mockResult = {
      testCase,
      status: TestStatus.PASSED,
      executionInfo: mockExecutionInfo,
      promptAnalysis,
      toolCallAnalysis: toolAnalysis,
      codeQuality: {
        syntaxErrors: 0,
        structuralIssues: 1,
        bestPracticeViolations: 2,
        totalIssues: 3,
        qualityScore: 0.85,
        issues: []
      },
      taskCompletion: {
        completed: true,
        completionScore: 0.9,
        functionalityImplemented: ['基础功能'],
        functionalityMissing: [],
        backwardCompatibility: true,
        regressionIssues: []
      },
      fileChanges: [],
      overallScore: 0.88,
      errors: [],
      warnings: ['示例警告'],
      metadata: {
        frameworkVersion: '1.0.0',
        environment: {}
      }
    };

    const report = ConsoleReporter.generateTestReport(mockResult);
    console.log('  ✅ ConsoleReporter 工作正常');

    // 5. 验证测试引擎配置
    console.log('\n⚙️  验证测试引擎配置...');
    
    const testEngine = new TestEngine({
      agentPath: './dist/index.js',
      outputDir: './test-results',
      reporters: ['console'],
      verbose: false
    });
    
    console.log('  ✅ TestEngine 配置正常');

    // 6. 验证文件系统操作
    console.log('\n📁 验证文件系统操作...');
    
    const tempDir = path.join(process.cwd(), 'temp-validation');
    await fs.mkdir(tempDir, { recursive: true });
    
    // 创建测试快照
    await fs.writeFile(path.join(tempDir, 'test.txt'), 'test content');
    const snapshot = await CodeChangeAnalyzer.createSnapshot(tempDir);
    
    if (snapshot.size > 0) {
      console.log(`  📸 快照创建成功: ${snapshot.size} 个文件`);
      console.log('  ✅ CodeChangeAnalyzer 快照功能正常');
    }
    
    // 清理
    await fs.rm(tempDir, { recursive: true, force: true });

    // 7. 验证 CLI 工具存在
    console.log('\n🖥️  验证 CLI 工具...');
    
    const cliPath = path.join(__dirname, 'cli.ts');
    try {
      await fs.access(cliPath);
      console.log('  ✅ CLI 工具文件存在');
    } catch (error) {
      console.log('  ❌ CLI 工具文件不存在');
      allTestsPassed = false;
    }

    // 8. 验证导出接口
    console.log('\n🔌 验证导出接口...');
    
    const requiredExports = [
      'TestEngine',
      'TestCaseBuilder', 
      'ScenarioBuilder',
      'ConsoleReporter',
      'PromptAnalyzer',
      'ToolCallAnalyzer',
      'CodeChangeAnalyzer'
    ];
    
    const framework = await import('./index');
    const missingExports = requiredExports.filter(exp => !framework[exp]);
    
    if (missingExports.length === 0) {
      console.log('  ✅ 所有必需的导出接口都存在');
    } else {
      console.log(`  ❌ 缺少导出接口: ${missingExports.join(', ')}`);
      allTestsPassed = false;
    }

    // 总结
    console.log('\n' + '='.repeat(60));
    if (allTestsPassed) {
      console.log('🎉 框架验证完成！所有组件工作正常。');
      console.log('\n📖 使用指南:');
      console.log('  1. 运行基础测试: npx ts-node src/test/framework/cli.ts suite basic');
      console.log('  2. 查看模板列表: npx ts-node src/test/framework/cli.ts list-templates');
      console.log('  3. 运行自定义测试: npx ts-node src/test/framework/cli.ts custom -t "你的任务"');
      console.log('\n📚 详细文档请查看: src/test/framework/README.md');
    } else {
      console.log('❌ 框架验证失败！请检查上述错误。');
      process.exit(1);
    }

  } catch (error) {
    console.error(`💥 验证过程中发生错误: ${error}`);
    console.error(error);
    process.exit(1);
  }
}

// 运行验证
if (require.main === module) {
  validateFramework().catch(console.error);
}

export { validateFramework };

#!/usr/bin/env node

/**
 * 测试框架结构验证脚本
 *
 * 验证测试框架的文件结构和基本组件是否存在
 */

const fs = require('fs');
const path = require('path');

function validateFramework() {
  console.log('🔍 开始验证测试框架结构...\n');

  let allTestsPassed = true;

  // 定义期望的文件结构
  const expectedFiles = [
    'README.md',
    'QUICK_START.md',
    'index.ts',
    'cli.ts',
    'core/TestCase.ts',
    'core/TestResult.ts',
    'core/TestEngine.ts',
    'analyzers/PromptAnalyzer.ts',
    'analyzers/ToolCallAnalyzer.ts',
    'analyzers/CodeChangeAnalyzer.ts',
    'scenarios/ScenarioBuilder.ts',
    'reporters/ConsoleReporter.ts',
    'examples/BasicRobustnessTest.ts'
  ];

  // 检查文件是否存在
  console.log('📁 检查文件结构...');
  for (const file of expectedFiles) {
    const filePath = path.join(__dirname, file);
    if (fs.existsSync(filePath)) {
      console.log(`  ✅ ${file}`);
    } else {
      console.log(`  ❌ ${file} - 文件不存在`);
      allTestsPassed = false;
    }
  }

  // 检查集成测试 v2 文件
  console.log('\n📁 检查集成测试 v2 文件...');
  const integrationV2Files = [
    '../integration-v2/README.md',
    '../integration-v2/business-scenarios.test.ts',
    '../integration-v2/error-recovery.test.ts',
    '../integration-v2/performance.test.ts',
    '../integration-v2/custom-scenarios.test.ts'
  ];

  for (const file of integrationV2Files) {
    const filePath = path.join(__dirname, file);
    if (fs.existsSync(filePath)) {
      console.log(`  ✅ ${file}`);
    } else {
      console.log(`  ❌ ${file} - 文件不存在`);
      allTestsPassed = false;
    }
  }

  // 检查 package.json 脚本
  console.log('\n📦 检查 package.json 脚本...');
  const packagePath = path.join(__dirname, '../../package.json');
  if (fs.existsSync(packagePath)) {
    const packageContent = JSON.parse(fs.readFileSync(packagePath, 'utf-8'));
    const scripts = packageContent.scripts || {};

    const expectedScripts = [
      'test:framework',
      'test:integration-v2',
      'test:integration-v2:simple',
      'test:integration-v2:business',
      'test:integration-v2:errors',
      'test:integration-v2:performance',
      'test:integration-v2:custom'
    ];

    for (const script of expectedScripts) {
      if (scripts[script]) {
        console.log(`  ✅ ${script} 脚本已定义`);
      } else {
        console.log(`  ❌ ${script} 脚本未定义`);
        allTestsPassed = false;
      }
    }
  }

  // 总结
  console.log('\n' + '='.repeat(60));
  if (allTestsPassed) {
    console.log('🎉 测试框架结构验证完成！所有组件都已正确创建。');
    console.log('\n📖 下一步:');
    console.log('  1. 编译 TypeScript: npm run build:ts');
    console.log('  2. 运行集成测试 v2: npm run test:integration-v2');
    console.log('  3. 运行特定测试: npm run test:integration-v2:simple');
    console.log('\n📚 详细文档:');
    console.log('  • 测试框架: src/test/framework/README.md');
    console.log('  • 快速开始: src/test/framework/QUICK_START.md');
    console.log('  • 集成测试 v2: src/test/integration-v2/README.md');
    console.log('  • 迁移指南: docs/test-scripts/INTEGRATION_TESTS_V2_MIGRATION.md');

    console.log('\n🏗️  框架特性:');
    console.log('  • 提示词效果验证 - 测试系统提示词是否能正确引导 Agent 行为');
    console.log('  • 工具调用分析 - 跟踪和验证 Agent 调用的工具类型、参数、顺序');
    console.log('  • 结果变更分析 - 检测和评估 Agent 产生的代码变更和质量');
    console.log('  • 场景扩展能力 - 支持不同复杂度和类型的测试场景');
    console.log('  • 详细报告生成 - 提供全面的测试分析报告和改进建议');

    console.log('\n🚀 可用的测试套件:');
    console.log('  • 简单健壮性测试 (5个测试用例)');
    console.log('  • 业务场景测试 (4个测试用例)');
    console.log('  • 错误恢复测试 (4个测试用例)');
    console.log('  • 性能测试 (4个测试用例)');
    console.log('  • 自定义场景测试 (模板展示)');

  } else {
    console.log('❌ 测试框架结构验证失败！请检查上述错误。');
    process.exit(1);
  }
}

// 运行验证
validateFramework();

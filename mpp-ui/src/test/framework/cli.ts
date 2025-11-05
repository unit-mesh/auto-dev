#!/usr/bin/env node

/**
 * AI Agent 健壮性测试框架 CLI
 * 
 * 命令行工具，用于运行各种测试场景
 */

import { program } from 'commander';
import * as path from 'path';
import {
  createTestEngine,
  ScenarioBuilder,
  TestCategory,
  ConsoleReporter,
  FRAMEWORK_VERSION
} from './index';

program
  .name('agent-test')
  .description('AI Agent 健壮性测试框架')
  .version(FRAMEWORK_VERSION);

// 运行预定义测试套件
program
  .command('suite <name>')
  .description('运行预定义的测试套件 (basic|business|error-recovery|performance)')
  .option('-a, --agent <path>', 'Agent 可执行文件路径', './dist/index.js')
  .option('-o, --output <dir>', '输出目录', './test-results')
  .option('-v, --verbose', '详细输出', false)
  .option('-p, --parallel', '并行执行', false)
  .option('-k, --keep-projects', '保留测试项目', false)
  .action(async (suiteName, options) => {
    console.log(`🚀 运行测试套件: ${suiteName}`);
    console.log(`⚠️  测试套件功能正在开发中，请使用 scenario 命令运行单个测试场景`);
    process.exit(0);
  });

// 运行单个测试场景
program
  .command('scenario <template>')
  .description('运行指定的测试场景模板')
  .option('-a, --agent <path>', 'Agent 可执行文件路径', './dist/index.js')
  .option('-p, --params <json>', '模板参数 (JSON 格式)', '{}')
  .option('-v, --verbose', '详细输出', false)
  .option('-k, --keep-projects', '保留测试项目', false)
  .action(async (templateId, options) => {
    console.log(`🧪 运行测试场景: ${templateId}`);
    
    try {
      const params = JSON.parse(options.params);
      const testCase = ScenarioBuilder.generateFromTemplate(templateId, params);
      
      const testEngine = createTestEngine({
        agentPath: options.agent,
        verbose: options.verbose,
        keepTestProjects: options.keepProjects
      });
      
      const result = await testEngine.runTest(testCase);
      console.log(ConsoleReporter.generateTestReport(result));
      
      process.exit(result.overallScore >= 0.8 ? 0 : 1);
      
    } catch (error) {
      console.error(`❌ 测试场景执行失败: ${error}`);
      process.exit(1);
    }
  });

// 列出可用的测试模板
program
  .command('list-templates')
  .description('列出所有可用的测试场景模板')
  .option('-c, --category <category>', '按类别筛选')
  .option('-d, --difficulty <level>', '按难度筛选 (easy|medium|hard|expert)')
  .action((options) => {
    console.log('📋 可用的测试场景模板:\n');
    
    const filter: any = {};
    if (options.category) {
      filter.category = options.category as TestCategory;
    }
    if (options.difficulty) {
      filter.difficulty = [options.difficulty];
    }
    
    const templates = ScenarioBuilder.filterTemplates(filter);
    
    templates.forEach(template => {
      console.log(`🔧 ${template.id}`);
      console.log(`   名称: ${template.name}`);
      console.log(`   描述: ${template.description}`);
      console.log(`   类别: ${template.category}`);
      console.log(`   难度: ${template.difficulty}`);
      console.log(`   预估时间: ${template.estimatedDuration}ms`);
      
      if (Object.keys(template.parameters).length > 0) {
        console.log(`   参数:`);
        Object.entries(template.parameters).forEach(([key, param]) => {
          const required = param.required ? ' (必需)' : '';
          const defaultValue = param.default !== undefined ? ` [默认: ${param.default}]` : '';
          console.log(`     • ${key}: ${param.description}${required}${defaultValue}`);
        });
      }
      console.log('');
    });
    
    console.log(`总计: ${templates.length} 个模板`);
  });

// 运行自定义测试
program
  .command('custom')
  .description('运行自定义测试')
  .requiredOption('-t, --task <task>', '测试任务描述')
  .option('-n, --name <name>', '测试名称', '自定义测试')
  .option('-a, --agent <path>', 'Agent 可执行文件路径', './dist/index.js')
  .option('--project-type <type>', '项目类型 (gradle-spring-boot|maven-spring-boot|npm-node|empty)', 'gradle-spring-boot')
  .option('--timeout <ms>', '超时时间 (毫秒)', '300000')
  .option('--expected-tools <tools>', '期望的工具列表 (逗号分隔)', '')
  .option('-v, --verbose', '详细输出', false)
  .option('-k, --keep-projects', '保留测试项目', false)
  .action(async (options) => {
    console.log(`🎯 运行自定义测试: ${options.name}`);
    
    try {
      const { TestCaseBuilder, TestCategory, ProjectType } = await import('./index');
      
      const builder = TestCaseBuilder.create(`custom-${Date.now()}`)
        .withName(options.name)
        .withDescription('自定义测试用例')
        .withCategory(TestCategory.BASIC_ROBUSTNESS)
        .withTask(options.task)
        .withProjectType(options.projectType as any)
        .withTimeout(parseInt(options.timeout));
      
      // 添加期望的工具
      if (options.expectedTools) {
        const tools = options.expectedTools.split(',').map((t: string) => t.trim());
        tools.forEach((tool: string) => {
          builder.expectTool(tool, { required: true });
        });
      }
      
      const testCase = builder.build();
      
      const testEngine = createTestEngine({
        agentPath: options.agent,
        verbose: options.verbose,
        keepTestProjects: options.keepProjects
      });
      
      const result = await testEngine.runTest(testCase);
      console.log(ConsoleReporter.generateTestReport(result));
      
      process.exit(result.overallScore >= 0.8 ? 0 : 1);
      
    } catch (error) {
      console.error(`❌ 自定义测试执行失败: ${error}`);
      process.exit(1);
    }
  });

// 显示框架信息
program
  .command('info')
  .description('显示框架信息')
  .action(() => {
    console.log(`
🤖 AI Agent 健壮性测试框架 v${FRAMEWORK_VERSION}

📋 功能特性:
  • 提示词效果验证 - 测试系统提示词是否能正确引导 Agent 行为
  • 工具调用分析 - 跟踪和验证 Agent 调用的工具类型、参数、顺序
  • 结果变更分析 - 检测和评估 Agent 产生的代码变更和质量
  • 场景扩展能力 - 支持不同复杂度和类型的测试场景
  • 详细报告生成 - 提供全面的测试分析报告和改进建议

🎯 测试类别:
  • basic-robustness - 基础健壮性测试
  • business-scenario - 业务场景测试
  • error-recovery - 错误恢复测试
  • performance - 性能测试
  • boundary-conditions - 边界条件测试

🔧 支持的项目类型:
  • gradle-spring-boot - Gradle Spring Boot 项目
  • maven-spring-boot - Maven Spring Boot 项目
  • npm-node - NPM Node.js 项目
  • empty - 空项目

📖 使用示例:
  agent-test suite basic                    # 运行基础测试套件
  agent-test scenario basic-file-operations # 运行文件操作场景
  agent-test custom -t "Create a REST API" # 运行自定义测试
  agent-test list-templates                 # 列出所有模板
    `);
  });

// 解析命令行参数
program.parse();

// 如果没有提供命令，显示帮助
if (!process.argv.slice(2).length) {
  program.outputHelp();
}

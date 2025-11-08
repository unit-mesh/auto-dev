/**
 * AI Agent 健壮性测试框架 - 主入口文件
 *
 * 导出所有核心组件，提供统一的 API 接口
 */

// 核心组件
import { TestEngine } from './core/TestEngine';
export { TestEngine };
export {
  TestCase,
  TestCaseBuilder,
  TestSuite,
  TestCategory,
  ProjectType,
  ToolCallExpectation,
  ChangeExpectation,
  PromptExpectation,
  FileDefinition
} from './core/TestCase';

export {
  TestResult,
  TestSuiteResult,
  TestStatus,
  ExecutionInfo,
  ToolCallInfo,
  FileChangeInfo,
  PromptAnalysisResult,
  ToolCallAnalysisResult,
  CodeQualityResult,
  TaskCompletionResult,
  TestResultBuilder
} from './core/TestResult';

// 分析器
export { PromptAnalyzer } from './analyzers/PromptAnalyzer';
export { ToolCallAnalyzer } from './analyzers/ToolCallAnalyzer';
export { CodeChangeAnalyzer } from './analyzers/CodeChangeAnalyzer';

// 场景构建器
export { ScenarioBuilder, ScenarioTemplate } from './scenarios/ScenarioBuilder';

// JSON 场景加载器
export { JsonScenarioLoader, JsonScenarioConfig } from './loaders/JsonScenarioLoader';

// 报告生成器
export { ConsoleReporter } from './reporters/ConsoleReporter';

/**
 * 快速创建测试引擎的工厂函数
 */
export function createTestEngine(config: {
  agentPath: string;
  outputDir?: string;
  verbose?: boolean;
  parallel?: boolean;
  keepTestProjects?: boolean;
}): TestEngine {
  return new TestEngine({
    agentPath: config.agentPath,
    outputDir: config.outputDir || './test-results',
    reporters: ['console'],
    verbose: config.verbose || false,
    parallel: config.parallel || false,
    keepTestProjects: config.keepTestProjects || false
  });
}

/**
 * 框架版本信息
 */
export const FRAMEWORK_VERSION = '1.0.0';

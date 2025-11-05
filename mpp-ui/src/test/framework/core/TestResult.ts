/**
 * AI Agent 测试结果数据结构
 *
 * 定义了测试执行结果的完整数据结构，包括执行信息、分析结果、质量指标等
 */

import { TestCase } from './TestCase';

export enum TestStatus {
  PASSED = 'passed',
  FAILED = 'failed',
  TIMEOUT = 'timeout',
  ERROR = 'error',
  SKIPPED = 'skipped'
}

export interface ExecutionInfo {
  startTime: Date;
  endTime: Date;
  duration: number;
  exitCode: number;
  stdout: string;
  stderr: string;
  iterations: number;
  timeoutOccurred: boolean;
}

export interface ToolCallInfo {
  tool: string;
  timestamp: Date;
  parameters?: Record<string, any>;
  success: boolean;
  output?: string;
  error?: string;
  duration?: number;
}

export interface FileChangeInfo {
  type: 'created' | 'modified' | 'deleted';
  path: string;
  sizeBefore?: number;
  sizeAfter?: number;
  contentPreview?: string;
  timestamp: Date;
}

export interface PromptAnalysisResult {
  followedSystemPrompt: boolean;
  exploredProjectFirst: boolean;
  usedAppropriateTools: boolean;
  handledErrorsGracefully: boolean;
  promptEffectivenessScore: number; // 0-1
  issues: string[];
}

export interface ToolCallAnalysisResult {
  totalCalls: number;
  uniqueTools: string[];
  toolAccuracy: number; // 0-1, 期望工具使用的准确率
  sequenceCorrectness: number; // 0-1, 工具调用顺序的正确性
  parameterCorrectness: number; // 0-1, 参数使用的正确性
  unexpectedTools: string[];
  missingTools: string[];
  toolCallDetails: ToolCallInfo[];
}

export interface CodeQualityResult {
  syntaxErrors: number;
  structuralIssues: number;
  bestPracticeViolations: number;
  totalIssues: number;
  qualityScore: number; // 0-1
  issues: {
    type: 'syntax' | 'structure' | 'best-practice';
    severity: 'error' | 'warning' | 'info';
    message: string;
    file?: string;
    line?: number;
  }[];
}

export interface TaskCompletionResult {
  completed: boolean;
  completionScore: number; // 0-1
  functionalityImplemented: string[];
  functionalityMissing: string[];
  backwardCompatibility: boolean;
  regressionIssues: string[];
}

export interface TestResult {
  // 基本信息
  testCase: TestCase;
  status: TestStatus;
  executionInfo: ExecutionInfo;

  // 分析结果
  promptAnalysis: PromptAnalysisResult;
  toolCallAnalysis: ToolCallAnalysisResult;
  codeQuality: CodeQualityResult;
  taskCompletion: TaskCompletionResult;

  // 文件变更
  fileChanges: FileChangeInfo[];

  // 综合评分
  overallScore: number; // 0-1, 综合评分

  // 错误和警告
  errors: string[];
  warnings: string[];

  // 自定义验证结果
  customValidationResults?: {
    name: string;
    passed: boolean;
    message?: string;
  }[];

  // 元数据
  metadata: {
    frameworkVersion: string;
    agentVersion?: string;
    environment: Record<string, string>;
    testProjectPath?: string;
  };
}

export interface TestSuiteResult {
  suiteId: string;
  suiteName: string;
  startTime: Date;
  endTime: Date;
  duration: number;

  // 统计信息
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number;
  errorTests: number;

  // 测试结果
  testResults: TestResult[];

  // 汇总分析
  summary: {
    averageScore: number;
    averageExecutionTime: number;
    mostCommonIssues: string[];
    toolUsageStats: Record<string, number>;
    categoryStats: Record<string, { passed: number; total: number }>;
  };
}

// 结果构建器，用于逐步构建测试结果
export class TestResultBuilder {
  private result: Partial<TestResult> = {
    errors: [],
    warnings: [],
    fileChanges: []
  };

  static create(testCase: TestCase): TestResultBuilder {
    return new TestResultBuilder().withTestCase(testCase);
  }

  withTestCase(testCase: TestCase): TestResultBuilder {
    this.result.testCase = testCase;
    return this;
  }

  withStatus(status: TestStatus): TestResultBuilder {
    this.result.status = status;
    return this;
  }

  withExecutionInfo(executionInfo: ExecutionInfo): TestResultBuilder {
    this.result.executionInfo = executionInfo;
    return this;
  }

  withPromptAnalysis(analysis: PromptAnalysisResult): TestResultBuilder {
    this.result.promptAnalysis = analysis;
    return this;
  }

  withToolCallAnalysis(analysis: ToolCallAnalysisResult): TestResultBuilder {
    this.result.toolCallAnalysis = analysis;
    return this;
  }

  withCodeQuality(quality: CodeQualityResult): TestResultBuilder {
    this.result.codeQuality = quality;
    return this;
  }

  withTaskCompletion(completion: TaskCompletionResult): TestResultBuilder {
    this.result.taskCompletion = completion;
    return this;
  }

  addFileChange(change: FileChangeInfo): TestResultBuilder {
    this.result.fileChanges!.push(change);
    return this;
  }

  addError(error: string): TestResultBuilder {
    this.result.errors!.push(error);
    return this;
  }

  addWarning(warning: string): TestResultBuilder {
    this.result.warnings!.push(warning);
    return this;
  }

  build(): TestResult {
    // 计算综合评分
    const scores = [
      this.result.promptAnalysis?.promptEffectivenessScore || 0,
      this.result.toolCallAnalysis?.toolAccuracy || 0,
      this.result.codeQuality?.qualityScore || 0,
      this.result.taskCompletion?.completionScore || 0
    ];

    this.result.overallScore = scores.reduce((sum, score) => sum + score, 0) / scores.length;

    return this.result as TestResult;
  }
}

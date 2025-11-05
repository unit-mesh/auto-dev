/**
 * AI Agent 测试用例定义
 * 
 * 定义了测试用例的完整结构，包括输入、期望行为、验证规则等
 */

export enum TestCategory {
  BASIC_ROBUSTNESS = 'basic-robustness',
  BUSINESS_SCENARIO = 'business-scenario',
  ERROR_RECOVERY = 'error-recovery',
  PERFORMANCE = 'performance',
  BOUNDARY_CONDITIONS = 'boundary-conditions'
}

export enum ProjectType {
  GRADLE_SPRING_BOOT = 'gradle-spring-boot',
  MAVEN_SPRING_BOOT = 'maven-spring-boot',
  NPM_NODE = 'npm-node',
  EMPTY = 'empty'
}

export interface FileDefinition {
  path: string;
  content: string;
  encoding?: string;
}

export interface PromptExpectation {
  shouldFollowSystemPrompt: boolean;
  shouldExploreProjectFirst?: boolean;
  shouldUseAppropriateTools?: boolean;
  shouldHandleErrorsGracefully?: boolean;
  customValidations?: ((output: string) => boolean)[];
}

export interface ToolCallExpectation {
  tool: string;
  required: boolean;
  minCalls?: number;
  maxCalls?: number;
  pattern?: RegExp;
  parameters?: Record<string, any>;
  order?: number; // 期望的调用顺序
}

export interface ChangeExpectation {
  type: 'file-created' | 'file-modified' | 'file-deleted' | 'dependency-added';
  path?: string;
  pattern?: RegExp;
  content?: string | RegExp;
  required: boolean;
}

export interface TestCase {
  // 基本信息
  id: string;
  name: string;
  description: string;
  category: TestCategory;
  tags?: string[];
  
  // 测试输入
  task: string;
  projectType: ProjectType;
  initialFiles?: FileDefinition[];
  
  // 期望行为验证
  expectedPromptBehavior: PromptExpectation;
  expectedToolCalls: ToolCallExpectation[];
  expectedChanges: ChangeExpectation[];
  
  // 质量标准
  qualityThresholds: {
    minToolAccuracy: number; // 工具使用准确率阈值 (0-1)
    maxExecutionTime: number; // 最大执行时间 (ms)
    minTaskCompletion: number; // 任务完成度阈值 (0-1)
    maxCodeIssues: number; // 最大代码问题数量
  };
  
  // 测试配置
  config: {
    timeout: number;
    maxIterations: number;
    retryCount: number;
    keepTestProject: boolean;
    quiet: boolean;
  };
  
  // 自定义验证函数
  customValidators?: {
    name: string;
    validator: (result: any) => Promise<boolean>;
    description: string;
  }[];
}

export interface TestSuite {
  id: string;
  name: string;
  description: string;
  testCases: TestCase[];
  setup?: () => Promise<void>;
  teardown?: () => Promise<void>;
}

// 测试用例构建器，提供流畅的 API
export class TestCaseBuilder {
  private testCase: Partial<TestCase> = {
    expectedToolCalls: [],
    expectedChanges: [],
    tags: [],
    expectedPromptBehavior: {
      shouldFollowSystemPrompt: true,
      shouldExploreProjectFirst: true,
      shouldUseAppropriateTools: true,
      shouldHandleErrorsGracefully: true
    },
    qualityThresholds: {
      minToolAccuracy: 0.7,
      maxExecutionTime: 300000,
      minTaskCompletion: 0.8,
      maxCodeIssues: 3
    },
    config: {
      timeout: 300000,
      maxIterations: 10,
      retryCount: 1,
      keepTestProject: false,
      quiet: true
    }
  };

  static create(id: string): TestCaseBuilder {
    return new TestCaseBuilder().withId(id);
  }

  withId(id: string): TestCaseBuilder {
    this.testCase.id = id;
    return this;
  }

  withName(name: string): TestCaseBuilder {
    this.testCase.name = name;
    return this;
  }

  withDescription(description: string): TestCaseBuilder {
    this.testCase.description = description;
    return this;
  }

  withCategory(category: TestCategory): TestCaseBuilder {
    this.testCase.category = category;
    return this;
  }

  withTask(task: string): TestCaseBuilder {
    this.testCase.task = task;
    return this;
  }

  withProjectType(projectType: ProjectType): TestCaseBuilder {
    this.testCase.projectType = projectType;
    return this;
  }

  expectTool(tool: string, options: Partial<ToolCallExpectation> = {}): TestCaseBuilder {
    this.testCase.expectedToolCalls!.push({
      tool,
      required: true,
      ...options
    });
    return this;
  }

  expectChange(type: ChangeExpectation['type'], options: Partial<ChangeExpectation> = {}): TestCaseBuilder {
    this.testCase.expectedChanges!.push({
      type,
      required: true,
      ...options
    });
    return this;
  }

  withTimeout(timeout: number): TestCaseBuilder {
    this.testCase.config!.timeout = timeout;
    return this;
  }

  build(): TestCase {
    if (!this.testCase.id || !this.testCase.name || !this.testCase.task) {
      throw new Error('TestCase must have id, name, and task');
    }

    return this.testCase as TestCase;
  }
}

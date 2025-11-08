/**
 * JSON 场景加载器
 * 
 * 支持从 JSON 文件加载测试场景配置，简化复杂测试用例的定义
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import { TestCase, TestCaseBuilder, TestCategory, ProjectType, ToolCallExpectation, ChangeExpectation } from '../core/TestCase';

/**
 * JSON 场景配置格式
 */
export interface JsonScenarioConfig {
  id: string;
  name: string;
  description: string;
  category: 'basic-robustness' | 'business-scenario' | 'error-recovery' | 'performance' | 'boundary-conditions';
  
  // 任务定义
  task: {
    description: string;
    context?: string; // 额外的上下文信息
    documentation?: string[]; // 相关文档链接
  };
  
  // 项目配置
  project: {
    type: 'gradle-spring-boot' | 'maven-spring-boot' | 'npm-node' | 'empty';
    initialFiles?: {
      path: string;
      content: string;
    }[];
  };
  
  // 期望的工具调用
  expectedTools: {
    tool: string;
    required: boolean;
    minCalls?: number;
    maxCalls?: number;
    order?: number;
    parameters?: Record<string, any>;
    description?: string; // 工具调用的说明
  }[];
  
  // 期望的文件变更
  expectedChanges: {
    type: 'file-created' | 'file-modified' | 'file-deleted' | 'dependency-added';
    path?: string;
    pattern?: string; // 正则表达式字符串
    content?: string; // 期望的内容或正则表达式
    required: boolean;
    description?: string; // 变更的说明
  }[];
  
  // 质量阈值
  quality?: {
    minToolAccuracy?: number;
    maxExecutionTime?: number;
    minTaskCompletion?: number;
    maxCodeIssues?: number;
  };
  
  // 测试配置
  config?: {
    timeout?: number;
    maxIterations?: number;
    retryCount?: number;
    keepTestProject?: boolean;
    quiet?: boolean;
  };
  
  // 自定义验证器
  customValidations?: {
    name: string;
    description: string;
    // 验证逻辑将在运行时动态加载
    validatorScript?: string;
  }[];
}

/**
 * JSON 场景加载器
 */
export class JsonScenarioLoader {
  /**
   * 从 JSON 文件加载测试场景
   */
  static async loadFromFile(filePath: string): Promise<TestCase> {
    const content = await fs.readFile(filePath, 'utf-8');
    const config: JsonScenarioConfig = JSON.parse(content);
    return this.buildFromConfig(config);
  }
  
  /**
   * 从 JSON 字符串加载测试场景
   */
  static loadFromString(jsonString: string): TestCase {
    const config: JsonScenarioConfig = JSON.parse(jsonString);
    return this.buildFromConfig(config);
  }
  
  /**
   * 批量加载目录下的所有 JSON 场景
   */
  static async loadFromDirectory(dirPath: string): Promise<TestCase[]> {
    const files = await fs.readdir(dirPath);
    const jsonFiles = files.filter(f => f.endsWith('.json'));
    
    const testCases: TestCase[] = [];
    for (const file of jsonFiles) {
      const filePath = path.join(dirPath, file);
      try {
        const testCase = await this.loadFromFile(filePath);
        testCases.push(testCase);
      } catch (error) {
        console.warn(`Failed to load scenario from ${file}: ${error}`);
      }
    }
    
    return testCases;
  }
  
  /**
   * 从配置对象构建测试用例
   */
  private static buildFromConfig(config: JsonScenarioConfig): TestCase {
    // 构建任务描述
    let taskDescription = config.task.description;
    
    // 添加上下文信息
    if (config.task.context) {
      taskDescription += `\n\nContext: ${config.task.context}`;
    }
    
    // 添加文档引用
    if (config.task.documentation && config.task.documentation.length > 0) {
      taskDescription += `\n\nRelevant documentation:\n${config.task.documentation.map(doc => `- ${doc}`).join('\n')}`;
    }
    
    // 创建测试用例构建器
    const builder = TestCaseBuilder.create(config.id)
      .withName(config.name)
      .withDescription(config.description)
      .withCategory(this.mapCategory(config.category))
      .withTask(taskDescription)
      .withProjectType(this.mapProjectType(config.project.type));
    
    // 添加期望的工具调用
    for (const toolExpectation of config.expectedTools) {
      const toolCallExpectation: Partial<ToolCallExpectation> = {
        required: toolExpectation.required,
        minCalls: toolExpectation.minCalls,
        maxCalls: toolExpectation.maxCalls,
        order: toolExpectation.order,
        parameters: toolExpectation.parameters
      };
      
      builder.expectTool(toolExpectation.tool, toolCallExpectation);
    }
    
    // 添加期望的变更
    for (const changeExpectation of config.expectedChanges) {
      const changeExp: Partial<ChangeExpectation> = {
        path: changeExpectation.path,
        pattern: changeExpectation.pattern ? new RegExp(changeExpectation.pattern) : undefined,
        content: changeExpectation.content,
        required: changeExpectation.required
      };
      
      builder.expectChange(changeExpectation.type, changeExp);
    }
    
    // 设置质量阈值
    if (config.quality) {
      // 注意：TestCaseBuilder 需要扩展以支持这些配置
      // 这里我们通过 build() 后修改对象来实现
    }
    
    // 设置测试配置
    if (config.config?.timeout) {
      builder.withTimeout(config.config.timeout);
    }
    
    const testCase = builder.build();
    
    // 应用质量阈值（如果有）
    if (config.quality) {
      if (config.quality.minToolAccuracy !== undefined) {
        testCase.qualityThresholds.minToolAccuracy = config.quality.minToolAccuracy;
      }
      if (config.quality.maxExecutionTime !== undefined) {
        testCase.qualityThresholds.maxExecutionTime = config.quality.maxExecutionTime;
      }
      if (config.quality.minTaskCompletion !== undefined) {
        testCase.qualityThresholds.minTaskCompletion = config.quality.minTaskCompletion;
      }
      if (config.quality.maxCodeIssues !== undefined) {
        testCase.qualityThresholds.maxCodeIssues = config.quality.maxCodeIssues;
      }
    }
    
    // 应用测试配置（如果有）
    if (config.config) {
      if (config.config.maxIterations !== undefined) {
        testCase.config.maxIterations = config.config.maxIterations;
      }
      if (config.config.retryCount !== undefined) {
        testCase.config.retryCount = config.config.retryCount;
      }
      if (config.config.keepTestProject !== undefined) {
        testCase.config.keepTestProject = config.config.keepTestProject;
      }
      if (config.config.quiet !== undefined) {
        testCase.config.quiet = config.config.quiet;
      }
    }
    
    return testCase;
  }
  
  /**
   * 映射类别
   */
  private static mapCategory(category: string): TestCategory {
    const categoryMap: Record<string, TestCategory> = {
      'basic-robustness': TestCategory.BASIC_ROBUSTNESS,
      'business-scenario': TestCategory.BUSINESS_SCENARIO,
      'error-recovery': TestCategory.ERROR_RECOVERY,
      'performance': TestCategory.PERFORMANCE,
      'boundary-conditions': TestCategory.BOUNDARY_CONDITIONS
    };
    
    return categoryMap[category] || TestCategory.BUSINESS_SCENARIO;
  }
  
  /**
   * 映射项目类型
   */
  private static mapProjectType(type: string): ProjectType {
    const typeMap: Record<string, ProjectType> = {
      'gradle-spring-boot': ProjectType.GRADLE_SPRING_BOOT,
      'maven-spring-boot': ProjectType.MAVEN_SPRING_BOOT,
      'npm-node': ProjectType.NPM_NODE,
      'empty': ProjectType.EMPTY
    };
    
    return typeMap[type] || ProjectType.GRADLE_SPRING_BOOT;
  }
  
  /**
   * 验证 JSON 配置的有效性
   */
  static validateConfig(config: JsonScenarioConfig): { valid: boolean; errors: string[] } {
    const errors: string[] = [];
    
    if (!config.id) errors.push('Missing required field: id');
    if (!config.name) errors.push('Missing required field: name');
    if (!config.description) errors.push('Missing required field: description');
    if (!config.category) errors.push('Missing required field: category');
    if (!config.task?.description) errors.push('Missing required field: task.description');
    if (!config.project?.type) errors.push('Missing required field: project.type');
    
    // 验证工具调用配置
    if (config.expectedTools) {
      config.expectedTools.forEach((tool, index) => {
        if (!tool.tool) errors.push(`expectedTools[${index}]: Missing tool name`);
        if (tool.required === undefined) errors.push(`expectedTools[${index}]: Missing required flag`);
      });
    }
    
    // 验证变更配置
    if (config.expectedChanges) {
      config.expectedChanges.forEach((change, index) => {
        if (!change.type) errors.push(`expectedChanges[${index}]: Missing change type`);
        if (change.required === undefined) errors.push(`expectedChanges[${index}]: Missing required flag`);
      });
    }
    
    return {
      valid: errors.length === 0,
      errors
    };
  }
}


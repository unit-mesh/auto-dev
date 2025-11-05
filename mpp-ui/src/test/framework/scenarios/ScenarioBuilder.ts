/**
 * 测试场景构建器
 * 
 * 提供可扩展的测试场景定义机制，支持不同复杂度和类型的测试用例
 */

import { TestCase, TestCaseBuilder, TestCategory, ProjectType, ToolCallExpectation, ChangeExpectation } from '../core/TestCase';

export interface ScenarioTemplate {
  id: string;
  name: string;
  description: string;
  category: TestCategory;
  difficulty: 'easy' | 'medium' | 'hard' | 'expert';
  estimatedDuration: number; // 预估执行时间（毫秒）
  
  // 模板参数
  parameters: {
    [key: string]: {
      type: 'string' | 'number' | 'boolean' | 'array';
      description: string;
      default?: any;
      required?: boolean;
    };
  };
  
  // 场景生成函数
  generate: (params: Record<string, any>) => TestCase;
}

export class ScenarioBuilder {
  private static templates = new Map<string, ScenarioTemplate>();

  /**
   * 注册场景模板
   */
  static registerTemplate(template: ScenarioTemplate): void {
    this.templates.set(template.id, template);
  }

  /**
   * 获取所有模板
   */
  static getAllTemplates(): ScenarioTemplate[] {
    return Array.from(this.templates.values());
  }

  /**
   * 根据模板生成测试用例
   */
  static generateFromTemplate(templateId: string, params: Record<string, any> = {}): TestCase {
    const template = this.templates.get(templateId);
    if (!template) {
      throw new Error(`未找到模板: ${templateId}`);
    }

    // 验证参数
    this.validateParameters(template, params);
    
    // 合并默认参数
    const mergedParams = this.mergeWithDefaults(template, params);
    
    return template.generate(mergedParams);
  }

  /**
   * 批量生成测试用例
   */
  static generateBatch(requests: { templateId: string; params?: Record<string, any> }[]): TestCase[] {
    return requests.map(req => this.generateFromTemplate(req.templateId, req.params));
  }

  /**
   * 根据条件筛选模板
   */
  static filterTemplates(filter: {
    category?: TestCategory;
    difficulty?: string[];
    maxDuration?: number;
  }): ScenarioTemplate[] {
    return this.getAllTemplates().filter(template => {
      if (filter.category && template.category !== filter.category) return false;
      if (filter.difficulty && !filter.difficulty.includes(template.difficulty)) return false;
      if (filter.maxDuration && template.estimatedDuration > filter.maxDuration) return false;
      return true;
    });
  }

  /**
   * 验证参数
   */
  private static validateParameters(template: ScenarioTemplate, params: Record<string, any>): void {
    for (const [key, paramDef] of Object.entries(template.parameters)) {
      if (paramDef.required && !(key in params)) {
        throw new Error(`缺少必需参数: ${key}`);
      }
      
      if (key in params) {
        const value = params[key];
        const expectedType = paramDef.type;
        
        if (expectedType === 'array' && !Array.isArray(value)) {
          throw new Error(`参数 ${key} 应该是数组类型`);
        } else if (expectedType !== 'array' && typeof value !== expectedType) {
          throw new Error(`参数 ${key} 应该是 ${expectedType} 类型`);
        }
      }
    }
  }

  /**
   * 合并默认参数
   */
  private static mergeWithDefaults(template: ScenarioTemplate, params: Record<string, any>): Record<string, any> {
    const merged = { ...params };
    
    for (const [key, paramDef] of Object.entries(template.parameters)) {
      if (!(key in merged) && paramDef.default !== undefined) {
        merged[key] = paramDef.default;
      }
    }
    
    return merged;
  }
}

// 预定义的场景模板

// 基础文件操作场景
ScenarioBuilder.registerTemplate({
  id: 'basic-file-operations',
  name: '基础文件操作',
  description: '测试基本的文件读写和项目探索能力',
  category: TestCategory.BASIC_ROBUSTNESS,
  difficulty: 'easy',
  estimatedDuration: 60000,
  parameters: {
    operation: {
      type: 'string',
      description: '操作类型: read, write, explore',
      default: 'explore',
      required: true
    },
    targetFile: {
      type: 'string',
      description: '目标文件路径',
      default: 'README.md'
    }
  },
  generate: (params) => {
    const operation = params.operation;
    const targetFile = params.targetFile;
    
    let task: string;
    let expectedTools: ToolCallExpectation[];
    let expectedChanges: ChangeExpectation[];
    
    switch (operation) {
      case 'read':
        task = `Read the ${targetFile} file and summarize its content`;
        expectedTools = [{ tool: 'read-file', required: true }];
        expectedChanges = [];
        break;
      case 'write':
        task = `Create a ${targetFile} file with project description`;
        expectedTools = [{ tool: 'write-file', required: true }];
        expectedChanges = [{ type: 'file-created', path: targetFile, required: true }];
        break;
      case 'explore':
      default:
        task = 'Explore the project structure and list all files';
        expectedTools = [{ tool: 'glob', required: true }];
        expectedChanges = [];
        break;
    }
    
    const builder = TestCaseBuilder.create(`basic-${operation}-${Date.now()}`)
      .withName(`基础${operation}操作`)
      .withDescription(`测试${operation}操作的基本功能`)
      .withCategory(TestCategory.BASIC_ROBUSTNESS)
      .withTask(task)
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .withTimeout(60000);

    // 添加期望的工具调用
    expectedTools.forEach(tool => builder.expectTool(tool.tool, tool));

    // 添加期望的变更
    expectedChanges.forEach(change => builder.expectChange(change.type, change));

    return builder.build();
  }
});

// 业务功能实现场景
ScenarioBuilder.registerTemplate({
  id: 'business-feature-implementation',
  name: '业务功能实现',
  description: '测试复杂业务功能的实现能力',
  category: TestCategory.BUSINESS_SCENARIO,
  difficulty: 'medium',
  estimatedDuration: 300000,
  parameters: {
    feature: {
      type: 'string',
      description: '功能类型: crud, auth, api, integration',
      required: true
    },
    entity: {
      type: 'string',
      description: '实体名称',
      default: 'User'
    },
    includeTests: {
      type: 'boolean',
      description: '是否包含测试',
      default: true
    }
  },
  generate: (params) => {
    const feature = params.feature;
    const entity = params.entity;
    const includeTests = params.includeTests;
    
    let task: string;
    let expectedTools: ToolCallExpectation[];
    let expectedChanges: ChangeExpectation[];
    
    switch (feature) {
      case 'crud':
        task = `Implement CRUD operations for ${entity} entity with REST endpoints`;
        expectedTools = [
          { tool: 'read-file', required: true },
          { tool: 'write-file', required: true, minCalls: 3 }
        ];
        expectedChanges = [
          { type: 'file-created', pattern: new RegExp(`${entity}.*\\.java`), required: true },
          { type: 'file-created', pattern: /Controller\.java/, required: true },
          { type: 'file-created', pattern: /Service\.java/, required: true }
        ];
        break;
      case 'auth':
        task = `Implement JWT authentication system`;
        expectedTools = [
          { tool: 'read-file', required: true },
          { tool: 'write-file', required: true, minCalls: 4 }
        ];
        expectedChanges = [
          { type: 'file-modified', path: 'build.gradle.kts', required: true },
          { type: 'file-created', pattern: /Security.*\.java/, required: true }
        ];
        break;
      default:
        task = `Implement ${feature} functionality for ${entity}`;
        expectedTools = [
          { tool: 'read-file', required: true },
          { tool: 'write-file', required: true }
        ];
        expectedChanges = [
          { type: 'file-created', required: true }
        ];
    }
    
    if (includeTests) {
      expectedChanges.push({
        type: 'file-created',
        pattern: /Test\.java$/,
        required: false
      });
    }
    
    const builder = TestCaseBuilder.create(`business-${feature}-${Date.now()}`)
      .withName(`${feature}功能实现`)
      .withDescription(`实现${entity}的${feature}功能`)
      .withCategory(TestCategory.BUSINESS_SCENARIO)
      .withTask(task)
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .withTimeout(300000);

    // 添加期望的工具调用
    expectedTools.forEach(tool => builder.expectTool(tool.tool, tool));

    // 添加期望的变更
    expectedChanges.forEach(change => builder.expectChange(change.type, change));

    return builder.build();
  }
});

// 错误恢复场景
ScenarioBuilder.registerTemplate({
  id: 'error-recovery',
  name: '错误恢复测试',
  description: '测试错误处理和恢复能力',
  category: TestCategory.ERROR_RECOVERY,
  difficulty: 'hard',
  estimatedDuration: 180000,
  parameters: {
    errorType: {
      type: 'string',
      description: '错误类型: compilation, dependency, syntax',
      required: true
    },
    severity: {
      type: 'string',
      description: '错误严重程度: low, medium, high',
      default: 'medium'
    }
  },
  generate: (params) => {
    const errorType = params.errorType;
    const severity = params.severity;
    
    let task: string;
    
    switch (errorType) {
      case 'compilation':
        task = 'Fix the compilation errors in the project and ensure it builds successfully';
        break;
      case 'dependency':
        task = 'Resolve dependency conflicts and update to compatible versions';
        break;
      case 'syntax':
        task = 'Fix syntax errors in the Java files and ensure code quality';
        break;
      default:
        task = `Fix ${errorType} errors in the project`;
    }
    
    return TestCaseBuilder.create(`error-${errorType}-${Date.now()}`)
      .withName(`${errorType}错误恢复`)
      .withDescription(`测试${errorType}错误的处理和恢复`)
      .withCategory(TestCategory.ERROR_RECOVERY)
      .withTask(task)
      .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
      .expectTool('shell', { required: true }) // 需要执行构建命令
      .expectTool('read-file', { required: true })
      .expectTool('write-file', { required: true })
      .withTimeout(180000)
      .build();
  }
});

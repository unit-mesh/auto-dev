#!/usr/bin/env node

/**
 * 测试场景生成器
 * 
 * 使用 AI 根据需求描述自动生成 JSON 测试场景配置
 * 
 * 使用方法:
 *   npm run generate:scenario -- "Add Spring AI with DeepSeek to project"
 *   npm run generate:scenario -- --interactive
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import readline from 'readline';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 场景模板
const SCENARIO_TEMPLATE = {
  id: '',
  name: '',
  description: '',
  category: 'business-scenario',
  task: {
    description: '',
    context: '',
    documentation: []
  },
  project: {
    type: 'gradle-spring-boot'
  },
  expectedTools: [],
  expectedChanges: [],
  quality: {
    minToolAccuracy: 0.7,
    maxExecutionTime: 600000,
    minTaskCompletion: 0.8,
    maxCodeIssues: 3
  },
  config: {
    timeout: 600000,
    maxIterations: 15,
    retryCount: 1,
    keepTestProject: false,
    quiet: false
  }
};

// 常见工具配置模板
const TOOL_TEMPLATES = {
  'read-file': {
    tool: 'read-file',
    required: true,
    minCalls: 1,
    description: 'Read existing files to understand project structure'
  },
  'write-file': {
    tool: 'write-file',
    required: true,
    minCalls: 1,
    description: 'Create new files'
  },
  'edit-file': {
    tool: 'edit-file',
    required: false,
    minCalls: 1,
    description: 'Edit existing files'
  },
  'shell': {
    tool: 'shell',
    required: false,
    minCalls: 1,
    description: 'Run build or verification commands'
  },
  'glob': {
    tool: 'glob',
    required: false,
    minCalls: 1,
    description: 'Explore project structure'
  },
  'web-fetch': {
    tool: 'web-fetch',
    required: false,
    minCalls: 1,
    description: 'Fetch documentation or external resources'
  }
};

// AI 提示词模板
const AI_PROMPT_TEMPLATE = `You are a test scenario generator for an AI coding agent testing framework.

Given a user requirement, generate a comprehensive JSON test scenario configuration.

User Requirement:
{requirement}

Generate a JSON configuration that includes:
1. A unique ID (format: category-keyword-001)
2. A clear name and description
3. Appropriate category (basic-robustness, business-scenario, error-recovery, performance, boundary-conditions)
4. Detailed task description with context
5. Expected tool calls (read-file, write-file, edit-file, shell, glob, web-fetch, grep)
6. Expected file changes (file-created, file-modified, file-deleted, dependency-added)
7. Quality thresholds
8. Test configuration

Consider:
- What tools the agent needs to use (in order)
- What files will be created or modified
- What dependencies might be added
- What documentation might be helpful
- Appropriate timeouts and quality thresholds

Output ONLY valid JSON matching this schema:
{schema}

JSON:`;

/**
 * 创建 readline 接口
 */
function createInterface() {
  return readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });
}

/**
 * 提问函数
 */
function question(rl, query) {
  return new Promise(resolve => rl.question(query, resolve));
}

/**
 * 交互式收集场景信息
 */
async function collectScenarioInfo() {
  const rl = createInterface();
  
  console.log('\n🎯 测试场景生成器 - 交互模式\n');
  
  const requirement = await question(rl, '📝 请描述你的需求（例如：Add Spring AI with DeepSeek to project）:\n> ');
  const category = await question(rl, '\n📂 选择类别 (basic-robustness/business-scenario/error-recovery/performance) [business-scenario]:\n> ') || 'business-scenario';
  const projectType = await question(rl, '\n🏗️  项目类型 (gradle-spring-boot/maven-spring-boot/npm-node) [gradle-spring-boot]:\n> ') || 'gradle-spring-boot';
  const documentation = await question(rl, '\n📚 相关文档链接（多个用逗号分隔，可选）:\n> ');
  const timeout = await question(rl, '\n⏱️  超时时间（毫秒）[600000]:\n> ') || '600000';
  
  rl.close();
  
  return {
    requirement,
    category,
    projectType,
    documentation: documentation ? documentation.split(',').map(d => d.trim()) : [],
    timeout: parseInt(timeout)
  };
}

/**
 * 基于规则生成场景配置
 */
function generateScenarioByRules(info) {
  const { requirement, category, projectType, documentation, timeout } = info;
  
  // 生成 ID
  const keyword = requirement.toLowerCase()
    .replace(/[^a-z0-9\s]/g, '')
    .split(/\s+/)
    .slice(0, 3)
    .join('-');
  const id = `${category}-${keyword}-001`;
  
  // 分析需求，推断需要的工具
  const tools = [];
  const changes = [];
  
  // 基本工具：总是需要读取文件
  tools.push({ ...TOOL_TEMPLATES['read-file'], order: 1 });
  
  // 如果提到 "add", "create", "implement"，需要写文件
  if (/add|create|implement|build/i.test(requirement)) {
    tools.push({ ...TOOL_TEMPLATES['write-file'], minCalls: 2, order: 3 });
  }
  
  // 如果提到 "update", "modify", "change"，需要编辑文件
  if (/update|modify|change|edit/i.test(requirement)) {
    tools.push({ ...TOOL_TEMPLATES['edit-file'], required: true, order: 2 });
  }
  
  // 如果提到 "build", "test", "run"，需要 shell
  if (/build|test|run|compile|verify/i.test(requirement)) {
    tools.push({ ...TOOL_TEMPLATES['shell'], required: true, order: 5 });
  }
  
  // 如果有文档链接，可能需要 web-fetch
  if (documentation.length > 0) {
    tools.push({ ...TOOL_TEMPLATES['web-fetch'], order: 2 });
  }
  
  // 如果提到探索、查找，需要 glob
  if (/explore|find|search|locate/i.test(requirement)) {
    tools.push({ ...TOOL_TEMPLATES['glob'], required: true, order: 1 });
  }
  
  // 推断文件变更
  if (/dependency|dependencies|gradle|maven|pom\.xml|build\.gradle/i.test(requirement)) {
    changes.push({
      type: 'file-modified',
      path: projectType.includes('gradle') ? 'build.gradle.kts' : 'pom.xml',
      required: true,
      description: 'Build file should be modified to add dependencies'
    });
  }
  
  // 如果提到服务、控制器等，推断需要创建的文件
  if (/service/i.test(requirement)) {
    changes.push({
      type: 'file-created',
      pattern: '.*Service\\.java',
      required: true,
      description: 'Service class should be created'
    });
  }
  
  if (/controller|api|endpoint/i.test(requirement)) {
    changes.push({
      type: 'file-created',
      pattern: '.*Controller\\.java',
      required: false,
      description: 'Controller class should be created'
    });
  }
  
  if (/entity|model|domain/i.test(requirement)) {
    changes.push({
      type: 'file-created',
      pattern: '.*\\.java',
      required: true,
      description: 'Entity/Model class should be created'
    });
  }
  
  if (/config|configuration/i.test(requirement)) {
    changes.push({
      type: 'file-created',
      pattern: '.*Config\\.java',
      required: false,
      description: 'Configuration class should be created'
    });
  }
  
  // 构建场景配置
  const scenario = {
    ...SCENARIO_TEMPLATE,
    id,
    name: requirement,
    description: `Test scenario for: ${requirement}`,
    category,
    task: {
      description: requirement,
      context: `This is a ${projectType} project. ${requirement}`,
      documentation
    },
    project: {
      type: projectType
    },
    expectedTools: tools,
    expectedChanges: changes,
    config: {
      ...SCENARIO_TEMPLATE.config,
      timeout
    }
  };
  
  return scenario;
}

/**
 * 使用 AI 生成场景配置（如果可用）
 */
async function generateScenarioByAI(info) {
  // 这里可以集成 LLM API 来生成更智能的配置
  // 目前先使用基于规则的生成
  console.log('\n💡 提示：AI 生成功能需要配置 LLM API，当前使用基于规则的生成\n');
  return generateScenarioByRules(info);
}

/**
 * 保存场景配置
 */
async function saveScenario(scenario, outputDir) {
  const filename = `${scenario.id}.json`;
  const filepath = path.join(outputDir, filename);
  
  await fs.mkdir(outputDir, { recursive: true });
  await fs.writeFile(filepath, JSON.stringify(scenario, null, 2), 'utf-8');
  
  return filepath;
}

/**
 * 主函数
 */
async function main() {
  const args = process.argv.slice(2);
  
  let info;
  
  if (args.includes('--interactive') || args.includes('-i')) {
    // 交互模式
    info = await collectScenarioInfo();
  } else if (args.length > 0) {
    // 命令行参数模式
    const requirement = args.join(' ');
    info = {
      requirement,
      category: 'business-scenario',
      projectType: 'gradle-spring-boot',
      documentation: [],
      timeout: 600000
    };
  } else {
    // 显示帮助
    console.log(`
🎯 测试场景生成器

使用方法:
  npm run generate:scenario -- "需求描述"
  npm run generate:scenario -- --interactive

示例:
  npm run generate:scenario -- "Add Spring AI with DeepSeek to project"
  npm run generate:scenario -- "Implement User CRUD with REST API"
  npm run generate:scenario -- -i

选项:
  -i, --interactive    交互式模式
  -h, --help          显示帮助信息
    `);
    process.exit(0);
  }
  
  console.log('\n🔧 生成测试场景配置...\n');
  
  // 生成场景配置
  const scenario = await generateScenarioByAI(info);
  
  // 保存到文件
  const outputDir = path.join(__dirname, '..', 'src', 'test', 'integration-v2', 'scenarios');
  const filepath = await saveScenario(scenario, outputDir);
  
  console.log('✅ 场景配置已生成！\n');
  console.log(`📁 文件路径: ${filepath}`);
  console.log(`🆔 场景 ID: ${scenario.id}`);
  console.log(`📝 场景名称: ${scenario.name}`);
  console.log(`🔧 期望工具: ${scenario.expectedTools.map(t => t.tool).join(', ')}`);
  console.log(`📄 期望变更: ${scenario.expectedChanges.length} 个`);
  console.log('\n预览:\n');
  console.log(JSON.stringify(scenario, null, 2));
  console.log('\n💡 提示: 你可以手动编辑生成的 JSON 文件来调整配置');
}

main().catch(error => {
  console.error('❌ 错误:', error.message);
  process.exit(1);
});


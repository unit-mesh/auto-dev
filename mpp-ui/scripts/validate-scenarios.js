#!/usr/bin/env node

/**
 * 验证所有 JSON 测试场景配置
 * 
 * 检查：
 * 1. JSON 格式是否正确
 * 2. 必需字段是否存在
 * 3. 字段值是否有效
 * 4. 正则表达式是否有效
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SCENARIOS_DIR = path.join(__dirname, '..', 'src', 'test', 'integration-v2', 'scenarios');

const VALID_CATEGORIES = [
  'basic-robustness',
  'business-scenario',
  'error-recovery',
  'performance',
  'boundary-conditions'
];

const VALID_PROJECT_TYPES = [
  'gradle-spring-boot',
  'maven-spring-boot',
  'npm-node',
  'empty'
];

const VALID_TOOLS = [
  'read-file',
  'write-file',
  'edit-file',
  'shell',
  'glob',
  'grep',
  'web-fetch'
];

const VALID_CHANGE_TYPES = [
  'file-created',
  'file-modified',
  'file-deleted',
  'dependency-added'
];

/**
 * 验证单个场景配置
 */
function validateScenario(config, filename) {
  const errors = [];
  const warnings = [];
  
  // 必需字段检查
  if (!config.id) errors.push('Missing required field: id');
  if (!config.name) errors.push('Missing required field: name');
  if (!config.description) errors.push('Missing required field: description');
  if (!config.category) errors.push('Missing required field: category');
  if (!config.task?.description) errors.push('Missing required field: task.description');
  if (!config.project?.type) errors.push('Missing required field: project.type');
  
  // 类别验证
  if (config.category && !VALID_CATEGORIES.includes(config.category)) {
    errors.push(`Invalid category: ${config.category}. Must be one of: ${VALID_CATEGORIES.join(', ')}`);
  }
  
  // 项目类型验证
  if (config.project?.type && !VALID_PROJECT_TYPES.includes(config.project.type)) {
    errors.push(`Invalid project type: ${config.project.type}. Must be one of: ${VALID_PROJECT_TYPES.join(', ')}`);
  }
  
  // ID 格式验证
  if (config.id && !/^[a-z0-9-]+$/.test(config.id)) {
    warnings.push(`ID should only contain lowercase letters, numbers, and hyphens: ${config.id}`);
  }
  
  // 工具调用验证
  if (config.expectedTools) {
    if (!Array.isArray(config.expectedTools)) {
      errors.push('expectedTools must be an array');
    } else {
      config.expectedTools.forEach((tool, index) => {
        if (!tool.tool) {
          errors.push(`expectedTools[${index}]: Missing tool name`);
        } else if (!VALID_TOOLS.includes(tool.tool)) {
          errors.push(`expectedTools[${index}]: Invalid tool name: ${tool.tool}. Must be one of: ${VALID_TOOLS.join(', ')}`);
        }
        
        if (tool.required === undefined) {
          errors.push(`expectedTools[${index}]: Missing required flag`);
        }
        
        if (tool.minCalls !== undefined && typeof tool.minCalls !== 'number') {
          errors.push(`expectedTools[${index}]: minCalls must be a number`);
        }
        
        if (tool.maxCalls !== undefined && typeof tool.maxCalls !== 'number') {
          errors.push(`expectedTools[${index}]: maxCalls must be a number`);
        }
        
        if (tool.minCalls !== undefined && tool.maxCalls !== undefined && tool.minCalls > tool.maxCalls) {
          errors.push(`expectedTools[${index}]: minCalls (${tool.minCalls}) cannot be greater than maxCalls (${tool.maxCalls})`);
        }
      });
    }
  } else {
    warnings.push('No expectedTools defined');
  }
  
  // 文件变更验证
  if (config.expectedChanges) {
    if (!Array.isArray(config.expectedChanges)) {
      errors.push('expectedChanges must be an array');
    } else {
      config.expectedChanges.forEach((change, index) => {
        if (!change.type) {
          errors.push(`expectedChanges[${index}]: Missing change type`);
        } else if (!VALID_CHANGE_TYPES.includes(change.type)) {
          errors.push(`expectedChanges[${index}]: Invalid change type: ${change.type}. Must be one of: ${VALID_CHANGE_TYPES.join(', ')}`);
        }
        
        if (change.required === undefined) {
          errors.push(`expectedChanges[${index}]: Missing required flag`);
        }
        
        // 验证正则表达式
        if (change.pattern) {
          try {
            new RegExp(change.pattern);
          } catch (e) {
            errors.push(`expectedChanges[${index}]: Invalid regex pattern: ${change.pattern} - ${e.message}`);
          }
        }
        
        // 至少需要 path 或 pattern
        if (!change.path && !change.pattern) {
          warnings.push(`expectedChanges[${index}]: Neither path nor pattern specified`);
        }
      });
    }
  } else {
    warnings.push('No expectedChanges defined');
  }
  
  // 质量阈值验证
  if (config.quality) {
    if (config.quality.minToolAccuracy !== undefined) {
      if (typeof config.quality.minToolAccuracy !== 'number' || 
          config.quality.minToolAccuracy < 0 || 
          config.quality.minToolAccuracy > 1) {
        errors.push('quality.minToolAccuracy must be a number between 0 and 1');
      }
    }
    
    if (config.quality.minTaskCompletion !== undefined) {
      if (typeof config.quality.minTaskCompletion !== 'number' || 
          config.quality.minTaskCompletion < 0 || 
          config.quality.minTaskCompletion > 1) {
        errors.push('quality.minTaskCompletion must be a number between 0 and 1');
      }
    }
    
    if (config.quality.maxExecutionTime !== undefined) {
      if (typeof config.quality.maxExecutionTime !== 'number' || config.quality.maxExecutionTime <= 0) {
        errors.push('quality.maxExecutionTime must be a positive number');
      }
    }
  }
  
  // 配置验证
  if (config.config) {
    if (config.config.timeout !== undefined) {
      if (typeof config.config.timeout !== 'number' || config.config.timeout <= 0) {
        errors.push('config.timeout must be a positive number');
      }
    }
    
    if (config.config.maxIterations !== undefined) {
      if (typeof config.config.maxIterations !== 'number' || config.config.maxIterations <= 0) {
        errors.push('config.maxIterations must be a positive number');
      }
    }
  }
  
  return { errors, warnings };
}

/**
 * 主函数
 */
async function main() {
  console.log('🔍 验证 JSON 测试场景配置...\n');
  
  let files;
  try {
    files = await fs.readdir(SCENARIOS_DIR);
  } catch (error) {
    console.error(`❌ 无法读取场景目录: ${SCENARIOS_DIR}`);
    console.error(error.message);
    process.exit(1);
  }
  
  const jsonFiles = files.filter(f => f.endsWith('.json'));
  
  if (jsonFiles.length === 0) {
    console.log('⚠️  未找到 JSON 场景文件');
    process.exit(0);
  }
  
  console.log(`📋 找到 ${jsonFiles.length} 个场景文件\n`);
  
  let totalErrors = 0;
  let totalWarnings = 0;
  const results = [];
  
  for (const file of jsonFiles) {
    const filePath = path.join(SCENARIOS_DIR, file);
    
    try {
      const content = await fs.readFile(filePath, 'utf-8');
      const config = JSON.parse(content);
      
      const { errors, warnings } = validateScenario(config, file);
      
      results.push({
        file,
        id: config.id,
        name: config.name,
        errors,
        warnings,
        valid: errors.length === 0
      });
      
      totalErrors += errors.length;
      totalWarnings += warnings.length;
      
      if (errors.length === 0 && warnings.length === 0) {
        console.log(`✅ ${file}`);
        console.log(`   ID: ${config.id}`);
        console.log(`   Name: ${config.name}`);
      } else {
        console.log(`${errors.length > 0 ? '❌' : '⚠️ '} ${file}`);
        console.log(`   ID: ${config.id || 'N/A'}`);
        console.log(`   Name: ${config.name || 'N/A'}`);
        
        if (errors.length > 0) {
          console.log(`   Errors (${errors.length}):`);
          errors.forEach(err => console.log(`     - ${err}`));
        }
        
        if (warnings.length > 0) {
          console.log(`   Warnings (${warnings.length}):`);
          warnings.forEach(warn => console.log(`     - ${warn}`));
        }
      }
      console.log();
      
    } catch (error) {
      console.log(`❌ ${file}`);
      console.log(`   Error: ${error.message}\n`);
      totalErrors++;
      results.push({
        file,
        errors: [error.message],
        warnings: [],
        valid: false
      });
    }
  }
  
  // 总结
  console.log('━'.repeat(60));
  console.log('\n📊 验证总结\n');
  console.log(`总场景数: ${jsonFiles.length}`);
  console.log(`✅ 有效: ${results.filter(r => r.valid).length}`);
  console.log(`❌ 错误: ${results.filter(r => !r.valid).length}`);
  console.log(`⚠️  警告: ${totalWarnings}`);
  console.log();
  
  if (totalErrors > 0) {
    console.log('❌ 验证失败！请修复上述错误。');
    process.exit(1);
  } else if (totalWarnings > 0) {
    console.log('⚠️  验证通过，但有警告。建议修复警告以提高质量。');
    process.exit(0);
  } else {
    console.log('✅ 所有场景配置都有效！');
    process.exit(0);
  }
}

main().catch(error => {
  console.error('❌ 验证过程出错:', error.message);
  process.exit(1);
});


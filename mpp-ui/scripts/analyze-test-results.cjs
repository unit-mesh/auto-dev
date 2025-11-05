#!/usr/bin/env node

/**
 * 测试结果分析脚本
 * 
 * 分析新测试框架 v2 的测试结果，检查通过率阈值，生成详细报告
 */

const fs = require('fs');
const path = require('path');

function analyzeTestResults() {
  console.log('📊 分析测试结果...\n');

  const testResultsDir = path.join(__dirname, '../test-results');
  const reportsDir = path.join(testResultsDir, 'reports');
  
  // 确保报告目录存在
  if (!fs.existsSync(reportsDir)) {
    fs.mkdirSync(reportsDir, { recursive: true });
  }

  const threshold = parseInt(process.env.PASS_THRESHOLD || '80');
  const testCategory = process.env.TEST_CATEGORY || 'unknown';
  
  console.log(`🎯 配置信息:`);
  console.log(`  - 测试类别: ${testCategory}`);
  console.log(`  - 通过率阈值: ${threshold}%`);
  console.log(`  - 测试结果目录: ${testResultsDir}`);
  console.log('');

  // 分析结果
  const analysis = {
    timestamp: new Date().toISOString(),
    testCategory,
    threshold,
    totalTests: 0,
    passedTests: 0,
    failedTests: 0,
    errorTests: 0,
    skippedTests: 0,
    passRate: 0,
    thresholdMet: false,
    averageScore: 0,
    averageExecutionTime: 0,
    details: []
  };

  try {
    // 查找测试结果文件
    const resultFiles = findTestResultFiles(testResultsDir);
    console.log(`📁 找到 ${resultFiles.length} 个测试结果文件`);

    if (resultFiles.length === 0) {
      console.log('⚠️  没有找到测试结果文件，可能测试未正常执行');
      analysis.details.push('没有找到测试结果文件');
    } else {
      // 解析每个结果文件
      for (const file of resultFiles) {
        console.log(`📄 分析文件: ${path.basename(file)}`);
        try {
          const content = fs.readFileSync(file, 'utf-8');
          parseTestResult(content, analysis);
        } catch (error) {
          console.log(`❌ 解析文件失败: ${error.message}`);
          analysis.details.push(`解析文件失败: ${path.basename(file)} - ${error.message}`);
        }
      }
    }

    // 计算统计信息
    if (analysis.totalTests > 0) {
      analysis.passRate = (analysis.passedTests / analysis.totalTests) * 100;
      analysis.thresholdMet = analysis.passRate >= threshold;
    }

    // 生成报告
    generateReport(analysis, reportsDir);
    
    // 输出结果
    console.log('\n📈 测试结果分析:');
    console.log(`  - 总测试数: ${analysis.totalTests}`);
    console.log(`  - 通过: ${analysis.passedTests}`);
    console.log(`  - 失败: ${analysis.failedTests}`);
    console.log(`  - 错误: ${analysis.errorTests}`);
    console.log(`  - 跳过: ${analysis.skippedTests}`);
    console.log(`  - 通过率: ${analysis.passRate.toFixed(1)}%`);
    console.log(`  - 阈值要求: ${threshold}%`);
    console.log(`  - 阈值达标: ${analysis.thresholdMet ? '✅ 是' : '❌ 否'}`);

    if (analysis.averageScore > 0) {
      console.log(`  - 平均得分: ${(analysis.averageScore * 100).toFixed(1)}%`);
    }
    if (analysis.averageExecutionTime > 0) {
      console.log(`  - 平均执行时间: ${(analysis.averageExecutionTime / 1000).toFixed(1)}秒`);
    }

    console.log('\n📋 详细信息:');
    analysis.details.forEach(detail => {
      console.log(`  • ${detail}`);
    });

    // 根据阈值决定退出码
    if (!analysis.thresholdMet && analysis.totalTests > 0) {
      console.log(`\n❌ 测试通过率 ${analysis.passRate.toFixed(1)}% 低于阈值 ${threshold}%`);
      process.exit(1);
    } else if (analysis.totalTests === 0) {
      console.log('\n⚠️  没有找到有效的测试结果');
      process.exit(1);
    } else {
      console.log(`\n✅ 测试通过率 ${analysis.passRate.toFixed(1)}% 达到阈值要求`);
      process.exit(0);
    }

  } catch (error) {
    console.error(`💥 分析过程中发生错误: ${error.message}`);
    analysis.details.push(`分析错误: ${error.message}`);
    generateReport(analysis, reportsDir);
    process.exit(1);
  }
}

function findTestResultFiles(dir) {
  const files = [];
  
  if (!fs.existsSync(dir)) {
    return files;
  }

  function walkDir(currentDir) {
    const entries = fs.readdirSync(currentDir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(currentDir, entry.name);
      
      if (entry.isDirectory()) {
        walkDir(fullPath);
      } else if (entry.isFile()) {
        // 查找可能的测试结果文件
        if (entry.name.includes('test') && 
            (entry.name.endsWith('.json') || 
             entry.name.endsWith('.xml') || 
             entry.name.endsWith('.log'))) {
          files.push(fullPath);
        }
      }
    }
  }

  walkDir(dir);
  return files;
}

function parseTestResult(content, analysis) {
  // 尝试解析不同格式的测试结果
  
  // 1. 尝试解析 JSON 格式
  try {
    const json = JSON.parse(content);
    if (json.totalTests !== undefined) {
      analysis.totalTests += json.totalTests || 0;
      analysis.passedTests += json.passedTests || 0;
      analysis.failedTests += json.failedTests || 0;
      analysis.errorTests += json.errorTests || 0;
      analysis.skippedTests += json.skippedTests || 0;
      
      if (json.summary && json.summary.averageScore) {
        analysis.averageScore = json.summary.averageScore;
      }
      if (json.duration) {
        analysis.averageExecutionTime = json.duration;
      }
      
      analysis.details.push(`JSON结果: ${json.totalTests}个测试，${json.passedTests}个通过`);
      return;
    }
  } catch (e) {
    // 不是 JSON 格式，继续尝试其他格式
  }

  // 2. 尝试解析文本格式的测试输出
  const lines = content.split('\n');
  let foundResults = false;

  for (const line of lines) {
    // 查找测试结果模式
    if (line.includes('✅') && line.includes('通过')) {
      analysis.passedTests++;
      analysis.totalTests++;
      foundResults = true;
    } else if (line.includes('❌') && line.includes('失败')) {
      analysis.failedTests++;
      analysis.totalTests++;
      foundResults = true;
    } else if (line.includes('⏰') && line.includes('超时')) {
      analysis.errorTests++;
      analysis.totalTests++;
      foundResults = true;
    }
    
    // 查找通过率信息
    const passRateMatch = line.match(/通过率[：:]\s*(\d+(?:\.\d+)?)%/);
    if (passRateMatch) {
      const rate = parseFloat(passRateMatch[1]);
      analysis.details.push(`发现通过率信息: ${rate}%`);
    }
  }

  if (foundResults) {
    analysis.details.push(`文本解析: 找到测试结果标记`);
  } else {
    analysis.details.push(`文本解析: 未找到明确的测试结果`);
  }
}

function generateReport(analysis, reportsDir) {
  // 生成 JSON 报告
  const jsonReport = path.join(reportsDir, 'analysis.json');
  fs.writeFileSync(jsonReport, JSON.stringify(analysis, null, 2));

  // 生成 Markdown 报告
  const mdReport = path.join(reportsDir, 'analysis.md');
  const markdown = `# 测试结果分析报告

## 📊 概览

- **测试类别**: ${analysis.testCategory}
- **分析时间**: ${analysis.timestamp}
- **通过率阈值**: ${analysis.threshold}%

## 📈 统计结果

| 指标 | 数值 |
|------|------|
| 总测试数 | ${analysis.totalTests} |
| 通过测试 | ${analysis.passedTests} |
| 失败测试 | ${analysis.failedTests} |
| 错误测试 | ${analysis.errorTests} |
| 跳过测试 | ${analysis.skippedTests} |
| **通过率** | **${analysis.passRate.toFixed(1)}%** |
| **阈值达标** | **${analysis.thresholdMet ? '✅ 是' : '❌ 否'}** |

${analysis.averageScore > 0 ? `- **平均得分**: ${(analysis.averageScore * 100).toFixed(1)}%` : ''}
${analysis.averageExecutionTime > 0 ? `- **平均执行时间**: ${(analysis.averageExecutionTime / 1000).toFixed(1)}秒` : ''}

## 📋 详细信息

${analysis.details.map(detail => `- ${detail}`).join('\n')}

## 🎯 结论

${analysis.thresholdMet 
  ? `✅ **测试通过**: 通过率 ${analysis.passRate.toFixed(1)}% 达到阈值要求 ${analysis.threshold}%`
  : `❌ **测试未通过**: 通过率 ${analysis.passRate.toFixed(1)}% 低于阈值要求 ${analysis.threshold}%`
}
`;

  fs.writeFileSync(mdReport, markdown);
  
  console.log(`📄 报告已生成:`);
  console.log(`  - JSON: ${jsonReport}`);
  console.log(`  - Markdown: ${mdReport}`);
}

// 运行分析
if (require.main === module) {
  analyzeTestResults();
}

module.exports = { analyzeTestResults };

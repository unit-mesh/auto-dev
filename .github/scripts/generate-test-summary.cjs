#!/usr/bin/env node

/**
 * GitHub Actions 测试摘要生成器
 * 
 * 为 GitHub Actions 生成详细的测试摘要，显示在 PR 和 Actions 页面
 */

const fs = require('fs');
const path = require('path');

function generateTestSummary() {
  console.log('📊 生成 GitHub Actions 测试摘要...');

  const testCategory = process.env.TEST_CATEGORY || 'unknown';
  const nodeVersion = process.env.NODE_VERSION || 'unknown';
  const passThreshold = process.env.PASS_THRESHOLD || '80';
  const jobStatus = process.env.JOB_STATUS || 'unknown';
  
  // 读取分析结果
  const analysisPath = path.join(__dirname, '../../mpp-ui/test-results/reports/analysis.json');
  let analysis = null;
  
  try {
    if (fs.existsSync(analysisPath)) {
      analysis = JSON.parse(fs.readFileSync(analysisPath, 'utf-8'));
      console.log('✅ 找到测试分析结果');
    } else {
      console.log('⚠️  未找到测试分析结果文件');
    }
  } catch (error) {
    console.log(`❌ 读取分析结果失败: ${error.message}`);
  }

  // 生成摘要内容
  let summary = `## 🤖 CodingAgent Integration Tests v2 Results\n\n`;
  
  // 基本信息
  summary += `### 📊 Test Configuration\n\n`;
  summary += `| 配置项 | 值 |\n`;
  summary += `|--------|----|\n`;
  summary += `| **测试类别** | \`${testCategory}\` |\n`;
  summary += `| **Node.js 版本** | \`${nodeVersion}\` |\n`;
  summary += `| **通过率阈值** | \`${passThreshold}%\` |\n`;
  summary += `| **执行状态** | ${getStatusBadge(jobStatus)} |\n`;
  summary += `| **执行时间** | \`${new Date().toISOString()}\` |\n\n`;

  // 测试结果
  if (analysis) {
    summary += `### 📈 Test Results\n\n`;
    
    // 结果概览
    const passRate = analysis.passRate || 0;
    const thresholdMet = analysis.thresholdMet || false;
    
    summary += `| 指标 | 数值 | 状态 |\n`;
    summary += `|------|------|------|\n`;
    summary += `| **总测试数** | ${analysis.totalTests || 0} | ℹ️ |\n`;
    summary += `| **通过测试** | ${analysis.passedTests || 0} | ✅ |\n`;
    summary += `| **失败测试** | ${analysis.failedTests || 0} | ${analysis.failedTests > 0 ? '❌' : '✅'} |\n`;
    summary += `| **错误测试** | ${analysis.errorTests || 0} | ${analysis.errorTests > 0 ? '💥' : '✅'} |\n`;
    summary += `| **跳过测试** | ${analysis.skippedTests || 0} | ${analysis.skippedTests > 0 ? '⏭️' : 'ℹ️'} |\n`;
    summary += `| **通过率** | **${passRate.toFixed(1)}%** | ${thresholdMet ? '✅' : '❌'} |\n`;
    
    if (analysis.averageScore > 0) {
      summary += `| **平均得分** | ${(analysis.averageScore * 100).toFixed(1)}% | ${analysis.averageScore >= 0.8 ? '✅' : analysis.averageScore >= 0.6 ? '⚠️' : '❌'} |\n`;
    }
    
    if (analysis.averageExecutionTime > 0) {
      const avgTimeMinutes = (analysis.averageExecutionTime / 1000 / 60).toFixed(1);
      summary += `| **平均执行时间** | ${avgTimeMinutes} 分钟 | ℹ️ |\n`;
    }
    
    summary += `\n`;

    // 阈值检查结果
    summary += `### 🎯 Threshold Check\n\n`;
    if (thresholdMet) {
      summary += `✅ **通过率达标**: ${passRate.toFixed(1)}% ≥ ${passThreshold}%\n\n`;
      summary += `🎉 **恭喜！** CodingAgent 在 ${testCategory} 类别的测试中表现优秀，达到了质量标准。\n\n`;
    } else {
      summary += `❌ **通过率未达标**: ${passRate.toFixed(1)}% < ${passThreshold}%\n\n`;
      summary += `⚠️ **需要改进**: CodingAgent 在 ${testCategory} 类别的测试中需要进一步优化。\n\n`;
    }

    // 框架特性验证
    summary += `### 🔬 Framework Features Validated\n\n`;
    summary += `新的测试框架 v2 提供了以下深度分析：\n\n`;
    summary += `- 🎯 **提示词效果分析**: 验证系统提示词是否正确引导 Agent 行为\n`;
    summary += `- 🔧 **工具调用分析**: 跟踪工具使用准确率、调用顺序、参数正确性\n`;
    summary += `- 📊 **代码质量分析**: 检测语法错误、结构问题、最佳实践违规\n`;
    summary += `- ✅ **任务完成度分析**: 评估功能实现完整性、向后兼容性\n`;
    summary += `- 📈 **标准化评分**: 统一的 0-1 分制评分体系\n`;
    summary += `- 📋 **详细报告**: 具体的改进建议和问题识别\n\n`;

    // 详细信息
    if (analysis.details && analysis.details.length > 0) {
      summary += `### 📋 Analysis Details\n\n`;
      analysis.details.forEach(detail => {
        summary += `- ${detail}\n`;
      });
      summary += `\n`;
    }
  } else {
    summary += `### ⚠️ Test Results\n\n`;
    summary += `无法读取详细的测试分析结果。请检查测试执行日志。\n\n`;
  }

  // 下一步建议
  summary += `### 🚀 Next Steps\n\n`;
  if (jobStatus === 'success' && analysis && analysis.thresholdMet) {
    summary += `✅ **测试通过**: 所有测试都达到了质量标准\n\n`;
    summary += `**建议**:\n`;
    summary += `- 继续保持代码质量\n`;
    summary += `- 考虑添加更多测试场景\n`;
    summary += `- 定期运行性能测试\n`;
  } else {
    summary += `🔍 **需要关注的问题**:\n\n`;
    summary += `1. 📋 查看详细的测试日志和错误信息\n`;
    summary += `2. 📁 下载测试工件进行本地分析\n`;
    summary += `3. 🛠️ 使用新测试框架的详细报告进行调试\n`;
    summary += `4. 🎯 针对失败的测试用例进行优化\n\n`;
    
    summary += `**可用的工件**:\n`;
    summary += `- \`test-results-v2-${nodeVersion}-${testCategory}\`: 详细测试结果和报告\n`;
    if (process.env.KEEP_TEST_PROJECTS === 'true') {
      summary += `- \`test-projects-${nodeVersion}-${testCategory}\`: 测试项目文件用于调试\n`;
    }
  }

  // 框架信息
  summary += `\n### 📚 Framework Information\n\n`;
  summary += `这些测试使用了全新的 **AI Agent 健壮性测试框架 v2**，相比原有测试提供了：\n\n`;
  summary += `- 🔬 **深度多维分析**: 不仅验证功能，还分析行为模式\n`;
  summary += `- 📊 **标准化报告**: 统一的评分体系和详细的改进建议\n`;
  summary += `- 🎭 **场景模板**: 可复用的测试场景，易于扩展\n`;
  summary += `- 🔧 **更好的可扩展性**: 模块化架构，支持自定义分析器\n\n`;
  
  summary += `📖 **相关文档**:\n`;
  summary += `- [测试框架文档](../mpp-ui/src/test/framework/README.md)\n`;
  summary += `- [集成测试 v2 说明](../mpp-ui/src/test/integration-v2/README.md)\n`;
  summary += `- [迁移指南](../docs/test-scripts/INTEGRATION_TESTS_V2_MIGRATION.md)\n`;

  // 写入到 GitHub Actions 摘要
  const summaryFile = process.env.GITHUB_STEP_SUMMARY;
  if (summaryFile) {
    fs.writeFileSync(summaryFile, summary);
    console.log('✅ GitHub Actions 摘要已生成');
  } else {
    console.log('⚠️  GITHUB_STEP_SUMMARY 环境变量未设置，输出摘要到控制台:');
    console.log('\n' + summary);
  }

  // 同时保存到文件
  const outputPath = path.join(__dirname, '../../mpp-ui/test-results/reports/github-summary.md');
  fs.writeFileSync(outputPath, summary);
  console.log(`📄 摘要已保存到: ${outputPath}`);

  return summary;
}

function getStatusBadge(status) {
  switch (status.toLowerCase()) {
    case 'success':
      return '✅ Success';
    case 'failure':
      return '❌ Failure';
    case 'cancelled':
      return '⏹️ Cancelled';
    default:
      return `ℹ️ ${status}`;
  }
}

// 运行生成器
if (require.main === module) {
  generateTestSummary();
}

module.exports = { generateTestSummary };

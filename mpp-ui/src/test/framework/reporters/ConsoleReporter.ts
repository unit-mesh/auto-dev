/**
 * 控制台测试报告生成器
 * 
 * 生成详细的控制台测试报告，包括测试结果、分析数据、改进建议等
 */

import { TestResult, TestSuiteResult, TestStatus } from '../core/TestResult';
import { PromptAnalyzer } from '../analyzers/PromptAnalyzer';
import { ToolCallAnalyzer } from '../analyzers/ToolCallAnalyzer';

export class ConsoleReporter {
  /**
   * 生成单个测试结果报告
   */
  static generateTestReport(result: TestResult): string {
    const lines: string[] = [];
    
    // 标题和基本信息
    lines.push(`\n${'='.repeat(80)}`);
    lines.push(`🧪 测试报告: ${result.testCase.name}`);
    lines.push(`${'='.repeat(80)}`);
    lines.push(`📝 描述: ${result.testCase.description}`);
    lines.push(`🏷️  类别: ${result.testCase.category}`);
    lines.push(`📋 任务: ${result.testCase.task}`);
    lines.push(`⏱️  执行时间: ${result.executionInfo.duration}ms`);
    lines.push(`📊 综合得分: ${(result.overallScore * 100).toFixed(1)}%`);
    
    // 测试状态
    const statusIcon = this.getStatusIcon(result.status);
    lines.push(`${statusIcon} 测试状态: ${result.status}`);
    
    if (result.status !== TestStatus.PASSED) {
      lines.push(`❌ 退出码: ${result.executionInfo.exitCode}`);
    }
    
    // 提示词分析
    lines.push(`\n📋 提示词效果分析:`);
    lines.push(`  • 遵循系统提示词: ${result.promptAnalysis.followedSystemPrompt ? '✅' : '❌'}`);
    lines.push(`  • 首先探索项目: ${result.promptAnalysis.exploredProjectFirst ? '✅' : '❌'}`);
    lines.push(`  • 使用合适工具: ${result.promptAnalysis.usedAppropriateTools ? '✅' : '❌'}`);
    lines.push(`  • 优雅处理错误: ${result.promptAnalysis.handledErrorsGracefully ? '✅' : '❌'}`);
    lines.push(`  • 有效性得分: ${(result.promptAnalysis.promptEffectivenessScore * 100).toFixed(1)}%`);
    
    if (result.promptAnalysis.issues.length > 0) {
      lines.push(`  ⚠️  问题:`);
      result.promptAnalysis.issues.forEach(issue => {
        lines.push(`    - ${issue}`);
      });
    }
    
    // 工具调用分析
    lines.push(`\n🔧 工具调用分析:`);
    lines.push(`  • 总调用次数: ${result.toolCallAnalysis.totalCalls}`);
    lines.push(`  • 使用的工具: ${result.toolCallAnalysis.uniqueTools.join(', ')}`);
    lines.push(`  • 工具准确率: ${(result.toolCallAnalysis.toolAccuracy * 100).toFixed(1)}%`);
    lines.push(`  • 顺序正确性: ${(result.toolCallAnalysis.sequenceCorrectness * 100).toFixed(1)}%`);
    lines.push(`  • 参数正确性: ${(result.toolCallAnalysis.parameterCorrectness * 100).toFixed(1)}%`);
    
    if (result.toolCallAnalysis.missingTools.length > 0) {
      lines.push(`  ❌ 缺失工具: ${result.toolCallAnalysis.missingTools.join(', ')}`);
    }
    
    if (result.toolCallAnalysis.unexpectedTools.length > 0) {
      lines.push(`  ⚠️  意外工具: ${result.toolCallAnalysis.unexpectedTools.join(', ')}`);
    }
    
    // 代码质量分析
    lines.push(`\n📊 代码质量分析:`);
    lines.push(`  • 语法错误: ${result.codeQuality.syntaxErrors}`);
    lines.push(`  • 结构问题: ${result.codeQuality.structuralIssues}`);
    lines.push(`  • 最佳实践违规: ${result.codeQuality.bestPracticeViolations}`);
    lines.push(`  • 总问题数: ${result.codeQuality.totalIssues}`);
    lines.push(`  • 质量得分: ${(result.codeQuality.qualityScore * 100).toFixed(1)}%`);
    
    // 任务完成情况
    lines.push(`\n✅ 任务完成分析:`);
    lines.push(`  • 任务完成: ${result.taskCompletion.completed ? '✅' : '❌'}`);
    lines.push(`  • 完成度: ${(result.taskCompletion.completionScore * 100).toFixed(1)}%`);
    lines.push(`  • 向后兼容: ${result.taskCompletion.backwardCompatibility ? '✅' : '❌'}`);
    
    if (result.taskCompletion.functionalityImplemented.length > 0) {
      lines.push(`  ✅ 已实现功能:`);
      result.taskCompletion.functionalityImplemented.forEach(func => {
        lines.push(`    - ${func}`);
      });
    }
    
    if (result.taskCompletion.functionalityMissing.length > 0) {
      lines.push(`  ❌ 缺失功能:`);
      result.taskCompletion.functionalityMissing.forEach(func => {
        lines.push(`    - ${func}`);
      });
    }
    
    // 文件变更
    if (result.fileChanges.length > 0) {
      lines.push(`\n📁 文件变更 (${result.fileChanges.length}):`);
      result.fileChanges.forEach(change => {
        const icon = change.type === 'created' ? '➕' : change.type === 'modified' ? '📝' : '🗑️';
        lines.push(`  ${icon} ${change.type}: ${change.path}`);
      });
    }
    
    // 错误和警告
    if (result.errors.length > 0) {
      lines.push(`\n❌ 错误:`);
      result.errors.forEach(error => {
        lines.push(`  • ${error}`);
      });
    }
    
    if (result.warnings.length > 0) {
      lines.push(`\n⚠️  警告:`);
      result.warnings.forEach(warning => {
        lines.push(`  • ${warning}`);
      });
    }
    
    // 改进建议
    const suggestions = this.generateImprovementSuggestions(result);
    if (suggestions.length > 0) {
      lines.push(`\n💡 改进建议:`);
      suggestions.forEach(suggestion => {
        lines.push(`  • ${suggestion}`);
      });
    }
    
    lines.push(`${'='.repeat(80)}\n`);
    
    return lines.join('\n');
  }

  /**
   * 生成测试套件报告
   */
  static generateSuiteReport(suiteResult: TestSuiteResult): string {
    const lines: string[] = [];
    
    // 套件标题
    lines.push(`\n${'█'.repeat(100)}`);
    lines.push(`🎯 测试套件报告: ${suiteResult.suiteName}`);
    lines.push(`${'█'.repeat(100)}`);
    
    // 基本统计
    lines.push(`📊 执行统计:`);
    lines.push(`  • 总测试数: ${suiteResult.totalTests}`);
    lines.push(`  • 通过: ${suiteResult.passedTests} (${((suiteResult.passedTests / suiteResult.totalTests) * 100).toFixed(1)}%)`);
    lines.push(`  • 失败: ${suiteResult.failedTests}`);
    lines.push(`  • 错误: ${suiteResult.errorTests}`);
    lines.push(`  • 跳过: ${suiteResult.skippedTests}`);
    lines.push(`  • 总执行时间: ${suiteResult.duration}ms`);
    lines.push(`  • 平均执行时间: ${suiteResult.summary.averageExecutionTime.toFixed(0)}ms`);
    lines.push(`  • 平均得分: ${(suiteResult.summary.averageScore * 100).toFixed(1)}%`);
    
    // 按类别统计
    lines.push(`\n📈 按类别统计:`);
    Object.entries(suiteResult.summary.categoryStats).forEach(([category, stats]) => {
      const passRate = (stats.passed / stats.total * 100).toFixed(1);
      lines.push(`  • ${category}: ${stats.passed}/${stats.total} (${passRate}%)`);
    });
    
    // 工具使用统计
    lines.push(`\n🔧 工具使用统计:`);
    const sortedTools = Object.entries(suiteResult.summary.toolUsageStats)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10);
    sortedTools.forEach(([tool, count]) => {
      lines.push(`  • ${tool}: ${count} 次`);
    });
    
    // 最常见问题
    if (suiteResult.summary.mostCommonIssues.length > 0) {
      lines.push(`\n⚠️  最常见问题:`);
      suiteResult.summary.mostCommonIssues.forEach((issue, index) => {
        lines.push(`  ${index + 1}. ${issue}`);
      });
    }
    
    // 详细测试结果
    lines.push(`\n📋 详细测试结果:`);
    suiteResult.testResults.forEach((result, index) => {
      const statusIcon = this.getStatusIcon(result.status);
      const score = (result.overallScore * 100).toFixed(1);
      const duration = result.executionInfo.duration;
      lines.push(`  ${index + 1}. ${statusIcon} ${result.testCase.name} - ${score}% (${duration}ms)`);
    });
    
    lines.push(`${'█'.repeat(100)}\n`);
    
    return lines.join('\n');
  }

  /**
   * 获取状态图标
   */
  private static getStatusIcon(status: TestStatus): string {
    switch (status) {
      case TestStatus.PASSED: return '✅';
      case TestStatus.FAILED: return '❌';
      case TestStatus.ERROR: return '💥';
      case TestStatus.TIMEOUT: return '⏰';
      case TestStatus.SKIPPED: return '⏭️';
      default: return '❓';
    }
  }

  /**
   * 生成改进建议
   */
  private static generateImprovementSuggestions(result: TestResult): string[] {
    const suggestions: string[] = [];
    
    // 基于提示词分析的建议
    suggestions.push(...PromptAnalyzer.generateImprovementSuggestions(result.promptAnalysis));
    
    // 基于工具调用分析的建议
    suggestions.push(...ToolCallAnalyzer.generateImprovementSuggestions(result.toolCallAnalysis));
    
    // 基于代码质量的建议
    if (result.codeQuality.qualityScore < 0.8) {
      suggestions.push('建议加强代码质量检查和最佳实践指导');
    }
    
    // 基于任务完成情况的建议
    if (result.taskCompletion.completionScore < 0.8) {
      suggestions.push('建议改进任务理解和执行策略');
    }
    
    return suggestions;
  }
}

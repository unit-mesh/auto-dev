/**
 * 提示词效果分析器
 * 
 * 分析 AI Agent 是否按照系统提示词的指导进行操作，
 * 验证提示词的有效性和 Agent 的遵循程度
 */

import { PromptAnalysisResult, ExecutionInfo, ToolCallInfo } from '../core/TestResult';
import { TestCase, PromptExpectation } from '../core/TestCase';

export interface PromptPattern {
  name: string;
  pattern: RegExp;
  weight: number; // 权重，用于计算综合得分
  description: string;
}

export class PromptAnalyzer {
  // 系统提示词相关的行为模式
  private static readonly PROMPT_PATTERNS: PromptPattern[] = [
    {
      name: 'project_exploration',
      pattern: /(?:glob|list|explore|structure|understand)/i,
      weight: 0.2,
      description: '是否首先探索项目结构'
    },
    {
      name: 'tool_selection',
      pattern: /(?:read-file|write-file|grep|shell)/i,
      weight: 0.3,
      description: '是否选择了合适的工具'
    },
    {
      name: 'error_handling',
      pattern: /(?:error|failed|exception|recovery)/i,
      weight: 0.2,
      description: '是否处理了错误情况'
    },
    {
      name: 'systematic_approach',
      pattern: /(?:step|first|then|next|finally)/i,
      weight: 0.15,
      description: '是否采用系统性方法'
    },
    {
      name: 'code_quality',
      pattern: /(?:test|quality|best.practice|standard)/i,
      weight: 0.15,
      description: '是否关注代码质量'
    }
  ];

  /**
   * 分析 Agent 输出是否符合系统提示词的指导
   */
  static analyze(
    testCase: TestCase,
    executionInfo: ExecutionInfo,
    toolCalls: ToolCallInfo[]
  ): PromptAnalysisResult {
    const output = executionInfo.stdout + executionInfo.stderr;
    const expectation = testCase.expectedPromptBehavior;
    
    const result: PromptAnalysisResult = {
      followedSystemPrompt: false,
      exploredProjectFirst: false,
      usedAppropriateTools: false,
      handledErrorsGracefully: false,
      promptEffectivenessScore: 0,
      issues: []
    };

    // 1. 检查是否遵循了系统提示词
    result.followedSystemPrompt = this.checkSystemPromptCompliance(output, expectation);

    // 2. 检查是否首先探索项目
    if (expectation.shouldExploreProjectFirst) {
      result.exploredProjectFirst = this.checkProjectExploration(toolCalls);
      if (!result.exploredProjectFirst) {
        result.issues.push('Agent 没有首先探索项目结构');
      }
    }

    // 3. 检查工具使用是否合适
    if (expectation.shouldUseAppropriateTools) {
      result.usedAppropriateTools = this.checkToolAppropriatenesss(toolCalls, testCase);
      if (!result.usedAppropriateTools) {
        result.issues.push('Agent 没有使用合适的工具');
      }
    }

    // 4. 检查错误处理
    if (expectation.shouldHandleErrorsGracefully) {
      result.handledErrorsGracefully = this.checkErrorHandling(output, toolCalls);
      if (!result.handledErrorsGracefully) {
        result.issues.push('Agent 没有优雅地处理错误');
      }
    }

    // 5. 运行自定义验证
    if (expectation.customValidations) {
      for (const validation of expectation.customValidations) {
        if (!validation(output)) {
          result.issues.push('自定义验证失败');
        }
      }
    }

    // 6. 计算提示词有效性得分
    result.promptEffectivenessScore = this.calculateEffectivenessScore(output, result);

    return result;
  }

  /**
   * 检查系统提示词遵循情况
   */
  private static checkSystemPromptCompliance(
    output: string,
    expectation: PromptExpectation
  ): boolean {
    // 检查输出中是否包含系统提示词相关的行为指标
    const indicators = [
      /(?:analyzing|understanding|exploring)/i,
      /(?:using|calling|executing).+(?:tool|command)/i,
      /(?:reading|writing|modifying).+file/i,
      /(?:step|approach|method)/i
    ];

    return indicators.some(pattern => pattern.test(output));
  }

  /**
   * 检查是否首先探索了项目结构
   */
  private static checkProjectExploration(toolCalls: ToolCallInfo[]): boolean {
    if (toolCalls.length === 0) return false;

    // 检查前几个工具调用是否包含探索性工具
    const explorationTools = ['glob', 'read-file', 'ls', 'find'];
    const firstFewCalls = toolCalls.slice(0, Math.min(3, toolCalls.length));
    
    return firstFewCalls.some(call => 
      explorationTools.some(tool => call.tool.toLowerCase().includes(tool))
    );
  }

  /**
   * 检查工具使用的合适性
   */
  private static checkToolAppropriatenesss(
    toolCalls: ToolCallInfo[],
    testCase: TestCase
  ): boolean {
    const expectedTools = testCase.expectedToolCalls.map(exp => exp.tool.toLowerCase());
    const actualTools = toolCalls.map(call => call.tool.toLowerCase());
    
    // 计算期望工具的覆盖率
    const coverage = expectedTools.filter(tool => 
      actualTools.some(actual => actual.includes(tool))
    ).length / expectedTools.length;
    
    return coverage >= 0.6; // 至少60%的期望工具被使用
  }

  /**
   * 检查错误处理能力
   */
  private static checkErrorHandling(output: string, toolCalls: ToolCallInfo[]): boolean {
    const hasErrors = output.includes('Error') || output.includes('Failed') || 
                     toolCalls.some(call => !call.success);
    
    if (!hasErrors) return true; // 没有错误，无需处理
    
    // 检查是否有错误恢复的迹象
    const recoveryIndicators = [
      /(?:retry|retrying|trying.again)/i,
      /(?:alternative|different.approach)/i,
      /(?:fixing|correcting|resolving)/i,
      /(?:error.recovery|recovery.mode)/i
    ];
    
    return recoveryIndicators.some(pattern => pattern.test(output));
  }

  /**
   * 计算提示词有效性得分
   */
  private static calculateEffectivenessScore(
    output: string,
    result: PromptAnalysisResult
  ): number {
    let score = 0;
    let totalWeight = 0;

    // 基于模式匹配计算得分
    for (const pattern of this.PROMPT_PATTERNS) {
      totalWeight += pattern.weight;
      if (pattern.pattern.test(output)) {
        score += pattern.weight;
      }
    }

    // 基于具体检查结果调整得分
    const checks = [
      result.exploredProjectFirst,
      result.usedAppropriateTools,
      result.handledErrorsGracefully,
      result.followedSystemPrompt
    ];

    const checkScore = checks.filter(Boolean).length / checks.length * 0.4;
    const patternScore = totalWeight > 0 ? (score / totalWeight) * 0.6 : 0;

    return Math.min(1, patternScore + checkScore);
  }

  /**
   * 生成提示词改进建议
   */
  static generateImprovementSuggestions(result: PromptAnalysisResult): string[] {
    const suggestions: string[] = [];

    if (!result.exploredProjectFirst) {
      suggestions.push('建议在系统提示词中强调首先探索项目结构的重要性');
    }

    if (!result.usedAppropriateTools) {
      suggestions.push('建议在系统提示词中提供更明确的工具选择指导');
    }

    if (!result.handledErrorsGracefully) {
      suggestions.push('建议在系统提示词中加强错误处理和恢复策略的描述');
    }

    if (result.promptEffectivenessScore < 0.7) {
      suggestions.push('建议重新审视系统提示词的结构和内容，提高指导的明确性');
    }

    return suggestions;
  }
}

/**
 * 工具调用分析器
 * 
 * 分析和验证 AI Agent 调用的工具类型、参数、顺序等，
 * 评估工具使用的准确性和合理性
 */

import { ToolCallAnalysisResult, ToolCallInfo, ExecutionInfo } from '../core/TestResult';
import { TestCase, ToolCallExpectation } from '../core/TestCase';

export interface ToolCallPattern {
  tool: string;
  pattern: RegExp;
  extractParams?: (match: RegExpMatchArray) => Record<string, any>;
}

export class ToolCallAnalyzer {
  // 工具调用模式匹配规则
  private static readonly TOOL_PATTERNS: ToolCallPattern[] = [
    {
      tool: 'glob',
      pattern: /● glob - ([^-]+) - ([^\n]+)/g,
      extractParams: (match) => ({ description: match[1], action: match[2] })
    },
    {
      tool: 'read-file',
      pattern: /● read-file - ([^-]+) - ([^\n]+)/g,
      extractParams: (match) => ({ description: match[1], action: match[2] })
    },
    {
      tool: 'write-file',
      pattern: /● write-file - ([^-]+) - ([^\n]+)/g,
      extractParams: (match) => ({ description: match[1], action: match[2] })
    },
    {
      tool: 'grep',
      pattern: /● grep - ([^-]+) - ([^\n]+)/g,
      extractParams: (match) => ({ description: match[1], action: match[2] })
    },
    {
      tool: 'shell',
      pattern: /● shell - ([^-]+) - ([^\n]+)/g,
      extractParams: (match) => ({ description: match[1], action: match[2] })
    }
  ];

  /**
   * 分析工具调用情况
   */
  static analyze(
    testCase: TestCase,
    executionInfo: ExecutionInfo
  ): ToolCallAnalysisResult {
    const output = executionInfo.stdout + executionInfo.stderr;
    
    // 1. 提取工具调用信息
    const toolCallDetails = this.extractToolCalls(output);
    
    // 2. 分析工具使用准确性
    const toolAccuracy = this.calculateToolAccuracy(testCase.expectedToolCalls, toolCallDetails);
    
    // 3. 分析调用顺序正确性
    const sequenceCorrectness = this.analyzeCallSequence(testCase.expectedToolCalls, toolCallDetails);
    
    // 4. 分析参数正确性
    const parameterCorrectness = this.analyzeParameterCorrectness(testCase.expectedToolCalls, toolCallDetails);
    
    // 5. 识别意外和缺失的工具
    const { unexpectedTools, missingTools } = this.identifyToolGaps(testCase.expectedToolCalls, toolCallDetails);
    
    const uniqueTools = [...new Set(toolCallDetails.map(call => call.tool))];
    
    return {
      totalCalls: toolCallDetails.length,
      uniqueTools,
      toolAccuracy,
      sequenceCorrectness,
      parameterCorrectness,
      unexpectedTools,
      missingTools,
      toolCallDetails
    };
  }

  /**
   * 从输出中提取工具调用信息
   */
  private static extractToolCalls(output: string): ToolCallInfo[] {
    const toolCalls: ToolCallInfo[] = [];
    const lines = output.split('\n');
    let currentTimestamp = new Date();

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      
      // 尝试匹配各种工具调用模式
      for (const pattern of this.TOOL_PATTERNS) {
        const matches = [...line.matchAll(pattern.pattern)];
        
        for (const match of matches) {
          const toolCall: ToolCallInfo = {
            tool: pattern.tool,
            timestamp: new Date(currentTimestamp.getTime() + i * 100), // 模拟时间戳
            success: !line.includes('Error') && !line.includes('Failed'),
            parameters: pattern.extractParams ? pattern.extractParams(match) : undefined
          };
          
          // 尝试提取输出信息
          if (i + 1 < lines.length) {
            const nextLine = lines[i + 1];
            if (!nextLine.startsWith('●') && nextLine.trim()) {
              toolCall.output = nextLine.trim();
            }
          }
          
          toolCalls.push(toolCall);
        }
      }
      
      // 也检查其他可能的工具调用指示器
      if (line.includes('Tool:') || line.includes('Using tool:') || line.includes('Executing tool:')) {
        const toolMatch = line.match(/(?:Tool:|Using tool:|Executing tool:)\s*([a-zA-Z-]+)/);
        if (toolMatch) {
          toolCalls.push({
            tool: toolMatch[1],
            timestamp: new Date(currentTimestamp.getTime() + i * 100),
            success: !line.includes('Error') && !line.includes('Failed')
          });
        }
      }
    }

    return toolCalls;
  }

  /**
   * 计算工具使用准确性
   */
  private static calculateToolAccuracy(
    expectedCalls: ToolCallExpectation[],
    actualCalls: ToolCallInfo[]
  ): number {
    if (expectedCalls.length === 0) return 1;

    let correctCalls = 0;
    const actualToolNames = actualCalls.map(call => call.tool.toLowerCase());

    for (const expected of expectedCalls) {
      const expectedTool = expected.tool.toLowerCase();
      const actualCount = actualToolNames.filter(tool => 
        tool.includes(expectedTool) || expectedTool.includes(tool)
      ).length;

      // 检查调用次数是否符合期望
      const minCalls = expected.minCalls || (expected.required ? 1 : 0);
      const maxCalls = expected.maxCalls || Infinity;

      if (actualCount >= minCalls && actualCount <= maxCalls) {
        correctCalls++;
      }
    }

    return correctCalls / expectedCalls.length;
  }

  /**
   * 分析工具调用顺序的正确性
   */
  private static analyzeCallSequence(
    expectedCalls: ToolCallExpectation[],
    actualCalls: ToolCallInfo[]
  ): number {
    // 获取有顺序要求的期望调用
    const orderedExpected = expectedCalls
      .filter(exp => exp.order !== undefined)
      .sort((a, b) => (a.order || 0) - (b.order || 0));

    if (orderedExpected.length === 0) return 1; // 没有顺序要求

    let correctSequences = 0;
    let lastFoundIndex = -1;

    for (const expected of orderedExpected) {
      const expectedTool = expected.tool.toLowerCase();
      
      // 在实际调用中查找这个工具
      const foundIndex = actualCalls.findIndex((call, index) => 
        index > lastFoundIndex && 
        (call.tool.toLowerCase().includes(expectedTool) || expectedTool.includes(call.tool.toLowerCase()))
      );

      if (foundIndex > lastFoundIndex) {
        correctSequences++;
        lastFoundIndex = foundIndex;
      }
    }

    return correctSequences / orderedExpected.length;
  }

  /**
   * 分析参数使用的正确性
   */
  private static analyzeParameterCorrectness(
    expectedCalls: ToolCallExpectation[],
    actualCalls: ToolCallInfo[]
  ): number {
    const expectedWithParams = expectedCalls.filter(exp => exp.parameters);
    if (expectedWithParams.length === 0) return 1; // 没有参数要求

    let correctParams = 0;

    for (const expected of expectedWithParams) {
      const matchingCalls = actualCalls.filter(call => 
        call.tool.toLowerCase().includes(expected.tool.toLowerCase()) ||
        expected.tool.toLowerCase().includes(call.tool.toLowerCase())
      );

      for (const call of matchingCalls) {
        if (call.parameters && expected.parameters) {
          const paramMatches = Object.entries(expected.parameters).every(([key, value]) => {
            const actualValue = call.parameters![key];
            return actualValue === value || 
                   (typeof actualValue === 'string' && actualValue.includes(String(value)));
          });

          if (paramMatches) {
            correctParams++;
            break; // 找到一个匹配的就够了
          }
        }
      }
    }

    return correctParams / expectedWithParams.length;
  }

  /**
   * 识别意外和缺失的工具
   */
  private static identifyToolGaps(
    expectedCalls: ToolCallExpectation[],
    actualCalls: ToolCallInfo[]
  ): { unexpectedTools: string[]; missingTools: string[] } {
    const expectedTools = expectedCalls.map(exp => exp.tool.toLowerCase());
    const actualTools = [...new Set(actualCalls.map(call => call.tool.toLowerCase()))];

    // 找出缺失的必需工具
    const missingTools = expectedCalls
      .filter(exp => exp.required)
      .filter(exp => !actualTools.some(actual => 
        actual.includes(exp.tool.toLowerCase()) || exp.tool.toLowerCase().includes(actual)
      ))
      .map(exp => exp.tool);

    // 找出意外的工具（不在期望列表中的）
    const unexpectedTools = actualTools.filter(actual => 
      !expectedTools.some(expected => 
        actual.includes(expected) || expected.includes(actual)
      )
    );

    return { unexpectedTools, missingTools };
  }

  /**
   * 生成工具使用改进建议
   */
  static generateImprovementSuggestions(result: ToolCallAnalysisResult): string[] {
    const suggestions: string[] = [];

    if (result.toolAccuracy < 0.7) {
      suggestions.push('工具选择准确性较低，建议改进系统提示词中的工具选择指导');
    }

    if (result.sequenceCorrectness < 0.8) {
      suggestions.push('工具调用顺序不够合理，建议在提示词中强调逻辑顺序');
    }

    if (result.parameterCorrectness < 0.8) {
      suggestions.push('工具参数使用不够准确，建议提供更详细的参数使用示例');
    }

    if (result.missingTools.length > 0) {
      suggestions.push(`缺少必需的工具调用: ${result.missingTools.join(', ')}`);
    }

    if (result.unexpectedTools.length > 0) {
      suggestions.push(`使用了意外的工具: ${result.unexpectedTools.join(', ')}`);
    }

    return suggestions;
  }
}

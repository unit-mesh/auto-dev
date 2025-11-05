/**
 * 代码变更分析器
 * 
 * 分析和评估 AI Agent 产生的文件变更、代码质量、功能完整性等
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import { FileChangeInfo, CodeQualityResult, TaskCompletionResult } from '../core/TestResult';
import { TestCase, ChangeExpectation } from '../core/TestCase';

export interface CodeMetrics {
  linesOfCode: number;
  complexity: number;
  maintainabilityIndex: number;
  duplicateLines: number;
}

export interface FileSnapshot {
  path: string;
  content: string;
  size: number;
  lastModified: Date;
  hash: string;
}

export class CodeChangeAnalyzer {
  /**
   * 分析项目文件变更
   */
  static async analyzeFileChanges(
    projectPath: string,
    beforeSnapshot: Map<string, FileSnapshot>,
    afterSnapshot: Map<string, FileSnapshot>
  ): Promise<FileChangeInfo[]> {
    const changes: FileChangeInfo[] = [];

    // 检查新创建的文件
    for (const [filePath, snapshot] of afterSnapshot) {
      if (!beforeSnapshot.has(filePath)) {
        changes.push({
          type: 'created',
          path: filePath,
          sizeAfter: snapshot.size,
          contentPreview: this.getContentPreview(snapshot.content),
          timestamp: snapshot.lastModified
        });
      }
    }

    // 检查修改的文件
    for (const [filePath, afterSnap] of afterSnapshot) {
      const beforeSnap = beforeSnapshot.get(filePath);
      if (beforeSnap && beforeSnap.hash !== afterSnap.hash) {
        changes.push({
          type: 'modified',
          path: filePath,
          sizeBefore: beforeSnap.size,
          sizeAfter: afterSnap.size,
          contentPreview: this.getContentPreview(afterSnap.content),
          timestamp: afterSnap.lastModified
        });
      }
    }

    // 检查删除的文件
    for (const [filePath, snapshot] of beforeSnapshot) {
      if (!afterSnapshot.has(filePath)) {
        changes.push({
          type: 'deleted',
          path: filePath,
          sizeBefore: snapshot.size,
          timestamp: new Date()
        });
      }
    }

    return changes;
  }

  /**
   * 分析代码质量
   */
  static async analyzeCodeQuality(
    projectPath: string,
    fileChanges: FileChangeInfo[]
  ): Promise<CodeQualityResult> {
    const result: CodeQualityResult = {
      syntaxErrors: 0,
      structuralIssues: 0,
      bestPracticeViolations: 0,
      totalIssues: 0,
      qualityScore: 1.0,
      issues: []
    };

    // 分析每个变更的文件
    for (const change of fileChanges) {
      if (change.type === 'deleted') continue;

      const filePath = path.join(projectPath, change.path);
      try {
        const content = await fs.readFile(filePath, 'utf-8');
        const fileIssues = await this.analyzeFileQuality(filePath, content);
        result.issues.push(...fileIssues);
      } catch (error) {
        result.issues.push({
          type: 'syntax',
          severity: 'error',
          message: `无法读取文件: ${error}`,
          file: change.path
        });
      }
    }

    // 统计问题数量
    result.syntaxErrors = result.issues.filter(i => i.type === 'syntax' && i.severity === 'error').length;
    result.structuralIssues = result.issues.filter(i => i.type === 'structure').length;
    result.bestPracticeViolations = result.issues.filter(i => i.type === 'best-practice').length;
    result.totalIssues = result.issues.length;

    // 计算质量得分
    result.qualityScore = this.calculateQualityScore(result);

    return result;
  }

  /**
   * 分析任务完成情况
   */
  static async analyzeTaskCompletion(
    testCase: TestCase,
    projectPath: string,
    fileChanges: FileChangeInfo[]
  ): Promise<TaskCompletionResult> {
    const result: TaskCompletionResult = {
      completed: false,
      completionScore: 0,
      functionalityImplemented: [],
      functionalityMissing: [],
      backwardCompatibility: true,
      regressionIssues: []
    };

    // 检查期望的变更是否实现
    const implementedChanges = await this.checkExpectedChanges(
      testCase.expectedChanges,
      fileChanges,
      projectPath
    );

    result.functionalityImplemented = implementedChanges.implemented;
    result.functionalityMissing = implementedChanges.missing;
    result.completionScore = implementedChanges.score;
    result.completed = result.completionScore >= 0.8;

    // 检查向后兼容性
    result.backwardCompatibility = await this.checkBackwardCompatibility(
      projectPath,
      fileChanges
    );

    return result;
  }

  /**
   * 分析单个文件的代码质量
   */
  private static async analyzeFileQuality(
    filePath: string,
    content: string
  ): Promise<CodeQualityResult['issues']> {
    const issues: CodeQualityResult['issues'] = [];
    const ext = path.extname(filePath).toLowerCase();

    // Java 文件分析
    if (ext === '.java') {
      issues.push(...this.analyzeJavaFile(filePath, content));
    }
    // TypeScript/JavaScript 文件分析
    else if (ext === '.ts' || ext === '.js') {
      issues.push(...this.analyzeTypeScriptFile(filePath, content));
    }
    // Kotlin 文件分析
    else if (ext === '.kt') {
      issues.push(...this.analyzeKotlinFile(filePath, content));
    }
    // 配置文件分析
    else if (['.json', '.yml', '.yaml', '.xml', '.properties'].includes(ext)) {
      issues.push(...this.analyzeConfigFile(filePath, content));
    }

    return issues;
  }

  /**
   * 分析 Java 文件
   */
  private static analyzeJavaFile(filePath: string, content: string): CodeQualityResult['issues'] {
    const issues: CodeQualityResult['issues'] = [];
    const lines = content.split('\n');

    // 检查基本语法问题
    if (!content.includes('class ') && !content.includes('interface ') && !content.includes('enum ')) {
      issues.push({
        type: 'syntax',
        severity: 'error',
        message: '文件中没有找到类、接口或枚举定义',
        file: filePath
      });
    }

    // 检查包声明
    if (!content.startsWith('package ')) {
      issues.push({
        type: 'structure',
        severity: 'warning',
        message: '缺少包声明',
        file: filePath,
        line: 1
      });
    }

    // 检查大括号匹配
    const openBraces = (content.match(/\{/g) || []).length;
    const closeBraces = (content.match(/\}/g) || []).length;
    if (openBraces !== closeBraces) {
      issues.push({
        type: 'syntax',
        severity: 'error',
        message: '大括号不匹配',
        file: filePath
      });
    }

    // 检查最佳实践
    if (content.includes('System.out.println')) {
      issues.push({
        type: 'best-practice',
        severity: 'info',
        message: '建议使用日志框架而不是 System.out.println',
        file: filePath
      });
    }

    return issues;
  }

  /**
   * 分析 TypeScript 文件
   */
  private static analyzeTypeScriptFile(filePath: string, content: string): CodeQualityResult['issues'] {
    const issues: CodeQualityResult['issues'] = [];

    // 检查基本语法
    const openBraces = (content.match(/\{/g) || []).length;
    const closeBraces = (content.match(/\}/g) || []).length;
    if (openBraces !== closeBraces) {
      issues.push({
        type: 'syntax',
        severity: 'error',
        message: '大括号不匹配',
        file: filePath
      });
    }

    // 检查分号使用
    const lines = content.split('\n');
    lines.forEach((line, index) => {
      const trimmed = line.trim();
      if (trimmed && !trimmed.endsWith(';') && !trimmed.endsWith('{') && 
          !trimmed.endsWith('}') && !trimmed.startsWith('//') && 
          !trimmed.startsWith('*') && !trimmed.startsWith('import')) {
        issues.push({
          type: 'best-practice',
          severity: 'info',
          message: '建议在语句末尾添加分号',
          file: filePath,
          line: index + 1
        });
      }
    });

    return issues;
  }

  /**
   * 分析 Kotlin 文件
   */
  private static analyzeKotlinFile(filePath: string, content: string): CodeQualityResult['issues'] {
    const issues: CodeQualityResult['issues'] = [];

    // 检查基本语法
    if (!content.includes('class ') && !content.includes('interface ') && 
        !content.includes('object ') && !content.includes('fun ')) {
      issues.push({
        type: 'syntax',
        severity: 'warning',
        message: '文件中没有找到类、接口、对象或函数定义',
        file: filePath
      });
    }

    return issues;
  }

  /**
   * 分析配置文件
   */
  private static analyzeConfigFile(filePath: string, content: string): CodeQualityResult['issues'] {
    const issues: CodeQualityResult['issues'] = [];
    const ext = path.extname(filePath).toLowerCase();

    try {
      if (ext === '.json') {
        JSON.parse(content);
      } else if (ext === '.xml') {
        // 简单的 XML 格式检查
        if (!content.includes('<?xml') && content.includes('<')) {
          issues.push({
            type: 'syntax',
            severity: 'warning',
            message: '可能缺少 XML 声明',
            file: filePath
          });
        }
      }
    } catch (error) {
      issues.push({
        type: 'syntax',
        severity: 'error',
        message: `配置文件格式错误: ${error}`,
        file: filePath
      });
    }

    return issues;
  }

  /**
   * 检查期望的变更是否实现
   */
  private static async checkExpectedChanges(
    expectedChanges: ChangeExpectation[],
    actualChanges: FileChangeInfo[],
    projectPath: string
  ): Promise<{ implemented: string[]; missing: string[]; score: number }> {
    const implemented: string[] = [];
    const missing: string[] = [];

    for (const expected of expectedChanges) {
      let found = false;

      switch (expected.type) {
        case 'file-created':
          found = actualChanges.some(change => 
            change.type === 'created' && 
            (expected.path ? change.path.includes(expected.path) : true) &&
            (expected.pattern ? expected.pattern.test(change.path) : true)
          );
          break;

        case 'file-modified':
          found = actualChanges.some(change => 
            change.type === 'modified' && 
            (expected.path ? change.path.includes(expected.path) : true) &&
            (expected.pattern ? expected.pattern.test(change.path) : true)
          );
          break;

        case 'dependency-added':
          // 检查构建文件是否包含新依赖
          found = await this.checkDependencyAdded(projectPath, expected);
          break;
      }

      if (found) {
        implemented.push(`${expected.type}: ${expected.path || expected.pattern?.source || 'unknown'}`);
      } else if (expected.required) {
        missing.push(`${expected.type}: ${expected.path || expected.pattern?.source || 'unknown'}`);
      }
    }

    const requiredChanges = expectedChanges.filter(c => c.required).length;
    const score = requiredChanges > 0 ? implemented.length / requiredChanges : 1;

    return { implemented, missing, score };
  }

  /**
   * 检查依赖是否已添加
   */
  private static async checkDependencyAdded(
    projectPath: string,
    expected: ChangeExpectation
  ): Promise<boolean> {
    const buildFiles = ['build.gradle.kts', 'build.gradle', 'pom.xml', 'package.json'];
    
    for (const buildFile of buildFiles) {
      const filePath = path.join(projectPath, buildFile);
      try {
        const content = await fs.readFile(filePath, 'utf-8');
        if (expected.content) {
          const pattern = typeof expected.content === 'string' 
            ? new RegExp(expected.content, 'i')
            : expected.content;
          if (pattern.test(content)) {
            return true;
          }
        }
      } catch (error) {
        // 文件不存在，继续检查下一个
      }
    }

    return false;
  }

  /**
   * 检查向后兼容性
   */
  private static async checkBackwardCompatibility(
    projectPath: string,
    fileChanges: FileChangeInfo[]
  ): Promise<boolean> {
    // 检查是否删除了重要文件
    const deletedFiles = fileChanges.filter(c => c.type === 'deleted');
    const criticalFiles = deletedFiles.filter(f => 
      f.path.includes('Application.') || 
      f.path.includes('Controller.') ||
      f.path.includes('Service.')
    );

    return criticalFiles.length === 0;
  }

  /**
   * 计算代码质量得分
   */
  private static calculateQualityScore(result: CodeQualityResult): number {
    const errorWeight = 0.5;
    const warningWeight = 0.3;
    const infoWeight = 0.2;

    const errors = result.issues.filter(i => i.severity === 'error').length;
    const warnings = result.issues.filter(i => i.severity === 'warning').length;
    const infos = result.issues.filter(i => i.severity === 'info').length;

    const penalty = errors * errorWeight + warnings * warningWeight + infos * infoWeight;
    return Math.max(0, 1 - penalty / 10); // 假设10个错误为最低分
  }

  /**
   * 获取内容预览
   */
  private static getContentPreview(content: string, maxLength: number = 200): string {
    if (content.length <= maxLength) return content;
    return content.substring(0, maxLength) + '...';
  }

  /**
   * 创建文件快照
   */
  static async createSnapshot(projectPath: string): Promise<Map<string, FileSnapshot>> {
    const snapshot = new Map<string, FileSnapshot>();
    
    try {
      await this.walkDirectory(projectPath, async (filePath) => {
        const relativePath = path.relative(projectPath, filePath);
        const stats = await fs.stat(filePath);
        const content = await fs.readFile(filePath, 'utf-8');
        
        snapshot.set(relativePath, {
          path: relativePath,
          content,
          size: stats.size,
          lastModified: stats.mtime,
          hash: this.simpleHash(content)
        });
      });
    } catch (error) {
      console.warn(`创建快照时出错: ${error}`);
    }

    return snapshot;
  }

  /**
   * 遍历目录
   */
  private static async walkDirectory(
    dir: string,
    callback: (filePath: string) => Promise<void>
  ): Promise<void> {
    const entries = await fs.readdir(dir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      
      if (entry.isDirectory() && !entry.name.startsWith('.') && entry.name !== 'node_modules') {
        await this.walkDirectory(fullPath, callback);
      } else if (entry.isFile()) {
        await callback(fullPath);
      }
    }
  }

  /**
   * 简单哈希函数
   */
  private static simpleHash(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // 转换为32位整数
    }
    return hash.toString(16);
  }
}

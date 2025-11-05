/**
 * AI Agent 测试引擎
 * 
 * 核心测试执行引擎，负责运行测试用例、收集结果、生成报告
 */

import { spawn, ChildProcess } from 'child_process';
import * as fs from 'fs/promises';
import * as path from 'path';
import * as os from 'os';

import { TestCase, TestSuite } from './TestCase';
import { TestResult, TestSuiteResult, TestStatus, ExecutionInfo, TestResultBuilder } from './TestResult';
import { PromptAnalyzer } from '../analyzers/PromptAnalyzer';
import { ToolCallAnalyzer } from '../analyzers/ToolCallAnalyzer';
import { CodeChangeAnalyzer } from '../analyzers/CodeChangeAnalyzer';

export interface TestEngineConfig {
  agentPath: string;
  outputDir: string;
  tempDir?: string;
  reporters: string[];
  parallel?: boolean;
  maxConcurrency?: number;
  globalTimeout?: number;
  keepTestProjects?: boolean;
  verbose?: boolean;
}

export interface TestProject {
  path: string;
  type: string;
  cleanup: () => Promise<void>;
}

export class TestEngine {
  private config: TestEngineConfig;
  private runningTests = new Map<string, ChildProcess>();

  constructor(config: TestEngineConfig) {
    this.config = {
      tempDir: os.tmpdir(),
      parallel: false,
      maxConcurrency: 3,
      globalTimeout: 600000, // 10 minutes
      keepTestProjects: false,
      verbose: false,
      ...config
    };
  }

  /**
   * 运行单个测试用例
   */
  async runTest(testCase: TestCase): Promise<TestResult> {
    console.log(`🧪 开始测试: ${testCase.name}`);
    
    let testProject: TestProject | null = null;
    let beforeSnapshot: Map<string, any> | null = null;
    
    try {
      // 1. 创建测试项目
      testProject = await this.createTestProject(testCase);
      console.log(`📁 测试项目创建于: ${testProject.path}`);
      
      // 2. 创建项目快照（变更前）
      beforeSnapshot = await CodeChangeAnalyzer.createSnapshot(testProject.path);
      
      // 3. 执行 Agent
      const executionInfo = await this.executeAgent(testCase, testProject.path);
      console.log(`⏱️  执行时间: ${executionInfo.duration}ms`);
      
      // 4. 创建项目快照（变更后）
      const afterSnapshot = await CodeChangeAnalyzer.createSnapshot(testProject.path);
      
      // 5. 分析结果
      const result = await this.analyzeResult(testCase, executionInfo, beforeSnapshot, afterSnapshot, testProject.path);
      
      console.log(`${result.status === TestStatus.PASSED ? '✅' : '❌'} 测试${result.status}: ${testCase.name}`);
      console.log(`📊 综合得分: ${(result.overallScore * 100).toFixed(1)}%`);
      
      return result;
      
    } catch (error) {
      console.error(`❌ 测试执行失败: ${error}`);
      
      return TestResultBuilder.create(testCase)
        .withStatus(TestStatus.ERROR)
        .addError(`测试执行失败: ${error}`)
        .build();
        
    } finally {
      // 清理测试项目
      if (testProject && !this.config.keepTestProjects) {
        await testProject.cleanup();
      }
    }
  }

  /**
   * 运行测试套件
   */
  async runSuite(testSuite: TestSuite): Promise<TestSuiteResult> {
    console.log(`🎯 开始测试套件: ${testSuite.name}`);
    const startTime = new Date();
    
    // 执行套件设置
    if (testSuite.setup) {
      await testSuite.setup();
    }
    
    try {
      let testResults: TestResult[];
      
      if (this.config.parallel) {
        testResults = await this.runTestsInParallel(testSuite.testCases);
      } else {
        testResults = await this.runTestsSequentially(testSuite.testCases);
      }
      
      const endTime = new Date();
      const duration = endTime.getTime() - startTime.getTime();
      
      // 统计结果
      const stats = this.calculateStats(testResults);
      
      const suiteResult: TestSuiteResult = {
        suiteId: testSuite.id,
        suiteName: testSuite.name,
        startTime,
        endTime,
        duration,
        ...stats,
        testResults,
        summary: this.generateSummary(testResults)
      };
      
      console.log(`🏁 测试套件完成: ${testSuite.name}`);
      console.log(`📈 通过率: ${((stats.passedTests / stats.totalTests) * 100).toFixed(1)}%`);
      
      return suiteResult;
      
    } finally {
      // 执行套件清理
      if (testSuite.teardown) {
        await testSuite.teardown();
      }
    }
  }

  /**
   * 运行多个测试场景
   */
  async runScenarios(testCases: TestCase[]): Promise<TestSuiteResult> {
    const testSuite: TestSuite = {
      id: `scenarios-${Date.now()}`,
      name: '测试场景集合',
      description: '批量运行的测试场景',
      testCases
    };
    
    return this.runSuite(testSuite);
  }

  /**
   * 创建测试项目
   */
  private async createTestProject(testCase: TestCase): Promise<TestProject> {
    const tempDir = await fs.mkdtemp(
      path.join(this.config.tempDir!, `agent-test-${testCase.id}-`)
    );

    // 根据项目类型创建项目结构
    switch (testCase.projectType) {
      case 'gradle-spring-boot':
        await this.createGradleProject(tempDir);
        break;
      case 'maven-spring-boot':
        await this.createMavenProject(tempDir);
        break;
      case 'npm-node':
        await this.createNpmProject(tempDir);
        break;
      case 'empty':
        // 空项目，不需要额外操作
        break;
    }

    // 添加初始文件
    if (testCase.initialFiles) {
      for (const file of testCase.initialFiles) {
        const filePath = path.join(tempDir, file.path);
        await fs.mkdir(path.dirname(filePath), { recursive: true });
        await fs.writeFile(filePath, file.content, { encoding: (file.encoding || 'utf-8') as BufferEncoding });
      }
    }

    return {
      path: tempDir,
      type: testCase.projectType,
      cleanup: async () => {
        try {
          await fs.rm(tempDir, { recursive: true, force: true });
        } catch (error) {
          console.warn(`清理测试项目失败: ${error}`);
        }
      }
    };
  }

  /**
   * 执行 Agent
   */
  private async executeAgent(testCase: TestCase, projectPath: string): Promise<ExecutionInfo> {
    const startTime = new Date();

    const args = [
      this.config.agentPath,
      'code',
      '--path', projectPath,
      '--task', testCase.task,
      '--max-iterations', testCase.config.maxIterations.toString()
    ];

    if (testCase.config.quiet) {
      args.push('--quiet');
    }

    return new Promise((resolve) => {
      const child = spawn('node', args, {
        stdio: ['pipe', 'pipe', 'pipe'],
        cwd: process.cwd()
      });

      this.runningTests.set(testCase.id, child);

      let stdout = '';
      let stderr = '';
      let iterations = 0;

      child.stdout?.on('data', (data) => {
        stdout += data.toString();
        if (this.config.verbose) {
          process.stdout.write(data);
        }
      });

      child.stderr?.on('data', (data) => {
        stderr += data.toString();
        if (this.config.verbose) {
          process.stderr.write(data);
        }
      });

      const timeoutHandle = setTimeout(() => {
        child.kill('SIGTERM');
        resolve({
          startTime,
          endTime: new Date(),
          duration: Date.now() - startTime.getTime(),
          exitCode: -1,
          stdout,
          stderr: stderr + '\n测试超时',
          iterations,
          timeoutOccurred: true
        });
      }, testCase.config.timeout);

      child.on('close', (code) => {
        clearTimeout(timeoutHandle);
        this.runningTests.delete(testCase.id);

        const endTime = new Date();
        resolve({
          startTime,
          endTime,
          duration: endTime.getTime() - startTime.getTime(),
          exitCode: code || 0,
          stdout,
          stderr,
          iterations,
          timeoutOccurred: false
        });
      });

      child.on('error', (error) => {
        clearTimeout(timeoutHandle);
        this.runningTests.delete(testCase.id);

        const endTime = new Date();
        resolve({
          startTime,
          endTime,
          duration: endTime.getTime() - startTime.getTime(),
          exitCode: -1,
          stdout,
          stderr: stderr + `\n进程错误: ${error.message}`,
          iterations,
          timeoutOccurred: false
        });
      });
    });
  }

  /**
   * 分析测试结果
   */
  private async analyzeResult(
    testCase: TestCase,
    executionInfo: ExecutionInfo,
    beforeSnapshot: Map<string, any>,
    afterSnapshot: Map<string, any>,
    projectPath: string
  ): Promise<TestResult> {
    const builder = TestResultBuilder.create(testCase)
      .withExecutionInfo(executionInfo);

    // 确定测试状态
    let status = TestStatus.PASSED;
    if (executionInfo.timeoutOccurred) {
      status = TestStatus.TIMEOUT;
    } else if (executionInfo.exitCode !== 0) {
      status = TestStatus.FAILED;
    }

    builder.withStatus(status);

    // 分析文件变更
    const fileChanges = await CodeChangeAnalyzer.analyzeFileChanges(
      projectPath,
      beforeSnapshot,
      afterSnapshot
    );

    fileChanges.forEach(change => builder.addFileChange(change));

    // 分析提示词效果
    const toolCalls = ToolCallAnalyzer.analyze(testCase, executionInfo).toolCallDetails;
    const promptAnalysis = PromptAnalyzer.analyze(testCase, executionInfo, toolCalls);
    builder.withPromptAnalysis(promptAnalysis);

    // 分析工具调用
    const toolCallAnalysis = ToolCallAnalyzer.analyze(testCase, executionInfo);
    builder.withToolCallAnalysis(toolCallAnalysis);

    // 分析代码质量
    const codeQuality = await CodeChangeAnalyzer.analyzeCodeQuality(projectPath, fileChanges);
    builder.withCodeQuality(codeQuality);

    // 分析任务完成情况
    const taskCompletion = await CodeChangeAnalyzer.analyzeTaskCompletion(testCase, projectPath, fileChanges);
    builder.withTaskCompletion(taskCompletion);

    // 运行自定义验证
    if (testCase.customValidators) {
      const customResults = [];
      for (const validator of testCase.customValidators) {
        try {
          const passed = await validator.validator(executionInfo);
          customResults.push({
            name: validator.name,
            passed,
            message: validator.description
          });
        } catch (error) {
          customResults.push({
            name: validator.name,
            passed: false,
            message: `验证失败: ${error}`
          });
        }
      }
      // Note: customValidationResults would need to be added to TestResult interface
    }

    // 检查质量阈值
    const result = builder.build();
    this.checkQualityThresholds(testCase, result, builder);

    return builder.build();
  }

  /**
   * 检查质量阈值
   */
  private checkQualityThresholds(testCase: TestCase, result: TestResult, builder: TestResultBuilder): void {
    const thresholds = testCase.qualityThresholds;

    if (result.toolCallAnalysis.toolAccuracy < thresholds.minToolAccuracy) {
      builder.addWarning(`工具使用准确率 ${(result.toolCallAnalysis.toolAccuracy * 100).toFixed(1)}% 低于阈值 ${(thresholds.minToolAccuracy * 100).toFixed(1)}%`);
    }

    if (result.executionInfo.duration > thresholds.maxExecutionTime) {
      builder.addWarning(`执行时间 ${result.executionInfo.duration}ms 超过阈值 ${thresholds.maxExecutionTime}ms`);
    }

    if (result.taskCompletion.completionScore < thresholds.minTaskCompletion) {
      builder.addWarning(`任务完成度 ${(result.taskCompletion.completionScore * 100).toFixed(1)}% 低于阈值 ${(thresholds.minTaskCompletion * 100).toFixed(1)}%`);
    }

    if (result.codeQuality.totalIssues > thresholds.maxCodeIssues) {
      builder.addWarning(`代码问题数量 ${result.codeQuality.totalIssues} 超过阈值 ${thresholds.maxCodeIssues}`);
    }
  }

  /**
   * 顺序运行测试
   */
  private async runTestsSequentially(testCases: TestCase[]): Promise<TestResult[]> {
    const results: TestResult[] = [];

    for (const testCase of testCases) {
      const result = await this.runTest(testCase);
      results.push(result);
    }

    return results;
  }

  /**
   * 并行运行测试
   */
  private async runTestsInParallel(testCases: TestCase[]): Promise<TestResult[]> {
    const concurrency = this.config.maxConcurrency!;
    const results: TestResult[] = [];

    for (let i = 0; i < testCases.length; i += concurrency) {
      const batch = testCases.slice(i, i + concurrency);
      const batchResults = await Promise.all(
        batch.map(testCase => this.runTest(testCase))
      );
      results.push(...batchResults);
    }

    return results;
  }

  /**
   * 计算统计信息
   */
  private calculateStats(testResults: TestResult[]) {
    const totalTests = testResults.length;
    const passedTests = testResults.filter(r => r.status === TestStatus.PASSED).length;
    const failedTests = testResults.filter(r => r.status === TestStatus.FAILED).length;
    const skippedTests = testResults.filter(r => r.status === TestStatus.SKIPPED).length;
    const errorTests = testResults.filter(r => r.status === TestStatus.ERROR).length;

    return {
      totalTests,
      passedTests,
      failedTests,
      skippedTests,
      errorTests
    };
  }

  /**
   * 生成测试摘要
   */
  private generateSummary(testResults: TestResult[]) {
    const averageScore = testResults.reduce((sum, r) => sum + r.overallScore, 0) / testResults.length;
    const averageExecutionTime = testResults.reduce((sum, r) => sum + r.executionInfo.duration, 0) / testResults.length;

    // 统计最常见的问题
    const allIssues = testResults.flatMap(r => [...r.errors, ...r.warnings]);
    const issueCount = new Map<string, number>();
    allIssues.forEach(issue => {
      issueCount.set(issue, (issueCount.get(issue) || 0) + 1);
    });
    const mostCommonIssues = Array.from(issueCount.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([issue]) => issue);

    // 统计工具使用情况
    const toolUsageStats: Record<string, number> = {};
    testResults.forEach(r => {
      r.toolCallAnalysis.uniqueTools.forEach(tool => {
        toolUsageStats[tool] = (toolUsageStats[tool] || 0) + 1;
      });
    });

    // 按类别统计
    const categoryStats: Record<string, { passed: number; total: number }> = {};
    testResults.forEach(r => {
      const category = r.testCase.category;
      if (!categoryStats[category]) {
        categoryStats[category] = { passed: 0, total: 0 };
      }
      categoryStats[category].total++;
      if (r.status === TestStatus.PASSED) {
        categoryStats[category].passed++;
      }
    });

    return {
      averageScore,
      averageExecutionTime,
      mostCommonIssues,
      toolUsageStats,
      categoryStats
    };
  }

  /**
   * 创建 Gradle Spring Boot 项目
   */
  private async createGradleProject(projectPath: string): Promise<void> {
    // 创建目录结构
    await fs.mkdir(path.join(projectPath, 'src/main/java/com/example'), { recursive: true });
    await fs.mkdir(path.join(projectPath, 'src/main/resources'), { recursive: true });
    await fs.mkdir(path.join(projectPath, 'src/test/java/com/example'), { recursive: true });

    // 创建 build.gradle.kts
    const buildGradle = `
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
`.trim();

    await fs.writeFile(path.join(projectPath, 'build.gradle.kts'), buildGradle);

    // 创建 settings.gradle.kts
    await fs.writeFile(path.join(projectPath, 'settings.gradle.kts'), 'rootProject.name = "test-project"');

    // 创建主应用类
    const mainClass = `
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
`.trim();

    await fs.writeFile(path.join(projectPath, 'src/main/java/com/example/TestApplication.java'), mainClass);

    // 创建 application.properties
    const appProperties = `
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
`.trim();

    await fs.writeFile(path.join(projectPath, 'src/main/resources/application.properties'), appProperties);
  }

  /**
   * 创建 Maven Spring Boot 项目
   */
  private async createMavenProject(projectPath: string): Promise<void> {
    // 创建目录结构
    await fs.mkdir(path.join(projectPath, 'src/main/java/com/example'), { recursive: true });
    await fs.mkdir(path.join(projectPath, 'src/main/resources'), { recursive: true });
    await fs.mkdir(path.join(projectPath, 'src/test/java/com/example'), { recursive: true });

    // 创建 pom.xml
    const pomXml = `
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.2.0</spring.boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>\${spring.boot.version}</version>
        </dependency>
    </dependencies>
</project>
`.trim();

    await fs.writeFile(path.join(projectPath, 'pom.xml'), pomXml);
  }

  /**
   * 创建 NPM Node.js 项目
   */
  private async createNpmProject(projectPath: string): Promise<void> {
    const packageJson = {
      name: "test-project",
      version: "1.0.0",
      description: "Test project for AI agent testing",
      main: "index.js",
      scripts: {
        test: "echo \"Error: no test specified\" && exit 1"
      },
      dependencies: {},
      devDependencies: {}
    };

    await fs.writeFile(path.join(projectPath, 'package.json'), JSON.stringify(packageJson, null, 2));
    await fs.writeFile(path.join(projectPath, 'index.js'), 'console.log("Hello World");');
  }

  /**
   * 停止所有运行中的测试
   */
  async stopAllTests(): Promise<void> {
    const promises = Array.from(this.runningTests.entries()).map(([testId, process]) => {
      return new Promise<void>((resolve) => {
        process.kill('SIGTERM');
        process.on('close', () => {
          this.runningTests.delete(testId);
          resolve();
        });

        // 强制终止的超时
        setTimeout(() => {
          if (this.runningTests.has(testId)) {
            process.kill('SIGKILL');
            this.runningTests.delete(testId);
          }
          resolve();
        }, 5000);
      });
    });

    await Promise.all(promises);
  }
}

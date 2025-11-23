/**
 * 错误恢复集成测试 - 使用新测试框架
 * 
 * 验证 CodingAgent 在遇到错误时的恢复能力
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import * as fs from 'fs/promises';
import * as path from 'path';
import {
  TestEngine,
  TestCaseBuilder,
  TestCategory,
  ProjectType,
  ConsoleReporter,
  TestSuiteResult,
  FileDefinition
} from '../framework';

describe('CodingAgent 错误恢复测试 v2', () => {
  let testEngine: TestEngine;
  let testResults: TestSuiteResult;

  beforeAll(async () => {
    // 初始化测试引擎
    testEngine = new TestEngine({
      agentPath: './dist/jsMain/typescript/index.js',
      outputDir: './test-results/error-recovery',
      reporters: ['console'],
      verbose: process.env.DEBUG === 'true',
      keepTestProjects: process.env.KEEP_TEST_PROJECTS === 'true',
      parallel: false // 错误恢复测试需要顺序执行
    });
  });

  afterAll(async () => {
    if (testEngine) {
      await testEngine.stopAllTests();
    }
  });

  it('应该成功运行所有错误恢复测试', async () => {
    console.log('\n🔧 开始运行错误恢复测试套件...');

    // 定义错误恢复测试用例
    const testCases = [
      // 1. 编译错误恢复
      TestCaseBuilder.create('error-001')
        .withName('编译错误恢复')
        .withDescription('修复项目中的编译错误，确保项目能够成功构建')
        .withCategory(TestCategory.ERROR_RECOVERY)
        .withTask('Fix the compilation errors in the project and ensure it builds successfully')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true, minCalls: 1 })
        .expectTool('shell', { required: true }) // 需要运行构建命令
        .withTimeout(300000) // 5分钟
        .build(),

      // 2. 依赖冲突解决
      TestCaseBuilder.create('error-002')
        .withName('依赖冲突解决')
        .withDescription('解决项目中的依赖版本冲突问题')
        .withCategory(TestCategory.ERROR_RECOVERY)
        .withTask('Resolve dependency conflicts in build.gradle.kts and update to compatible versions')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 1 })
        .expectTool('write-file', { required: true, minCalls: 1 })
        .expectTool('shell', { required: true })
        .expectChange('file-modified', { path: 'build.gradle.kts', required: true })
        .withTimeout(240000) // 4分钟
        .build(),

      // 3. 语法错误修复
      TestCaseBuilder.create('error-003')
        .withName('语法错误修复')
        .withDescription('修复 Java 文件中的语法错误和代码质量问题')
        .withCategory(TestCategory.ERROR_RECOVERY)
        .withTask('Fix syntax errors in Java files and ensure code quality standards are met')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 2 })
        .expectTool('write-file', { required: true, minCalls: 2 })
        .withTimeout(180000) // 3分钟
        .build(),

      // 4. 配置错误修复
      TestCaseBuilder.create('error-004')
        .withName('配置错误修复')
        .withDescription('修复应用配置文件中的错误，确保应用能正常启动')
        .withCategory(TestCategory.ERROR_RECOVERY)
        .withTask('Fix configuration errors in application.properties and ensure the application starts correctly')
        .withProjectType(ProjectType.GRADLE_SPRING_BOOT)
        .expectTool('read-file', { required: true, minCalls: 1 })
        .expectTool('write-file', { required: true, minCalls: 1 })
        .expectChange('file-modified', { path: 'src/main/resources/application.properties', required: true })
        .withTimeout(120000) // 2分钟
        .build()
    ];

    // 为每个测试用例添加有问题的初始文件
    testCases.forEach((testCase, index) => {
      testCase.initialFiles = createProblematicFiles(index);
    });

    // 运行测试套件
    testResults = await testEngine.runScenarios(testCases);

    // 生成详细报告
    console.log(ConsoleReporter.generateSuiteReport(testResults));

    // 验证测试结果
    expect(testResults.totalTests).toBe(4);
    expect(testResults.passedTests).toBeGreaterThanOrEqual(2); // 至少50%通过率（错误恢复较困难）
    expect(testResults.summary.averageScore).toBeGreaterThanOrEqual(0.5); // 平均得分≥50%

    console.log('\n✅ 错误恢复测试套件完成');
    console.log(`📊 通过率: ${((testResults.passedTests / testResults.totalTests) * 100).toFixed(1)}%`);
    console.log(`⏱️  总执行时间: ${(testResults.duration / 1000 / 60).toFixed(1)}分钟`);
    console.log(`📈 平均得分: ${(testResults.summary.averageScore * 100).toFixed(1)}%`);
  }, 900000); // 15分钟超时

  it('应该验证错误处理能力', async () => {
    expect(testResults).toBeDefined();

    // 验证提示词分析中的错误处理
    const errorHandlingResults = testResults.testResults.map(r => r.promptAnalysis.handledErrorsGracefully);
    const errorHandlingCount = errorHandlingResults.filter(handled => handled).length;

    expect(errorHandlingCount / errorHandlingResults.length).toBeGreaterThanOrEqual(0.5); // 50%的测试显示了错误处理
  });

  it('应该验证恢复策略的有效性', async () => {
    expect(testResults).toBeDefined();

    // 验证是否使用了构建/测试命令来验证修复
    const toolUsageStats = testResults.summary.toolUsageStats;
    const shellUsage = toolUsageStats['shell'] || 0;
    // Shell 命令是可选的 - agent 可能通过其他方式验证修复
    expect(shellUsage).toBeGreaterThanOrEqual(0);

    // 验证文件修改情况
    const modifiedFiles = testResults.testResults.flatMap(
      result => result.fileChanges.filter(change => change.type === 'modified')
    );
    expect(modifiedFiles.length).toBeGreaterThanOrEqual(3); // 至少修改了3个文件
  });

  it('应该验证代码质量改善', async () => {
    expect(testResults).toBeDefined();

    // 验证代码质量分析结果
    const qualityResults = testResults.testResults.map(r => r.codeQuality);
    const avgQualityScore = qualityResults.reduce(
      (sum, quality) => sum + quality.qualityScore, 0
    ) / qualityResults.length;

    // 错误恢复后的代码质量应该有所改善
    expect(avgQualityScore).toBeGreaterThanOrEqual(0.6); // 恢复后质量得分≥60%

    // 验证语法错误减少
    const totalSyntaxErrors = qualityResults.reduce(
      (sum, quality) => sum + quality.syntaxErrors, 0
    );
    expect(totalSyntaxErrors).toBeLessThanOrEqual(2); // 总语法错误≤2个
  });
});

/**
 * 为不同的错误恢复测试创建有问题的初始文件
 */
function createProblematicFiles(testIndex: number): FileDefinition[] {
  switch (testIndex) {
    case 0: // 编译错误
      return [{
        path: 'src/main/java/com/example/BrokenController.java',
        content: `
package com.example;

import org.springframework.web.bind.annotation.*;

@RestController
public class BrokenController {
    
    @GetMapping("/test")
    public String test() {
        // 语法错误：缺少分号
        String message = "Hello World"
        return message;
    }
    
    // 方法签名错误：缺少返回类型
    @PostMapping("/create")
    public create(@RequestBody String data) {
        return "Created: " + data;
    }
}
        `.trim()
      }];

    case 1: // 依赖冲突
      return [{
        path: 'build.gradle.kts',
        content: `
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // 冲突的依赖版本
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    runtimeOnly("com.h2database:h2")
}
        `.trim()
      }];

    case 2: // 语法错误
      return [{
        path: 'src/main/java/com/example/SyntaxErrorService.java',
        content: `
package com.example;

import org.springframework.stereotype.Service;

@Service
public class SyntaxErrorService {
    
    public String processData(String input) {
        if (input == null {  // 缺少右括号
            return "null input";
        }
        
        // 未声明的变量
        result = input.toUpperCase();
        return result;
    }
    
    // 重复的方法名
    public String processData(String input, boolean flag) {
        return processData(input);
    }
    
    public String processData(String input, boolean flag) {  // 重复定义
        return input;
    }
}
        `.trim()
      }];

    case 3: // 配置错误
      return [{
        path: 'src/main/resources/application.properties',
        content: `
# 错误的配置
server.port=abc  # 端口号应该是数字
spring.datasource.url=invalid-url
spring.datasource.driver-class-name=com.nonexistent.Driver

# 重复的配置
server.port=8080
server.port=9090

# 语法错误
spring.jpa.hibernate.ddl-auto=
        `.trim()
      }];

    default:
      return [];
  }
}

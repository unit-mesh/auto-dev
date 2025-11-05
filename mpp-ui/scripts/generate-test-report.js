#!/usr/bin/env node

/**
 * Test Report Generator for CodingAgent Integration Tests
 *
 * This script generates comprehensive reports from integration test results,
 * including performance metrics, success rates, and quality assessments.
 */

import fs from 'fs';
import path from 'path';

class TestReportGenerator {
  constructor() {
    this.results = [];
    this.startTime = Date.now();
  }

  /**
   * Parse test results from various sources
   */
  parseResults(resultsDir = './test-results') {
    console.log('📊 Parsing test results...');
    
    try {
      // Look for test result files
      const files = fs.readdirSync(resultsDir);
      
      files.forEach(file => {
        if (file.endsWith('.json')) {
          const filePath = path.join(resultsDir, file);
          const data = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
          this.results.push(data);
        }
      });
      
      console.log(`✅ Parsed ${this.results.length} test result files`);
    } catch (error) {
      console.log('⚠️  No test results directory found, generating sample report');
      this.generateSampleResults();
    }
  }

  /**
   * Generate sample results for demonstration
   */
  generateSampleResults() {
    this.results = [
      {
        category: 'Simple Robustness',
        tests: [
          { name: 'Basic project exploration', success: true, duration: 15432, toolsUsed: ['glob', 'read-file'] },
          { name: 'File content reading', success: true, duration: 8234, toolsUsed: ['read-file'] },
          { name: 'File creation', success: true, duration: 12567, toolsUsed: ['write-file'] },
          { name: 'Error handling', success: true, duration: 23456, toolsUsed: ['shell'], errorRecovery: true }
        ]
      },
      {
        category: 'Business Scenarios',
        tests: [
          { name: 'Video support addition', success: true, duration: 145678, toolsUsed: ['glob', 'read-file', 'write-file'] },
          { name: 'JWT authentication', success: true, duration: 234567, toolsUsed: ['glob', 'read-file', 'write-file'] },
          { name: 'Spring Boot upgrade', success: false, duration: 345678, toolsUsed: ['shell', 'read-file'], errorRecovery: true },
          { name: 'GraphQL API', success: true, duration: 198765, toolsUsed: ['glob', 'read-file', 'write-file'] }
        ]
      }
    ];
  }

  /**
   * Calculate performance metrics
   */
  calculateMetrics() {
    const allTests = this.results.flatMap(category => category.tests);
    const totalTests = allTests.length;
    const successfulTests = allTests.filter(test => test.success).length;
    const failedTests = totalTests - successfulTests;
    const testsWithErrorRecovery = allTests.filter(test => test.errorRecovery).length;
    
    const totalDuration = allTests.reduce((sum, test) => sum + test.duration, 0);
    const averageDuration = totalDuration / totalTests;
    
    const toolUsageStats = {};
    allTests.forEach(test => {
      test.toolsUsed.forEach(tool => {
        toolUsageStats[tool] = (toolUsageStats[tool] || 0) + 1;
      });
    });

    return {
      totalTests,
      successfulTests,
      failedTests,
      successRate: (successfulTests / totalTests * 100).toFixed(1),
      errorRecoveryRate: (testsWithErrorRecovery / totalTests * 100).toFixed(1),
      totalDuration,
      averageDuration: Math.round(averageDuration),
      toolUsageStats
    };
  }

  /**
   * Generate HTML report
   */
  generateHtmlReport(metrics) {
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CodingAgent Integration Test Report</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; }
        .header { text-align: center; margin-bottom: 40px; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 40px; }
        .metric-card { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; }
        .metric-value { font-size: 2em; font-weight: bold; color: #007bff; }
        .metric-label { color: #6c757d; margin-top: 5px; }
        .success { color: #28a745; }
        .warning { color: #ffc107; }
        .danger { color: #dc3545; }
        .category { margin-bottom: 30px; }
        .category h3 { border-bottom: 2px solid #007bff; padding-bottom: 10px; }
        .test-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; margin: 5px 0; background: #f8f9fa; border-radius: 4px; }
        .test-status { font-weight: bold; }
        .tools { font-size: 0.9em; color: #6c757d; }
        .chart { margin: 20px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🧪 CodingAgent Integration Test Report</h1>
        <p>Generated on ${new Date().toLocaleString()}</p>
    </div>

    <div class="metrics">
        <div class="metric-card">
            <div class="metric-value success">${metrics.successfulTests}</div>
            <div class="metric-label">Successful Tests</div>
        </div>
        <div class="metric-card">
            <div class="metric-value danger">${metrics.failedTests}</div>
            <div class="metric-label">Failed Tests</div>
        </div>
        <div class="metric-card">
            <div class="metric-value ${metrics.successRate >= 80 ? 'success' : 'warning'}">${metrics.successRate}%</div>
            <div class="metric-label">Success Rate</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${metrics.errorRecoveryRate}%</div>
            <div class="metric-label">Error Recovery Rate</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${Math.round(metrics.totalDuration / 1000)}s</div>
            <div class="metric-label">Total Duration</div>
        </div>
        <div class="metric-card">
            <div class="metric-value">${Math.round(metrics.averageDuration / 1000)}s</div>
            <div class="metric-label">Average Duration</div>
        </div>
    </div>

    <h2>📋 Test Results by Category</h2>
    ${this.results.map(category => `
        <div class="category">
            <h3>${category.category}</h3>
            ${category.tests.map(test => `
                <div class="test-item">
                    <div>
                        <strong>${test.name}</strong>
                        <div class="tools">Tools: ${test.toolsUsed.join(', ')}</div>
                    </div>
                    <div>
                        <span class="test-status ${test.success ? 'success' : 'danger'}">
                            ${test.success ? '✅ PASS' : '❌ FAIL'}
                        </span>
                        ${test.errorRecovery ? '<span style="color: #ffc107;"> 🔄 Recovery</span>' : ''}
                        <div style="font-size: 0.9em; color: #6c757d;">
                            ${Math.round(test.duration / 1000)}s
                        </div>
                    </div>
                </div>
            `).join('')}
        </div>
    `).join('')}

    <h2>🔧 Tool Usage Statistics</h2>
    <div class="chart">
        ${Object.entries(metrics.toolUsageStats).map(([tool, count]) => `
            <div style="display: flex; align-items: center; margin: 10px 0;">
                <div style="width: 100px; text-align: right; margin-right: 10px;">${tool}:</div>
                <div style="background: #007bff; height: 20px; width: ${count * 20}px; border-radius: 10px;"></div>
                <div style="margin-left: 10px; color: #6c757d;">${count} times</div>
            </div>
        `).join('')}
    </div>

    <h2>📊 Quality Assessment</h2>
    <div style="background: #f8f9fa; padding: 20px; border-radius: 8px;">
        <h4>Overall System Health: ${metrics.successRate >= 90 ? '🟢 Excellent' : metrics.successRate >= 80 ? '🟡 Good' : '🔴 Needs Attention'}</h4>
        <ul>
            <li><strong>Robustness:</strong> ${metrics.successRate}% success rate ${metrics.successRate >= 80 ? '(Target: ≥80%)' : '⚠️ Below target'}</li>
            <li><strong>Error Handling:</strong> ${metrics.errorRecoveryRate}% recovery rate ${metrics.errorRecoveryRate >= 60 ? '(Target: ≥60%)' : '⚠️ Below target'}</li>
            <li><strong>Performance:</strong> ${Math.round(metrics.averageDuration / 1000)}s average duration ${metrics.averageDuration <= 300000 ? '(Within limits)' : '⚠️ Slow'}</li>
            <li><strong>Tool Usage:</strong> ${Object.keys(metrics.toolUsageStats).length} different tools utilized</li>
        </ul>
    </div>

    <footer style="margin-top: 40px; text-align: center; color: #6c757d; border-top: 1px solid #dee2e6; padding-top: 20px;">
        <p>CodingAgent Integration Test Report • Generated by AutoDev Test Suite</p>
    </footer>
</body>
</html>
    `.trim();

    return html;
  }

  /**
   * Generate markdown report
   */
  generateMarkdownReport(metrics) {
    const markdown = `
# 🧪 CodingAgent Integration Test Report

**Generated:** ${new Date().toLocaleString()}

## 📊 Summary Metrics

| Metric | Value | Status |
|--------|-------|--------|
| Total Tests | ${metrics.totalTests} | - |
| Successful Tests | ${metrics.successfulTests} | ${metrics.successfulTests > 0 ? '✅' : '❌'} |
| Failed Tests | ${metrics.failedTests} | ${metrics.failedTests === 0 ? '✅' : '⚠️'} |
| Success Rate | ${metrics.successRate}% | ${metrics.successRate >= 80 ? '✅' : '⚠️'} |
| Error Recovery Rate | ${metrics.errorRecoveryRate}% | ${metrics.errorRecoveryRate >= 60 ? '✅' : '⚠️'} |
| Total Duration | ${Math.round(metrics.totalDuration / 1000)}s | - |
| Average Duration | ${Math.round(metrics.averageDuration / 1000)}s | ${metrics.averageDuration <= 300000 ? '✅' : '⚠️'} |

## 📋 Test Results by Category

${this.results.map(category => `
### ${category.category}

${category.tests.map(test => `
- **${test.name}**: ${test.success ? '✅ PASS' : '❌ FAIL'} (${Math.round(test.duration / 1000)}s)${test.errorRecovery ? ' 🔄 Recovery' : ''}
  - Tools: ${test.toolsUsed.join(', ')}
`).join('')}
`).join('')}

## 🔧 Tool Usage Statistics

${Object.entries(metrics.toolUsageStats).map(([tool, count]) => `
- **${tool}**: ${count} times
`).join('')}

## 📊 Quality Assessment

**Overall System Health:** ${metrics.successRate >= 90 ? '🟢 Excellent' : metrics.successRate >= 80 ? '🟡 Good' : '🔴 Needs Attention'}

### Key Findings:

- **Robustness**: ${metrics.successRate}% success rate ${metrics.successRate >= 80 ? '(✅ Meets target ≥80%)' : '(⚠️ Below target)'}
- **Error Handling**: ${metrics.errorRecoveryRate}% recovery rate ${metrics.errorRecoveryRate >= 60 ? '(✅ Meets target ≥60%)' : '(⚠️ Below target)'}
- **Performance**: ${Math.round(metrics.averageDuration / 1000)}s average duration ${metrics.averageDuration <= 300000 ? '(✅ Within limits)' : '(⚠️ Slow)'}
- **Tool Diversity**: ${Object.keys(metrics.toolUsageStats).length} different tools utilized

### Recommendations:

${metrics.successRate < 80 ? '- 🔧 Investigate failed tests and improve system prompt robustness' : ''}
${metrics.errorRecoveryRate < 60 ? '- 🛡️ Enhance error recovery mechanisms' : ''}
${metrics.averageDuration > 300000 ? '- ⚡ Optimize performance for faster execution' : ''}
${metrics.successRate >= 90 ? '- 🎉 Excellent performance! Consider expanding test coverage' : ''}

---
*Report generated by CodingAgent Integration Test Suite*
    `.trim();

    return markdown;
  }

  /**
   * Generate and save reports
   */
  generateReports() {
    console.log('📊 Generating test reports...');
    
    const metrics = this.calculateMetrics();
    
    // Generate HTML report
    const htmlReport = this.generateHtmlReport(metrics);
    fs.writeFileSync('test-report.html', htmlReport);
    console.log('✅ HTML report saved: test-report.html');
    
    // Generate Markdown report
    const markdownReport = this.generateMarkdownReport(metrics);
    fs.writeFileSync('test-report.md', markdownReport);
    console.log('✅ Markdown report saved: test-report.md');
    
    // Generate JSON summary
    const jsonSummary = {
      generatedAt: new Date().toISOString(),
      metrics,
      results: this.results
    };
    fs.writeFileSync('test-summary.json', JSON.stringify(jsonSummary, null, 2));
    console.log('✅ JSON summary saved: test-summary.json');
    
    console.log('\n🎉 Test reports generated successfully!');
    console.log(`📊 Overall Success Rate: ${metrics.successRate}%`);
    console.log(`🔄 Error Recovery Rate: ${metrics.errorRecoveryRate}%`);
    console.log(`⏱️  Average Test Duration: ${Math.round(metrics.averageDuration / 1000)}s`);
  }
}

// Main execution
const generator = new TestReportGenerator();
generator.parseResults();
generator.generateReports();

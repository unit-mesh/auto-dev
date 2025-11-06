/**
 * Domain Dictionary Utilities
 *
 * Provides TypeScript-friendly interface to the Kotlin domain dictionary generation functionality
 */

import type { LegacyConfig } from '../config/ConfigManager.js';
import * as fs from 'fs';
import * as path from 'path';

// Import the compiled Kotlin/JS module
// @ts-ignore - Kotlin/JS generated module
import MppCore from '@autodev/mpp-core';

// Access the exported Kotlin/JS classes
const { JsDomainDictGenerator, JsModelConfig } = MppCore.cc.unitmesh.llm;

/**
 * Result of domain dictionary generation
 */
export interface DomainDictResult {
  success: boolean;
  content: string;
  errorMessage?: string;
}

/**
 * Domain Dictionary Generator Service
 */
export class DomainDictService {
  private generator: any;

  constructor(
    private projectPath: string,
    private config: LegacyConfig,
    private maxTokenLength: number = 8192
  ) {
    // Create Kotlin model config
    const modelConfig = new JsModelConfig(
      config.provider,
      config.model,
      config.apiKey || '',
      config.temperature || 0.7,
      config.maxTokens || 8192,
      config.baseUrl || ''
    );

    // Create domain dictionary generator
    this.generator = new JsDomainDictGenerator(
      projectPath,
      modelConfig,
      maxTokenLength
    );
  }

  /**
   * Generate domain dictionary and return complete result
   */
  async generate(): Promise<string> {
    try {
      return await this.generator.generate();
    } catch (error) {
      throw new Error(`Domain dictionary generation failed: ${error instanceof Error ? error.message : String(error)}`);
    }
  }

  /**
   * Generate and save domain dictionary to file
   */
  async generateAndSave(): Promise<DomainDictResult> {
    try {
      const result = await this.generator.generateAndSave();
      return {
        success: result.success,
        content: result.content,
        errorMessage: result.errorMessage
      };
    } catch (error) {
      return {
        success: false,
        content: '',
        errorMessage: error instanceof Error ? error.message : String(error)
      };
    }
  }

  /**
   * Check if domain dictionary file exists
   */
  async exists(): Promise<boolean> {
    try {
      return await this.generator.exists();
    } catch (error) {
      return false;
    }
  }

  /**
   * Load existing domain dictionary content
   */
  async loadContent(): Promise<string | null> {
    try {
      return await this.generator.loadContent();
    } catch (error) {
      return null;
    }
  }

  /**
   * Create a domain dictionary service instance
   */
  static create(
    projectPath: string,
    config: LegacyConfig,
    maxTokenLength: number = 8192
  ): DomainDictService {
    if (!isValidProjectPath(projectPath)) {
      throw new Error(`Invalid project path: ${projectPath}`);
    }
    return new DomainDictService(projectPath, config, maxTokenLength);
  }
}

/**
 * Utility function to get current working directory
 */
export function getCurrentProjectPath(): string {
  return process.cwd();
}

/**
 * Utility function to validate if a path looks like a valid project directory
 */
export function isValidProjectPath(projectPath: string): boolean {
  try {
    // Check if directory exists
    if (!fs.existsSync(projectPath)) {
      return false;
    }

    // Check if it's a directory
    const stat = fs.statSync(projectPath);
    if (!stat.isDirectory()) {
      return false;
    }

    // Check for common project indicators
    const projectIndicators = [
      'package.json',
      'build.gradle',
      'build.gradle.kts',
      'pom.xml',
      'Cargo.toml',
      'pyproject.toml',
      'requirements.txt',
      '.git'
    ];

    return projectIndicators.some(indicator =>
      fs.existsSync(path.join(projectPath, indicator))
    );
  } catch (error) {
    return false;
  }
}

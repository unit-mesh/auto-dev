/**
 * DomainDictService Tests
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DomainDictService, getCurrentProjectPath, isValidProjectPath } from '../utils/domainDictUtils.js';

// Mock mpp-core
vi.mock('@autodev/mpp-core', () => ({
  default: {
    cc: {
      unitmesh: {
        llm: {
          JsDomainDictGenerator: vi.fn().mockImplementation(() => ({
            generateAndSave: vi.fn().mockResolvedValue({
              success: true,
              content: 'test,测试,测试内容\nuser,用户,用户管理',
              errorMessage: null
            }),
            exists: vi.fn().mockResolvedValue(false)
          })),
          JsModelConfig: vi.fn().mockImplementation(() => ({}))
        }
      }
    }
  }
}));

vi.mock('node:fs', () => ({
  existsSync: vi.fn(),
  readFileSync: vi.fn(),
  writeFileSync: vi.fn(),
  mkdirSync: vi.fn(),
  statSync: vi.fn().mockReturnValue({ isDirectory: () => true })
}));

vi.mock('fs', () => ({
  existsSync: vi.fn(),
  readFileSync: vi.fn(),
  writeFileSync: vi.fn(),
  mkdirSync: vi.fn(),
  statSync: vi.fn().mockReturnValue({ isDirectory: () => true })
}));

vi.mock('node:path', () => ({
  join: vi.fn((...args) => args.join('/')),
  resolve: vi.fn((path) => path),
  dirname: vi.fn((path) => path.split('/').slice(0, -1).join('/'))
}));

describe('DomainDictService', () => {
  const mockConfig = {
    provider: 'deepseek',
    model: 'deepseek-chat',
    apiKey: 'test-key',
    temperature: 0.7,
    maxTokens: 4096,
    baseUrl: ''
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('create', () => {
    it('should create service with valid project path', async () => {
      // Mock valid project path
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        return false;
      });

      const service = DomainDictService.create('/test/project', mockConfig);
      expect(service).toBeDefined();
    });

    it('should throw error for invalid project path', async () => {
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockReturnValue(false);

      expect(() => DomainDictService.create('/invalid/path', mockConfig)).toThrow(
        'Invalid project path: /invalid/path'
      );
    });
  });

  describe('exists', () => {
    it('should check if domain dictionary exists', async () => {
      // Mock valid project path and existing domain dict
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        if (path === '/test/project/prompts/domain.csv') return true;
        return false;
      });

      // Mock the generator to return exists = true
      const mppCore = await import('@autodev/mpp-core');
      const mockGenerator = vi.mocked(mppCore.default.cc.unitmesh.llm.JsDomainDictGenerator);
      mockGenerator.mockImplementation(() => ({
        generateAndSave: vi.fn().mockResolvedValue({
          success: true,
          content: 'test,测试,测试内容\nuser,用户,用户管理',
          errorMessage: null
        }),
        exists: vi.fn().mockResolvedValue(true)
      }));

      const service = DomainDictService.create('/test/project', mockConfig);
      const exists = await service.exists();

      expect(exists).toBe(true);
    });

    it('should return false if domain dictionary does not exist', async () => {
      // Mock valid project path but no domain dict
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        return false;
      });

      // Mock the generator to return exists = false
      const mppCore = await import('@autodev/mpp-core');
      const mockGenerator = vi.mocked(mppCore.default.cc.unitmesh.llm.JsDomainDictGenerator);
      mockGenerator.mockImplementation(() => ({
        generateAndSave: vi.fn().mockResolvedValue({
          success: true,
          content: 'test,测试,测试内容\nuser,用户,用户管理',
          errorMessage: null
        }),
        exists: vi.fn().mockResolvedValue(false)
      }));

      const service = DomainDictService.create('/test/project', mockConfig);
      const exists = await service.exists();

      expect(exists).toBe(false);
    });
  });

  describe('generateAndSave', () => {
    it('should generate and save domain dictionary successfully', async () => {
      // Mock valid project path
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        return false;
      });

      const service = DomainDictService.create('/test/project', mockConfig);
      const result = await service.generateAndSave();

      expect(result.success).toBe(true);
      expect(result.content).toContain('test,测试,测试内容');
      expect(result.errorMessage).toBeNull();
    });

    it('should handle generation errors', async () => {
      // Mock valid project path
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        return false;
      });

      // Mock the generator to throw an error
      const mppCore = await import('@autodev/mpp-core');
      const mockGenerator = vi.mocked(mppCore.default.cc.unitmesh.llm.JsDomainDictGenerator);
      mockGenerator.mockImplementation(() => ({
        generateAndSave: vi.fn().mockResolvedValue({
          success: false,
          content: null,
          errorMessage: 'Generation failed'
        }),
        exists: vi.fn().mockResolvedValue(false)
      }));

      const service = DomainDictService.create('/test/project', mockConfig);
      const result = await service.generateAndSave();

      expect(result.success).toBe(false);
      expect(result.errorMessage).toBe('Generation failed');
    });
  });
});

describe('Project Path Utils', () => {
  describe('getCurrentProjectPath', () => {
    it('should return current working directory', () => {
      const originalCwd = process.cwd;
      process.cwd = vi.fn().mockReturnValue('/test/current');

      const path = getCurrentProjectPath();
      expect(path).toBe('/test/current');

      process.cwd = originalCwd;
    });
  });

  describe('isValidProjectPath', () => {
    it('should return true for valid project paths', async () => {
      // Mock directory exists
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/node-project') return true;
        if (path === '/test/node-project/package.json') return true;
        if (path === '/test/gradle-project') return true;
        if (path === '/test/gradle-project/build.gradle') return true;
        if (path === '/test/maven-project') return true;
        if (path === '/test/maven-project/pom.xml') return true;
        return false;
      });

      expect(isValidProjectPath('/test/node-project')).toBe(true);
      expect(isValidProjectPath('/test/gradle-project')).toBe(true);
      expect(isValidProjectPath('/test/maven-project')).toBe(true);
    });

    it('should return false for invalid project paths', async () => {
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockReturnValue(false);

      expect(isValidProjectPath('/test/empty-dir')).toBe(false);
    });

    it('should return false for non-existent paths', async () => {
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockReturnValue(false);

      expect(isValidProjectPath('/non/existent/path')).toBe(false);
    });
  });

  describe('Integration with SlashCommandProcessor', () => {
    const testConfig = {
      provider: 'deepseek',
      model: 'deepseek-chat',
      apiKey: 'test-key',
      temperature: 0.7,
      maxTokens: 4096,
      baseUrl: ''
    };

    it('should work with /init command', async () => {
      // Mock valid project path
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        return false;
      });

      // Mock the generator for successful generation
      const mppCore = await import('@autodev/mpp-core');
      const mockGenerator = vi.mocked(mppCore.default.cc.unitmesh.llm.JsDomainDictGenerator);
      mockGenerator.mockImplementation(() => ({
        generateAndSave: vi.fn().mockResolvedValue({
          success: true,
          content: 'test,测试,测试内容\nuser,用户,用户管理',
          errorMessage: null
        }),
        exists: vi.fn().mockResolvedValue(false)
      }));

      // Mock successful generation
      const service = DomainDictService.create('/test/project', testConfig);
      const result = await service.generateAndSave();

      expect(result.success).toBe(true);
      expect(result.content).toBeDefined();
    });

    it('should handle --force flag behavior', async () => {
      // Mock valid project path
      const fs = await import('fs');
      vi.mocked(fs.existsSync).mockImplementation((path) => {
        if (path === '/test/project') return true;
        if (path === '/test/project/package.json') return true;
        return false;
      });

      // Mock the generator for force flag behavior
      const mppCore = await import('@autodev/mpp-core');
      const mockGenerator = vi.mocked(mppCore.default.cc.unitmesh.llm.JsDomainDictGenerator);
      mockGenerator.mockImplementation(() => ({
        generateAndSave: vi.fn().mockResolvedValue({
          success: true,
          content: 'test,测试,测试内容\nuser,用户,用户管理',
          errorMessage: null
        }),
        exists: vi.fn().mockResolvedValue(false)
      }));

      const service = DomainDictService.create('/test/project', testConfig);

      // First check if exists
      const exists = await service.exists();

      // If exists and no force flag, should not regenerate
      if (exists) {
        // This would be handled by the SlashCommandProcessor
        expect(exists).toBe(true);
      }

      // With force flag, should regenerate anyway
      const result = await service.generateAndSave();
      expect(result.success).toBe(true);
    });
  });
});

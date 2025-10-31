/**
 * Tests for DevIns compiler integration
 */

import { describe, it, expect } from 'vitest';
import { hasDevInsCommands } from '../utils/commandUtils.js';

describe('DevIns Compiler Integration', () => {
  describe('hasDevInsCommands', () => {
    it('should detect /command: syntax', () => {
      expect(hasDevInsCommands('解释代码 /read-file:build.gradle.kts')).toBe(true);
      expect(hasDevInsCommands('/read-file:src/main.kt')).toBe(true);
      expect(hasDevInsCommands('/write-file:output.txt')).toBe(true);
    });

    it('should detect @agent syntax', () => {
      expect(hasDevInsCommands('@code 解释这段代码')).toBe(true);
      expect(hasDevInsCommands('使用 @architect 设计系统')).toBe(true);
    });

    it('should detect $variable syntax', () => {
      expect(hasDevInsCommands('使用 $projectName')).toBe(true);
      expect(hasDevInsCommands('$var1 和 $var2')).toBe(true);
    });

    it('should return false for normal text', () => {
      expect(hasDevInsCommands('这是普通文本')).toBe(false);
      expect(hasDevInsCommands('explain this code')).toBe(false);
      expect(hasDevInsCommands('hello world')).toBe(false);
    });

    it('should handle mixed commands', () => {
      expect(hasDevInsCommands('@code /read-file:main.kt $projectName')).toBe(true);
    });
  });
});

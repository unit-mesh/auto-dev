/**
 * Tests for StatusBarManager
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock vscode module
vi.mock('vscode', () => ({
  window: {
    createStatusBarItem: vi.fn().mockReturnValue({
      text: '',
      tooltip: '',
      command: '',
      backgroundColor: undefined,
      show: vi.fn(),
      hide: vi.fn(),
      dispose: vi.fn()
    })
  },
  StatusBarAlignment: {
    Left: 1,
    Right: 2
  },
  ThemeColor: vi.fn().mockImplementation((id: string) => ({ id, toString: () => id }))
}));

describe('StatusBarManager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should be importable', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    expect(StatusBarManager).toBeDefined();
  });

  it('should create instance', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(manager).toBeDefined();
  });

  it('should have setState method', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(typeof manager.setState).toBe('function');
  });

  it('should have showMessage method', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(typeof manager.showMessage).toBe('function');
  });

  it('should have dispose method', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(typeof manager.dispose).toBe('function');
  });

  it('should set state to idle', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(() => manager.setState('idle')).not.toThrow();
  });

  it('should set state to thinking', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(() => manager.setState('thinking')).not.toThrow();
    manager.dispose(); // Clean up animation interval
  });

  it('should set state to streaming', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(() => manager.setState('streaming')).not.toThrow();
    manager.dispose(); // Clean up animation interval
  });

  it('should set state to error', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(() => manager.setState('error')).not.toThrow();
  });

  it('should show temporary message', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(() => manager.showMessage('Test message', 100)).not.toThrow();
  });

  it('should dispose without error', async () => {
    const { StatusBarManager } = await import('../../src/services/status-bar');
    const manager = new StatusBarManager();
    expect(() => manager.dispose()).not.toThrow();
  });
});


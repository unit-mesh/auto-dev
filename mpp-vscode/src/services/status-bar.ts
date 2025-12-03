/**
 * Status Bar Service - Shows AutoDev status in VSCode status bar
 */

import * as vscode from 'vscode';

export type StatusBarState = 'idle' | 'thinking' | 'streaming' | 'error';

/**
 * Status Bar Manager for AutoDev
 */
export class StatusBarManager {
  private statusBarItem: vscode.StatusBarItem;
  private state: StatusBarState = 'idle';
  private animationInterval: NodeJS.Timeout | undefined;
  private animationFrame = 0;

  constructor() {
    this.statusBarItem = vscode.window.createStatusBarItem(
      vscode.StatusBarAlignment.Right,
      100
    );
    this.statusBarItem.command = 'autodev.chat';
    this.statusBarItem.tooltip = 'Click to open AutoDev Chat';
    this.updateDisplay();
    this.statusBarItem.show();
  }

  /**
   * Set the status bar state
   */
  setState(state: StatusBarState, message?: string): void {
    this.state = state;
    this.stopAnimation();

    if (state === 'thinking' || state === 'streaming') {
      this.startAnimation();
    }

    this.updateDisplay(message);
  }

  /**
   * Show a temporary message
   */
  showMessage(message: string, timeout = 3000): void {
    const previousState = this.state;
    this.updateDisplay(message);
    
    setTimeout(() => {
      if (this.state === previousState) {
        this.updateDisplay();
      }
    }, timeout);
  }

  /**
   * Dispose the status bar item
   */
  dispose(): void {
    this.stopAnimation();
    this.statusBarItem.dispose();
  }

  private updateDisplay(message?: string): void {
    const icons: Record<StatusBarState, string> = {
      idle: '$(sparkle)',
      thinking: this.getThinkingIcon(),
      streaming: this.getStreamingIcon(),
      error: '$(error)'
    };

    const icon = icons[this.state];
    const text = message || this.getDefaultText();
    
    this.statusBarItem.text = `${icon} ${text}`;
    this.statusBarItem.backgroundColor = this.state === 'error' 
      ? new vscode.ThemeColor('statusBarItem.errorBackground')
      : undefined;
  }

  private getDefaultText(): string {
    switch (this.state) {
      case 'idle':
        return 'AutoDev';
      case 'thinking':
        return 'Thinking...';
      case 'streaming':
        return 'Generating...';
      case 'error':
        return 'Error';
    }
  }

  private getThinkingIcon(): string {
    const frames = ['$(loading~spin)', '$(sync~spin)', '$(gear~spin)'];
    return frames[this.animationFrame % frames.length];
  }

  private getStreamingIcon(): string {
    const frames = ['$(pulse)', '$(radio-tower)', '$(broadcast)'];
    return frames[this.animationFrame % frames.length];
  }

  private startAnimation(): void {
    this.animationFrame = 0;
    this.animationInterval = setInterval(() => {
      this.animationFrame++;
      this.updateDisplay();
    }, 500);
  }

  private stopAnimation(): void {
    if (this.animationInterval) {
      clearInterval(this.animationInterval);
      this.animationInterval = undefined;
    }
    this.animationFrame = 0;
  }
}


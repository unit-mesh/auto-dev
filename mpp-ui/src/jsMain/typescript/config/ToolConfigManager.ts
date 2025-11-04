/**
 * Tool Configuration Manager for CLI
 * 
 * Manages tool configurations including built-in tools and MCP tools
 */

import mppCore from '@autodev/mpp-core';
import { ConfigManager } from './ConfigManager.js';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

const { cc: KotlinCC } = mppCore;

/**
 * Tool configuration interface
 */
export interface ToolConfigFile {
  enabledBuiltinTools: string[];
  enabledMcpTools: string[];
  mcpServers: Record<string, any>;
  chatConfig: {
    temperature: number;
    systemPrompt: string;
    maxTokens: number;
  };
}

/**
 * Tool item interface
 */
export interface ToolItem {
  name: string;
  displayName: string;
  description: string;
  category: string;
  source: 'BUILTIN' | 'MCP';
  enabled: boolean;
  serverName?: string;
}

/**
 * Tool configuration manager
 */
export class ToolConfigManager {
  private static configPath = path.join(os.homedir(), '.autodev', 'mcp.json');
  
  /**
   * Load tool configuration
   */
  static async load(): Promise<ToolConfigFile> {
    try {
      if (fs.existsSync(this.configPath)) {
        const content = fs.readFileSync(this.configPath, 'utf-8');
        return JSON.parse(content);
      }
    } catch (error) {
      console.error('Error loading tool config:', error);
    }
    
    // Return default configuration
    return this.getDefault();
  }
  
  /**
   * Save tool configuration
   */
  static async save(config: ToolConfigFile): Promise<void> {
    try {
      const configDir = path.dirname(this.configPath);
      if (!fs.existsSync(configDir)) {
        fs.mkdirSync(configDir, { recursive: true });
      }
      
      fs.writeFileSync(
        this.configPath,
        JSON.stringify(config, null, 2),
        'utf-8'
      );
      
      console.log(`✅ Configuration saved to: ${this.configPath}`);
    } catch (error) {
      console.error('Error saving tool config:', error);
      throw error;
    }
  }
  
  /**
   * Get default tool configuration
   */
  static getDefault(): ToolConfigFile {
    return {
      enabledBuiltinTools: [
        'read-file',
        'write-file',
        'list-files',
        'edit-file',
        'patch-file',
        'grep',
        'glob',
        'shell',
        'error-recovery',
        'log-summary',
        'codebase-investigator'
      ],
      enabledMcpTools: [],
      mcpServers: {},
      chatConfig: {
        temperature: 0.7,
        systemPrompt: '',
        maxTokens: 4096
      }
    };
  }
  
  /**
   * Get all built-in tools grouped by category
   */
  static getBuiltinToolsByCategory(): Record<string, ToolItem[]> {
    try {
      const result = KotlinCC.unitmesh.agent.config.JsToolConfigManager.getBuiltinToolsByCategory();
      
      // Convert JS object to TypeScript object
      const tools: Record<string, ToolItem[]> = {};
      const categories = Object.keys(result);
      
      for (const category of categories) {
        tools[category] = result[category] || [];
      }
      
      return tools;
    } catch (error) {
      console.error('Error getting builtin tools:', error);
      return {};
    }
  }
  
  /**
   * Get configuration summary
   */
  static async getConfigSummary(): Promise<string> {
    const config = await this.load();
    
    const lines = [
      `📊 Tool Configuration Summary`,
      ``,
      `📁 Config file: ${this.configPath}`,
      ``,
      `🔧 Built-in Tools: ${config.enabledBuiltinTools.length} enabled`,
      ...config.enabledBuiltinTools.map(tool => `  ✓ ${tool}`),
      ``
    ];
    
    if (config.enabledMcpTools.length > 0) {
      lines.push(`🔌 MCP Tools: ${config.enabledMcpTools.length} enabled`);
      config.enabledMcpTools.forEach(tool => {
        lines.push(`  ✓ ${tool}`);
      });
      lines.push('');
    }
    
    const mcpServerNames = Object.keys(config.mcpServers);
    if (mcpServerNames.length > 0) {
      lines.push(`🌐 MCP Servers: ${mcpServerNames.length} configured`);
      mcpServerNames.forEach(name => {
        const server = config.mcpServers[name];
        const status = server.disabled ? '⚠️ disabled' : '✓ enabled';
        lines.push(`  ${status} ${name}`);
      });
      lines.push('');
    }
    
    lines.push(`⚙️  Chat Config:`);
    lines.push(`  Temperature: ${config.chatConfig.temperature}`);
    lines.push(`  Max Tokens: ${config.chatConfig.maxTokens}`);
    
    return lines.join('\n');
  }
  
  /**
   * Enable a tool
   */
  static async enableTool(toolName: string, isMcp: boolean = false): Promise<void> {
    const config = await this.load();
    
    if (isMcp) {
      if (!config.enabledMcpTools.includes(toolName)) {
        config.enabledMcpTools.push(toolName);
      }
    } else {
      if (!config.enabledBuiltinTools.includes(toolName)) {
        config.enabledBuiltinTools.push(toolName);
      }
    }
    
    await this.save(config);
    console.log(`✅ Enabled tool: ${toolName}`);
  }
  
  /**
   * Disable a tool
   */
  static async disableTool(toolName: string, isMcp: boolean = false): Promise<void> {
    const config = await this.load();
    
    if (isMcp) {
      config.enabledMcpTools = config.enabledMcpTools.filter(t => t !== toolName);
    } else {
      config.enabledBuiltinTools = config.enabledBuiltinTools.filter(t => t !== toolName);
    }
    
    await this.save(config);
    console.log(`✅ Disabled tool: ${toolName}`);
  }
  
  /**
   * List all available tools
   */
  static async listTools(): Promise<void> {
    const config = await this.load();
    const toolsByCategory = this.getBuiltinToolsByCategory();
    
    console.log('\n📦 Available Built-in Tools:\n');
    
    const categories = Object.keys(toolsByCategory).sort();
    for (const category of categories) {
      console.log(`\n${getCategoryEmoji(category)} ${category}:`);
      
      const tools = toolsByCategory[category];
      for (const tool of tools) {
        const enabled = config.enabledBuiltinTools.includes(tool.name);
        const status = enabled ? '✓' : '○';
        const statusColor = enabled ? '\x1b[32m' : '\x1b[90m';
        console.log(`  ${statusColor}${status}\x1b[0m ${tool.displayName} (${tool.name})`);
        console.log(`     ${tool.description}`);
      }
    }
    
    if (Object.keys(config.mcpServers).length > 0) {
      console.log('\n\n🔌 MCP Tools:\n');
      Object.keys(config.mcpServers).forEach(serverName => {
        const server = config.mcpServers[serverName];
        const status = server.disabled ? '⚠️ disabled' : '✓ enabled';
        console.log(`  ${status} ${serverName}`);
        if (server.command) {
          console.log(`     Command: ${server.command} ${(server.args || []).join(' ')}`);
        }
        if (server.url) {
          console.log(`     URL: ${server.url}`);
        }
      });
    }
    
    console.log('\n');
  }
  
  /**
   * Add MCP server
   */
  static async addMcpServer(
    name: string,
    command?: string,
    args?: string[],
    url?: string
  ): Promise<void> {
    const config = await this.load();
    
    if (!command && !url) {
      throw new Error('Either command or url must be provided');
    }
    
    if (command && url) {
      throw new Error('Cannot specify both command and url');
    }
    
    config.mcpServers[name] = {
      command,
      url,
      args: args || [],
      disabled: false,
      autoApprove: []
    };
    
    await this.save(config);
    console.log(`✅ Added MCP server: ${name}`);
  }
  
  /**
   * Remove MCP server
   */
  static async removeMcpServer(name: string): Promise<void> {
    const config = await this.load();
    
    if (!config.mcpServers[name]) {
      throw new Error(`MCP server not found: ${name}`);
    }
    
    delete config.mcpServers[name];
    
    await this.save(config);
    console.log(`✅ Removed MCP server: ${name}`);
  }
}

/**
 * Get emoji for category
 */
function getCategoryEmoji(category: string): string {
  switch (category) {
    case 'FileSystem':
      return '📁';
    case 'Search':
      return '🔍';
    case 'Execution':
      return '💻';
    case 'SubAgent':
      return '🤖';
    default:
      return '🔧';
  }
}


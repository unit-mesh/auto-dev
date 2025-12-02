/**
 * IDE Server - MCP protocol server for external tool communication
 * 
 * Provides HTTP endpoints for diff operations and workspace context.
 */

import * as vscode from 'vscode';
import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { randomUUID } from 'crypto';
import { Server as HTTPServer } from 'http';
import * as path from 'path';
import * as fs from 'fs/promises';
import * as os from 'os';
import { DiffManager } from './diff-manager';

const IDE_SERVER_PORT_ENV_VAR = 'AUTODEV_IDE_SERVER_PORT';
const IDE_WORKSPACE_PATH_ENV_VAR = 'AUTODEV_IDE_WORKSPACE_PATH';

/**
 * IDE Server for MCP protocol communication
 */
export class IDEServer {
  private server: HTTPServer | undefined;
  private context: vscode.ExtensionContext | undefined;
  private portFile: string | undefined;
  private authToken: string | undefined;

  constructor(
    private readonly log: (message: string) => void,
    private readonly diffManager: DiffManager,
    private readonly port: number
  ) {}

  /**
   * Start the IDE server
   */
  async start(context: vscode.ExtensionContext): Promise<void> {
    this.context = context;
    this.authToken = randomUUID();

    const app = express();
    app.use(express.json({ limit: '10mb' }));

    // CORS - only allow non-browser requests
    app.use(cors({
      origin: (origin, callback) => {
        if (!origin) {
          return callback(null, true);
        }
        return callback(new Error('Request denied by CORS policy.'), false);
      }
    }));

    // Host validation
    app.use((req: Request, res: Response, next: NextFunction) => {
      const host = req.headers.host || '';
      const allowedHosts = [`localhost:${this.port}`, `127.0.0.1:${this.port}`];
      if (!allowedHosts.includes(host)) {
        return res.status(403).json({ error: 'Invalid Host header' });
      }
      next();
    });

    // Auth validation
    app.use((req: Request, res: Response, next: NextFunction) => {
      const authHeader = req.headers.authorization;
      if (!authHeader) {
        this.log('Missing Authorization header');
        return res.status(401).send('Unauthorized');
      }
      
      const parts = authHeader.split(' ');
      if (parts.length !== 2 || parts[0] !== 'Bearer' || parts[1] !== this.authToken) {
        this.log('Invalid auth token');
        return res.status(401).send('Unauthorized');
      }
      next();
    });

    // Health check endpoint
    app.get('/health', (_req: Request, res: Response) => {
      res.json({ status: 'ok', version: '0.1.0' });
    });

    // Get workspace context
    app.get('/context', (_req: Request, res: Response) => {
      const workspaceFolders = vscode.workspace.workspaceFolders;
      const activeEditor = vscode.window.activeTextEditor;
      
      res.json({
        workspaceFolders: workspaceFolders?.map(f => ({
          name: f.name,
          path: f.uri.fsPath
        })) ?? [],
        activeFile: activeEditor?.document.uri.fsPath ?? null,
        selection: activeEditor?.selection ? {
          start: { line: activeEditor.selection.start.line, character: activeEditor.selection.start.character },
          end: { line: activeEditor.selection.end.line, character: activeEditor.selection.end.character }
        } : null
      });
    });

    // Open diff endpoint
    app.post('/diff/open', async (req: Request, res: Response) => {
      try {
        const { filePath, content } = req.body;
        if (!filePath || content === undefined) {
          return res.status(400).json({ error: 'filePath and content are required' });
        }
        
        await this.diffManager.showDiff(filePath, content);
        res.json({ success: true });
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        this.log(`Error opening diff: ${message}`);
        res.status(500).json({ error: message });
      }
    });

    // Close diff endpoint
    app.post('/diff/close', async (req: Request, res: Response) => {
      try {
        const { filePath } = req.body;
        if (!filePath) {
          return res.status(400).json({ error: 'filePath is required' });
        }
        
        const content = await this.diffManager.closeDiffByPath(filePath);
        res.json({ success: true, content });
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        this.log(`Error closing diff: ${message}`);
        res.status(500).json({ error: message });
      }
    });

    // Read file endpoint
    app.post('/file/read', async (req: Request, res: Response) => {
      try {
        const { filePath } = req.body;
        if (!filePath) {
          return res.status(400).json({ error: 'filePath is required' });
        }
        
        const uri = vscode.Uri.file(filePath);
        const content = await vscode.workspace.fs.readFile(uri);
        res.json({ success: true, content: Buffer.from(content).toString('utf8') });
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        res.status(500).json({ error: message });
      }
    });

    // Write file endpoint
    app.post('/file/write', async (req: Request, res: Response) => {
      try {
        const { filePath, content } = req.body;
        if (!filePath || content === undefined) {
          return res.status(400).json({ error: 'filePath and content are required' });
        }

        const uri = vscode.Uri.file(filePath);
        await vscode.workspace.fs.writeFile(uri, Buffer.from(content, 'utf8'));
        res.json({ success: true });
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        res.status(500).json({ error: message });
      }
    });

    // Start server
    return new Promise((resolve, reject) => {
      this.server = app.listen(this.port, '127.0.0.1', async () => {
        this.log(`IDE Server listening on port ${this.port}`);

        // Write port file for external tools
        await this.writePortFile();
        this.syncEnvVars();

        resolve();
      });

      this.server.on('error', (err) => {
        this.log(`IDE Server error: ${err.message}`);
        reject(err);
      });
    });
  }

  /**
   * Stop the IDE server
   */
  async stop(): Promise<void> {
    if (this.server) {
      return new Promise((resolve) => {
        this.server!.close(() => {
          this.log('IDE Server stopped');
          resolve();
        });
      });
    }
  }

  /**
   * Sync environment variables for terminals
   */
  syncEnvVars(): void {
    if (!this.context) return;

    const workspaceFolders = vscode.workspace.workspaceFolders;
    const workspacePath = workspaceFolders && workspaceFolders.length > 0
      ? workspaceFolders.map(f => f.uri.fsPath).join(path.delimiter)
      : '';

    this.context.environmentVariableCollection.replace(
      IDE_SERVER_PORT_ENV_VAR,
      this.port.toString()
    );
    this.context.environmentVariableCollection.replace(
      IDE_WORKSPACE_PATH_ENV_VAR,
      workspacePath
    );
  }

  /**
   * Write port file for external tools to discover the server
   */
  private async writePortFile(): Promise<void> {
    const autodevDir = path.join(os.homedir(), '.autodev');
    this.portFile = path.join(autodevDir, 'ide-server.json');

    const content = JSON.stringify({
      port: this.port,
      authToken: this.authToken,
      workspacePath: vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? '',
      pid: process.pid
    });

    try {
      await fs.mkdir(autodevDir, { recursive: true });
      await fs.writeFile(this.portFile, content);
      await fs.chmod(this.portFile, 0o600);
      this.log(`Port file written to: ${this.portFile}`);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      this.log(`Failed to write port file: ${message}`);
    }
  }
}


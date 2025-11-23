/**
 * Document Mode - Automated document query with DocQL
 * 
 * Similar to ReviewMode, this mode:
 * 1. Scans for documents in the project
 * 2. Registers documents with the DocumentRegistry
 * 3. Uses DocumentAgent with DocQLTool for intelligent querying
 */

import mppCore from '@autodev/mpp-core';
import { semanticChalk } from '../design-system/theme-helpers.js';
import * as fs from 'fs';
import * as path from 'path';

const { cc: KotlinCC } = mppCore;

export interface DocumentOptions {
    projectPath: string;
    query: string;
    documentPath?: string;
}

export interface DocumentResult {
    success: boolean;
    message: string;
    metadata?: {
        documentsScanned?: number;
        documentsRegistered?: number;
    };
}

/**
 * Scan for documents in a directory
 */
function scanDocuments(dirPath: string, extensions: string[] = ['.md', '.pdf', '.docx', '.txt']): string[] {
    const documents: string[] = [];
    
    function scanDir(currentPath: string) {
        try {
            const entries = fs.readdirSync(currentPath, { withFileTypes: true });
            
            for (const entry of entries) {
                const fullPath = path.join(currentPath, entry.name);
                
                // Skip common non-document directories
                if (entry.isDirectory()) {
                    const skipDirs = ['node_modules', '.git', 'build', 'dist', 'target', '.gradle', 'bin'];
                    if (!skipDirs.includes(entry.name) && !entry.name.startsWith('.')) {
                        scanDir(fullPath);
                    }
                } else if (entry.isFile()) {
                    const ext = path.extname(entry.name).toLowerCase();
                    if (extensions.includes(ext)) {
                        documents.push(fullPath);
                    }
                }
            }
        } catch (error: any) {
            // Silently skip inaccessible directories
            console.log(semanticChalk.muted(`⚠️  Skipping ${currentPath}: ${error.message}`));
        }
    }
    
    scanDir(dirPath);
    return documents;
}

/**
 * Register a document with the DocumentRegistry
 */
async function registerDocument(
    filePath: string,
    projectPath: string,
    agent: any
): Promise<boolean> {
    try {
        const relativePath = path.relative(projectPath, filePath);
        const content = fs.readFileSync(filePath, 'utf-8');
        
        const registered = await agent.registerDocument(relativePath, content);
        return registered;
    } catch (error: any) {
        console.log(semanticChalk.warning(`⚠️  Failed to register ${filePath}: ${error.message}`));
        return false;
    }
}

/**
 * Run document query
 */
export async function runDocument(
    options: DocumentOptions,
    llmService: any,
    renderer?: any
): Promise<DocumentResult> {
    const { projectPath, query, documentPath } = options;
    const startTime = Date.now();

    console.log(semanticChalk.info(`\n📄 AutoDev Document Query`));
    console.log(semanticChalk.muted(`Project: ${projectPath}`));
    console.log(semanticChalk.muted(`Query: ${query}`));
    if (documentPath) {
        console.log(semanticChalk.muted(`Document: ${documentPath}`));
    }
    console.log();

    try {
        // ===== STEP 1: Initialize Platform Parsers =====
        console.log(semanticChalk.info('🔧 Initializing document parsers...'));
        KotlinCC.unitmesh.llm.JsDocumentRegistry.initializePlatformParsers();
        console.log(semanticChalk.success('✅ Parsers initialized'));
        console.log();

        // ===== STEP 2: Create Document Agent =====
        const dummyParser = KotlinCC.unitmesh.llm.JsDocumentParserFactory.createParserForFile("dummy.md");
        const agent = new KotlinCC.unitmesh.agent.JsDocumentAgent(
            llmService,
            dummyParser,
            renderer || null,
            null
        );

        // ===== STEP 3: Scan and Register Documents =====
        let documentsScanned = 0;
        let documentsRegistered = 0;

        if (documentPath) {
            // Register specific document
            const fullPath = path.resolve(projectPath, documentPath);
            if (fs.existsSync(fullPath)) {
                console.log(semanticChalk.info(`📖 Reading document: ${documentPath}...`));
                
                const registered = await registerDocument(fullPath, projectPath, agent);
                if (registered) {
                    console.log(semanticChalk.success(`✅ Document registered successfully`));
                    documentsRegistered = 1;
                    documentsScanned = 1;
                } else {
                    console.log(semanticChalk.warning(`⚠️  Failed to register document`));
                }
                console.log();
            } else {
                console.log(semanticChalk.warning(`⚠️  Document file not found: ${fullPath}`));
                console.log();
            }
        } else {
            // Scan entire project for documents (default behavior)
            console.log(semanticChalk.info('📖 Scanning project for documents...'));
            const foundDocs = scanDocuments(projectPath);
            documentsScanned = foundDocs.length;
            console.log(semanticChalk.info(`Found ${foundDocs.length} documents`));
            console.log();

            if (foundDocs.length > 0) {
                console.log(semanticChalk.info('📝 Registering documents...'));
                for (const docPath of foundDocs) {
                    const relativePath = path.relative(projectPath, docPath);
                    console.log(semanticChalk.muted(`  • ${relativePath}`));
                    
                    const registered = await registerDocument(docPath, projectPath, agent);
                    if (registered) {
                        documentsRegistered++;
                    }
                }
                console.log(semanticChalk.success(`✅ Registered ${documentsRegistered}/${documentsScanned} documents`));
                console.log();
            }
        }

        // ===== STEP 4: Execute Document Query =====
        console.log(semanticChalk.info('🧠 Starting document query with AI agent...'));
        console.log();

        const queryStartTime = Date.now();

        const result = await agent.executeTask(
            query,
            documentPath || null,
            // renderer ? (chunk: string) => renderer.renderLLMResponseChunk(chunk) : undefined
        );

        const queryDuration = Date.now() - queryStartTime;
        console.log();
        console.log(semanticChalk.success('✅ Query complete!'));
        console.log(semanticChalk.muted(`⏱️  Time: ${queryDuration}ms`));
        console.log();

        // ===== STEP 5: Display Results =====
        if (result.success) {
            console.log(semanticChalk.info('📊 Result:'));
            console.log();
            console.log(result.message);
            console.log();
        } else {
            console.log(semanticChalk.error(`❌ Query failed: ${result.message}`));
        }

        const totalDuration = Date.now() - startTime;
        console.log(semanticChalk.success('✅ Complete!'));
        console.log(semanticChalk.muted(`⏱️  Total time: ${totalDuration}ms`));
        console.log();

        return {
            success: result.success,
            message: result.message,
            metadata: {
                documentsScanned,
                documentsRegistered
            }
        };

    } catch (error: any) {
        console.error(semanticChalk.error(`❌ Document query failed: ${error.message}`));
        if (error.stack) {
            console.error(semanticChalk.muted(error.stack));
        }
        return {
            success: false,
            message: `Document query failed: ${error.message}`
        };
    }
}

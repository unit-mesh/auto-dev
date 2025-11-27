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
function scanDocuments(dirPath: string, extensions: string[] = [
    '.md', '.pdf', '.docx', '.pptx', '.txt',  // Documents
    '.java', '.kt', '.kts',                    // JVM source code
    '.js', '.ts', '.tsx',                      // JavaScript/TypeScript
    '.py', '.go', '.rs', '.cs'                 // Other languages
]): string[] {
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
 * Result of document registration attempt
 */
interface RegistrationResult {
    success: boolean;
    filePath: string;
    errorType?: string;
}

/**
 * Register a document with the DocumentRegistry
 */
async function registerDocument(
    filePath: string,
    projectPath: string,
    agent: any
): Promise<RegistrationResult> {
    try {
        const relativePath = path.relative(projectPath, filePath);
        const content = fs.readFileSync(filePath, 'utf-8');

        const registered = await agent.registerDocument(relativePath, content);
        return { success: registered, filePath };
    } catch (error: any) {
        // Extract error type for summary (e.g., "Cannot find module 'web-tree-sitter'")
        const errorType = error.message?.split(':')[0] || 'Unknown error';
        return { success: false, filePath, errorType };
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

    // ===== INPUT VALIDATION =====
    // Validate query parameter
    if (!query || query.trim().length === 0) {
        console.error(semanticChalk.error('❌ Error: Query parameter is required and cannot be empty'));
        return {
            success: false,
            message: 'Error: Query parameter is required and cannot be empty.\n\nUsage: document -p <path> -q "<your question>"'
        };
    }

    // Validate project path exists
    if (!fs.existsSync(projectPath)) {
        console.error(semanticChalk.error(`❌ Error: Project path does not exist: ${projectPath}`));
        return {
            success: false,
            message: `Error: Project path does not exist: ${projectPath}\n\nPlease provide a valid directory path.`
        };
    }

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


        // Track failed registrations by error type for summary
        const failedByErrorType: Map<string, number> = new Map();

        if (documentPath) {
            // Register specific document
            const fullPath = path.resolve(projectPath, documentPath);
            if (fs.existsSync(fullPath)) {
                console.log(semanticChalk.info(`📖 Reading document: ${documentPath}...`));

                // Calculate relative path for registration
                const relativePath = path.relative(projectPath, fullPath);
                console.log(semanticChalk.muted(`   Registering as: ${relativePath}`));

                const result = await registerDocument(fullPath, projectPath, agent);
                if (result.success) {
                    console.log(semanticChalk.success(`✅ Document registered successfully`));
                    documentsRegistered = 1;
                    documentsScanned = 1;
                } else {
                    console.log(semanticChalk.warning(`⚠️  Failed to register document: ${result.errorType || 'Unknown error'}`));
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
            console.log(semanticChalk.muted(`   Found ${foundDocs.length} documents`));

            if (foundDocs.length > 0) {
                // Register documents silently - only show progress for large sets
                const showProgress = foundDocs.length > 50;
                let lastProgress = 0;

                for (let i = 0; i < foundDocs.length; i++) {
                    const docPath = foundDocs[i];
                    const result = await registerDocument(docPath, projectPath, agent);
                    if (result.success) {
                        documentsRegistered++;
                    } else if (result.errorType) {
                        // Track errors by type for summary
                        const count = failedByErrorType.get(result.errorType) || 0;
                        failedByErrorType.set(result.errorType, count + 1);
                    }

                    // Show progress every 25% for large document sets
                    if (showProgress) {
                        const progress = Math.floor((i + 1) / foundDocs.length * 100);
                        if (progress >= lastProgress + 25) {
                            process.stdout.write(`\r   Registering... ${progress}%`);
                            lastProgress = progress;
                        }
                    }
                }

                if (showProgress) {
                    process.stdout.write('\r                          \r');  // Clear progress line
                }
                console.log(semanticChalk.success(`✅ Registered ${documentsRegistered}/${documentsScanned} documents`));
                
                // Show summary of failures if any
                if (failedByErrorType.size > 0) {
                    const totalFailed = documentsScanned - documentsRegistered;
                    console.log(semanticChalk.muted(`   ${totalFailed} documents skipped (code parsing requires tree-sitter)`));
                }
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

        // Note: The LLM response is already streamed during execution,
        // so we don't need to print result.message again to avoid duplication.
        // Only show the final status.
        if (result.success) {
            console.log(semanticChalk.success('✅ Query complete!'));
            console.log(semanticChalk.muted(`⏱️  Total time: ${queryDuration}ms`));
        } else {
            console.log(semanticChalk.error(`❌ Query failed: ${result.message}`));
            console.log(semanticChalk.muted(`⏱️  Time: ${queryDuration}ms`));
        }
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

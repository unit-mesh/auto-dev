/**
 * JavaScript Integration Test for Tool Template Generation
 * Tests the complete flow from tool registry to template generation with JSON Schema format
 */

const { describe, it, expect, beforeAll } = require('@jest/globals');
const path = require('path');

// Import the compiled mpp-core module
const mppCore = require('../../build/packages/js/autodev-mpp-core');

describe('JS Tool Template Integration Tests', () => {
    let JsToolRegistry;
    let JsCodingAgentContextBuilder;
    let JsCodingAgentPromptRenderer;
    
    beforeAll(() => {
        // Extract the required classes from the module
        JsToolRegistry = mppCore.cc.unitmesh.llm.JsToolRegistry;
        JsCodingAgentContextBuilder = mppCore.cc.unitmesh.agent.JsCodingAgentContextBuilder;
        JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;
        
        expect(JsToolRegistry).toBeDefined();
        expect(JsCodingAgentContextBuilder).toBeDefined();
        expect(JsCodingAgentPromptRenderer).toBeDefined();
    });
    
    describe('Tool Registry and JSON Schema Generation', () => {
        let toolRegistry;
        let toolList;
        
        beforeAll(() => {
            toolRegistry = new JsToolRegistry('/test/project');
            toolList = toolRegistry.formatToolListForAI();
        });
        
        it('should create tool registry successfully', () => {
            expect(toolRegistry).toBeDefined();
            expect(toolRegistry.getAvailableTools()).toBeDefined();
            expect(toolRegistry.getAvailableTools().length).toBeGreaterThan(0);
        });
        
        it('should generate tool list with JSON Schema format', () => {
            expect(toolList).toBeDefined();
            expect(toolList.length).toBeGreaterThan(1000);
            
            // Check JSON Schema format
            expect(toolList).toContain('## '); // Markdown headers
            expect(toolList).toContain('```json'); // JSON Schema blocks
            expect(toolList).toContain('"$schema"'); // Schema field
            expect(toolList).toContain('draft-07/schema#'); // Draft-07 schema
            expect(toolList).toContain('"type": "object"'); // Object type
            expect(toolList).toContain('"properties"'); // Properties field
            expect(toolList).toContain('"required"'); // Required field
            expect(toolList).toContain('"additionalProperties"'); // Additional properties
            
            // Should not contain XML format
            expect(toolList).not.toContain('<tool name=');
            expect(toolList).not.toContain('<parameters>');
            
            // Should contain examples
            expect(toolList).toContain('**Example:**');
        });
        
        it('should contain all expected tools', () => {
            const expectedTools = ['read-file', 'write-file', 'shell', 'grep', 'glob'];
            
            expectedTools.forEach(toolName => {
                expect(toolList).toContain(`## ${toolName}`);
            });
        });
        
        it('should have detailed parameter information for read-file', () => {
            const readFileStart = toolList.indexOf('## read-file');
            const readFileEnd = toolList.indexOf('## ', readFileStart + 1);
            const readFileSection = readFileEnd > 0 
                ? toolList.substring(readFileStart, readFileEnd)
                : toolList.substring(readFileStart);
            
            // Check parameter details
            expect(readFileSection).toContain('"path"');
            expect(readFileSection).toContain('"type": "string"');
            expect(readFileSection).toContain('"startLine"');
            expect(readFileSection).toContain('"type": "integer"');
            expect(readFileSection).toContain('"minimum"');
            expect(readFileSection).toContain('"default"');
            expect(readFileSection).toContain('"description"');
        });
    });
    
    describe('Complete Template Generation', () => {
        let context;
        let template;
        
        beforeAll(() => {
            const toolRegistry = new JsToolRegistry('/test/project');
            const toolList = toolRegistry.formatToolListForAI();
            
            const builder = new JsCodingAgentContextBuilder();
            context = builder
                .setProjectPath('/test/project')
                .setOsInfo('macOS 14.0')
                .setTimestamp(new Date().toISOString())
                .setToolList(toolList)
                .setBuildTool('gradle')
                .setShell('/bin/zsh')
                .build();
            
            const renderer = new JsCodingAgentPromptRenderer();
            template = renderer.render(context, 'EN');
        });
        
        it('should create context successfully', () => {
            expect(context).toBeDefined();
            expect(context.projectPath).toBe('/test/project');
            expect(context.toolList.length).toBeGreaterThan(1000);
        });
        
        it('should generate complete template with JSON Schema', () => {
            expect(template).toBeDefined();
            expect(template.length).toBeGreaterThan(5000);
            
            // Check template structure
            expect(template).toContain('Environment Information');
            expect(template).toContain('Available Tools');
            expect(template).toContain('Task Execution Guidelines');
            expect(template).toContain('Response Format');
            
            // Check JSON Schema format in template
            expect(template).toContain('```json');
            expect(template).toContain('"$schema"');
            expect(template).toContain('draft-07/schema#');
            expect(template).toContain('## read-file');
            expect(template).toContain('**Example:**');
        });
        
        it('should have proper tool descriptions in template', () => {
            // Check that tools are properly formatted
            expect(template).toContain('## read-file');
            expect(template).toContain('**Description:**');
            expect(template).toContain('**Parameters JSON Schema:**');
            
            // Check JSON Schema structure
            expect(template).toContain('"properties"');
            expect(template).toContain('"required"');
            expect(template).toContain('"type": "object"');
        });
    });
    
    describe('Performance and Quality Metrics', () => {
        it('should generate tool list efficiently', () => {
            const startTime = Date.now();
            
            const toolRegistry = new JsToolRegistry('/test/project');
            const toolList = toolRegistry.formatToolListForAI();
            
            const endTime = Date.now();
            const duration = endTime - startTime;
            
            expect(duration).toBeLessThan(1000); // Should complete within 1 second
            expect(toolList.length).toBeGreaterThan(5000); // Should be substantial
        });
        
        it('should have improved information density', () => {
            const toolRegistry = new JsToolRegistry('/test/project');
            const toolList = toolRegistry.formatToolListForAI();
            
            // The new format should be significantly longer than the old XML format
            expect(toolList.length).toBeGreaterThan(8000); // Expect ~8.5k characters
            
            // Should have rich parameter information
            const parameterMatches = (toolList.match(/"type":/g) || []).length;
            expect(parameterMatches).toBeGreaterThan(20); // Many type definitions
            
            const descriptionMatches = (toolList.match(/"description":/g) || []).length;
            expect(descriptionMatches).toBeGreaterThan(15); // Many descriptions
        });
    });
});

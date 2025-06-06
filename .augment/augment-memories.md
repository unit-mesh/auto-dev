# Refactoring Approach
- User prefers phased/incremental refactoring approach where each stage is validated as OK before moving to the next phase.
- User prefers incremental refactoring approach, starting with extracting processors first and evaluating each step before proceeding to the next phase.
- User prefers gradual refactoring approach and is concerned about maintenance costs when refactoring compiler code.
- User prefers less granular refactoring approach, splitting large files into 2-3 files rather than many small specialized processors.
- User prefers to extract only ValueParser.kt from HobbitHoleParser refactoring, not wanting the more granular ExpressionParser extraction.
- User prefers to compress/optimize code to around 300 lines when possible.
- User suggests considering YAML-like parsing approach to handle code with various quotes and special characters, and recommends prompting the model about potential parsing issues.
- AI responses may contain placeholder comments like '// ... existing getters and setters ...' and '// ... existing methods ...' that need special handling in the processing logic.
- User prefers to separate edit logic into testable components and generate comprehensive tests for the functionality.
- User prefers refactoring processEditFileCommand and processWriteCommand into separate classes for better decoupling and separation of concerns.

# CodeHighlightSketch
- For CodeHighlightSketch: prefer not showing editorFragment by default (use popup/collapse), unify collapse logic regardless of isUser flag, and use Command icons when isDevIns is detected.
- For CodeHighlightSketch: markdown and plain text content should not use folding/collapsing behavior.
- For CodeHighlightSketch: non-user and non-DevIns content should be displayed (not collapsed), and EditorFragment's folding UI should be moved into the Sketch to resolve conflicts with existing folding logic.
- For edit_file command handling: add processing in CodeHighlightSketch's onDoneStream, convert to DiffLangSketch using DiffLangSketchProvider for display, and auto-execute in AutoSketchMode.
- In CodeHighlightSketch, firstline should be updated in updateViewText method rather than setupCollapsedView because updateViewText handles dynamic character-by-character processing.

# PlanLangSketch
- User prefers PlanLangSketch to be pinned by default to save right-side space, or have a compression/size reduction option for better user experience.
- User prefers compressing the PlanLangSketch component itself rather than just compressing the toolbar when implementing space-saving features.
- When autoPin is enabled for PlanLangSketch, the current Plan should not be displayed to avoid showing duplicate content, or the Plan Toolbar should have a collapse feature that defaults to collapsed when autoPin is active.

# Testing
- The DevInsCompiler class has existing tests that can be used to validate refactoring changes.
- User prefers to generate comprehensive tests for the functionality.
- For EditApplyTest, use the command ':core:test --tests "cc.unitmesh.devti.command.EditApplyTest"' to run the tests.

# Performance
- User prefers WorkspaceFileSearchPopup to use lazy loading with recently opened files prioritized first, rather than loading all files at once to prevent IDEA freezing.
- User prefers EditFileInsCommand core logic moved to core module instead of submodule, and DevIns processing logic moved from onDoneStream to updateViewText with isComplete condition for better real-time performance.

# IDEA File Searching
- For IDEA file searching, use the non-deprecated FilenameIndex.processFilesByName(String name, boolean caseSensitively, GlobalSearchScope scope, Processor<VirtualFile> processor) instead of the deprecated versions that take Project parameter.

# Commit Messages
- User prefers to merge GitHub-specific and general commit message actions into a single action that conditionally fetches issues for GitHub projects and uses default behavior for non-GitHub projects.
- User prefers CommitMsgGenContext to have only issueId and issueDetail fields instead of separate issueNumber, issueTitle, and issueBody fields.
- User prefers GitHub issue fetching operations to have a 5-second timeout limit to avoid blocking when issues cannot be retrieved quickly.

# Commands
- User wants to create EditFileInsCommand as a new command type for file editing, separate from PatchInsCommand which is not suitable for file changes.
- For executeEditFileCommand, should use TextFilePatch and create UI similar to createSingleFileDiffSketch method, with corresponding modifications to executeEditFileCommand Results.
- User prefers implementing edit_file command using markdown parsing approach rather than Service calls to avoid potential issues.
- User prefers to use edit_file command instead of patch command and wants patch references removed from system prompts.
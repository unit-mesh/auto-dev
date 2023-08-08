# Usage

1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
2. Configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `AutoDev`

### CodeCompletion mode

You can:

- Right-click on the code editor, select `AutoDev` -> `CodeCompletion` -> `CodeComplete`
- or use `Alt + Enter` to open `Intention Actions` menu, select `AutoDev` -> `CodeCompletion`

![Code completion](https://unitmesh.cc/auto-dev/completion-mode.png)

### Custom Action

![Code completion](https://unitmesh.cc/auto-dev/custom-action.png)

For more, see [Custom Action](docs/custom-action.md)

### AutoCRUD mode

1. add `// devti://story/github/1` comments in your code.
2. configure GitHub repository for Run Configuration.
3. click `AutoDev` button in the comments' left.

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

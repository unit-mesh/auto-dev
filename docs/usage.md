# Usage

1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
2. Configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `AutoDev`

## Config

### Official OpenAI config

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `OpenAI`, select `Open AI Model` -> `gpt-3.5-turbo`
3. fill `OpenAI API Key` with your OpenAI API Key.
4. Apply and OK.

### OpenAI Proxy

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `OpenAI`, select `Open AI Model` -> `gpt-3.5-turbo`,
3. fill `Custom OpenAI Host` with your OpenAI API Endpoint.
4. fill `OpenAI API Key` with your Host OpenAI API Key.
5. Apply and OK.

### Azure Config

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `Azure`, select `Open AI Model` -> `gpt-3.5-turbo`
3. fill `Custom OpenAI Host` with your OpenAI API Endpoint. (with Key in URL)
4. Apply and OK.

### Custom Config

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`.
2. select `AI Engine` -> `Custom`
3. fill `Custom Engine Server`
4. fill `Custom Engine Token` if needed.
5. Apply and OK.

## Features

### CodeCompletion mode

You can:

- Right-click on the code editor, select `AutoDev` -> `CodeCompletion` -> `CodeComplete`
- or use `Alt + Enter` to open `Intention Actions` menu, select `AutoDev` -> `CodeCompletion`

![Code completion](https://unitmesh.cc/auto-dev/completion-mode.png)

### Custom Action

![Code completion](https://unitmesh.cc/auto-dev/custom-action.png)

For more, see [Custom Action](docs/custom-action.md)

### AutoCRUD mode （Only support Java/Kotlin project)

1. add `// devti://story/github/1` comments in your code.
2. configure GitHub repository for Run Configuration.
3. click `AutoDev` button in the comments' left.

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

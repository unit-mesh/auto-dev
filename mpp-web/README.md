# mpp-web - AutoDev Web UI

轻量级 Web UI，架构与 CLI 完全相同：**纯 TypeScript/React + mpp-core**

## 架构对比

```
mpp-ui (CLI)                      mpp-web (Web UI)
├── TypeScript/React (Ink)        ├── TypeScript/React (DOM)
└── @autodev/mpp-core              └── @autodev/mpp-core ← 相同！
```

**关键特点**：
- ✅ 不需要 Kotlin 编译到浏览器
- ✅ 不需要 Gradle webpack 任务
- ✅ 直接使用 mpp-core（和 CLI 一样）
- ✅ Vite 快速构建（~2秒）
- ✅ 体积小（~50KB + mpp-core）

## 开发

```bash
# 1. 构建 mpp-core (只需一次，或 mpp-core 更新时)
npm run build:kotlin

# 2. 启动开发服务器
npm run dev
# 访问 http://localhost:3000

# 3. 生产构建
npm run build
```

## 与 mpp-ui 的区别

| 特性 | mpp-ui (CLI) | mpp-web (Web UI) |
|-----|-------------|-----------------|
| 运行环境 | Node.js | Browser |
| UI 框架 | React + Ink | React + DOM |
| 核心依赖 | mpp-core | mpp-core |
| 构建工具 | Gradle + TSC | Vite |
| 构建时间 | ~3秒 | ~2秒 |
| Kotlin 编译 | ❌ No | ❌ No |

## 依赖说明

```json
{
  "dependencies": {
    "@autodev/mpp-core": "^0.1.4",  // 核心逻辑（与 CLI 共享）
    "react": "^18.3.1",              // UI 框架
    "react-dom": "^18.3.1"           // DOM 渲染
  }
}
```

**没有**：
- ❌ Compose HTML/Web
- ❌ Kotlin/JS 浏览器编译
- ❌ 11 分钟的 Webpack 构建
- ❌ 5MB+ 的 bundle

## 为什么这样设计？

1. **与 CLI 架构一致** - 都是 TypeScript + mpp-core
2. **快速构建** - Vite 比 Kotlin/JS + Webpack 快得多
3. **轻量级** - 只有必要的依赖
4. **易于开发** - 标准的 React/Vite 开发体验

## 项目结构

```
mpp-web/
├── src/
│   ├── main.tsx        # 入口点
│   ├── App.tsx         # 主应用
│   └── index.css       # 样式
├── public/
│   └── index.html      # HTML 模板
├── package.json        # 依赖配置
├── tsconfig.json       # TypeScript 配置
├── vite.config.ts      # Vite 配置
└── README.md
```


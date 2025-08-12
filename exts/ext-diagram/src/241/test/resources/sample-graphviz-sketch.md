# Graphviz LangSketch 示例

这个文档展示了如何在 AutoDev 中使用 Graphviz LangSketch 功能。

## 简单的有向图

```dot
digraph SimpleGraph {
    A -> B;
    B -> C;
    A -> C;
}
```

## 带属性的图表

```graphviz
digraph AttributedGraph {
    // 图表属性
    rankdir=TB;
    bgcolor=white;
    
    // 节点定义
    Start [label="开始", shape=box, color=green, style=filled];
    Process [label="处理", shape=ellipse, color=blue];
    Decision [label="决策", shape=diamond, color=yellow, style=filled];
    End [label="结束", shape=box, color=red, style=filled];
    
    // 边定义
    Start -> Process [label="开始处理"];
    Process -> Decision [label="检查结果"];
    Decision -> End [label="完成", color=green];
    Decision -> Process [label="重试", color=red, style=dashed];
}
```

## 无向图示例

```gv
graph UndirectedGraph {
    A -- B;
    B -- C;
    C -- D;
    D -- A;
    A -- C;
}
```

## 复杂的系统架构图

```dot
digraph SystemArchitecture {
    rankdir=LR;
    
    subgraph cluster_frontend {
        label="前端层";
        style=filled;
        color=lightgrey;
        
        Web [label="Web界面", shape=box];
        Mobile [label="移动端", shape=box];
    }
    
    subgraph cluster_backend {
        label="后端层";
        style=filled;
        color=lightblue;
        
        API [label="API网关", shape=ellipse];
        Service1 [label="用户服务", shape=ellipse];
        Service2 [label="订单服务", shape=ellipse];
    }
    
    subgraph cluster_data {
        label="数据层";
        style=filled;
        color=lightyellow;
        
        DB1 [label="用户数据库", shape=cylinder];
        DB2 [label="订单数据库", shape=cylinder];
        Cache [label="缓存", shape=note];
    }
    
    // 连接关系
    Web -> API;
    Mobile -> API;
    API -> Service1;
    API -> Service2;
    Service1 -> DB1;
    Service1 -> Cache;
    Service2 -> DB2;
    Service2 -> Cache;
}
```

这些示例展示了 Graphviz LangSketch 支持的各种图表类型和语法。在 AutoDev 工具窗口中，这些代码块会被自动识别并渲染为交互式图表。

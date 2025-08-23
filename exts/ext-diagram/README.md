# Graphviz Diagram Extension

This extension provides support for viewing and editing Graphviz DOT files as interactive diagrams in IntelliJ IDEA.

Supported File Types:

- `.dot` files
- `.gv` files  
- `.graphviz` files

```dot
digraph G {
    rankdir=TB;
    node [shape=box, style=rounded];

    subgraph cluster_agents {
        label="代理定义";
        TravelAgent [label=<
            <b>旅行代理</b><br/>协调预订航班、酒店和支付
        >];
        FlightAgent [label=<
            <b>航班代理</b><br/>技能: findFlights(query: object): flightOptions
        >];
        HotelAgent [label=<
            <b>酒店代理</b><br/>技能: findHotels(query: object): hotelOptions
        >];
        PaymentAgent [label=<
            <b>支付代理</b><br/>技能: processPayment(amount: float): transactionID
        >];
        RefundHandler [label=<
            <b>退款处理代理</b><br/>技能: initiateRefund(transactionID: string): void
        >];
    }

    subgraph cluster_parallel {
        label="1. 并行查询";
        TravelAgent -> FlightAgent [label="findFlights()"];
        TravelAgent -> HotelAgent [label="findHotels()"];
    }

    subgraph cluster_conditional {
        label="2. 条件决策与支付";
        TravelAgent -> PaymentAgent [label="processPayment()"];
        TravelAgent -> User [label="无法满足所有预订要求"];
    }

    PaymentAgent -> RefundHandler [label="initiateRefund()", color=red, style=dashed];
}
```

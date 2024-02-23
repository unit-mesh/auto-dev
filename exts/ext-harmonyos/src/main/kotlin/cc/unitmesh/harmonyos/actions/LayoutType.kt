package cc.unitmesh.harmonyos.actions

enum class LayoutType(val description: String, val example: String) {
    Flex(
        "弹性布局（Flex）提供更加有效的方式对容器中的子元素进行排列、对齐和分配剩余空间。",
        "Column({ space: 5 }) {\n" +
                "  Flex({ direction: FlexDirection.Row, wrap: FlexWrap.NoWrap, justifyContent: FlexAlign.SpaceBetween, alignItems: ItemAlign.Center }) {\n" +
                "    Text('1').width('30%').height(50).backgroundColor(0xF5DEB3)\n" +
                "    Text('2').width('30%').height(50).backgroundColor(0xD2B48C)\n" +
                "    Text('3').width('30%').height(50).backgroundColor(0xF5DEB3)\n" +
                "  }\n" +
                "  .height(70)\n" +
                "  .width('90%')\n" +
                "  .backgroundColor(0xAFEEEE)\n" +
                "}.width('100%').margin({ top: 5 })"
    ),
    LinearLayout(
        "Row/Column即线性布局（LinearLayout）是开发中最常用的布局，通过线性容器Row和Column构建。",
        "Column({ space: 20 }) {\n" +
                "  Text('space: 20').fontSize(15).fontColor(Color.Gray).width('90%')\n" +
                "  Row().width('90%').height(50).backgroundColor(0xF5DEB3)\n" +
                "  Row().width('90%').height(50).backgroundColor(0xD2B48C)\n" +
                "  Row().width('90%').height(50).backgroundColor(0xF5DEB3)\n" +
                "}.width('100%')"
    ),
    RelativeContainer(
        "RelativeContainer为采用相对布局的容器，支持容器内部的子元素设置相对位置关系。",
        "RelativeContainer() {\n" +
                "  Row()\n" +
                "    // 添加其他属性\n" +
                "    .alignRules({\n" +
                "      top: { anchor: '__container__', align: VerticalAlign.Top },\n" +
                "      left: { anchor: '__container__', align: HorizontalAlign.Start }\n" +
                "    })\n" +
                "    .id(\"row1\")\n" +
                "\n" +
                "  Row()\n" +
                "    ...\n" +
                "    .alignRules({\n" +
                "      top: { anchor: '__container__', align: VerticalAlign.Top },\n" +
                "      right: { anchor: '__container__', align: HorizontalAlign.End }\n" +
                "    })\n" +
                "    .id(\"row2\")\n" +
                "}\n" +
                "..."
    ),

    GridLayout(
        "栅格布局（GridRow/GridCol）是一种通用的辅助定位工具，对移动设备的界面设计有较好的借鉴作用。",
        "GridRow() {\n" +
                "  ForEach(this.bgColors, (item, index) => {\n" +
                "    GridCol() {\n" +
                "      Row() {\n" +
                "        Text(`\${index + 1}`)\n" +
                "      }.width('100%').height('50')\n" +
                "    }.backgroundColor(item)\n" +
                "  })\n" +
                "}           "
    ),
    List(
        "列表是一种复杂的容器，当列表项达到一定数量，内容超过屏幕大小时，可以自动提供滚动功能。",
        "    List() {\n" +
                "  ListItem() {\n" +
                "    Text('北京').fontSize(24)\n" +
                "  }\n" +
                "  ListItem() {\n" +
                "    Text('杭州').fontSize(24)\n" +
                "  }\n" +
                "  ListItem() {\n" +
                "    Text('上海').fontSize(24)\n" +
                "  }\n" +
                "}\n" +
                ".backgroundColor('#FFF1F3F5')\n" +
                ".alignListItem(ListItemAlign.Center)"
    ),
    StackLayout(
        "层叠布局（StackLayout）用于在屏幕上预留一块区域来显示组件中的元素，提供元素可以重叠的布局。",
        "Stack({ alignContent: Alignment.Bottom }) {\n" +
                "    Flex({ wrap: FlexWrap.Wrap }) {\n" +
                "      ForEach(this.arr, (item) => {\n" +
                "        Text(item)\n" +
                "          .width(100)\n" +
                "          .height(100)\n" +
                "          .fontSize(16)\n" +
                "          .margin(10)\n" +
                "          .textAlign(TextAlign.Center)\n" +
                "          .borderRadius(10)\n" +
                "          .backgroundColor(0xFFFFFF)\n" +
                "      }, item => item)\n" +
                "    }.width('100%').height('100%')\n" +
                "    Flex({ justifyContent: FlexAlign.SpaceAround, alignItems: ItemAlign.Center }) {\n" +
                "      Text('联系人').fontSize(16)\n" +
                "      Text('设置').fontSize(16)\n" +
                "      Text('短信').fontSize(16)\n" +
                "    }\n" +
                "    .width('50%')\n" +
                "    .height(50)\n" +
                "    .backgroundColor('#16302e2e')\n" +
                "    .margin({ bottom: 15 })\n" +
                "    .borderRadius(15)\n" +
                "  }.width('100%').height('100%').backgroundColor('#CFD0CF')\n" +
                "}"
    )
}
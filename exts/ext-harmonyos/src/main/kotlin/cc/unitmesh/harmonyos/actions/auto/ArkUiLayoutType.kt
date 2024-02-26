package cc.unitmesh.harmonyos.actions.auto

enum class ArkUiLayoutType(val description: String, val example: String) {
    FlexLayout(
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
        "Row/Column 即线性布局（LinearLayout）是开发中最常用的布局，通过线性容器 Row 和 Column 构建。",
        "Row({}) {\n" +
                "  Column() {\n" +
                "    // ...\n" +
                "  }.width('20%').height(30).backgroundColor(0xF5DEB3)\n" +
                "\n" +
                "  Column() {\n" +
                "    // ...\n" +
                "  }.width('20%').height(30).backgroundColor(0xD2B48C)\n" +
                "\n" +
                "  Column() {\n" +
                "    // ...\n" +
                "  }.width('20%').height(30).backgroundColor(0xF5DEB3)\n" +
                "}.width('100%').height(200).alignItems(VerticalAlign.Center).backgroundColor('rgb(242,242,242)')"
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
                "    // ...\n" +
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
                "}.width('80%').margin({ left: 10, top: 5, bottom: 5 }).height(200)"
    ),
    List(
        "列表是一种复杂的容器，当列表项达到一定数量，内容超过屏幕大小时，可以自动提供滚动功能。",
        "List({ space: 20, initialIndex: 0 }) {\n" +
                "  ForEach(this.arr, (item) => {\n" +
                "      ListItem() {\n" +
                "        Text(`\${item}`)\n" +
                "          .fontSize(50)\n" +
                "          .fontWeight(FontWeight.Bold)\n" +
                "          .fontColor(['red', 'green', 'blue'][item % 3])\n" +
                "          .padding({ left: '10', right: '10' })\n" +
                "          .backgroundColor('white')\n" +
                "      }\n" +
                "      .backgroundColor('gray')\n" +
                "    }, (item) => item)\n" +
                "}\n" +
                ".listDirection(Axis.Vertical) // 排列方向 默认就是纵向排列\n" +
                ".divider({ strokeWidth: 2, color: 0xFFFFFF, startMargin: 20, endMargin: 20 }) // 每行之间的分界线，默认没有分界线\n" +
                ".edgeEffect(EdgeEffect.Spring) // 滑动到边缘无效果\n" +
                ".onScrollIndex((firstIndex: number, lastIndex: number) => {\n" +
                "  console.info('first' + firstIndex)\n" +
                "  console.info('last' + lastIndex)\n" +
                "})\n" +
                ".width('100%')\n" +
                ".height('100%')"
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
                "  }.width('100%').height('100%').backgroundColor('#CFD0CF')\n" +
                "}.width('100%').height(150).margin({ top: 5 })"
    );

    companion object {
        fun overview(): String {
            return ArkUiLayoutType.values().joinToString("\n") {
                it.name + ":" + it.description
            }
        }

        fun tryFormat(name: String): String? {
            return try {
                valueOf(name).example
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
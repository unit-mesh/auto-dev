package cc.unitmesh.harmonyos.actions.auto

enum class ArkUiComponentType(description: String, example: String) {
    Button(
        "可快速创建不同样式的按钮。", "Button('Ok', { type: ButtonType.Normal, stateEffect: true }) \n" +
                "  .borderRadius(8) \n" +
                "  .backgroundColor(0x317aff) \n" +
                "  .width(90)\n" +
                "  .height(40)"
    ),
    Checkbox(
        "提供多选框组件，通常用于某选项的打开或关闭。",
        "Checkbox({name: 'checkbox1',  group: 'checkboxGroup'})\n" +
                " .select(true)\n" +
                " .selectedColor(0xed6f21)\n" +
                " .onChange((value: boolean) => {\n" +
                "   console.info('Checkbox1 change is'+ value)\n" +
                " })"
    ),
    CheckboxGroup(
        "多选框群组，用于控制多选框全选或者不全选状态。",
        "CheckboxGroup({ group: 'checkboxGroup' })\n" +
                "  .selectedColor('#007DFF')\n" +
                "  .onChange((itemName: CheckboxGroupResult) => {\n" +
                "    console.info(\"checkbox group content\" + JSON.stringify(itemName))\n" +
                "  })"
    ),
    Image(
        "Image为图片组件，常用于在应用中显示图片。Image支持加载string、PixelMap和Resource类型的数据源，支持png、jpg、bmp、svg和gif类型的图片格式。",
        "Image(this.src)\n" +
                "  .width(120).height(120)\n" +
                "  .onClick(() => {\n" +
                "    if (this.src == this.on || this.src == this.off2on) {\n" +
                "      this.src = this.on2off\n" +
                "    } else {\n" +
                "      this.src = this.off2on\n" +
                "    }\n" +
                "  })\n" +
                "  .onFinish(() => {\n" +
                "    if (this.src == this.off2on) {\n" +
                "      this.src = this.on\n" +
                "    } else {\n" +
                "      this.src = this.off\n" +
                "    }\n" +
                "  })"

    ),
    Menu(
        "以垂直列表形式显示的菜单。",
        "      MenuItemGroup({ header: '小标题' }) {\n" +
                "    MenuItem({ content: \"菜单选项\" })\n" +
                "      .selectIcon(true)\n" +
                "      .selected(this.select)\n" +
                "      .onChange((selected) => {\n" +
                "        console.info(\"menuItem select\" + selected);\n" +
                "        this.iconStr2 = \$r(\"app.media.icon\");\n" +
                "      })\n" +
                "    MenuItem({\n" +
                "      startIcon: \$r(\"app.media.view_list_filled\"),\n" +
                "      content: \"菜单选项\",\n" +
                "      endIcon: \$r(\"app.media.arrow_right_filled\"),\n" +
                "      builder: this.SubMenu.bind(this)\n" +
                "    })\n" +
                "  }"
    ),
    Text(
        "显示一段文本的组件。。", "Text('TextAlign set to Center.')\n" +
                "    .textAlign(TextAlign.Center)\n" +
                "    .fontSize(12)\n" +
                "    .border({ width: 1 })\n" +
                "    .padding(10)\n" +
                "    .width('100%')"
    ),
    TextArea(
        "多行文本输入框。",
        "TextArea({\n" +
                "  placeholder: 'The text area can hold an unlimited amount of text. input your word...',\n" +
                "  controller: this.controller\n" +
                "})\n" +
                "  .placeholderFont({ size: 16, weight: 400 })\n" +
                "  .width(336)\n" +
                "  .height(56)\n" +
                "  .margin(20)\n" +
                "  .fontSize(16)\n" +
                "  .fontColor('#182431')\n" +
                "  .backgroundColor('#FFFFFF')\n" +
                "  .onChange((value: string) => {\n" +
                "    this.text = value\n" +
                "   })"
    ),
    TextInput(
        "单行文本输入框组件。",
        "TextInput({ text: this.text, placeholder: 'input your word...', controller: this.controller })\n" +
                "   .placeholderColor(Color.Grey)\n" +
                "   .placeholderFont({ size: 14, weight: 400 })\n" +
                "   .caretColor(Color.Blue)\n" +
                "   .width(400)\n" +
                "   .height(40)\n" +
                "   .margin(20)\n" +
                "   .fontSize(14)\n" +
                "   .fontColor(Color.Black)\n" +
                "   .inputFilter('[a-z]', (e) => {\n" +
                "     console.log(JSON.stringify(e))\n" +
                "   })\n" +
                "   .onChange((value: string) => {\n" +
                "     this.text = value\n" +
                "   })"
    ),
    Radio(
        "提供相应的用户交互选择项。", "  Radio({ value: 'Radio1', group: 'radioGroup' })\n" +
                "    .onChange((isChecked: boolean) => {\n" +
                "      if(isChecked) {\n" +
                "        //需要执行的操作\n" +
                "      }\n" +
                "    })"
    ),
    Toggle(
        "Toggle为开关组件，常用于在应用中进行开关操作。",
        "Toggle({ type: ToggleType.Switch, isOn: false })\n" +
                "  .selectedColor('#007DFF')\n" +
                "  .switchPointColor('#FFFFFF')\n" +
                "  .onChange((isOn: boolean) => {\n" +
                "    console.info('Component status:' + isOn)\n" +
                "  })"
    ),
    Web(
        "提供具有网页显示能力的Web组件，@ohos.web.webview提供web控制能力。",
        "Web({ src: \$rawfile(\"xxx.html\"), controller: this.controller })\n" +
                "    .onBeforeUnload((event) => {\n" +
                "      console.log(\"event.url:\" + event.url)\n" +
                "      console.log(\"event.message:\" + event.message)\n" +
                "      AlertDialog.show({\n" +
                "        title: 'onBeforeUnload',\n" +
                "        message: 'text',\n" +
                "        primaryButton: {\n" +
                "          value: 'cancel',\n" +
                "          action: () => {\n" +
                "            event.result.handleCancel()\n" +
                "          }\n" +
                "        },\n" +
                "        secondaryButton: {\n" +
                "          value: 'ok',\n" +
                "          action: () => {\n" +
                "            event.result.handleConfirm()\n" +
                "          }\n" +
                "        },\n" +
                "        cancel: () => {\n" +
                "          event.result.handleCancel()\n" +
                "        }\n" +
                "      })\n" +
                "      return true\n" +
                "    })"
    )
}
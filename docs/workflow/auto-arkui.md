---
layout: default
title: Auto ArkUI
nav_order: 4
parent: Workflow
---

Work on the following IDEs:

Android Studio
{: .label .label-blue }
DevEco Studio
{: .label .label-blue }

Demo Video: [https://www.bilibili.com/video/BV11A4m137k9/](https://www.bilibili.com/video/BV11A4m137k9/)

## Implementation

Select the requirement in text, select `Auto Generate ArkUI`.

![AutoDev ArkUI Sample](https://harmonyos-dev.github.io/aigc-harmonyos-sample/images/autodev-arkui-sample.png)

### Text requirement

- // 生成一个经典的前端 counter
- // 生成一个聊天列表页，item 需要包含头像、昵称、最后一条聊天记录，尽可能让页面美观
- // 生成 Search 组件，可以设置placeholder文本样式和颜色、搜索框内文本样式，以及submit和onChange等方法触发时的操作。

more detail:

```markdown
// 音乐专辑主页
// 头部返回栏: 因元素单一、位置固定在顶部，因此适合采用自适应拉伸，充分利用顶部区域。
// 专辑封面: 使用栅格组件控制占比，在小尺寸屏幕下封面图与歌单描述在同一行。
// 歌曲列表: 使用栅格组件控制宽度，在小尺寸屏幕下宽度为屏幕的100%，中尺寸屏幕下宽度为屏幕的50%，大尺寸屏幕下宽度为屏幕的75%。
// 播放器: 采用自适应拉伸，充分使用底部区域。
```

more detail example 2:

```markdown
// 生成一个：健康饮食详细页。
// 要求：使用滑动组件展示食物的详细信息，包括使用画布组件展示单位重量的食物各个营养元素的的占比,使用进度条组件展示当前食物是否为高热食物，
// 以及展示单位重量的食物所包含的热量、脂肪、蛋白质、碳水以及维他命C值；并且点击记录按钮可以弹出记录饮食的弹窗，包括记录食物的种类、重量以及用餐时间，
// 可以通过点击完成添加饮食，同时添加的饮食信息会在“记录”Tab页签做展示。
```

### Android Layout example

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:orientation="vertical" >
    <TextView android:id="@+id/text"
              android:layout_width="wrap_content"
              android:layout_height="wrap_content"
              android:text="Hello, I am a TextView" />
    <Button android:id="@+id/button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Hello, I am a Button" />
</LinearLayout>
```

For more examples, see the [AutoDev for HarmonyOS](https://harmonyos-dev.github.io/aigc-harmonyos-sample)
#!/bin/bash

# AutoDev iOS App - 一键构建和运行脚本
# 
# 这个脚本会:
# 1. 编译 Kotlin Framework
# 2. 安装 CocoaPods 依赖
# 3. 打开 Xcode 项目

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   AutoDev iOS App - Build & Run       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

# 检查是否在 mpp-ios 目录
if [ ! -f "Podfile" ]; then
    echo -e "${RED}❌ 错误: 请在 mpp-ios 目录下运行此脚本${NC}"
    exit 1
fi

# 步骤 1: 编译 Kotlin Framework
echo -e "${BLUE}📦 步骤 1/3: 编译 Kotlin Framework...${NC}"
cd ..

# 检测架构
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    echo -e "${GREEN}✓ 检测到 Apple Silicon (ARM64)${NC}"
    TARGET="iosSimulatorArm64"
else
    echo -e "${GREEN}✓ 检测到 Intel (x86_64)${NC}"
    TARGET="iosX64"
fi

echo -e "${YELLOW}  编译 mpp-core...${NC}"
./gradlew :mpp-core:linkDebugFramework${TARGET} --console=plain

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ mpp-core 编译失败${NC}"
    exit 1
fi

echo -e "${YELLOW}  编译 mpp-ui...${NC}"
./gradlew :mpp-ui:linkDebugFramework${TARGET} --console=plain

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ mpp-ui 编译失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Framework 编译成功!${NC}"
echo ""

# 步骤 2: 安装 CocoaPods 依赖
echo -e "${BLUE}📦 步骤 2/3: 安装 CocoaPods 依赖...${NC}"
cd mpp-ios

# 检查 CocoaPods 是否安装
if ! command -v pod &> /dev/null; then
    echo -e "${RED}❌ CocoaPods 未安装${NC}"
    echo -e "${YELLOW}请运行: sudo gem install cocoapods${NC}"
    exit 1
fi

echo -e "${YELLOW}  运行 pod install...${NC}"
pod install

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ pod install 失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ CocoaPods 依赖安装成功!${NC}"
echo ""

# 步骤 3: 打开 Xcode 项目
echo -e "${BLUE}🚀 步骤 3/3: 打开 Xcode 项目...${NC}"

if [ -d "AutoDevApp.xcworkspace" ]; then
    echo -e "${GREEN}✓ 打开 AutoDevApp.xcworkspace${NC}"
    open AutoDevApp.xcworkspace
else
    echo -e "${YELLOW}⚠️  .xcworkspace 未找到,打开 .xcodeproj${NC}"
    open AutoDevApp.xcodeproj
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          构建完成! 🎉                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}下一步:${NC}"
echo -e "  1. 在 Xcode 中选择模拟器 (例如: iPhone 15 Pro)"
echo -e "  2. 点击 Run 按钮 (⌘R)"
echo -e "  3. 享受 AutoDev iOS App! 🚀"
echo ""


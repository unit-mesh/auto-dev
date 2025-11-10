#!/bin/bash

# AutoDev iOS - Framework 编译脚本
# 
# 用法:
#   ./build-framework.sh              # 编译 Debug 版本 (模拟器)
#   ./build-framework.sh release      # 编译 Release 版本
#   ./build-framework.sh device       # 编译真机版本

set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔨 AutoDev iOS Framework Builder${NC}"
echo ""

# 解析参数
CONFIG="Debug"
TARGET_TYPE="simulator"

if [ "$1" = "release" ]; then
    CONFIG="Release"
    echo -e "${YELLOW}📦 配置: Release${NC}"
elif [ "$1" = "device" ]; then
    TARGET_TYPE="device"
    echo -e "${YELLOW}📱 目标: 真机 (Device)${NC}"
else
    echo -e "${YELLOW}📱 目标: 模拟器 (Simulator)${NC}"
fi

# 检测架构
ARCH=$(uname -m)
if [ "$TARGET_TYPE" = "device" ]; then
    TARGET="iosArm64"
    echo -e "${GREEN}✓ 目标架构: ARM64 (真机)${NC}"
elif [ "$ARCH" = "arm64" ]; then
    TARGET="iosSimulatorArm64"
    echo -e "${GREEN}✓ 目标架构: ARM64 (Apple Silicon 模拟器)${NC}"
else
    TARGET="iosX64"
    echo -e "${GREEN}✓ 目标架构: x86_64 (Intel 模拟器)${NC}"
fi

# 进入项目根目录
cd "$(dirname "$0")/.."

echo ""
echo -e "${BLUE}📦 编译 mpp-core...${NC}"
./gradlew :mpp-core:link${CONFIG}Framework${TARGET} --console=plain

echo ""
echo -e "${BLUE}📦 编译 mpp-ui...${NC}"
./gradlew :mpp-ui:link${CONFIG}Framework${TARGET} --console=plain

echo ""
echo -e "${GREEN}✅ Framework 编译成功!${NC}"
echo ""
echo -e "${BLUE}Framework 位置:${NC}"

# 转换为小写
TARGET_LOWER=$(echo "$TARGET" | tr '[:upper:]' '[:lower:]')
CONFIG_LOWER=$(echo "$CONFIG" | tr '[:upper:]' '[:lower:]')

echo -e "  mpp-core: ${YELLOW}mpp-core/build/bin/${TARGET_LOWER}/${CONFIG_LOWER}Framework/AutoDevCore.framework${NC}"
echo -e "  mpp-ui:   ${YELLOW}mpp-ui/build/bin/${TARGET_LOWER}/${CONFIG_LOWER}Framework/AutoDevUI.framework${NC}"
echo ""


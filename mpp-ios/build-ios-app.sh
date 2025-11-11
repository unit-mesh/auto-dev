#!/bin/bash

# AutoDev iOS App - 完整构建脚本
# 
# 这个脚本会:
# 1. 编译 Kotlin Multiplatform Framework (mpp-core 和 mpp-ui)
# 2. 安装 CocoaPods 依赖
# 3. 构建 iOS 应用 (可选)
# 4. 打开 Xcode 项目或直接运行到模拟器
#
# 用法:
#   ./build-ios-app.sh                    # 编译 framework + pod install + 打开 Xcode
#   ./build-ios-app.sh --build            # 编译 framework + pod install + 构建 app
#   ./build-ios-app.sh --run              # 编译 framework + pod install + 构建 + 运行到模拟器
#   ./build-ios-app.sh --clean            # 清理构建产物后重新构建
#   ./build-ios-app.sh --release          # 使用 Release 配置构建
#   ./build-ios-app.sh --device           # 为真机构建

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 默认参数
BUILD_CONFIG="Debug"
TARGET_TYPE="simulator"
ACTION="open"  # open, build, run
CLEAN_BUILD=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            ACTION="build"
            shift
            ;;
        --run)
            ACTION="run"
            shift
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --release)
            BUILD_CONFIG="Release"
            shift
            ;;
        --device)
            TARGET_TYPE="device"
            shift
            ;;
        --help)
            echo "用法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  --build      编译 iOS 应用但不运行"
            echo "  --run        编译并运行到模拟器"
            echo "  --clean      清理构建产物后重新构建"
            echo "  --release    使用 Release 配置"
            echo "  --device     为真机构建"
            echo "  --help       显示此帮助信息"
            echo ""
            echo "示例:"
            echo "  $0                    # 编译 framework 并打开 Xcode"
            echo "  $0 --build            # 完整构建 iOS 应用"
            echo "  $0 --run              # 构建并运行到模拟器"
            echo "  $0 --clean --release  # 清理后使用 Release 配置构建"
            exit 0
            ;;
        *)
            echo -e "${RED}未知参数: $1${NC}"
            echo "使用 --help 查看帮助"
            exit 1
            ;;
    esac
done

echo -e "${CYAN}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   AutoDev iOS App - 完整构建脚本                   ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════╝${NC}"
echo ""

# 检查是否在 mpp-ios 目录
if [ ! -f "Podfile" ]; then
    echo -e "${RED}❌ 错误: 请在 mpp-ios 目录下运行此脚本${NC}"
    exit 1
fi

# 显示配置信息
echo -e "${BLUE}📋 构建配置:${NC}"
echo -e "  配置: ${YELLOW}${BUILD_CONFIG}${NC}"
echo -e "  目标: ${YELLOW}${TARGET_TYPE}${NC}"
echo -e "  动作: ${YELLOW}${ACTION}${NC}"
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "  清理: ${YELLOW}是${NC}"
fi
echo ""

# 检测架构
ARCH=$(uname -m)
if [ "$TARGET_TYPE" = "device" ]; then
    GRADLE_TARGET="IosArm64"
    echo -e "${GREEN}✓ 目标架构: ARM64 (真机)${NC}"
elif [ "$ARCH" = "arm64" ]; then
    GRADLE_TARGET="IosSimulatorArm64"
    echo -e "${GREEN}✓ 检测到 Apple Silicon (ARM64 模拟器)${NC}"
else
    GRADLE_TARGET="IosX64"
    echo -e "${GREEN}✓ 检测到 Intel (x86_64 模拟器)${NC}"
fi
echo ""

# 进入项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# ============================================================
# 步骤 1: 清理构建产物 (如果需要)
# ============================================================
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "${BLUE}🧹 步骤 1/5: 清理构建产物...${NC}"
    
    echo -e "${YELLOW}  清理 Gradle 缓存...${NC}"
    ./gradlew :mpp-core:clean :mpp-ui:clean --console=plain
    
    echo -e "${YELLOW}  清理 CocoaPods 缓存...${NC}"
    cd mpp-ios
    rm -rf Pods
    rm -rf ~/Library/Caches/CocoaPods
    rm -f Podfile.lock
    cd "$PROJECT_ROOT"
    
    echo -e "${GREEN}✅ 清理完成!${NC}"
    echo ""
fi

# ============================================================
# 步骤 2: 编译 Kotlin Multiplatform Framework
# ============================================================
STEP_NUM=2
if [ "$CLEAN_BUILD" = true ]; then
    STEP_NUM=2
else
    STEP_NUM=1
fi

echo -e "${BLUE}📦 步骤 ${STEP_NUM}/4: 编译 Kotlin Framework...${NC}"

echo -e "${YELLOW}  编译 mpp-core...${NC}"
./gradlew :mpp-core:link${BUILD_CONFIG}Framework${GRADLE_TARGET} --console=plain

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ mpp-core 编译失败${NC}"
    exit 1
fi

echo -e "${YELLOW}  编译 mpp-ui...${NC}"
./gradlew :mpp-ui:link${BUILD_CONFIG}Framework${GRADLE_TARGET} --console=plain

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ mpp-ui 编译失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Framework 编译成功!${NC}"

# 显示 framework 位置
GRADLE_TARGET_LOWER=$(echo "$GRADLE_TARGET" | tr '[:upper:]' '[:lower:]')
BUILD_CONFIG_LOWER=$(echo "$BUILD_CONFIG" | tr '[:upper:]' '[:lower:]')
echo -e "${CYAN}  Framework 位置:${NC}"
echo -e "    mpp-core: ${YELLOW}mpp-core/build/bin/${GRADLE_TARGET_LOWER}/${BUILD_CONFIG_LOWER}Framework/AutoDevCore.framework${NC}"
echo -e "    mpp-ui:   ${YELLOW}mpp-ui/build/bin/${GRADLE_TARGET_LOWER}/${BUILD_CONFIG_LOWER}Framework/AutoDevUI.framework${NC}"
echo ""

# ============================================================
# 步骤 3: 安装 CocoaPods 依赖
# ============================================================
STEP_NUM=$((STEP_NUM + 1))
echo -e "${BLUE}📦 步骤 ${STEP_NUM}/4: 安装 CocoaPods 依赖...${NC}"

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

# ============================================================
# 步骤 4: 执行相应的动作
# ============================================================
STEP_NUM=$((STEP_NUM + 1))

if [ "$ACTION" = "open" ]; then
    # 打开 Xcode 项目
    echo -e "${BLUE}🚀 步骤 ${STEP_NUM}/4: 打开 Xcode 项目...${NC}"
    
    if [ -d "AutoDevApp.xcworkspace" ]; then
        echo -e "${GREEN}✓ 打开 AutoDevApp.xcworkspace${NC}"
        open AutoDevApp.xcworkspace
    else
        echo -e "${YELLOW}⚠️  .xcworkspace 未找到,打开 .xcodeproj${NC}"
        open AutoDevApp.xcodeproj
    fi
    
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║          构建完成! 🎉                              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}下一步:${NC}"
    echo -e "  1. 在 Xcode 中选择模拟器 (例如: iPhone 15 Pro)"
    echo -e "  2. 点击 Run 按钮 (⌘R)"
    echo -e "  3. 享受 AutoDev iOS App! 🚀"
    echo ""

elif [ "$ACTION" = "build" ] || [ "$ACTION" = "run" ]; then
    # 使用 xcodebuild 构建应用
    echo -e "${BLUE}🔨 步骤 ${STEP_NUM}/4: 构建 iOS 应用...${NC}"

    SCHEME="AutoDevApp"
    WORKSPACE="AutoDevApp.xcworkspace"

    if [ "$TARGET_TYPE" = "device" ]; then
        DESTINATION="generic/platform=iOS"
        SDK="iphoneos"
    else
        # 获取可用的模拟器
        SIMULATOR=$(xcrun simctl list devices available | grep "iPhone" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/')
        if [ -z "$SIMULATOR" ]; then
            echo -e "${RED}❌ 未找到可用的模拟器${NC}"
            exit 1
        fi
        DESTINATION="id=$SIMULATOR"
        SDK="iphonesimulator"
    fi

    echo -e "${YELLOW}  解析 Swift Package 依赖...${NC}"
    # 首先解析 Swift Package 依赖（包括 MCP SDK）
    xcodebuild \
        -workspace "$WORKSPACE" \
        -scheme "$SCHEME" \
        -resolvePackageDependencies \
        -quiet

    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Swift Package 依赖解析失败${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ Swift Package 依赖解析成功${NC}"
    echo ""

    echo -e "${YELLOW}  使用 xcodebuild 构建应用...${NC}"
    echo -e "${CYAN}  Scheme: ${SCHEME}${NC}"
    echo -e "${CYAN}  Configuration: ${BUILD_CONFIG}${NC}"
    echo -e "${CYAN}  SDK: ${SDK}${NC}"
    echo -e "${CYAN}  Destination: ${DESTINATION}${NC}"
    echo ""

    # 构建应用
    xcodebuild \
        -workspace "$WORKSPACE" \
        -scheme "$SCHEME" \
        -configuration "$BUILD_CONFIG" \
        -sdk "$SDK" \
        -destination "$DESTINATION" \
        -derivedDataPath "./build" \
        build \
        CODE_SIGN_IDENTITY="" \
        CODE_SIGNING_REQUIRED=NO \
        CODE_SIGNING_ALLOWED=NO \
        | tee build.log

    BUILD_RESULT=${PIPESTATUS[0]}

    if [ $BUILD_RESULT -ne 0 ]; then
        echo -e "${RED}❌ 构建失败${NC}"
        echo -e "${YELLOW}查看详细日志: build.log${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ 构建成功!${NC}"
    echo ""

    # 查找构建的 .app 文件
    APP_PATH=$(find ./build/Build/Products -name "AutoDevApp.app" -type d | head -n 1)

    if [ -z "$APP_PATH" ]; then
        echo -e "${RED}❌ 未找到构建的应用${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ 应用已构建:${NC}"
    echo -e "${CYAN}  路径: ${APP_PATH}${NC}"
    echo -e "${CYAN}  大小: $(du -sh "$APP_PATH" | cut -f1)${NC}"
    echo ""

    if [ "$ACTION" = "run" ]; then
        # 运行到模拟器
        echo -e "${BLUE}🚀 运行应用到模拟器...${NC}"

        # 启动模拟器
        echo -e "${YELLOW}  启动模拟器...${NC}"
        xcrun simctl boot "$SIMULATOR" 2>/dev/null || true
        sleep 2

        # 卸载旧版本（如果存在）
        BUNDLE_ID="cc.unitmesh.AutoDevApp"
        xcrun simctl uninstall "$SIMULATOR" "$BUNDLE_ID" 2>/dev/null || true

        # 安装应用
        echo -e "${YELLOW}  安装应用到模拟器...${NC}"
        xcrun simctl install "$SIMULATOR" "$APP_PATH"

        if [ $? -ne 0 ]; then
            echo -e "${RED}❌ 应用安装失败${NC}"
            exit 1
        fi

        # 启动应用
        echo -e "${YELLOW}  启动应用...${NC}"
        xcrun simctl launch --console "$SIMULATOR" "$BUNDLE_ID"

        # 打开模拟器窗口
        open -a Simulator

        echo -e "${GREEN}✅ 应用已启动!${NC}"
    fi

    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║          构建完成! 🎉                              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════╝${NC}"
    echo ""

    if [ "$ACTION" = "build" ]; then
        echo -e "${BLUE}应用位置:${NC}"
        echo -e "  ${CYAN}${APP_PATH}${NC}"
        echo ""
        echo -e "${BLUE}下一步:${NC}"
        echo -e "  运行应用: ${YELLOW}./build-ios-app.sh --run${NC}"
        echo ""
    fi
fi


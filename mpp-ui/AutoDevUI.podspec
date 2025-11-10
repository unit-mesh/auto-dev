Pod::Spec.new do |spec|
  spec.name                     = 'AutoDevUI'
  spec.version                  = '1.0.0'
  spec.homepage                 = 'https://github.com/unit-mesh/auto-dev'
  spec.source                   = { :git => 'Not Published', :tag => 'Cocoapods/#{spec.name}/#{spec.version}' }
  spec.authors                  = 'Unit Mesh'
  spec.license                  = 'MPL-2.0'
  spec.summary                  = 'AutoDev Compose Multiplatform UI for iOS'

  spec.ios.deployment_target    = '14.0'
  spec.libraries                = 'c++'

  # 依赖 mpp-core
  spec.dependency 'AutoDevCore'

  # 根据架构选择正确的 framework
  spec.vendored_frameworks      = 'build/bin/iosSimulatorArm64/debugFramework/AutoDevUI.framework'

  # Pod 准备命令 - 在 pod install 时编译 framework
  spec.prepare_command = <<-CMD
    set -e
    cd ..

    # 检测架构
    ARCH=$(uname -m)
    if [ "$ARCH" = "arm64" ]; then
      TARGET="IosSimulatorArm64"
    else
      TARGET="IosX64"
    fi

    echo "Building AutoDevUI for $TARGET..."
    ./gradlew :mpp-ui:linkDebugFramework$TARGET
  CMD
end


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
  # Keep build outputs so CocoaPods treats the spec as non-empty
  spec.preserve_paths           = 'build/bin/**/*'

  # Note: AutoDevUI framework already exports AutoDevCore (configured in build.gradle.kts)
  # No need to add AutoDevCore as a separate dependency to avoid duplicate symbols

  # Pod 准备命令 - 在 pod install 时编译 framework（作为兜底，主要通过 Podfile 的 FRAMEWORK_SEARCH_PATHS 进行链接）
  spec.prepare_command = <<-CMD
    set -e
    cd ..

    echo "Building AutoDevUI frameworks (simulator + device) ..."
    ./gradlew :mpp-ui:linkDebugFrameworkIosSimulatorArm64 \
               :mpp-ui:linkDebugFrameworkIosX64 \
               :mpp-ui:linkDebugFrameworkIosArm64
  CMD
end


# [](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.7...v) (2025-01-24)

## [Unreleased]

## [2.0.0-alpha.7] - 2025-01-24

### Bug Fixes

- **devins-lang:** update dir.devin example to use correct syntax ([ea93fa3](https://github.com/unit-mesh/auto-dev/commit/ea93fa30aab1b92a3bfbe475371535bc72e68000))
- **git:** handle unformatted commit message text [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([bc5c4e9](https://github.com/unit-mesh/auto-dev/commit/bc5c4e9282265db648b7974b13d2207f514b0bb8))
- **parser:** add 'bash' as alias for 'Shell Script' in CodeUtil [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8b6816f](https://github.com/unit-mesh/auto-dev/commit/8b6816fccf7ceec644baa6efcca983aa66fe1021))
- **run:** handle run failure and cleanup scratch file [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8a2e422](https://github.com/unit-mesh/auto-dev/commit/8a2e422c4654a5e9163664cac3be04e67ec7a7b8))

### Features

- **build:** add support for platform version 223 [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([ec85cfb](https://github.com/unit-mesh/auto-dev/commit/ec85cfb934ed4833514b8ebe63d0bbf1cea8c021))
- **release-note:** add template-based release note generation [#256](https://github.com/unit-mesh/auto-dev/issues/256) ([76943cf](https://github.com/unit-mesh/auto-dev/commit/76943cf3940211a52b708c5d076f733450f49f93))
- **settings:** add DevOps configurable provider [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([5153032](https://github.com/unit-mesh/auto-dev/commit/51530326d65158cda633c6f9c602e1241bcaa6d6))

### Reverts

- Revert "refactor(terminal): replace terminal text extraction method" ([065c149](https://github.com/unit-mesh/auto-dev/commit/065c1496fdb7aaddf6e0a251ac0d832d0a3dbda4))
- Revert "ci(java): comment out failing or unused test cases" ([ad5df09](https://github.com/unit-mesh/auto-dev/commit/ad5df098304301d0fbb6a5c583cf1727b49463e7))

## [1.8.18](https://github.com/unit-mesh/auto-dev/compare/v1.8.17...v[1.8.18]) (2024-12-28)

### Bug Fixes

- **android:** handle exceptions in isApplicable method ([8725848](https://github.com/unit-mesh/auto-dev/commit/872584853e9772bfe89fe06ef0a62b3735b41594))
- **code-review:** 移除代码审查模板中的冗余代码块标记 ([028d881](https://github.com/unit-mesh/auto-dev/commit/028d8817e2ca78041c4b19b335efadfdadcefadc))
- **gui:** handle exceptions in AutoDevRunDevInsAction ([9572a61](https://github.com/unit-mesh/auto-dev/commit/9572a614a3189b48353752958d6e92ddc8b04c70))
- **gui:** simplify document text access in AutoDevInputSection ([f5236c8](https://github.com/unit-mesh/auto-dev/commit/f5236c84b9c6da1ea33934314018c8afc15be7ac))
- **llms:** skip empty messages in CustomSSEProcessor ([f9337d6](https://github.com/unit-mesh/auto-dev/commit/f9337d6e49163594e797128f259c5e42fccf342a))

### Features

- **agent:** add Dify API support and enhance custom agent config [#251](https://github.com/unit-mesh/auto-dev/issues/251) ([9cc5268](https://github.com/unit-mesh/auto-dev/commit/9cc526862783b14bbcf578e4874e330198bf855f))
- **chat:** enhance chat handling with history and response tracking ([803e184](https://github.com/unit-mesh/auto-dev/commit/803e184d8388b16643d5aaadfb2f24209509b86b)), closes [#251](https://github.com/unit-mesh/auto-dev/issues/251)
- **code-review:** 添加 diff 上下文展示和版本更新 ([eba213a](https://github.com/unit-mesh/auto-dev/commit/eba213a22203f676f8048057aa982a0603348f8a))
- **completion:** enhance agent selection in completion ([3b1b3bb](https://github.com/unit-mesh/auto-dev/commit/3b1b3bb4ba156a480ad1dc1a1d5eb12140578bc2))
- **custom:** add dynamic field replacement in custom requests [#251](https://github.com/unit-mesh/auto-dev/issues/251) ([ba50600](https://github.com/unit-mesh/auto-dev/commit/ba506007b887a573811da2512dac8cbfcc96499e))
- **parser:** add support for shell script and HTTP request language names ([c7d5f80](https://github.com/unit-mesh/auto-dev/commit/c7d5f80f4843082f4d7cec2e04bc052e09ef0543))
- **ui:** enhance cursor positioning and UI adjustments ([ccbe3e2](https://github.com/unit-mesh/auto-dev/commit/ccbe3e23f0d0c6d8b6c61262379450a3960bf9c1))

## [1.8.17](https://github.com/unit-mesh/auto-dev/compare/v1.8.16...v[1.8.17]) (2024-12-11)

### Bug Fixes

- **parser:** correct newline escaping in test cases ([b0cc99a](https://github.com/unit-mesh/auto-dev/commit/b0cc99a4d4102c993099e247cb66d34b2b3e7956))
- **parser:** remove unnecessary newline replacement ([831f747](https://github.com/unit-mesh/auto-dev/commit/831f747f5101ef9ad63dcf756adfc40456820daf))

### Features

- **code-completion:** enhance inlay code completion and formatting ([c2585bb](https://github.com/unit-mesh/auto-dev/commit/c2585bba132a5345b1508c51e14185e7efee19a9))

## [1.8.16](https://github.com/unit-mesh/auto-dev/compare/v1.8.15...v[1.8.16]) (2024-12-07)

### Bug Fixes

- **builder:** 优化上下文提供者处理逻辑 ([c5e7fb5](https://github.com/unit-mesh/auto-dev/commit/c5e7fb5ceec808621be60804eb0b48827b9217a3))
- **core:** Optimize TestCodeGenTask and JSAutoTestService logic ([276342c](https://github.com/unit-mesh/auto-dev/commit/276342cf9d9615348cac134d8f0f967651592414))
- **core:** 修复自定义提示配置加载时的日志记录 ([bdf63f5](https://github.com/unit-mesh/auto-dev/commit/bdf63f5a625ef327a6aaec8499ab2ca5c9816fd4))
- **CustomSSEProcessor:** 支持自定义大模型 customFields 使用复杂类型字段 ([#248](https://github.com/unit-mesh/auto-dev/issues/248)) ([8c0bb92](https://github.com/unit-mesh/auto-dev/commit/8c0bb9201945ab75a62965b3a22346e0b644223f))
- **java:** Clean up PsiMethod and improve MethodContext creation ([19647cf](https://github.com/unit-mesh/auto-dev/commit/19647cf56d6fe65674b2d1923f11042fe6c69a60))
- **java:** 优化 findRelatedClasses 方法以避免不必要的类型解析 ([9d3340f](https://github.com/unit-mesh/auto-dev/commit/9d3340fbbd70766109ec24abc6ac222861e6c175))
- **LLMSettingComponent:** 修复新装用户引擎未选择情况下的 `Array contains no element matching the predicate.` 异常 ([#247](https://github.com/unit-mesh/auto-dev/issues/247)) ([10848e3](https://github.com/unit-mesh/auto-dev/commit/10848e3a4077bbbaeccb79d35c8349aaad1da31c))
- **tests:** update Kotlin test cases with proper syntax and semicolon usage ([a54aabd](https://github.com/unit-mesh/auto-dev/commit/a54aabd5fdc61775a519383e1953f872e3348846))

### Features

- **document:** Add examples to custom living doc prompt builder ([dd2cd39](https://github.com/unit-mesh/auto-dev/commit/dd2cd39e04ec002323a927da0065039a2da0c7ef))
- **java:** add support for AssertJ and update JUnit detection logic ([e1e9c26](https://github.com/unit-mesh/auto-dev/commit/e1e9c26cbf660ed20c180edf65e79ef049d005ca))

## [1.8.15](https://github.com/unit-mesh/auto-dev/compare/v1.8.12...v[1.8.15]) (2024-11-16)

### Reverts

- Revert "chore(plugin): update IntelliJ dependency and add JSON module config" ([f49134b](https://github.com/unit-mesh/auto-dev/commit/f49134bf550957556a2fcaeb873ee1b7d4230d16))

## [1.8.12](https://github.com/unit-mesh/auto-dev/compare/v[1.8.12]-ALPHA...v[1.8.12]) (2024-10-05)

### Features

- **build:** add kotlinx serialization plugin and dependency [#239](https://github.com/unit-mesh/auto-dev/issues/239) ([055633c](https://github.com/unit-mesh/auto-dev/commit/055633c1f992483648a54aa10a38cfd040833d4e))

## [1.8.12-ALPHA](https://github.com/unit-mesh/auto-dev/compare/v1.8.11...v[1.8.12-ALPHA]) (2024-09-26)

### Features

- **build.gradle.kts:** add IntelliJ platform plugins and tasks [#236](https://github.com/unit-mesh/auto-dev/issues/236) ([a884459](https://github.com/unit-mesh/auto-dev/commit/a884459b6cb57e426bc94decebd4283fa96b0a14))
- **build:** upgrade Gradle version and IntelliJ plugin [#236](https://github.com/unit-mesh/auto-dev/issues/236) ([82c9ab5](https://github.com/unit-mesh/auto-dev/commit/82c9ab540c5cf0849b813da7081bc94631a4137d))
- **github-actions:** update build workflow and split tasks into separate jobs ([a92966b](https://github.com/unit-mesh/auto-dev/commit/a92966bca0262f885131dbdded5ea6b45bcba4ee))
- **gradle:** increase JVM memory for Kotlin and Gradle [#236](https://github.com/unit-mesh/auto-dev/issues/236) ([ddd30c1](https://github.com/unit-mesh/auto-dev/commit/ddd30c1a207083b59be2cf2a0beb399c60415b20))

## [1.8.11](https://github.com/unit-mesh/auto-dev/compare/v1.8.9-SNAPSHOT...v[1.8.11]) (2024-09-08)

### Bug Fixes

- **gui:** handle exceptions in language detection ([40f1c0d](https://github.com/unit-mesh/auto-dev/commit/40f1c0d89d8e47a2a8f64c703d0c23cbf011a2d8))
- **provider:** return immediately in handleFromType for PsiClassType ([6c92163](https://github.com/unit-mesh/auto-dev/commit/6c921634ac130289a4ca9ab0e9eec33b5b6157c3))
- Unable to receive notifications when changes are made to the document ([#228](https://github.com/unit-mesh/auto-dev/issues/228)) ([22cd295](https://github.com/unit-mesh/auto-dev/commit/22cd29579053e60f9695ae4dfc3751039d67c6a9))

## [1.8.9-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v1.8.8...v[1.8.9-SNAPSHOT]) (2024-08-09)

### Bug Fixes

- **settings:** update comparison and remove unused dependency ([853dd9d](https://github.com/unit-mesh/auto-dev/commit/853dd9d377f66ee5245e80707cdc36511717b16c))

### Features

- **embedding:** add LocalEmbedding class for text embedding [#200](https://github.com/unit-mesh/auto-dev/issues/200) ([2af87cc](https://github.com/unit-mesh/auto-dev/commit/2af87ccb07d38cdd80b91c55f3a0ea9b1d889770))
- **embedding:** implement in-memory and disk-synchronized embedding search indices [#200](https://github.com/unit-mesh/auto-dev/issues/200) ([6d5ca70](https://github.com/unit-mesh/auto-dev/commit/6d5ca70311af20d4ac1b84816f6c3194297f22b1))

## [1.8.8](https://github.com/unit-mesh/auto-dev/compare/v1.8.7-RELEASE...v[1.8.8]) (2024-08-07)

### Bug Fixes

- **codecomplete:** handle empty range in collectInlays function ([48901f8](https://github.com/unit-mesh/auto-dev/commit/48901f8ae6f53982da2ae3bfc88b6d6547557cde))
- **devti:** fix response body handling in ResponseBodyCallback [#209](https://github.com/unit-mesh/auto-dev/issues/209) ([cef9581](https://github.com/unit-mesh/auto-dev/commit/cef958174117dd609edbab6c52b789b29cb19c70))
- Failed to reset on the autoDevSettings UI ([8b16443](https://github.com/unit-mesh/auto-dev/commit/8b164436c39a19467e3bc4c23a44d84fab89baae))
- fix import ([50f3c8b](https://github.com/unit-mesh/auto-dev/commit/50f3c8be3d1ee8183e28c569376db87066e2b55f))
- **LLMInlayManager:** use InlayModelImpl for inlay model ([86d0840](https://github.com/unit-mesh/auto-dev/commit/86d08408a031f29ce28743b703932bea9f2ef876))
- wrong trigger when user typing from code. ([08f8fbb](https://github.com/unit-mesh/auto-dev/commit/08f8fbb5a9ed06337f22b1a2a852969fa472edeb))

### Features

- **diff-simplifier:** Include binary or large changes in output ([8fc6255](https://github.com/unit-mesh/auto-dev/commit/8fc625507fae4fa26c39d6963fa23c87f06662f3))
- **JavaAutoTestService:** update cocoa-core dependency and remove unused import ([56a6fa3](https://github.com/unit-mesh/auto-dev/commit/56a6fa37a1eb65d6439033a7b2c3cc0dc9ae51c0))
- **service:** add support for creating Maven run configuration [#164](https://github.com/unit-mesh/auto-dev/issues/164) ([cdc003a](https://github.com/unit-mesh/auto-dev/commit/cdc003ae040cbaa20898a3f891295852c4b6f969))
- **smartpaste:** add SmartCopyPasteProcessor ([4b427b4](https://github.com/unit-mesh/auto-dev/commit/4b427b46cb861544f79cd9e10ae5e5ab66e80677))

## [1.8.7-RELEASE](https://github.com/unit-mesh/auto-dev/compare/v1.8.7-SNAPSHOT...v[1.8.7-RELEASE]) (2024-06-13)

### Bug Fixes

- **java:** simplify JavaVersionProvider isApplicable method ([4c20a81](https://github.com/unit-mesh/auto-dev/commit/4c20a8123cbc19459548cb1732392463eae2e210))

## [1.8.7-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v1.8.6-RELEASE...v[1.8.7-SNAPSHOT]) (2024-06-06)

### Bug Fixes

- **context:** handle exceptions in ClassContextProvider [#199](https://github.com/unit-mesh/auto-dev/issues/199) ([347c452](https://github.com/unit-mesh/auto-dev/commit/347c4522a1b682f5300aaa1372c1592ef243bf58))

### Features

- **api-test:** add API test request generation template [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([177a66c](https://github.com/unit-mesh/auto-dev/commit/177a66ce551564747a035cbac75b6d0762fe6850))
- **api-test:** add HttpClient API test generation [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([5c982af](https://github.com/unit-mesh/auto-dev/commit/5c982af9264650c0de34f2d9ef2866e791ed6c00))

## [1.8.6-RELEASE](https://github.com/unit-mesh/auto-dev/compare/v1.8.6-SNAPSHOT...v[1.8.6-RELEASE]) (2024-05-30)

### Bug Fixes

- **flow:** handle null case for base package name ([14b5656](https://github.com/unit-mesh/auto-dev/commit/14b56563fb75424855483aec0630372f9fe01194))
- **flow:** update code creation flag in JvmAutoDevFlow ([c9cebfe](https://github.com/unit-mesh/auto-dev/commit/c9cebfef375a1238f02990b537a9dca1892b1c5b))
- **parser:** update regex pattern for code block [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([f08942b](https://github.com/unit-mesh/auto-dev/commit/f08942bc3b8c5e2c59e730993a52cdab999cac16))
- **runner:** handle null result in test execution [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([b70900c](https://github.com/unit-mesh/auto-dev/commit/b70900c9b7009d80589ce3022ad1ae1622cbc4da))

### Features

- **actions:** add AutoTestInMenuAction for batch testing [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([1719118](https://github.com/unit-mesh/auto-dev/commit/1719118168de5b59fd6b5d805eff62a827720380))
- **actions:** add batch test generation capability [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([7e4f6ed](https://github.com/unit-mesh/auto-dev/commit/7e4f6ed8e86f34c89bc8c5525e1088927548a67a))
- **http-client:** add support for creating HttpRequestRunConfiguration [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([30efd49](https://github.com/unit-mesh/auto-dev/commit/30efd493c3598317b772a155cde4fd4dd81b79c2))
- **http-client:** add support for creating temporary scratch files [#198](https://github.com/unit-mesh/auto-dev/issues/198) && closed [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([71c13c9](https://github.com/unit-mesh/auto-dev/commit/71c13c9912e3c7e48e3bd432f2fe27fcd3813294))
- **http:** add HttpClientProvider interface and implementation [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([8434fd3](https://github.com/unit-mesh/auto-dev/commit/8434fd30eacb3406c59d603292b5f237e4428c91))
- **http:** add support for executing HTTP requests from files [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([2c158ea](https://github.com/unit-mesh/auto-dev/commit/2c158ea3fa5d63608ed89754230ee25e39692eaf))
- **intentions:** add test verification feature [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([4080c38](https://github.com/unit-mesh/auto-dev/commit/4080c38f1ffae9ffbdbc9f3885dce166e3222e1c))
- **prompting:** improve API test request generation ([72225bb](https://github.com/unit-mesh/auto-dev/commit/72225bba7ae2c9f7a18034b9323216b3f372c5ac)), closes [#196](https://github.com/unit-mesh/auto-dev/issues/196)

## [1.8.6-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v1.8.5-SNAPSHOT...v[1.8.6-SNAPSHOT]) (2024-05-29)

### Bug Fixes

- **compiler:** use FileDocumentManager to get current file in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([871c4df](https://github.com/unit-mesh/auto-dev/commit/871c4dfcc37d7ac1d424d8c763b1a7612c4fc4e8))
- **custom-action:** add selectedRegex field to CustomIntentionConfig [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([c30ac7f](https://github.com/unit-mesh/auto-dev/commit/c30ac7f192a96110c9ee063aff07c9eac9a9ca4a))
- **gui:** use getVirtualFile method for editor document for version-222 ([bdb92b3](https://github.com/unit-mesh/auto-dev/commit/bdb92b30145a2216810d3de347bea8fa1af8d77e))
- **refactoring-tool:** update rename method call in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([bbc1f95](https://github.com/unit-mesh/auto-dev/commit/bbc1f95980fa33868f4c022ae81cf5301ebcdffc))
- **refactoring:** trim input strings and handle null psiFile [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([4a5c634](https://github.com/unit-mesh/auto-dev/commit/4a5c634c9c84e182957b3f08d51be487114a124d))
- remove AI_Copilot from floating code toolbar for [#222](https://github.com/unit-mesh/auto-dev/issues/222) ([8875290](https://github.com/unit-mesh/auto-dev/commit/8875290d08638a32c5725180a0482d0526ba285b))
- **test:** improve error message for test syntax errors ([24f7c16](https://github.com/unit-mesh/auto-dev/commit/24f7c160ec85a3fb8cca4839b0204fe1f2e6e9fc))

### Features

- **action-test:** add extContext support and RAG execution functionality [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([6166f38](https://github.com/unit-mesh/auto-dev/commit/6166f380f3201d47846819465439dca7089eb4f8))
- Add TypeScript refactoring support and improve JavaScript file context builder [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([c3956f2](https://github.com/unit-mesh/auto-dev/commit/c3956f244a479b367a392b4e5bab26196e96aec6))
- **agent:** add enabled flag to CustomAgentConfig [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([0dfaa32](https://github.com/unit-mesh/auto-dev/commit/0dfaa32149f7e572bb8542374f615971065c083f))
- **autodev-core:** update service implementations and action descriptions ([8e07e8e](https://github.com/unit-mesh/auto-dev/commit/8e07e8e90b9c17a04c34d089c16fb8b9f016cb57))
- **chat:** add refactor prompt to static code analysis results [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([30c28a1](https://github.com/unit-mesh/auto-dev/commit/30c28a1ffeb7e8d898cbd292f4a93934b0dc11a8))
- **chat:** update DevIn language instructions in RefactorThisAction [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([5d61d47](https://github.com/unit-mesh/auto-dev/commit/5d61d4738f20cb266e66ee164de8fd011aabccca))
- **chat:** update DevIn language support in RefactorThisAction [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([a08a979](https://github.com/unit-mesh/auto-dev/commit/a08a979beca0296d25282101ac30cf2f79712e9e))
- **custom-action:** add batchFileRegex and batchFiles method [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([b8a2c16](https://github.com/unit-mesh/auto-dev/commit/b8a2c16484a9db45e0ac1895a52bb3f99167e238))
- **editor:** replace findVirtualFile with FileDocumentManager in AutoDevRunDevInsAction ([1f92c36](https://github.com/unit-mesh/auto-dev/commit/1f92c36b247052c362947b20c096e56170720016))
- **ext-context:** add auto-test API endpoint with user stories ([8b7db54](https://github.com/unit-mesh/auto-dev/commit/8b7db54eb34b652881141082adee6720b6c46821))
- **FileGenerateTask:** Add 'codeOnly' option to generate code-only files [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([9a962ed](https://github.com/unit-mesh/auto-dev/commit/9a962ed8959712d5f364b217de9447907f1b26b6))
- **gui:** add RunDevIns action and refactor JavaRefactoringTool [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([d33f1da](https://github.com/unit-mesh/auto-dev/commit/d33f1da7db1b95dcef1f288b16d7e4fa7d0c40d9))
- **gui:** replace getVirtualFile with findVirtualFile and add RunDevIns action [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([1805bf3](https://github.com/unit-mesh/auto-dev/commit/1805bf356a283e460769a06eba2a59aaa16d3030))
- **intention-action:** introduce CustomExtContext and refactor TestCodeGenTask [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([fd12229](https://github.com/unit-mesh/auto-dev/commit/fd122291c4855f35cd0b6668b4b00f05d200c100))
- **JavaRefactoringTool:** enhance renaming functionality [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([929ab39](https://github.com/unit-mesh/auto-dev/commit/929ab39d2872ac5f4208cf98b66a46071746c59d))
- **JavaRefactoringTool:** replace RenameQuickFix with RenameUtil [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([0581307](https://github.com/unit-mesh/auto-dev/commit/058130714540e2af2b357434804bb09fa2ae643b))
- **JavaTypeUtil:** make resolveByType method public and limit class resolution ([39e5988](https://github.com/unit-mesh/auto-dev/commit/39e59887122d6f34d68635f89b42a6ebf442ec41))
- **language:** add refactoring function provider [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([b898971](https://github.com/unit-mesh/auto-dev/commit/b898971000b3e123a98b064b9c330fe6c619faea))
- **refactoring:** add JavaRefactoringTool implementation [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([65913f0](https://github.com/unit-mesh/auto-dev/commit/65913f02b370188cd54893ca291309c032dd15d8))
- **refactoring:** add KotlinRefactoringTool and refactor JavaRefactoringTool [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([0d2afd8](https://github.com/unit-mesh/auto-dev/commit/0d2afd8bcb86ec631a0b4da6fca44f07df65980b))
- **refactoring:** add RefactoringTool interface and extension point [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([62d7513](https://github.com/unit-mesh/auto-dev/commit/62d7513bec2b01e9437563ca54b2fbdaa578b9d0))
- **refactoring:** add RenameElementFix to JavaRefactoringTool [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([bb42c9e](https://github.com/unit-mesh/auto-dev/commit/bb42c9e9efd1e8e6496a46b0141f55247cd06507))
- **refactoring:** enhance Java refactoring support [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([bf1bbc9](https://github.com/unit-mesh/auto-dev/commit/bf1bbc90af56ad08dd2fcf595a1154825c6d88e0))
- **refactoring:** enhance refactoring commands and tools [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([7656757](https://github.com/unit-mesh/auto-dev/commit/7656757dcbda544106a94ad98a1e351cfc7ebfeb))
- **refactoring:** enhance rename functionality in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([154eb0d](https://github.com/unit-mesh/auto-dev/commit/154eb0de52beeb7959c54ea8829c228767259331))
- **refactoring:** enhance rename method with PsiFile parameter [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([6864cda](https://github.com/unit-mesh/auto-dev/commit/6864cda2de61d6be9fc51d8629c56809bef56119))
- **refactoring:** prioritize Java language in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([b6fe789](https://github.com/unit-mesh/auto-dev/commit/b6fe789fe4b66a045439f7c6b6ac7ae9e6ed07d1))
- **RefactoringTool:** add lookupFile method to interface and implementation [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([16de4cb](https://github.com/unit-mesh/auto-dev/commit/16de4cb4f2fead6cac98ffc01ed2b5fb64c5f6be))
- **RefactoringTool:** enhance rename method to support method renaming [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([0cfd2e8](https://github.com/unit-mesh/auto-dev/commit/0cfd2e854800d9857771fed89b0e003b008771a4))
- **team-exec-task:** enhance task with progress display and dynamic title #[#193](https://github.com/unit-mesh/auto-dev/issues/193) ([e627f6e](https://github.com/unit-mesh/auto-dev/commit/e627f6ec1af1ac5b81c9f06d4baf54e437946db8))
- **team-prompt:** add support for batch file processing and error notification [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([5629753](https://github.com/unit-mesh/auto-dev/commit/5629753d3b4cc5d3c6474ce9bd0f69c181030dc6))
- **team:** Add ReplaceCurrentFile interaction and batch file processing support [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([2b24045](https://github.com/unit-mesh/auto-dev/commit/2b24045ad8c95e41ddd16638251079f832680d10))
- **templating:** add comment prefixes for new languages ([f9498a9](https://github.com/unit-mesh/auto-dev/commit/f9498a99541a0dec6520d708c27c1f0853c17322))
- **test:** add support for custom RAG context [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([efb06f3](https://github.com/unit-mesh/auto-dev/commit/efb06f3287eb010860d12eda6ed1a9def8205532))
- **variables:** add ALL variable and glob matcher for batch files [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([66f4588](https://github.com/unit-mesh/auto-dev/commit/66f4588b72739b587a11393b8fe20a366fa1998b))

## [1.8.5-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.4-RC...v[1.8.5-RC]) (2024-05-24)

### Bug Fixes

- **lang:** refactor DevIn documentation provider registration && fixed [#191](https://github.com/unit-mesh/auto-dev/issues/191) ([fdf040f](https://github.com/unit-mesh/auto-dev/commit/fdf040ffd9ece69e449d117e723a0204347b4df6))
- **openai:** dsiable system prompt when empty request preparation [#190](https://github.com/unit-mesh/auto-dev/issues/190) ([b2a9c78](https://github.com/unit-mesh/auto-dev/commit/b2a9c78147ed6c6f567865cc17bae1db1b6e6a43))
- **provider:** improve error message and handle package declaration ([a494708](https://github.com/unit-mesh/auto-dev/commit/a49470882c007cf1af09ba06b61ce28ae44b0c42))

### Features

- **provider:** add class insertion if KotlinClass not found ([f1864b5](https://github.com/unit-mesh/auto-dev/commit/f1864b5e33d4524a01f53ff52ba511210c5e8274))
- **rigth-click-actions:** add 'Fix This' action for code refactoring assistance ([ec103c5](https://github.com/unit-mesh/auto-dev/commit/ec103c5c586e5e1b768bb61890427e0798f85bbf))
- **settings:** implement auto-dev update check on startup ([e317c27](https://github.com/unit-mesh/auto-dev/commit/e317c27835828d114e6605572c098e8a6bca0edc))

## [1.8.4-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.3-RC...v[1.8.4-RC]) (2024-05-23)

### Bug Fixes

- **practise:** correct boolean logic in suggestion filtering ([634fdd9](https://github.com/unit-mesh/auto-dev/commit/634fdd9c9afa7da650bbc29349d5b43edab2257e))
- **settings:** handle null project in LLMSettingComponent and AutoDevCoderConfigurable [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([c4c7ab8](https://github.com/unit-mesh/auto-dev/commit/c4c7ab8278a596028697a849c2ea5b2674638e7a))

### Features

- **chat:** add copy message functionality [#30](https://github.com/unit-mesh/auto-dev/issues/30) ([b3e7af6](https://github.com/unit-mesh/auto-dev/commit/b3e7af6ddff2cee8f4c7e929e443cbd28e9a92d8))
- **git-actions:** move Vcs commit message and release note suggestion actions to ext-git package and update autodev-core.xml to include them in the correct action groups. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([9ac18c0](https://github.com/unit-mesh/auto-dev/commit/9ac18c00f26ea6e7d3d96672c0de35a54f6787d4))
- **json-text-provider:** add new extension point and provider class for JSON text editing support. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([24a4923](https://github.com/unit-mesh/auto-dev/commit/24a4923e45564141f16f6f1cd84cb1cbbdf3ab61))
- **json-text-provider:** refactor to use extension point and remove duplicate code. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([2f04ba2](https://github.com/unit-mesh/auto-dev/commit/2f04ba216794fa5e1909f57ccaa7492ef35c77a0))
- **json-text-provider:** remove unused code to reduce dupcliate to keep same JsonField. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([33a6491](https://github.com/unit-mesh/auto-dev/commit/33a64911a01237db6c32ced96b3509e8f6d0bcbf))
- **local-bundle:** add new idea-plugin for improved local bundle management [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([d145593](https://github.com/unit-mesh/auto-dev/commit/d14559348f630b2f201156ab199e31bae7fe381e))
- **lookup-manager:** filter suggestions and update plugin version ([b53510b](https://github.com/unit-mesh/auto-dev/commit/b53510bdd394c752054ab08fa1d66f11c8012f2c))
- **openai:** add chat history control based on coderSetting state ([22f54d2](https://github.com/unit-mesh/auto-dev/commit/22f54d2057355cefa50c7227d91f71d42ca6dd6a))
- **openai:** enhance chat history management in OpenAIProvider ([2c893eb](https://github.com/unit-mesh/auto-dev/commit/2c893eb1057e5dd9c31a66f33c870daa0370d641))
- **python:** add __init__ and myfunc to PythonContextTest class and related imports. ([9f19dfb](https://github.com/unit-mesh/auto-dev/commit/9f19dfb4113cf48fa93b730e3b3d989f60576fca))
- **python:** add PythonContextTest class and related imports ([358f045](https://github.com/unit-mesh/auto-dev/commit/358f045c2607ba258462c98062387446db9be7d6))
- **python:** Optimize PythonAutoDevFlow and related components ([e15006e](https://github.com/unit-mesh/auto-dev/commit/e15006e6f9b5c9f6b3a78dc39ee7aff80dc65671))
- **python:** refactor PythonAutoTestService and related classes for improved performance and maintainability. ([d889c77](https://github.com/unit-mesh/auto-dev/commit/d889c77ba43b3fd2e1b440ed75f9d0f02a117e4f))
- **rename:** enhance suggestion filtering in RenameLookupManagerListener ([a167e87](https://github.com/unit-mesh/auto-dev/commit/a167e87db96d7566c3414610b1fea937db4cccfb))

## [1.8.3-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.2-RC...v[1.8.3-RC]) (2024-05-08)

### Bug Fixes

- **124:** support stop chat ([6ad68aa](https://github.com/unit-mesh/auto-dev/commit/6ad68aa2dfd272b9cf515ae23c65e0e47caff905))
- add missed import ([b604867](https://github.com/unit-mesh/auto-dev/commit/b6048675f198e625a7f3a63e6e3b7a27783c2e34))
- change prompts to fix issue [#156](https://github.com/unit-mesh/auto-dev/issues/156), but this is not the best way [#156](https://github.com/unit-mesh/auto-dev/issues/156) ([e4bb514](https://github.com/unit-mesh/auto-dev/commit/e4bb514a214be3bdb00b22a2844677c8e9700103))
- fix condition issue [#152](https://github.com/unit-mesh/auto-dev/issues/152) ([5e8588d](https://github.com/unit-mesh/auto-dev/commit/5e8588d52b93cb1beccd9c16e9a2b1d6d101d904))
- fix error ([4549dca](https://github.com/unit-mesh/auto-dev/commit/4549dcab2a7d6fad63297ee560df2057b02f9be8))
- fix path issue for [#152](https://github.com/unit-mesh/auto-dev/issues/152) ([62d9e78](https://github.com/unit-mesh/auto-dev/commit/62d9e78d47b4d01cebb5c1677b565bfe488cecfd))
- fix tests ([108fcda](https://github.com/unit-mesh/auto-dev/commit/108fcda2ef976412628ecffae12661a2fcc3aa7a))
- fix typos ([61482a6](https://github.com/unit-mesh/auto-dev/commit/61482a6f19178647fbeef54449c339be2e82d260))
- fix typos for [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([562c868](https://github.com/unit-mesh/auto-dev/commit/562c868ac073f5b9be8996404b4cb613d3e342af))
- **java:** remove unnecessary imports and update method signature for `prepareContainingClassContext` to be more flexible. ([34b4a3c](https://github.com/unit-mesh/auto-dev/commit/34b4a3c69549fd3deaf7f920d5a75ecb80c36720))
- **javascript:** remove JavaScriptContextPrompter and make JavaScript use DefaultContextPrompter [#151](https://github.com/unit-mesh/auto-dev/issues/151) ([4b8a2cd](https://github.com/unit-mesh/auto-dev/commit/4b8a2cd2ef6f908a0bbd5ed6781b640de8ac1fc9))
- **kotlin:** handle source file path in test generation service [#152](https://github.com/unit-mesh/auto-dev/issues/152) ([63e4fe1](https://github.com/unit-mesh/auto-dev/commit/63e4fe162d3c822389a987976a3045472e058c4b))
- **prompt:** refactor to use action instruction for prompt text [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([ea2bac2](https://github.com/unit-mesh/auto-dev/commit/ea2bac296602f496848e933ed6cb11f39a1b626a))
- **serialization:** add kotlinx.serialization support for decoding strings for [#149](https://github.com/unit-mesh/auto-dev/issues/149) ([110fab2](https://github.com/unit-mesh/auto-dev/commit/110fab2d519d27a3234b38e3d84a24a146ab3d7d))
- should not include unselected file when generate commit message [#160](https://github.com/unit-mesh/auto-dev/issues/160) ([97eba22](https://github.com/unit-mesh/auto-dev/commit/97eba22fd6281ca670232ba23a275818f8ebe1c8))
- should not send history when generate documentation ([47c800c](https://github.com/unit-mesh/auto-dev/commit/47c800c8dbda38c5ee0bd94774e833b0eabce1e5))
- should wrap runReadAction when read PsiElment fields [#154](https://github.com/unit-mesh/auto-dev/issues/154) ([1fe918f](https://github.com/unit-mesh/auto-dev/commit/1fe918fe4007fa984871c91a92abcd72c3fd9589))
- **spring:** update SpringMVC library detection logic for core frameworks mapping ([cf1fe51](https://github.com/unit-mesh/auto-dev/commit/cf1fe51e2f3216e85bcf936b1cff2ab54a968e39))
- wrap runReadAction [#154](https://github.com/unit-mesh/auto-dev/issues/154) ([90b99b7](https://github.com/unit-mesh/auto-dev/commit/90b99b7b8e554f0feb70560a954066f7e90b9906))

### Features

- add regex support to custom intention matching for [#174](https://github.com/unit-mesh/auto-dev/issues/174) ([dc1f94e](https://github.com/unit-mesh/auto-dev/commit/dc1f94eccc619b4d86e91b9220c6edd9d3ff28d4))
- can provider genericity parameter to LLM when generate test ([65e5117](https://github.com/unit-mesh/auto-dev/commit/65e5117ba03138b418937c5e4745df3223e30cbc))
- **compiler:** add support for DevInUsed in WriteInsCommand and fixed [#172](https://github.com/unit-mesh/auto-dev/issues/172) ([7bb3d77](https://github.com/unit-mesh/auto-dev/commit/7bb3d776f82f54c4d376859b0a68386e86f12b12))
- **console-action:** refactor FixThisAction to use ErrorPromptBuilder for better error handling and display. [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([3d4ded3](https://github.com/unit-mesh/auto-dev/commit/3d4ded343fc3358531a587880d050afd5a20f51c))
- **console-action:** refactor FixThisAction to use ErrorPromptBuilder for better error handling and display. [#151](https://github.com/unit-mesh/auto-dev/issues/151) ([003c10f](https://github.com/unit-mesh/auto-dev/commit/003c10feaf0d6263125fc15a44fa26925d86ca95))
- **csharp:** remove CsharpContextPrompter and refactor CSharpClassContextBuilder to use DefaultContextPrompter [#151](https://github.com/unit-mesh/auto-dev/issues/151) ([b71e8c3](https://github.com/unit-mesh/auto-dev/commit/b71e8c33603464dbc392e19323e788688d7932c7))
- **devins-lang:** add refactor commands and related test data [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([eaf0add](https://github.com/unit-mesh/auto-dev/commit/eaf0add0187774df05d5b8b065cbe253435de2fe))
- **devins-lang:** add refactor commands and related test data [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([4400d6f](https://github.com/unit-mesh/auto-dev/commit/4400d6fe9e3280a8245a7b8c1f0e09374f05f1a1))
- **devins-lang:** add rename refactor command and related test data [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([c8ff245](https://github.com/unit-mesh/auto-dev/commit/c8ff245e24d8b14c3ed581dc740a26d98f5d6cc6))
- **devins-lang:** add spike result for refactoring implementation for [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([23fd4cf](https://github.com/unit-mesh/auto-dev/commit/23fd4cff137cac9e740b2839dc29eeb36d0ddb7f))
- **devins-lang:** add support for flow control in DevInsPromptProcessor.kt and DevInsCompiler.kt [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([2908e41](https://github.com/unit-mesh/auto-dev/commit/2908e412e409849b304740a75c8ebe26fc5fe30b))
- **exts-git:** extract and rename git plugin support [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([eb55792](https://github.com/unit-mesh/auto-dev/commit/eb5579239858e4123c23d9be41c2693688639d5d))
- **exts-git:** extract git plugin support and rename refactoring implementation for [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([3970a5f](https://github.com/unit-mesh/auto-dev/commit/3970a5f681ae26915d89380c16b496b5511c18b7))
- **kotlin:** add  parameter to Version: ImageMagick 7.1.1-30 Q16-HDRI x86_64 22086 https://imagemagick.org ([84c0700](https://github.com/unit-mesh/auto-dev/commit/84c0700a64f91a515fc3a090bcf2b98be7890916))
- **kotlin:** optimize imports and refactor isService/isController methods for better readability. ([7b8e88a](https://github.com/unit-mesh/auto-dev/commit/7b8e88ad086fc18dae0fbeb92a83042701d7abf5))
- provide current class when generate test ([1cdd5e6](https://github.com/unit-mesh/auto-dev/commit/1cdd5e62c919123aa03c0cddab2d47203a93ae85))
- **rust:** remove RustContextPrompter and use RustVariableContextBuilder for variable context ([9b2b588](https://github.com/unit-mesh/auto-dev/commit/9b2b58898fd9eca360efacfec71af1bdb9ed39c6)), closes [#151](https://github.com/unit-mesh/auto-dev/issues/151)
- **stop:** update icon for [#149](https://github.com/unit-mesh/auto-dev/issues/149) && closed [#124](https://github.com/unit-mesh/auto-dev/issues/124) ([3fe1f1a](https://github.com/unit-mesh/auto-dev/commit/3fe1f1a06c2d8dae4f80f8c0c0140e42127ac37d))
- 增加新特性，可以正则过滤编辑器选中的内容，提问内容更加精简，明确 ([d47aec1](https://github.com/unit-mesh/auto-dev/commit/d47aec107870807f1532722d343c7f2b6bfe908b))

## [1.8.2-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.1...v[1.8.2-RC]) (2024-04-15)

### Bug Fixes

- **devins-lang:** optimize directory creation logic for file creation [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([42d1219](https://github.com/unit-mesh/auto-dev/commit/42d1219d3c440250fc68f3520db6350d7b4235e6))
- fix async issue for [#144](https://github.com/unit-mesh/auto-dev/issues/144) ([36923e4](https://github.com/unit-mesh/auto-dev/commit/36923e4e880a4c2b0cef4d70376ea37a3be19736))
- **test:** improve read access for non-identifier owners in TestCodeGenTask for typescript ([ba8c4a2](https://github.com/unit-mesh/auto-dev/commit/ba8c4a2eeb3745e8425277c85be8451cebe6b78a))

### Features

- **agent:** add support for DevIns language in code blocks [#144](https://github.com/unit-mesh/auto-dev/issues/144) ([ddd7565](https://github.com/unit-mesh/auto-dev/commit/ddd75651366a0629164cb0331f25f9d2ca0d7e40))
- **devins-lang:** add code parsing and notification for DevIns response [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([2bf268b](https://github.com/unit-mesh/auto-dev/commit/2bf268b67d13e948236ee8293c63705fec5f2996))
- **devins-lang:** optimize directory creation and add runInEdt for smoother execution [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([7724a74](https://github.com/unit-mesh/auto-dev/commit/7724a74dd7e4328ac761cb2b12949cf6383b0a64))
- **java:** add code parsing for LivingDocumentationTask ([aef1974](https://github.com/unit-mesh/auto-dev/commit/aef197429bfefef04634b748179c9fe55ddabbfa))

## [1.8.1](https://github.com/unit-mesh/auto-dev/compare/v1.8.0...v[1.8.1]) (2024-04-10)

### Bug Fixes

- **actions:** add ActionUpdateThread.EDT to ensure UI updates occur on the new EDT thread for smoother user experience. ([3890a73](https://github.com/unit-mesh/auto-dev/commit/3890a73bc3e4277677f64ab7a4e27f1f0520422b))
- **commit-message:** optimize commit message generation by incorporating the original commit message. ([5d8e08f](https://github.com/unit-mesh/auto-dev/commit/5d8e08f3d78265175440a32ffab1fd5521e4bac5))
- **compiler:** use consistent error message format across commands ([8ce54d0](https://github.com/unit-mesh/auto-dev/commit/8ce54d0bfa0dee4f55e81e70512e12bba57e81c3))
- delete duplicate name in zh.properties file ([f38a77d](https://github.com/unit-mesh/auto-dev/commit/f38a77d4f718136ee0b0c5a0a79595ad1b615366))
- **devins-lang:** remove unnecessary conditionals and simplify response handling ([18abafc](https://github.com/unit-mesh/auto-dev/commit/18abafc71023c1958f494d26bf9889be2bc3b09b))
- **devins-lang:** remove unused imports and refactor compiler creation logic ([2229789](https://github.com/unit-mesh/auto-dev/commit/2229789979cb6cf752959ca6f6eb695e0e3768e8))
- fix import issue ([d9a4762](https://github.com/unit-mesh/auto-dev/commit/d9a4762656bc2a1b9ac5a27b9c2c6b7da61431d7))
- fix package issue ([a6f9cf3](https://github.com/unit-mesh/auto-dev/commit/a6f9cf30f9d3a0da9a8a81c750f5716d409de9e7))
- **java:** add PsiErrorElement handling and collect syntax errors [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([e78eca4](https://github.com/unit-mesh/auto-dev/commit/e78eca49f0f6c1350d5f2c59da91b59652d3a9a4))
- **kotlin-provider:** add PsiErrorElement handling and collect syntax errors ([1a67593](https://github.com/unit-mesh/auto-dev/commit/1a67593b46111696b24fcf91986189190c5cecfb))
- **language-processor:** handle local commands in DevInsCustomAgentResponse ([ae8b4db](https://github.com/unit-mesh/auto-dev/commit/ae8b4db126938266ec022e50bdddf157b2e31b13))
- **llm:** handle null response in CustomSSEProcessor ([5de1db3](https://github.com/unit-mesh/auto-dev/commit/5de1db3ad8e5fe328e290128b59e49301fe66340))
- **llm:** handle null response in CustomSSEProcessor ([41f2c72](https://github.com/unit-mesh/auto-dev/commit/41f2c722cfa2ab1a74ef02619553ce7fd09c69a5))
- **refactor:** add PsiErrorElement handling and collect syntax errors [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([bbc691a](https://github.com/unit-mesh/auto-dev/commit/bbc691a59d5d16c633a5f2bc4db9b3c1a1428ca5))
- **refactor:** refactor rename lookup manager listener to use custom rename lookup element [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([82caa05](https://github.com/unit-mesh/auto-dev/commit/82caa058280256c1df8973a333687542416136b1))
- **rust:** update CargoCommandConfigurationType to support 241 version ([ed892d9](https://github.com/unit-mesh/auto-dev/commit/ed892d987a4963c2abd806136158f08077712b13))
- **terminal:** resolve compatibility issues in version 222 [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([559edb3](https://github.com/unit-mesh/auto-dev/commit/559edb356523e18c571fce81825ca2bb867fb9d0))
- **terminal:** resolve compatibility issues in version 222 by refactoring rename lookup manager listener to use custom rename lookup element and improving shell command input popup. This commit fixes the compatibility issues in version 222 of the terminal extension by refactoring the rename lookup manager listener to use a custom rename lookup element. It also improves the shell command input popup by using more appropriate imports and methods from the UIUtil class. ([c5916cd](https://github.com/unit-mesh/auto-dev/commit/c5916cd4026866a23496245e7d15eddb2974da07))
- **ui:** remove unnecessary todo comments and refactor binding logic for consistency ([dbfa022](https://github.com/unit-mesh/auto-dev/commit/dbfa022f9141200d30da2857f3eecc91d0f292cf))

### Features

- add Chinese to more UI ([0f9cc68](https://github.com/unit-mesh/auto-dev/commit/0f9cc684f9190da36c3c7c5c4bc7fbc89beed920))
- add nature language directory for support Chinese prompts template ([fd6b889](https://github.com/unit-mesh/auto-dev/commit/fd6b8899d3102836ffd723f5338b12906cda6b8a))
- **autodev:** add toggle in naming for [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([44008dc](https://github.com/unit-mesh/auto-dev/commit/44008dc0a538d441f1a4e55cbac6d483b27d029c))
- change default shortcut of inlay complete code ([0de56fe](https://github.com/unit-mesh/auto-dev/commit/0de56fe25999a01fd9c5a8b8dee02eae43e809df))
- **chat:** improve refactoring support by adding post-action support and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([2369148](https://github.com/unit-mesh/auto-dev/commit/23691481036e14e24c1dd8a94926a74af54e6238))
- **chat:** improve refactoring support by adding post-action support and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([e6ec4de](https://github.com/unit-mesh/auto-dev/commit/e6ec4de6e65c68f51e79d60a98d7fc5bbb6f4b8a))
- **commit-message:** improve commit message generation template for Chinese and English users, ensuring clarity and adherence to best practices. ([f80495f](https://github.com/unit-mesh/auto-dev/commit/f80495f555907788573cc66042af441a4f9ca9e6))
- **database:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([7526032](https://github.com/unit-mesh/auto-dev/commit/7526032bc55750aa0e8c02a25d47797a0c5b1807))
- **database:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([53412a9](https://github.com/unit-mesh/auto-dev/commit/53412a9fa1e861ec46b8664bcc68cd884e7413a2))
- **devins-lang:** add support for line info in commands ([72efaef](https://github.com/unit-mesh/auto-dev/commit/72efaef3601b210d7a4f868ff4127b70105041d9))
- **devins-lang:** add support for line info in commands ([d524095](https://github.com/unit-mesh/auto-dev/commit/d524095392ac70cf1d47bda378baffcaa3ed86b8))
- **devins-lang:** add support for line info in commands and improve symbol resolution formatting ([2d8d1f1](https://github.com/unit-mesh/auto-dev/commit/2d8d1f14181b21865f161abac3c4f21bd8f198b5))
- **devins-lang:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([a9b9b77](https://github.com/unit-mesh/auto-dev/commit/a9b9b777b57069ed11d429f0caeda0f3a191425f))
- **devins-lang:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([6872c07](https://github.com/unit-mesh/auto-dev/commit/6872c070529641f854e9f025ae53cf22a25b5d94))
- **devins-lang:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([12b768b](https://github.com/unit-mesh/auto-dev/commit/12b768b12960c4ab32260f18099d1a64cd329dc8))
- **devins-lang:** improve symbol resolution for file commands ([ef0ee46](https://github.com/unit-mesh/auto-dev/commit/ef0ee46bed15b8a41482ad5b41b0e22b4e896de0))
- **devins-lang:** refactor file handling logic to improve performance and add support for line info in commands. ([710e945](https://github.com/unit-mesh/auto-dev/commit/710e94524b05edda68a0bc10236bfe03b68cb559))
- **devins-lang:** try add support for parsing and verifying SQL scripts before inserting them into the editor. ([29b6e61](https://github.com/unit-mesh/auto-dev/commit/29b6e610be4e8daf1c0d913266f1a6bee0930791))
- **docs:** Enhance the commit message generation section of the Git documentation to include an optimized process for generating clear and accurate commit messages. ([1b34335](https://github.com/unit-mesh/auto-dev/commit/1b34335bf1d2bf51b60f85991604f1e4694273bf))
- **docs:** update refactoring documentation with additional examples and suggestions. [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([814493b](https://github.com/unit-mesh/auto-dev/commit/814493be0096a52ace0a0906056f2cdaba74b728))
- **ext-terminal:** add ShellSuggestContext class and documentation [#1235](https://github.com/unit-mesh/auto-dev/issues/1235) ([e815743](https://github.com/unit-mesh/auto-dev/commit/e815743ea5c337c2038dbc195a79bc3974f8af13))
- **ext-terminal:** introduce NewTerminalUiUtil class and refactor suggestCommand method to support new UI context [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([3533b1d](https://github.com/unit-mesh/auto-dev/commit/3533b1d5b2168767fb87efb5e231c9199a5d77fc))
- **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([712d45c](https://github.com/unit-mesh/auto-dev/commit/712d45c2e9a3d599864885933f39673c29981192))
- **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([41b4b7e](https://github.com/unit-mesh/auto-dev/commit/41b4b7e9a59a3f88918f0618c07a5d5f33afad20))
- **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([7d87fd0](https://github.com/unit-mesh/auto-dev/commit/7d87fd07e4d54abd539f54446277a9bafa85928f))
- **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([6d4e800](https://github.com/unit-mesh/auto-dev/commit/6d4e80087fb154ba1961a760899c4a969aa41a2a))
- **ext-terminal:** refactor suggestCommand method to support new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([60d15ee](https://github.com/unit-mesh/auto-dev/commit/60d15eec37c752fa1edea7476a1f9bd13449b6e4))
- **ext-terminal:** refactor suggestCommand method to support new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([633e393](https://github.com/unit-mesh/auto-dev/commit/633e3931eb353bfb6e2de99fffb78e7ae52fe75f))
- **i18n:** update Chinese translations for improved clarity and consistency. [#125](https://github.com/unit-mesh/auto-dev/issues/125) ([dbc4873](https://github.com/unit-mesh/auto-dev/commit/dbc4873f462f3e4c392a1299989d6de2cea96834))
- In generating code, remove markdown chars ([34c5c84](https://github.com/unit-mesh/auto-dev/commit/34c5c8442aa4c6a8bf2d17e5f20dbf14b469d6c1))
- **language-processor:** add support for DevIns language processing ([b2dcf1e](https://github.com/unit-mesh/auto-dev/commit/b2dcf1e92f3f8d90e91f2467f59b71546bb3e4d5))
- **language-processor:** add support for local commands in DevInsCustomAgentResponse ([4726633](https://github.com/unit-mesh/auto-dev/commit/4726633185f152629cdbae620ca3145ac89c254c))
- **prompt:** Enhance Chinese prompt generation logic to ensure generated text is clearer and more accurate, following best practices. ([793041d](https://github.com/unit-mesh/auto-dev/commit/793041de8b72634e8805802c52ceb45da873f4d1))
- **prompting:** improve logging of final prompts for better debugging and remove unnecessary println statement. ([c2b0fed](https://github.com/unit-mesh/auto-dev/commit/c2b0fed3faee0308b27c85db6897329445bcf91a))
- **prompting:** introduce BasicTextPrompt class to simplify prompt text construction and improve code readability [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([ac8fb12](https://github.com/unit-mesh/auto-dev/commit/ac8fb124fbab78fc84cde78a1c54211085b83bbf))
- **refactor:** add post-action support for refactoring and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([18818dc](https://github.com/unit-mesh/auto-dev/commit/18818dcbecf95210484dd99e8bd5f630e4bab93a))
- **refactor:** add PsiErrorElement handling and collect syntax errors [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([14092e0](https://github.com/unit-mesh/auto-dev/commit/14092e08eb25818af985d3a3d8784727e377f553))
- **refactor:** add RenameLookupElement to improve UI support and code completion [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([2d7d81c](https://github.com/unit-mesh/auto-dev/commit/2d7d81c53084eafa7bd58772f28ef9eb9724c0e8))
- **refactor:** add RenameLookupElement to improve ui support and code completion [#132](https://github.com/unit-mesh/auto-dev/issues/132) [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([5caa4c8](https://github.com/unit-mesh/auto-dev/commit/5caa4c8a53a07ff6a7cbb669e00b3595f86b37ec))
- **refactor:** add RenameLookupManagerListener to improve refactoring support by adding post-action support and code completion [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([529b72d](https://github.com/unit-mesh/auto-dev/commit/529b72dd43df26af7f630ee999193135f07caf5d))
- **refactoring:** add RenameLookupElement to improve UI support and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([6348f1e](https://github.com/unit-mesh/auto-dev/commit/6348f1e16b14f54638ece1a43b63ad39e3606bbe))
- **refactoring:** extract method for readable [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([0d1ac46](https://github.com/unit-mesh/auto-dev/commit/0d1ac460aa8ceff2c908379c9bb1ae141eb97ba9))
- **refactoring:** improve RenameLookupManagerListener by adding RenameLookupElement and using ApplicationManager to invoke the stream function in a non-blocking way, resolving [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([c1cc521](https://github.com/unit-mesh/auto-dev/commit/c1cc521b65fa6758a6fec942e67e1a7154328efb))
- **refactoring:** improve RenameLookupManagerListener by adding RenameLookupElement and using ApplicationManager to invoke the stream function in a non-blocking way, resolving [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([baf8809](https://github.com/unit-mesh/auto-dev/commit/baf880918b49772b6dc3e04b8ab29555caf5a0f3))
- **refactoring:** introduce template-based refactoring suggestions [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([f965194](https://github.com/unit-mesh/auto-dev/commit/f965194a92070f86da9dc1aef7ef26a8bb469dd7))
- **refactoring:** move check flag before run [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([a6a2e63](https://github.com/unit-mesh/auto-dev/commit/a6a2e63b642fb8a962bfbe8c4490cde7d64003ed))
- **refactoring:** try to improve user exp for lookup [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([976e809](https://github.com/unit-mesh/auto-dev/commit/976e80932b0a243f6e0d8a22422375008bd55353))
- rename package from prompts/openai to prompts/default ([5ba2525](https://github.com/unit-mesh/auto-dev/commit/5ba252577ef013fd73e6454111dbcaae03605d1c))
- **rename-suggestion:** improve rename suggestion logic and add logging [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([3324a79](https://github.com/unit-mesh/auto-dev/commit/3324a79cb38f24abdba65f1316e80714b3937f1c))
- **rename-suggestion:** improve rename suggestion logic and add logging [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([c70fea8](https://github.com/unit-mesh/auto-dev/commit/c70fea83ee97231e8989efd44fc327490de7cd7e))
- **rename-suggestion:** improve rename suggestion logic and add logging [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([184174c](https://github.com/unit-mesh/auto-dev/commit/184174ca8f14bb451957239cbf2376c4951c7d48))
- **settings:** add test connection feature for LLM settings ([c2dd6ba](https://github.com/unit-mesh/auto-dev/commit/c2dd6baf0a87924e1374b3d2b993208030ac509f))
- **settings:** rename 'testConnection' to 'testLLMConnection' and update related references. ([9c784d9](https://github.com/unit-mesh/auto-dev/commit/9c784d96162d77b9a74c4660223a0d39519d1f4d))
- **shell-suggest:** add today's date and OS information to context [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([fe3917a](https://github.com/unit-mesh/auto-dev/commit/fe3917a16aaf48240ed70f257e946c804e920dcf))
- should cut down prompts when they exceed the max token lenghth ([aafb936](https://github.com/unit-mesh/auto-dev/commit/aafb9360999bb9da141fa0a6f4b3833cd34f4226))
- should log exception error when bundle failed to set parent ([d130486](https://github.com/unit-mesh/auto-dev/commit/d130486e9fd3caf6d40177935af6898fe65e5378))
- should return en as default prompts template if target language template not exist ([31ffeee](https://github.com/unit-mesh/auto-dev/commit/31ffeeed229c8d63333195efe13704c6bb6eccec))
- simple prompts support Chinese ([a9633b3](https://github.com/unit-mesh/auto-dev/commit/a9633b319aeca3095ebbb3ad7cf78f67fb95fc56))
- support Chinese in UI ([49e773a](https://github.com/unit-mesh/auto-dev/commit/49e773a38dba77f5f74e2c85559b4ff201c5151f))
- **terminal:** add AI-generated shell script support [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([fc977a5](https://github.com/unit-mesh/auto-dev/commit/fc977a59395ce90d4dd0a35112d4e5a24c9fa0df))
- **terminal:** add basic support for AI-generated shell scripts [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([a2244d9](https://github.com/unit-mesh/auto-dev/commit/a2244d9204a72e091b7cefa5abe236248ebc3161))
- **terminal:** add compatibility support for 222 and 233 versions ([85175f8](https://github.com/unit-mesh/auto-dev/commit/85175f88a1bc77b8a8a562ffea5cbadae53ed623))
- **terminal:** add shell command suggestion feature [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([0c9a04b](https://github.com/unit-mesh/auto-dev/commit/0c9a04b347bfdcff728d45cd36a8a58ff0cc8690))
- **terminal:** add shell command suggestion feature [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([aea4114](https://github.com/unit-mesh/auto-dev/commit/aea41145e5ec77811cf73d2493441045d89bd8ce))
- **terminal:** add shell tool detection and context-aware command suggestions [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([5538584](https://github.com/unit-mesh/auto-dev/commit/55385847f2bee6ed6ef4a12f250882885a528687))
- **terminal:** add spike result for [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([e511179](https://github.com/unit-mesh/auto-dev/commit/e511179991a37689c4f6217ea284a7c5e5ecf5ad))
- **terminal:** improve shell command input popup [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([635ad7f](https://github.com/unit-mesh/auto-dev/commit/635ad7fcc7ef4e53c646b079d80d0cb17d4b0469))
- **terminal:** improve shell command suggestion output formatting [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([35bed12](https://github.com/unit-mesh/auto-dev/commit/35bed12e920e4ad2bdf5f361089212354fe186d8))
- **terminal:** improve shell command suggestion output formatting and add support for line commands ([c37ef91](https://github.com/unit-mesh/auto-dev/commit/c37ef910521fb253d41bd179003ec64d6d6d5b1e))
- **terminal:** update relative point to use RelativePoint for improved popup positioning ([5279966](https://github.com/unit-mesh/auto-dev/commit/5279966810ef4bc3595768375cb13ac4f0353093))
- **terminal:** update tooltip text for default message ([ae28f7d](https://github.com/unit-mesh/auto-dev/commit/ae28f7d92ae9c0cd80be33b005e3f588576ae372))
- translate some prompts template to Chinese ([c907ede](https://github.com/unit-mesh/auto-dev/commit/c907eded4acd8c4eb4efba1c18f453ae6238ed96))

## [1.7.5](https://github.com/unit-mesh/auto-dev/compare/v1.7.4...v[1.7.5]) (2024-03-29)

### Bug Fixes

- **custom-sse-processor:** handle non-standard response format and log parsing errors ([20dda56](https://github.com/unit-mesh/auto-dev/commit/20dda56980c963fce22a92077857f25170ad0ce3))
- **scala-test-service:** comment out code causing compatibility issues in version 222~232 ([92eb05e](https://github.com/unit-mesh/auto-dev/commit/92eb05e8fb52f34dc3a963c40c47490bcd426637))

## [1.7.4](https://github.com/unit-mesh/auto-dev/compare/v1.7.3...v[1.7.4]) (2024-03-28)

### Bug Fixes

- **core:** handle null response in JsonPath parsing ([7e60675](https://github.com/unit-mesh/auto-dev/commit/7e60675043123e566eb652fdf6acdc77f17670a8))
- **core:** openAI custom model not work as expected ([d4eee77](https://github.com/unit-mesh/auto-dev/commit/d4eee7778e6698db378292733b927f408bed7f78)), closes [#119](https://github.com/unit-mesh/auto-dev/issues/119)
- **devins-cpp:** move test config for Intellij IDEA 223 only, which is C++  test configurations and test discovery [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([fb588e3](https://github.com/unit-mesh/auto-dev/commit/fb588e30ca0d0b65e2c8d4a2c9df23dcf12c7e3b))
- **devins-lang:** add basic handle for exitCode=-1 to recall function ([6bcdf15](https://github.com/unit-mesh/auto-dev/commit/6bcdf159a05a2295895027a86cebc59ec9a78279))
- **devins-lang:** fix process termination listener [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([ff38ea9](https://github.com/unit-mesh/auto-dev/commit/ff38ea9b97d5e801883d889482d2c17d06fce192))
- **devins-lang:** handle nullable inputStream and improve string concatenation for better performance and readability. ([910daa0](https://github.com/unit-mesh/auto-dev/commit/910daa0446b15ddb5b9b8883ff41e3d3f49e7ce1))
- **devins-lang:** improve file content extraction ([5f8dc29](https://github.com/unit-mesh/auto-dev/commit/5f8dc29616978779e995b2fa941038cbe51b02be)), closes [#100](https://github.com/unit-mesh/auto-dev/issues/100)
- **devins-lang:** improve file writing performance [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([6340666](https://github.com/unit-mesh/auto-dev/commit/6340666063c9a1fc9ec395c6e9f9807a79b416b5))
- **error-handling:** ensure correct line range calculation in ErrorMessageProcessor ([fc47e49](https://github.com/unit-mesh/auto-dev/commit/fc47e492de703a33e64b7aba4d63baff3a7ea708))
- fix IDEA 222 error in get changes data ([faaa7c9](https://github.com/unit-mesh/auto-dev/commit/faaa7c922df5dd99e7375e289240cd7b07ca3cf0))
- **java-auto-test:** ensure thread safety when finding and parsing PsiJavaFile ([ee7a79c](https://github.com/unit-mesh/auto-dev/commit/ee7a79c407d2d0d64e4eac1403747c7e1195786b))
- **run-service:** ensure correct process lifecycle handling and remove unnecessary imports ([cdec106](https://github.com/unit-mesh/auto-dev/commit/cdec106daf1cf1413be870bd80cf8454c8fe5ac8))

### Features

- add custom AI engine setting for inlay code complete ([7de0431](https://github.com/unit-mesh/auto-dev/commit/7de0431b7fd49fddfe3817c9762030e12c76bb7a))
- add inlay code complete custom ai engine toggle in dev coder config ([268f309](https://github.com/unit-mesh/auto-dev/commit/268f309f7b798261f31fc85d9e92e43b2bc3edc7))
- **auto-test:** refactor and optimize auto-test service implementations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3c69b77](https://github.com/unit-mesh/auto-dev/commit/3c69b772f29011a3872bb81795cb7cc853fbc6ce))
- **browser:** init tool code [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([5ca636c](https://github.com/unit-mesh/auto-dev/commit/5ca636c7156178403ac8a855c78421df1c4e1b52))
- **devins-android:** init Android test service support ([24a5da1](https://github.com/unit-mesh/auto-dev/commit/24a5da1b2b28d538c2cfc04f418b81c02401e3c9))
- **devins-cpp:** add support for C++ test configurations and test discovery [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([23865dd](https://github.com/unit-mesh/auto-dev/commit/23865dd46f87d780534a91f337a642750524e320))
- **devins-cpp:** add support for IDEA version 222 OCLanguage in test discovery ([551d815](https://github.com/unit-mesh/auto-dev/commit/551d815950e47ae9d73973b8d0dcce598fd29305))
- **devins-cpp:** refactor for factory usage [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([9dd5c48](https://github.com/unit-mesh/auto-dev/commit/9dd5c48e8d4d7b82c66cdffc8b404ff4b8f9cd74))
- **devins-golang:** add support for Golang run configurations and test context provider [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([f1ddea0](https://github.com/unit-mesh/auto-dev/commit/f1ddea0ed6b3c597c3347edeb734d05cef114bfc))
- **devins-kotlin:** refactor RunService to use new ExecutionManager API [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([8e47d2e](https://github.com/unit-mesh/auto-dev/commit/8e47d2e59b7867bb374fd7e4747dedf1e14d41cc))
- **devins-lang:** add docs support for built-in command examples [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([ebacccd](https://github.com/unit-mesh/auto-dev/commit/ebacccd2617746eed607354d8bb80f729b7339bb))
- **devins-lang:** add markdown support for built-in command examples ([8bd3bce](https://github.com/unit-mesh/auto-dev/commit/8bd3bcecbe8228e1ae6fc0596e37a61b5e45527d))
- **devins-lang:** add support for browsing web content with new command `/browse` ([5e8fac4](https://github.com/unit-mesh/auto-dev/commit/5e8fac471a6c65bee450f1aa593fbd0892660c06))
- **devins-lang:** add support for built-in command examples [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([68fd6b6](https://github.com/unit-mesh/auto-dev/commit/68fd6b6afcc58144255494829d3631a041b4b207))
- **devins-lang:** add support for LLM responses in DevInsConversations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([5f9bf7f](https://github.com/unit-mesh/auto-dev/commit/5f9bf7faf520965d7a4250d8011c71942de9a8da))
- **devins-lang:** add support for processing flag comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([13b796f](https://github.com/unit-mesh/auto-dev/commit/13b796f7ba92d102b621ace5b965d88dd9fa8d03))
- **devins-lang:** improve conversation service and compiler [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([261780f](https://github.com/unit-mesh/auto-dev/commit/261780f17cc279599d2fd69e877875a325f995fd))
- **devins-lang:** introduce new ShellRunService to support running shell scripts. This service simplifies the execution of shell commands within the DevIns IDE, enhancing the user experience. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3ce99a7](https://github.com/unit-mesh/auto-dev/commit/3ce99a7799457ad57fadb50e5d659161f8bfffe8))
- **devins-lang:** introduce new ShellRunService to support running shell scripts. This service simplifies the execution of shell commands within the DevIns IDE, enhancing the user experience. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([1c48d01](https://github.com/unit-mesh/auto-dev/commit/1c48d01b0fc64bc99785125c0ab608ddadf57d37))
- **devins-lang:** refactor reorg conversation [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([56db7e5](https://github.com/unit-mesh/auto-dev/commit/56db7e5b6d1f92f2a25be53943837df4440d7785))
- **devins-lang:** refactor reorg conversation [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([fc307a9](https://github.com/unit-mesh/auto-dev/commit/fc307a93a6657846c65a32f0435532c97e307ad3))
- **devins-lang:** remove unused methods [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([cd1bf89](https://github.com/unit-mesh/auto-dev/commit/cd1bf897ee2b7a7af52d291f77af1af4d9cbe808))
- **devins-python:** add support for creating Python run configurations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([691fff5](https://github.com/unit-mesh/auto-dev/commit/691fff55851f71ea439bd6511b109f3ce67bf4cc))
- **devins-rsut:** add support for creating Rust run configurations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([802d634](https://github.com/unit-mesh/auto-dev/commit/802d634407606674cebeed09093e1698aeafa4dc))
- **devins-run:** add default langauge runner support for configurations and test discovery [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([49e2ae6](https://github.com/unit-mesh/auto-dev/commit/49e2ae698f1dc710fdd4bf12e7a15a3c5ed4ec1f))
- **devins-scala:** add support for Scala run configurations and test context provider [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([b687994](https://github.com/unit-mesh/auto-dev/commit/b6879946b2bd81ffb98d9940d839458d957e61b8))
- **run-service:** add support for specifying a test element when creating run configurations. This enhancement allows for more targeted and efficient execution of tests within the DevIns IDE. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3375f8a](https://github.com/unit-mesh/auto-dev/commit/3375f8ae3298021648f705854ae084a1244beb82))
- **run-service:** introduce new ShellRunService to support running shell scripts. This service simplifies the execution of shell commands within the DevIns IDE, enhancing the user experience. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([44b3859](https://github.com/unit-mesh/auto-dev/commit/44b3859da27a39c13d980c393beae186aed19420))
- **run-service:** refactor createConfiguration method to use PSI file lookup and create RunConfigurationSettings instance. This refactoring improves the readability and maintainability of the RunService class. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([939cfe7](https://github.com/unit-mesh/auto-dev/commit/939cfe77b684453fd0cbde7826d078e6cba9046a))
- **runner:** introduce new RunContext class and refactor RunServiceTask and RunServiceExt to use it. This change simplifies the execution context management and improves code readability. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3db0623](https://github.com/unit-mesh/auto-dev/commit/3db06232509f173e109d303167a6fe88e64d5e60))
- **scrapy:** add new browser tool and document cleaner ([2b95738](https://github.com/unit-mesh/auto-dev/commit/2b95738bf35d064b73676b4813bf7564499502da))
- **scrapy:** add new browser tool and document cleaner ([abcf8c0](https://github.com/unit-mesh/auto-dev/commit/abcf8c0a6977d7846bca0c75beb5bf33c862d62c))
- **scrapy:** refactor and improve document cleaning logic ([cc9f956](https://github.com/unit-mesh/auto-dev/commit/cc9f956b9ef1015080508f8af6681bcf28045578))
- **scrapy:** refactor and improve document cleaning logic ([041d743](https://github.com/unit-mesh/auto-dev/commit/041d7432bcd1112fddb07d973bf8afc75fd22223))
- **scrapy:** refactor and improve document cleaning logic [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([898f8ed](https://github.com/unit-mesh/auto-dev/commit/898f8ed8fbf51ad06ebc7e2882d522a52365c2d2))
- **scrapy:** refactor and improve document cleaning logic [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([12a0c92](https://github.com/unit-mesh/auto-dev/commit/12a0c92fd9b1a6dbe5cd8b7342dc384a755c1b51))
- should dispose inlay when esc ([b746704](https://github.com/unit-mesh/auto-dev/commit/b74670426f9b0e5b2e10bb8796cba8700ac12a81))
- use custom agent when inlay complete code ([d426ab3](https://github.com/unit-mesh/auto-dev/commit/d426ab3a86e5481ac9826a0ed47e95ed33c432df))

## [1.7.3](https://github.com/unit-mesh/auto-dev/compare/v1.7.2...v[1.7.3]) (2024-03-22)

### Bug Fixes

- **actions:** fix variable name in CommitMessageSuggestionAction ([edc3e8c](https://github.com/unit-mesh/auto-dev/commit/edc3e8cbeed3506db87e6d3a6de7d8e0b4d100a5))
- **codecomplete:** fix LLMInlayManager imports for 241 version [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([9cdca52](https://github.com/unit-mesh/auto-dev/commit/9cdca524b6c9d2519e4848243c92179ba438dd68))
- **compiler:** fix patch execution race condition [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([ab76784](https://github.com/unit-mesh/auto-dev/commit/ab76784233ace593b9043d15c478f0078c916888))
- **devins-java:** improve symbol resolution logic [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([fd6c134](https://github.com/unit-mesh/auto-dev/commit/fd6c134296c9f1d7051bdfeae6fa9fd274c85faa))
- **devins-lang:** add newline to "Done!" message [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([552d5b0](https://github.com/unit-mesh/auto-dev/commit/552d5b0e91c2f8453f51bb0f35b0fda98d04c754))
- **devins-lang:** correct highlighting for variable, agent, and command identifiers [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f83d305](https://github.com/unit-mesh/auto-dev/commit/f83d305fc5a62c75c3a06387f0f459bee7295824))
- **devins-lang:** fix asynchronous execution issue [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([2fc3b52](https://github.com/unit-mesh/auto-dev/commit/2fc3b5281ded43046b81edba44c98a2e36cde749))
- **devins-lang:** improve error handling and add validation for file-func command ([edbb0c5](https://github.com/unit-mesh/auto-dev/commit/edbb0c570f74f580dd168095140010dbf6a97428)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
- **devins-lang:** improve error message for duplicate agent calls [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9e726bc](https://github.com/unit-mesh/auto-dev/commit/9e726bca6389a233e8784001355d63b6845e5706))
- **devins-lang:** improve file selection message [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([35f950a](https://github.com/unit-mesh/auto-dev/commit/35f950abd95f3028fad0909a6f2c8dccd67dff8d))
- **devins-lang:** improve process handler creation for IDE 222 version ([b21925a](https://github.com/unit-mesh/auto-dev/commit/b21925a4f17aa7578bab68f27c939ed4125c255f))
- **devins-lang:** improve readability of SyntaxHighlighterFactory [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6a20095](https://github.com/unit-mesh/auto-dev/commit/6a20095574de5f67d90bdc65b2008247ccee85f6))
- **devins-lang:** Improve token type string representation and handle whitespace in agent ID regex [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([8e46a8a](https://github.com/unit-mesh/auto-dev/commit/8e46a8a55af8268b6cdc2103479143eca1862e2a))
- **devins-lang:** refactor language injection check [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([0e52bca](https://github.com/unit-mesh/auto-dev/commit/0e52bca6507f1407a86c48e6b7160307394de317))
- **devins-lang:** replace "DevliError" with "DevInsError" for consistency and clarity. ([8fdbba8](https://github.com/unit-mesh/auto-dev/commit/8fdbba8899643751f4d40b7d79a2d5accbc24949))
- **devins-lang:** restrict agent_id to non-whitespace characters [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([388d484](https://github.com/unit-mesh/auto-dev/commit/388d484bede47d5dfcb6a32687c6d06971d8a46e))
- **devins-language:** update ToolHubVariable property names [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3b12625](https://github.com/unit-mesh/auto-dev/commit/3b12625a3d1e306e8051d5f5ebf9e11553de2b4a))
- **devins-language:** use List instead of Iterable for lookup result [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([26b2581](https://github.com/unit-mesh/auto-dev/commit/26b2581bd45b1b96f81da4cea42e6dc89e56d5b1))
- **devins-linting:** improve detection of duplicate agent IDs in DevInsDuplicateAgentInspection [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([df2bb82](https://github.com/unit-mesh/auto-dev/commit/df2bb82e4af08afd1fe0db9a431cfbf1e923cf2f))
- **editor:** fix TypeOverHandler package name [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([a6a04ce](https://github.com/unit-mesh/auto-dev/commit/a6a04ced029552005eda0495021c9e89ee6011d5))
- fix flow issue ([2458221](https://github.com/unit-mesh/auto-dev/commit/2458221647c6975a9e759f01bd4571de11502ac3))
- fix merge error and typo ([a9e8b06](https://github.com/unit-mesh/auto-dev/commit/a9e8b06033199607bc8fb39e3cf4e7abf62e7b0f))
- **inlay:** fix compatibility issue with IDEA 241 version for [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([2ff3c17](https://github.com/unit-mesh/auto-dev/commit/2ff3c17832c593f22c6f5a06be39045fb940404d))
- **inlay:** update key names and message in LLMInlayManagerImpl.kt ([a5d5e4d](https://github.com/unit-mesh/auto-dev/commit/a5d5e4d039afa5e806813f7215dc73f4602295f5)), closes [#109](https://github.com/unit-mesh/auto-dev/issues/109)
- **java:** improve symbol resolution logic [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ef24e71](https://github.com/unit-mesh/auto-dev/commit/ef24e71350c088515359f944219c2fa98fdaa5b5))
- **java:** simplify package name lookup [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cd8ea46](https://github.com/unit-mesh/auto-dev/commit/cd8ea46f17aad68737bb0d43d209728e6e1c4335))
- pick up presentationUtil [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([216a231](https://github.com/unit-mesh/auto-dev/commit/216a2317af431acb3560a531fe46c32514e5e817))
- **provider:** fix console view initialization [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f402274](https://github.com/unit-mesh/auto-dev/commit/f402274a194201901b9aea3447f1e4e89b1233d7))
- refactor DevInBundle to use non-NLS strings and correct bundle name [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([fdbf0d0](https://github.com/unit-mesh/auto-dev/commit/fdbf0d0a487c847ed0df0854d0d67250fd08b867))
- **runconfig:** remove unnecessary log statements [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([0b976e0](https://github.com/unit-mesh/auto-dev/commit/0b976e0b8638bdaff13a16896bab4c8b9de8508c))
- **service:** fix canonicalName generation in JavaAutoTestService [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9c4da21](https://github.com/unit-mesh/auto-dev/commit/9c4da217aea947247ff56ca061ae65e3f31d4bf9))
- **test:** rename class and modify test case to assert null return for invalid regex pattern. ([b0d0ddf](https://github.com/unit-mesh/auto-dev/commit/b0d0ddf735669e557d6c879aa36ae57efc74a3c3))

### Features

- 222 support inlay code complete [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([26f933a](https://github.com/unit-mesh/auto-dev/commit/26f933a7dbec68f3201dc55f02f5440f40ffe39b))
- change inlay complete code trigger: use shortcut key instead of automatic ([581e56d](https://github.com/unit-mesh/auto-dev/commit/581e56de9e8121f60a0f5f1ac116c6c71faf2321))
- clean markdown chars and remove unused brace“ ([470ec20](https://github.com/unit-mesh/auto-dev/commit/470ec20f8e00a1eeea1122b9033e6b7afcf95ceb))
- **completion:** rename ToolHub to ToolHubVariable and update completion provider ([11cc6df](https://github.com/unit-mesh/auto-dev/commit/11cc6dfae43e6f8594777ae47c76c024b12a7b92))
- **completion:** replace DevInsCompletionProvider with DevInsSymbolProvider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1dc1fa2](https://github.com/unit-mesh/auto-dev/commit/1dc1fa2313ec94cdb046f79d53a04ec96684f34a))
- **devins-compiler:** add support for custom commands [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([fab54c0](https://github.com/unit-mesh/auto-dev/commit/fab54c0f858522bc20c395a7a2fdcc3b20d0221e))
- **devins-compiler:** Use VariableTemplateCompiler for variable compilation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7b79a33](https://github.com/unit-mesh/auto-dev/commit/7b79a33e5e61d96288dc55cb0546c6e3151bd443))
- **devins-documentation:** add support for custom variables in documentation provider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([4911ce2](https://github.com/unit-mesh/auto-dev/commit/4911ce20aeb2bb557ee8291bd21fd42f263619d5))
- **devins-java:** add package name completion [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d00d5a0](https://github.com/unit-mesh/auto-dev/commit/d00d5a0c1bae501b0b6dc80bd5b2dbc26b4916d3))
- **devins-java:** add resolveSymbol method to DevInsSymbolProvider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d20961f](https://github.com/unit-mesh/auto-dev/commit/d20961f579cc4b45a6bd8a7af5a04ad61f3b8e7c))
- **devins-java:** add support for resolving symbols in Java packages [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([da3628a](https://github.com/unit-mesh/auto-dev/commit/da3628a2bd8b7f6e285e9abaa95151f1464d11e1))
- **devins-java:** add support for retrieving module name [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([0cc6b54](https://github.com/unit-mesh/auto-dev/commit/0cc6b544fa3e0f79a2d4c91ac007cf82b0719faa))
- **devins-lang:** add documentation provider and refactor custom agent completion ([4c8a49b](https://github.com/unit-mesh/auto-dev/commit/4c8a49b2de4a2d384378c480ca94c32deb5ecd88)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
- **devins-lang:** add duplicate agent declaration inspection [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d2df012](https://github.com/unit-mesh/auto-dev/commit/d2df0124af9c6c46525316f5508b0c08573fac8e))
- **devins-lang:** add message filtering to console [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([db9b866](https://github.com/unit-mesh/auto-dev/commit/db9b866eba52e2f9bac66094ea4f6b752f9da02e))
- **devins-lang:** add SHELL command and related functionality [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d354989](https://github.com/unit-mesh/auto-dev/commit/d3549893a66b41b41d6084e1323f901b648663e6))
- **devins-lang:** add support for execution environment customization [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([3580aa3](https://github.com/unit-mesh/auto-dev/commit/3580aa31ddb839e1e2066fd23dadc6f1edada486))
- **devins-lang:** add support for file function autocomplete [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1d1552a](https://github.com/unit-mesh/auto-dev/commit/1d1552aa3ae61f74d292a95163192ea4ca4c354d))
- **devins-lang:** add support for highlighting single-line comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([64e5811](https://github.com/unit-mesh/auto-dev/commit/64e58117205c82eaebf37bca60dc9177be056db9))
- **devins-lang:** add support for load custom commands in language completion [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([7a5bb37](https://github.com/unit-mesh/auto-dev/commit/7a5bb371029cf7636474f714466c80c7cf9bd6d0))
- **devins-lang:** add support for single-line comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([b0740cd](https://github.com/unit-mesh/auto-dev/commit/b0740cd992534f9d8ba338c934cf9f3ea974a638))
- **devins-lang:** add support for single-line comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([e24f874](https://github.com/unit-mesh/auto-dev/commit/e24f87418d315251aa0db4f4a99041a55f0ff0ff))
- **devins-lang:** add support for system calling with identifiers and colon-separated parameters [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([37b88cf](https://github.com/unit-mesh/auto-dev/commit/37b88cfc878542dd56a88b481c7764ef0b4b70db))
- **devins-lang:** add task creation design [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([53c09f6](https://github.com/unit-mesh/auto-dev/commit/53c09f6eb48df7c1cb82a8dc8ce95a57061c608e))
- **devins-lang:** extract toolhub for variables [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([bb1c989](https://github.com/unit-mesh/auto-dev/commit/bb1c989d9c3451e006c50b4fb6786570b24aa62c))
- **devins-lang:** improve dynamic run configuration creation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f4c33bc](https://github.com/unit-mesh/auto-dev/commit/f4c33bcccc05217c4d336992669b8b46bcb2644f))
- **devins-lang:** refactor completion providers to use new naming convention and improve code readability. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([98e92c9](https://github.com/unit-mesh/auto-dev/commit/98e92c9a8f7cf814914f8a2bcf8be516803dfd81))
- **devins-lang:** refactor extract SymbolInsCommand class and remove old implementation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9219ac8](https://github.com/unit-mesh/auto-dev/commit/9219ac8300effce21cc0186a42592eeb6fc59988))
- **devins-language:** add DevInsRunListener for handling run events [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([e4b4aef](https://github.com/unit-mesh/auto-dev/commit/e4b4aef8ac0d033014eb3be8b1a1a0bd5e256d66))
- **devins-language:** add method to create DevInFile [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([59d5b23](https://github.com/unit-mesh/auto-dev/commit/59d5b236d9a00045b454ccdb285768e7a1f1c8ce))
- **devins-language:** add support for custom agent execution [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([35cfb7b](https://github.com/unit-mesh/auto-dev/commit/35cfb7b011e2682e46943ede5a57f48d91787447))
- **devins-language:** add support for detecting and reporting duplicate agent calls in DevInLanguage [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c03995c](https://github.com/unit-mesh/auto-dev/commit/c03995c8c23df943e48ab87083f9513570e82d53))
- **devins-lang:** update language bundle and related classes to use DevInBundle [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e3b7960](https://github.com/unit-mesh/auto-dev/commit/e3b79600f0fd4dfd1458e09e878e70f00647ccbe))
- **flow:** add support for custom flows [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([4bd0b56](https://github.com/unit-mesh/auto-dev/commit/4bd0b56a0de609c08af9130a0106773442d61036))
- **language:** add support for file function with dynamic file names [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([9dc36e5](https://github.com/unit-mesh/auto-dev/commit/9dc36e5a3950902ed7bca5d702236b551fa19029))
- **language:** add support for tool hub variables [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([2796660](https://github.com/unit-mesh/auto-dev/commit/2796660dcd8dae37db3e9cdf974f3fa5df6ef21f))
- **language:** improve code completion in DevIns language ([1cf4ae3](https://github.com/unit-mesh/auto-dev/commit/1cf4ae3ce2f5c594e6640d0ff3ffc079d878f15f))
- **provider:** add DevInsCompletionProvider and modify references [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6136ed5](https://github.com/unit-mesh/auto-dev/commit/6136ed5480b6fe30125c002015cfdf8372e06376))

### Reverts

- Revert "[liujia]update some idea file to setup project in local" ([e8959a7](https://github.com/unit-mesh/auto-dev/commit/e8959a788740ff6560501d21c077cdade19c4311))
- Revert "refactor: clean inlay model" ([93aa5a8](https://github.com/unit-mesh/auto-dev/commit/93aa5a8ff4dce4ccdb9e1e64a3318871df500f4a))

## [1.7.2](https://github.com/unit-mesh/auto-dev/compare/v1.7.1...v[1.7.2]) (2024-03-17)

### Bug Fixes

- **compiler:** improve handling of file paths and project roots [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a3d25f6](https://github.com/unit-mesh/auto-dev/commit/a3d25f6bd545ab3ef35772988b7bb0525be71f68))
- **completion:** improve completion provider for DevInTypes.COLON [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7f0c1cb](https://github.com/unit-mesh/auto-dev/commit/7f0c1cb8e4e4928d15aa01b777c7c974ebe69f10))
- **completion:** improve performance by using ReadAction and runBlockingCancellable [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([42449f1](https://github.com/unit-mesh/auto-dev/commit/42449f1927bf82b02074aeb450ff89caab27fc17))
- **completion:** try correct order of completion contributors and add background task for git commit history loading [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([33fea81](https://github.com/unit-mesh/auto-dev/commit/33fea815c08813a1a5c3534ccda7e6074a13b063))
- **custom-schema-provider:** correct class name and resource file reference ([3f2a973](https://github.com/unit-mesh/auto-dev/commit/3f2a973147908922f257de52b3c9d650766765e4))
- **devin-lang:** correct logging and enable action for non-zero offsets [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([b8c0bc2](https://github.com/unit-mesh/auto-dev/commit/b8c0bc21120a18b873b200d6fa7f348260742344))
- **devin-lang:** improve logging and fix compilation errors [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9443239](https://github.com/unit-mesh/auto-dev/commit/9443239fcb3b18bd74d3872502f6a2c0a9e6d04f))
- **devins-compiler:** fix error handling in DevInsCompiler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d38f4d1](https://github.com/unit-mesh/auto-dev/commit/d38f4d133e78054afc5a0cefc73c220ce371cfde))
- **devins-compiler:** fix result checking and code block skipping [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cffde71](https://github.com/unit-mesh/auto-dev/commit/cffde71b0d45be25427adda6fc48196971a6d998))
- **devins-lang:** fix console output formatting [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([97c26e9](https://github.com/unit-mesh/auto-dev/commit/97c26e9db0236a2dd35913f7c07931e27ef13e9c))
- **devins-lang:** fix output equality assertion ([ff4269d](https://github.com/unit-mesh/auto-dev/commit/ff4269dfcfb9cde18cf342b0a36cca22fa8d9d9f))
- **devins-lang:** improve file lookup logic in AutoCommand [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([8c25c78](https://github.com/unit-mesh/auto-dev/commit/8c25c7866eadbf30d74bd37cb2ab2b0caecf1e5b))
- **devins-lang:** modify PatchInsCommand.kt and InsCommand.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ad41f5b](https://github.com/unit-mesh/auto-dev/commit/ad41f5b5314a747c81218cfdaacf2a83342d7e84))
- **devins-lang:** use GenericProgramRunner for DevInProgramRunner [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([91acc81](https://github.com/unit-mesh/auto-dev/commit/91acc81947ca73ab0cb5c87cd8d0acc3383a5138))
- **devins:** fix condition to correctly process commands ([84de6c4](https://github.com/unit-mesh/auto-dev/commit/84de6c4e0b85a804502d610be64d814d23ce51ac))
- **devti-lang:** improve file reference completion provider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ded9c3c](https://github.com/unit-mesh/auto-dev/commit/ded9c3cf0286d1df1435cf0237ad5fb175f0eedc))
- **devti-lang:** improve run configuration handling [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cc8b04e](https://github.com/unit-mesh/auto-dev/commit/cc8b04e3fae3befd1c8770aa7810bd0bfe3d6988))
- **devti:** migrate test data generation action to use AI ([c5fa199](https://github.com/unit-mesh/auto-dev/commit/c5fa19905ec0ce8f8711a47f4f47b4352066bf2c))
- **exts/devin-lang:** Improve parsing and lexing of DevInLang files [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([26f823f](https://github.com/unit-mesh/auto-dev/commit/26f823fc21e991e8f7d072c764cab4fdddc3175f))
- **exts/devin-lang:** improve resource management in RunProfileState ([861b5d8](https://github.com/unit-mesh/auto-dev/commit/861b5d8b1d09819f4c42f1c2d34667d98e2ba89e))
- fix import issue ([9776b57](https://github.com/unit-mesh/auto-dev/commit/9776b576db405c056f0d1e5fb2c9599b7697a049))
- **folding:** correct handling of file references in DevInFileReferenceFoldingBuilder.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ca240d1](https://github.com/unit-mesh/auto-dev/commit/ca240d19d39f3ee5219fabca99d7d5ffd2a39955))
- **folding:** improve file reference folding in DevInFileReferenceFoldingBuilder [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([2a7a330](https://github.com/unit-mesh/auto-dev/commit/2a7a3303a24a0539e32e5585fdef56e999e9d369))
- **git:** fix 222 & 233 version GitUtil class for committing local changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([71c3d69](https://github.com/unit-mesh/auto-dev/commit/71c3d697d23cbbfa9b6488e334169d0e90130856)), closes [#233](https://github.com/unit-mesh/auto-dev/issues/233)
- **language:** update external ID in DevInFile.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7521246](https://github.com/unit-mesh/auto-dev/commit/75212464952be0d9bfec2893f1301732ec2f49d3))
- **runconfig:** update AutoDevConfigurationType to use AutoCRUDConfigurationOptions [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cd98122](https://github.com/unit-mesh/auto-dev/commit/cd981229dcfc8f3354fe2ae343f872807889397c))
- **run:** rename DevInRunFileAction to DevInsRunFileAction [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([85e75fe](https://github.com/unit-mesh/auto-dev/commit/85e75fe5bb8f10a5f2db51040cb54f0b12249aeb))
- **text-block-view:** update text listener registration and text parsing for assistant messages ([011f7ab](https://github.com/unit-mesh/auto-dev/commit/011f7ab8a1cadb4a09dc41fb0ea974ed5bb7f1f4))

### Features

- **chat:** add custom agent response provider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e599196](https://github.com/unit-mesh/auto-dev/commit/e59919618549eeabffb68f4d8ee2adb76c28b2d5))
- **compiler:** add support for committing changes ([14e9439](https://github.com/unit-mesh/auto-dev/commit/14e943935ad54c15ff73eae478c7cc6e7fe35a63))
- **compiler:** add support for rev auto command [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1ea0bb8](https://github.com/unit-mesh/auto-dev/commit/1ea0bb8bc29839941aa8c47a19af1fa552f98839))
- **completion:** add icons to builtin commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6f0e1e6](https://github.com/unit-mesh/auto-dev/commit/6f0e1e6d96b70bf3a8293e50f5556c26d99bc833))
- **completion:** add support for automatic colon insertion and caret positioning after builtin commands completion. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([5920d9b](https://github.com/unit-mesh/auto-dev/commit/5920d9b850c7c8682341681f67e0a11953a8d9a6))
- **completion:** add support for built-in agent completion [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a09cd0f](https://github.com/unit-mesh/auto-dev/commit/a09cd0f731d68d817f732e94b1d0b18bdf508291))
- **completion:** improve built-in agent support for file and revision references [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([eb60574](https://github.com/unit-mesh/auto-dev/commit/eb605740dfaf089a092e327f1d88fd892f846945))
- **completion:** improve completion provider for DevInTypedHandler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a696be3](https://github.com/unit-mesh/auto-dev/commit/a696be3a3a99330b310957ebdde4ad26cbc1715b))
- **completion:** improve file reference completion by using editor history and project directory. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([78d95fb](https://github.com/unit-mesh/auto-dev/commit/78d95fb98b6c51a8a2db4379b3823af03d7c91a1))
- **completion:** improve file reference completion support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1d2e422](https://github.com/unit-mesh/auto-dev/commit/1d2e4229742653039d0895f74bcff3c084a33e95))
- **completion:** improve file reference provider with project file index [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([32bf931](https://github.com/unit-mesh/auto-dev/commit/32bf931166d1747741a804409ddf9975368e57b6))
- **completion:** refactor completion provider and add support for revision references [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c7eee9a](https://github.com/unit-mesh/auto-dev/commit/c7eee9a059002755c0d9eea2a0baf853ecb641f4))
- **completion:** rename and modify CodeLanguageProvider to support code fence languages ([0ca5616](https://github.com/unit-mesh/auto-dev/commit/0ca56168795f87d8ad8c20d9c8f835b8bfdc0a39))
- **devin-compiler:** add support for builtin commands and agents in DevInCompiler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([81766ff](https://github.com/unit-mesh/auto-dev/commit/81766ff30660467e70a4cad7dd08eb122d128d8a))
- **devin-lang:** add AutoDevRunConfigurationProducer and related classes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6e9efac](https://github.com/unit-mesh/auto-dev/commit/6e9efac7922e44278f370a717daaf8c43c8a4283))
- **devin-lang:** add console output support to DevInRunConfigurationProfileState [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([849582f](https://github.com/unit-mesh/auto-dev/commit/849582f56cef21189784a4b1b3f5fb1ff9acdf64))
- **devin-lang:** add DevInCompilerTest and DevInCompiler classes to support DevInFile compilation and testing. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ed9378b](https://github.com/unit-mesh/auto-dev/commit/ed9378b21086830c6b23135b7f48b813e5730edc))
- **devin-lang:** add DevInRunFileAction and related changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([035a3cb](https://github.com/unit-mesh/auto-dev/commit/035a3cbb893d137e29c5b8fbdcb864b0271a05f2))
- **devin-lang:** add FileAutoCommand and refactor DevInCompiler to support dynamic file content retrieval. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e74019a](https://github.com/unit-mesh/auto-dev/commit/e74019a8aeeb67222c97870330e408bbac5e6b58))
- **devin-lang:** add fullWidth utility function to AutoDevSettingsEditor.kt and remove unused imports from DevInProgramRunner.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([29480d5](https://github.com/unit-mesh/auto-dev/commit/29480d59726a8fceda07484912526d788f12a276))
- **devin-lang:** add highlighting for agent and command identifiers [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([039d053](https://github.com/unit-mesh/auto-dev/commit/039d0535c4a4fefb177e8a4e015dbdf4c66d04df))
- **devin-lang:** add highlighting for property values [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1c58f51](https://github.com/unit-mesh/auto-dev/commit/1c58f513b353ad71c3531ab64da0f70eee4223c2))
- **devin-lang:** add support for agent properties [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([632372d](https://github.com/unit-mesh/auto-dev/commit/632372da1ba126989ee21d762e2c240dd23444da))
- **devin-lang:** add support for DevIn Language in kover and update documentation ([26f1115](https://github.com/unit-mesh/auto-dev/commit/26f1115644824e4efee78a9d652ce164ecae20cf)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
- **devin-lang:** add support for DevInRunConfigurationProfileState and related changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([3a37baa](https://github.com/unit-mesh/auto-dev/commit/3a37baae83f9ba06fa71ea435d04f5784e7fcd20))
- **devin-lang:** add support for file path processing in DevInCompiler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9909f15](https://github.com/unit-mesh/auto-dev/commit/9909f15e407a559e62b59b2373712b8d22e95400))
- **devin-lang:** add support for script path configuration in run configurations [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([044c6cd](https://github.com/unit-mesh/auto-dev/commit/044c6cdfd937f3e35bb3648acc2ed2893baf5745))
- **devin-lang:** add support for writing content to a file [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6140691](https://github.com/unit-mesh/auto-dev/commit/6140691b44bfcfaf8cfb665a1d3d2328cf145ca0))
- **devin-lang:** extend language identifier regex to support spaces and dots [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9ec0123](https://github.com/unit-mesh/auto-dev/commit/9ec0123de414b1e8956b59de89634a88f2b985a5))
- **devin-lang:** improve run line markers provider ([07a549d](https://github.com/unit-mesh/auto-dev/commit/07a549df15fc9de32b45ad04507726368afe9120))
- **devin-lang:** init design for patch and run commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c6cea24](https://github.com/unit-mesh/auto-dev/commit/c6cea2432a298bcae1d73806f516f31f03f4e42a))
- **devin-lang:** refactor process handling in RunConfigurationProfileState [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([34ca50e](https://github.com/unit-mesh/auto-dev/commit/34ca50e07112042ecef70f45c65796da28262c6b))
- **devin-lang:** update notification group id and add LLM support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a2e96b6](https://github.com/unit-mesh/auto-dev/commit/a2e96b6f9931c1d2fe2f6d11340ce176d2d1343e))
- **devins-compiler:** add support for commit command [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a498580](https://github.com/unit-mesh/auto-dev/commit/a4985808b7437b682eee28d72f60d78802ec68c1))
- **devins-compiler:** add support for writing and auto commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d7232d8](https://github.com/unit-mesh/auto-dev/commit/d7232d863f3291d86896cb12def57bfdb71e6447))
- **devins-lang:** add logging to handle commit loading errors [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ec7e984](https://github.com/unit-mesh/auto-dev/commit/ec7e9845155c2bb5c79d056f63a51951ed016356))
- **devins-lang:** add support for WRITE command [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a0c228b](https://github.com/unit-mesh/auto-dev/commit/a0c228b166a38334e06237e4ea364fd9e14ad392))
- **devins-lang:** introduce DevIns Lang as the AI Agent language [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e109520](https://github.com/unit-mesh/auto-dev/commit/e10952043652e21a1c087a7ebdfc689d9bfee580))
- **devins-language:** add LineInfo data class and fromString method [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([2798643](https://github.com/unit-mesh/auto-dev/commit/27986430cb0ac24f931377262045cdc9df6f081b)), closes [filepath#L1-L12](https://github.com/filepath/issues/L1-L12)
- **docs:** rename DevIn Input Language to DevIn Agent Language [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([4b504a8](https://github.com/unit-mesh/auto-dev/commit/4b504a86eecfaedf3d1542d637c95bd59cff3d13))
- **exec:** add CommitInsCommand for executing commits [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1870425](https://github.com/unit-mesh/auto-dev/commit/187042531635facab9f27294bcf8f8c70d6a8431))
- **folding:** add file reference folding support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9c1aae4](https://github.com/unit-mesh/auto-dev/commit/9c1aae4430145e7e97c33ab5c29a7cecc7e816bc))
- **git-completion:** add asynchronous loading of git commits [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([b237db7](https://github.com/unit-mesh/auto-dev/commit/b237db70995e4404e87786da720c6ff98d9c75f3))
- **gui:** add support for custom agent file schema validation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d52cf76](https://github.com/unit-mesh/auto-dev/commit/d52cf7604a29d5a142f21dc79ffda6342b4e337e))
- **language:** add support for flow in DevInRunConfigurationProfileState [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([afc0897](https://github.com/unit-mesh/auto-dev/commit/afc08970979d2b84e94389fc230f97f90d07a604))
- **lexer:** add support for agent value block ([a3b37a1](https://github.com/unit-mesh/auto-dev/commit/a3b37a1b249973fb755048a7f175716d3e576574)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
- **provider:** enable DevIn agent responses [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1efc73c](https://github.com/unit-mesh/auto-dev/commit/1efc73c34f10ce9c65f70c17934726e456dfabc7))
- **run:** add support for DevInRunFileAction and related changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ac187cf](https://github.com/unit-mesh/auto-dev/commit/ac187cff8bdd37d76ca5f9deef9cebf63cda33d7))
- **runconfig:** add AutoDevCommandRunner and related configuration types [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1834fed](https://github.com/unit-mesh/auto-dev/commit/1834fedb87e26344c22a3e71c8ef5856d3fbefbf))
- **runconfig:** refactor AutoDevConfigurationFactory and AutoDevConfiguration classes to use inheritance and override methods for better code organization and maintainability. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([84e5033](https://github.com/unit-mesh/auto-dev/commit/84e5033bf06a8267a75781b59f5f27ccccfbe913))
- **schema:** add support for custom prompts schema ([d76bc07](https://github.com/unit-mesh/auto-dev/commit/d76bc07e50b949df6010faa22aacd5b62f7a6e55))
- **testing:** add support for running individual test files [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([68063b1](https://github.com/unit-mesh/auto-dev/commit/68063b106a188f92bde14519cafa9477d1fe7b6f))
- **utils:** add isRepository function to MvcUtil.kt ([9e6343c](https://github.com/unit-mesh/auto-dev/commit/9e6343ceba358289cffd8da0d267cf2ef9a24a11))

## [1.7.1](https://github.com/unit-mesh/auto-dev/compare/v1.7.0...v[1.7.1]) (2024-03-13)

### Bug Fixes

- **chat-coding-panel:** improve handling of request intentions [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([9fa1808](https://github.com/unit-mesh/auto-dev/commit/9fa1808256e33d8355494fafa738294a912397b3))
- **chat-coding-service:** handle custom RAG requests [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([46b499e](https://github.com/unit-mesh/auto-dev/commit/46b499eb5dcbfa49dc4fd3261098da66ce8240f1))
- **chat:** handle custom agent state and add support for custom variable compilation [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([93a5e52](https://github.com/unit-mesh/auto-dev/commit/93a5e527df18b03e754ceb3b9b246c3671c2db52))
- **chat:** handle empty ragsJsonConfig in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([df410b1](https://github.com/unit-mesh/auto-dev/commit/df410b1acacbf45db9e7f76d70e68eca48b6bb7b))
- **chat:** hide progress bar after update [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([56a88fc](https://github.com/unit-mesh/auto-dev/commit/56a88fcf3b413b3610097dd635ef9d15fb384dbd))
- **completion:** improve chatbot response handling with JSON parsing enhancements ([561e36a](https://github.com/unit-mesh/auto-dev/commit/561e36ab4dbae1b9dba1a1890cb2da0ad673ef26))
- **CoUnitPromptGenerator:** ensure retrofit service creation is consistent [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([2e0de74](https://github.com/unit-mesh/auto-dev/commit/2e0de7406464f0bcb37bf3dd26bbd967a151a23b))
- **custom-agent:** ensure null safety in agent state resetting [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b6587f2](https://github.com/unit-mesh/auto-dev/commit/b6587f2324b355a91d986fd138f11c837c735060))
- **diff-simplifier:** improve handling of real-world diffs [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4393411](https://github.com/unit-mesh/auto-dev/commit/43934115d772e7a43e738be7c32ef1e5524a57a9))
- **diff:** handle VcsException and log diff before error [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([e94b191](https://github.com/unit-mesh/auto-dev/commit/e94b1915ab8514fbb986065f8cea5be81e6a5149))
- **diff:** handle VcsException and log diff before error [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([6c0eb94](https://github.com/unit-mesh/auto-dev/commit/6c0eb94e964476b45c5c811ff2078d1e75e77979))
- fix import ([4a0a7a4](https://github.com/unit-mesh/auto-dev/commit/4a0a7a4451a66f471d1bb462a39ef843f9109979))
- fix import issue agaian... ([eaae8b9](https://github.com/unit-mesh/auto-dev/commit/eaae8b91158a0737ce81f86ad0c135f4f978f3a0))
- fix typos ([c839c89](https://github.com/unit-mesh/auto-dev/commit/c839c8981c507b3f13bed84b58e9df74fe75c2bf))
- **gui:** ensure correct selection in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1f74f9f](https://github.com/unit-mesh/auto-dev/commit/1f74f9fff2348ff04b770a81da81f4b664b2cd84))
- **gui:** Improve chat input handling [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([f157522](https://github.com/unit-mesh/auto-dev/commit/f15752228244f23290d8fc6e2c90ad36a6f5239e))
- **gui:** improve code block rendering and parsing [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([19a0e97](https://github.com/unit-mesh/auto-dev/commit/19a0e97a1aba87af61bbae27f8ff478440a242b6))
- **gui:** only trigger popup on '$' input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4535dc8](https://github.com/unit-mesh/auto-dev/commit/4535dc81807bc14ca29ea8d745b8bd48fcf0cf9d))
- **gui:** prevent progress bar from resetting after user input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4dbf66d](https://github.com/unit-mesh/auto-dev/commit/4dbf66dc1e3897da8db48d7cf41d28e338fe7af8))
- **gui:** refactor event dispatcher initialization [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b0bb226](https://github.com/unit-mesh/auto-dev/commit/b0bb226327dddb436e600f4a2221d47ac778bd5d))
- **gui:** remove Dev Portal and Doc options from customRags combobox, set layoutPanel to non-opaque to improve visibility [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0f0d5b9](https://github.com/unit-mesh/auto-dev/commit/0f0d5b95e47a6df83e70e2ed21f507b9e544e49c))
- **gui:** simplify chat coding panel layout [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0b513e4](https://github.com/unit-mesh/auto-dev/commit/0b513e4cf73ad56e8c7b7c5bfbda171bbe9d3b09))
- **jcef:** use official JCEF builder and handle exceptions [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([d116151](https://github.com/unit-mesh/auto-dev/commit/d1161516584801f0b143a30c3ad214ef5d16ceb5))
- **LivingDocPromptBuilder:** handle exceptions when getting context for PsiElement ([d3367fb](https://github.com/unit-mesh/auto-dev/commit/d3367fb4a40bb8f81a6427296833848ef0cc947e))
- **response-handling:** handle empty SSE lines and "ping" events [#97](https://github.com/unit-mesh/auto-dev/issues/97) ([c654495](https://github.com/unit-mesh/auto-dev/commit/c654495d3398145d5fdc59d373e3b8ce7c58df56))
- **sse-starlette:** handle SSE events with data prefixed with ":ping" and fixed[#97](https://github.com/unit-mesh/auto-dev/issues/97) ([e448c28](https://github.com/unit-mesh/auto-dev/commit/e448c283c2bad2c8310cb25786fcf73af98c0136))
- **sse:** handle empty lines and comments in SSE event stream [#97](https://github.com/unit-mesh/auto-dev/issues/97) ([d307861](https://github.com/unit-mesh/auto-dev/commit/d307861c149774a69b0df29264a09e9e7c6c8f51))
- **tasks:** add onFinished() methods to notify application status ([632be81](https://github.com/unit-mesh/auto-dev/commit/632be815fd6e5467851e4afb22a9f3a60452b225))

### Features

- **chat:** add removeLastMessage function to clear chat history [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([247a8ad](https://github.com/unit-mesh/auto-dev/commit/247a8ad2323df3f26017a9c7376787cd2ed45ae2))
- **counit:** rename and refactor to support custom agent functionality [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0375d22](https://github.com/unit-mesh/auto-dev/commit/0375d229de476119e8e5a7242e1ac503f1c72c59))
- **custom_agent:** add state management for custom agent flow [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1a59765](https://github.com/unit-mesh/auto-dev/commit/1a597658c29271d1ed89db0166c2b952dd59c6a2))
- **custom-actions:** improve logging and error handling in CustomActionBaseIntention and CustomAgentChatProcessor. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b900633](https://github.com/unit-mesh/auto-dev/commit/b900633b60672e55d0c4315243e66365c3fb496f))
- **custom-action:** use i18n for family name ([f9695b7](https://github.com/unit-mesh/auto-dev/commit/f9695b7a4836c26776bf691aec5e9f3874ca5830))
- **custom-agent:** add basic support for custom agent execution [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([62e30a1](https://github.com/unit-mesh/auto-dev/commit/62e30a1bf457ee5b5ec89831502cc6854b8f2007))
- **custom-agent:** add custom agent support [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([e089d02](https://github.com/unit-mesh/auto-dev/commit/e089d02f90640cb67861daa0b07aa39b2cbd6f6e))
- **custom-agent:** add support for authentication types in custom agent execution [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([59f54c9](https://github.com/unit-mesh/auto-dev/commit/59f54c9e0402f6874ace09dff460a3d59c8712e5))
- **custom-agent:** add support for custom request format and improve serialization #%1 ([4bd6840](https://github.com/unit-mesh/auto-dev/commit/4bd68404aaf79a4b2dca0e79689a90c783b3a169))
- **custom-agent:** add support for custom web views in chat responses [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([6cd1e2c](https://github.com/unit-mesh/auto-dev/commit/6cd1e2c65f46e5bb20546eccd2e03f1d1aad85eb))
- **custom-agent:** add support for custom webview in chat interface [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([dcae35c](https://github.com/unit-mesh/auto-dev/commit/dcae35ca798e8ac85d750d57f6730b40f9a88250))
- **custom-agent:** add support for OpenAI ChatGPT integration [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ddaa4e2](https://github.com/unit-mesh/auto-dev/commit/ddaa4e271e97c96875d318eba30bdc8a7a5f65b7))
- **custom-agent:** add WebBlockView class and import Component for WebViewWindow class [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b4fd22b](https://github.com/unit-mesh/auto-dev/commit/b4fd22bd35124e463985ca5221ea7f9751905cba))
- **custom-agent:** refactor and add stream response action [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ef80c30](https://github.com/unit-mesh/auto-dev/commit/ef80c30c65a5bbbc957ebb7ec563b4e1fb740f10))
- **custom-agent:** refactor and add support for custom agent response actions [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0bfac9e](https://github.com/unit-mesh/auto-dev/commit/0bfac9e633e3052d7b335ff34023602d3690d627))
- **custom-agent:** refactor to use LlmProvider for chat processing [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([88b4633](https://github.com/unit-mesh/auto-dev/commit/88b4633d3293c50c42834e9f56e112dd96249faa))
- **custom-agent:** use agent-specific url for requests [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([65e9e5e](https://github.com/unit-mesh/auto-dev/commit/65e9e5e2d05eac41c7668fc9cf63995dfc73097d))
- **custom-arg:** add support for custom RAG settings and refactor related components and configurations [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([bca1bba](https://github.com/unit-mesh/auto-dev/commit/bca1bba0fb836e517472b82f1f3f2e9276f56e1f))
- **custom-variable:** improve variable list component and add custom variable support [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([bf2440d](https://github.com/unit-mesh/auto-dev/commit/bf2440dc4922044645241e4546aa6fd487eaf149))
- **custom:** add support for custom agent configuration and UI improvements [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([05ecb7a](https://github.com/unit-mesh/auto-dev/commit/05ecb7a97c3a2a1540e7f870f0a6bcb7e2ddee9d))
- **gui:** add AutoDevVariableListComponent and improve popup behavior in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([8b54522](https://github.com/unit-mesh/auto-dev/commit/8b54522cfb60db24aadee0527814c081cdc79663))
- **gui:** Add default rag selection and refactor custom rag loading logic [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([cf7bb77](https://github.com/unit-mesh/auto-dev/commit/cf7bb77dfc6da653548b7137a88deeb9f275b771))
- **gui:** add key listener to AutoDevInputSection for better user experience [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ae6665e](https://github.com/unit-mesh/auto-dev/commit/ae6665e22d15fa45382ac5288cb1def8f3e49401))
- **gui:** add resetAgent() method to clear custom agent selection in chat coding panel and input section. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([768e4c0](https://github.com/unit-mesh/auto-dev/commit/768e4c0aab4dabf2ec22720161f4f517444df12f))
- **gui:** add resetAgent() method to clear custom agent selection in chat coding panel and input section. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([16240e0](https://github.com/unit-mesh/auto-dev/commit/16240e008fab623c2ccc16acef404af997e35ee2))
- **gui:** add support for auto-completion popup in chat input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([741dda8](https://github.com/unit-mesh/auto-dev/commit/741dda8e7d2b6163e77eb3e3a9ada1bdf56b7156))
- **gui:** add support for custom rag apps selection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([27d1269](https://github.com/unit-mesh/auto-dev/commit/27d12698c5dd1d0bacd9d596477c37aed43ebc71))
- **gui:** add support for custom variables in chat input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ccf3967](https://github.com/unit-mesh/auto-dev/commit/ccf39677a00778580b1c0c05d999c67c6e23f0c6))
- **gui:** refactor AutoDevVariableListComponent to use JBList and add support for variable selection popup [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([e5993a4](https://github.com/unit-mesh/auto-dev/commit/e5993a40bc41a57019610446c1807d055f5e9cd8))
- **model:** introduce CustomRagApp and ResponseAction enum, refactor CodePayload to use text instead of payload_type, update AutoDevInputSection to use send icon, add examples to Tooling class as QAExample objects. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([6a7c26a](https://github.com/unit-mesh/auto-dev/commit/6a7c26a939516949d1be9406d77ac9a9eeb07913))
- **server:** add support for HTMLResponse in mock_frontend [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1cfa154](https://github.com/unit-mesh/auto-dev/commit/1cfa154ffd04a1734404be1004d59a6cc9562bdc))
- **view:** add WebViewWindow class to handle browser events and implement JavaScript communication. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([00d9169](https://github.com/unit-mesh/auto-dev/commit/00d9169f9de1be098aacb5b7d1a3b716a53a8092))
- **view:** improve web view window background color to JBColor.WHITE [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([467ebbf](https://github.com/unit-mesh/auto-dev/commit/467ebbf7d82c5ffd6cf7516bb7288444a4c13a11))
- **webview:** add support for custom scheme handler and load methods [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([d49734a](https://github.com/unit-mesh/auto-dev/commit/d49734aaa5954394be706e386cafb8dc68a48d7f))

### BREAKING CHANGES

- rename `message` -> `content`

### Reverts

- Revert "refactor(custom-agent): improve response action handling and add removeLastMessage function #51" ([214eb79](https://github.com/unit-mesh/auto-dev/commit/214eb7995c64d3c7c3a44812a10972aff2939599)), closes [#51](https://github.com/unit-mesh/auto-dev/issues/51)

## [1.6.5](https://github.com/unit-mesh/auto-dev/compare/v1.6.4...v[1.6.5]) (2024-03-03)

### Bug Fixes

- **241:** update action threads for UI actions ([a97f102](https://github.com/unit-mesh/auto-dev/commit/a97f10220275a89ddedb4a65eb7ff688c758ad44))
- **docs:** correct custom request format documentation ([c8ad270](https://github.com/unit-mesh/auto-dev/commit/c8ad270c3ec77a335befa72d825617dc97774366))
- **intentions:** update description for AutoDevIntention and AutoSqlAction ([1a28168](https://github.com/unit-mesh/auto-dev/commit/1a2816879152aeb3c215f5687145699bfc2d4c2d))
- **prepush:** update background thread for action update since of @Deprecated API changed ([4b2e390](https://github.com/unit-mesh/auto-dev/commit/4b2e39024b73c870fb162947f266a8917985ac56))
- **provider:** remove unused logger in LivingDocumentation and HarmonyOsLivingDocumentation ([025cb95](https://github.com/unit-mesh/auto-dev/commit/025cb953db258b32043fe8e402ff344f2cd3997e))
- **SSE response handling:** Skip logging of SSE responses ([8e6affb](https://github.com/unit-mesh/auto-dev/commit/8e6affb963ba25a3da6849cda74f8358016ca57b))

### Features

- **custom-llm:** Add response format validation and success request tracking ([ffb07be](https://github.com/unit-mesh/auto-dev/commit/ffb07bef5d4a48426478e4a3e3ee7752462b75bc))
- **docs:** enhance Moonshot AI custom integration in docs ([fde46b8](https://github.com/unit-mesh/auto-dev/commit/fde46b856374d790f93df568a04bdeec782b5ac7))
- **docs:** update AI configurations and usage guide ([c42eb6d](https://github.com/unit-mesh/auto-dev/commit/c42eb6d9ee028ef8f2b61d4158952f70386ae655))
- **gui:** add auto dev insert to code action ([f56d7de](https://github.com/unit-mesh/auto-dev/commit/f56d7deb50c95119c21ef10557f93bea7d086e07))
- **harmonyos:** improve LinearLayout and RelativeContainer layouts ([9f9b228](https://github.com/unit-mesh/auto-dev/commit/9f9b2287a89480974d19737c4477d84e41e5803b))
- **harmonyos:** modify List component to include new features ([4e40af6](https://github.com/unit-mesh/auto-dev/commit/4e40af648b24576c5ac7514971ef67cd32223104))
- **icon:** add support for auto dev insert file action ([66c2e96](https://github.com/unit-mesh/auto-dev/commit/66c2e964b7b9a948c0f73cc281c1854237419ae1))
- Improve code insertion in AutoDevInsertCodeAction ([a1a63bb](https://github.com/unit-mesh/auto-dev/commit/a1a63bbd7a4f901e663bfd2233570abfa203826d))
- init check for openai hosts ([efeb143](https://github.com/unit-mesh/auto-dev/commit/efeb1432208de1c1802f2a73c5a0365e521b0724))

## [1.6.4](https://github.com/unit-mesh/auto-dev/compare/v1.6.3...v[1.6.4]) (2024-02-25)

### Bug Fixes

- **harmonyos:** convert ArkUiExample to data class ([0c71a69](https://github.com/unit-mesh/auto-dev/commit/0c71a6948c72cc2fc5585d4b38dd05916651ff48))
- **harmonyos:** update ext-harmonyos README ([37f323d](https://github.com/unit-mesh/auto-dev/commit/37f323d92ca4004ee90cf76fd6053698e730bd3c))

### Features

- **actions:** add new component types ([232dcac](https://github.com/unit-mesh/auto-dev/commit/232dcacbeedbfb66f193604eaea39cf055244bec))
- **arkui:** add support for ArkUi special features, components, and layouts ([998f5dc](https://github.com/unit-mesh/auto-dev/commit/998f5dc25afdc5f8ddae911d37f62e86cfb5ac51))
- **ext-harmonyos:** add android plugin dependency and component class ([62042f1](https://github.com/unit-mesh/auto-dev/commit/62042f13ed51437ac0af420bfb2877ed4580f4a5))
- **extension:** add HarmonyOS extension and API reference ([09494cf](https://github.com/unit-mesh/auto-dev/commit/09494cf7980c6ae466eba7cc36e08f5acf351cd3))
- **harmonyos:** add AutoArkUiFlow class and ArkUiContext data class ([e33509a](https://github.com/unit-mesh/auto-dev/commit/e33509a104b776164feb2636911f02c53da3ee9e))
- **harmonyos:** add JS and CPP PSI modules ([da4852e](https://github.com/unit-mesh/auto-dev/commit/da4852e7ef80b657f8bf1712b487d80909e05eea))
- **harmonyos:** add margin to Button in ArkUiComponentType ([e6f4734](https://github.com/unit-mesh/auto-dev/commit/e6f4734a4889138656dedd3eb37e38fd9a974197))
- **harmonyos:** add support for Android Studio platform ([f611959](https://github.com/unit-mesh/auto-dev/commit/f6119596f0e441803ae507d654e5ed12084427ee))
- **harmonyos:** add support for ArkUi migration ([bf6579e](https://github.com/unit-mesh/auto-dev/commit/bf6579e65b9e9e8f6fd2ae4270cf35221b1af0d2))
- **harmonyos:** add support for ArkUI migration expert ([e670925](https://github.com/unit-mesh/auto-dev/commit/e670925a4d023c53845ae6a75760e4f46456f9a3))
- **harmonyos:** add support for parsing select text ([d5c89cc](https://github.com/unit-mesh/auto-dev/commit/d5c89cca901bd327d2bcb189c88d9414844fb36d))
- **harmonyos:** add support for sending UI to chat panel ([e175992](https://github.com/unit-mesh/auto-dev/commit/e175992779ce93974c223568c9daa0b30f340639))
- **harmonyos:** add supported layout types ([09f6c06](https://github.com/unit-mesh/auto-dev/commit/09f6c06a98ec80e1d50fc65857b3051b9b0f8731))
- **harmonyos:** improve ArkUiLayoutType and ArkUi documentation ([9147ce1](https://github.com/unit-mesh/auto-dev/commit/9147ce1aaefe9269d857a0c1fae2ef7efe621f8d))
- **harmonyos:** improve HarmonyOSChatContextProvider ([2f754ef](https://github.com/unit-mesh/auto-dev/commit/2f754efacf9c1ae674200644886221d55177f426))
- **harmonyos:** update ArkUiFlow design method and add ArkUiLayoutType and ArkUiComponentType ([4ad516d](https://github.com/unit-mesh/auto-dev/commit/4ad516d1fe7e05bd87886e6cab4be8387b01fabb))
- **harmonyos:** update AutoArkUiFlow and related classes ([a6ef010](https://github.com/unit-mesh/auto-dev/commit/a6ef010c95460a10e05882890a0bccb27e8cfd59))
- **harmonyos:** update documentation and chat context provider ([91317d1](https://github.com/unit-mesh/auto-dev/commit/91317d1a47a136d1647cd08e4f065d16098ed032))
- **javascript:** add TypeScript living documentation ([8a0ad02](https://github.com/unit-mesh/auto-dev/commit/8a0ad022d25fc73eb8ad87344be23d26378b919b))
- **living-doc:** add HarmonyOS living documentation support ([d4612d6](https://github.com/unit-mesh/auto-dev/commit/d4612d67445cfbbd17d3419e2d4e5c2615f5463b))
- **plugin:** add support for HarmonyOS ([56f7a99](https://github.com/unit-mesh/auto-dev/commit/56f7a9943ace6ac3ee0cd7145aee87b7d61ec99c))
- **provider:** add HarmonyOS chat context provider ([1eeeb23](https://github.com/unit-mesh/auto-dev/commit/1eeeb235f144cc2b52be4fd79865808a708f68bd))
- **provider:** add logging to HarmonyOsLivingDocumentation ([ebc575f](https://github.com/unit-mesh/auto-dev/commit/ebc575f76b0f8c8c189ea0848979ce1371cd14d4))

## [1.6.3](https://github.com/unit-mesh/auto-dev/compare/v1.6.1...v[1.6.3]) (2024-02-22)

### Bug Fixes

- **android:** remove space before Android SDK target version ([5f32088](https://github.com/unit-mesh/auto-dev/commit/5f320884626a3fd3b6384a39f9501c29913ae6a8))
- **go:** improve documentation generation process ([90f446f](https://github.com/unit-mesh/auto-dev/commit/90f446ff5fe7d3796808e5d89d60977c34284437))
- **goland:** fix 222 version lost interface issue ([e6def78](https://github.com/unit-mesh/auto-dev/commit/e6def7842c08f02dba2e6b0469987cd0cf845251))

### Features

- **build:** add Gradle IntelliJ Plugin and update version ([45c714a](https://github.com/unit-mesh/auto-dev/commit/45c714acb47e4aa5f180b1e514946744442f4119))
- **chat:** refactor chat action names ([fb307d9](https://github.com/unit-mesh/auto-dev/commit/fb307d912e30debeadee2776151d098c7fc65da5))
- **completion:** add support for text replacement in code completion ([f552e0d](https://github.com/unit-mesh/auto-dev/commit/f552e0d7e14a31e4e83134f60c35dedad8d72ff7))
- **docs:** add compatible strategy documentation ([23581b6](https://github.com/unit-mesh/auto-dev/commit/23581b657b2cc32a3dbd303f6f99431027b6fdfe))
- **docs:** add demo project link to customization guide ([cea4574](https://github.com/unit-mesh/auto-dev/commit/cea4574874efe5ab17f00d734279e7d35cde6257))
- **docs:** add unit-driven design pattern documentation ([3b09c90](https://github.com/unit-mesh/auto-dev/commit/3b09c90601576d0cf2bff2426c24c1798b1bd14f))
- **interaction:** add support for ReplaceSelection prompt type ([e1da93e](https://github.com/unit-mesh/auto-dev/commit/e1da93eb2a91cd5c858def1d72d9ac50ab451aa2))
- **living-docs:** add parameter and return tag instructions ([603a4c2](https://github.com/unit-mesh/auto-dev/commit/603a4c2dd687338f947ffecf19d231f2f9ac0474))
- **provider:** add Android extension support ([2860c85](https://github.com/unit-mesh/auto-dev/commit/2860c85f1451d88d4cbfe56189b0c8693b5f33f9))
- **tests:** add @Ignore annotation to end-point prompt test ([c99ed29](https://github.com/unit-mesh/auto-dev/commit/c99ed29f8080095b248ea7bb65b867cc96c84fc6))

## [1.6.1](https://github.com/unit-mesh/auto-dev/compare/v1.6.0...v[1.6.1]) (2024-02-18)

### Bug Fixes

- **actions:** remove unnecessary null check ([51dc43e](https://github.com/unit-mesh/auto-dev/commit/51dc43e27563b31dc7eba607783bff45b8842d32))
- **docs:** fix workflow steps in auto-page.md ([883528c](https://github.com/unit-mesh/auto-dev/commit/883528c01cb69856f4d43949120d3af56c2483df))
- fix typos ([927939f](https://github.com/unit-mesh/auto-dev/commit/927939f446f99a5acce8fa887f82880aebc5f708))
- **genius:** update SQL generation templates ([20735ee](https://github.com/unit-mesh/auto-dev/commit/20735eef361ee7b7a5e23b4b5f2917a274e1086d))
- **javascript:** add logger statements for null values [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([57527d5](https://github.com/unit-mesh/auto-dev/commit/57527d51ec70754a89290157a036c9bda8326bbe))
- **team:** fix nullability issue in TeamPromptsBuilder ([f36e74d](https://github.com/unit-mesh/auto-dev/commit/f36e74deab9f05efa751959e1103da80c198bd57))
- **ui:** add lost support for idea 222 vertical alignment in grid layout ([dfa73cc](https://github.com/unit-mesh/auto-dev/commit/dfa73cc8073cce0f44e36f94c5a9ad26617be37b))

### Features

- **autodev-pair:** init AutoDev Pair documentation and tool window ([55a103e](https://github.com/unit-mesh/auto-dev/commit/55a103eb6c6af7d462a9c6a8c974e8c9f3e10c51))
- **commit-msg:** update commit message guidelines ([9e1cc3b](https://github.com/unit-mesh/auto-dev/commit/9e1cc3b3e1b31517cea1d04243148e38475529b1))
- **database:** add check for empty table names [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([ec3bf1d](https://github.com/unit-mesh/auto-dev/commit/ec3bf1d5929db58e511f390d5a40fa2a0002d196))
- **database:** add code parsing for SQL script [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([d15ce43](https://github.com/unit-mesh/auto-dev/commit/d15ce43d0571f788715226be4892d1cb32ad496e))
- **database:** add DbContext and DbContextProvider classes [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([6017221](https://github.com/unit-mesh/auto-dev/commit/601722160b86b485cc3bedb4330743241bb925f2))
- **database:** add DbContextActionProvider class [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([737a47a](https://github.com/unit-mesh/auto-dev/commit/737a47a94096f6429db2b26849a8e6f7751e4959))
- **database:** add GenerateEntityAction for PL/SQL to Java for design new workflow ([9f030fa](https://github.com/unit-mesh/auto-dev/commit/9f030fa60c2db6b7707cca258089dd89465d4fb7))
- **database:** add GenerateFunctionAction for testing PL/SQL ([74952dd](https://github.com/unit-mesh/auto-dev/commit/74952ddd26f9bf2b96b0e8d440fd32c09c55a94d))
- **database:** add GenerateUnittestAction ([7329ac6](https://github.com/unit-mesh/auto-dev/commit/7329ac65fc7e1b75bc5956822f7ce46989a9a49a))
- **database:** add GenSqlScriptBySelection action ([f597094](https://github.com/unit-mesh/auto-dev/commit/f5970945e9abe2b991bde1417c3db0c587b42fb5))
- **database:** add PL/SQL migration actions [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([4d849e3](https://github.com/unit-mesh/auto-dev/commit/4d849e37fefa350d5787aa9c5dac8fe161d95684))
- **database:** add prompter for generating SQL script [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([26227ba](https://github.com/unit-mesh/auto-dev/commit/26227babd5cd3e2c9419a06a6d1f21f6146cf68e))
- **database:** add SQL generation functionality [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([0d2e6ce](https://github.com/unit-mesh/auto-dev/commit/0d2e6ce9a987038e267c14859bf29166f3bf51c0))
- **database:** add SQL generation templates [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([62d3e13](https://github.com/unit-mesh/auto-dev/commit/62d3e13fe4769d110c7ac1dcea97c1bdf6c667d4))
- **database:** improve getTableColumns method [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([db58264](https://github.com/unit-mesh/auto-dev/commit/db58264073e24efd0a6515901bfd3fc96daf12c2))
- **database:** init ModularDesignAction and modify VisualSqlAction for design [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([2a11e29](https://github.com/unit-mesh/auto-dev/commit/2a11e2931692df1c8198b74ffd239a8fe2ab5973))
- **docs:** add usecases and workflow documentation [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([9462c15](https://github.com/unit-mesh/auto-dev/commit/9462c1525fa20f22c94cfd9a2d5e33127b556599))
- **flow:** add context parameter to execute method [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([2a88bde](https://github.com/unit-mesh/auto-dev/commit/2a88bde062bae48bb756dcc2003263ca677e168a))
- **flow:** add documentation and comments to TaskFlow interface [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([ecf891e](https://github.com/unit-mesh/auto-dev/commit/ecf891e97660786e092b4a01964f6bd9ae705b67))
- **flow:** add TaskFlow interface and implement in GenSqlFlow [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([dc8abdb](https://github.com/unit-mesh/auto-dev/commit/dc8abdb5e5987afe5416dc96d1602c402431ea10))
- **gui:** add AutoDevPairToolWindow class ([936cd3d](https://github.com/unit-mesh/auto-dev/commit/936cd3d488943587f0b93b510241739a9da81301))
- **javascript:** add AutoPageFlow and AutoPageTask [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([d687152](https://github.com/unit-mesh/auto-dev/commit/d687152063899387be7fc82e90de590b6a1db0d6))
- **javascript:** add FrontendFlow and DsComponent interfaces [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([b50b5df](https://github.com/unit-mesh/auto-dev/commit/b50b5dfeb4573581b3d63fa0e0ee4324c5017932))
- **javascript:** add function signature and props to DsComponent [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([8ab5cd4](https://github.com/unit-mesh/auto-dev/commit/8ab5cd420092ebe1e604795ac2e2f5ecc0c1ea5e))
- **javascript:** add GenComponentAction and GenComponentFlow [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([ddb96de](https://github.com/unit-mesh/auto-dev/commit/ddb96de01e1c8d1c7a114be0cdab3d66325a08eb))
- **javascript:** add language and frameworks to AutoPageContext.build [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([123da5b](https://github.com/unit-mesh/auto-dev/commit/123da5b5f0e151303415ab911f24b8ffa6d407f4))
- **javascript:** add language method to JsDependenciesSnapshot [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([bbb480b](https://github.com/unit-mesh/auto-dev/commit/bbb480b236bbb5e5831e9f3041b41b024a3b0a53))
- **javascript:** add mostPopularFrameworks method to JsDependenciesSnapshot [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([7792907](https://github.com/unit-mesh/auto-dev/commit/7792907e7f5c615c816f6abf93f79dd3854e0b1c))
- **javascript:** add new files and modify existing files [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([e3a53bf](https://github.com/unit-mesh/auto-dev/commit/e3a53bf4c42395679c08aa91838275a58eeaa3c5))
- **javascript:** add ReactFlow pages retrieval ([76bcefc](https://github.com/unit-mesh/auto-dev/commit/76bcefcaefa7873a163a9c74294ee5eba648ce5b))
- **javascript:** add ReactUtil and ReactUtilTest [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([d1323f9](https://github.com/unit-mesh/auto-dev/commit/d1323f9716db1290356688fe9b5cb7d686eeeef5))
- **javascript:** add serialization to DsComponent ([08431fb](https://github.com/unit-mesh/auto-dev/commit/08431fb4676f9c041088357b3d4a385f8bd46dcc))
- **pair:** add LayeredArch and ProjectPackageTree classes ([d71b8d5](https://github.com/unit-mesh/auto-dev/commit/d71b8d54b637ac3e3f9251df7c662f5c4ba3c518))
- **pair:** improve KotlinWriteTestService and TreeNodeTest ([120a59b](https://github.com/unit-mesh/auto-dev/commit/120a59b6b47b56539f1eec678ad632ddac5197b7))
- **tasking:** add Tasking class and test cases [#79](https://github.com/unit-mesh/auto-dev/issues/79) ([f7244e4](https://github.com/unit-mesh/auto-dev/commit/f7244e47b575f18c924429f5c3c2794807bfe814))
- **template:** add overrideTemplate method && closed [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([0f4ef52](https://github.com/unit-mesh/auto-dev/commit/0f4ef526ce1904fb9b004e3582144b55fa66cd57))

### Reverts

- Revert "refactor(project): update module names and file paths" ([092e029](https://github.com/unit-mesh/auto-dev/commit/092e0291eca860c7a0804dde5bce403da521dc79))

## [1.5.5](https://github.com/unit-mesh/auto-dev/compare/v1.5.4...v[1.5.5]) (2024-01-21)

### Bug Fixes

- **java:** add Java language check in AutoCrudAction ([6669b4b](https://github.com/unit-mesh/auto-dev/commit/6669b4ba8cfa142805760eb894e59ca6a765753b))
- **llm:** add trailing slash to customOpenAiHost && fixed [#77](https://github.com/unit-mesh/auto-dev/issues/77) ([f68d124](https://github.com/unit-mesh/auto-dev/commit/f68d12431e5ed774ec22f6acbaaa33810e72f4a8))
- **test:** add check for now writing test service [#78](https://github.com/unit-mesh/auto-dev/issues/78) ([a4b0d04](https://github.com/unit-mesh/auto-dev/commit/a4b0d04c285a1c702af0d55cc11953fb67beb8ad))

### Features

- **database:** add SQL living documentation support ([08c82bd](https://github.com/unit-mesh/auto-dev/commit/08c82bdd6865bfb824e8008a845335ddf013ebb2))
- **database:** improve finding nearest SQL definition ([6e95d47](https://github.com/unit-mesh/auto-dev/commit/6e95d47186cccadc981369172ceb9ebcf09da9ea))
- **docs:** add basic PL/SQL implementation ([478f1d9](https://github.com/unit-mesh/auto-dev/commit/478f1d9bab7f2347ac34bbc9cdfd0b627e1919e9))
- **java:** add detectLanguageLevel function ([6f7b156](https://github.com/unit-mesh/auto-dev/commit/6f7b156bd6d28ba7fef67f5f21654d948e86501b))
- **provider:** add language level detection ([8cd2584](https://github.com/unit-mesh/auto-dev/commit/8cd25842513a7fb7e999ae2df7b93d9aa01cb326))
- **rust:** add support for EnumContext ([d58b435](https://github.com/unit-mesh/auto-dev/commit/d58b435ddc6bc6e6c5cc0dedca1f0f6bb6efd341))
- **scala:** add ScalaClassContextBuilder and test case ([98ef74f](https://github.com/unit-mesh/auto-dev/commit/98ef74fc68b21399f0e6b58e90f81ffb63dd282d))
- **sql:** add functionality to update living documentation ([c99b21d](https://github.com/unit-mesh/auto-dev/commit/c99b21d09d42532fffd601e82c0d0a41dda88f61))

## [1.5.4](https://github.com/unit-mesh/auto-dev/compare/v1.5.3...v[1.5.4]) (2024-01-19)

### Bug Fixes

- **context:** fix null pointer exception in MethodContext and JSWriteTestService ([e476620](https://github.com/unit-mesh/auto-dev/commit/e4766201f5c13c5349bba9f985d3d0becea4f98b))
- **cpp:** fix 222 version issue ([fd3adf0](https://github.com/unit-mesh/auto-dev/commit/fd3adf0887cb83c9c7b2a86e7ff7067b39174f5a))
- **cpp:** fix error type issue in cpp ([2f857d9](https://github.com/unit-mesh/auto-dev/commit/2f857d9d12008125a84d66e8114f03c74911e588))
- **cpp:** fix run config issue for temp, make as todos ([a4bce6f](https://github.com/unit-mesh/auto-dev/commit/a4bce6fe1f57276e3e4ea821ee2adac5f5d5da74))
- **cpp:** update test cases for CppContextPrettifyTest, CppClassContextBuilderTest, and CppMethodContextBuilderTest ([be189fb](https://github.com/unit-mesh/auto-dev/commit/be189fb5d105b6ec3ccdfbef4fdd770bf4ba99f4))
- fix import issues ([f298b1d](https://github.com/unit-mesh/auto-dev/commit/f298b1d78007b50ee4198f7fc4370660c48fdf48))
- fix release path issues ([f18f664](https://github.com/unit-mesh/auto-dev/commit/f18f66495dd935f18b48a1f93ed5198ae5a627d4))
- fix typos ([fa14939](https://github.com/unit-mesh/auto-dev/commit/fa1493951d7931f1d526ae54460456af0d40079f))
- **flow:** implement getStories method ([c274f4b](https://github.com/unit-mesh/auto-dev/commit/c274f4bdb032a080bde8ad5069b2f43468749256))
- **i18n:** fix typo and shorten message length ([8e2f120](https://github.com/unit-mesh/auto-dev/commit/8e2f120bbed77e22985c89b45ad0499a0e8837c5))
- **java:** resolve issue with resolving classes in JavaTypeUtil ([e13fc40](https://github.com/unit-mesh/auto-dev/commit/e13fc4008db50baa5337a55ecd9bb3fc0f526e07))
- **javascript:** modify JavaScriptVersionProvider and add JavaScriptVersionProviderTest ([7113149](https://github.com/unit-mesh/auto-dev/commit/7113149cf0eb28a50c6e4af5b47c23824136b5cc))
- **provider:** add logger and remove unnecessary code ([c3bfc53](https://github.com/unit-mesh/auto-dev/commit/c3bfc53b9712d6a2d9034d7567652b3aa5156f14))
- **provider:** improve error message for missing language support ([19b6940](https://github.com/unit-mesh/auto-dev/commit/19b6940c3974bdf1700ebffe4e1a7902b97b662f))
- **rust:** fix 233 typo ([31c5f10](https://github.com/unit-mesh/auto-dev/commit/31c5f10ffe9b78ad0c6eaff558dfbb71458acd14))
- **rust:** fix tests ([c6761e8](https://github.com/unit-mesh/auto-dev/commit/c6761e87bc5bc01b3399350abed2358e739e8500))
- **service:** modify JavaWriteTestService to use JavaLanguage ([ea37212](https://github.com/unit-mesh/auto-dev/commit/ea3721287b020d94e49da33593a213442de9bffb))
- **test:** add error notification when test file creation fails ([45f21b2](https://github.com/unit-mesh/auto-dev/commit/45f21b2fd37d1510b922ae9057bf4f89385ec52c))
- **util:** rename JsUtil package ([f747922](https://github.com/unit-mesh/auto-dev/commit/f747922f58bd84604f0b81e403f0c475af03fd89))

### Features

- **autodev:** add Open Settings action to autodev-core.xml ([b192d30](https://github.com/unit-mesh/auto-dev/commit/b192d3018a209f19c46bdb465853afd56b44e6b7))
- **build:** add kover plugin for code coverage ([1e06da0](https://github.com/unit-mesh/auto-dev/commit/1e06da0e316bba83380255e0e435b386d1833909))
- **ci:** add Codecov coverage report upload ([f67277a](https://github.com/unit-mesh/auto-dev/commit/f67277a0c42e0d7017490f39f3ffd916c027d185))
- **codecov:** add Codecov badges to documentation and README ([b19f3bb](https://github.com/unit-mesh/auto-dev/commit/b19f3bb180e7c89bab406f057234f317c2fb9de7))
- **commit-message:** add guidelines for writing commit messages ([a4108bc](https://github.com/unit-mesh/auto-dev/commit/a4108bc49dece5d10553020a1468369dfa174759))
- **commit:** add commit message generation with template rendering ([ddbead7](https://github.com/unit-mesh/auto-dev/commit/ddbead757cc772f96e11a877e332e12b5f2cd626))
- **context:** add CppVariableContextBuilder ([f98ee90](https://github.com/unit-mesh/auto-dev/commit/f98ee90742e2b88720592d82560ae99bcf9060c6))
- **context:** improve formatting in ClassContext and VariableContext ([274ebc7](https://github.com/unit-mesh/auto-dev/commit/274ebc7a3bc4197306c3acf2534482058765631e))
- **cpp:** add CMakefileUtil and CppBuildSystemProvider ([cd6ccae](https://github.com/unit-mesh/auto-dev/commit/cd6ccaef6777b873083c35131977436479f45e6b))
- **cpp:** add comment about testing framework choice ([4aceadd](https://github.com/unit-mesh/auto-dev/commit/4aceadd079d40f8fdaba40fa5c1e87f53705b229))
- **cpp:** add CppCodeModifier class ([5b374c4](https://github.com/unit-mesh/auto-dev/commit/5b374c49698020d104b58f8eafff219be11ec454))
- **cpp:** add CppContextPrettify utility class and test case ([b3d9dd6](https://github.com/unit-mesh/auto-dev/commit/b3d9dd66292acf6a57d006f47eb1f4dff0c03c73))
- **cpp:** add CppFileContextBuilder for ObjectiveC ([f8208f9](https://github.com/unit-mesh/auto-dev/commit/f8208f909d9a6341a6e23e3a9e2a6125d24cc51a))
- **cpp:** add CppWriteTestService and CppContextPrettify modifications ([cce73c2](https://github.com/unit-mesh/auto-dev/commit/cce73c28c6fcff417ac94b8e1866b0751b4f6e91))
- **cpp:** add support for additional CIDR language constructs ([4b3588f](https://github.com/unit-mesh/auto-dev/commit/4b3588f252c3a310fb263ac721f64f7acecb4431))
- **cpp:** add test file creation logic ([89cda73](https://github.com/unit-mesh/auto-dev/commit/89cda732231887d0540780f6e684905ea0c62ff3))
- **directory:** add AutoDevDirectoryCompletionContributor ([a51de46](https://github.com/unit-mesh/auto-dev/commit/a51de4621fd13187423a0b2721cca32ab7bb7bd8))
- **docs:** add guide for configuring new language plugin ([f6e068b](https://github.com/unit-mesh/auto-dev/commit/f6e068bf47f1c28d1f17e31c25fd5a24522cb296))
- **github-actions:** add workflow generation ([9c018ca](https://github.com/unit-mesh/auto-dev/commit/9c018ca0dbd31e923df52dc20329dfa532516f15))
- **go:** add Go version and target version to chat context ([5ff85e0](https://github.com/unit-mesh/auto-dev/commit/5ff85e068d4ce73e39353538fa7e62806d063ac8))
- **go:** add GoStructContextBuilder and GoStructContextBuilderTest ([8d01da3](https://github.com/unit-mesh/auto-dev/commit/8d01da33423a8cef59a079a29c21462e27683dc2))
- **go:** add GoVariableContextBuilder and GoFileContextBuilder ([584e6c5](https://github.com/unit-mesh/auto-dev/commit/584e6c5f9c8104582a20273dbce29b5f57767414))
- **go:** add GoVersionChatContextProvider and GoWriteTestService ([aa875ec](https://github.com/unit-mesh/auto-dev/commit/aa875ec27d779dd57be3980c39a93e4d5155252a))
- **go:** add method context builder test ([3a5eaf3](https://github.com/unit-mesh/auto-dev/commit/3a5eaf3ae1df0067ebe3b7b3eedd650641b98cca))
- **goland:** add Go language support ([27ff5a6](https://github.com/unit-mesh/auto-dev/commit/27ff5a615949fdc1fae5e1207eac415761518fc2))
- **goland:** add GoMethodContextBuilder and GoPsiUtil ([5b89fc5](https://github.com/unit-mesh/auto-dev/commit/5b89fc579b28ec613e4a21bfd8e79ebd0b905619))
- **goland:** add GoWriteTestService for writing test cases ([2eef8d1](https://github.com/unit-mesh/auto-dev/commit/2eef8d1ad0aea989ebd5fb116b3c67e39a778eeb))
- **java, javascript:** add type resolution in test and fix return type handling ([10d9d87](https://github.com/unit-mesh/auto-dev/commit/10d9d873cc9603c49d30871dea8e29282cfb2590))
- **java:** add code prompter methods and documentation ([f7caff1](https://github.com/unit-mesh/auto-dev/commit/f7caff1567f28003b04592ddddcdf2ad76f70ea2))
- **javascript:** add import statements to TestFileContext ([2ce3bd4](https://github.com/unit-mesh/auto-dev/commit/2ce3bd4fa349643e235be582bc236a52b7d7c2b4))
- **javascript:** add JavaScriptClassContextBuilder and update JSPsiUtil ([8123892](https://github.com/unit-mesh/auto-dev/commit/812389267862b7204166baf07ff8861f00f037cc))
- **javascript:** add logging to JSWriteTestService ([ce00e1d](https://github.com/unit-mesh/auto-dev/commit/ce00e1d2ba20145d548fc3fd2a2fedafd90565d9))
- **javascript:** add test cases for JSWriteTestService ([99d3124](https://github.com/unit-mesh/auto-dev/commit/99d31245123237480d0f335128e4791e535e4dfd))
- **javascript:** improve test file generation ([e7a3a93](https://github.com/unit-mesh/auto-dev/commit/e7a3a938aabe928091694bc1fabba085a368c9f7))
- **provider:** add test element to TestFileContext ([79aba96](https://github.com/unit-mesh/auto-dev/commit/79aba9631a5f4a10072b9e64c640fce0552d1723))
- **python:** add Python language support to WriteTestService ([5e68d61](https://github.com/unit-mesh/auto-dev/commit/5e68d61ca05729072ec4df9e2cc291d96ef46875))
- **rust:** add forbiddenRules to RustLivingDocumentation ([166ec2d](https://github.com/unit-mesh/auto-dev/commit/166ec2d525c7ead698983c24e0d3d563d1bdd722))
- **rust:** add relevant classes to TestFileContext ([3e0c992](https://github.com/unit-mesh/auto-dev/commit/3e0c99273571698a86e6f52b1c1fe484dd25e362))
- **rust:** add Rust plugin to build.gradle.kts ([5611309](https://github.com/unit-mesh/auto-dev/commit/5611309e81a74417cf0b787956a4a428993f20c9))
- **rust:** add RustClassContextBuilderTest and modify RustClassContextBuilder ([5aa76c2](https://github.com/unit-mesh/auto-dev/commit/5aa76c249822610772c2bdd7540ccd96e9686568))
- **rust:** add RustCodeModifier class ([f538223](https://github.com/unit-mesh/auto-dev/commit/f5382233dfbf340d9602174cab11163b056a287b))
- **rust:** add RustFileContextBuilder for file context ([5c7ad6f](https://github.com/unit-mesh/auto-dev/commit/5c7ad6f7c9326d9dd3f4665fae7aac620b034c04))
- **rust:** add RustTestContextProvider and update WriteTestService ([eab438b](https://github.com/unit-mesh/auto-dev/commit/eab438bd15cd8684928ca62a17500723f320ea91))
- **rust:** add RustVariableContextBuilder ([96063c6](https://github.com/unit-mesh/auto-dev/commit/96063c6e2fe8dc5da0249b7109adbc3797468a48))
- **rust:** add support for formatting impl items ([af31942](https://github.com/unit-mesh/auto-dev/commit/af31942071aa314398cf25c7983ea0ef208d848a))
- **rust:** add test code insertion functionality ([7dd4cfd](https://github.com/unit-mesh/auto-dev/commit/7dd4cfd0d1f70d9aebd32387dd6f5a3a49c7ba19))
- **rust:** ignore tests for 2333 ([a4fe3b9](https://github.com/unit-mesh/auto-dev/commit/a4fe3b9f13a530049a131c1111d5987f60c4fce8))
- **rust:** update RustVersionContextProvider, RustTestService, RunCLion.xml, build.gradle.kts, RustMethodContextBuilder ([c41b4b1](https://github.com/unit-mesh/auto-dev/commit/c41b4b15975054a927a0439faf26c625a9648a6f))
- **statusbar:** add AutoDev status bar widget and action ([6747716](https://github.com/unit-mesh/auto-dev/commit/67477165a3330bed63d8ec17e567ca2a01e7f87f))
- **statusbar:** add AutoDev status bar widget factory ([38b37f6](https://github.com/unit-mesh/auto-dev/commit/38b37f657fad20f6f49e3c1fbf73794eb7092798))
- **statusbar:** add AutoDevStatusId to AutoDevStatusBarWidget ([a0993d6](https://github.com/unit-mesh/auto-dev/commit/a0993d66e393314e9d60d3ab6f530b2e5281505a))
- **statusbar:** add AutoDevStatusListener interface ([fab07cd](https://github.com/unit-mesh/auto-dev/commit/fab07cd91c9ebd3e171c87670ea8725b53768d9e))
- **statusbar:** add AutoDevStatusService and AutoDevStatus classes ([54269bc](https://github.com/unit-mesh/auto-dev/commit/54269bcc2a878bab0e548cd3e4196c1baa8b64ba))
- **statusbar:** add new status icons ([be22f9a](https://github.com/unit-mesh/auto-dev/commit/be22f9a5e349faee9a5a309d155a2813bd9154d4))
- **statusbar:** add status event producer ([66ef52f](https://github.com/unit-mesh/auto-dev/commit/66ef52f1c570823b5c9ef083e6edc2259ed9a6c8))
- **statusbar:** add status notifications during test generation ([b2aa8df](https://github.com/unit-mesh/auto-dev/commit/b2aa8dfca661f8ec7b3fb34cf5eceadefcc1a2ba))
- **statusbar:** enable AutoDevStatusBarWidget on status bar ([93bcffe](https://github.com/unit-mesh/auto-dev/commit/93bcffe2e28a3a7e9054e2ef413dc9a2a8baa170))
- **template:** add new AutoDev customize prompt action ([cf0b8d6](https://github.com/unit-mesh/auto-dev/commit/cf0b8d6b851ba4df538219da1004ca38ed704c42))
- **test:** add test case for Test.kt ([5a2d2cc](https://github.com/unit-mesh/auto-dev/commit/5a2d2ccb715b31f778f274f5c09980e5dc28153b))
- **test:** add test code templates ([2af285d](https://github.com/unit-mesh/auto-dev/commit/2af285dfd943b41b6de3b827a3595374fcfaef94))
- **testing:** add flow.collect to TestCodeGenTask ([2bb353e](https://github.com/unit-mesh/auto-dev/commit/2bb353e1894a083aa59e9f93d13783b32e6fda54))
- **testing:** add source code to be tested ([fc1763f](https://github.com/unit-mesh/auto-dev/commit/fc1763fcb57858cdbb97e21ccadc0bb347169586))
- **testing:** add webstorm version to build.gradle.kts ([363329b](https://github.com/unit-mesh/auto-dev/commit/363329b32f9c5631ebb39cb90b5944198c28ca08))
- **util:** add isInProject function ([e806624](https://github.com/unit-mesh/auto-dev/commit/e8066243efcd26f1cc40606519f362b034a4e766))
- **vcs:** add commit message suggestion functionality ([fb3caac](https://github.com/unit-mesh/auto-dev/commit/fb3caaca0d3b31163cb08688938fd462f04ef639))
- **vcs:** add flow.collect to CommitMessageSuggestionAction ([396317b](https://github.com/unit-mesh/auto-dev/commit/396317bfd8de7762caeab4e67cb57f67de4d4b56))
- **webstorm:** add guessTestFrameworkName utility function ([ce4baad](https://github.com/unit-mesh/auto-dev/commit/ce4baad6d94208ed66826120168d61bde0f2cd92))
- **webstorm:** add JavaScript test framework detection ([3e78eee](https://github.com/unit-mesh/auto-dev/commit/3e78eee7c5ed255d0bc9410385d2b7e1236f793a))
- **webstorm:** add JavaScriptVersionProvider ([8dd8902](https://github.com/unit-mesh/auto-dev/commit/8dd890291409f68e73dcbaf2bc8350e055644eca))
- **webstorm:** add support for web chat creation context ([0971813](https://github.com/unit-mesh/auto-dev/commit/09718139bd162e8e9414264d0ca0a541c1bb5002))
- **webstorm:** add utility functions for JavaScript testing ([10cc3ea](https://github.com/unit-mesh/auto-dev/commit/10cc3ea03f755c9223666c10d02eade53f1d272c))

## [1.5.3](https://github.com/unit-mesh/auto-dev/compare/v1.5.2...v[1.5.3]) (2024-01-12)

### Bug Fixes

- **commit:** empty commit message before generate ([25c4559](https://github.com/unit-mesh/auto-dev/commit/25c4559cb559679680d25ec1ca439bc2b159dcf6))
- **diff:** fix file rename message formatting ([2039103](https://github.com/unit-mesh/auto-dev/commit/203910341cc114310f4a0a2b12ac27a235d7739b))
- ignore test for 222 version ([bcd208d](https://github.com/unit-mesh/auto-dev/commit/bcd208d07847aab542a077a6288c5785122f0c9f))
- **kotlin:** update Kotlin API version message ([e3cee52](https://github.com/unit-mesh/auto-dev/commit/e3cee52e62843d657b22fe8c80ed8dfe4e7d9297))
- **provider:** fix KotlinTestDataBuilderTest and KotlinClassContextBuilder ([d23b9d5](https://github.com/unit-mesh/auto-dev/commit/d23b9d59d2ec772f341cc53fe23db6978ec4e30e))
- **provider:** insert method if code does not contain @Test annotation ([d49f41f](https://github.com/unit-mesh/auto-dev/commit/d49f41f6b095d4be9923c135b302e3d3ecffd199))
- **provider:** refactor KotlinTestContextProvider ([fa3364a](https://github.com/unit-mesh/auto-dev/commit/fa3364a274bba9faa89815e40c3407820f5c70ec))
- **vcs:** fix CommitMessageSuggestionAction not updating editorField text ([ef6c680](https://github.com/unit-mesh/auto-dev/commit/ef6c6802d0a91085d96e66225ddcce65eca80c5a))

### Features

- **code-review:** add action to ChangesViewToolbar ([6ccb3e7](https://github.com/unit-mesh/auto-dev/commit/6ccb3e7b5242793b2ebead802728c71eaf29b460))
- **context:** add support for annotations in ClassContext ([abc5305](https://github.com/unit-mesh/auto-dev/commit/abc53050c13ed5fa99f605bb31b6670f882e9b9b))
- **context:** add support for parsing Java annotations ([520ad09](https://github.com/unit-mesh/auto-dev/commit/520ad095065655298819ce2ec15ed433bfe7e9fc))
- **context:** update ClassContext to include package information ([b0f9fcb](https://github.com/unit-mesh/auto-dev/commit/b0f9fcb5ebdc5a4df1089916b5f2315ce04aecda))
- **diff:** add handling for delete files ([72724da](https://github.com/unit-mesh/auto-dev/commit/72724da5c629c784f0169336467ad969c29fe214))
- **diff:** add test case for DiffSimplifier ([f6dde52](https://github.com/unit-mesh/auto-dev/commit/f6dde52a680a0fb5ab8812700fbbcb40bb70f080))
- **java:** add base route retrieval to JavaTestDataBuilder ([df4d12e](https://github.com/unit-mesh/auto-dev/commit/df4d12eb70e2af7f5db235709af138d9776dae21))
- **java:** add JUnit rule caching mechanism ([21b5ae2](https://github.com/unit-mesh/auto-dev/commit/21b5ae2666c0a444ff25cd1952af42530a72d086))
- **kotlin:** add baseRoute method to KotlinTestDataBuilder ([3da7e16](https://github.com/unit-mesh/auto-dev/commit/3da7e16f9e65215a5383be828ec6a53082b213cb))
- **kotlin:** add getTypeText() function to KotlinContextCollector ([0f29abe](https://github.com/unit-mesh/auto-dev/commit/0f29abeff8cab49ea6d764b992e3e66e77c4e960))
- **kotlin:** add KotlinContextCollector class ([8845285](https://github.com/unit-mesh/auto-dev/commit/8845285a0b03111aa6c251b17afbb0d3d1ecbe45))
- **kotlin:** add KotlinVersionProvider ([c75e0fc](https://github.com/unit-mesh/auto-dev/commit/c75e0fc1ddc330ed6fd0e99246f756c8bb11d901))
- **kotlin:** add KotlinVersionProvider ([92c6611](https://github.com/unit-mesh/auto-dev/commit/92c661160490ceb39f6d9af7abf240d31b452502))
- **kotlin:** add test case for KotlinTestDataBuilder ([bca537e](https://github.com/unit-mesh/auto-dev/commit/bca537e75279b523edf5b798b8a7122bce17ac81))
- **living documentation:** add functionality to build documentation from suggestion ([8bc6677](https://github.com/unit-mesh/auto-dev/commit/8bc66777e2d51947333378fb7485417d48010e70))
- **livingdoc:** improve prompt builder instructions ([e4f319a](https://github.com/unit-mesh/auto-dev/commit/e4f319a35956769c6f782e710439d763ed21c107))
- **livingdoc:** improve prompt builder instructions ([08111fb](https://github.com/unit-mesh/auto-dev/commit/08111fb5be6e9b0cebe6bb1777bfcb849aacc1ea))
- **prompting:** add more file patterns to default ignore list ([19e58e8](https://github.com/unit-mesh/auto-dev/commit/19e58e861e69750471995729fbd97ee27aa9e701))
- **provider:** add imports to TestFileContext ([05419e5](https://github.com/unit-mesh/auto-dev/commit/05419e5ace469bebb425c3c72759539b32d3a9d2))
- **provider:** add isSpringRelated method to KotlinTestContextProvider ([06173db](https://github.com/unit-mesh/auto-dev/commit/06173dbb8f0316f3ab923231f0bb5e9201a39528))
- **provider:** add KotlinTestContextProvider ([c5903df](https://github.com/unit-mesh/auto-dev/commit/c5903df38c859ebe52366bf5b2913280fa3f1a9f))
- **provider:** add KotlinTestDataBuilder implementation ([559fcd4](https://github.com/unit-mesh/auto-dev/commit/559fcd4fe76c3cdcf2c19693050128ccdf77a39a))
- **provider:** add module dependency lookup ([e1a8da6](https://github.com/unit-mesh/auto-dev/commit/e1a8da68531ba12b9b57488c2741367352f3dc30))
- **provider:** add support for Spring annotations ([296169d](https://github.com/unit-mesh/auto-dev/commit/296169dc8b599386b5e96c96ba9009bd3ab269a1))
- **provider:** add templated test prompts ([94d588a](https://github.com/unit-mesh/auto-dev/commit/94d588a1aea6c8d09da86feab3d0d474c6e38c6a))
- **readme:** update installation instructions ([582b337](https://github.com/unit-mesh/auto-dev/commit/582b337169bce343fbb244bc94839f969e8a702b))
- **review:** remove unused methods and refactor computeDiff ([5c59c2e](https://github.com/unit-mesh/auto-dev/commit/5c59c2e6d5ceaceb0515de96ceb4c510f39cc488))
- **runConfigurations:** add BuildPlugin.xml ([3e92547](https://github.com/unit-mesh/auto-dev/commit/3e925470a12835741eba51b765d20c5e9fc990d7))
- **test:** add KotlinMethodContextBuilderTest ([11536f3](https://github.com/unit-mesh/auto-dev/commit/11536f36a2e279b149665db59ad75657f02af1a2))
- **test:** add logging for prompter ([2cae29e](https://github.com/unit-mesh/auto-dev/commit/2cae29eb656236c73313ebff1876b6d801d91251))
- **test:** add test case for MethodController ([776dab5](https://github.com/unit-mesh/auto-dev/commit/776dab52d77c678ea34a923f63d48473b12b7d27))
- **testing:** update test prompt with language information ([ce2abef](https://github.com/unit-mesh/auto-dev/commit/ce2abefdd16b9b742c57551a8f9d3d8a60d54367))
- **tests:** add code snippet to prompter ([b5a24ae](https://github.com/unit-mesh/auto-dev/commit/b5a24ae62a588e7bb155399c1e52bd8c65359f46))
- **tests:** add KotlinTestDataBuilderTest and KotlinTestDataBuilder ([07a0dd9](https://github.com/unit-mesh/auto-dev/commit/07a0dd98fbb59563b522a57cd730192e9fc84222))
- **vcs:** add asynchronous commit message suggestion ([6bfd34c](https://github.com/unit-mesh/auto-dev/commit/6bfd34c59ec431f68c01935afa19098a4bf87ecd))
- **vcs:** add logging for empty diff context ([16ab325](https://github.com/unit-mesh/auto-dev/commit/16ab32516085b3e52dc9a0678692ca50ab8f142f))
- **vcs:** add notification for empty diff context ([f2382c5](https://github.com/unit-mesh/auto-dev/commit/f2382c5d75612fd3fa78520dadff418cec623c15))
- **vcs:** add PrepushReviewAction ([d16bb38](https://github.com/unit-mesh/auto-dev/commit/d16bb38bde4339c8388976fac07b26a4e711c760))
- **vcs:** add VcsPrompting import and service ([d25f92b](https://github.com/unit-mesh/auto-dev/commit/d25f92be86d38635097c1972e0a28414bb3256a7))
- **vcs:** generate commit message with prompt ([8d6932e](https://github.com/unit-mesh/auto-dev/commit/8d6932e252a29898204e65a9bedf501a6e056d54))

## [1.5.2](https://github.com/unit-mesh/auto-dev/compare/1.4.4...v[1.5.2]) (2024-01-05)

### Bug Fixes

- add lost messages for Azure ([468a5ce](https://github.com/unit-mesh/auto-dev/commit/468a5ce4c3fe27960d31fd73ee9e1818fdf16025))
- **apidata:** disable java in qualified name ([db4646a](https://github.com/unit-mesh/auto-dev/commit/db4646a68960f4c10e228a95e0fca5884439535e))
- **apidata:** fix fieldName erorr for class strcuture ([af5a87b](https://github.com/unit-mesh/auto-dev/commit/af5a87b43c09a47f5fac6e841248335964899510))
- **apidata:** fix simple handle for uppsercase ([6152514](https://github.com/unit-mesh/auto-dev/commit/61525145e6b21d9db8f54410324b47425ddc9fad))
- disable default choice for chat base action ([53a192b](https://github.com/unit-mesh/auto-dev/commit/53a192b7ec18e2cb2a966437d951a838f38f5cda))
- disable file cache ([97a793a](https://github.com/unit-mesh/auto-dev/commit/97a793aba43049726ebe24d336002c99ec987fc5))
- **doc:** update doc by experience ([1b374de](https://github.com/unit-mesh/auto-dev/commit/1b374def5805425592ffdc63dcc38f44cd08e14c))
- fix 222 build issue on import ([fb468a4](https://github.com/unit-mesh/auto-dev/commit/fb468a4dbbaef48e1fe8725b9eb8b415d5711d8a))
- fix auto completion lost request ([7914ec2](https://github.com/unit-mesh/auto-dev/commit/7914ec23531aec21dca5a56a0a52e80996109d14))
- fix axure auto service issue ([2ad6f43](https://github.com/unit-mesh/auto-dev/commit/2ad6f43f2ca456a1fe9c06f4320c4d4dca8aa87b))
- fix build & Depecrated 213 && 221 && closed [#59](https://github.com/unit-mesh/auto-dev/issues/59) ([1221128](https://github.com/unit-mesh/auto-dev/commit/1221128f9a59425bb9f81d79f965f03eb4d8e9e8))
- fix chat panel crash issue ([2818699](https://github.com/unit-mesh/auto-dev/commit/28186993d32c067d8d18ba95cc09c302b26b0b3b))
- fix commit message gen issues ([149069a](https://github.com/unit-mesh/auto-dev/commit/149069a7089eff89e9c0407b3c28a921ba75cad6))
- fix compile issue ([71a7eb1](https://github.com/unit-mesh/auto-dev/commit/71a7eb129feb8353d7d1f45b977750c3ded5efdc))
- fix config issues ([5c75798](https://github.com/unit-mesh/auto-dev/commit/5c75798eec41a1f4e99e93931f9ff88558d6558d))
- fix empty input issue ([aa2846b](https://github.com/unit-mesh/auto-dev/commit/aa2846b245bf2334bc58cbd31300be7003482d64))
- fix erro action again ([9cdadd2](https://github.com/unit-mesh/auto-dev/commit/9cdadd2c25cf1345a86edb635f0ee0dc83cf6d78))
- fix get issues ([02979db](https://github.com/unit-mesh/auto-dev/commit/02979db1b37aeed713e8e9258d30762bd355483a))
- fix import issue ([9dba8db](https://github.com/unit-mesh/auto-dev/commit/9dba8dbfe4a604d296f8e8bd86adc2bf8f9f7f71))
- fix imports ([463dc13](https://github.com/unit-mesh/auto-dev/commit/463dc1355bab485d47a0fb8054ac69c0924d5612))
- fix imports ([36f39d2](https://github.com/unit-mesh/auto-dev/commit/36f39d23f7e6bddfb61a24d3681db004f161cbbf))
- fix imports ([ed3db21](https://github.com/unit-mesh/auto-dev/commit/ed3db21eff80a87e800544cddf87b167468767f0))
- fix int autodev llm server error issue ([04691fc](https://github.com/unit-mesh/auto-dev/commit/04691fcd15cfbb2d707dc82731fcf70b2fbd2015))
- fix kotlin code modifier issue ([501fccc](https://github.com/unit-mesh/auto-dev/commit/501fccc2f6c911e97848c4f599e3a0e8f1f56a91))
- fix kotlin read issue ([96b13cb](https://github.com/unit-mesh/auto-dev/commit/96b13cb14db7bc445931c951d1192ed7e3844f25))
- fix python test isse ([e407586](https://github.com/unit-mesh/auto-dev/commit/e40758644411303351db8e4d18ea0772cde1dd52))
- fix replace issue in comment ([0683e6f](https://github.com/unit-mesh/auto-dev/commit/0683e6f96915c59252d8f4251dda7459de16d416))
- fix strategy issues ([333b75e](https://github.com/unit-mesh/auto-dev/commit/333b75e82974454370ea954950d11a5b68dc6445))
- fix super class lost issue ([31907a6](https://github.com/unit-mesh/auto-dev/commit/31907a6b3d58e78cc3807487ee657018ac2f0110))
- fix test ([7a70f8b](https://github.com/unit-mesh/auto-dev/commit/7a70f8b5c77082e3bd00d59f9d0316b750401aba))
- fix test issues ([4a9ea9c](https://github.com/unit-mesh/auto-dev/commit/4a9ea9c095ef531b7e259edbc106ae99f273b506))
- fix test prompt issue ([ec89161](https://github.com/unit-mesh/auto-dev/commit/ec89161d896c5bc8b68a64d4b2e6ec656ea925d4))
- fix tests ([688499c](https://github.com/unit-mesh/auto-dev/commit/688499c4ec5bf4b1fbd10cdf87e9d907d80378f5))
- fix tests ([f452428](https://github.com/unit-mesh/auto-dev/commit/f452428f8dd43c8b49294009bc558612fc482770))
- fix tests ([ab69050](https://github.com/unit-mesh/auto-dev/commit/ab69050f36f71dcca4b4f2b073babfefad2e5fb7))
- fix typos ([95e1a9f](https://github.com/unit-mesh/auto-dev/commit/95e1a9f78bbba9b75828cd7a9ebd584c3c4e761f))
- is some super class no in project will be crash ([d203734](https://github.com/unit-mesh/auto-dev/commit/d2037344a0e5779466181995aa4e31388b1a4868))
- make output in file better ([6a25cf3](https://github.com/unit-mesh/auto-dev/commit/6a25cf3375a0ae654e1ee20e41501b0b7b1c13c1))
- 修复前一提交 SSE 的问题 ([54457f5](https://github.com/unit-mesh/auto-dev/commit/54457f558ce1f8c939fead5562835a6e7a6c13ae))

### Features

- add catch for not ready action ([79286b0](https://github.com/unit-mesh/auto-dev/commit/79286b022a2d759d7198d912afaa1e37714712f6))
- add cleanup for kotlin code ([dd84795](https://github.com/unit-mesh/auto-dev/commit/dd847957bd08c4bf44ea5da37407591391c09b87))
- add config for disable advanced prompt ([40b61f1](https://github.com/unit-mesh/auto-dev/commit/40b61f16d2ec6b64e35b57f26e7215a72ed08184))
- add filter for caches and skip for popular frameworks ([6e4c205](https://github.com/unit-mesh/auto-dev/commit/6e4c205faa1b18dc34dfc5728764f651f5b2e662))
- add for junit rules ([dc5289d](https://github.com/unit-mesh/auto-dev/commit/dc5289dba409b3ef46752cff95e66869a2e2a9fd))
- add for test file path ([16fc139](https://github.com/unit-mesh/auto-dev/commit/16fc139f1ad8dde10c939e01c8578fcdb250ced9))
- add more for overvide prompts [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([f9b37b8](https://github.com/unit-mesh/auto-dev/commit/f9b37b8f49ba33adbd4b4d7c32736b3a89fe5673))
- add timeout for handler ([687b04a](https://github.com/unit-mesh/auto-dev/commit/687b04aabfe76f8ef9227f721843288e0c6ec843))
- **coder:** add custom for generate test [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([b10a0f5](https://github.com/unit-mesh/auto-dev/commit/b10a0f503f2aa6be02a001e45dc7ddd5dafd6102))
- **coder:** add disable history messages [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([265d343](https://github.com/unit-mesh/auto-dev/commit/265d343ca896ec5206a32ea9df1b02809c35500b))
- **coder:** enable custom txt for prompt ([019ae0d](https://github.com/unit-mesh/auto-dev/commit/019ae0d6a94d8132af8df383f00c9e6e6459a039))
- **coder:** init for setting config ([6db0dc8](https://github.com/unit-mesh/auto-dev/commit/6db0dc8b6b961a5abe39c340b7efe7c35cf95e4f))
- **coder:** init local completion for sample code ([c7b8d21](https://github.com/unit-mesh/auto-dev/commit/c7b8d2141bacc08bee95ece6d25a2568371ef683))
- enable recording datasets works in local ([2fcab1c](https://github.com/unit-mesh/auto-dev/commit/2fcab1c532933f461627504eafa11de48098d27a))
- init recording local param for [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([f24cc6a](https://github.com/unit-mesh/auto-dev/commit/f24cc6a90b2c1200c254cab255869c441cd4ca57))
- **js:** add documentation support ([f6b0136](https://github.com/unit-mesh/auto-dev/commit/f6b0136892b1471a4c6d5c4cb263f32a4926bc79))
- **js:** make doc better ([c69a335](https://github.com/unit-mesh/auto-dev/commit/c69a3350ecfe5b8919c7623c1e1516da7db28d51))
- **python:** add lookup funciton for target ([6a828df](https://github.com/unit-mesh/auto-dev/commit/6a828df2d313a6f3b16dadeffaed01486801ebd8))
- **python:** fix insert issues ([6786175](https://github.com/unit-mesh/auto-dev/commit/6786175c34ea9a50d8f69935c099c880d359355e))
- **python:** init python doc ([c30111d](https://github.com/unit-mesh/auto-dev/commit/c30111de7ea3b43d34e0f4c99c96efc8b3c2586e))
- **rust:** fix for insert issue ([9ce7734](https://github.com/unit-mesh/auto-dev/commit/9ce7734e41943160a153adb3e72bc935d682b143))
- **rust:** init basic documentation ([a63049b](https://github.com/unit-mesh/auto-dev/commit/a63049b672ef8ae1e017dab445f631a305f68523))
- try to add unload listener ([11f7c28](https://github.com/unit-mesh/auto-dev/commit/11f7c287cba32b4d43157a5c84b23e22f9731789))
- try to handle for basic class issues ([2897dd7](https://github.com/unit-mesh/auto-dev/commit/2897dd743638ed9504a3c49752ab1b56b1704b52))
- update java prompts ([d321afe](https://github.com/unit-mesh/auto-dev/commit/d321afe66d2c6e8e87ef2d5f768b6c33dcd34df0))

### Reverts

- Revert "refactor: clean code" ([afe5460](https://github.com/unit-mesh/auto-dev/commit/afe54608c6028b2530b2c734d1e136785300c6b5))

## [1.4.4](https://github.com/unit-mesh/auto-dev/compare/v1.4.3...[1.4.4]) (2023-12-14)

### Bug Fixes

- customEngineServer -> customOpenAiHost ([cf03a2b](https://github.com/unit-mesh/auto-dev/commit/cf03a2b46607880e20d68763bf8266c1aac1b31f)), closes [#62](https://github.com/unit-mesh/auto-dev/issues/62)
- demo issue ([434cd77](https://github.com/unit-mesh/auto-dev/commit/434cd7717c6f0aaa7e145421a83e1a447d8161bf))
- fix import runblocking issue ([439b136](https://github.com/unit-mesh/auto-dev/commit/439b136b5337ea7205463ee560489b1837283c1b))
- fix lint ([fe52832](https://github.com/unit-mesh/auto-dev/commit/fe52832442bc61642428245246016f8024dd7898))
- fix syntax error issue ([8642d8d](https://github.com/unit-mesh/auto-dev/commit/8642d8d0e070c27039ebef1e8d847fc43f3f5ec9))
- fix 助手回答为空的情况。 ([e0cca51](https://github.com/unit-mesh/auto-dev/commit/e0cca51f80c4488ddab5c4cf74dc277ddd207a76))
- state 改为 getter 避免配置更新不生效 ([d53c6aa](https://github.com/unit-mesh/auto-dev/commit/d53c6aaa24cc5988e2301ee8741fd9fac13a9f2b)), closes [#62](https://github.com/unit-mesh/auto-dev/issues/62)
- state 改为 getter 避免配置更新不生效 ([a3b22ae](https://github.com/unit-mesh/auto-dev/commit/a3b22ae1db2dad0ade8c3d5a4834b39b31ddd28f)), closes [#62](https://github.com/unit-mesh/auto-dev/issues/62)

### Features

- add request format complete ([07bfced](https://github.com/unit-mesh/auto-dev/commit/07bfced3e4960f1dbc013d5f0fcf4aa753adce7c))
- add request format complete ([f341134](https://github.com/unit-mesh/auto-dev/commit/f341134f055a4be599f79906dc7409f938768596))
- add request format config ([e0f040e](https://github.com/unit-mesh/auto-dev/commit/e0f040ece73505afa312a484da0351e57eb5e465))
- add request format config ([ac03af8](https://github.com/unit-mesh/auto-dev/commit/ac03af87f2440a9e10c4b3517eaccc70f71d1fd0))
- add request format logic-1 ([054b587](https://github.com/unit-mesh/auto-dev/commit/054b587f71d794506ce638dafa594e92112ffd7a))
- add request format logic-1 ([20132e4](https://github.com/unit-mesh/auto-dev/commit/20132e4565a87d4d2a6cd434f12142ce1c434fde))
- add xinghuo api version config setting ([d6d035a](https://github.com/unit-mesh/auto-dev/commit/d6d035aace1f423bf6e0ec4c89ef5caeaf6be542))
- **ts:** init basic doc writing listener ([3786575](https://github.com/unit-mesh/auto-dev/commit/3786575ceaa7b37bde31c553b4d0eeb2ef8fcdd9))
- 增加自定义请求。可修改请求 Header 及 reqeust body ([a72f085](https://github.com/unit-mesh/auto-dev/commit/a72f0857bd9bec59a63b335cdea4d033027d850b))

## [1.4.3](https://github.com/unit-mesh/auto-dev/compare/v1.4.1...v[1.4.3]) (2023-11-20)

### Bug Fixes

- add disposed ([b7e467f](https://github.com/unit-mesh/auto-dev/commit/b7e467f66b60d67cfe1d8573360bd81106d41199))
- align to new api changes ([59999d8](https://github.com/unit-mesh/auto-dev/commit/59999d8b45cc8b7754d8408a590d759aca542a2b))
- fix build typos ([34b9b15](https://github.com/unit-mesh/auto-dev/commit/34b9b15f1141b27b311707b4cf6653f5017918fd))
- fix config isseu ([d9d5133](https://github.com/unit-mesh/auto-dev/commit/d9d5133dea363fd553584ccd8c869d1a1efab018))
- fix deps for 233 ([225cf49](https://github.com/unit-mesh/auto-dev/commit/225cf49b8b928e598a65001cfcc22ca9252c6d60))
- fix testing issue ([7fac889](https://github.com/unit-mesh/auto-dev/commit/7fac88917f15e5cd974efbdcf28dc1f37d50ec64))
- potential IndexOutOfBoundsException when streaming response ([c3f8c43](https://github.com/unit-mesh/auto-dev/commit/c3f8c43bc3501f16d790a0549d893e2094ca7c59))
- try to six string type issue ([f7aee83](https://github.com/unit-mesh/auto-dev/commit/f7aee83c98b8e0b4d5e6fb1637e70c99d5f45cb9))

### Features

- **213:** fix some deps version issues ([aad9032](https://github.com/unit-mesh/auto-dev/commit/aad90321518a0ca3a23ac82fa43dfacecc8fc6ff))
- add collect for class data structure [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([e0d9d89](https://github.com/unit-mesh/auto-dev/commit/e0d9d89f2e5788ce7b1135fb20ade673059b9885))
- add gen for test data [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([dc2bd1a](https://github.com/unit-mesh/auto-dev/commit/dc2bd1a803ec391becf9e4c3a39da255373a5c44))
- add lost boxed type for convert inout bound [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([ed67f2a](https://github.com/unit-mesh/auto-dev/commit/ed67f2ab4f45cce7aface52f1a4ad7ea44071cf0))
- impl output bound data [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([1a2e01e](https://github.com/unit-mesh/auto-dev/commit/1a2e01e973cb5a357914e15523c8200b246eb590))
- init render option for children [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([610e33b](https://github.com/unit-mesh/auto-dev/commit/610e33b143a4d8aea423591152aa039937fdc24e))
- make custom quick action works [#57](https://github.com/unit-mesh/auto-dev/issues/57) ([f7e12e0](https://github.com/unit-mesh/auto-dev/commit/f7e12e0566198817ed14c7841eabda1d693126eb))
- make java lang works and closed [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([b1b7440](https://github.com/unit-mesh/auto-dev/commit/b1b7440db7c08147fb82570e6bf7bf23ef2705b1))
- **migration:** init generate test data enterpoint [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([486e5a0](https://github.com/unit-mesh/auto-dev/commit/486e5a053a2dbcb55ac51a554a030b228322d568))
- thinking in prepare context for input and output [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([1923476](https://github.com/unit-mesh/auto-dev/commit/1923476f344b5ea706815717b0d53900fc524275))
- **writing:** add prompt type for custom action prompt ([0bae8b0](https://github.com/unit-mesh/auto-dev/commit/0bae8b08d1200ef9a9fe94d72a9c5ca7ea0a8401))

### Reverts

- Revert "chore: setjdk to 11 for 213" ([3f34f89](https://github.com/unit-mesh/auto-dev/commit/3f34f89e939206f8a493ab584b84e771e836323f))

## [1.4.1](https://github.com/unit-mesh/auto-dev/compare/v1.4.0...v[1.4.1]) (2023-10-28)

### Bug Fixes

- fix document erorr issues ([efedf72](https://github.com/unit-mesh/auto-dev/commit/efedf72c0798c15777a7f69cab5fe3cee907d3d5))
- **test:** fix instruction error issue ([f7c9e03](https://github.com/unit-mesh/auto-dev/commit/f7c9e0305d823dbca7e6e8d408d09dd3c5867c4d))
- **test:** fix lost current class for method testing ([7d81858](https://github.com/unit-mesh/auto-dev/commit/7d8185878e0c4d17aa99c15e09c80fbdb3b8af19))

### Features

- init commits for changes ([a6d1d82](https://github.com/unit-mesh/auto-dev/commit/a6d1d82d0ed3ed3f50999d80c94e122ba621c25a))
- **reivew:** init fetch github issue by story ids ([2b79429](https://github.com/unit-mesh/auto-dev/commit/2b79429b67bb311d05514c5035719e382fb4bd42))
- **review:** add chinese context to full context ([8bcd37b](https://github.com/unit-mesh/auto-dev/commit/8bcd37b0b970a6fe24e209b7b778365f071abfcc))
- **review:** init default file ignore for patterns ([48ee07f](https://github.com/unit-mesh/auto-dev/commit/48ee07f8da7040ea23460b6ce5953e65c034ef9b))
- **review:** make fetch github issue works ([285da68](https://github.com/unit-mesh/auto-dev/commit/285da68ef7afd5c24a20b3c47162cfd40364d3e7))

## [1.2.5](https://github.com/unit-mesh/auto-dev/compare/v1.2.3...v[1.2.5]) (2023-10-15)

### Bug Fixes

- change jsonpath to https://github.com/codeniko/JsonPathKt && fixed [#48](https://github.com/unit-mesh/auto-dev/issues/48) ([7973211](https://github.com/unit-mesh/auto-dev/commit/79732111cb903f3e62d8256de2d66a753bd3702a))
- fix inout empty issue ([13e0ab1](https://github.com/unit-mesh/auto-dev/commit/13e0ab11337c03f44b7d26074b0758cbd61fe2c8))
- fix prompt ([215e7d8](https://github.com/unit-mesh/auto-dev/commit/215e7d85d99c42f73e3c148882b3f2427194d952))

### Features

- **docs:** adjust prompt ([d12ea4a](https://github.com/unit-mesh/auto-dev/commit/d12ea4a71bfb9b53472268ae2238076063bd5f6c))
- **docs:** init kotlin living documentation ([abf3d74](https://github.com/unit-mesh/auto-dev/commit/abf3d743d4a06b4959e6eaba1ca1e505d582ec93))

## [1.2.3](https://github.com/unit-mesh/auto-dev/compare/v1.2.2...v[1.2.3]) (2023-10-09)

### Bug Fixes

- add fix for [#47](https://github.com/unit-mesh/auto-dev/issues/47) ([04f9c1b](https://github.com/unit-mesh/auto-dev/commit/04f9c1bd694336402c57c00208d5400e6dda5c59))
- fix custom prompt emoji issue ([13e7061](https://github.com/unit-mesh/auto-dev/commit/13e7061a54bab06b2b00c07e0a6d5c9119fd0f3c))

## [1.2.2](https://github.com/unit-mesh/auto-dev/compare/v1.2.1...v[1.2.2]) (2023-09-24)

### Bug Fixes

- [#40](https://github.com/unit-mesh/auto-dev/issues/40) ([010d142](https://github.com/unit-mesh/auto-dev/commit/010d142d630a23f02cdde153483467ad4dedf493))
- add sleep for debug mode, if machine is slowly ([06a6dbe](https://github.com/unit-mesh/auto-dev/commit/06a6dbe3358f6cc62e413d3e8f8b55dfd19bd55b))

### Features

- **chat:** add delay before rendering last message  The default delay is 20 seconds, but it can be customized. ([17e5585](https://github.com/unit-mesh/auto-dev/commit/17e5585612c7338ab4a17635a2ec1fa35949785e))
- **settings:** add quest delay seconds parameter ([f423caf](https://github.com/unit-mesh/auto-dev/commit/f423caf969f8a6490221d35b5449276549853e38)), closes [#21](https://github.com/unit-mesh/auto-dev/issues/21)

## [1.2.1](https://github.com/unit-mesh/auto-dev/compare/v1.2.0...v[1.2.1]) (2023-08-25)

### Bug Fixes

- [#36](https://github.com/unit-mesh/auto-dev/issues/36) 添加漏掉的 GitHub Token 选项 ([cfb6a60](https://github.com/unit-mesh/auto-dev/commit/cfb6a608d9bb620e99ba0b4783f712605e7f6e04))
- add clear message for [#35](https://github.com/unit-mesh/auto-dev/issues/35) ([cf60775](https://github.com/unit-mesh/auto-dev/commit/cf607755a4254052461ff0dbf8da5125ebee0fbb))
- change current selected engine not apply(until reopen project) ([01ea78a](https://github.com/unit-mesh/auto-dev/commit/01ea78a031585b24a49f76048f00a1047fdfb7bc))
- **counit:** fix syntax error issue ([f532d1e](https://github.com/unit-mesh/auto-dev/commit/f532d1e6d8fdac56461fe5fac9f1e2d5005eee22))
- Custom LLM engin server not work when change(until reopen project) ([dd488de](https://github.com/unit-mesh/auto-dev/commit/dd488de3e7a1c450c9bc2876f948f9242ed3187e))
- fix ci ([9b8859b](https://github.com/unit-mesh/auto-dev/commit/9b8859b02e8b394db8f2ea4d6f52ef4262e70acc))
- fix lost user information issue ([2bfc82a](https://github.com/unit-mesh/auto-dev/commit/2bfc82ab80616c1aec617762e0569713cec33d01))
- fix role issues ([fe75364](https://github.com/unit-mesh/auto-dev/commit/fe753645bc5e44a90101e5ba914267cb82f4b3f7))
- fix url path issue ([b4c2001](https://github.com/unit-mesh/auto-dev/commit/b4c2001b0434f99b627eb1f8b8e13ed1944f1c55))
- if already used Custom Engine config will not work after changed until reopen project. close [#31](https://github.com/unit-mesh/auto-dev/issues/31) ([624b0bc](https://github.com/unit-mesh/auto-dev/commit/624b0bcfe156012dfb0f26e885c202e0f715da8d))
- 添加漏掉的 custome request format 选项 ([abfd500](https://github.com/unit-mesh/auto-dev/commit/abfd500b64700fc7c141fe9b6749170c30631c67))

### Features

- add clion context   as example ([4489358](https://github.com/unit-mesh/auto-dev/commit/4489358a36522a8d2842a5f2a0015f21abac200b))
- **counit:** align to new APIs datastructure ([1150f54](https://github.com/unit-mesh/auto-dev/commit/1150f54f5ba8cb422aecc36867504400870d0942))
- **counit:** fix format ([a010536](https://github.com/unit-mesh/auto-dev/commit/a0105368d5e9c81cc1968210db78763aee191317))
- **counit:** init basic apis ([e78f864](https://github.com/unit-mesh/auto-dev/commit/e78f864afdc3a5c4e6869d2bc8109fb059cc7247))
- **counit:** init basic prompter ([1a30643](https://github.com/unit-mesh/auto-dev/commit/1a306437275b12c81cd1bc783ec75b0485fbc25d))
- **counit:** init counit setting service ([51bc5fb](https://github.com/unit-mesh/auto-dev/commit/51bc5fb8f74ca873cbe141e55ffaaa22b3fce885))
- **counit:** init for tool panel ([b2215ba](https://github.com/unit-mesh/auto-dev/commit/b2215ba8928d8f83e9d2fa32579eb3e7db00ab49))
- **counit:** make basic work flows ([21c25c7](https://github.com/unit-mesh/auto-dev/commit/21c25c774647eec638a3571de43553da574f9614))
- **document:** revert for json path language because different IDE product issue ([acae8ea](https://github.com/unit-mesh/auto-dev/commit/acae8eadebf335a18a213b7fefd9d6290f718921))
- init cpp module for ruipeng ([f74f094](https://github.com/unit-mesh/auto-dev/commit/f74f0949f3f12aae6dabbeacde86933c8be66987))
- **kanban:** add GitLabIssue implementation ([ab0d91d](https://github.com/unit-mesh/auto-dev/commit/ab0d91d1d7084b3f90f3bad56451d6e4af4eaec6))
- **rust:** init context ([4e7cd19](https://github.com/unit-mesh/auto-dev/commit/4e7cd19ba30f98ecf2649f844e0bcd4e4e085b00))
- **settings:** add GitLab options ([36548b5](https://github.com/unit-mesh/auto-dev/commit/36548b5975710870ba661f44eb67d975410c1991))
- **settings:** add new LLMParam components ([946f5a0](https://github.com/unit-mesh/auto-dev/commit/946f5a083b8a1fde8ac5f39f1ef76a87634d58b1))
- **settings:** add new LLMParam components ([dac557b](https://github.com/unit-mesh/auto-dev/commit/dac557be7ee264907da6a85c9836df008b58a95a))
- **settings:** add XingHuo provider configuration close [#29](https://github.com/unit-mesh/auto-dev/issues/29) ([cca35c6](https://github.com/unit-mesh/auto-dev/commit/cca35c6d8136dc7d4a81edf96e3a38692dbc6746))

## [1.1.4](https://github.com/unit-mesh/auto-dev/compare/v1.1.3...v[1.1.4]) (2023-08-18)

### Features

- add json path for config ([46efe24](https://github.com/unit-mesh/auto-dev/commit/46efe24a63009dde13142ba941463efa11239293))

## [1.1.3](https://github.com/unit-mesh/auto-dev/compare/v1.1.2...v[1.1.3]) (2023-08-18)

### Bug Fixes

- add lost for match string ([bf0e1a4](https://github.com/unit-mesh/auto-dev/commit/bf0e1a446ffc94460ec99d9f0d93364a376b507f))
- align cutom LLM server api to OpenAI API ([a718ed1](https://github.com/unit-mesh/auto-dev/commit/a718ed1944faca6554db7b2234d2e95572a6b9dd))
- AutoCrud Action only available when editor has selection ([cf49bd1](https://github.com/unit-mesh/auto-dev/commit/cf49bd1d86f7d56bf714ef5a099a684f10695aa5))
- change default json path lib to Kotlin version && done [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([4d5e573](https://github.com/unit-mesh/auto-dev/commit/4d5e5739562de032997a1ef82f4a9752aad61ccc))
- **doc:** fix format issues ([6e9c1a5](https://github.com/unit-mesh/auto-dev/commit/6e9c1a50746e37700315368c723a97a4b7e9a583))
- fix no returns ([87d688f](https://github.com/unit-mesh/auto-dev/commit/87d688f144af0465bcb9f95bf99a8e6e5936eed6))
- fix typos ([23effbd](https://github.com/unit-mesh/auto-dev/commit/23effbd0337c19a0fa712e252f6ad0778cdadc04))
- openai setting change not affect in same project lifecycle (will affect after reopen project) ([eb74909](https://github.com/unit-mesh/auto-dev/commit/eb74909f08d89d42fbdb10d896df8f8201ea1f3b))
- remove correnct line in duplicated ([6758260](https://github.com/unit-mesh/auto-dev/commit/67582604967a60558af94b8f6ab36ef434a9b48d))
- **similar:** fix lost chunk issues ([e003da9](https://github.com/unit-mesh/auto-dev/commit/e003da9ea192d18ebb99383b37d960c0bea5e1b9))

### Features

- add basic ins cancel logic for code complete ([018a82e](https://github.com/unit-mesh/auto-dev/commit/018a82e7a012cb71ef00db4ef6d437e6106e2823))
- add json path lib as design for [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([2c85f40](https://github.com/unit-mesh/auto-dev/commit/2c85f40b93ff93f9bff56451763646986e22f5d9))
- add lost docs adn closed [#27](https://github.com/unit-mesh/auto-dev/issues/27) ([daa43c1](https://github.com/unit-mesh/auto-dev/commit/daa43c18af93f1b40dd3fd0e67e51429df9a5308))
- add tips for Autocrud mode && closed [#28](https://github.com/unit-mesh/auto-dev/issues/28) ([bc837ca](https://github.com/unit-mesh/auto-dev/commit/bc837ca54352ca95b2cf9b904efbb6e2b7cac9e2))
- **custom:** fix new line issues ([6da2821](https://github.com/unit-mesh/auto-dev/commit/6da282106be8b862514c8fae846c258dc14d264d))
- **doc:** add result as context ([c8ebf40](https://github.com/unit-mesh/auto-dev/commit/c8ebf40dbc9d1954e6e9b8a83672ee3332e79d01))
- **docs:** add living documentation example ([2f9de5d](https://github.com/unit-mesh/auto-dev/commit/2f9de5d38c88a65457239fccc3d91645ba28be8b))
- **docs:** add living documentation examples ([1b8c42f](https://github.com/unit-mesh/auto-dev/commit/1b8c42f18ade8907497ae9ef80feb07049566466))
- init basic doc for [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([1e2199c](https://github.com/unit-mesh/auto-dev/commit/1e2199c13746424ebdae38b9a41ce31090e9989a))
- init custom doc config ([bcee2a5](https://github.com/unit-mesh/auto-dev/commit/bcee2a5210f050893b0e03b49d0adcbe206bdb87))
- init ui config for custom json format [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([b249f72](https://github.com/unit-mesh/auto-dev/commit/b249f72987e11dec62fa8c582ba699c442fd8584))
- **similar:** fix query size issue ([220eb29](https://github.com/unit-mesh/auto-dev/commit/220eb296e2b55adecec11af950b527cfccd6c96d))
- try to add json path && but server crash :crying: [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([88cbb9d](https://github.com/unit-mesh/auto-dev/commit/88cbb9d68865db420ca03dbfd131650529b78801))
- try to add prompot schema ([1aff07e](https://github.com/unit-mesh/auto-dev/commit/1aff07e4e17004815cc80f6ccaa48e623942ed6e))
- update for app settings ui [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([5cef076](https://github.com/unit-mesh/auto-dev/commit/5cef07684a7f5c0ffa7f5b8fc22a66fd0cb27d06))

## [1.1.2](https://github.com/unit-mesh/auto-dev/compare/v1.1.1...v[1.1.2]) (2023-08-09)

### Bug Fixes

- fix compile issues ([7e2c936](https://github.com/unit-mesh/auto-dev/commit/7e2c936b0db7a475a82242f95629ed3bdbc9fccd))
- fix crash ([24c3cd7](https://github.com/unit-mesh/auto-dev/commit/24c3cd7a0ee9cae68044571473cb6a0778a93889))
- fix permissions ([f85a28d](https://github.com/unit-mesh/auto-dev/commit/f85a28d59d5edefbceba571429227856af4d91b3))
- 修复编译失败 ([ccc539b](https://github.com/unit-mesh/auto-dev/commit/ccc539bb0adba83a72b21e4664a81a255f94eaae))

### Features

- **doc:** add basdic prompt ([4fddd6d](https://github.com/unit-mesh/auto-dev/commit/4fddd6d28777fe973dc2c64f26a3e088feec7ea7))
- **doc:** init basic intention ([3c050e0](https://github.com/unit-mesh/auto-dev/commit/3c050e091dd177f582eddd0e9548db60efd02400))
- **doc:** init java living doc ([9a37dce](https://github.com/unit-mesh/auto-dev/commit/9a37dce7135fa118109b232ba847f9067826e3db))
- **doc:** make it works ([4d65801](https://github.com/unit-mesh/auto-dev/commit/4d658017d68da4215f536add4056a7f7703b5b5c))

## [1.1.1](https://github.com/unit-mesh/auto-dev/compare/v1.1.0...v[1.1.1]) (2023-08-08)

### Features

- add csharp mod ([0a49b52](https://github.com/unit-mesh/auto-dev/commit/0a49b52a1fe6a5c1bc6d0a6a8e40c23d1e036014))
- add csharp module ([e5bad15](https://github.com/unit-mesh/auto-dev/commit/e5bad154d612a18f1372de15ca4574d40b75acd8))
- add custom action to prompt ([d3ce0ed](https://github.com/unit-mesh/auto-dev/commit/d3ce0ed8bfef4d7498ac22e684094e26e41e6647))
- add match rule of regex ([58dca56](https://github.com/unit-mesh/auto-dev/commit/58dca56246b151811a2aea422e61285ef15e7134))
- add run rider ([4968c33](https://github.com/unit-mesh/auto-dev/commit/4968c3389aeebd41a8251c343c2f5f1f40654f97))
- add token length config to [#19](https://github.com/unit-mesh/auto-dev/issues/19) ([ba83c80](https://github.com/unit-mesh/auto-dev/commit/ba83c8010a27fbd486e941b1b55907d196581ca3))
- **biz:** make explain biz works ([6068a35](https://github.com/unit-mesh/auto-dev/commit/6068a357416f637c7934b2298ffd9b1322e6e7fd))
- **biz:** reorg code ([e72683b](https://github.com/unit-mesh/auto-dev/commit/e72683b58711d0261f8a5263d99188e8f03499bd))
- **biz:** update for context ([3f5b10a](https://github.com/unit-mesh/auto-dev/commit/3f5b10a4f0888b869821f478b28daa48abb7af3d))
- **co:** add basic api parse ([75e095d](https://github.com/unit-mesh/auto-dev/commit/75e095d271bd73fa9d759f5d5db37cce0e2ee242))
- **co:** add chapi for scan scanner ([cdeff14](https://github.com/unit-mesh/auto-dev/commit/cdeff14eb40a470f9629c14bc2c2d6c492af1b7f))
- **co:** init basic actions ([d9b0d08](https://github.com/unit-mesh/auto-dev/commit/d9b0d082441ec8f70dde3c75a66b434cf7cbf487))
- **co:** split to new module for plugin size ([c180711](https://github.com/unit-mesh/auto-dev/commit/c18071142f33bc6ccde02fc94632f28920fbe2a6))
- **custom:** add basic custom prompts to intentions ([9af81d0](https://github.com/unit-mesh/auto-dev/commit/9af81d03e0a6550ec85c4281f8964efb6160799f))
- **custom:** add basic variable ([9d7aef0](https://github.com/unit-mesh/auto-dev/commit/9d7aef08e68e1419c5d53c19e6ff42cf0defb5d8))
- **custom:** add intention prompt ([6791739](https://github.com/unit-mesh/auto-dev/commit/6791739185306b5be08922533c83b43aacf211fd))
- **custom:** enable for custom by varible ([a626347](https://github.com/unit-mesh/auto-dev/commit/a62634770ebe881369c28ee91ec4292255b25b83))
- **custom:** make custom prompt works ([71393df](https://github.com/unit-mesh/auto-dev/commit/71393df6c94111f95dd64392737931c1829954e4))
- make api works ([3bd2387](https://github.com/unit-mesh/auto-dev/commit/3bd2387d3824889d21dafe07c1afc2554063acc3))
- make azure works with stream ([962b599](https://github.com/unit-mesh/auto-dev/commit/962b599e91e078d866398a0ebe487b64d862a948))
- make custom server works with stream ([a7a9c1c](https://github.com/unit-mesh/auto-dev/commit/a7a9c1c418684105f8bbd198ac4b07fee346a67c))
- **python:** add to add collect ([b67933f](https://github.com/unit-mesh/auto-dev/commit/b67933f81f24b7dd082a4e33f37589f5c37d2b56))
- try with new api ([6a94505](https://github.com/unit-mesh/auto-dev/commit/6a945052ed89c43bc0ca337cf29467db3d7d1b8e))

### Bug Fixes

- **azure:** fix crash issues ([8d2581d](https://github.com/unit-mesh/auto-dev/commit/8d2581d377381e075cffa8546456f4c96a6f956f))
- disable for veww ([8bb2063](https://github.com/unit-mesh/auto-dev/commit/8bb20630ad5c8fa266a37bf56e20e5ddbb8f5f4e))
- disable for welcome board ([7ab469b](https://github.com/unit-mesh/auto-dev/commit/7ab469b0c617adcffceb329c75ff8b9105687024))
- fix exception issues ([2fa1976](https://github.com/unit-mesh/auto-dev/commit/2fa19766cabeca08f087c485c58ea886f4a83e49))
- fix format issues ([f83b70f](https://github.com/unit-mesh/auto-dev/commit/f83b70fc3fc7c9771dfd022860b3f5cb858c28dc))
- fix lost element issue ([3dd043e](https://github.com/unit-mesh/auto-dev/commit/3dd043e334db3e6595fef97a5fad12ffdb953ee2))
- fix null issue [#19](https://github.com/unit-mesh/auto-dev/issues/19) [#14](https://github.com/unit-mesh/auto-dev/issues/14) ([1fe63fe](https://github.com/unit-mesh/auto-dev/commit/1fe63fe94364e9c434ad661bf6e228174adba7cf))
- fix tests ([ee67cde](https://github.com/unit-mesh/auto-dev/commit/ee67cde53356947d5c46e4e6833db8a5043ebd2e))
- fix width issue ([b94ccc0](https://github.com/unit-mesh/auto-dev/commit/b94ccc0ca443737d54fe31b5b1bcb389d1c1e02a))
- remove dpilicate code ([1a89ed0](https://github.com/unit-mesh/auto-dev/commit/1a89ed03fb4dd22678f25f6f3c66122de1204f66))
- settings not change but show modified ([e3e327d](https://github.com/unit-mesh/auto-dev/commit/e3e327d6b92008de73312106ae2cefdd769aa698))
- try add csharp framework context ([d2e7bfe](https://github.com/unit-mesh/auto-dev/commit/d2e7bfe7423a6824903b5690e591458c5c878563))

## [1.0.2](https://github.com/unit-mesh/auto-dev/compare/v[1.0.2]-beta.1...v[1.0.2]) (2023-08-07)

### Bug Fixes

- clen deps ([0625702](https://github.com/unit-mesh/auto-dev/commit/062570212a849c292aff0c72d3d2972d023fdc88))
- deplay get display text message time ([46a24e8](https://github.com/unit-mesh/auto-dev/commit/46a24e8ae4b16636937e8917c3c43ab8f29ef1da))
- fix a read issue before other ([5142ec2](https://github.com/unit-mesh/auto-dev/commit/5142ec2cb2ca3ed562baa122ec1b930ddd1d4081))
- fix change sessions ([3ce2a57](https://github.com/unit-mesh/auto-dev/commit/3ce2a575b2a5c01b99808e44038e8cf358aa7ca2))
- fix examples for commits ([469624a](https://github.com/unit-mesh/auto-dev/commit/469624af173055b25a2ad079d80d91f68b5e5038))
- fix format ([0e61d89](https://github.com/unit-mesh/auto-dev/commit/0e61d8998175576fc0b21dd640bad90062bbbff6))
- fix internal api issues ([16449c2](https://github.com/unit-mesh/auto-dev/commit/16449c290cbd4b591ec0c766937be0ad7f663297))
- fix processor issue ([a8fc892](https://github.com/unit-mesh/auto-dev/commit/a8fc892daf26bef9d22ee609ea1be829410d613c))
- fix read issues ([a318041](https://github.com/unit-mesh/auto-dev/commit/a31804159bd88999294ab3d9a1c5712e95c23f9e))
- fix request prompt issue ([08036ac](https://github.com/unit-mesh/auto-dev/commit/08036ac7396c88672a3d672848ace10033b59f2e))
- fix simliar chunk issue ([6b79923](https://github.com/unit-mesh/auto-dev/commit/6b799231dbbaf3fd0a28d8c2dafce775c1478328))
- fix tests ([de6bb26](https://github.com/unit-mesh/auto-dev/commit/de6bb26c845fa26249bf72c2ab7257e603d90ce9))
- fix typos ([109db5e](https://github.com/unit-mesh/auto-dev/commit/109db5e5573c9fd6b45fc2e31c94c293399e10a4))
- try to resolve index issues ([25b2275](https://github.com/unit-mesh/auto-dev/commit/25b2275a4c0d3a7099aafd35aeb24dd15f03664a))

### Features

- add basic for context ([5e6af8e](https://github.com/unit-mesh/auto-dev/commit/5e6af8ee5974c27244c2ee70d8e63b669daa2b9d))
- add basic rust for template ([d892646](https://github.com/unit-mesh/auto-dev/commit/d8926462594b8c66ed8c848d59cb9c4a109eb50f))
- **biz:** add chat with biz ([6723920](https://github.com/unit-mesh/auto-dev/commit/6723920fcb9cc5adb724a6d429efd0ed53c54abd))
- **clion:** try with oc ([4593cb8](https://github.com/unit-mesh/auto-dev/commit/4593cb88b2fa1467b55620438c17fa0b84763c13))
- make clion runnable ([71df852](https://github.com/unit-mesh/auto-dev/commit/71df852c80844a3b3df09521c26c3f371bb5de7c))
- **rust:** try to talk with biz in biz ([d138da7](https://github.com/unit-mesh/auto-dev/commit/d138da73d7b2e21fdfbf08466031e27b1404865e))
- try to use refs ([ce3c671](https://github.com/unit-mesh/auto-dev/commit/ce3c67186ce50f847e968455d14e4ec4f3bcea70))
- update for explain biz logic ([95049b5](https://github.com/unit-mesh/auto-dev/commit/95049b5ebe4b71093e6256fdaa65efe266a54900))

## [1.0.2-beta.1](https://github.com/unit-mesh/auto-dev/compare/[1.0.2-beta.1]...v[1.0.2-beta.1]) (2023-08-05)

### Bug Fixes

- fix issues of internal API ([d3e56ab](https://github.com/unit-mesh/auto-dev/commit/d3e56ab7d9720281938a06a0f1178d19f3427dc1))
- **ui:** fix genearete code waring ([6955882](https://github.com/unit-mesh/auto-dev/commit/6955882b49b85f7201ed7ab9a829c2dc9a9a1fd0))
- **ui:** fix magic number ([8554153](https://github.com/unit-mesh/auto-dev/commit/855415349f488b23d2cfc3c221c566a9e832e9fe))

### Features

- add welcome page ([3dc5c19](https://github.com/unit-mesh/auto-dev/commit/3dc5c19686d6958be977203af9a6f399c1e7676c))
- init for chat messages ([fd139df](https://github.com/unit-mesh/auto-dev/commit/fd139df66c88f3698d705f7f8121f78ed48b318d))
- make genearte code here ([3bce491](https://github.com/unit-mesh/auto-dev/commit/3bce4914198e7cc942f8519fed761523356003e7))
- **ui:** thinking in generate in placE ([ec4563d](https://github.com/unit-mesh/auto-dev/commit/ec4563dc7fe601eadc20c9f5a97a2d2d26cdff49))

## [1.0.1](https://github.com/unit-mesh/auto-dev/compare/v1.0.0...v[1.0.1]) (2023-08-05)

### Bug Fixes

- add lost group id ([99c55c7](https://github.com/unit-mesh/auto-dev/commit/99c55c7e94ec0811ebdf63e966cfbf5ee2778e03))
- fix align issues ([580be9d](https://github.com/unit-mesh/auto-dev/commit/580be9d9cb5bbb20dc64c582ddd99e52429a2ec3))
- fix code complete issue ([d4764d2](https://github.com/unit-mesh/auto-dev/commit/d4764d2146ee5710c924bf2193008ef8cf83d53f))
- fix ddl drop issue ([37809f3](https://github.com/unit-mesh/auto-dev/commit/37809f33c2e3b4549d3db0ae18d275b307431922))
- fix dispose issues ([e03ebc7](https://github.com/unit-mesh/auto-dev/commit/e03ebc7432db8386729733020ba76305fa604622))
- fix document length issues ([2a0a65b](https://github.com/unit-mesh/auto-dev/commit/2a0a65bcfe43d88bf5bf330d168540b433274f61))
- fix dumb issues ([5d19ce6](https://github.com/unit-mesh/auto-dev/commit/5d19ce6ffdf7e68c22a4f03b72f7f98e107713b6))
- fix error level issues ([02f905b](https://github.com/unit-mesh/auto-dev/commit/02f905b445356fbc6b6c2ec66b6b60a1f5cf499e))
- fix file lost contexT ([9970bdc](https://github.com/unit-mesh/auto-dev/commit/9970bdcfdb93ebc4e43a3bd7126efa5ca2373456))
- fix java code issues ([042d078](https://github.com/unit-mesh/auto-dev/commit/042d07860241af64c222641d9fa340efe1ad5282))
- fix mvc issues ([8c9fec0](https://github.com/unit-mesh/auto-dev/commit/8c9fec0d9fcdf48ba29dbc54b718bae4d8a43c7e))
- fix naming issue ([8bff8fc](https://github.com/unit-mesh/auto-dev/commit/8bff8fc686eef747907882a0e666beb24476c377))
- fix new line issues ([ea74c90](https://github.com/unit-mesh/auto-dev/commit/ea74c90de1a8bc5ce826fe2662f7ae40f332d9df))
- fix no project issues ([073e279](https://github.com/unit-mesh/auto-dev/commit/073e2794c6407c20ecefc6a5b0b1554b59670769))
- fix prompt issues ([dda3586](https://github.com/unit-mesh/auto-dev/commit/dda3586415fd35143089abb5088ee9b3de367b58))
- fix renam issues ([aaa097d](https://github.com/unit-mesh/auto-dev/commit/aaa097d376dfff2ed9f9a00c339a7ba476d6b51a))
- fix runtime issues ([e318210](https://github.com/unit-mesh/auto-dev/commit/e318210658dc31cfe3045082cc75e0dfae361ccb))
- fix setContent not input issues ([efc14c1](https://github.com/unit-mesh/auto-dev/commit/efc14c10ad4a498a3576937aff3d0de82b00ccab))
- fix sometimes offsets issues ([90e4055](https://github.com/unit-mesh/auto-dev/commit/90e4055a20e3e4e8c545a8acbc72b082e87b635a))
- fix tests ([48e1f26](https://github.com/unit-mesh/auto-dev/commit/48e1f26f979ec063fad1074257e21ab45c86988c))
- fix tests ([f6c13c6](https://github.com/unit-mesh/auto-dev/commit/f6c13c61559d18b9a255cc82736228906eb63bac))
- fix tests ([a75481a](https://github.com/unit-mesh/auto-dev/commit/a75481acf05e626a049a775594487eed5a72f616))
- fix typos ([f64f748](https://github.com/unit-mesh/auto-dev/commit/f64f748a0aeb1994fba0e3489adaaade1fc60b14))
- fix typos ([cc8cf15](https://github.com/unit-mesh/auto-dev/commit/cc8cf15d4d1045f9892f668e7b95451b8eab6351))
- fix typos ([b974965](https://github.com/unit-mesh/auto-dev/commit/b97496504bf02f0d5d5fc96b740316ecb681fa4b))
- fix typos ([0b2d4e1](https://github.com/unit-mesh/auto-dev/commit/0b2d4e1e589fd580e443c685419a11828e0dc20f))
- fix typos ([3cfcc0a](https://github.com/unit-mesh/auto-dev/commit/3cfcc0afbfd1c400661fe59764fdb52fac263bd7))
- fix typos ([6320e1a](https://github.com/unit-mesh/auto-dev/commit/6320e1a3810ee655fcf4a268dfaed9615a29e8f4))
- fix typos ([d6a9522](https://github.com/unit-mesh/auto-dev/commit/d6a9522555a6924d976bb92a3420d11803593b53))
- fix uppercase issues ([5ae6f7b](https://github.com/unit-mesh/auto-dev/commit/5ae6f7beff71d4629443c1344b454c07734c7608))
- fix warning ([3d20f9f](https://github.com/unit-mesh/auto-dev/commit/3d20f9f9e4800e04cc1948eaec23a701c68fe0db))
- fix when controllers empty ([6baa6b8](https://github.com/unit-mesh/auto-dev/commit/6baa6b8d3c3f09a951c91f9df9a8be2ad86ee37e))
- **go:** disable version for temp ([cd1a46c](https://github.com/unit-mesh/auto-dev/commit/cd1a46c11cbb089a52b7e5d3a1cd7618555eb6e8))
- **js:** fix context issues ([ae19ff4](https://github.com/unit-mesh/auto-dev/commit/ae19ff4ae391ea3139ba60e97ed25e6a261fefcc))
- **js:** fix lost tsx syntax error issue ([e42bc96](https://github.com/unit-mesh/auto-dev/commit/e42bc96b1c5d466ac9e5eb25c79f8eaae07a3350))
- **js:** fix test framework ([58f4ef4](https://github.com/unit-mesh/auto-dev/commit/58f4ef4a70b2ba6decf52415b45f62557b2aecb4))
- **test:** fix for root element issue ([f85b9b3](https://github.com/unit-mesh/auto-dev/commit/f85b9b360148d0fca31d028b0fda018766910bd2))
- **test:** make it pass ([8e2dd9d](https://github.com/unit-mesh/auto-dev/commit/8e2dd9d699923039e1889014540db1911f0a55ce))
- try to fix for auto crud issues ([22828b6](https://github.com/unit-mesh/auto-dev/commit/22828b63b91867e4996b2f0dfa90b4a835fb8d34))
- try to reolsve psi class ref type ([2112b91](https://github.com/unit-mesh/auto-dev/commit/2112b9146ce5c4134c36ac4087b5c06aa52169d5))
- try to resolve editor issues ([76b698c](https://github.com/unit-mesh/auto-dev/commit/76b698c8edc5263a6a12aa57c2402aa6853efe8e))
- try to resolve for file ([8cc1c7f](https://github.com/unit-mesh/auto-dev/commit/8cc1c7fd833258b1c09c50d2e87e2c550d3cb67f))
- update for deps ([ed73604](https://github.com/unit-mesh/auto-dev/commit/ed73604eb33912d21afb15ddb26c3fba10b6a97d))

### Features

- add basic handle for cache ([aee8504](https://github.com/unit-mesh/auto-dev/commit/aee8504d921c3a2ccd626077e79aa128a0b6dcee))
- add chat with this ([ed44a9d](https://github.com/unit-mesh/auto-dev/commit/ed44a9d9262a41a32d0c0d6d4941b659c221918c))
- add context creator ([ccf3939](https://github.com/unit-mesh/auto-dev/commit/ccf39398f7f340c8252f0c9500cf7a7c9ccb3f68))
- add custom llm server example ([8d4373a](https://github.com/unit-mesh/auto-dev/commit/8d4373a956af21ed4a758dce29b892b7771c060f))
- add custom server example ([7766320](https://github.com/unit-mesh/auto-dev/commit/7766320c1f0048ab2140c8d377ca18fc0dbac761))
- add custom server to server ([f175368](https://github.com/unit-mesh/auto-dev/commit/f1753686dc52fccef99834440cc674a3809dcb03))
- add for context ([1af9ccb](https://github.com/unit-mesh/auto-dev/commit/1af9ccb787d091b58214e0dd89ac71331c296a56))
- add for mock provider ([c355211](https://github.com/unit-mesh/auto-dev/commit/c35521154e5e64b55b404815cc203f6d9d1653fe))
- add for more contents ([2819b8a](https://github.com/unit-mesh/auto-dev/commit/2819b8a8641ff05425ee91f7b0612cc38f72ba64))
- add for new code block ([4f9f19d](https://github.com/unit-mesh/auto-dev/commit/4f9f19dbce0f41361e9b9224d2cb042c09181e31))
- add for write test service ([993ec10](https://github.com/unit-mesh/auto-dev/commit/993ec108e60c19a769f92d036b8ab507694248dc))
- add get for explain element ([0363e8c](https://github.com/unit-mesh/auto-dev/commit/0363e8c4722830bdf1c1a2765e9bebf9029f5163))
- add good ui with sender ([4874767](https://github.com/unit-mesh/auto-dev/commit/48747676db8ddcc608ed32ab359407f424d91f76))
- add group for intetion ([a819cfa](https://github.com/unit-mesh/auto-dev/commit/a819cfa2ed7f07db37874883f637b657e1e74233))
- add language toolbar ([e1b5f8d](https://github.com/unit-mesh/auto-dev/commit/e1b5f8da5e57ffd365d33ea3557052f40528e83f))
- add new chat with selectiongst ([164f848](https://github.com/unit-mesh/auto-dev/commit/164f8480cf95126b7ad20e692b8d7cbef2163b34))
- add run after finish testing ([9ac7890](https://github.com/unit-mesh/auto-dev/commit/9ac78908859a4eba5f28ca34c08c29022a22b79a))
- add simple rule for writting test ([5f38256](https://github.com/unit-mesh/auto-dev/commit/5f38256b088952237d5257be8dad37e49eea9377))
- add suffix for items ([fa05a0e](https://github.com/unit-mesh/auto-dev/commit/fa05a0efc93e4f4331208fa18a65ce62adaeaf2b))
- align for input ui ([7543b15](https://github.com/unit-mesh/auto-dev/commit/7543b1552158611dc512feb4dd43fadfb044e44f))
- **context:** try for new provder ([b87af78](https://github.com/unit-mesh/auto-dev/commit/b87af785efac6058ca27c3499ac36f616331dd2a))
- **context:** update for context ([0182c29](https://github.com/unit-mesh/auto-dev/commit/0182c29a17138fcc17d384e0aeaa751cabd70fa2))
- enable autocrud for [#13](https://github.com/unit-mesh/auto-dev/issues/13) ([eb38aaa](https://github.com/unit-mesh/auto-dev/commit/eb38aaa39e6be6939faa1c1723b3db4112daf55f))
- enable context for full classes ([62963e1](https://github.com/unit-mesh/auto-dev/commit/62963e11b3bad6d5d171f957b5cafb17c5226e8f))
- fix context mpety issues ([2fd0695](https://github.com/unit-mesh/auto-dev/commit/2fd06954ac5801994cb051e46d8d6dc1b142750f))
- fix duplicate context issues ([a951ed6](https://github.com/unit-mesh/auto-dev/commit/a951ed6400dfc3a2f7def781bc67dd3f63883b70))
- fix duplicate warning ([85c9d1a](https://github.com/unit-mesh/auto-dev/commit/85c9d1a3ccb877421b4fd132520a267d89f33262))
- init basic js context provider ([5164da0](https://github.com/unit-mesh/auto-dev/commit/5164da09b9906ad21a7814a0f1c6f031da763cbc))
- init basic transform logic ([8262f9b](https://github.com/unit-mesh/auto-dev/commit/8262f9bbc702718c26e2c473230f5b40af72ada2))
- init for package json reader ([a1b5c45](https://github.com/unit-mesh/auto-dev/commit/a1b5c45cd3045e425dd4c6ed24a4485c11472c8a))
- init frameworks informationg ([d268842](https://github.com/unit-mesh/auto-dev/commit/d26884208f27e6a73cbf00bffeb7a156aec03225))
- init goland mods ([880d12d](https://github.com/unit-mesh/auto-dev/commit/880d12d5660c1e1e4b0ae5a1d29e7fc1576f17ce))
- **java:** inline duplicate code ([77232a2](https://github.com/unit-mesh/auto-dev/commit/77232a283101c907ba44afc2c9059c933b3ec59f))
- **javascript:** fix roor package.json error issue ([1d5553a](https://github.com/unit-mesh/auto-dev/commit/1d5553a2228d08cb3c02225ed8cb03b669a8d015))
- **js:** align context builder ([c9ea1d1](https://github.com/unit-mesh/auto-dev/commit/c9ea1d17f539f6c422b4ca015feccc6f79039d7f))
- **js:** disable for insert class ([6fceaf1](https://github.com/unit-mesh/auto-dev/commit/6fceaf18662401e203983eebe048f131fb2d1905))
- **js:** init snapshot util ([447e6d7](https://github.com/unit-mesh/auto-dev/commit/447e6d768e3455f1667a4e99a7f9d5cd749046db))
- **js:** init test service container ([750baff](https://github.com/unit-mesh/auto-dev/commit/750baff5fbe790a1991a832094f887c6e90bc521))
- **js:** make test sowrks ([1989ba1](https://github.com/unit-mesh/auto-dev/commit/1989ba17fdf520566310851d4772249ab0fbc4a6))
- **js:** update for libs ([508a990](https://github.com/unit-mesh/auto-dev/commit/508a990d266d6636f84d315c1dbedd0033a4b7ca))
- **js:** update for test framework ([380ad76](https://github.com/unit-mesh/auto-dev/commit/380ad76b548d990dd0ef924f13d9837763ad092a))
- **kotlin:** add better support for it ([044a30c](https://github.com/unit-mesh/auto-dev/commit/044a30cb8ac10522e3ec426e63db3aba774838ba))
- **kotlin:** add error handle foreditor ([3b5793f](https://github.com/unit-mesh/auto-dev/commit/3b5793fb6c38411158e9812b9a0341a01793e4da))
- **kotlin:** add model gpt-3.5-turbo-16k this is an enhanced model with an increased token limit of 16k, which is more advantageous for handling large document texts ([b6354b6](https://github.com/unit-mesh/auto-dev/commit/b6354b6a1fcfec416eb2d5e767c6c061a71d4493))
- **kotlin:** add support for code ([34ef517](https://github.com/unit-mesh/auto-dev/commit/34ef5179329ee0fe09900366d97beb29e17b9759))
- **kotlin:** init code modifier ([e3a2738](https://github.com/unit-mesh/auto-dev/commit/e3a2738ace7f09fd6137da8b616af46bab40133f))
- **kotlin:** init for kotlin code ([624fd47](https://github.com/unit-mesh/auto-dev/commit/624fd47d73041028e3047e523ffc6acfc02ef770))
- **kotlin:** init inserter api ([9c7086a](https://github.com/unit-mesh/auto-dev/commit/9c7086a0aec14fb4f7572b6e77ba20687c6ccdc7))
- **kotlin:** make context works ([4efe9ba](https://github.com/unit-mesh/auto-dev/commit/4efe9ba45405ff286a0c164003a3d7a792180bc5))
- **kotlin:** make kotlin classes works ([aff005c](https://github.com/unit-mesh/auto-dev/commit/aff005cc3fbd208e1f2c236e46655a5df9198f52))
- **kotlin:** refactor FileContext to ClassContext ([3f20b36](https://github.com/unit-mesh/auto-dev/commit/3f20b36b2dfaba809554b3593e817e3ebfb12d61))
- make it works ([ff97dd8](https://github.com/unit-mesh/auto-dev/commit/ff97dd8a72b6cc9334e4844ee3560378abd6a7e7))
- move up related classes ([ccc6b31](https://github.com/unit-mesh/auto-dev/commit/ccc6b31b1881881fd3d45382307c150a922cac57))
- rename input ([36a9587](https://github.com/unit-mesh/auto-dev/commit/36a95874b08cd517b847ba521ff353cc0c490179))
- **test:** add for create dir ([62111fa](https://github.com/unit-mesh/auto-dev/commit/62111fa3610add08769ef7b5972ab9f44ab9c21f))
- **test:** auto insert to code ([d48e704](https://github.com/unit-mesh/auto-dev/commit/d48e704eb427faad86226272174e90df6644550c))
- **test:** make it works ([fe3cecc](https://github.com/unit-mesh/auto-dev/commit/fe3ceccb47a974485ace83aaa1a446af82e0a940))
- **test:** make model related ([8ddf6a0](https://github.com/unit-mesh/auto-dev/commit/8ddf6a0efb9c5c05c6e8eb53853c723773c9bf86))
- **test:** make not crash ([b610e6b](https://github.com/unit-mesh/auto-dev/commit/b610e6bab1bf46e0636c32e3228cc04e1a9fda61))
- **test:** make test file to project ([628c6ee](https://github.com/unit-mesh/auto-dev/commit/628c6ee8158568cb888fb2d22e36fd099f8f9756))
- **test:** set for indicator ([c5f4a8f](https://github.com/unit-mesh/auto-dev/commit/c5f4a8f7f6fa8c243de4a5a3cabc7aa2ce213ce5))
- **test:** update for sample ([636af2b](https://github.com/unit-mesh/auto-dev/commit/636af2b58e2be0c9156770489144fd40a15f90cf))
- **test:** update samples ([6270725](https://github.com/unit-mesh/auto-dev/commit/627072546b85c62247334154d985639cbd9387b8))
- thinking in bettwer title ([f709844](https://github.com/unit-mesh/auto-dev/commit/f7098444fccac2eac2d56573fb530380f9546e11))
- try to add validator ([459b711](https://github.com/unit-mesh/auto-dev/commit/459b71137ff1241b5d4212fe15f7c2bf43b3fb9c))
- try to resolve for coce ([77d3080](https://github.com/unit-mesh/auto-dev/commit/77d30807df2241a038f74ff582fb86171e90dac2))
- try to use bundle key ([11f277e](https://github.com/unit-mesh/auto-dev/commit/11f277e078ae8b20adbae40a231bb407a11a4a87))
- udpate for actions ([947ffbe](https://github.com/unit-mesh/auto-dev/commit/947ffbec5cc5e37f634ab7f4725554a11afb64ad))
- udpate for roles ([f8903f9](https://github.com/unit-mesh/auto-dev/commit/f8903f9957bdc21b4d31d24ef8ce09d098c21050))
- **ui:** add copy to toolbar ([77d62d2](https://github.com/unit-mesh/auto-dev/commit/77d62d22d11ce477e90a3071ba4e3d241c1755fc))
- **ui:** add foundation for message block ([f1125bc](https://github.com/unit-mesh/auto-dev/commit/f1125bca1dbb4409559fecc590e96dd684994fb7))
- **ui:** add handle for enter with new line ([7466800](https://github.com/unit-mesh/auto-dev/commit/746680073f258700a9f44d06abdb45895406953a))
- **ui:** add i18n to send response ([6aa46db](https://github.com/unit-mesh/auto-dev/commit/6aa46db07afdb0f0aaf9125d00627632ad588d26))
- **ui:** change view part ([4b70ad6](https://github.com/unit-mesh/auto-dev/commit/4b70ad608c2d529651ec8d2859d08d0c87e47bf4))
- **ui:** etract for new method ([51342a2](https://github.com/unit-mesh/auto-dev/commit/51342a26c9ffaca625e5d8e18343cefd3f7c0083))
- **ui:** extract for new ui ([86a5c72](https://github.com/unit-mesh/auto-dev/commit/86a5c7253ea8a883ce85c4e623cfb923de13af2d))
- **ui:** format ([3cbdc70](https://github.com/unit-mesh/auto-dev/commit/3cbdc700a7ad232459e99c7150fcb228f4b240a0))
- **ui:** init for like and dislike ([07301b2](https://github.com/unit-mesh/auto-dev/commit/07301b21b71e873c5acfb15f79366b7de8e83e1b))
- **ui:** init for part message ([0af1217](https://github.com/unit-mesh/auto-dev/commit/0af121716b93d79312e77ced527cc098051d9c19))
- **ui:** make chat works ([61d822d](https://github.com/unit-mesh/auto-dev/commit/61d822d6b080e62c1e10b2d3f1d06b3aeff81d39))
- **ui:** make it works ([7f882a3](https://github.com/unit-mesh/auto-dev/commit/7f882a3f6adff0b169c518d3c30ba5846764efa1))
- **ui:** make new content builder works ([0b7499c](https://github.com/unit-mesh/auto-dev/commit/0b7499c0c272c496fef8ddf566a874e5833cd100))
- **ui:** make panel works ([cf21878](https://github.com/unit-mesh/auto-dev/commit/cf218787e42a63dcdc08323ee0f8b11ff0a1ad1c))
- **ui:** render view after dispatch ([056d643](https://github.com/unit-mesh/auto-dev/commit/056d643604c7360e1d975c8262b4aff87a8cc19e))
- **ui:** update for bottom ([00cc025](https://github.com/unit-mesh/auto-dev/commit/00cc025d34c8c6e9e192a368b543c44c049f63b9))
- **ui:** update for display text ([c7e6e74](https://github.com/unit-mesh/auto-dev/commit/c7e6e74663e2515cbb94645416f19588918fc3b9))
- update for context ([d79a6f6](https://github.com/unit-mesh/auto-dev/commit/d79a6f63190e546c9e2700049a07a66f2aacbc19))
- update for js context ([4c2d883](https://github.com/unit-mesh/auto-dev/commit/4c2d883997f22b6756eedc97ce56aa40756351fd))
- update for libs ([0d68503](https://github.com/unit-mesh/auto-dev/commit/0d68503982d7c10b50a414d266fc9caa1ad7ca49))
- update for promter settings ([560b5e3](https://github.com/unit-mesh/auto-dev/commit/560b5e34fa3d45858fb55f53fba9109545520e45))
- update for samples ([ecffe36](https://github.com/unit-mesh/auto-dev/commit/ecffe365ec0ef613f6f3c22793d681480a683553))
- update for test code ([fdd3943](https://github.com/unit-mesh/auto-dev/commit/fdd3943b0312f426ddbdb5a7cd4d0b0f4bc43aee))

## [0.7.3](https://github.com/unit-mesh/auto-dev/compare/v0.6.1...v[0.7.3]) (2023-07-25)

### Bug Fixes

- add try catch for chunk ([2ff3a63](https://github.com/unit-mesh/auto-dev/commit/2ff3a633ec54063eda2260e234b4623db4a536a8))
- disable plugin verifier ([a4bcd00](https://github.com/unit-mesh/auto-dev/commit/a4bcd000bc8f755c5ca5701991b1b94867883a99))
- fix action group position issues ([c505638](https://github.com/unit-mesh/auto-dev/commit/c5056389ba486193fd419956a22d736ed4431811))
- fix base issues ([c6574d2](https://github.com/unit-mesh/auto-dev/commit/c6574d2a87723767c232092d6296252e12feda79))
- fix comment code ([f46ac9f](https://github.com/unit-mesh/auto-dev/commit/f46ac9f97436cd0f2ed201ffae7ac8b2cd55f85b))
- fix confilic ([2358565](https://github.com/unit-mesh/auto-dev/commit/2358565a0efa28524c829193ed2090b304fd5bcb))
- fix crash ([003b648](https://github.com/unit-mesh/auto-dev/commit/003b648494e9c86a35ade50950ab04738718c8f8))
- fix duplicated ([164efc8](https://github.com/unit-mesh/auto-dev/commit/164efc88a6d0dbeae9a0e57d10617f9c4bcf5c9c))
- fix indent ([b53ec47](https://github.com/unit-mesh/auto-dev/commit/b53ec479483a05cdeaa9d1c14066b9bb731a67a0))
- fix java langauge error issue ([9d2d4d5](https://github.com/unit-mesh/auto-dev/commit/9d2d4d552575225ebeadd47dd3ea72be9d19e5a5))
- fix not class issuesg ([3403ee8](https://github.com/unit-mesh/auto-dev/commit/3403ee84e6bd9c4cf4979cbd9cc7fbd109938c24))
- fix not controller has simliary chunk issues ([905306a](https://github.com/unit-mesh/auto-dev/commit/905306ae4953284447c17ceb2b57a3750a658ab6))
- fix null issues ([9b4677b](https://github.com/unit-mesh/auto-dev/commit/9b4677bfd8f58fb07d0ec1296af5d0d9b33ad861))
- fix null issues ([a1edce8](https://github.com/unit-mesh/auto-dev/commit/a1edce8be6e40ed98e8bcc6f86370420104ee4a2))
- fix path issues ([c6a87c2](https://github.com/unit-mesh/auto-dev/commit/c6a87c2bdfb580b7562306a22042355e1f9222e4))
- fix plugin duplicated issue ([f797a1c](https://github.com/unit-mesh/auto-dev/commit/f797a1c2df70a90da934c7e92c08cd6d399373f3))
- fix refactor issues ([5264de7](https://github.com/unit-mesh/auto-dev/commit/5264de7fbb271ab674033a59fffe296fce730f64))
- fix services error issues ([e8fccbb](https://github.com/unit-mesh/auto-dev/commit/e8fccbb35ab1b4f7b71c988972b3cf39b7dc2448))
- fix some cofings ([59795e2](https://github.com/unit-mesh/auto-dev/commit/59795e2a6531fa1dd628058720990bda3431a85e))
- fix t ypos ([ff0c12a](https://github.com/unit-mesh/auto-dev/commit/ff0c12a5676906036836a097fe69b5f8c89dd1b8))
- fix tests ([0754581](https://github.com/unit-mesh/auto-dev/commit/0754581db50e309cc020c774631e42ad5651c5bf))
- fix warning ([0237d4a](https://github.com/unit-mesh/auto-dev/commit/0237d4ada11a863f9f913cf7fb1ff6351f925266))
- **python:** fix runtime issues ([f32ab66](https://github.com/unit-mesh/auto-dev/commit/f32ab66d678b0a51ac6452734996f9312e0b96e4))
- remove some librs to idea only [#7](https://github.com/unit-mesh/auto-dev/issues/7) ([8cf9839](https://github.com/unit-mesh/auto-dev/commit/8cf983974f2762789c7c89603353f95c80dac1d1))
- set default layer name ([e88608d](https://github.com/unit-mesh/auto-dev/commit/e88608d77e44c9dc37004f60d75417ea7292d175))
- try to make test case ([e8a5d15](https://github.com/unit-mesh/auto-dev/commit/e8a5d152537e57634f1d1487a6a5022c09455676))
- update for config ([074df5c](https://github.com/unit-mesh/auto-dev/commit/074df5cd4ad6645c6ef42fa63e9ae6a4b32e854e))

### Features

- add first inline ([efe921a](https://github.com/unit-mesh/auto-dev/commit/efe921abc64b86a276cb3efa99b004eda69cb6c7))
- add for hucnk prompt ([73462bb](https://github.com/unit-mesh/auto-dev/commit/73462bb57b21f31da2856c662aff815e8725d3e1))
- add format code ([961b4df](https://github.com/unit-mesh/auto-dev/commit/961b4dfaa8e9140965185fe7a25c309fa671b7ab))
- add offset for handle prompt text ([5bdf188](https://github.com/unit-mesh/auto-dev/commit/5bdf188dd5713bf170473d067354cdd55bfaff88))
- align find issue to JetBrains ([8159a52](https://github.com/unit-mesh/auto-dev/commit/8159a5248b45c6d1a69834f27f7787e5d9a69734))
- align kotlin context ([b9bcbd9](https://github.com/unit-mesh/auto-dev/commit/b9bcbd9e81b9293eda9195d68a6e82ea3b29f05f))
- **changelog:** init for action group ([37841c0](https://github.com/unit-mesh/auto-dev/commit/37841c021f15c71b607d436589bc2b29019a169d))
- **context:** fix compile issue ([1e76a2f](https://github.com/unit-mesh/auto-dev/commit/1e76a2f7ee85fc3c501769805190323e75867619))
- **context:** redesign to uml method for autodev ([00afd26](https://github.com/unit-mesh/auto-dev/commit/00afd2617d609ed16d6e98ebd53727f66c3902cd))
- fix fix tests ([7b66dcd](https://github.com/unit-mesh/auto-dev/commit/7b66dcd138352312f488ab12b3000bb032428f83))
- fix no prompter issues ([e667582](https://github.com/unit-mesh/auto-dev/commit/e6675828d087f0997b63e721976340f4ab606f8f))
- init basic python prompter for context ([ef8d32f](https://github.com/unit-mesh/auto-dev/commit/ef8d32f731eade90e1dce3248c66a0ec145ffc17))
- init builder ([0466e44](https://github.com/unit-mesh/auto-dev/commit/0466e443e29b344fc30de2023a778ee9350b74a3))
- init first code completion intension ([420a0ba](https://github.com/unit-mesh/auto-dev/commit/420a0ba26123ecc8c1e532e4ada8078d5ae8e8e5))
- init for kotlin package ([f999196](https://github.com/unit-mesh/auto-dev/commit/f999196071a48a5a68a2b42b346b7ebcb6c2590a))
- init js class loader ([04a785b](https://github.com/unit-mesh/auto-dev/commit/04a785b07e55224c0cfc70ad37a0ed0514e2f856))
- init llm inlay manager ([5434b42](https://github.com/unit-mesh/auto-dev/commit/5434b4290d3735f32a86d31e9dff44611c9dc156))
- init plugin modules for like clinon ([6179197](https://github.com/unit-mesh/auto-dev/commit/61791972c8f8d3f1e5d4760874129bcfb60157cf))
- init python plugins ([4f75e6b](https://github.com/unit-mesh/auto-dev/commit/4f75e6ba3ad58ce420207e2ee2a3ab93c5f07c42))
- init save and format code ([1e34279](https://github.com/unit-mesh/auto-dev/commit/1e342797e0b2cf99ca241de87ba10724eb85d8a9))
- init some basic methods ([fa9f250](https://github.com/unit-mesh/auto-dev/commit/fa9f250ee48f1d49efb8fd3c1d8bbaf65d41b613))
- init text attributes ([042806e](https://github.com/unit-mesh/auto-dev/commit/042806ecb979914d60f6a4c66fe1590e0a0512ab))
- **inlay:** add autodev core block ([61a7bc8](https://github.com/unit-mesh/auto-dev/commit/61a7bc87a8f596af91f3ad0de53a9c976d25f837))
- **inlay:** add simple editor modified ([88d74e8](https://github.com/unit-mesh/auto-dev/commit/88d74e8ef3da47f7373993cff25f31b097e2c9ca))
- **inlay:** init command listener ([e5c42bd](https://github.com/unit-mesh/auto-dev/commit/e5c42bd9d007345b5c41969857e3348807a460d1))
- **inlay:** make for inserts ([80b06af](https://github.com/unit-mesh/auto-dev/commit/80b06af7924056a3f5a1cd32d9b1895e8b8e1135))
- **inlay:** try to release ([89eb4d5](https://github.com/unit-mesh/auto-dev/commit/89eb4d5299f33d9cec35b3b48702cda5cfb24014))
- **inlay:** try to set for listener ([93c724f](https://github.com/unit-mesh/auto-dev/commit/93c724f88b095a79f1198ffc50f451fbf38d5de2))
- **inlay:** use command listener ([fa46000](https://github.com/unit-mesh/auto-dev/commit/fa4600046b2ba07f8a0d7475d2644980f2308673))
- **inlay:** use inlay mode for code completion ([aa230c7](https://github.com/unit-mesh/auto-dev/commit/aa230c74ee9a69e5e790cc1a44efcbf76e6a395b))
- **js:** init advice ([ee2b14a](https://github.com/unit-mesh/auto-dev/commit/ee2b14a53c6672681e754341d7a0820e262e2ab4))
- **kotlin:** import code to source ([fb7c687](https://github.com/unit-mesh/auto-dev/commit/fb7c687b11e81c49ec59c37deefbcc50036d57ac))
- **kotlin:** init kotlin context prompter ([b3493b1](https://github.com/unit-mesh/auto-dev/commit/b3493b102dca3e98584ad4d930bfc4a51b6be0e5))
- **kotlin:** make context works ([381ec37](https://github.com/unit-mesh/auto-dev/commit/381ec37e85737511576bb9dc6a91a97c6c26fd76))
- make compile works ([ec332fc](https://github.com/unit-mesh/auto-dev/commit/ec332fc12408a88fb5e2cea93ddb9622f662f170))
- make intenstion works ([3141315](https://github.com/unit-mesh/auto-dev/commit/3141315fbdf7d7b82c07f7f16d5cbc3e3d50d887))
- make java simliar chunk works ([174afea](https://github.com/unit-mesh/auto-dev/commit/174afea4a9ffc54637e44870f0b5e51c4b2cae13))
- make prompmt works ([dc76f14](https://github.com/unit-mesh/auto-dev/commit/dc76f14d1b52a071d3f7c70bcdbfe8e47d6e0bab))
- make to extension point ([bf8ac64](https://github.com/unit-mesh/auto-dev/commit/bf8ac64921474ec5f098e5229c3e7b3233544d89))
- **python:** add simliar chunk for testing ([7dc3016](https://github.com/unit-mesh/auto-dev/commit/7dc301653ef51fcb2123dab5e86d0e8fa8860895))
- **python:** init context ([2f24e41](https://github.com/unit-mesh/auto-dev/commit/2f24e4147745668c0ccc43e334fa793ec5f4c779))
- **releasenote:** clean prompter ([65f54cf](https://github.com/unit-mesh/auto-dev/commit/65f54cfcce1c48e2d24aebc5535bdec4c3a1933f))
- **releasenote:** init for change log ([ebf8f22](https://github.com/unit-mesh/auto-dev/commit/ebf8f22ddfe03a4a2c086d162f45dec045ad2aad))
- **releasenote:** make it works ([86732ed](https://github.com/unit-mesh/auto-dev/commit/86732ed3e6de0897863bf3ca1bb16e5015431fde))
- **test:** init intention ([380a972](https://github.com/unit-mesh/auto-dev/commit/380a9723ad341f82836afc12a8679d61b4148e1e))
- **test:** init test context provider ([bff5c33](https://github.com/unit-mesh/auto-dev/commit/bff5c33a383f620cd5af986ef828e867fe86833d))
- try direct to codeS ([78dfa9b](https://github.com/unit-mesh/auto-dev/commit/78dfa9b98aeaa240196a3a98eabb3c93a9689955))
- try to add for displays ([04707c8](https://github.com/unit-mesh/auto-dev/commit/04707c88ac775b94d4d6e3d9469b481f027e3119))
- try to use completion ([c875910](https://github.com/unit-mesh/auto-dev/commit/c8759108552744a994a1b268c775afd98c0de68c))
- try to use simliar chunk ([d3734e3](https://github.com/unit-mesh/auto-dev/commit/d3734e3f3e7261afbc926f2bdd7f583201bb8280))
- try to use text presentation ([3ccdbca](https://github.com/unit-mesh/auto-dev/commit/3ccdbcae6b09bd49d3f28da2bc645df8e46bfb6d))
- udpate for idea plugin ([f103bf8](https://github.com/unit-mesh/auto-dev/commit/f103bf83c69fc29eabc7166fa8f28850b572a1ce))
- **ui:** init text inlay painter ([1d83926](https://github.com/unit-mesh/auto-dev/commit/1d83926361b2e196c1e85e859404e5b572ae69d0))
- update config for run cli ([14782ba](https://github.com/unit-mesh/auto-dev/commit/14782ba3e8dbf78bfd2369f43ffd1b5b1fdc9a32))
- update for comment usages ([a7f6d37](https://github.com/unit-mesh/auto-dev/commit/a7f6d37566b84a23be6dc6e3b81b9e62e775d51a))
- update for listener ([71c426d](https://github.com/unit-mesh/auto-dev/commit/71c426d0d771891adef196e3818aebdcd6ff9c1f))
- update for run action ([47fb8d7](https://github.com/unit-mesh/auto-dev/commit/47fb8d7bcbab12a917c1192109c5ce9207e1029b))
- update for run config ([db9d1e6](https://github.com/unit-mesh/auto-dev/commit/db9d1e66c2e62fef5a747a2079914607cb71abf1))
- update for tasks ([dd808bc](https://github.com/unit-mesh/auto-dev/commit/dd808bc761b3866f8564a68e941e4ae0c66be9ad))
- update for tasks ([8d029da](https://github.com/unit-mesh/auto-dev/commit/8d029dacb43f4b2f974ac19cf9e91868eda6f45e))
- update for tasks ([4cb47f0](https://github.com/unit-mesh/auto-dev/commit/4cb47f035da554e30fc44b2e42e878922d43b69f))
- update for tool Window icon ([9b20afb](https://github.com/unit-mesh/auto-dev/commit/9b20afb7d444d796d63a071c8cfd93dc069be02b))
- use director action ([76f3952](https://github.com/unit-mesh/auto-dev/commit/76f3952f5e83ffbcd45ee228fbb2df9076b2b366))

## [0.6.1](https://github.com/unit-mesh/auto-dev/compare/v0.6.0...v[0.6.1]) (2023-07-16)

### Bug Fixes

- add lost codes ([a5bf86b](https://github.com/unit-mesh/auto-dev/commit/a5bf86bd385c0cb3da948c9e9c94b0a8b2ef30d5))
- fix cos ([bda26ca](https://github.com/unit-mesh/auto-dev/commit/bda26ca5ca1729302f923d39d36b94b2f5326476))
- fix format issues ([6fce6f7](https://github.com/unit-mesh/auto-dev/commit/6fce6f79f862b1261348f1488cc30eb8b26404ae))
- fix interface error issues ([9f04d13](https://github.com/unit-mesh/auto-dev/commit/9f04d13856a94f85c11bf35dc6a7ee6b890b4648))
- fix issues for all format ([83e3776](https://github.com/unit-mesh/auto-dev/commit/83e37766602acdbbe76f5c12a1084c9dc6968051))
- fix prompting issue ([01966ec](https://github.com/unit-mesh/auto-dev/commit/01966ec5f8b106458d13f3c9fd026a6d71a519d9))
- fix read issues ([8c6eab0](https://github.com/unit-mesh/auto-dev/commit/8c6eab0155903af651aa2d30eda86a3216158973))
- fix sample errror ([eefb61e](https://github.com/unit-mesh/auto-dev/commit/eefb61e37cda5b8abe9b8066f85fa40fa281949e))
- fix single loading issues ([09d21b7](https://github.com/unit-mesh/auto-dev/commit/09d21b70adf7472efd3eb29f600c754706d2c2f0))
- fix split issues ([690f839](https://github.com/unit-mesh/auto-dev/commit/690f839a21b5ed374f3345d44aae1361d61c5bef))
- fix test ([a241a3c](https://github.com/unit-mesh/auto-dev/commit/a241a3c8cd69c289110c843f865d528716ba82c2))
- make repository works ([0f17226](https://github.com/unit-mesh/auto-dev/commit/0f17226c5b1e065d1d66f490d9e704710fb73345))
- try to update for dtos & entities ([23a7e5d](https://github.com/unit-mesh/auto-dev/commit/23a7e5d059747f538672aca4bafe1010cf5bc7fe))

### Features

- add basic for dto ([94c1b9a](https://github.com/unit-mesh/auto-dev/commit/94c1b9a3ab2036f69e4bb368b63e31afa4d5f653))
- add basic parse for multiple codeblocks ([34926d2](https://github.com/unit-mesh/auto-dev/commit/34926d27c7a8ce204d1ad5d3c735a8010415cd79))
- add condition for check method ([ea6c87a](https://github.com/unit-mesh/auto-dev/commit/ea6c87a1a55ba83c2e84f93dda5e8a51cd810a95))
- add crud processor ([6204066](https://github.com/unit-mesh/auto-dev/commit/62040667aab54fc3d3c0edde0b5c1408e6ad7637))
- add default config for prompt ([c1a2779](https://github.com/unit-mesh/auto-dev/commit/c1a2779fa8f76ca3beedbea5b336d796ce70712b))
- add for custom prompot config ([bb4f0b2](https://github.com/unit-mesh/auto-dev/commit/bb4f0b2b6a5b811d2a4f98d56a9a0b2af71cdd93))
- add handle for start with indent ([191ae4c](https://github.com/unit-mesh/auto-dev/commit/191ae4c425b22584aa5b415c3d02c3d4a1bf9892))
- add lost for define entity ([05e75f1](https://github.com/unit-mesh/auto-dev/commit/05e75f1f98f477f6362dcbfdf758959e8e76baba))
- add requirements to prompot item ([0b07662](https://github.com/unit-mesh/auto-dev/commit/0b07662fe534f166832a49d70ed10d8cc4e5e143))
- add test to keep config same to prompot ([674dfe6](https://github.com/unit-mesh/auto-dev/commit/674dfe66e30ac2da4606e9bce7db6be43eccc4e2))
- enable for scroll ([e0cf097](https://github.com/unit-mesh/auto-dev/commit/e0cf097aaa211109135469e8e5cf8b60dff28fc5))
- init basic code post processor ([4f8652e](https://github.com/unit-mesh/auto-dev/commit/4f8652e40c432d3b5006d93791e86fcab2ff423f))
- init basic service logic ([aab5c0d](https://github.com/unit-mesh/auto-dev/commit/aab5c0d4349af1f07fe1b5f81cc0d4e28a610765))
- init default spec ([0c3d87f](https://github.com/unit-mesh/auto-dev/commit/0c3d87f2c992ad680f6b5f6d65e49e8df0308926))
- init for create dot and entity ([5d37ce5](https://github.com/unit-mesh/auto-dev/commit/5d37ce5bfabcdda0b98eb5242e0400621152fb39))
- init for psi code ([b459fa4](https://github.com/unit-mesh/auto-dev/commit/b459fa487b92c84fb3c3d163917d50c88b21274c))
- make space works ([631c818](https://github.com/unit-mesh/auto-dev/commit/631c818fa213fdbfe566786f198e0a6933562fb5))
- make spec to configureable ([83cf990](https://github.com/unit-mesh/auto-dev/commit/83cf9904c42f468a34b3db78351c30c03d942c3b))
- update for autodev demo ([0e6e759](https://github.com/unit-mesh/auto-dev/commit/0e6e7590ebb1c3bf252d1f622fa6ad1f185ed952))
- update for cursor position ([12feb34](https://github.com/unit-mesh/auto-dev/commit/12feb34a6949de0d99bcd8f9d526b255145992dd))

## [0.5.5](https://github.com/unit-mesh/auto-dev/compare/v0.5.4...v[0.5.5]) (2023-07-12)

### Bug Fixes

- fix usage issues ([9cff4fa](https://github.com/unit-mesh/auto-dev/commit/9cff4fa531e9b07e0047c4e62115fae4406e6205))

### Features

- add basic stacks ([c5bfc1b](https://github.com/unit-mesh/auto-dev/commit/c5bfc1bf809c2f0c06f2ca3febd5b74ca338676c))
- add commit faeture ([afcaafa](https://github.com/unit-mesh/auto-dev/commit/afcaafae1f988d426c78cdc1aaf47ae58b3a72c2))
- add fix this for issues ([12f418c](https://github.com/unit-mesh/auto-dev/commit/12f418caf7a20f2dc0e49beddaee4ba6d638c89c))
- add gradle to deps ([025d85f](https://github.com/unit-mesh/auto-dev/commit/025d85fb9a012503345afae3e98c1f4c56330e28))
- add simple fix issues ([5eb07f8](https://github.com/unit-mesh/auto-dev/commit/5eb07f869f35f8e4fd05829e824a8bd26a811b61))
- init ddl action ([e76f7e5](https://github.com/unit-mesh/auto-dev/commit/e76f7e59597ce4ec953859842dbfd9a799133d3a))
- init for generate commit message ([377df63](https://github.com/unit-mesh/auto-dev/commit/377df6356fe4f9655555ecb1bed863ac67731259))
- init for sql ddl ([43970b1](https://github.com/unit-mesh/auto-dev/commit/43970b1af79cc45d4ad65e2d90f800b3906e4f62))
- init library data ([60c5d6d](https://github.com/unit-mesh/auto-dev/commit/60c5d6dde6ade2d13ad149d9e5f9a7be5a1a9dfd))
- init stream api ([7bc6889](https://github.com/unit-mesh/auto-dev/commit/7bc688996d2af994b1c9277a654736e9319ae3c3))
- init vcs for detect cchange ([98261d7](https://github.com/unit-mesh/auto-dev/commit/98261d73e5761342a9c10481d84b1760c094a2d5))

## [0.5.4](https://github.com/unit-mesh/auto-dev/compare/v0.4.0...v[0.5.4]) (2023-07-11)

### Bug Fixes

- remove unused parameters ([fb68b71](https://github.com/unit-mesh/auto-dev/commit/fb68b71cf8d0bd75603fba9b2329d9cfc6718f97))

### Features

- add find bug support ([f971135](https://github.com/unit-mesh/auto-dev/commit/f971135f59f98cc3665bc7516edbab5f3aed8926))
- add proxy ([e6ef839](https://github.com/unit-mesh/auto-dev/commit/e6ef8399fec5d921564e0b154f2b0c6970fcb097))
- add proxy for items ([ddb5c20](https://github.com/unit-mesh/auto-dev/commit/ddb5c20a0c163ae8348d069430a114d824b06d54))
- add simple suggestion ([7fd8aaa](https://github.com/unit-mesh/auto-dev/commit/7fd8aaa3a64b83bf6358e4d144823100c58daca6))
- init for service template ([a0a1b8a](https://github.com/unit-mesh/auto-dev/commit/a0a1b8aff8970131ffb6aef5f6304b9cee07cb61))
- make custom open api host works ([f4861ce](https://github.com/unit-mesh/auto-dev/commit/f4861ce82e93b3e3614b435da2b93b7305f046d1))

## [0.0.8](https://github.com/unit-mesh/auto-dev/compare/v0.0.7...v[0.0.8]) (2023-04-21)

### Bug Fixes

- fix default timeout ([9746266](https://github.com/unit-mesh/auto-dev/commit/9746266b5dd7d809c7a619f7b6c4cd8d65d4afa1))
- try to merge configure ([6f0d24b](https://github.com/unit-mesh/auto-dev/commit/6f0d24ba26972eef53815a7ed8da7d3a7cc889ac))
- update template ([69eb06c](https://github.com/unit-mesh/auto-dev/commit/69eb06c2cc26e5b947aa94524bac8581a125b709))

### Features

- add progressbar for code complete action ([a8b3fdc](https://github.com/unit-mesh/auto-dev/commit/a8b3fdc0e0f7465301d336065747ffa6334d07ce))
- update config for xml ([d5c70f6](https://github.com/unit-mesh/auto-dev/commit/d5c70f681b3454fa3b77105f34231f2a6890a44d))

## [0.0.7](https://github.com/unit-mesh/auto-dev/compare/v0.0.3...v[0.0.7]) (2023-04-19)

### Bug Fixes

- fix lost version ([47e027f](https://github.com/unit-mesh/auto-dev/commit/47e027f831236f02e3113221f7127376e97e9744))
- fix runtime issues ([90d1ca0](https://github.com/unit-mesh/auto-dev/commit/90d1ca0bdf924ccdaffe4882f98b006ff0cdfb1a))
- fix typos ([fe0417f](https://github.com/unit-mesh/auto-dev/commit/fe0417f6575b6358db9336b27300ce88c23ab50a))
- fix typos for xml ([04d5bf5](https://github.com/unit-mesh/auto-dev/commit/04d5bf5cc02abc010e1e5bd88940f5ffcbd408ed))
- fix version again ([f1794c5](https://github.com/unit-mesh/auto-dev/commit/f1794c5a71df443150436133613f6d778b674003))
- modify api usage ([74fb4de](https://github.com/unit-mesh/auto-dev/commit/74fb4dec22a00bdfa9dfa151552a6424def93d74))
- not follow the openai README ([ac2abd1](https://github.com/unit-mesh/auto-dev/commit/ac2abd1122960b147a2cd191341f30cb1b4f49a7))

### Features

- add auto comment support ([f47572d](https://github.com/unit-mesh/auto-dev/commit/f47572db4e062bafb6c12c2cb43817f8807fe976))
- make code comments works ([db369d2](https://github.com/unit-mesh/auto-dev/commit/db369d2df86e28c418c4f812fbdaadab7532e25d))

## [0.0.3](https://github.com/unit-mesh/auto-dev/compare/v0.0.2...v[0.0.3]) (2023-04-18)

### Bug Fixes

- fix line marker leaft psi issue ([3fbacf3](https://github.com/unit-mesh/auto-dev/commit/3fbacf3e903fc30f56dc24b81b151fbfc3d772ba))
- fix some typos ([14d0b91](https://github.com/unit-mesh/auto-dev/commit/14d0b919aeb4d7497345edd2778b8eb97a036ff1))

### Features

- add brain configures ([d049040](https://github.com/unit-mesh/auto-dev/commit/d04904004a47e2fbc729ca7f32a08c50e7283198))
- add code complete actions ([07bff16](https://github.com/unit-mesh/auto-dev/commit/07bff1656337d094c329d0ed854abbeb7e1f5f08))
- add controller to method name ([93b1808](https://github.com/unit-mesh/auto-dev/commit/93b18088482b2bcad7df4abebb0c49b22db2cbe5))
- add copilot types ([2986a33](https://github.com/unit-mesh/auto-dev/commit/2986a330b586101b6353c74a13cec6d9423a7600))
- add find bug icon ([f5ec8c4](https://github.com/unit-mesh/auto-dev/commit/f5ec8c4dad3b62a6a9fb68f4ae523d2085a164c0))
- add linemarker for testings ([3f0879a](https://github.com/unit-mesh/auto-dev/commit/3f0879a0ca43b80964c0261acf426d0f79f4493f))
- init find bug line marker ([d74d9fb](https://github.com/unit-mesh/auto-dev/commit/d74d9fb11e713a0b367d20e80e24a0bf1664ea4b))
- init settings bar ([45060bf](https://github.com/unit-mesh/auto-dev/commit/45060bf50e450c871894676b7611d96cf16d0263))
- make it works ([4add40c](https://github.com/unit-mesh/auto-dev/commit/4add40c17b7b723fc5f34d43002b0c82700c194d))
- udpate code cmplete ([5e05dab](https://github.com/unit-mesh/auto-dev/commit/5e05dab1e70ef6e43572b80400c392777bbdea12))

## [0.0.2](https://github.com/unit-mesh/auto-dev/compare/v0.0.1...v[0.0.2]) (2023-04-17)

### Bug Fixes

- fix class empty issues ([a9cf9be](https://github.com/unit-mesh/auto-dev/commit/a9cf9be1a107a11acc2e56f92940c33aeb743b62))
- fix template code error issues ([ef46df4](https://github.com/unit-mesh/auto-dev/commit/ef46df45dad8bd4c22d9b8f9632d5433e1578845))
- fix template dir error issues ([9485bdb](https://github.com/unit-mesh/auto-dev/commit/9485bdb44553e83bc33877d0ea7ad7b18df6ec71))
- fix test warnings ([b8a3685](https://github.com/unit-mesh/auto-dev/commit/b8a36852648c1ea84ae1d3fadd470bace46910f2))
- fix tests ([647eb17](https://github.com/unit-mesh/auto-dev/commit/647eb17a8ec99e74a6d5bd8427ad3d809f821c49))
- fix typos ([7e64357](https://github.com/unit-mesh/auto-dev/commit/7e6435701a7e7abad7924d6f401ea41eee78801d))
- fix warining ([aed6609](https://github.com/unit-mesh/auto-dev/commit/aed66098c196e0595723f13d7354d0f159e6624d))
- set tempature to 0.0 ([f2e066e](https://github.com/unit-mesh/auto-dev/commit/f2e066e86871cb16d999b5fafec4aaf6ed8e2c37))
- update template ([c1749fe](https://github.com/unit-mesh/auto-dev/commit/c1749fe96d7bac007268fb5a6737ce80e7b8267e))

### Features

- add handle for template ([b2a9df5](https://github.com/unit-mesh/auto-dev/commit/b2a9df5396805ac48e1f84f01a77b28125916ac1))

## [0.0.1](https://github.com/unit-mesh/auto-dev/compare/e085cfe3974610d9fe3459ed61639c27dd96af95...v[0.0.1]) (2023-04-17)

### Bug Fixes

- fix comments issues ([7dd2f48](https://github.com/unit-mesh/auto-dev/commit/7dd2f48aa33c508828517d069af223678c74f3c6))
- fix compile warning ([83e33cc](https://github.com/unit-mesh/auto-dev/commit/83e33cc87ef5eb3a11d2bbc6128dbee11aa87a93))
- fix create from context issues ([e8f05fb](https://github.com/unit-mesh/auto-dev/commit/e8f05fb83b5fe79745b62905731252fb733b051f))
- fix for api engien width issues ([f8eaf64](https://github.com/unit-mesh/auto-dev/commit/f8eaf64b4e51f27d5ab8e6462d7a89c37bba18db))
- fix for kotlin psi issues ([6dd12db](https://github.com/unit-mesh/auto-dev/commit/6dd12dbfdeae457bf2303ce0e00c264cf9f314fc))
- fix for story ([97e6034](https://github.com/unit-mesh/auto-dev/commit/97e60348dfcc759aa252a2f02d85550b19cd2609))
- fix README ([e085cfe](https://github.com/unit-mesh/auto-dev/commit/e085cfe3974610d9fe3459ed61639c27dd96af95))
- fix running issues ([7a84358](https://github.com/unit-mesh/auto-dev/commit/7a84358ae5c06c49edae1c81188d793aa7985307))
- fix some typos ([96fda09](https://github.com/unit-mesh/auto-dev/commit/96fda097fe797cc745ebad93f7724128f549dc76))
- fix story id ([e19e279](https://github.com/unit-mesh/auto-dev/commit/e19e279c7c3d8c45c6729b5cf3c7cadd812ccf94))
- fix tests ([9b3cc72](https://github.com/unit-mesh/auto-dev/commit/9b3cc724028f049effc728d09aed03efeac2c406))
- fix typos ([3891d5d](https://github.com/unit-mesh/auto-dev/commit/3891d5d5c6f0eff389386a443232ff09541dee7f))
- fix typos ([118a655](https://github.com/unit-mesh/auto-dev/commit/118a65566522c6aec94bf01e8a91234325588fed))
- fix ui issues ([da7836b](https://github.com/unit-mesh/auto-dev/commit/da7836b4e84a863a539950c4c49b199fd236c2ce))
- fix writtings issues ([456a4ec](https://github.com/unit-mesh/auto-dev/commit/456a4ecf2e4ca90a1d705939997546273a549977))

### Features

- add first test for samples ([89efd92](https://github.com/unit-mesh/auto-dev/commit/89efd927301030462a32dd23dd2b97a1987da504))
- add github repo to projects ([1301826](https://github.com/unit-mesh/auto-dev/commit/13018264453755ac38a5b7e9e864cffe9c500ecf))
- add indicator ([6b3af63](https://github.com/unit-mesh/auto-dev/commit/6b3af6315a2f30d1cd18a6ce38430f3cb3a0bcb2))
- add init for controller update ([c2f3155](https://github.com/unit-mesh/auto-dev/commit/c2f31552d04ddd7228da1e56497658659c86dab3))
- add more filter for works ([f107637](https://github.com/unit-mesh/auto-dev/commit/f107637296acc53dfaa59c3e6d5d024c42a57683))
- add project info to projecT ([fa255e0](https://github.com/unit-mesh/auto-dev/commit/fa255e0e590d1bbacedbb1576b53d18f3766825d))
- add test for annotator ([e046f4f](https://github.com/unit-mesh/auto-dev/commit/e046f4f26f77100da31a0ccb00b4b6017bc67813))
- add test for controller prompt ([db017b5](https://github.com/unit-mesh/auto-dev/commit/db017b5dc7234a3f20377ff25a21029d0ea8828a))
- init basic bnfgst ([0f96930](https://github.com/unit-mesh/auto-dev/commit/0f96930f35ede61b7e99538bca6889834fc4f464))
- init basic command cli ([783435f](https://github.com/unit-mesh/auto-dev/commit/783435f12e78433ed35c9d9a38dad60432494b3c))
- init basic command runner ([0a8e7f4](https://github.com/unit-mesh/auto-dev/commit/0a8e7f4e4523f612b09afe9d26a134ad46ca1623))
- init basic configure ([9d12a5f](https://github.com/unit-mesh/auto-dev/commit/9d12a5f419b09545e7d787d80e156cebb39c0d27))
- init basic configure editor ([2ee854c](https://github.com/unit-mesh/auto-dev/commit/2ee854c58ed06ab874b1bfb9efb35ae40ba7b336))
- init basic datastruct for github and gpt ([787450e](https://github.com/unit-mesh/auto-dev/commit/787450e4d71be1220bd377f39ef4f0f67358841e))
- init basic format ([e6c3676](https://github.com/unit-mesh/auto-dev/commit/e6c36766e1a1422491629112fd9f82644dade699))
- init basic github actions ([4326f80](https://github.com/unit-mesh/auto-dev/commit/4326f80b86bfc3f53480e3be09c44ec209e13312))
- init basic workflow for devti flow ([4825f55](https://github.com/unit-mesh/auto-dev/commit/4825f5511abc218dc43d03cc63fc996100eb6d22))
- init command ([c797a45](https://github.com/unit-mesh/auto-dev/commit/c797a45e512b67a3267820fa0a77f4d9dfea9fd9))
- init config for button ([9005114](https://github.com/unit-mesh/auto-dev/commit/90051141696cbbdc1d4682d89e1e19cf0fb2b047))
- init display story line ([3cec8c4](https://github.com/unit-mesh/auto-dev/commit/3cec8c4947206875f57c8dffac1e8b6bc6865013))
- init dt command runner ([733eb4e](https://github.com/unit-mesh/auto-dev/commit/733eb4e0bf00be40da0f6e57f990230a092724fc))
- init dt model ext ([08e81f6](https://github.com/unit-mesh/auto-dev/commit/08e81f64ce4383cea8249af88da4f2bb09f92f5e))
- init for devti window ([e6e066f](https://github.com/unit-mesh/auto-dev/commit/e6e066f958062318a4559a34830872794161d238))
- init for update story ([7e582aa](https://github.com/unit-mesh/auto-dev/commit/7e582aa3237bd779223ba8dcf673f5f33a5c22d8))
- init java endpoint fetcher ([452f867](https://github.com/unit-mesh/auto-dev/commit/452f86794b94613041ee7db035dc114622170554))
- init parser ([e3b7e98](https://github.com/unit-mesh/auto-dev/commit/e3b7e98623f4ef5c7d01d0d05b47dcbd13b3866f))
- make code can be re-try ([d9d695c](https://github.com/unit-mesh/auto-dev/commit/d9d695cae10c838086f5a7644538ed14e4e2acdf))
- make filter controller works ([ca90e20](https://github.com/unit-mesh/auto-dev/commit/ca90e2029b854a77acb794874246cebe303446a8))
- make it works ([ec96d01](https://github.com/unit-mesh/auto-dev/commit/ec96d01ae39a977dce1d3f569b4051333cc38dbd))
- make pass data to success ([e5ad825](https://github.com/unit-mesh/auto-dev/commit/e5ad825df9fadc5223dff49692daa89b064022c3))
- try to save configure type ([dd5772f](https://github.com/unit-mesh/auto-dev/commit/dd5772faed142cabe148841926473f361696367d))
- tryt o use old version for grammar kit ([db0c8c4](https://github.com/unit-mesh/auto-dev/commit/db0c8c4fd607d68f78dc66822cc373e94be9507b))
- update for configure ([1eb22b8](https://github.com/unit-mesh/auto-dev/commit/1eb22b8a0dfb9aa6a379aa6fb05dd93bf07c05af))
- use single binding ([9092752](https://github.com/unit-mesh/auto-dev/commit/9092752a4a79ff64d062e089137f427a83db3988))

[Unreleased]: https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.7...HEAD
[2.0.0-alpha.7]: https://github.com/unit-mesh/auto-dev/compare/v1.8.18...v2.0.0-alpha.7
[1.8.18]: https://github.com/unit-mesh/auto-dev/compare/v1.8.17...v1.8.18
[1.8.17]: https://github.com/unit-mesh/auto-dev/compare/v1.8.16...v1.8.17
[1.8.16]: https://github.com/unit-mesh/auto-dev/compare/v1.8.15...v1.8.16
[1.8.15]: https://github.com/unit-mesh/auto-dev/compare/v1.8.12...v1.8.15
[1.8.12]: https://github.com/unit-mesh/auto-dev/compare/v1.8.12-ALPHA...v1.8.12
[1.8.12-ALPHA]: https://github.com/unit-mesh/auto-dev/compare/v1.8.11...v1.8.12-ALPHA
[1.8.11]: https://github.com/unit-mesh/auto-dev/compare/v1.8.9-SNAPSHOT...v1.8.11
[1.8.9-SNAPSHOT]: https://github.com/unit-mesh/auto-dev/compare/v1.8.8...v1.8.9-SNAPSHOT
[1.8.8]: https://github.com/unit-mesh/auto-dev/compare/v1.8.7-RELEASE...v1.8.8
[1.8.7-SNAPSHOT]: https://github.com/unit-mesh/auto-dev/compare/v1.8.6-RELEASE...v1.8.7-SNAPSHOT
[1.8.7-RELEASE]: https://github.com/unit-mesh/auto-dev/compare/v1.8.7-SNAPSHOT...v1.8.7-RELEASE
[1.8.6-SNAPSHOT]: https://github.com/unit-mesh/auto-dev/compare/v1.8.5-RC...v1.8.6-SNAPSHOT
[1.8.6-RELEASE]: https://github.com/unit-mesh/auto-dev/compare/v1.8.6-SNAPSHOT...v1.8.6-RELEASE
[1.8.5-RC]: https://github.com/unit-mesh/auto-dev/compare/v1.8.4-RC...v1.8.5-RC
[1.8.4-RC]: https://github.com/unit-mesh/auto-dev/compare/v1.8.3-RC...v1.8.4-RC
[1.8.3-RC]: https://github.com/unit-mesh/auto-dev/compare/v1.8.2-RC...v1.8.3-RC
[1.8.2-RC]: https://github.com/unit-mesh/auto-dev/compare/v1.8.1...v1.8.2-RC
[1.8.1]: https://github.com/unit-mesh/auto-dev/compare/v1.7.5...v1.8.1
[1.7.5]: https://github.com/unit-mesh/auto-dev/compare/v1.7.4...v1.7.5
[1.7.4]: https://github.com/unit-mesh/auto-dev/compare/v1.7.3...v1.7.4
[1.7.3]: https://github.com/unit-mesh/auto-dev/compare/v1.7.2...v1.7.3
[1.7.2]: https://github.com/unit-mesh/auto-dev/compare/v1.7.1...v1.7.2
[1.7.1]: https://github.com/unit-mesh/auto-dev/compare/v1.6.5...v1.7.1
[1.6.5]: https://github.com/unit-mesh/auto-dev/compare/v1.6.4...v1.6.5
[1.6.4]: https://github.com/unit-mesh/auto-dev/compare/v1.6.3...v1.6.4
[1.6.3]: https://github.com/unit-mesh/auto-dev/compare/v1.6.1...v1.6.3
[1.6.1]: https://github.com/unit-mesh/auto-dev/compare/v1.5.5...v1.6.1
[1.5.5]: https://github.com/unit-mesh/auto-dev/compare/v1.5.4...v1.5.5
[1.5.4]: https://github.com/unit-mesh/auto-dev/compare/v1.5.3...v1.5.4
[1.5.3]: https://github.com/unit-mesh/auto-dev/compare/v1.5.2...v1.5.3
[1.5.2]: https://github.com/unit-mesh/auto-dev/compare/v1.4.4...v1.5.2
[1.4.4]: https://github.com/unit-mesh/auto-dev/compare/v1.4.3...v1.4.4
[1.4.3]: https://github.com/unit-mesh/auto-dev/compare/v1.4.1...v1.4.3
[1.4.1]: https://github.com/unit-mesh/auto-dev/compare/v1.2.5...v1.4.1
[1.2.5]: https://github.com/unit-mesh/auto-dev/compare/v1.2.3...v1.2.5
[1.2.3]: https://github.com/unit-mesh/auto-dev/compare/v1.2.2...v1.2.3
[1.2.2]: https://github.com/unit-mesh/auto-dev/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/unit-mesh/auto-dev/compare/v1.1.4...v1.2.1
[1.1.4]: https://github.com/unit-mesh/auto-dev/compare/v1.1.3...v1.1.4
[1.1.3]: https://github.com/unit-mesh/auto-dev/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/unit-mesh/auto-dev/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/unit-mesh/auto-dev/compare/v1.0.2...v1.1.1
[1.0.2]: https://github.com/unit-mesh/auto-dev/compare/v1.0.2-beta.1...v1.0.2
[1.0.2-beta.1]: https://github.com/unit-mesh/auto-dev/compare/v1.0.1...v1.0.2-beta.1
[1.0.1]: https://github.com/unit-mesh/auto-dev/compare/v0.7.3...v1.0.1
[0.7.3]: https://github.com/unit-mesh/auto-dev/compare/v0.6.1...v0.7.3
[0.6.1]: https://github.com/unit-mesh/auto-dev/compare/v0.5.5...v0.6.1
[0.5.5]: https://github.com/unit-mesh/auto-dev/compare/v0.5.4...v0.5.5
[0.5.4]: https://github.com/unit-mesh/auto-dev/compare/v0.0.8...v0.5.4
[0.0.8]: https://github.com/unit-mesh/auto-dev/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/unit-mesh/auto-dev/compare/v0.0.3...v0.0.7
[0.0.3]: https://github.com/unit-mesh/auto-dev/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/unit-mesh/auto-dev/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/unit-mesh/auto-dev/commits/v0.0.1

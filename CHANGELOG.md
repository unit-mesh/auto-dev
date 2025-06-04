# [](https://github.com/unit-mesh/auto-dev/compare/v2.2.0...v) (2025-06-04)

## Unreleased


# [2.2.0](https://github.com/unit-mesh/auto-dev/compare/v2.1.3...v2.2.0) (2025-06-03)


### Bug Fixes

* **agent:** model.update use Java List type, in kotlin should use MutableList ([0ae0d40](https://github.com/unit-mesh/auto-dev/commit/0ae0d40f8f81a0a5f4bfa4c03e031a65001061d2)), closes [#387](https://github.com/unit-mesh/auto-dev/issues/387)
* **AutoDevInputSection:** add newline before file list in input return ([3e5a994](https://github.com/unit-mesh/auto-dev/commit/3e5a994a85df21dd73c411b36b7475583fdb1d30))
* **AutoDevInputSection:** handle empty file list in renderText ([130a764](https://github.com/unit-mesh/auto-dev/commit/130a76450711fbfb5d364eed2490e4673d0cd27e))
* **CodeHighlightSketch:** handle editor release exception ([50c535d](https://github.com/unit-mesh/auto-dev/commit/50c535dcbdaa82d78860d563770b47cf168ca48f))
* **CodeHighlightSketch:** handle nullability for run button presentation icon ([d1bba49](https://github.com/unit-mesh/auto-dev/commit/d1bba492e2c470f437509dbf91613b5cd8dfff77))
* **completion:** handle null paths in file completion ([90f978f](https://github.com/unit-mesh/auto-dev/commit/90f978fc5c258cac25b001492ef9c1ee3a1c0276))
* **core:** handle null project base path and patch names ([13adb55](https://github.com/unit-mesh/auto-dev/commit/13adb55e8a16faaa248a4434c62a1c5e4beb44f4))
* correct line info calculation in file command ([e2ad8dc](https://github.com/unit-mesh/auto-dev/commit/e2ad8dc34c14e487893ef64c93d47fec2b4dbe7a))
* handle ApplicationManager null in test environments ([c35593d](https://github.com/unit-mesh/auto-dev/commit/c35593da1ff781432e44a549c2d8d912bb8b4769))
* **java:** wrap PSI operations in runReadAction ([f1ff942](https://github.com/unit-mesh/auto-dev/commit/f1ff9424d280e19ac42c1b3f78f603d84a2c018f))
* **JSAutoTestService:** change warning logs to error logs for element retrieval failures ([3103af0](https://github.com/unit-mesh/auto-dev/commit/3103af054e624dd1602631a4bb6b8ee47597200f))
* **llm:** fix 223 version issue ([1e0e57f](https://github.com/unit-mesh/auto-dev/commit/1e0e57faf63331a0271f5611cad6e14b9eec8806))
* **LLMModelManager:** rename id to modelId for clarity in model items ([efe7709](https://github.com/unit-mesh/auto-dev/commit/efe7709537ec1a9c786c598b7ac1fb6380466836))
* **LLMModelManager:** standardize GitHub branding and simplify model handling ([e3f8932](https://github.com/unit-mesh/auto-dev/commit/e3f89322a2eb3b99c6fd3a15e3bbbada595d35a2))
* logger info ([6a87171](https://github.com/unit-mesh/auto-dev/commit/6a87171aac51afc775062d5c41b23b6ad5f708f5))
* **mcp:** enhance loading behavior and error handling in tool tree ([308758e](https://github.com/unit-mesh/auto-dev/commit/308758e26fbeb392ea7f00016503db800358b067))
* **mcp:** improve popup closing and UI refresh behavior ([4a8fca0](https://github.com/unit-mesh/auto-dev/commit/4a8fca0d56b4a3c2e95dfd83b7b0b2cb6170004c))
* **patch:** add instruction for handling patch failures ([8adc28b](https://github.com/unit-mesh/auto-dev/commit/8adc28b42f9c01aad4970de4445304f78b3f7585))
* **sketch:** improve diff sketch UI styling and borders ([c5759fc](https://github.com/unit-mesh/auto-dev/commit/c5759fc33eb896629bda87d91093c30e311f3468))
* **sketch:** improve run button for DevIns code execution ([ab9e24a](https://github.com/unit-mesh/auto-dev/commit/ab9e24aa9cb9454d4eab3bc99837a822a8f274ed))
* **sketch:** make bottom border configurable in action bar ([ccabdbb](https://github.com/unit-mesh/auto-dev/commit/ccabdbba321c52edbd95963c63b6ce6de92335f1))
* **sketch:** update runButton action to use AutoDevInsertCodeAction ([25aaff5](https://github.com/unit-mesh/auto-dev/commit/25aaff5ec2582a28423f8e722c1bf282cba181c6))
* **sketch:** update runButton icon setting method ([d74f40e](https://github.com/unit-mesh/auto-dev/commit/d74f40e79a0483fed6ab3a9dde2e6c9a116e79b6))
* **sketch:** update UI elements and text handling ([9f40329](https://github.com/unit-mesh/auto-dev/commit/9f40329721c04776bb198f0c159f1313a738fa1c))
* **structure:** improve HTML tag presentation with location string handling [#392](https://github.com/unit-mesh/auto-dev/issues/392) ([67ca1db](https://github.com/unit-mesh/auto-dev/commit/67ca1db299baaeee02c18ecc5875bf6f80137708))
* **TokenLength:** increase maximum token length to 128000 ([7f45f07](https://github.com/unit-mesh/auto-dev/commit/7f45f07530b7270d608b3f5107a933dff6d6f211))
* typo ([2f7a555](https://github.com/unit-mesh/auto-dev/commit/2f7a555f3df248f8981e7d69dd3556aae692ab0e))
* **ui:** improve editor handling in SummaryMessagesAction ([6c02bbf](https://github.com/unit-mesh/auto-dev/commit/6c02bbf6e41826397c643f9827f98b8c3b67d614))
* **ui:** improve session deletion and click handling ([80d0590](https://github.com/unit-mesh/auto-dev/commit/80d059038b967590c8ef988a8ec1bc3e33e35811))
* **ui:** limit session name to 32 characters in NewSketchAction ([537d2e9](https://github.com/unit-mesh/auto-dev/commit/537d2e9e9e9d4a66f2ca91298b431c371d7566e6))
* **ui:** prevent action execution when sketch is incomplete ([c355944](https://github.com/unit-mesh/auto-dev/commit/c3559444bb3360113e970e09887c5ea97b14a547))
* **ui:** prevent double release of editor in EditorUtil ([af77121](https://github.com/unit-mesh/auto-dev/commit/af7712157e59469887f0d2110349799a8b40f05d))


### Features

* **AutoDevInputSection:** add model selector renderer and enable sketch mode ([520931b](https://github.com/unit-mesh/auto-dev/commit/520931b13eecea2b48ae797706abb8cb434e0e33))
* **AutoDevInputSection:** refactor input section into manager components ([41be96c](https://github.com/unit-mesh/auto-dev/commit/41be96c498d921f2315254f90745e5ea73505834))
* **chat:** add LLM request cancellation support [#394](https://github.com/unit-mesh/auto-dev/issues/394) ([c78ab07](https://github.com/unit-mesh/auto-dev/commit/c78ab07aa98de7ede53af457af10ed3453dd103d))
* **chat:** add model indicator in chat input section ([fd8437f](https://github.com/unit-mesh/auto-dev/commit/fd8437f1b2828b779817717e93cc475d456cd1c2))
* **chat:** add model selector to replace static model label ([11b22ed](https://github.com/unit-mesh/auto-dev/commit/11b22ed7da62725373e9d605c58593c947c2247c))
* **compiler:** add URL support for structure command [#397](https://github.com/unit-mesh/auto-dev/issues/397) ([a2529ce](https://github.com/unit-mesh/auto-dev/commit/a2529ce114c5c027ac4a828634ef4ac7ccfd41c2))
* **completion:** add timeout for toolchain command completion ([9bda9e7](https://github.com/unit-mesh/auto-dev/commit/9bda9e7c77aac956a5ff463a8691ed144d9afd58))
* **core:** add GitHub Actions pipeline monitoring ([a34b916](https://github.com/unit-mesh/auto-dev/commit/a34b916d5b0910d993bb61eaa2910d3be956a47c))
* **github-copilot:** fix model display timing and add manual refresh functionality ([43e3ba8](https://github.com/unit-mesh/auto-dev/commit/43e3ba89bc7c3513a17a54dbe2ccd7ed40e092c7))
* **gui:** add chat history persistence and view [#364](https://github.com/unit-mesh/auto-dev/issues/364) ([4222477](https://github.com/unit-mesh/auto-dev/commit/42224771a08d68ba9705fd70516dc4c2d2600b5b))
* **gui:** add token usage progress bar to chat panel ([178a605](https://github.com/unit-mesh/auto-dev/commit/178a6054b37ff3ae31545dd48e9d3c953dd7c281))
* **gui:** enhance session history popup with relative time display and delete functionality ([12cd69e](https://github.com/unit-mesh/auto-dev/commit/12cd69e44baabe6a4a81754a42ba5a3ad4f3a864))
* **gui:** improve input UI with minimum height and font ([d7e8d60](https://github.com/unit-mesh/auto-dev/commit/d7e8d60d79c13dbbbac16c1209598352d257e617))
* **gui:** prevent saving session when displaying history ([15b75ec](https://github.com/unit-mesh/auto-dev/commit/15b75ecdf225fc94c13d5b9ebd455f3f15a7d246))
* **llm:** add github copilot models support ([6c332a7](https://github.com/unit-mesh/auto-dev/commit/6c332a71db587cc318911aaa1bb90595b5020826))
* **llm:** add token usage parsing in LLM streaming ([5886c05](https://github.com/unit-mesh/auto-dev/commit/5886c057639f7a1bb050c2f0db2d2598e1badc96))
* **llm:** add token usage service with session tracking ([2f1a936](https://github.com/unit-mesh/auto-dev/commit/2f1a9363aacea69da545acf8e2040879407b5604))
* **llm:** add token usage tracking with event system ([7ce823a](https://github.com/unit-mesh/auto-dev/commit/7ce823a3b5897dee748a053a92d0c10bf908a83d))
* **llm:** enhance model management UI with additional fields for Model and Temperature, and improve validation ([982f989](https://github.com/unit-mesh/auto-dev/commit/982f9895e7e7f54945926d101333bf287a171fe0))
* **llm:** filter disabled github copilot models ([cf9826d](https://github.com/unit-mesh/auto-dev/commit/cf9826d72b01f012f6f98a1f87c6904475b4b7be))
* **llm:** implement getUsedMaxToken method ([9478115](https://github.com/unit-mesh/auto-dev/commit/94781154cf2317b8bbe2b9e8be73ffa2c0c2ee94))
* **llm:** optimize LLM list display and management with simplified UI, delete functionality, and improved editing experience ([86cf62f](https://github.com/unit-mesh/auto-dev/commit/86cf62f80b0d04d49fce19c4fe335c34b6453e9d))
* **llm:** refactor LLM settings for improved user experience and add GitHub Copilot model support ([45765b7](https://github.com/unit-mesh/auto-dev/commit/45765b705705dd452169fa37b9d63b332fdf5ce6))
* **llms:** add FunctionTool for LLM function calling support ([dc22322](https://github.com/unit-mesh/auto-dev/commit/dc2232203c672e9d8fd86e0ec89b04cf8cd7290c))
* **mcp:** add cached tool management and lookup ([dd62eb2](https://github.com/unit-mesh/auto-dev/commit/dd62eb2c929a293257d66c894f530020e0d575d0))
* **mcp:** add dynamic tool config listener to sketch ([fc67fb8](https://github.com/unit-mesh/auto-dev/commit/fc67fb86fd1024ea446d05b4e47a248cd6f710da))
* **mcp:** add loading state and refresh functionality ([2203f18](https://github.com/unit-mesh/auto-dev/commit/2203f18d2e4d68b07d9be29df6a6910dad31418b))
* **mcp:** add MCP tools configuration UI ([6bd52a0](https://github.com/unit-mesh/auto-dev/commit/6bd52a0134ea9608699711f96d25b4684915b2af))
* **mcp:** add server and tool tree node classes for improved structure ([d991581](https://github.com/unit-mesh/auto-dev/commit/d991581e3504e31919cb64f330cc4bbb4a97357a))
* **mcp:** implement PersistentStateComponent for McpConfigService to manage tool selections ([4a21e73](https://github.com/unit-mesh/auto-dev/commit/4a21e738accc0bd7e6db4f3a55f4612585f318df))
* **notifications:** add info notification method for project alerts ([13b379f](https://github.com/unit-mesh/auto-dev/commit/13b379f68d8db3a3f58a24064beab6e4ddae0a21))
* **notifications:** set default notification type to INFORMATION ([3e2444c](https://github.com/unit-mesh/auto-dev/commit/3e2444c282588da3ce7cf5809e7f9efbc65e292c))
* **observer:** add setting to control pipeline monitoring ([d283256](https://github.com/unit-mesh/auto-dev/commit/d283256a54e40aee190f9ff08b1d47a1af26d187))
* **quick-assistant:** add ESC handler for inlay panel && dismiss previous inlay panel when new one shown ([a732555](https://github.com/unit-mesh/auto-dev/commit/a7325557199b688fdf6db22b1d25221f21a0b7e4)), closes [#375](https://github.com/unit-mesh/auto-dev/issues/375)
* **search:** limit ripgrep results and improve formatting ([8a917d8](https://github.com/unit-mesh/auto-dev/commit/8a917d8c4d11fe1b9f98519dc8c5a25901f7f91f))
* **settings:** enhance LLM settings with JSON highlighting and response resolver ([062c78d](https://github.com/unit-mesh/auto-dev/commit/062c78d6beabb93f510e54cfb8b149fc53030c51))
* **sketch:** add expanded panel for DevIn code preview with toggle functionality ([234e681](https://github.com/unit-mesh/auto-dev/commit/234e6813082d6441f9b3f0503d0b9977339da2cf))
* **sketch:** add isUser parameter to distinguish user messages ([dcf09a2](https://github.com/unit-mesh/auto-dev/commit/dcf09a2796c7df31d7e6cce08bf5a5808322be52))
* **sketch:** add toggle button for diff panel visibility ([d83a02e](https://github.com/unit-mesh/auto-dev/commit/d83a02e8c1561d11009ce0c9c6cb06e91565f2c2))
* **sketch:** extract PatchProcessor for patch operations ([7ff0b95](https://github.com/unit-mesh/auto-dev/commit/7ff0b95f3a6b3fe521751ee69b9f9265872f91e4))
* **sketch:** implement DevIn collapsible code preview ([926996c](https://github.com/unit-mesh/auto-dev/commit/926996c67efa57e93f66e56bc32189a344969c1e))
* **sketch:** optimize runButton icon based on completion state ([87c1143](https://github.com/unit-mesh/auto-dev/commit/87c1143d3f8722ea237debf4988f5e7431d41a99))
* **sketch:** refactor CodeHighlightSketch editor initialization ([9384bc8](https://github.com/unit-mesh/auto-dev/commit/9384bc8a6006b093dacc86eb7dd0ada944d2f22e))
* **sketch:** replace repair button with regenerate button ([ebe9793](https://github.com/unit-mesh/auto-dev/commit/ebe97939f92e282a03c9d283ca685b3775a73c72))
* **SketchToolWindow:** enhance thinking panel with scrollable view and visibility control and clsosed [#396](https://github.com/unit-mesh/auto-dev/issues/396) ([50a4823](https://github.com/unit-mesh/auto-dev/commit/50a482325dade123f258f03aef4f6e7890d76af9))
* **sketch:** track completion state in code highlight ([96a2372](https://github.com/unit-mesh/auto-dev/commit/96a23725014ef5d49876f3274b4447877b8c7208))
* **sketch:** update icon naming for InsertCode action to uppercase ([7e4976f](https://github.com/unit-mesh/auto-dev/commit/7e4976f49f90f3a74e9f2b4e596d1ea7ed34e69a))
* **structure:** 改进 HTML 标签的文本呈现 [#397](https://github.com/unit-mesh/auto-dev/issues/397) ([dac43aa](https://github.com/unit-mesh/auto-dev/commit/dac43aa06048e957306e8bd9ae5850a440abc351))
* **toolbar:** add SummaryMessagesAction for conversation summaries [#383](https://github.com/unit-mesh/auto-dev/issues/383) ([1a7d40c](https://github.com/unit-mesh/auto-dev/commit/1a7d40c8dd97112d9c9cbfe2cac95c28ee1497b6))
* **toolbar:** update SummaryMessagesAction with Chinese prompts [#383](https://github.com/unit-mesh/auto-dev/issues/383) ([1a61641](https://github.com/unit-mesh/auto-dev/commit/1a616419e3394a189dcd0e1fc71b010d47f0558a))
* **ui:** add token usage display panel to chat input ([27d9af8](https://github.com/unit-mesh/auto-dev/commit/27d9af81af3f7fb67c90083c28a1472a8d4c875e))
* **ui:** expand file display name logic for framework routing ([529b384](https://github.com/unit-mesh/auto-dev/commit/529b384c806e4778c5a9077a5ab16f41e27ec27e))
* **ui:** move existing files to top when added with first=true ([6a0063a](https://github.com/unit-mesh/auto-dev/commit/6a0063a74033e4e77c4ee1bc23bed89718cbc29a))
* **WorkspaceFileSearchPopup:** redesign file search UI ([474c3b1](https://github.com/unit-mesh/auto-dev/commit/474c3b1ecb84e01e7926d9f84819325311e297ab))



## [2.1.3](https://github.com/unit-mesh/auto-dev/compare/v2.1.2...v2.1.3) (2025-05-23)


### Bug Fixes

* **BuiltinMcpTools:** replace toLowerCase() with lowercase() for string comparison ([cde8bf4](https://github.com/unit-mesh/auto-dev/commit/cde8bf424f09fc7b5f03ac3424b411236eee9bae))
* **SimpleDevinPrompter:** wrap VariableTemplateCompiler creation in runReadAction ([1c1e391](https://github.com/unit-mesh/auto-dev/commit/1c1e39127fe3b967f18852d70020833f21175d04))


### Features

* **mcp:** add JBProtocol support for AutoDev commands [#392](https://github.com/unit-mesh/auto-dev/issues/392) unit-mesh/autodev-workbench[#89](https://github.com/unit-mesh/auto-dev/issues/89) ([e2c6097](https://github.com/unit-mesh/auto-dev/commit/e2c60974d8521cf82eccf1be922f0c773cd08836))
* **MCPService:** enhance parseArgs to support GET requests with query parameters unit-mesh/autodev-workbench[#89](https://github.com/unit-mesh/auto-dev/issues/89) ([8c36b94](https://github.com/unit-mesh/auto-dev/commit/8c36b94fcea51563a5f071e8d5386b4cb19d6ae1))



## [2.1.2](https://github.com/unit-mesh/auto-dev/compare/v2.1.1...v2.1.2) (2025-05-13)


### Bug Fixes

* **testing:** wrap PSI operations in runReadAction ([1b745b3](https://github.com/unit-mesh/auto-dev/commit/1b745b36e61bb1c6f1c479b649ca420e259235a7))


### Features

* **language:** rename Shire references to DevIn in editor and index classes ([fa8dfa2](https://github.com/unit-mesh/auto-dev/commit/fa8dfa25c54991a89537e8e07722cb01ea20ac38))
* **language:** rename Shire to DevIn in code and configuration files ([b1c38a3](https://github.com/unit-mesh/auto-dev/commit/b1c38a3e047b0f01b59ad31237478ee6f97cc9f4))



## [2.1.1](https://github.com/unit-mesh/auto-dev/compare/v2.1.0...v2.1.1) (2025-04-27)


### Bug Fixes

* **ToolchainCommandCompletion:** correct return statement formatting in getText function ([033f53d](https://github.com/unit-mesh/auto-dev/commit/033f53d0c8b1a219b14535694fef2e4539766fc4))


### Features

* **completion:** add HobbitHole key and value completion providers [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([ff8b0ea](https://github.com/unit-mesh/auto-dev/commit/ff8b0ea349d436fcf85d934f931c90d87a304855))
* **folding:** implement Shire folding builder for code structure ([603b082](https://github.com/unit-mesh/auto-dev/commit/603b082a570efd8dfdba0f56df94f50311453021))
* **highlight:** add syntax highlighting for Shire language ([9344bb4](https://github.com/unit-mesh/auto-dev/commit/9344bb4d1b95901e6acf8af53b3a92955810d94d))
* **HobbitHole:** add new action status and model properties with default values ([449282e](https://github.com/unit-mesh/auto-dev/commit/449282e1c64e83d699d500a63d09f39cee002511))



# [2.1.0](https://github.com/unit-mesh/auto-dev/compare/v2.0.9...v2.1.0) (2025-04-24)


### Bug Fixes

* **ContextVariableResolver:** handle potential exception when accessing containing file [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([0f37301](https://github.com/unit-mesh/auto-dev/commit/0f373018f0afae8369cc7395cca155daa3f8061b))
* **CrawlProcessorTest:** update test URL to remove trailing slash for accurate parsing ([4d06fe5](https://github.com/unit-mesh/auto-dev/commit/4d06fe589f5d989d7538f3d084be17413d61ee9e))
* **DevInsCompiler:** ensure safe access to nextSibling text using runReadAction [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([53e115d](https://github.com/unit-mesh/auto-dev/commit/53e115da41025d48a2429a4d2f0b2026562a5d67))
* **localization:** update toolchain not found message to include placeholder [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([d8fab1d](https://github.com/unit-mesh/auto-dev/commit/d8fab1d94b2f3ac72984b76a9406c2c1022df489))
* **RestClientUtil:** correct path formatting by replacing DefaultESModuleLoader.SLASH with a literal slash ([0148bfd](https://github.com/unit-mesh/auto-dev/commit/0148bfd86bccceed939681ee71fe8cb2918d8c1a))


### Features

* **action:** introduce VariableActionEventDataHolder for VCS variable actions context management [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([227af8a](https://github.com/unit-mesh/auto-dev/commit/227af8a03e7e733dc79e475e144c2fa7da94bfe2))
* **actions:** add PasteManagerService import to ShireActionStartupActivity [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([c23d5da](https://github.com/unit-mesh/auto-dev/commit/c23d5daaaca070b4bbc40bd58abf55c0ba7daefd))
* **agent:** add display message handling for custom agents [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([8dbd22e](https://github.com/unit-mesh/auto-dev/commit/8dbd22eac2109ef1dbc8a941ea1ac3dde4df629f))
* **agent:** add local mode support and logging for LLM responses [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([ba0a83e](https://github.com/unit-mesh/auto-dev/commit/ba0a83e71d2d27c85820ecd8d02632315ce0683b))
* **agent:** enhance DevIns agent context handling [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([ab7dea3](https://github.com/unit-mesh/auto-dev/commit/ab7dea3a056239a863f63b80ae7618b278d48d2a))
* **agent:** enhance DevIns agent integration and UI [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([3c71382](https://github.com/unit-mesh/auto-dev/commit/3c71382368185aeeccd50c761302e9d1f6cff415))
* **agent:** enhance variable template handling and introduce new file creation services [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([1d59130](https://github.com/unit-mesh/auto-dev/commit/1d591308283d45ce912321674c59990ed5138c47))
* **agent:** refactor agent system with DevIns integration [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([94606e2](https://github.com/unit-mesh/auto-dev/commit/94606e21b2c2439abe5c57ad5c75934c96936320))
* **autodev-core:** add DevIns Tool extension point for agent tool collection ([1e4d6d1](https://github.com/unit-mesh/auto-dev/commit/1e4d6d1f9c664ee218b354fe4c1a042cb437043b))
* **chat:** enhance loading animation and cleanup UI ([d4a2243](https://github.com/unit-mesh/auto-dev/commit/d4a2243235b9d28b889fa710ba38b52619ec369e)), closes [#379](https://github.com/unit-mesh/auto-dev/issues/379)
* **chat:** extract LoadingSpinner component and enhance loading UI [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([b04be6e](https://github.com/unit-mesh/auto-dev/commit/b04be6ecec49b2b77e1ab40523d94082f3d328df))
* **conversations:** add refreshIdeOutput and retryScriptExecution methods for improved conversation handling [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([b15489c](https://github.com/unit-mesh/auto-dev/commit/b15489c2ea6a679d25f90bad8a4e843ce8200c80))
* **coroutines:** refactor processIfClause to be suspend function and update related calls [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([eafc193](https://github.com/unit-mesh/auto-dev/commit/eafc193538ee13a032954d8692138b08b3b3f16f))
* **database:** add DatabaseVariableProvider and SqlContextBuilder for database variable resolution [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([717008b](https://github.com/unit-mesh/auto-dev/commit/717008ba5a07ddd92cfddf2c23694226b986c6ba))
* **debugger:** implement Shire debugging features including breakpoints and variable snapshots [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([d66cbd9](https://github.com/unit-mesh/auto-dev/commit/d66cbd99e0a09b479ad406da7ed40f25a3b340d0))
* **devins:** introduce ActionLocationEditor and ShireActionLocation enums for enhanced action location handling [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([9c09819](https://github.com/unit-mesh/auto-dev/commit/9c098195b35cbb6d8002b7e384b9617f47ec0c86))
* **escaper:** refactor createLiteralTextEscaper to use custom implementation ([7f91dd7](https://github.com/unit-mesh/auto-dev/commit/7f91dd7bbb35afffd865de51aea17f4b337c8b64))
* **GitActionLocationEditor:** add shireActionLocationEditor implementation to plugin extensions ([b55fea9](https://github.com/unit-mesh/auto-dev/commit/b55fea9f55f69fcdb46568fe0a72ab083352f5bb))
* **git:** add GitActionLocationEditor and GitToolchainVariableProvider implementations [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([3aff9bc](https://github.com/unit-mesh/auto-dev/commit/3aff9bcc8dcd78e3f551a1745d1d685bce2941f8))
* **hobbit:** add agentic property to enhance interaction capabilities [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([f47e696](https://github.com/unit-mesh/auto-dev/commit/f47e69679ae2b426141f6f4c0ebed4e82527bf40))
* **httpclient:** implement CUrlConverter and CUrlHttpHandler for handling cURL requests [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([06449ce](https://github.com/unit-mesh/auto-dev/commit/06449ce6e7c37abe8db91763116b0f71e1ef5323))
* **language:** add JavaShireQLInterpreter and JavaSymbolProvider for enhanced Java support [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([cfe4699](https://github.com/unit-mesh/auto-dev/commit/cfe4699944d689b07ff2ffdf5bd543491466fb42))
* **language:** rename Shire to DevIn in test annotations ([ba52e55](https://github.com/unit-mesh/auto-dev/commit/ba52e55a8a9074987e0300ad254cb2cbaf992759))
* **processors:** refactor ThreadProcessor to use suspend functions and improve shell command execution [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([46b4c10](https://github.com/unit-mesh/auto-dev/commit/46b4c102b28a1aa3b0d084e5c2eeb3da5a4fa1a9))
* **run:** add ConsoleService for centralized console output management [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([985099a](https://github.com/unit-mesh/auto-dev/commit/985099a60907e3acadd0612d124573db1944f637))
* **settings:** enable custom agent by default and improve code readability pattern pipeline. [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([603aaae](https://github.com/unit-mesh/auto-dev/commit/603aaae8b2fb39b8ef6b3bd0df78cfcd9a13c273))
* **shire:** add new DevIns action templates and update related configurations [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([87f1d5d](https://github.com/unit-mesh/auto-dev/commit/87f1d5dfa1ea96dd24c3f2199fbcc012028d6f81))
* **shire:** add post processors && update package structure and imports to align with new namespace [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([a3de41e](https://github.com/unit-mesh/auto-dev/commit/a3de41e394494976950e1ffdaa5deed70778e14e))
* **shire:** enhance file handling utilities and streamline editor interactions [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([796fea1](https://github.com/unit-mesh/auto-dev/commit/796fea13fdeeaef87b8fbf07650bf694e951e659))
* **shire:** implement chat completion task and editor interaction for code suggestions [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([5f3c0dc](https://github.com/unit-mesh/auto-dev/commit/5f3c0dc71a41923fb652ec341fdf4c3dd7c99fad))
* **shire:** merge local agent language shire into AutoDev [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([d7fae98](https://github.com/unit-mesh/auto-dev/commit/d7fae98c80fad4dd47a30be0b21f967d9daf3e71))
* **shire:** merge local agent language shire into AutoDev [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([5e6a4a2](https://github.com/unit-mesh/auto-dev/commit/5e6a4a289bb9da19b0d4278c0f7e67070d45f616))
* **tests:** add parsing tests for DevIn language ([ad9001d](https://github.com/unit-mesh/auto-dev/commit/ad9001d27c0920d773bb6c115b2ec2a1fae81a45))
* **tests:** add unit tests for CrawlProcessor and JsonPathProcessor [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([7628fc6](https://github.com/unit-mesh/auto-dev/commit/7628fc6e6a2c1793c5db9490991d70be482c6984))
* **variable:** add DebugValueVariable for enhanced variable handling and refactor related components [#379](https://github.com/unit-mesh/auto-dev/issues/379) ([036727f](https://github.com/unit-mesh/auto-dev/commit/036727ff3e82d7b1bebb5ff79e0739ed16c0c2a9))
* **version:** bump plugin version to 2.1.0 ([6d4630d](https://github.com/unit-mesh/auto-dev/commit/6d4630d5c54d2bd4a848cbba3dc6a3879d4b4e71))



## [2.0.9](https://github.com/unit-mesh/auto-dev/compare/v2.0.8...v2.0.9) (2025-04-22)


### Bug Fixes

* **i18n:** correct server translation in Chinese ([7f0931d](https://github.com/unit-mesh/auto-dev/commit/7f0931d1f0b488e45974f7cae0892c3136e04157))


### Features

* **editor:** add action listener to config button for opening configuration dialog ([8cb0f83](https://github.com/unit-mesh/auto-dev/commit/8cb0f83db0b03caced809cd02abc9074547fc354))
* **editor:** internationalize MCP preview editor UI elements and messages ([bfb7c2d](https://github.com/unit-mesh/auto-dev/commit/bfb7c2d11381c01299841b5165bd20468a936c7f))
* **editor:** internationalize preview and refresh action titles in MCP editor ([40ae860](https://github.com/unit-mesh/auto-dev/commit/40ae8606acecd85415ec97803c9524e0ba667a37))
* **observer:** add GitHub issue processing to RemoteHookObserver ([453f53a](https://github.com/unit-mesh/auto-dev/commit/453f53aa48e950ee70af81e73be4dac532b100e2))
* **quick-assistant:** add ESC handler for inlay panel && dismiss previous inlay panel when new one shown ([4900655](https://github.com/unit-mesh/auto-dev/commit/490065532d24f4723aef4e3e464c31366b2c2410)), closes [#375](https://github.com/unit-mesh/auto-dev/issues/375)


### Reverts

* Revert "refactor(presentation): remove unused PresentationUtil file" ([7ef0407](https://github.com/unit-mesh/auto-dev/commit/7ef0407a32613b23c62a993f2b43517bb0e6571a))



## [2.0.8](https://github.com/unit-mesh/auto-dev/compare/v2.0.7...v2.0.8) (2025-04-13)


### Bug Fixes

* **devins-lang:** improve error handling for toolchain functions ([a261604](https://github.com/unit-mesh/auto-dev/commit/a2616043e5a8946df10306fe577d982fead7e8a1))
* **devins-lang:** remove extra backticks in toolchain command completion ([7ece9d5](https://github.com/unit-mesh/auto-dev/commit/7ece9d561b0016f1b46e5b379f90db0094a4a7cf))
* **editor:** remove scrollbar and caret settings [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([b8df6e2](https://github.com/unit-mesh/auto-dev/commit/b8df6e21f2debf6a10528be739fe9db1e650b1ce))
* **mcp:** adjust tool detail panel layout and styling [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([ae59b16](https://github.com/unit-mesh/auto-dev/commit/ae59b161f2ffc2e6fdaf2c975af9a2211c30c749))
* **mcp:** update function call syntax in system prompt and tool detail panel [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([0773cb6](https://github.com/unit-mesh/auto-dev/commit/0773cb652d96ad75bb5ad94013d931c927b0d031))
* **preview:** reset result panel before loading new content [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([081ab92](https://github.com/unit-mesh/auto-dev/commit/081ab92d54b9d151a44a70d03e23ebd5e593d14b))


### Features

* **core:** add MCP file editor with preview ([62de959](https://github.com/unit-mesh/auto-dev/commit/62de9598aba8451cfb1e5067c1a49b3bc2cb703a)), closes [#371](https://github.com/unit-mesh/auto-dev/issues/371)
* **core:** implement streaming support for MCP editor [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([823f6b1](https://github.com/unit-mesh/auto-dev/commit/823f6b19880f1fb876663fe874b5563a32b387b5))
* **devti:** add result panel for displaying responses in McpPreviewEditor [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([5d460fe](https://github.com/unit-mesh/auto-dev/commit/5d460feeda7d051831d4acd2241d883e1a83ab47))
* **devti:** implement tools panel and chatbot configuration in MCP Preview with v0 [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([b6343d1](https://github.com/unit-mesh/auto-dev/commit/b6343d1e30065611cd9da4dd5a23c1625e214601))
* **devti:** improve MCP server configuration handling and UI [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([c362fa7](https://github.com/unit-mesh/auto-dev/commit/c362fa739187cff7b1de41608cf56c37600dd96e))
* **editor:** enable soft wraps in editor [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([731966a](https://github.com/unit-mesh/auto-dev/commit/731966a991dba52ba9f0cb3bd9af06df80ec72fd))
* **editor:** set "Default" as fallback model name [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([71c3db9](https://github.com/unit-mesh/auto-dev/commit/71c3db9d4a82777fa0b7a549b1cfaf97fc848ca3))
* **mcp:** add detailed request/response panels [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([21722af](https://github.com/unit-mesh/auto-dev/commit/21722aff8a345cf94e2a55e8aeaf51d51ba1e7bd))
* **mcp:** add execution time display to chat results [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([be9a4ae](https://github.com/unit-mesh/auto-dev/commit/be9a4aee6d6dea2bf50e255ced0ef1cee405aad7))
* **mcp:** add JSON editor for tool parameters ([7a73f8a](https://github.com/unit-mesh/auto-dev/commit/7a73f8a60e1989790662124b4e719c72e15c5cf5)), closes [#371](https://github.com/unit-mesh/auto-dev/issues/371)
* **mcp:** add localization for MCP Chat Config Dialog [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([dfe9d3f](https://github.com/unit-mesh/auto-dev/commit/dfe9d3f01d5f01b35e2beee547521f94cc753ab8))
* **mcp:** add localization for MCP Result Panel [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([cf8b970](https://github.com/unit-mesh/auto-dev/commit/cf8b9709bf162ebc3b2fc15963998e65db6116ad))
* **mcp:** add message log panel for tool execution [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([56e9609](https://github.com/unit-mesh/auto-dev/commit/56e9609d625f7126c2b5c5683445b3825843b5d3))
* **mcp:** add result panel with tool call visualization ([fbb6993](https://github.com/unit-mesh/auto-dev/commit/fbb69932d2a703c101f4c8f698a10a076708577f))
* **mcp:** add search ch functionality to MCP tools panel [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([a4f0684](https://github.com/unit-mesh/auto-dev/commit/a4f0684cb9211d7ba0816688658ee7a35a6682f8))
* **mcp:** add search functionality to McpToolListPanel ([c7bfd16](https://github.com/unit-mesh/auto-dev/commit/c7bfd1619a05cf6237c0700f97fe5f8328bdcda8))
* **mcp:** add tool detail dialog localization [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([e782581](https://github.com/unit-mesh/auto-dev/commit/e782581078881ff1efd4200d96dfc2390c1755ae))
* **mcp:** add tool execution capability to result panel ([ce86496](https://github.com/unit-mesh/auto-dev/commit/ce8649649f4e79da80c13867d9208e080fcd36c3))
* **mcp:** enhance system prompt for Sketch agent [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([672a6f5](https://github.com/unit-mesh/auto-dev/commit/672a6f5e005c65f8e5aa890a94343653590d88a8))
* **mcp:** enhance tool call message display [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([1c4fb23](https://github.com/unit-mesh/auto-dev/commit/1c4fb230178f9f11aaf60de07c953db1fc807487))
* **mcp:** enhance tool detail panel with dynamic title and improved description display [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([ae665b0](https://github.com/unit-mesh/auto-dev/commit/ae665b0896701ba305a014d7cc54382dea06a3e8))
* **mcp:** enhance tool integration and UI in LLM config [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([7423b7e](https://github.com/unit-mesh/auto-dev/commit/7423b7e8389514b7e152af4a73ff4e6d90c65553))
* **mcp:** extract tool list panel logic to separate component [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([ce2c485](https://github.com/unit-mesh/auto-dev/commit/ce2c4850287022c407acc49140086a4010dcc35f))
* **mcp:** filter enabled servers only [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([a62384b](https://github.com/unit-mesh/auto-dev/commit/a62384ba7dc2c5c5a17aec1620f663f53df2e154))
* **mcp:** implement tool collection from custom MCP servers [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([72d1b82](https://github.com/unit-mesh/auto-dev/commit/72d1b82f512bd343623f9262b971f996aeb21a80))
* **mcp:** improve server tool loading and error handling in UI [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([a1193ae](https://github.com/unit-mesh/auto-dev/commit/a1193ae6d43e088d10cb508a88017911ca084e1f))
* **mcp:** improve split pane and panel layouts [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([1a134ab](https://github.com/unit-mesh/auto-dev/commit/1a134ababc51cc38c7ff794d77352acebe5fa1c0))
* **mcp:** integrate LLM config with chatbot selector [#337](https://github.com/unit-mesh/auto-dev/issues/337) ([a5c8122](https://github.com/unit-mesh/auto-dev/commit/a5c81228b78eab609dfaa3a6b7ebb3ad0adb6505))
* **mcp:** integrate with MCP server and enhance tool management [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([934c1db](https://github.com/unit-mesh/auto-dev/commit/934c1db2276eeaa65b1f7221759631c26fcb0989))
* **mcp:** replace prompt field with markdown editor in config dialog [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([23a1bff](https://github.com/unit-mesh/auto-dev/commit/23a1bff95102a68f82dad07f228c496feb2d204d))
* **mcp:** set work directory for MCP server command [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([dad6906](https://github.com/unit-mesh/auto-dev/commit/dad69060fbc32764b78ffe70fced9fd1b3717572))
* **mcp:** update system prompt and UI for tool integration [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([0c92bed](https://github.com/unit-mesh/auto-dev/commit/0c92bed4568db300d09c3e66ccedb2c52cdcb6e0))
* **mcp:** 添加工具测试功能 ([605c01c](https://github.com/unit-mesh/auto-dev/commit/605c01c90df4feea05566ed41063c5c2e9469ff9))
* **ui:** extract request detail panel into separate component [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([8556f2f](https://github.com/unit-mesh/auto-dev/commit/8556f2ffbba32742b6e6a57a284024ac779ca5e2))
* **ui:** extract ResponseDetailPanel to separate file [#371](https://github.com/unit-mesh/auto-dev/issues/371) ([e670f28](https://github.com/unit-mesh/auto-dev/commit/e670f28ac32ea4ff6bab2f59170fde7b178562ef))



## [2.0.7](https://github.com/unit-mesh/auto-dev/compare/v2.0.6...v2.0.7) (2025-04-10)


### Bug Fixes

* **archview:** use module type option instead of type name ([0bef83f](https://github.com/unit-mesh/auto-dev/commit/0bef83ff38a140624e64efb3ddea880f2010e8a0))
* **devti:** handle missing example files with error message ([837f65b](https://github.com/unit-mesh/auto-dev/commit/837f65bfadb3e37289d3a6d09a82f64dc3e92e40))


### Features

* **core:** add domain dictionary generation feature [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([fcc5dc6](https://github.com/unit-mesh/auto-dev/commit/fcc5dc60c9cd54edbfebdb460c6cf61779e26be7))
* **core:** add environment variable support for MCP servers ([a70687e](https://github.com/unit-mesh/auto-dev/commit/a70687e553fb96b8cddd2d40b1f486d28be6f7f0))
* **core:** implement domain dictionary generation feature [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([93c23d9](https://github.com/unit-mesh/auto-dev/commit/93c23d925fc9077bb3959eb6ecbc5eeadf6bee32))
* **devins-lang:** add icon for rule completion items ([8ed9ac2](https://github.com/unit-mesh/auto-dev/commit/8ed9ac217204ce2e23126305d79062646eac7911))
* **devti:** add domain dictionary service and magic icon [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([4b9f2e9](https://github.com/unit-mesh/auto-dev/commit/4b9f2e99bcf58d6b24479c3a45c0877ff851196c))
* **devti:** add prompt enhancement feature ([19fc0bf](https://github.com/unit-mesh/auto-dev/commit/19fc0bf430557f3c142b454fd08e38081431db44)), closes [#358](https://github.com/unit-mesh/auto-dev/issues/358)
* **devti:** generate domain dictionary and display in editor [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([429e2ee](https://github.com/unit-mesh/auto-dev/commit/429e2eef279f9eb8376bc57c3d10d6a93e51a2a3))
* **goland,javascript:** add language-specific dictionary providers [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([6a4089b](https://github.com/unit-mesh/auto-dev/commit/6a4089b97e32b1f3ffb5d7e9757a1f72df7a7ede))
* **indexer:** add language-specific file name providers for Python and Rust [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([e3fde74](https://github.com/unit-mesh/auto-dev/commit/e3fde749f1ee81717ab84733b2ad0b2af2e7ffe0))
* **indexer:** add README content to domain dictionary generation ([66b6e8d](https://github.com/unit-mesh/auto-dev/commit/66b6e8d89a735a69d7feaf50e962b0346bbe7704))
* **indexer:** enhance domain dictionary generation with logging and error handling ([9f48eec](https://github.com/unit-mesh/auto-dev/commit/9f48eec40a55ba6ddee8321fbf82cb2bdceec493))
* **kotlin:** add language-specific file name dictionary provider for Kotlin [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([d0c91b7](https://github.com/unit-mesh/auto-dev/commit/d0c91b736f6226e63206f9fee79069840ad0e4ee))
* **prompt:** add project README information to enhance prompt context [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([cba74ec](https://github.com/unit-mesh/auto-dev/commit/cba74ecc66ea6bca9736703f889ed038578f6629))
* **RunTestUtil:** add extension property for RangeMarker to retrieve TextRange ([089c12d](https://github.com/unit-mesh/auto-dev/commit/089c12d755cd2ffe23ffa958945b1766ebc4d1e6))



## [2.0.6](https://github.com/unit-mesh/auto-dev/compare/v2.0.5...v2.0.6) (2025-04-08)


### Bug Fixes

* **agent:** resolve issue with custom fields in message ([8a7adab](https://github.com/unit-mesh/auto-dev/commit/8a7adab8714c265bd423d82d9d89692380bbfb7d))
* **devins-lang:** update file icon display in autocomplete ([19fedf5](https://github.com/unit-mesh/auto-dev/commit/19fedf5263bd85cef2e1d0d55ed8237e46bb0910))
* **devti:** handle exceptions when registering AgentObserver ([439d315](https://github.com/unit-mesh/auto-dev/commit/439d3151702d298995856a9b0cf176ce013c58ef))
* **devti:** handle LightVirtualFile in file diff sketch ([8a9c6a9](https://github.com/unit-mesh/auto-dev/commit/8a9c6a993be3c803b014ca8f5c40b4dda14fd3b7))


### Features

* **devins:** improve Java symbol lookup and chat rendering ([75e9518](https://github.com/unit-mesh/auto-dev/commit/75e95189754fd6f41b72ed30b8995e688b10337a))
* **gui:** add background color for user role messages ([3fb8c55](https://github.com/unit-mesh/auto-dev/commit/3fb8c552af774fb59b32eb771f229ebecea8904c))
* **gui:** clear file list and workspace on input submit ([0a95f3c](https://github.com/unit-mesh/auto-dev/commit/0a95f3c9b3d8eeeb6e36fe6f95af26ebc8bfb4ed))
* **gui:** refactor MessageView layout and add toolbar ([61950ac](https://github.com/unit-mesh/auto-dev/commit/61950ac55e9ea8fbe2dcdaa9ba25ef92821872c7))


### Performance Improvements

* **devins:** optimize Java symbol provider and adjust DevIn completion order ([8060066](https://github.com/unit-mesh/auto-dev/commit/80600662fa0883f2120a5cb2c91cb0ec58aa8f89))
* **devins:** optimize Java symbol provider and adjust DevIn completion order ([0295531](https://github.com/unit-mesh/auto-dev/commit/029553170d6b4af1dd52e0e65bcecda88a021e0d))



## [2.0.5](https://github.com/unit-mesh/auto-dev/compare/v2.0.4...v2.0.5) (2025-04-03)


### Bug Fixes

* **devti:** enhance shell command execution and clean up code ([b992680](https://github.com/unit-mesh/auto-dev/commit/b992680ccef3e9135ed66484a23ae64f24602777))
* **tests:** add updateCustomFormat import to CustomLLMProviderTest ([1963b9d](https://github.com/unit-mesh/auto-dev/commit/1963b9d30b45f008a26b77fdf1255cbb4c373421))
* **tests:** add updateCustomFormat import to CustomLLMProviderTest ([edfd55a](https://github.com/unit-mesh/auto-dev/commit/edfd55a0c1a4514652e72d28fcb535a742379cc5))


### Features

* **customize:** add MCP services test functionality ([d25d2da](https://github.com/unit-mesh/auto-dev/commit/d25d2dace980288f0dfe5b9e5b3f0509fbce1a01))
* **devins-lang:** enhance usage command output with file path and line number [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([7a964d0](https://github.com/unit-mesh/auto-dev/commit/7a964d0cb0be4ee2b5d9d23581c5edc0ccfefc04))
* **devins:** add usage command to get class, method, or field usage in the project [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([5eecf11](https://github.com/unit-mesh/auto-dev/commit/5eecf11ad08316591ad2d3e22ad70e17292b4350))
* **devti:** add file context information to search results and improve class context formatting [#359](https://github.com/unit-mesh/auto-dev/issues/359) ([80a36bb](https://github.com/unit-mesh/auto-dev/commit/80a36bba117cfe29be6c9bb89601390eb26bd58d))
* **devti:** add MCP group support and refactor toolchain functions ([706a47f](https://github.com/unit-mesh/auto-dev/commit/706a47fb0f7eea7c56f4a7e1fe9bdbaa3ed16c56))
* **devti:** implement caller lookup functionality [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([70e86e7](https://github.com/unit-mesh/auto-dev/commit/70e86e78f9f13b054a5f93d97882989bda87fad6))
* **devti:** implement server grouping and search in McpServicesTestDialog- Add server grouping functionality to the table ([4498bd9](https://github.com/unit-mesh/auto-dev/commit/4498bd92ebebe2baa79e5bb66f4cd381350dfc66))
* **devti:** restore and enhance right toolbar functionality ([3b9f7e4](https://github.com/unit-mesh/auto-dev/commit/3b9f7e49b2f434522ad98eab97d987ae912603c9))
* **gui:** add function to add all open files in the chat input toolbar ([82b2987](https://github.com/unit-mesh/auto-dev/commit/82b298724b0b4608d580c4c7fa2eff53482b989b))
* **gui:** add functionality to get recently opened files ([1d7ca1d](https://github.com/unit-mesh/auto-dev/commit/1d7ca1d8cc22b91d2f93b4909b786dce7e0ed878))
* **gui:** add recently opened files to input reference ([655b14b](https://github.com/unit-mesh/auto-dev/commit/655b14b0392828f8998a1e5c585b57a00773a141))
* **gui:** append workspace files to input text [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([d082c04](https://github.com/unit-mesh/auto-dev/commit/d082c048c71a7b438eff19a0deabadb206eec748))
* **gui:** enhance file loading and display in WorkspacePanel [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([60f11ca](https://github.com/unit-mesh/auto-dev/commit/60f11caad6748cbdbb2410d06763d711ea934328))
* **gui:** implement file search popup for workspace panel [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([fe991d4](https://github.com/unit-mesh/auto-dev/commit/fe991d4f00ebd86bb2e6c48c29ad463991e2d95d))
* **gui:** implement workspace panel for file management [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([8b05b45](https://github.com/unit-mesh/auto-dev/commit/8b05b456ab79092a53f7de5f86c14601cbe973e0))
* **gui:** improve file search popup in WorkspacePanel [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([1ea63a7](https://github.com/unit-mesh/auto-dev/commit/1ea63a7f7feda1eb82d5b4312eee7d2120e82405))
* **gui:** replace MarkdownLanguageField with EditorEx for plan editing ([0b7a4a9](https://github.com/unit-mesh/auto-dev/commit/0b7a4a9cf4694d4db86ed93ea6b8909f49a6a24a))
* **i18n:** add internationalization support for issue input panel ([b421bad](https://github.com/unit-mesh/auto-dev/commit/b421bad5f96853e7a53b5324ba101250f6e11416))
* **java:** add class caller lookup functionality [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([66f3666](https://github.com/unit-mesh/auto-dev/commit/66f3666fad57fcce2fd25bda3525c86b7874c9c0))
* **mcp:** improve MCP server handling and tool information retrieval ([2a0ef02](https://github.com/unit-mesh/auto-dev/commit/2a0ef027badba0c849bf46a09901f5660dd86bef))
* **pycharm:** add related class provider for Python [#358](https://github.com/unit-mesh/auto-dev/issues/358) ([384830b](https://github.com/unit-mesh/auto-dev/commit/384830bb75f6f3c8ff105729c90fd21d1a8dc59e))
* **terminal:** add stop functionality to terminal sketch provider ([93b6813](https://github.com/unit-mesh/auto-dev/commit/93b68137acecd43452f88d82c899e1ca134140f2))
* **terminal:** enhance terminal execution states and update UI accordingly ([40af31e](https://github.com/unit-mesh/auto-dev/commit/40af31e946ccc1dba92bcf0d1624b8906bfafcca))


### Performance Improvements

* **provider:** wrap package class retrieval in read action ([4460fcd](https://github.com/unit-mesh/auto-dev/commit/4460fcd8ea8e60931f87a234b70ff9ebdf336498))



## [2.0.4](https://github.com/unit-mesh/auto-dev/compare/v2.0.3...v2.0.4) (2025-03-31)


### Bug Fixes

* **devti:** handle null afterName in AgentStateService ([f701f81](https://github.com/unit-mesh/auto-dev/commit/f701f813be333d8ada06e92270ed30d5ccfcb328))
* **devti:** improve MCP server connection and tool listing ([04d54a3](https://github.com/unit-mesh/auto-dev/commit/04d54a35572d60ab0568a4e131e09439cea34412))



## [2.0.3](https://github.com/unit-mesh/auto-dev/compare/v2.0.2...v2.0.3) (2025-03-31)


### Bug Fixes

* **devins-lang:** enhance file not found check in FileInsCommand [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([0518c5c](https://github.com/unit-mesh/auto-dev/commit/0518c5c0fba1c8ed054733101381e6d265ef3381))
* **devti:** handle click event for NEW type changes in planner result ([93a91e9](https://github.com/unit-mesh/auto-dev/commit/93a91e97f3dfffcf3182dd893c790dc33dca853c)), closes [#352](https://github.com/unit-mesh/auto-dev/issues/352)
* **devti:** handle file creation errors in planner result summary ([230a3af](https://github.com/unit-mesh/auto-dev/commit/230a3af628711f3d347ca7828f964fb3e0a3d5af)), closes [#352](https://github.com/unit-mesh/auto-dev/issues/352)
* **devti:** handle file read errors and improve null safety [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([bb8da4c](https://github.com/unit-mesh/auto-dev/commit/bb8da4ccb57d4aa9e9ba61cbcc258ac551de0877))
* **devti:** handle file read errors in SingleFileDiffSketch [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([0cd223f](https://github.com/unit-mesh/auto-dev/commit/0cd223ff167f43aa1e729db917efe833508f97d4))
* **devti:** handle patch parsing exceptions ([153b69f](https://github.com/unit-mesh/auto-dev/commit/153b69fbc45a1b99391780e9ec8d4704d52380a4))
* **devti:** prevent duplicate changes in agent state service [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([f65d325](https://github.com/unit-mesh/auto-dev/commit/f65d325215f7bdfcbc038c6e715b48995ce1d8c2))
* **devti:** remove ending newline and +``` from diff content ([4cd7c4c](https://github.com/unit-mesh/auto-dev/commit/4cd7c4c3009f2e372427cda80c4a3c0d966e6b6e))
* **devtools:** improve diff view handling for deleted files and exceptions [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([4ca5a31](https://github.com/unit-mesh/auto-dev/commit/4ca5a31e3a62ce08688fe1aecf10dfc518475f40))


### Features

* **core:** add create issue functionality in AutoDev Planner [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([b07bc69](https://github.com/unit-mesh/auto-dev/commit/b07bc69a63adeabd350c561abdbec1fb27de18af))
* **core:** add framework configuration provider for Spring [#338](https://github.com/unit-mesh/auto-dev/issues/338) ([7e2faf1](https://github.com/unit-mesh/auto-dev/commit/7e2faf10909940a206e5e953e209ab6d3ae8b0ed))
* **core:** add ShadowPlanner for generating development plans [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([932850c](https://github.com/unit-mesh/auto-dev/commit/932850c81f8570548244ac081a100e46b4067a5e))
* **core:** update git-delete and git-edit icons [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([58e5762](https://github.com/unit-mesh/auto-dev/commit/58e576266b1023cf77bb4d97a9a579ca4dcb2144))
* **dev-planner:** add loading state indication when processing issue text [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([c857509](https://github.com/unit-mesh/auto-dev/commit/c8575097834e10cb3b9b52ea1fc82cc2f192786b))
* **dev-planner:** implement issue input functionality and enhance tool window logic [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([6d20c1a](https://github.com/unit-mesh/auto-dev/commit/6d20c1afb807659f48ebe74501a7e8f056c811af))
* **devin:** update disable /write in sketch and use patch only [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([8fc0567](https://github.com/unit-mesh/auto-dev/commit/8fc0567d70df8e1109e98bca5e1dc44e99511572))
* **devti:** enhance issue input panel with placeholder functionality ([09fda6e](https://github.com/unit-mesh/auto-dev/commit/09fda6e4643deb96ebc6dbd20ced126b21569ab7)), closes [#353](https://github.com/unit-mesh/auto-dev/issues/353)
* **devti:** improve task step UI and add retry functionality closed [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([61eaa16](https://github.com/unit-mesh/auto-dev/commit/61eaa16031ab8c1ca9f74ebe6709286913b19b34))
* **devtools:** add option to enable/disable diff viewer in settings [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([3a8606e](https://github.com/unit-mesh/auto-dev/commit/3a8606ee732795505df22b840a3695449fad03cf))
* **devtools:** enable auto lint and diff viewer by default ([e1b0e78](https://github.com/unit-mesh/auto-dev/commit/e1b0e7837dc8e584fd05200db0a689b908171c3e))
* **diff:** improve diff viewer layout and auto-command handling [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([53f1c25](https://github.com/unit-mesh/auto-dev/commit/53f1c250636e3d266519a508f9d8dff5c2a2cc25))
* **endpoints:** try to add Spring framework support for Web API views [#338](https://github.com/unit-mesh/auto-dev/issues/338) ([75adef1](https://github.com/unit-mesh/auto-dev/commit/75adef18fe9fd5661dae906b1c893bb116d0c1ca))
* **gui:** add change summary to AutoDevPlannerToolWindow [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([7b4ae3e](https://github.com/unit-mesh/auto-dev/commit/7b4ae3e28761964894025ad4a563f0906b54281f))
* **gui:** add hyperlink to file name in planner result [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([6b40d4c](https://github.com/unit-mesh/auto-dev/commit/6b40d4c362939f209aa617151a9074413308a708))
* **gui:** add toolbar to AutoDevPlannerToolWindow and update IssueInputPanel [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([e70f265](https://github.com/unit-mesh/auto-dev/commit/e70f26545ca97523f358ec98e70bf2cfba9fb559))
* **gui:** enhance change management in planner results [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([94a39b3](https://github.com/unit-mesh/auto-dev/commit/94a39b37c7cabfbaa4847dd34dfe70f4fa59f9a7))
* **gui:** enhance planner result summary with actions and details [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([1981d60](https://github.com/unit-mesh/auto-dev/commit/1981d607403589fa85a5cc186b60fd3770fd427d))
* **gui:** implement a loading panel with animation for AutoDev Planner [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([c1eec6f](https://github.com/unit-mesh/auto-dev/commit/c1eec6f3682fcab8f1287cdf9cdf28498d7e4695))
* **gui:** improve keyboard accessibility for action buttons [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([e0ba5c2](https://github.com/unit-mesh/auto-dev/commit/e0ba5c20798016a7d7323e8a1533bffce78f0a39))
* **gui:** redesign LoadingPanel with advanced animation and dark mode support [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([b650ad4](https://github.com/unit-mesh/auto-dev/commit/b650ad49f064afcad17b61303c80ce825828f176))
* **gui:** show relative file path in planner result [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([72b81b0](https://github.com/unit-mesh/auto-dev/commit/72b81b0286729c1d827ad406eab4c0a251f0ee85))
* **gui:** support create new file and optimize change actions [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([dd16174](https://github.com/unit-mesh/auto-dev/commit/dd161745be4c63eb1a0e3e7a65c7fb14ef22d47a))
* **gui:** use custom icons for planner changes [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([3af249f](https://github.com/unit-mesh/auto-dev/commit/3af249f9ddb2ee801ff694a571f33d4109ae3f46))
* **provider:** add Spring Cloud detection in Gradle projects [#338](https://github.com/unit-mesh/auto-dev/issues/338) ([39e24f5](https://github.com/unit-mesh/auto-dev/commit/39e24f587c6db323663271a47f1693e36aa43267))
* **sketch:** add rerun functionality for failed tasks [#352](https://github.com/unit-mesh/auto-dev/issues/352) ([59b5353](https://github.com/unit-mesh/auto-dev/commit/59b5353ef882dc06138cd6fd25f6f576dbd4f7c0))



## [2.0.2](https://github.com/unit-mesh/auto-dev/compare/v2.0.2-SNAPSHOT...v2.0.2) (2025-03-27)


### Bug Fixes

* **core, terminal:** handle JDK resolution exceptions ([c180cc8](https://github.com/unit-mesh/auto-dev/commit/c180cc88e65c2fa97e169a4cbce192872a4c270f))
* **devins-lang:** improve patch command error handling and UI feedback ([b19ca92](https://github.com/unit-mesh/auto-dev/commit/b19ca920f0188f4b026fef9209d0c5e241604800))


### Features

* **agent:** add MCP support and improve toolchain representation ([7ce703c](https://github.com/unit-mesh/auto-dev/commit/7ce703c74cfbe0fa2781daee4bdb80578f4f41e8))
* **core:** add completion support for MCP tools and refactor related classes ([38abbf7](https://github.com/unit-mesh/auto-dev/commit/38abbf772d3944b02b788f8fb1fd6487c6df434d))
* **core:** add mock data generation for MCP tooltips ([4b19877](https://github.com/unit-mesh/auto-dev/commit/4b19877dbf6b6479a66d6316750f4cb73db2d361))
* **devti:** add module information to SketchRunContext [#350](https://github.com/unit-mesh/auto-dev/issues/350) ([88ca1b7](https://github.com/unit-mesh/auto-dev/commit/88ca1b7d370ba5f5cc52ea9bed779693b8dbc1c5))



## [2.0.2-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v2.0.1...v2.0.2-SNAPSHOT) (2025-03-27)


### Features

* **core:** add support for .devin files in project rules [#349](https://github.com/unit-mesh/auto-dev/issues/349) ([d343e5b](https://github.com/unit-mesh/auto-dev/commit/d343e5b28371f6007cade363f029dead064e048c))
* **core:** add support for user-defined coding rules [#349](https://github.com/unit-mesh/auto-dev/issues/349) ([584e2f5](https://github.com/unit-mesh/auto-dev/commit/584e2f53232198a8a8db8c5f4978b828331d22b7))
* **devins-lang:** add rule completion and update rule path ([68589b8](https://github.com/unit-mesh/auto-dev/commit/68589b8f5d77d192f80416ccb8cceb27be64fa74))
* **devti:** add 'rule' command to retrieve code quality guidelines [#349](https://github.com/unit-mesh/auto-dev/issues/349) ([58d8bcc](https://github.com/unit-mesh/auto-dev/commit/58d8bcc10bea17c4b2d65f64a6db684199f06d0a))
* **devti:** add support for user-defined coding rules [#349](https://github.com/unit-mesh/auto-dev/issues/349) ([c8f4393](https://github.com/unit-mesh/auto-dev/commit/c8f43939d8441446a03e96e3b1a57a7ea0598aeb))
* **devti:** enhance ripgrep binary search for Windows and Unix systems [#344](https://github.com/unit-mesh/auto-dev/issues/344) ([cf348f2](https://github.com/unit-mesh/auto-dev/commit/cf348f2597e1cfee728cbfb0fe5d5b0c96f05e60))
* **devti:** support project rule in AutoDev Sketch/Composer [#349](https://github.com/unit-mesh/auto-dev/issues/349) ([4a02594](https://github.com/unit-mesh/auto-dev/commit/4a02594d37a6f04e43225bff9d6d14cd307a0b5c))


### Performance Improvements

* **devins-lang:** move DevInsCompiler.compile() to IO dispatcher ([1e8baf2](https://github.com/unit-mesh/auto-dev/commit/1e8baf2b89b16ba06b7553ab891023cb040677f8))



## [2.0.1](https://github.com/unit-mesh/auto-dev/compare/v2.0.0...v2.0.1) (2025-03-26)


### Bug Fixes

* **devti:** optimize diff repair prompt and streaming processing ([2ce2e76](https://github.com/unit-mesh/auto-dev/commit/2ce2e7657f725a71bad1c369cb2cf7c7fae6ae74))


### Features

* **devti:** enhance file list UI and functionality [#344](https://github.com/unit-mesh/auto-dev/issues/344) ([de652f7](https://github.com/unit-mesh/auto-dev/commit/de652f7293edba0b3703742df80d4b77f59689ba))
* **gui:** add clear all button and improve file display in AutoDev input section ([bad095f](https://github.com/unit-mesh/auto-dev/commit/bad095f74469e4a0cb6a106bc03953375d96acfc))
* **gui:** add toolbar with clear button and file selection reminder [#344](https://github.com/unit-mesh/auto-dev/issues/344) ([105a990](https://github.com/unit-mesh/auto-dev/commit/105a99020fd11a0adf3af446cd1a4fafdbd441b0))


### Performance Improvements

* **devti:** optimize code application in DiffRepair ([b1411c1](https://github.com/unit-mesh/auto-dev/commit/b1411c1649b1a36e8247026ca2e70d27b27b6229))



# [2.0.0](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.9...v2.0.0) (2025-03-25)


### Bug Fixes

* **devti:** handle multi-hunk patches in DiffLangSketch ([8921fc8](https://github.com/unit-mesh/auto-dev/commit/8921fc835b877df6555b45db0b26dde30ba28650))
* **gui:** add validity check for virtual file before processing ([b95e1ce](https://github.com/unit-mesh/auto-dev/commit/b95e1ce23a7b8ebad492cb7f1332b9095a26aecc))
* **terminal:** notify user of command execution errors ([3fe64d9](https://github.com/unit-mesh/auto-dev/commit/3fe64d97c9a369f471bee31f14cd82293b50d133))


### Features

* **terminal:** add executing status and update result panel ([802eddd](https://github.com/unit-mesh/auto-dev/commit/802eddd5bd6afc6df3f5a06d60311790ef65f76e))
* **terminal:** implement terminal runner service with enhanced functionality ([efab7b5](https://github.com/unit-mesh/auto-dev/commit/efab7b548e10530b4fd7342d6888fa91d088983c))
* **terminal:** set JAVA_HOME for terminal sessions ([f56325a](https://github.com/unit-mesh/auto-dev/commit/f56325a1f8f96f9162675623ea998eecd48368c0))



# [2.0.0-rc.9](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.8...v2.0.0-rc.9) (2025-03-25)


### Bug Fixes

* **auto-save:** change action update thread to EDT and improve file creation error handling ([5090d8c](https://github.com/unit-mesh/auto-dev/commit/5090d8cd5ede85b217f3a53f541d53fb1f455946))


### Features

* **coder:** add auto-scroll feature in sketch and closed [#340](https://github.com/unit-mesh/auto-dev/issues/340) ([acb4833](https://github.com/unit-mesh/auto-dev/commit/acb4833471e69f558268f9fd3f2799b1c76282dc))
* **devtools:** add copy action to plan toolbar and improve code execution ([1d5407e](https://github.com/unit-mesh/auto-dev/commit/1d5407ec0f57bf45c64b10b5e6c3f3f3f31bae80))



# [2.0.0-rc.8](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.7...v2.0.0-rc.8) (2025-03-24)


### Bug Fixes

* **devti:** handle exception in diff panel creation and update planner icon ([13eba06](https://github.com/unit-mesh/auto-dev/commit/13eba0690e29931456658af7909d1d90a43a54da))
* **devti:** handle exception in diff panel creation and update planner icon ([a00b9f8](https://github.com/unit-mesh/auto-dev/commit/a00b9f8ad24a3a3e9710b82616acda25bc9b18b8))
* **devti:** improve Markdown processing and shell safety check ([5039744](https://github.com/unit-mesh/auto-dev/commit/5039744a454f0ca34f187338c3cb179ab50482c7))
* **devtools:** trim input text in chat panels ([a1b4dd8](https://github.com/unit-mesh/auto-dev/commit/a1b4dd8943bd06de643299e3b2f187429e40c4aa))
* fix typos ([15bad5e](https://github.com/unit-mesh/auto-dev/commit/15bad5ea2d3c6a2309728b41c0bc275ac457e453))
* **terminal:** handle exceptions in command safety check and improve UI feedback ([8b6630c](https://github.com/unit-mesh/auto-dev/commit/8b6630c72e774ff0fd266b78f3b998ef5b3d22ce))
* **terminal:** improve shell syntax safety check for dangerous commands ([72ed670](https://github.com/unit-mesh/auto-dev/commit/72ed670aa7f76ca59ff2fbc45cee860eaa9bd635))


### Features

* **coder:** add auto lint code feature ([0afe92f](https://github.com/unit-mesh/auto-dev/commit/0afe92f343ee14a51e576cfc82796b1c8bff6cea))
* **core:** add create_test_for_file tool for AutoDev ([50c6caf](https://github.com/unit-mesh/auto-dev/commit/50c6caf75e58d46ce24a62d03382a2c1f39f22f5))
* **core:** add option to enable auto run terminal commands [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([597ccf5](https://github.com/unit-mesh/auto-dev/commit/597ccf56afc2ba38070ec826730f37b845569363))
* **core:** add option to hide toolbar in CodeHighlightSketch ([6c89b64](https://github.com/unit-mesh/auto-dev/commit/6c89b64a69aafef4cb9929fce8ba6396d2114f8e))
* **core:** add repair functionality to sketch patch ([deab5dc](https://github.com/unit-mesh/auto-dev/commit/deab5dc339522759302c55b736da7994c22587a2))
* **core:** add save file action to AutoDev tool window ([2c9769e](https://github.com/unit-mesh/auto-dev/commit/2c9769ee6c73eb6864c8e8effb21c90941a5ee3b))
* **core:** enhance shell command safety check ([4cb55b7](https://github.com/unit-mesh/auto-dev/commit/4cb55b7fb8ca596e69f0f1c82afd37c2ceb7ebaf))
* **core:** enhance shell safety checks with additional dangerous command patterns ([93c983d](https://github.com/unit-mesh/auto-dev/commit/93c983d3e41d777529912100d4a5da9b19ab6169))
* **core:** optimize DevIn tag handling in AutoDevInlineChatPanel ([37bf949](https://github.com/unit-mesh/auto-dev/commit/37bf9494c1f09479f3265d3df7d7c263b8613f76))
* **core:** update send icon to version 2 ([adad4d9](https://github.com/unit-mesh/auto-dev/commit/adad4d92e5c5f6399318cae7884a2bae757d3f1e))
* **database:** add safety check for dangerous SQL operations ([8f9ea98](https://github.com/unit-mesh/auto-dev/commit/8f9ea98e97f67444f5c12add0be140749ff9f61a))
* **devti:** enhance auto-sketch mode with additional commands ([5f10d32](https://github.com/unit-mesh/auto-dev/commit/5f10d3200adc6e5e1d3ee5ace915a332858c3711))
* **devtools:** implement file saving functionality for AutoDev ([76a4703](https://github.com/unit-mesh/auto-dev/commit/76a4703f7851c6e1216e545cd25254c88fb71f02))
* **devtools:** improve shell command execution and output display ([f029612](https://github.com/unit-mesh/auto-dev/commit/f0296126943dda2ff5b3bf0536d03fd4700bf282))
* **gui:** add localization for save file action ([c3fd97e](https://github.com/unit-mesh/auto-dev/commit/c3fd97e6d480d161b65691daa3377f60a6a5dadc))
* **gui:** replace AutoDevIcons with NewChatAction and simplify MessageView- Remove unused import of AutoDevIcons in MessageView.kt ([4cef76c](https://github.com/unit-mesh/auto-dev/commit/4cef76cdb13b43f9d8752ae7a0748537c429bcc6))
* **settings:** add enable render webview option ([2bdb46c](https://github.com/unit-mesh/auto-dev/commit/2bdb46c6f13e7f160309464e1c2e445ad175b3c8))
* **terminal:** add actions to terminal sketch ([b047edd](https://github.com/unit-mesh/auto-dev/commit/b047edd9ae07b919e65b5ce3c479157add5b70d6))
* **terminal:** add resizable terminal panel and improve terminal sketch [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([b24010a](https://github.com/unit-mesh/auto-dev/commit/b24010a307058a2e3d709f7c3772c5e4278b458e))
* **terminal:** add show/hide terminal action and update related messages ([5ceba0d](https://github.com/unit-mesh/auto-dev/commit/5ceba0d07f39e2d7c53910a8510ed706d2b7e353))
* **terminal:** implement advanced shell command safety check [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([681b95d](https://github.com/unit-mesh/auto-dev/commit/681b95db48d1fcdb9478065b20ab953d9bb0da67))
* **terminal:** implement real-time UI updating for terminal output ([e07a0b2](https://github.com/unit-mesh/auto-dev/commit/e07a0b23aaa6288bb6d8cf85d4225e454042ddc4))
* **terminal:** implement safety check for dangerous shell commands ([6b5666c](https://github.com/unit-mesh/auto-dev/commit/6b5666ccc7fc3a14ba7c458f3bc67c9b20ba9c9f)), closes [#335](https://github.com/unit-mesh/auto-dev/issues/335)
* **terminal:** improve collapsible panel and add execution result feedback && closed [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([92e3b15](https://github.com/unit-mesh/auto-dev/commit/92e3b151a7ed7cee270d07dbfdfb089e75c4ddb8))
* **terminal:** store and reuse execution results ([927a7c8](https://github.com/unit-mesh/auto-dev/commit/927a7c83eefa4f8d11ec53a85fa9ab99271e7a40))



# [2.0.0-rc.7](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.5...v2.0.0-rc.7) (2025-03-20)


### Bug Fixes

* **devti:** improve UI display for markdown and plain text files ([af41812](https://github.com/unit-mesh/auto-dev/commit/af41812b50c4bcf27f33fe0d18e347719627e560))


### Features

* **BuiltinCommand:** disable command in sketch mode [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([60f9b02](https://github.com/unit-mesh/auto-dev/commit/60f9b02ae8907c1b0709dc6e600141a978b04fe2))
* **core:** update icon for AutoDev Planner tool window ([c492fc9](https://github.com/unit-mesh/auto-dev/commit/c492fc9c24d6fd2af2577698f2e7cf97d9ca9f89))
* **settings:** add MCP server port and restart requirement ([5ecf871](https://github.com/unit-mesh/auto-dev/commit/5ecf87126ae352e1e444d8b92ac4e5c8097e70a5))
* **shell:** add async shell execution with progress tracking [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([c94b3b4](https://github.com/unit-mesh/auto-dev/commit/c94b3b45e8cc4917bdb2020c8d4d9798f102cf50))
* **sketch:** add markdown preview support ([e6d6c38](https://github.com/unit-mesh/auto-dev/commit/e6d6c38c8dffc4ce35ed3ce51961c7735aac1f09))
* **terminal:** add collapsible panel for shell code display [#335](https://github.com/unit-mesh/auto-dev/issues/335) ([ca98448](https://github.com/unit-mesh/auto-dev/commit/ca98448d2205766e9d0334f537cc25151502fb96))


### Performance Improvements

* **provider:** wrap file search in runReadAction ([fd3df19](https://github.com/unit-mesh/auto-dev/commit/fd3df192fb7f8724edc2700135cc62bf35fdf3da))
* **sketch:** disable markdown preview by default ([b1e132e](https://github.com/unit-mesh/auto-dev/commit/b1e132ed47a682877c826d05c144e4f98b01f22f))



# [2.0.0-rc.5](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.4...v2.0.0-rc.5) (2025-03-19)


### Bug Fixes

* **plan:** prevent saving unchanged content and handle empty plans ([a98420b](https://github.com/unit-mesh/auto-dev/commit/a98420bb0355db500642d16d9e359371b80884a4))
* **sketch:** remove extra spaces in Chinese comment ([b5345dd](https://github.com/unit-mesh/auto-dev/commit/b5345ddb261515be83b6a7f459607c1a50d079a1))


### Features

* **agent:** add token-based message compression ([8ac27e1](https://github.com/unit-mesh/auto-dev/commit/8ac27e1d7cf29602c635c8fdb996c8bbedcb88a0))
* **mcp:** add IssueEvaluateTool for issue analysis ([53dca04](https://github.com/unit-mesh/auto-dev/commit/53dca0426c0bc89e2e5bfed346863d7b3a8a900e))
* **mcp:** add IssueEvaluateTool for issue analysis ([b95f815](https://github.com/unit-mesh/auto-dev/commit/b95f8153a121b735617e1c3375cac6a726436000))
* **sketch:** add AutoSketchModeListener and blocking plan review ([0a61e78](https://github.com/unit-mesh/auto-dev/commit/0a61e78137ebf79520268a20fa45573dcff7f0ed))
* **sketch:** enable AutoSketchMode by default ([32f756c](https://github.com/unit-mesh/auto-dev/commit/32f756c55156c8b1d2671acf1b05ddf0b76cd330))
* **snippet:** expand snippet file name detection ([2d8988f](https://github.com/unit-mesh/auto-dev/commit/2d8988f9d91791d54bdeac219dce8d4685e6ec94))



# [2.0.0-rc.4](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.3...v2.0.0-rc.4) (2025-03-18)


### Bug Fixes

* **core:** add writeText extension function for VirtualFile for 223 ([7aed665](https://github.com/unit-mesh/auto-dev/commit/7aed66560eaefe8608ff29bd380d47ed431fe8e0))
* **core:** add writeText extension function for VirtualFile for 223 ([e63e1ea](https://github.com/unit-mesh/auto-dev/commit/e63e1eade45210ce038e8d8d6905ded8f76ee86b))
* **devti:** handle empty plan items and add custom engine settings ([56c9789](https://github.com/unit-mesh/auto-dev/commit/56c978981a4c80996f87a1b668d60bfd0344b070))
* **planner:** correct typo in AutoDevPlannerToolWindowFactory ([92c0368](https://github.com/unit-mesh/auto-dev/commit/92c0368ebc172cb000b3612343181be5e868b43b)), closes [#331](https://github.com/unit-mesh/auto-dev/issues/331)
* **ui:** improve layout and scroll behavior in PlanSketch ([185f9c6](https://github.com/unit-mesh/auto-dev/commit/185f9c6b861828d01f110ff5be11ebe231f26ca6)), closes [#331](https://github.com/unit-mesh/auto-dev/issues/331)


### Features

* **coder:** add auto repair diff functionality ([60bb370](https://github.com/unit-mesh/auto-dev/commit/60bb3703647580eb7bab2a4b48ae3ae5b62d35d0))
* **core:** add code file link support in task description [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([de78a26](https://github.com/unit-mesh/auto-dev/commit/de78a26f2fec8c87a4fd4688d0a6ad1f653ec5a6))
* **plan:** add edit plan functionality [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([9c0edc8](https://github.com/unit-mesh/auto-dev/commit/9c0edc83be8b6aa05e88b2fe63d417952f2b6218))
* **plan:** add plan review action and test cases [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([934a985](https://github.com/unit-mesh/auto-dev/commit/934a985c99f54dcd6ce98a0853efe8f88b705405))
* **plan:** add plan review functionality and scrollable UI ([0d1eae1](https://github.com/unit-mesh/auto-dev/commit/0d1eae1e05dd12785b7a1499dac9655edfd6b676)), closes [#259](https://github.com/unit-mesh/auto-dev/issues/259)
* **planner:** add editor mode and improve plan management [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([31c1c0b](https://github.com/unit-mesh/auto-dev/commit/31c1c0b00a43f08192bdf4920b44b613dcbc8801))



# [2.0.0-rc.3](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.2...v2.0.0-rc.3) (2025-03-17)


### Bug Fixes

* **command:** ensure file insertion executes on the EDT for thread safety [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([1a02563](https://github.com/unit-mesh/auto-dev/commit/1a0256315c549e3024fc082e6d3364410b48e58e))
* **compiler:** handle exceptions in patch application ([6bad2c2](https://github.com/unit-mesh/auto-dev/commit/6bad2c2048435e96d6cc606d601a2a4d7aaa9fe3))
* **database:** improve error handling in SQL execution ([d14e03b](https://github.com/unit-mesh/auto-dev/commit/d14e03b7694d0c7667e0e86c5600a3f082a27e97))
* **FileIns:** use relative path in file output ([a16f7cf](https://github.com/unit-mesh/auto-dev/commit/a16f7cf5f9ad38dee051f9b2b53fe11fb001822e))
* **observer:** handle build failure notification [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([0fab4b6](https://github.com/unit-mesh/auto-dev/commit/0fab4b62faaa3d7b123dccce192ebdcb1f18ffb9))
* **observer:** remove unused onImportFailed logic [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([0225dd2](https://github.com/unit-mesh/auto-dev/commit/0225dd27abdb01b20f9792066f02cd143839181e))
* **parser:** handle indentation in code blocks [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([855db60](https://github.com/unit-mesh/auto-dev/commit/855db607262811ebe42af04bf7f9ac128249b44f))
* **sketch.vm:** update DevIn tag syntax and patch instructions ([f8aa890](https://github.com/unit-mesh/auto-dev/commit/f8aa890b78c7671557b1183c8f9e292b31a35aaa))
* **sketch:** update test command and disable RUN in sketch mode [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([d3a0d1d](https://github.com/unit-mesh/auto-dev/commit/d3a0d1d1d775a6df8bac57ac6e12442c1c20d733))
* **task-status:** update task status logic for TODO and COMPLETED [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([a022140](https://github.com/unit-mesh/auto-dev/commit/a0221405eeec9d9fdf97b8e6b4e2c80e3aa07549))
* **terminal:** correct terminal message sending logic ([1684407](https://github.com/unit-mesh/auto-dev/commit/16844078eaa07299c2b4826f8ae0619c0f17c0ba))


### Features

* **agent:** add resetMessages method to AgentStateService ([06598a1](https://github.com/unit-mesh/auto-dev/commit/06598a1e44674cdf7dd73940c8e551eb9efc7f63))
* **agent:** add state reset and message preprocessing [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([01bedd6](https://github.com/unit-mesh/auto-dev/commit/01bedd608f0b1df96feca2d80383283c2af731eb))
* **agent:** enhance AgentState with additional properties ([821d4e3](https://github.com/unit-mesh/auto-dev/commit/821d4e31d65f268e20625a07c5058adccfd26a47))
* **agent:** refactor plan structure to use PlanList and PlanTask [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([56bde59](https://github.com/unit-mesh/auto-dev/commit/56bde59abd06bd1d9039997508e80d39840d711f))
* **code:** support multiple code blocks in CodeHighlightSketch [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([bcbe6bc](https://github.com/unit-mesh/auto-dev/commit/bcbe6bc07a9141ec548f45290731e789cc0583f3))
* **compiler:** add background task for directory processing ([dcb4f48](https://github.com/unit-mesh/auto-dev/commit/dcb4f480faae8267f84794a87ba38274b210ffbf))
* **compiler:** limit file content to 300 lines ([d4b10b9](https://github.com/unit-mesh/auto-dev/commit/d4b10b996958e1976cd3aceda36dbc94c7c6e413))
* **diff-repair:** include user intention in diff repair context ([cf0e478](https://github.com/unit-mesh/auto-dev/commit/cf0e4787af4e9abe41d464cf03bc13f3ecd674a9))
* **observer:** add AgentState and AgentStateService [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([33cea27](https://github.com/unit-mesh/auto-dev/commit/33cea27278861c440a9630a9df87028bd387dc85))
* **observer:** add enable observer setting and validation [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([d330927](https://github.com/unit-mesh/auto-dev/commit/d3309275ac05483d3acc25950e09453e673f036f))
* **observer:** enable AddDependencyAgentObserver and refactor message processing [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([9ba6e6a](https://github.com/unit-mesh/auto-dev/commit/9ba6e6a826929dd9968cc66a8614bc5dbafa163a))
* **observer:** enhance test error reporting and console editor handling [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([19cf4ba](https://github.com/unit-mesh/auto-dev/commit/19cf4ba0f10bf35a481117b6d3dbcde9bc7c70de))
* **observer:** re-enable ExternalTaskAgentObserver and improve error handling [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([9012c1a](https://github.com/unit-mesh/auto-dev/commit/9012c1a2aa06acdab60c0dfd10ab3b89e1688fad))
* **parser:** add GitHub-style TODO support in MarkdownPlanParser [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([a4abd6b](https://github.com/unit-mesh/auto-dev/commit/a4abd6b0e29cfa3dd165eaf5fe43a15779bef658))
* **parser:** add support for THOUGHT and PLAN tags [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([1262136](https://github.com/unit-mesh/auto-dev/commit/1262136400f828429a661120bfc66d4a3e5088a8))
* **parser:** add test for Markdown to HTML conversion [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([e17e3a3](https://github.com/unit-mesh/auto-dev/commit/e17e3a3118914584a56603c3ad4ce31997f1f579))
* **plan-parser:** enhance markdown section status parsing [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([c7db2d4](https://github.com/unit-mesh/auto-dev/commit/c7db2d44d3ac0b56d18f78425619cd463bc6dd83))
* **plan:** add execute button for incomplete tasks and update plan handling [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([f1e468f](https://github.com/unit-mesh/auto-dev/commit/f1e468f56a9322967cbe821b08aa77752b80f20c))
* **plan:** add Markdown plan parser and UI integration [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([ee06e6f](https://github.com/unit-mesh/auto-dev/commit/ee06e6f6f0411c8bc5e8a5abb56148fab44ead77))
* **plan:** add PDCA cycle support to AgentPlan [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([c425059](https://github.com/unit-mesh/auto-dev/commit/c425059300fe309734322fab9d4df60aa8713eea))
* **plan:** add subtask support and improve plan structure [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([01b21c0](https://github.com/unit-mesh/auto-dev/commit/01b21c08d6a4c428cf6a5dcdb084e4db56ca9736))
* **plan:** add task status indicators and UI enhancements ([fe3dfc8](https://github.com/unit-mesh/auto-dev/commit/fe3dfc8e65eef12b5182dbfd813834a928ca80ce)), closes [#331](https://github.com/unit-mesh/auto-dev/issues/331)
* **plan:** enhance plan structure and verification steps [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([c6c1210](https://github.com/unit-mesh/auto-dev/commit/c6c1210018a13e9dbbbaaf7aacda192fdbaf2139))
* **settings:** add JsonTextProvider import to settings components [#332](https://github.com/unit-mesh/auto-dev/issues/332) ([a4ff169](https://github.com/unit-mesh/auto-dev/commit/a4ff169a62bbce93d35b2a674d263db8cc242976))
* **sketch:** add command transpilation and tool integration [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([77c49e6](https://github.com/unit-mesh/auto-dev/commit/77c49e6f373eac6acb44760ddb9b6a9fe7b887a3))
* **sketch:** add plan prompt support for Sketch AI ([c9779b9](https://github.com/unit-mesh/auto-dev/commit/c9779b9108361ae3190322f01e7f8f56e7294002))
* **test:** add TestAgentObserver for test failure handling [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([a718b80](https://github.com/unit-mesh/auto-dev/commit/a718b80fd3d0b63157201a9ad2f0f7e04207a788))
* **toolWindow:** add AutoDevPlaner tool window ([e33c82b](https://github.com/unit-mesh/auto-dev/commit/e33c82b7942e62c45c994d289fa59ce4868f7982)), closes [#223](https://github.com/unit-mesh/auto-dev/issues/223)
* **toolwindow:** add test failure observer to AutoDev tool window [#259](https://github.com/unit-mesh/auto-dev/issues/259) ([9fdcb82](https://github.com/unit-mesh/auto-dev/commit/9fdcb829872a9da3e3916200e740ebd5f839b7a1))
* **ui:** add execute task button to ThoughtPlanSketchProvider [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([5b1be1a](https://github.com/unit-mesh/auto-dev/commit/5b1be1a1aab5ee4750e0754865b9f47b3597b40d))
* **ui:** add toolbar and popup for ThoughtPlanSketch [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([0ddbc88](https://github.com/unit-mesh/auto-dev/commit/0ddbc88a4bf3ebdf1a0f6114dec27178b0638ae6))
* **ui:** enhance section status handling and UI updates [#331](https://github.com/unit-mesh/auto-dev/issues/331) ([45bf139](https://github.com/unit-mesh/auto-dev/commit/45bf139ec5332343aafdeedd74d8cdc1ef7e9d7f))


### Reverts

* Revert "refactor(terminal): replace terminal initialization logic #259" ([62032e0](https://github.com/unit-mesh/auto-dev/commit/62032e0577892ab8a9e73ee4189506d7e3dc9c67)), closes [#259](https://github.com/unit-mesh/auto-dev/issues/259)



# [2.0.0-rc.2](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-rc.1...v2.0.0-rc.2) (2025-03-12)


### Bug Fixes

* **DevInsCompiler:** ensure TOOLCHAIN_COMMAND is included in command execution flow ([b8c2b8d](https://github.com/unit-mesh/auto-dev/commit/b8c2b8dfd0e4e9a3ec6ebb1ced141edf5477e79e))


### Features

* **database:** add MCP tool for fetching database schema [#330](https://github.com/unit-mesh/auto-dev/issues/330) and closed [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([f9a642d](https://github.com/unit-mesh/auto-dev/commit/f9a642dea9b867f41f87f36022ada4fce7529258))
* **java:** include class inheritors in related classes lookup ([02c539e](https://github.com/unit-mesh/auto-dev/commit/02c539e42278fbca57114fd5e5030b34f0007317))
* **mcp:** add MCP client and configuration support [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([8145db8](https://github.com/unit-mesh/auto-dev/commit/8145db8666cc1f30962de78c6151466eb7b15641))
* **mcp:** add MCP server support and configuration [#33](https://github.com/unit-mesh/auto-dev/issues/33)- ([fb53c52](https://github.com/unit-mesh/auto-dev/commit/fb53c522c94d3916122c95907b32c1456a8ea620))
* **mcp:** add McpFunctionProvider and enhance tool handling [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([2d91b91](https://github.com/unit-mesh/auto-dev/commit/2d91b910b340c4f19888bab7371f81bdeaf834ef))
* **mcp:** add MCPService for handling MCP tool requests [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([caacabb](https://github.com/unit-mesh/auto-dev/commit/caacabbdaa7311f112a70f4af755250f44ac5463))
* **mcp:** add McpTool extension point and built-in tools [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([e970fbb](https://github.com/unit-mesh/auto-dev/commit/e970fbbaac829b4b5bea7e270f95978726d950f4))
* **mcp:** add tools for finding commits by message and retrieving VCS status ([03cd25f](https://github.com/unit-mesh/auto-dev/commit/03cd25f5f39ed3919c28ee5134e8dba72ba8faa2))
* **mcp:** enhance MCP tool execution and documentation [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([5b955c3](https://github.com/unit-mesh/auto-dev/commit/5b955c37eba7f26498657c3c056075c4f108982d))
* **mcp:** enhance tool execution with JSON argument parsing [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([4acdadd](https://github.com/unit-mesh/auto-dev/commit/4acdadd9bf8c692f2b4633ed7ef5f0bf64eb89e0))
* **mcp:** skip validation if MCP server is disabled [#330](https://github.com/unit-mesh/auto-dev/issues/330) ([4f7b3d1](https://github.com/unit-mesh/auto-dev/commit/4f7b3d1a4caccfd329dc09d2c4940f6a355de64c))
* **search:** display total results in search output ([0afdd8f](https://github.com/unit-mesh/auto-dev/commit/0afdd8f85ac267d0b09ad19d80800eb9d7a1a37a))
* **ui:** add copy action to message view toolbar ([6571cc5](https://github.com/unit-mesh/auto-dev/commit/6571cc52902f21339890d0d71d0f974660051b55))



# [2.0.0-rc.1](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.7...v2.0.0-rc.1) (2025-03-11)


### Bug Fixes

* **chat:** handle index not ready exception with notification ([25d8df6](https://github.com/unit-mesh/auto-dev/commit/25d8df61c639aa56041d2bc6dea9582dfb76fa04))
* **container:** handle Docker server errors and refactor build data creation [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([0c82a15](https://github.com/unit-mesh/auto-dev/commit/0c82a15dce92e612b3e92c9f06d3311c7b785185))
* **java:** handle null text in method signature builder [#324](https://github.com/unit-mesh/auto-dev/issues/324) ([b3f84f3](https://github.com/unit-mesh/auto-dev/commit/b3f84f3115e4e57f0e9386fd5db03fb27ac0ed28))
* **test:** ensure editor selection after file opening [#324](https://github.com/unit-mesh/auto-dev/issues/324) ([6ca9d01](https://github.com/unit-mesh/auto-dev/commit/6ca9d01d1d506e8942b9d2ba10f2be4cb5a3654e))
* **ui:** handle URL scheme error in LLM provider [#326](https://github.com/unit-mesh/auto-dev/issues/326) ([12ac068](https://github.com/unit-mesh/auto-dev/commit/12ac0681db55ecd875f6f80bfa2ba8c8744d2472))
* **vcs:** handle exceptions and improve diff processing ([8066d38](https://github.com/unit-mesh/auto-dev/commit/8066d38a1bccb619698a9f3177c6a861f2cb930e))


### Features

* **chat:** add IDE version context provider ([bc7f724](https://github.com/unit-mesh/auto-dev/commit/bc7f72424709c3f2e9a77515c346a586eea3c7e0))
* **chat:** implement new chat action and UI enhancements ([659afd5](https://github.com/unit-mesh/auto-dev/commit/659afd5910c623579e41c8dd512a8859d56d0180))
* **chat:** trigger onFinish callback after message completion [#329](https://github.com/unit-mesh/auto-dev/issues/329) ([ad390a3](https://github.com/unit-mesh/auto-dev/commit/ad390a3c9a72cd1c9263c9896112b209d0de79a8))
* **container:** add action creation for Docker servers [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([31479d9](https://github.com/unit-mesh/auto-dev/commit/31479d91bbf182a4593bafd89cd611df2ea4bd35))
* **container:** add Docker deployment UI and context creation ([84d4f44](https://github.com/unit-mesh/auto-dev/commit/84d4f443cdce742948bc4ab2386c88923c0ef278)), closes [#306](https://github.com/unit-mesh/auto-dev/issues/306)
* **container:** add RunDevContainerService for devcontainer.json support [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([72fa299](https://github.com/unit-mesh/auto-dev/commit/72fa299a3df4bb4ee333af97abd0fdb4b82f3fdf))
* **container:** add support for baseline version 242 in RunDevContainerService [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([93ef6e5](https://github.com/unit-mesh/auto-dev/commit/93ef6e538c171933d1415979019477214b9a6761))
* **markdown:** add Markdown preview support ([23f41bc](https://github.com/unit-mesh/auto-dev/commit/23f41bc369e38054a2c8f7d4b9ce0636498be32d))
* **markdown:** add theme-aware styling for code blocks ([578231c](https://github.com/unit-mesh/auto-dev/commit/578231cb5f9d112cc6b0facc34d3be4d17b15276))
* **run-action:** add Dockerfile support and i18n for run action [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([3c0560c](https://github.com/unit-mesh/auto-dev/commit/3c0560c9c38c83d0bcd5bc2d2e4171af5309e94d))
* **snippet:** enhance JSON handling for devcontainer files [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([f45c822](https://github.com/unit-mesh/auto-dev/commit/f45c822e1b3a99f72f9e9724e15f91668abe2799))



# [2.0.0-beta.7](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.6...v2.0.0-beta.7) (2025-03-06)


### Bug Fixes

* **llm:** handle empty custom LLMs gracefully ([063c304](https://github.com/unit-mesh/auto-dev/commit/063c304ccbfed13d72f90ea7a3525713161f3b32))


### Features

* **compatibility:** add 223 compatibility resources [#327](https://github.com/unit-mesh/auto-dev/issues/327) ([b76d2ab](https://github.com/unit-mesh/auto-dev/commit/b76d2aba9cc79cd8ec87128032049f6cc300cc75))
* **docker:** add Docker connection and runtime support [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([dd0adfc](https://github.com/unit-mesh/auto-dev/commit/dd0adfc2d3b56e8d27d5c17fa65c1157c5801f5a))



# [2.0.0-beta.6](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.5...v2.0.0-beta.6) (2025-03-06)


### Bug Fixes

* **bridge.vm:** update api endpoints output format ([8199655](https://github.com/unit-mesh/auto-dev/commit/8199655c16c3ac0833540f752e6f2444b510c572))
* **bridge.vm:** update migration plan comments to reflect user updates ([e70e1c0](https://github.com/unit-mesh/auto-dev/commit/e70e1c0a6e6f91b46390c3b3bc288e14de9ee5f9))
* **compiler:** adjust directory listing logic for depth control [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([8e2e2c0](https://github.com/unit-mesh/auto-dev/commit/8e2e2c025e0ffaaf9452b1188406c535336c76fb))
* **compiler:** log toolchain execution errors ([4f3ab88](https://github.com/unit-mesh/auto-dev/commit/4f3ab8872eb272470619fa588edd661722154eeb))
* **database:** add timeout to SQL execution future ([9c7612f](https://github.com/unit-mesh/auto-dev/commit/9c7612f5f261ffe4a0ac93a86ad6743ee9d00cfb))
* **editor:** add error handling for text updates and dependencies ([d61e083](https://github.com/unit-mesh/auto-dev/commit/d61e0838199d128d58336c830ed390a998f1735e))
* **endpoints:** handle exceptions in WebApiViewFunctionProvider ([d947ee5](https://github.com/unit-mesh/auto-dev/commit/d947ee5b0f5cbe6cf95bb0df18362840d54cae37))
* **endpoints:** handle progress indicator for async API collection ([eafcf83](https://github.com/unit-mesh/auto-dev/commit/eafcf837ef3472eecb7a5df73ef6c271b3fe5fad))
* **knowledge:** format code snippet output with newline ([5f34372](https://github.com/unit-mesh/auto-dev/commit/5f343725eea6857eb17149f0bf1b8d3de848c3c6))
* **knowledge:** handle invalid API format with file lookup [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([8c8ade1](https://github.com/unit-mesh/auto-dev/commit/8c8ade1edc29cb17f9f4186c376d1f7509996abe))
* **knowledge:** wrap file path access in runReadAction ([0979670](https://github.com/unit-mesh/auto-dev/commit/09796704ab34d60e94b05e429eead7d43516b82a))
* **lexer:** handle empty text segments in DevInLexer ([9035f46](https://github.com/unit-mesh/auto-dev/commit/9035f46d73ba5af50b46d2f85bb0e9ea6df8a7c8))
* **lexer:** handle special chars in text segments ([6beb383](https://github.com/unit-mesh/auto-dev/commit/6beb383a61069b8a1531be1a1f8245b76ca5ecb2))
* **sketch:** handle empty and newline inputs in SketchInputListener ([b6f986d](https://github.com/unit-mesh/auto-dev/commit/b6f986d171cd219f6c1600310446001c8ac462f4))
* **sketch:** handle empty and newline inputs in SketchInputListener ([d22f0d3](https://github.com/unit-mesh/auto-dev/commit/d22f0d3b17dca35b4f13dbe72d8351a71f31a5c7))
* **ui:** change error notification to warning for empty patches ([b7d6b2a](https://github.com/unit-mesh/auto-dev/commit/b7d6b2a905da91be14c5416b129107854e50592b))
* **web-api:** format web API endpoints output with code block ([6b07457](https://github.com/unit-mesh/auto-dev/commit/6b07457af5439c36a0d7e0110070caef484b6382))


### Features

* **archview:** add methods and slots to UiComponent ([e180831](https://github.com/unit-mesh/auto-dev/commit/e1808315fceec0cf203d260c9b4a7e5f33934991))
* **archview:** enhance component view output with count ([38b3369](https://github.com/unit-mesh/auto-dev/commit/38b33691735fa5f785350b95cb09ea1c94b6d804))
* **archview:** format module list output as code block ([1fef0f6](https://github.com/unit-mesh/auto-dev/commit/1fef0f621f9c09a0fda16738945495fd79b03815))
* **compiler:** add directory compression for deep nesting [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([0bdb5e6](https://github.com/unit-mesh/auto-dev/commit/0bdb5e6f471dc3c13145e92ed7e475c5c14c3859))
* **compiler:** add parallel directory node support [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([20025bd](https://github.com/unit-mesh/auto-dev/commit/20025bd3e3e06ddf3e4d1594059f0290ab6a96ff))
* **compiler:** enhance DirInsCommand with smart depth control and compact mode ([b433e8d](https://github.com/unit-mesh/auto-dev/commit/b433e8d5dd69efc84c67688b569d094f7ccf1512))
* **compiler:** limit directory listing depth to 2 ([d3637f6](https://github.com/unit-mesh/auto-dev/commit/d3637f684dfd9e2619122e7bdd27ab5b1bb1cf96))
* **completion:** add toolchain command completion support [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([d83285e](https://github.com/unit-mesh/auto-dev/commit/d83285e0c534ae878f0fe393a301156eb3b68d83))
* **component-view:** add mode support for component collection ([295ad13](https://github.com/unit-mesh/auto-dev/commit/295ad13f5a98ac34ae528f79ad7ad13494442d94))
* **endpoints:** add callee lookup for related classes [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([93756d3](https://github.com/unit-mesh/auto-dev/commit/93756d3622e73d839e3896b2cd2dd0a0a355ad6d))
* **endpoints:** add EndpointKnowledgeWebApiProvider for API call tree lookup [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([6eab126](https://github.com/unit-mesh/auto-dev/commit/6eab126109d82842e08c626f9e83e7163df4bfe4))
* **endpoints:** add file name to web API endpoint output [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([685c29c](https://github.com/unit-mesh/auto-dev/commit/685c29c3caf9a83bb9d68537922ab218b5698397))
* **endpoints:** add WebApiView toolchain function [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([1129c14](https://github.com/unit-mesh/auto-dev/commit/1129c143242ff0c30dfde4c70f324f1a31e7cfd4))
* **endpoints:** enhance web API endpoint display format ([9e0969d](https://github.com/unit-mesh/auto-dev/commit/9e0969d3480ed6f7cd73d0f6bf7cb397a2e2bca8))
* **gradle:** detect Gradle DSL type in JavaBuildSystemProvider ([b464574](https://github.com/unit-mesh/auto-dev/commit/b464574e56fbc4e639119a6a1a5aecbe44be7b5a))
* **javascript:** add callee lookup for JS functions [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([eacc191](https://github.com/unit-mesh/auto-dev/commit/eacc191173b907f44abea5792d0705abfaababd7))
* **javascript:** add JavaScript type resolver and related class provider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([7fd9580](https://github.com/unit-mesh/auto-dev/commit/7fd958005f62110006de5585d0e4267ab8d460f8))
* **knowledge:** add KnowledgeWebApiProvider extension [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([696d1a7](https://github.com/unit-mesh/auto-dev/commit/696d1a793c0e7172a9650d7acc22f99e85e533b4))
* **knowledge:** add support for additional HTTP methods ([a8ac29d](https://github.com/unit-mesh/auto-dev/commit/a8ac29d52ba7d3006d88d97ad7dddbbdef0f6eb8))
* **knowledge:** init KnowledgeFunctionProvider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([52f2449](https://github.com/unit-mesh/auto-dev/commit/52f2449a4595da45e8fbfaa536d2e1b096730ea6))
* **kotlin:** add callee lookup for Kotlin functions [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([55ad595](https://github.com/unit-mesh/auto-dev/commit/55ad595faca8f947517f2d6c35f47af7da368676))
* **sketch:** add toolchain function support in DevIns [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([be0e071](https://github.com/unit-mesh/auto-dev/commit/be0e07142de23ef9238c61a13636498a7c403370))
* **toolchain:** add toolchain icon and improve command completion [#307](https://github.com/unit-mesh/auto-dev/issues/307) ([6b3e887](https://github.com/unit-mesh/auto-dev/commit/6b3e887442d3cc30c3bbdccfc92102125ed2c398))



# [2.0.0-beta.5](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.4...v2.0.0-beta.5) (2025-03-01)


### Bug Fixes

* **code:** adjust migration plan logic and message handling [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([1ba749a](https://github.com/unit-mesh/auto-dev/commit/1ba749a5f7075465158d4a6218570b565f0f026f))
* **core:** update directory traversal for project migration [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([9be0b9c](https://github.com/unit-mesh/auto-dev/commit/9be0b9caa461fa2fcf2e0efb818fb7032ea9254a))
* **llms:** correct condition for plan usage in CustomLLMProvider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([d8b8c0d](https://github.com/unit-mesh/auto-dev/commit/d8b8c0db80c3103f8297787fba83eda73b649002))
* **patch:** handle null PatchReader and filePatches [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([9a5f44b](https://github.com/unit-mesh/auto-dev/commit/9a5f44b1d68ff9a25ea8183217bbfae6ea299cf3))
* **templates:** adjust code block formatting and tool instructions [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([35028e0](https://github.com/unit-mesh/auto-dev/commit/35028e004fade06aa780ae6493090bdff3ce8e54))


### Features

* **bridge:** add PATCH tool and update dependency refresh logic [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([0524922](https://github.com/unit-mesh/auto-dev/commit/052492225d08548fb73432cd048344cbf5d74065))
* **bridge:** enhance module info display and migration guidance [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([e912eeb](https://github.com/unit-mesh/auto-dev/commit/e912eeb5cd49c5512de9a301705ca8dbe18e5961))
* **bridge:** enhance project module info display and migration guidance [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([628ea62](https://github.com/unit-mesh/auto-dev/commit/628ea62fb82a6fbc68c16e3c6701e43e975bb7e5))
* **bridge:** enhance SCC function provider and documentation [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([e8399e9](https://github.com/unit-mesh/auto-dev/commit/e8399e9cf011759bf07b57121eabf50ba6c1c4ec))
* **bridge:** refine toolset and enhance dependency output formatting [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([c971e02](https://github.com/unit-mesh/auto-dev/commit/c971e02380a8edcc0b8f73df1045f9b4d221c186))
* **compiler:** add notification for missing commands [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([91ca175](https://github.com/unit-mesh/auto-dev/commit/91ca1753e2c1c8c65dcce5c9f200097a4fcd6289))
* **config:** enhance JSON schema with password format and language injection [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([7501077](https://github.com/unit-mesh/auto-dev/commit/75010770041e20ae85e058b71e0e9de2d689c401))
* **sketch:** add thinking text display in SketchToolWindow [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([80e8ea1](https://github.com/unit-mesh/auto-dev/commit/80e8ea1ae1e71148c0947aea5793f36d69b16b92))



# [2.0.0-beta.4](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.3...v2.0.0-beta.4) (2025-02-27)


### Bug Fixes

* **archview:** remove leading slash in WebApi command [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([cd724e3](https://github.com/unit-mesh/auto-dev/commit/cd724e37e010c01c9ccca10192702bb6fb0d68b0))
* **bridge:** correct file opening parameters in StructureCommandUtil [#319](https://github.com/unit-mesh/auto-dev/issues/319) ([40528eb](https://github.com/unit-mesh/auto-dev/commit/40528eb7bfca5e6b830625cee6f4bd1cac7c19e2))
* **container:** refactor DockerContextProvider to use PsiFileImpl [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([c17776b](https://github.com/unit-mesh/auto-dev/commit/c17776b3daf8558922556dff4cf21ed9742627c7))
* **core:** prevent empty input processing in SketchInputListener ([95a0088](https://github.com/unit-mesh/auto-dev/commit/95a008864b7887477764323cdb0565d4a3b78a97))
* **core:** simplify invokeLater call in SketchToolWindow [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([49b3ee1](https://github.com/unit-mesh/auto-dev/commit/49b3ee1bf9cca34bd878f981bd152a7307ef1927))
* **docker:** update FROM regex to support platform and alias syntax [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([67e44de](https://github.com/unit-mesh/auto-dev/commit/67e44deb305c6018ed69ee2196fee841a58b2d58))
* **java:** handle null newTestMethod in JavaCodeModifier [#312](https://github.com/unit-mesh/auto-dev/issues/312) ([00db051](https://github.com/unit-mesh/auto-dev/commit/00db051dd4b0c0185d2a9c7d6aab2538d62aa36f))
* **llm-provider:** correct initial message check and add document listener [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([699cc0a](https://github.com/unit-mesh/auto-dev/commit/699cc0a2f5e6f8663d9665c85c9c115f53a84d1b))
* **prompting:** improve language block handling in PromptOptimizer [#317](https://github.com/unit-mesh/auto-dev/issues/317) ([8ea5d35](https://github.com/unit-mesh/auto-dev/commit/8ea5d35b36c13b464a3b98e93ae72035b779c1d5))
* **provider:** add logging for model requests and extend modelType options [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([1020aa1](https://github.com/unit-mesh/auto-dev/commit/1020aa1cd15cf1bcbf9d9108518ac1e353808b99))
* **schema:** correct filename for custom LLM schema ([9ce0ae6](https://github.com/unit-mesh/auto-dev/commit/9ce0ae629be818224f5c90a7828a445cdbde53b7))
* **vue:** update file type for virtual files in VueUIComponentProvider ([bfaa587](https://github.com/unit-mesh/auto-dev/commit/bfaa587816e877c616cee67b39e300f326822c03))


### Features

* **assessment:** add SccFunctionProvider for SCC command [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([45fcf80](https://github.com/unit-mesh/auto-dev/commit/45fcf80fa0c9dd6a40cd4d0b14701d35a9198dc5))
* **assistant:** enhance legacy system migration guidance [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([809ab40](https://github.com/unit-mesh/auto-dev/commit/809ab408a57557a0ecc645552cc6e81bf5b27a42))
* **bridge:** add bridge.vm template and update bridge components  [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([b1d25cb](https://github.com/unit-mesh/auto-dev/commit/b1d25cb667c7f2f290e8090dbb0ef8a1668708d3))
* **bridge:** add BridgeToolProvider and update tool list formatting [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([e9c3fbc](https://github.com/unit-mesh/auto-dev/commit/e9c3fbcdaf58e97ca561293bd512c198791772c0))
* **bridge:** add BridgeToolWindow and enhance SketchToolWindow [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([76ca561](https://github.com/unit-mesh/auto-dev/commit/76ca561befa4718e43e7a5a26c20ae4d668163f8))
* **bridge:** add ComponentViewFunctionProvider and refactor BridgeCommandProvider [#319](https://github.com/unit-mesh/auto-dev/issues/319) ([3012be0](https://github.com/unit-mesh/auto-dev/commit/3012be003c78b81544ee24918a7c14ab63bfcc26))
* **bridge:** add StylingViewFunctionProvider for CSS file handling [#319](https://github.com/unit-mesh/auto-dev/issues/319) ([e435187](https://github.com/unit-mesh/auto-dev/commit/e4351873c2316cf5d4049aa6fdfea0bb19c42f8d))
* **chat:** add BRIDGE action type and translations [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([bb4be32](https://github.com/unit-mesh/auto-dev/commit/bb4be32a2056e54c7dc2c1fe25f103cdfaa62c75))
* **container:** add dev container support and integrate Docker gateway [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([a66f465](https://github.com/unit-mesh/auto-dev/commit/a66f465e2ab36ffe8184a30bfa7cbb5b2f22705e))
* **container:** add DockerContextProvider for chat context [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([d57bccb](https://github.com/unit-mesh/auto-dev/commit/d57bccbd470cc6f00c594fd43b6062b5238a1863))
* **container:** enhance Dockerfile service with Docker connection support [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([88654aa](https://github.com/unit-mesh/auto-dev/commit/88654aa580d3831f19547199bf172c10c34d2842))
* **core:** add JsonText and JsonPath param types [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([ffe2bb3](https://github.com/unit-mesh/auto-dev/commit/ffe2bb322c6c2c391d52f5eeef5816df5ed479df))
* **custom:** add AutoDevNotifications on non-empty output [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([f21ec5c](https://github.com/unit-mesh/auto-dev/commit/f21ec5ce9ee09886b4272acf7c735241e25c5b53))
* **docker:** add Docker support to 223 [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([b1e7a1b](https://github.com/unit-mesh/auto-dev/commit/b1e7a1b669916e25720dfc6a38175c4081d3a3ac))
* **docker:** add Dockerfile parser and enhance context provider [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([12f801c](https://github.com/unit-mesh/auto-dev/commit/12f801c6fec0e82bd7b59c8f2169b8b57a89764e))
* **docker:** enhance DockerContextProvider with regex and runReadAction [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([2649ffd](https://github.com/unit-mesh/auto-dev/commit/2649ffd5123845e34d9a6bf02a3fb06d184ed4bb))
* **docker:** implement DockerContextProvider and RunDockerfileService [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([b1a53d3](https://github.com/unit-mesh/auto-dev/commit/b1a53d357951b03a512f592e250c04aedea493e0))
* **editor:** implement right toolbar and preview layout for file editor ([dd53855](https://github.com/unit-mesh/auto-dev/commit/dd53855ddeff1602675307b194b0bfa0920edc4a))
* **ext-container:** enhance Docker context provider with error handling [#306](https://github.com/unit-mesh/auto-dev/issues/306) ([d382e71](https://github.com/unit-mesh/auto-dev/commit/d382e7142b15748aeb59d138965bf1c52a348274))
* **javascript:** add ReactUIComponentProvider for UI component collection [#319](https://github.com/unit-mesh/auto-dev/issues/319) ([5527d13](https://github.com/unit-mesh/auto-dev/commit/5527d13fd0cbbe41bf71620ca82607cd140c96c0))
* **knowledge:** add history function provider for knowledge transfer ([97bfac8](https://github.com/unit-mesh/auto-dev/commit/97bfac88439c322c711bd395da6905b70bbcb8a4))
* **openrewrite:** add OpenRewrite plugin integration [#319](https://github.com/unit-mesh/auto-dev/issues/319) ([78d7fed](https://github.com/unit-mesh/auto-dev/commit/78d7fed92fd4b6e9023f628451ab455215279c79))
* **plugins:** add Jupyter support and update plugin configurations ([bc86beb](https://github.com/unit-mesh/auto-dev/commit/bc86bebc72b15419c1f47cd03f1c50cc8da7a665))
* **prompting:** add PromptOptimizer to trim code spaces [#317](https://github.com/unit-mesh/auto-dev/issues/317) ([44d00c6](https://github.com/unit-mesh/auto-dev/commit/44d00c6f09e1cc61fda6fc2c014d7d5b13af389b))
* **prompting:** enhance trimCodeSpace to handle Python code [#316](https://github.com/unit-mesh/auto-dev/issues/316) ([e477111](https://github.com/unit-mesh/auto-dev/commit/e4771116fb99d8adac24cb5a433ddb988efb66b0))
* **scc:** add SCC wrapper for code analysis [#319](https://github.com/unit-mesh/auto-dev/issues/319) ([6155a78](https://github.com/unit-mesh/auto-dev/commit/6155a78a79349465e1fb1fe55beb99455c54d887))
* **schema:** add modelType field with predefined options to JSON schema [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([5092cc0](https://github.com/unit-mesh/auto-dev/commit/5092cc0b74e49d57557df3a9df68f975830dc3b2))
* **schema:** refactor and extend JSON schema providers [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([49d0bfe](https://github.com/unit-mesh/auto-dev/commit/49d0bfe751027ce2345adeedecf4e9c3b12b3ba7))
* **settings:** add trim code option before sending [#317](https://github.com/unit-mesh/auto-dev/issues/317) ([643bb41](https://github.com/unit-mesh/auto-dev/commit/643bb41d4ce16db45f49bc694ee69e2442594492))
* **toolchain:** add `funcNames` method to `ToolchainFunctionProvider` ([6bbed83](https://github.com/unit-mesh/auto-dev/commit/6bbed8375f13b8475dfcdb48becde9f740b1304b)), closes [#308](https://github.com/unit-mesh/auto-dev/issues/308)
* **tools:** enhance AgentTool and add new tool examples [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([4cc3dd8](https://github.com/unit-mesh/auto-dev/commit/4cc3dd87d4e8507351b5e77ac6e2ec38235a7fb1))
* **ui:** add custom action and documentation links [#271](https://github.com/unit-mesh/auto-dev/issues/271) ([2d8e8ba](https://github.com/unit-mesh/auto-dev/commit/2d8e8ba665b5098745647d5f8ef1dea076eb3fa8))



# [2.0.0-beta.3](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.2...v2.0.0-beta.3) (2025-02-19)


### Bug Fixes

* add binary check to AutoDevInputSection and VueRelatedClassProvider ([89608ae](https://github.com/unit-mesh/auto-dev/commit/89608ae55006d39fc12df48cbac25e7cfc4b56a0))
* **dependencies:** update file extension check and visibility logic [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([df10e23](https://github.com/unit-mesh/auto-dev/commit/df10e23f0d045bf54c16b42c3ae6c621867971ea))
* **editor:** adjust resize logic for EditorFragment ([e4e82e3](https://github.com/unit-mesh/auto-dev/commit/e4e82e38685ec9f303ca72ff37e1fbcf3823034d))
* **java:** simplify module data retrieval in JavaBuildSystemProvider ([5e82be8](https://github.com/unit-mesh/auto-dev/commit/5e82be8bde817e284e5a5defa0a87bb753082120))
* **java:** update module data retrieval in JavaBuildSystemProvider ([92a7f91](https://github.com/unit-mesh/auto-dev/commit/92a7f91ec0ee5f7f15e2fcddadc9135011398710))
* Remove redundant code and optimize tool window content creation ([3059127](https://github.com/unit-mesh/auto-dev/commit/3059127a540f2537f5a21d05e8393c9b21cc3452))
* Remove redundant code and optimize tool window content creation ([c110e0c](https://github.com/unit-mesh/auto-dev/commit/c110e0cf041e59db03f2a222bd2b1726e82511d9))
* remove unused import and adjust null checks ([d175671](https://github.com/unit-mesh/auto-dev/commit/d17567175b12edb305762b7be102d27249756ae7))
* **ripgrep:** handle binary lookup exceptions gracefully ([41357c5](https://github.com/unit-mesh/auto-dev/commit/41357c58eedd44d60ed3266b8443ac5a52a4e278))
* **runner:** replace with AutoDevNotifications for warnings ([ec280c8](https://github.com/unit-mesh/auto-dev/commit/ec280c8badec04c3265ea281633f5fbece3344f4))
* **search:** correct RIPgrep installation link formatting ([6a7acbe](https://github.com/unit-mesh/auto-dev/commit/6a7acbe52a953b49971bdf1cd32928b9ad4898fd))
* **sketch:** add project disposal check in CodeHighlightSketch [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([4206c36](https://github.com/unit-mesh/auto-dev/commit/4206c360149c4742646f2a8f1564f6f41050fa6a))
* **sketch:** add project disposal check in SketchInputListener ([1abd39f](https://github.com/unit-mesh/auto-dev/commit/1abd39faaf08734c8e8a92d7840efb837c5ea539))
* **sketch:** Add text selection to SketchToolWindow ([5b4dea6](https://github.com/unit-mesh/auto-dev/commit/5b4dea6ea21ad7928db409220d9857752d747653))
* **sketch:** optimize TerminalSketchProvider URL handling ([5cb68ba](https://github.com/unit-mesh/auto-dev/commit/5cb68ba6f3f8d927fbcfcc44d9bd5201beb10448))
* 修复错误 import ([7fa2e1f](https://github.com/unit-mesh/auto-dev/commit/7fa2e1fddfac123cc639069051529e5a5511fcdb))


### Code Refactoring

* remove system block and adjust related tests ([c60086e](https://github.com/unit-mesh/auto-dev/commit/c60086e6863a5878ae3f38400613c91b3d38e9a0))


### Features

* add dependencies check action [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([8eb88bf](https://github.com/unit-mesh/auto-dev/commit/8eb88bf3c6250582ba18d6c932565d4a8621afc1))
* add dependencies function provider and auto check [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([566d173](https://github.com/unit-mesh/auto-dev/commit/566d1738a93a92f5296d4fb7e2a4bfdcd8556f0c))
* add Swagger plugin support to Gradle properties ([4d29865](https://github.com/unit-mesh/auto-dev/commit/4d2986503b7c0f40ac43b945bec739546f67935a))
* add toolchain command support and dependencies function provider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([5953d61](https://github.com/unit-mesh/auto-dev/commit/5953d61b8ed6cd9541c0b656edbd6e46eb30faf5))
* **build:** add support for detecting package files in build systems [#316](https://github.com/unit-mesh/auto-dev/issues/316) ([ac41bb1](https://github.com/unit-mesh/auto-dev/commit/ac41bb18604ba9a3261e1dc695c972d765bd7aa7))
* **code-highlight:** add fileName support and improve write command [#208](https://github.com/unit-mesh/auto-dev/issues/208) ([4133f23](https://github.com/unit-mesh/auto-dev/commit/4133f23d98dee5d8c2dc2074b72b75e79239bcb1))
* **code:** default to markdown for code fences and preview ([4d0b823](https://github.com/unit-mesh/auto-dev/commit/4d0b82303c15d1c63ce21c9e5b420df15317b6ff))
* **commands:** add BROWSE to READ_COMMANDS and simplify BrowseInsCommand ([ec7a607](https://github.com/unit-mesh/auto-dev/commit/ec7a60784aeec264ec22e6edb7d584f3618d3515))
* **completion:** add STRUCTURE command to completion options ([cc6862a](https://github.com/unit-mesh/auto-dev/commit/cc6862a7cc71f8c6d52537f35c42b555ec0790f2))
* **dependencies:** add action to check dependencies [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([d78a46a](https://github.com/unit-mesh/auto-dev/commit/d78a46a646da3815e92d6c4edf54606a0c9a3213))
* **dependencies:** add dependency collection for Maven and Gradle [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([b784624](https://github.com/unit-mesh/auto-dev/commit/b7846249ff2a2cbd6b768f91d523e5a27d01e116))
* **dependencies:** add formatted dependency output to DependenciesFunctionProvider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([bb5439b](https://github.com/unit-mesh/auto-dev/commit/bb5439b18cf7aeeb9dd9135f93ded4f02c5c591d))
* **dependencies:** add project dependencies model and inspections [#316](https://github.com/unit-mesh/auto-dev/issues/316) ([16f8bdc](https://github.com/unit-mesh/auto-dev/commit/16f8bdc720db96def7c022728513af35521fa9ee))
* **editor:** add preview editor support to EditorFragment ([c07eee8](https://github.com/unit-mesh/auto-dev/commit/c07eee80474497db4ff999fa3dcd96bca7f85e53))
* **go:** add GoBuildSystemProvider for dependency management [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([bd5d893](https://github.com/unit-mesh/auto-dev/commit/bd5d8931d3416e2c9ccac54bc0ba613620083188))
* **go:** add GoLangPlaygroundSketchProvider ([1b9a5e6](https://github.com/unit-mesh/auto-dev/commit/1b9a5e68bbc8b056a41fc4a4435bacac03f0475c))
* **go:** implement module retrieval functionality in GoBuildSystemProvider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([76e0329](https://github.com/unit-mesh/auto-dev/commit/76e03298a724e80284fe040310c0a21d86082643))
* **inline-chat:** handle ESC key to close panel ([#302](https://github.com/unit-mesh/auto-dev/issues/302)) ([f8019d9](https://github.com/unit-mesh/auto-dev/commit/f8019d9788d97cb44ff63dd897e89e4c26415798)), closes [#301](https://github.com/unit-mesh/auto-dev/issues/301)
* **lint:** add tool ID to SketchInspectionError ([991cec8](https://github.com/unit-mesh/auto-dev/commit/991cec8b4d3cc8c0ef1730f2d2dfdcb09b720242))
* **lint:** add UI for displaying lint inspection errors [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([6640073](https://github.com/unit-mesh/auto-dev/commit/6640073c8eabae7aaa8939a38f01c538e5057631))
* **openapi:** add OpenAPISketchProvider support ([052952a](https://github.com/unit-mesh/auto-dev/commit/052952a70e1f759ebbe71c15190c9158b1f947bf))
* **preview:** set default layout for file editor preview ([6bc690f](https://github.com/unit-mesh/auto-dev/commit/6bc690f9732d11caf6490992940e21c709c28fec))
* **python:** add Python build system provider [#308](https://github.com/unit-mesh/auto-dev/issues/308) ([5ffcfa4](https://github.com/unit-mesh/auto-dev/commit/5ffcfa46a96d44ba462707892349cc59bf3985e2))
* **sketch:** Add interrupt handling and process listeners ([#300](https://github.com/unit-mesh/auto-dev/issues/300)) ([170e77f](https://github.com/unit-mesh/auto-dev/commit/170e77fc3f245971c188cd576344685671cdc78f))
* **sketch:** add localized lint error messages ([6f61f81](https://github.com/unit-mesh/auto-dev/commit/6f61f81c94b5ed53a84024ff99a107d8d49efafc))
* **sketch:** enhance inspection and error handling in PsiErrorCollector [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([df51262](https://github.com/unit-mesh/auto-dev/commit/df51262ac421a2c6e4434b4e1eddcbd0a276c136))
* **sketch:** enhance patch repair UI and add editor retrieval ([39d1e71](https://github.com/unit-mesh/auto-dev/commit/39d1e71114bfb8c67c11931c8c80d70280d49a9f))
* **structure:** enhance file structure retrieval and presentation [#309](https://github.com/unit-mesh/auto-dev/issues/309) ([561d646](https://github.com/unit-mesh/auto-dev/commit/561d6460449411e0d3c521111c6f2589b7804bad))
* **ui:** include file name in task progress display ([9733140](https://github.com/unit-mesh/auto-dev/commit/9733140d6a86b26e1afd4d324c66596a4ebc93d8))
* **vue:** add VueRelatedClassProvider and integrate Vue module [#309](https://github.com/unit-mesh/auto-dev/issues/309) ([0831873](https://github.com/unit-mesh/auto-dev/commit/083187309be8e8344494287e38138f6ff9518a39))
* **vue:** enhance Vue-related class provider and add tests [#309](https://github.com/unit-mesh/auto-dev/issues/309) ([2734789](https://github.com/unit-mesh/auto-dev/commit/2734789b2d7bac1f03228fefe1832d36dadc25a0))
* 多窗口, 优化输出不会刷新Process ([#297](https://github.com/unit-mesh/auto-dev/issues/297)) ([4c442df](https://github.com/unit-mesh/auto-dev/commit/4c442dfa97c2748dff136d0a81adf6aa4c399d82))


### BREAKING CHANGES

* remove `#` system API syntax support

This commit removes the `#` system API syntax support from the DevIn language,
introducing a breaking change. The following changes have been made:

1. **Grammar Changes**:
   - Removed the `SYSTEM_START` rule from `DevInParser.bnf`.
   - Commented out the `SYSTEM_BLOCK` state and related rules in `DevInLexer.flex`.
   - Updated the `TEXT_SEGMENT` regex to exclude `#` from valid characters.

2. **Compiler Changes**:
   - Removed the `SYSTEM_START` and `NUMBER` token handling from `DevInSyntaxHighlighter.kt`.
   - Updated the expected output in `DevInCompilerTest.kt` to reflect the removal of the system API syntax.

3. **Documentation Updates**:
   - Updated `devins-language.md` to reflect the removal of the `#` system API syntax, marking it as deprecated and removed in version 2.0.0.

**Impact**:
- Any existing code that relies on the `#` system API syntax will no longer work and will need to be refactored.
- Developers should update their code to use alternative mechanisms for interacting with third-party systems.

**Migration Guide**:
- Replace any usage of `#` system API syntax with alternative approaches, such as custom commands or agents.
- Update any tests or documentation that reference the `#` system API syntax.

This change is part of the effort to simplify the language and remove less commonly used features to improve maintainability and clarity.



# [2.0.0-beta.2](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-beta.1...v2.0.0-beta.2) (2025-02-11)


### Bug Fixes

* **parser:** filter empty code fences and handle normal devin blocks ([782e7c1](https://github.com/unit-mesh/auto-dev/commit/782e7c19aa9a558e33dfeafe2b5b5b5d682ad3e1))
* **sketch:** disable BuiltinCommand in sketch mode ([f7ccf6f](https://github.com/unit-mesh/auto-dev/commit/f7ccf6feabc014ca05333b3f470411f288384a81))
* update diff repair instructions in templates ([96a1c55](https://github.com/unit-mesh/auto-dev/commit/96a1c55f9bbfa943d9ef29219882a314560d9f00))
* **update:** comment out AutoDevUpdateStartupActivity and fixed [#291](https://github.com/unit-mesh/auto-dev/issues/291) ([8c914a8](https://github.com/unit-mesh/auto-dev/commit/8c914a8ae3d376d9b84ad7c837fa8e1f1dea08fa))


### Features

* **patch:** add repair button and refactor diff repair logic ([3b93202](https://github.com/unit-mesh/auto-dev/commit/3b932028041e7b2164db47031aa1c334df571d58))
* **patch:** simplify diff repair templates and remove examples ([18a7638](https://github.com/unit-mesh/auto-dev/commit/18a763878005562d4dadee0c0c2545470dd7ddb3))
* **sketch:** add BuiltinCommand for /write command handling ([0d40170](https://github.com/unit-mesh/auto-dev/commit/0d401708bac7e234928fe2e4c78a0721dabdeb6b))



# [2.0.0-beta.1](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.11...v2.0.0-beta.1) (2025-02-10)


### Bug Fixes

* **compiler:** add timeout and error handling for file lookup ([54aa1a6](https://github.com/unit-mesh/auto-dev/commit/54aa1a6bf1fb2ac8c0817b5c4ed0a75e5aa1d61c))
* **DatabaseFunctionProvider:** update comment style in schema result ([34b7ea1](https://github.com/unit-mesh/auto-dev/commit/34b7ea1f2f3d6552ee16682c14d2a14bae1fcf5d))
* **diff:** correct patch application logic and streamline file panel initialization ([50d90f4](https://github.com/unit-mesh/auto-dev/commit/50d90f47535b22e7d81e8cb69c1f4831602970cf))
* **editor:** lower minDevinLineThreshold to 1 and clear blockViews ([9401013](https://github.com/unit-mesh/auto-dev/commit/9401013f6d195bd0ef22f40f10682da3e9fff921))
* **editor:** update expand/collapse logic and threshold ([9d144e6](https://github.com/unit-mesh/auto-dev/commit/9d144e6838cca5cfa4d0a15f8565466f80cbfd79))
* **endpoints:** add EndpointsContextProvider for 223 ([469ab07](https://github.com/unit-mesh/auto-dev/commit/469ab076ca29aec3bf908e8b30d22787bf6e81d9))
* **EndpointsContextProvider:** simplify provider availability check ([638c73c](https://github.com/unit-mesh/auto-dev/commit/638c73c93221c169776c32db80413c0c91fe50d7))
* **http:** remove semicolon from JSON content type [#289](https://github.com/unit-mesh/auto-dev/issues/289) ([6695dfc](https://github.com/unit-mesh/auto-dev/commit/6695dfc252695715e223e36488d9796423dee728))
* **JavaMethodContextBuilder:** handle null method in getSignatureString ([e275a30](https://github.com/unit-mesh/auto-dev/commit/e275a30488113a090e404481d9fb58ac749bf553))
* **llms:** convert request content to byte array [#289](https://github.com/unit-mesh/auto-dev/issues/289) and related to square/okhttp[#3081](https://github.com/unit-mesh/auto-dev/issues/3081) ([6e17816](https://github.com/unit-mesh/auto-dev/commit/6e17816ea32abf4855f669b5c8823c4ab7856afb))
* **llms:** update RequestBody creation for byte array handling [#289](https://github.com/unit-mesh/auto-dev/issues/289) ([3b900d6](https://github.com/unit-mesh/auto-dev/commit/3b900d692e5703f80f413cb240a90eba10f93463))
* **parser:** add error logging in CodeBlockElement ([76aafb8](https://github.com/unit-mesh/auto-dev/commit/76aafb8b9b08f4b5db1cfa92f69d24fc41a2b218))
* **parser:** add null check for devinEnd in CodeFence ([d41714d](https://github.com/unit-mesh/auto-dev/commit/d41714d7436418fefaa7b447332e62bb161262cf))
* **parser:** change error log to warn in CodeBlockElement ([06b4afb](https://github.com/unit-mesh/auto-dev/commit/06b4afb8b9d7cea16fa1ba1680c707dbc3e63306))
* **parser:** enhance error handling for nested devin blocks in CodeFence ([3cf624d](https://github.com/unit-mesh/auto-dev/commit/3cf624d7caa4d729d771b399e657e8ef15cdac76))
* **parser:** handle multiple devin code blocks and improve parsing logic ([ea4a601](https://github.com/unit-mesh/auto-dev/commit/ea4a6010da11a526f4746a08a0a0a69ce5cb93d3))
* **parser:** handle special case for devin tags in CodeFence ([d74571d](https://github.com/unit-mesh/auto-dev/commit/d74571dcc26f3287b250bf81016106ccbd42d662))
* **parser:** optimize error handling for devin tags in CodeFence ([f3c86e1](https://github.com/unit-mesh/auto-dev/commit/f3c86e1d276fecce24b8c45576de463f8d9da840))
* **parser:** simplify obtainFenceContent in CodeBlockElement ([47f23c9](https://github.com/unit-mesh/auto-dev/commit/47f23c97fe89bd6a529ef1232c1a794301b78f08))
* **refactoring:** log errors during element deletion ([04c0136](https://github.com/unit-mesh/auto-dev/commit/04c0136b360e9164282511e484c7790e7fceb9a4))
* **sketch:** adjust line thresholds and optimize error handling [#288](https://github.com/unit-mesh/auto-dev/issues/288)``` ([4e03bcb](https://github.com/unit-mesh/auto-dev/commit/4e03bcba231b9ecaafdda481b0bc971c60719e5e))
* **sketch:** clear historyPanel on reset ([0b54ec6](https://github.com/unit-mesh/auto-dev/commit/0b54ec6f31c92ff08128eb8c1de0c941b6db85ee))
* **sketch:** refine context information prompts and logging in language injector ([91bb330](https://github.com/unit-mesh/auto-dev/commit/91bb330807b4688c08767fdf084678a4034bb55f))
* **sketch:** remove rollback button and update lint error label [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([571b9ae](https://github.com/unit-mesh/auto-dev/commit/571b9ae0418baebed8527b27c5c127927dbc6845))
* **sketch:** simplify build tool string format in SketchRunContext ([4af9246](https://github.com/unit-mesh/auto-dev/commit/4af924607a49e1aab5adf7c7c193324d8687f355))
* **ui:** add repair button and improve context handling in DiffLangSketch ([2e306f4](https://github.com/unit-mesh/auto-dev/commit/2e306f4c66542313dd66859c4f06fc9f7e681ad0))
* **ui:** add vfile validity check in AutoDevInputSection ([f2e7363](https://github.com/unit-mesh/auto-dev/commit/f2e7363bba656c8ac0ea20eefa856fbf9c67cc45))
* **ui:** enhance null check and add isNotNullOrEmpty method in CodeHighlightSketch ([026c004](https://github.com/unit-mesh/auto-dev/commit/026c004b7efa221f1fbb9cb9c5bc5b65559619b2))
* **ui:** enhance null check in CodeHighlightSketch init ([c95c931](https://github.com/unit-mesh/auto-dev/commit/c95c931ce3fe1af36dfc15268fc42ab4ce8e201c))
* **ui:** handle disposed editors in focusLost event and closed [#289](https://github.com/unit-mesh/auto-dev/issues/289) ([5d49245](https://github.com/unit-mesh/auto-dev/commit/5d492455686a01fccca195d352e855d616fcfb2d))
* **ui:** improve line numbers visibility check in CodeHighlightSketch ([20b3524](https://github.com/unit-mesh/auto-dev/commit/20b3524ab919ea3305c4046da9f48423c99748b1))
* **ui:** prevent operations on disposed editors in CodeHighlightSketch ([7d9d908](https://github.com/unit-mesh/auto-dev/commit/7d9d908273e4388f5546265c49b32cd35c577b11))
* **ui:** remove redundant editor operations in DiffLangSketch ([2e2748c](https://github.com/unit-mesh/auto-dev/commit/2e2748c655d9e4f530d91dc88d7a4fff34415df3))


### Features

* **code-highlight:** add /write command support for DevIn [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([061c13a](https://github.com/unit-mesh/auto-dev/commit/061c13aaa730184e491f50d0c00d62c214074ddd))
* **endpoints:** add endpoints plugin support ([dba4c63](https://github.com/unit-mesh/auto-dev/commit/dba4c63a680e8276ae230079f879b7061aec14d1)), closes [#287](https://github.com/unit-mesh/auto-dev/issues/287)
* **endpoints:** add Spring support and refine endpoint detection [#287](https://github.com/unit-mesh/auto-dev/issues/287) ([682642f](https://github.com/unit-mesh/auto-dev/commit/682642f198d0c95124f9856f5170239dd921b173))
* **endpoints:** update provider to use new API and filter available providers [#287](https://github.com/unit-mesh/auto-dev/issues/287) ([7c60472](https://github.com/unit-mesh/auto-dev/commit/7c604728416ccd92a64838e90057b95eca6e5e0c))
* Enhance context checks and tool execution in sketch.vm ([7dbd0e6](https://github.com/unit-mesh/auto-dev/commit/7dbd0e67780664aa5b5714c1dd73eaada777f50a))
* **javascript:** add framework version ([42ab37b](https://github.com/unit-mesh/auto-dev/commit/42ab37be3e239dce9aa5c762cecbc00f79f7e881))
* **lint:** add PsiSyntaxCheckingVisitor and PsiErrorCollector for syntax linting [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([ddd5be7](https://github.com/unit-mesh/auto-dev/commit/ddd5be72ddff55111cfbd665004f156be698ecde))
* **ripgrep:** add support for locating rg in /usr/local/bin on macOS ([ad2286f](https://github.com/unit-mesh/auto-dev/commit/ad2286f269f00a21918c2e28d7533d2dc6b92839))
* **sketch:** add history panel for request prompts ([258020f](https://github.com/unit-mesh/auto-dev/commit/258020f5a7b91ea4a59fa33da26f7711d0e5b45e))
* **sketch:** add OpenAPI support and rename SingleFileDiffView [#287](https://github.com/unit-mesh/auto-dev/issues/287) ([a460231](https://github.com/unit-mesh/auto-dev/commit/a4602314cb9f1f06eff6db24693c46fd951f87e2))
* **sketch:** enhance updateViewText with complete flag and lint checks [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([df25b1b](https://github.com/unit-mesh/auto-dev/commit/df25b1b4efe5bd7370b9c38e65f278ac51531fc0))
* **sketch:** enhance updateViewText with complete flag and lint checks [#288](https://github.com/unit-mesh/auto-dev/issues/288) ([83d9b97](https://github.com/unit-mesh/auto-dev/commit/83d9b975d414a9fcc5fc7e62cc7072537ceef97e))



# [2.0.0-alpha.11](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.10...v2.0.0-alpha.11) (2025-02-07)


### Bug Fixes

* Add Disposable to CodeHighlightSketch and optimize Terminal UI ([ac386fd](https://github.com/unit-mesh/auto-dev/commit/ac386fd9ccba909f4293bc20f58cf9ca7481c378))
* Add lang dependency, enable startup activity, and define new extension points [#283](https://github.com/unit-mesh/auto-dev/issues/283) ([de6c499](https://github.com/unit-mesh/auto-dev/commit/de6c499b544bb1611ac50d5cd9ea23f4e516793c))


### Features

* Add script path to run config and enhance dir listing checks ([ffa8030](https://github.com/unit-mesh/auto-dev/commit/ffa8030b0fbcc815c8ee5bd32747d7c87a69d53b))
* **diff:** enhance SingleFileDiffView with editor param and optimize stream handling ([a44f4ea](https://github.com/unit-mesh/auto-dev/commit/a44f4ea6eae4d1ff440c4d677cdb3825cdf04959))
* **diff:** enhance SingleFileDiffView with editor param and optimize stream handling``` ([0e32fd1](https://github.com/unit-mesh/auto-dev/commit/0e32fd16a56b21848046db31d61a03e1632ffc4b))
* Enhance terminal UI with new icons and button styles [#265](https://github.com/unit-mesh/auto-dev/issues/265) ([da9527a](https://github.com/unit-mesh/auto-dev/commit/da9527a4c4ab1b40da73b04d4b4999e61da61b96))
* Enhance WebView buttons with cursor and border styles ([601e0e4](https://github.com/unit-mesh/auto-dev/commit/601e0e4c4973d27d29e778213334d419c9ba87f0))
* **javascript:** detect build tool and framework dynamically ([d24b458](https://github.com/unit-mesh/auto-dev/commit/d24b458746f382c2d2d4739f1806c2f2b531f254))
* Update UI elements with new icons and improved button styles ([3ae99ad](https://github.com/unit-mesh/auto-dev/commit/3ae99ad82fdf8ae318152d1f5e7ae46db1c29073))



# [2.0.0-alpha.10](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.8...v2.0.0-alpha.10) (2025-02-06)


### Bug Fixes

* **chat:** ensure file is added with force flag in selectionChanged ([811e9c3](https://github.com/unit-mesh/auto-dev/commit/811e9c36a1e3ff6a599b7440b76a0cf8ae0db39b))
* **diff:** handle edge case in line highlighting ([8e848c6](https://github.com/unit-mesh/auto-dev/commit/8e848c6ad9f2c252afa0e53cc19c4e17a083c3d3))
* **terminal:** change "Send to Sketch" button text to "Send" ([695b552](https://github.com/unit-mesh/auto-dev/commit/695b55262287436573797c64761a9ebba951111c))
* **ui:** remove foreground color changes in SingleFileDiffView ([9f8689e](https://github.com/unit-mesh/auto-dev/commit/9f8689e87c92af800740c1d31f33cc87c1282872))


### Features

* **docs:** add custom AI Composer guide and update nav_order ([d015fa6](https://github.com/unit-mesh/auto-dev/commit/d015fa69278d444e245cf8a79960f84315e02526))
* **terminal:** add dynamic title label to terminal widget ([60c78a0](https://github.com/unit-mesh/auto-dev/commit/60c78a009d2010e01f354c5e3ffc9517f58bf220))
* **terminal:** add webview for local server in TerminalSketchProvider [#265](https://github.com/unit-mesh/auto-dev/issues/265) ([4ea9609](https://github.com/unit-mesh/auto-dev/commit/4ea96098e049e0f63e15d0d01ea598572fc6604b))
* **ui:** add scroll pane to elementsList in AutoDevInputSection ([69d0642](https://github.com/unit-mesh/auto-dev/commit/69d0642cc1ed3cad45234fd1b9f5503c4706ca6b))



# [2.0.0-alpha.8](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.7...v2.0.0-alpha.8) (2025-01-25)


### Bug Fixes

* **snippet:** handle null scratch file creation ([e42b0ff](https://github.com/unit-mesh/auto-dev/commit/e42b0ff96af70a7cc6895694bc2191a1f19f25db))
* **terminal:** use detected shell for script execution ([fc09faf](https://github.com/unit-mesh/auto-dev/commit/fc09fafb14cca28fe9b1ab294f231e5920bcc4d1))



# [2.0.0-alpha.7](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-alpha.6...v2.0.0-alpha.7) (2025-01-24)


### Bug Fixes

* **commands:** disable certain builtin commands by default ([3c00fa1](https://github.com/unit-mesh/auto-dev/commit/3c00fa1e0a7dd3fa8ccbc6505da72eb2ee358e70))
* **compiler:** adjust text length validation and file search logic ([3fe7324](https://github.com/unit-mesh/auto-dev/commit/3fe7324b4a2584f96ea244ae47b131c2fd37dd67))
* **compiler:** remove unused process handler code ([955716d](https://github.com/unit-mesh/auto-dev/commit/955716de81dcb92693ac86ffd4c6bc5e97498eae))
* **ripgrep:** change JSON parse error log level to warn ([2e4f368](https://github.com/unit-mesh/auto-dev/commit/2e4f36876a8d6b458295fe93b0557d74cc5bf722))
* **shell:** handle LightVirtualFile in ShellRunService ([f48b510](https://github.com/unit-mesh/auto-dev/commit/f48b510487e1cee00e6666adb6bc8510924583bc))
* **sketch:** correct typo in answer tags and improve scroll logic [#263](https://github.com/unit-mesh/auto-dev/issues/263) ([0ef403c](https://github.com/unit-mesh/auto-dev/commit/0ef403c4864c2cfe9fcc8815bc4ec5b551e53f22))
* **terminal:** handle terminal text retrieval with reflection ([b8cc0e6](https://github.com/unit-mesh/auto-dev/commit/b8cc0e63bb7f76a9563ab64880365628ce0c1cb7))


### Features

* **commands:** add isApplicable method to InsCommand ([172f8d1](https://github.com/unit-mesh/auto-dev/commit/172f8d14c69c06a37d3f3e7b217debde41560f01))
* **compiler:** add RipgrepSearchInsCommand and optimize file search ([613090a](https://github.com/unit-mesh/auto-dev/commit/613090ac380b189ba6731a4a60e52e4eff5d7aff))
* **completion:** add database function completions ([8509986](https://github.com/unit-mesh/auto-dev/commit/85099864fee4e379b8e519fafa0c157b4ee3318f))
* **database:** format database schema output as SQL code block ([dff81b6](https://github.com/unit-mesh/auto-dev/commit/dff81b6c1a9a5492902b43f0a7ee849299be12e4))
* **ripgrep:** add ripgrep search command and implementation ([055c080](https://github.com/unit-mesh/auto-dev/commit/055c08048f55e27083918708f091cd60f4c92db7))
* **ripgrep:** buffer JSON lines for incomplete parsing ([1731623](https://github.com/unit-mesh/auto-dev/commit/173162375f68a38c92ea51578d7d84c585f9c796))
* **run:** fallback to CLI mode when run service fails ([ba3ab9c](https://github.com/unit-mesh/auto-dev/commit/ba3ab9c11df2503a8e7cd7fba27297cbc014e161))
* **shell:** make shell scripts executable before running ([cdc38b4](https://github.com/unit-mesh/auto-dev/commit/cdc38b4c12e2d3c65fd7ecedb775b0a3d73d0181))
* **sketch:** add send functionality and enableInSketch flag ([4cf2fe6](https://github.com/unit-mesh/auto-dev/commit/4cf2fe60c71bc09586cb2c414c8a9747144277e0))
* **sketch:** add WebpageSketchProvider for HTML support [#265](https://github.com/unit-mesh/auto-dev/issues/265) ([115bb28](https://github.com/unit-mesh/auto-dev/commit/115bb28db37faa590393ea57b829bc55eb0ccf74))
* **terminal:** add "Send to Sketch" button and filter terminal output ([c352e9a](https://github.com/unit-mesh/auto-dev/commit/c352e9ab758fee1668fce7290bc6e2a75b97fe1a))
* **terminal:** enhance shell execution with process output handling ([0068040](https://github.com/unit-mesh/auto-dev/commit/0068040ff8debcd8ee1b15c4538cb3178f61a4ba))
* **ui:** set minimum width for URL field and add border to WebView [#265](https://github.com/unit-mesh/auto-dev/issues/265) ([b29be86](https://github.com/unit-mesh/auto-dev/commit/b29be863b59ee5c614da826c2a2e573cb46d7e89))
* **webview:** add URL field and refresh button to WebViewWindow [#265](https://github.com/unit-mesh/auto-dev/issues/265) ([d798b40](https://github.com/unit-mesh/auto-dev/commit/d798b40804dc3b6c709850639d6c0a92b46fadc8))


### Reverts

* Revert "refactor(terminal): replace terminal text extraction method" ([065c149](https://github.com/unit-mesh/auto-dev/commit/065c1496fdb7aaddf6e0a251ac0d832d0a3dbda4))
* Revert "ci(java): comment out failing or unused test cases" ([ad5df09](https://github.com/unit-mesh/auto-dev/commit/ad5df098304301d0fbb6a5c583cf1727b49463e7))



# [2.0.0-alpha.6](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-ALPHA5...v2.0.0-alpha.6) (2025-01-19)


### Bug Fixes

* **243:** init 243 version support ([e2b3311](https://github.com/unit-mesh/auto-dev/commit/e2b331114bf4dd80c916eeef5316e2e56b395d6b))
* **compiler:** skip ignored and .idea/ files in local search ([90c5413](https://github.com/unit-mesh/auto-dev/commit/90c5413212b2ec1405d31dc541e7ae96bc4bad0d))
* **file-command:** improve file lookup logic for FileInsCommand [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([b8931c9](https://github.com/unit-mesh/auto-dev/commit/b8931c9669e7c294ff3add319f78cf8e99c19097))
* **i18n:** correct test connection button tooltip message ([75db5be](https://github.com/unit-mesh/auto-dev/commit/75db5bec6363dfcb2d03c5cce0a4dd0f8967e37b))
* **sketch:** correct logic for filtering DevIn code blocks [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([ade1a8f](https://github.com/unit-mesh/auto-dev/commit/ade1a8f8563b8ce405b431670782de45f5b12ae6))
* **terminal:** handle null editor case in popup positioning ([26d4127](https://github.com/unit-mesh/auto-dev/commit/26d41277090834aeb9d44c09615a2d6c88146534))
* **terminal:** update langSketchProvider implementation and platform version ([99c327b](https://github.com/unit-mesh/auto-dev/commit/99c327b678cf29b3ceb5fa0c368f12ea5ad44b49))


### Features

* **coroutine:** add worker thread dispatcher and scope ([7410128](https://github.com/unit-mesh/auto-dev/commit/741012851ef870c027ee32a8e6af4419ae4aaca0))
* **file-utils:** add findFile utility for project file search ([c8db562](https://github.com/unit-mesh/auto-dev/commit/c8db562c7cb54eada3c853d85f9aea49da036bbc)), closes [#257](https://github.com/unit-mesh/auto-dev/issues/257)
* **terminal:** 添加对平台版本 243 的支持 ([1e9ba5e](https://github.com/unit-mesh/auto-dev/commit/1e9ba5ed0acb053b3c8a8a3c3cb3268ada7edbfe))



# [2.0.0-ALPHA5](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-ALPHA3...v2.0.0-ALPHA5) (2025-01-18)


### Bug Fixes

* **commands:** update rev command description and examples [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([73178c7](https://github.com/unit-mesh/auto-dev/commit/73178c7de0a7bd0702184d9d747014c6bf3168b6))
* **compiler:** return null instead of error message for missing file ([c225e03](https://github.com/unit-mesh/auto-dev/commit/c225e03f88d3f0c08ebf94624d519095cd91ab35))
* **compiler:** throw exception for missing file ([8e756d7](https://github.com/unit-mesh/auto-dev/commit/8e756d7559560fbec285e1f7e191b3b340035a81))
* **DatabaseInsCommand:** add error handling and notify on failure ([4495873](https://github.com/unit-mesh/auto-dev/commit/4495873afaa5eef9c3c8ec525c71ec3155b1da5c))
* **diff:** remove unused imports in DiffLangSketch ([19878e6](https://github.com/unit-mesh/auto-dev/commit/19878e632ee607dab432ee8d148180d320626de7))
* **i18n:** update sketch composer mode label ([167db9c](https://github.com/unit-mesh/auto-dev/commit/167db9c65b95bc3601e845fff8f22528f63765f1))
* **run:** handle exceptions in AutoDevRunAction and cleanup code [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([eb7bf0a](https://github.com/unit-mesh/auto-dev/commit/eb7bf0a5b5d9c488e62718333df1a5e3da13dd21))
* **sketch:** remove DevinsError check in SketchToolWindow ([51f8994](https://github.com/unit-mesh/auto-dev/commit/51f8994cdae492392c56d21db0b05454cd75472e))
* **test:** update assertion message in JavaScriptVersionProviderTest ([f8f0d82](https://github.com/unit-mesh/auto-dev/commit/f8f0d820acb635a20b8d6405d89b87a2dc67ef26))
* **toolExamples:** correct instruction for handling existing files ([52a0b89](https://github.com/unit-mesh/auto-dev/commit/52a0b89b0e070afbee80b284151a956445056459))
* **ui:** set preferred size for buttons in SingleFileDiffView ([b0e756f](https://github.com/unit-mesh/auto-dev/commit/b0e756f89caa9698add8b19ca45db17d6384798c))


### Features

* **diff:** add run and repair diff actions ([26e4c76](https://github.com/unit-mesh/auto-dev/commit/26e4c7644d81d5cc3ca196a6857f83a59ab7ce37))
* **project:** add ProjectFileUtil for file project checks [#275](https://github.com/unit-mesh/auto-dev/issues/275) ([a17ab78](https://github.com/unit-mesh/auto-dev/commit/a17ab788028aec956e77bfd4a32f9b50a803899e))
* **shell:** enhance shell cmd exec, refactor service, update examples ([c370c05](https://github.com/unit-mesh/auto-dev/commit/c370c0554e0599c3a8ef471bb14a62e742e0c1a4))
* **shell:** enhance shell command execution and refactor service [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([4d8c9a6](https://github.com/unit-mesh/auto-dev/commit/4d8c9a658cbee454e14f76045194a9955d3431f6))
* **sketch:** add build tool info and improve UI styling [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([f4780b8](https://github.com/unit-mesh/auto-dev/commit/f4780b85e8235f174d72f4dbd873d7cdc6025bc5))
* **sketch:** add mermaid and plantuml support [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([5b89cf8](https://github.com/unit-mesh/auto-dev/commit/5b89cf88f4962fa4931b61c1a0c1a9650dc97d0b))
* **sketch:** add mermaid and plantuml support [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([4420024](https://github.com/unit-mesh/auto-dev/commit/4420024d96a644e24630168df64f2d78c990a5f1))
* **sketch:** update diff stream diff block [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([97159a6](https://github.com/unit-mesh/auto-dev/commit/97159a6c05628a4fa73748c168c1dfa0e3b59f10))
* **snippet:** add AutoInputService for DevIn language support [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([b3a5b0c](https://github.com/unit-mesh/auto-dev/commit/b3a5b0cc0f155f4f744e9fbd2c529c74d8c0c943))
* **toolbar:** add NewSketchAction for creating sketch panels [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8805d5a](https://github.com/unit-mesh/auto-dev/commit/8805d5a831f493b9bc80d24d0d457233fbab432e))
* **ui:** add copy-to-clipboard functionality and cleanup logic [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([a74d2da](https://github.com/unit-mesh/auto-dev/commit/a74d2da482a7dd4efc453572340671c5143a5bbd))
* **ui:** conditionally add header for multiple file patches [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([69fd27b](https://github.com/unit-mesh/auto-dev/commit/69fd27b0f27bc767750a85ddee8a98ae274d6bcb))
* **vcs:** add RevisionProvider interface and improve DiffSimplifier [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([de54736](https://github.com/unit-mesh/auto-dev/commit/de54736a443755fb511eef0c56f812d30d499c72))



# [2.0.0-ALPHA3](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-ALPHA2...v2.0.0-ALPHA3) (2025-01-15)


### Bug Fixes

* **gui:** handle LightVirtualFile in relativePath calculation ([9f6db3e](https://github.com/unit-mesh/auto-dev/commit/9f6db3e02167912b70cd12a4bd601a458f547ebd))


### Features

* **python:** add framework detection logic ([ce7314c](https://github.com/unit-mesh/auto-dev/commit/ce7314c45182e39bca1217a87febd1db6cdbfab7))
* **run-config:** add show console toggle for DevIns run configurations [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8c278d2](https://github.com/unit-mesh/auto-dev/commit/8c278d29e4693661d832d1160369acb4dc2ab444))
* **run:** add `isFromToolAction` parameter to `runFile` method ([50eb955](https://github.com/unit-mesh/auto-dev/commit/50eb9551826b9e0ca075391664798790641a30dc))
* **run:** add project run service for task execution ([83496fb](https://github.com/unit-mesh/auto-dev/commit/83496fbe44f11dfc38eed52006c4740f4af4d832))
* **sketch:** add current and opened files to context [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8ab56f1](https://github.com/unit-mesh/auto-dev/commit/8ab56f1b9f9dde3aaf0a6ce2971fb4344c7e99c4))
* **sketch:** add framework context to SketchRunContext [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([5bbd24a](https://github.com/unit-mesh/auto-dev/commit/5bbd24a64939262939b7a9f98a9dced43a1786fb))
* **sketch:** enhance DevIn tool integration and UI improvements [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([2e08ad2](https://github.com/unit-mesh/auto-dev/commit/2e08ad29dcb6c398464bd59192900df5ce1a79d6))
* **terminal:** add TerminalDiffLangSketchProvider and refactor UI [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([bd949f8](https://github.com/unit-mesh/auto-dev/commit/bd949f8f79fa0a3dc6a8f78bb6f9f4d4ff8c7561))
* **terminal:** add TerminalDiffLangSketchProvider for bash support ([7102d28](https://github.com/unit-mesh/auto-dev/commit/7102d286e3e25481cba240d0522d8cc2952fd757))
* **toolbar:** add sketch panel creation in NewChatAction [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8ee6841](https://github.com/unit-mesh/auto-dev/commit/8ee6841c0a1f25989c629657dd18495d1118f8ad))



# [2.0.0-ALPHA2](https://github.com/unit-mesh/auto-dev/compare/v2.0.0-ALPHA1...v2.0.0-ALPHA2) (2025-01-14)


### Bug Fixes

* **devins-lang:** update dir.devin example to use correct syntax ([ea93fa3](https://github.com/unit-mesh/auto-dev/commit/ea93fa30aab1b92a3bfbe475371535bc72e68000))
* **git:** handle unformatted commit message text [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([bc5c4e9](https://github.com/unit-mesh/auto-dev/commit/bc5c4e9282265db648b7974b13d2207f514b0bb8))
* **parser:** add 'bash' as alias for 'Shell Script' in CodeUtil [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8b6816f](https://github.com/unit-mesh/auto-dev/commit/8b6816fccf7ceec644baa6efcca983aa66fe1021))
* **run:** handle run failure and cleanup scratch file [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8a2e422](https://github.com/unit-mesh/auto-dev/commit/8a2e422c4654a5e9163664cac3be04e67ec7a7b8))


### Features

* **compiler:** add DIR command for listing files and directories [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([60378f7](https://github.com/unit-mesh/auto-dev/commit/60378f7b2dc7a2051f55293412a7510a42b6838c))
* **completion:** add directory reference completion support [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([2c4e493](https://github.com/unit-mesh/auto-dev/commit/2c4e493f7fea7148cca3b86001e08b324fd53230))
* **core:** add JSON and HTTP extension points ([e48309e](https://github.com/unit-mesh/auto-dev/commit/e48309e5038007a9d2589086afb12d9644f1d28a))
* **database:** add SqlRunService and DatabaseFunctionProvider [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([9fcd816](https://github.com/unit-mesh/auto-dev/commit/9fcd816d5558f5da52eb5ba66ed1ab32f2a1710a))
* **gui:** update AutoDev tool window content creation [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([cc5373d](https://github.com/unit-mesh/auto-dev/commit/cc5373d6acda8b9f8f48b0c609da35be069b936c))
* **inline-chat:** enhance inline chat with context and template rendering [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([38a5fd4](https://github.com/unit-mesh/auto-dev/commit/38a5fd485dbc0091ca5fe4637125bd462d58d406))
* **java:** add related symbol resolution and command [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([8e70b21](https://github.com/unit-mesh/auto-dev/commit/8e70b21d8ebdb7f5eab1e7b46b1a0f742091bae8))
* **json:** add optional JSON module dependency [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([232c801](https://github.com/unit-mesh/auto-dev/commit/232c8010d6582002e6ad49762f8b700123865d7c))
* **parser:** add support for DevIns code format [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([4c77940](https://github.com/unit-mesh/auto-dev/commit/4c779406acce157501f6afae0c887764a6964633))
* **parser:** improve DevIn code block parsing logic [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([f09b369](https://github.com/unit-mesh/auto-dev/commit/f09b369b1c9b6a348b75f32368799031625786f4))
* **prompting:** expose collectFrameworkContext as public method [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([d672fc2](https://github.com/unit-mesh/auto-dev/commit/d672fc21f3c43973f6888b18bbb4972466ce90b5))
* **run:** add DevInRunService for .devin file execution [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([d3ac095](https://github.com/unit-mesh/auto-dev/commit/d3ac095c04187cbc1ddcd690ba3cbd61651380b5))
* **run:** refactor and extend RunService for file execution [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([15656e1](https://github.com/unit-mesh/auto-dev/commit/15656e1b72124d0b80869f83a9461492e320b226))
* **search:** enhance local search with context lines and scope support ([fad212f](https://github.com/unit-mesh/auto-dev/commit/fad212f1f5a9f188edf864e64e3d7c05e8cb37f7)), closes [#257](https://github.com/unit-mesh/auto-dev/issues/257)
* **search:** replace GrepSearch with LocalSearch command [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([f9a112d](https://github.com/unit-mesh/auto-dev/commit/f9a112db2f411922692ed1ce36162f2002d6bc6e))
* **shell:** add ShellUtil for detecting available shells [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([02f4293](https://github.com/unit-mesh/auto-dev/commit/02f429381de375b902f12a6f6863fb68fffe408d))
* **shell:** add support for shell script execution [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([f454226](https://github.com/unit-mesh/auto-dev/commit/f4542265c31b5afafb689b3fd981af7f8b0f5b9b))
* **shell:** support dynamic shell content execution [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([fb6279a](https://github.com/unit-mesh/auto-dev/commit/fb6279a97c550c545566e2c23710a2f87b84295d))
* **sketch:** add sketch functionality and rename InlineChatPanelView to ChatSketchView [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([cc762f1](https://github.com/unit-mesh/auto-dev/commit/cc762f1835d4b5540b76efd358c98681b8d50b25))
* **sketch:** add SketchRunContext and SketchToolchain [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([1399b02](https://github.com/unit-mesh/auto-dev/commit/1399b02c9e9efae354b32b27bc247fef15c3ded2))
* **sketch:** add toolchain provider and refactor sketch UI [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([0ad8531](https://github.com/unit-mesh/auto-dev/commit/0ad8531cf704b235b34baa7364bf7da791982649))
* **sketch:** add toolList support and refactor toolchain [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([c160a7d](https://github.com/unit-mesh/auto-dev/commit/c160a7d24ee9e8c057bd0e600acefa720cbdff30))
* **sketch:** enhance toolchain and add open command [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([7093f33](https://github.com/unit-mesh/auto-dev/commit/7093f3348ab2d6dcc6331fcf94abc618836479a1))
* **templates:** enhance context variable usage in templates ([587d64d](https://github.com/unit-mesh/auto-dev/commit/587d64d1443afd88f8faa1004af51d9a4922bdd9))
* **toolchain:** add toolchain function provider and database command [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([dd1b375](https://github.com/unit-mesh/auto-dev/commit/dd1b375d5941cbb2a31c915c3cc2451c5a46ad1f))
* **toolwindow:** add sketch panel and improve editor handling [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([4732572](https://github.com/unit-mesh/auto-dev/commit/4732572f57cc04e19cbafc7c17dc67d04cfbf6e4))
* **update:** add AutoDevUpdateStartupActivity and refactor UI components [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([0e92133](https://github.com/unit-mesh/auto-dev/commit/0e9213374fb0bb7f16a534bf235c5ad156c615f3))



# [2.0.0-ALPHA1](https://github.com/unit-mesh/auto-dev/compare/v1.9.0...v2.0.0-ALPHA1) (2025-01-11)


### Features

* **diff:** add related select [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([91abe1b](https://github.com/unit-mesh/auto-dev/commit/91abe1bce1d515b59b8fcc33ddc4ed02a3256bcd))
* **diff:** init stream diff code [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([93b8316](https://github.com/unit-mesh/auto-dev/commit/93b83168b58195f7a411ed598c52918123cebddd))
* **inline:** add inline chat [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([0db978c](https://github.com/unit-mesh/auto-dev/commit/0db978c2bcc08927fdf9c0ae567266d3ccba6570))
* **settings:** add LLM key validation and connection tips [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([3f71f8e](https://github.com/unit-mesh/auto-dev/commit/3f71f8e12df0088fb20be218199a18406b1c2d91))
* **settings:** update UI and schema handling for AutoDev [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([fca0563](https://github.com/unit-mesh/auto-dev/commit/fca0563a1c4ebedd8fbec8b2392bd20e1f579baf))



# [1.9.0](https://github.com/unit-mesh/auto-dev/compare/v1.8.18...v1.9.0) (2025-01-11)


### Features

* **build:** add support for platform version 223 [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([ec85cfb](https://github.com/unit-mesh/auto-dev/commit/ec85cfb934ed4833514b8ebe63d0bbf1cea8c021))
* **release-note:** add template-based release note generation [#256](https://github.com/unit-mesh/auto-dev/issues/256) ([76943cf](https://github.com/unit-mesh/auto-dev/commit/76943cf3940211a52b708c5d076f733450f49f93))
* **settings:** add DevOps configurable provider [#257](https://github.com/unit-mesh/auto-dev/issues/257) ([5153032](https://github.com/unit-mesh/auto-dev/commit/51530326d65158cda633c6f9c602e1241bcaa6d6))



## [1.8.18](https://github.com/unit-mesh/auto-dev/compare/v1.8.17...v1.8.18) (2024-12-28)


### Bug Fixes

* **android:** handle exceptions in isApplicable method ([8725848](https://github.com/unit-mesh/auto-dev/commit/872584853e9772bfe89fe06ef0a62b3735b41594))
* **code-review:** 移除代码审查模板中的冗余代码块标记 ([028d881](https://github.com/unit-mesh/auto-dev/commit/028d8817e2ca78041c4b19b335efadfdadcefadc))
* **gui:** handle exceptions in AutoDevRunDevInsAction ([9572a61](https://github.com/unit-mesh/auto-dev/commit/9572a614a3189b48353752958d6e92ddc8b04c70))
* **gui:** simplify document text access in AutoDevInputSection ([f5236c8](https://github.com/unit-mesh/auto-dev/commit/f5236c84b9c6da1ea33934314018c8afc15be7ac))
* **llms:** skip empty messages in CustomSSEProcessor ([f9337d6](https://github.com/unit-mesh/auto-dev/commit/f9337d6e49163594e797128f259c5e42fccf342a))


### Features

* **agent:** add Dify API support and enhance custom agent config [#251](https://github.com/unit-mesh/auto-dev/issues/251) ([9cc5268](https://github.com/unit-mesh/auto-dev/commit/9cc526862783b14bbcf578e4874e330198bf855f))
* **chat:** enhance chat handling with history and response tracking ([803e184](https://github.com/unit-mesh/auto-dev/commit/803e184d8388b16643d5aaadfb2f24209509b86b)), closes [#251](https://github.com/unit-mesh/auto-dev/issues/251)
* **code-review:** 添加 diff 上下文展示和版本更新 ([eba213a](https://github.com/unit-mesh/auto-dev/commit/eba213a22203f676f8048057aa982a0603348f8a))
* **completion:** enhance agent selection in completion ([3b1b3bb](https://github.com/unit-mesh/auto-dev/commit/3b1b3bb4ba156a480ad1dc1a1d5eb12140578bc2))
* **custom:** add dynamic field replacement in custom requests [#251](https://github.com/unit-mesh/auto-dev/issues/251) ([ba50600](https://github.com/unit-mesh/auto-dev/commit/ba506007b887a573811da2512dac8cbfcc96499e))
* **parser:** add support for shell script and HTTP request language names ([c7d5f80](https://github.com/unit-mesh/auto-dev/commit/c7d5f80f4843082f4d7cec2e04bc052e09ef0543))
* **ui:** enhance cursor positioning and UI adjustments ([ccbe3e2](https://github.com/unit-mesh/auto-dev/commit/ccbe3e23f0d0c6d8b6c61262379450a3960bf9c1))



## [1.8.17](https://github.com/unit-mesh/auto-dev/compare/v1.8.16...v1.8.17) (2024-12-11)


### Bug Fixes

* **parser:** correct newline escaping in test cases ([b0cc99a](https://github.com/unit-mesh/auto-dev/commit/b0cc99a4d4102c993099e247cb66d34b2b3e7956))
* **parser:** remove unnecessary newline replacement ([831f747](https://github.com/unit-mesh/auto-dev/commit/831f747f5101ef9ad63dcf756adfc40456820daf))


### Features

* **code-completion:** enhance inlay code completion and formatting ([c2585bb](https://github.com/unit-mesh/auto-dev/commit/c2585bba132a5345b1508c51e14185e7efee19a9))



## [1.8.16](https://github.com/unit-mesh/auto-dev/compare/v1.8.15...v1.8.16) (2024-12-07)


### Bug Fixes

* **builder:** 优化上下文提供者处理逻辑 ([c5e7fb5](https://github.com/unit-mesh/auto-dev/commit/c5e7fb5ceec808621be60804eb0b48827b9217a3))
* **core:** Optimize TestCodeGenTask and JSAutoTestService logic ([276342c](https://github.com/unit-mesh/auto-dev/commit/276342cf9d9615348cac134d8f0f967651592414))
* **core:** 修复自定义提示配置加载时的日志记录 ([bdf63f5](https://github.com/unit-mesh/auto-dev/commit/bdf63f5a625ef327a6aaec8499ab2ca5c9816fd4))
* **CustomSSEProcessor:** 支持自定义大模型 customFields 使用复杂类型字段 ([#248](https://github.com/unit-mesh/auto-dev/issues/248)) ([8c0bb92](https://github.com/unit-mesh/auto-dev/commit/8c0bb9201945ab75a62965b3a22346e0b644223f))
* **java:** Clean up PsiMethod and improve MethodContext creation ([19647cf](https://github.com/unit-mesh/auto-dev/commit/19647cf56d6fe65674b2d1923f11042fe6c69a60))
* **java:** 优化 findRelatedClasses 方法以避免不必要的类型解析 ([9d3340f](https://github.com/unit-mesh/auto-dev/commit/9d3340fbbd70766109ec24abc6ac222861e6c175))
* **LLMSettingComponent:** 修复新装用户引擎未选择情况下的 `Array contains no element matching the predicate.` 异常 ([#247](https://github.com/unit-mesh/auto-dev/issues/247)) ([10848e3](https://github.com/unit-mesh/auto-dev/commit/10848e3a4077bbbaeccb79d35c8349aaad1da31c))
* **tests:** update Kotlin test cases with proper syntax and semicolon usage ([a54aabd](https://github.com/unit-mesh/auto-dev/commit/a54aabd5fdc61775a519383e1953f872e3348846))


### Features

* **document:** Add examples to custom living doc prompt builder ([dd2cd39](https://github.com/unit-mesh/auto-dev/commit/dd2cd39e04ec002323a927da0065039a2da0c7ef))
* **java:** add support for AssertJ and update JUnit detection logic ([e1e9c26](https://github.com/unit-mesh/auto-dev/commit/e1e9c26cbf660ed20c180edf65e79ef049d005ca))



## [1.8.15](https://github.com/unit-mesh/auto-dev/compare/v1.8.12...v1.8.15) (2024-11-16)


### Reverts

* Revert "chore(plugin): update IntelliJ dependency and add JSON module config" ([f49134b](https://github.com/unit-mesh/auto-dev/commit/f49134bf550957556a2fcaeb873ee1b7d4230d16))



## [1.8.12](https://github.com/unit-mesh/auto-dev/compare/v1.8.12-ALPHA...v1.8.12) (2024-10-05)


### Features

* **build:** add kotlinx serialization plugin and dependency [#239](https://github.com/unit-mesh/auto-dev/issues/239) ([055633c](https://github.com/unit-mesh/auto-dev/commit/055633c1f992483648a54aa10a38cfd040833d4e))



## [1.8.12-ALPHA](https://github.com/unit-mesh/auto-dev/compare/v1.8.11...v1.8.12-ALPHA) (2024-09-26)


### Features

* **build.gradle.kts:** add IntelliJ platform plugins and tasks [#236](https://github.com/unit-mesh/auto-dev/issues/236) ([a884459](https://github.com/unit-mesh/auto-dev/commit/a884459b6cb57e426bc94decebd4283fa96b0a14))
* **build:** upgrade Gradle version and IntelliJ plugin [#236](https://github.com/unit-mesh/auto-dev/issues/236) ([82c9ab5](https://github.com/unit-mesh/auto-dev/commit/82c9ab540c5cf0849b813da7081bc94631a4137d))
* **github-actions:** update build workflow and split tasks into separate jobs ([a92966b](https://github.com/unit-mesh/auto-dev/commit/a92966bca0262f885131dbdded5ea6b45bcba4ee))
* **gradle:** increase JVM memory for Kotlin and Gradle [#236](https://github.com/unit-mesh/auto-dev/issues/236) ([ddd30c1](https://github.com/unit-mesh/auto-dev/commit/ddd30c1a207083b59be2cf2a0beb399c60415b20))



## [1.8.11](https://github.com/unit-mesh/auto-dev/compare/v1.8.9-SNAPSHOT...v1.8.11) (2024-09-08)


### Bug Fixes

* **gui:** handle exceptions in language detection ([40f1c0d](https://github.com/unit-mesh/auto-dev/commit/40f1c0d89d8e47a2a8f64c703d0c23cbf011a2d8))
* **provider:** return immediately in handleFromType for PsiClassType ([6c92163](https://github.com/unit-mesh/auto-dev/commit/6c921634ac130289a4ca9ab0e9eec33b5b6157c3))
* Unable to receive notifications when changes are made to the document ([#228](https://github.com/unit-mesh/auto-dev/issues/228)) ([22cd295](https://github.com/unit-mesh/auto-dev/commit/22cd29579053e60f9695ae4dfc3751039d67c6a9))



## [1.8.9-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v1.8.8...v1.8.9-SNAPSHOT) (2024-08-09)


### Bug Fixes

* **settings:** update comparison and remove unused dependency ([853dd9d](https://github.com/unit-mesh/auto-dev/commit/853dd9d377f66ee5245e80707cdc36511717b16c))


### Features

* **embedding:** add LocalEmbedding class for text embedding [#200](https://github.com/unit-mesh/auto-dev/issues/200) ([2af87cc](https://github.com/unit-mesh/auto-dev/commit/2af87ccb07d38cdd80b91c55f3a0ea9b1d889770))
* **embedding:** implement in-memory and disk-synchronized embedding search indices [#200](https://github.com/unit-mesh/auto-dev/issues/200) ([6d5ca70](https://github.com/unit-mesh/auto-dev/commit/6d5ca70311af20d4ac1b84816f6c3194297f22b1))



## [1.8.8](https://github.com/unit-mesh/auto-dev/compare/v1.8.7-RELEASE...v1.8.8) (2024-08-07)


### Bug Fixes

* **codecomplete:** handle empty range in collectInlays function ([48901f8](https://github.com/unit-mesh/auto-dev/commit/48901f8ae6f53982da2ae3bfc88b6d6547557cde))
* **devti:** fix response body handling in ResponseBodyCallback [#209](https://github.com/unit-mesh/auto-dev/issues/209) ([cef9581](https://github.com/unit-mesh/auto-dev/commit/cef958174117dd609edbab6c52b789b29cb19c70))
* Failed to reset on the autoDevSettings UI ([8b16443](https://github.com/unit-mesh/auto-dev/commit/8b164436c39a19467e3bc4c23a44d84fab89baae))
* fix import ([50f3c8b](https://github.com/unit-mesh/auto-dev/commit/50f3c8be3d1ee8183e28c569376db87066e2b55f))
* **LLMInlayManager:** use InlayModelImpl for inlay model ([86d0840](https://github.com/unit-mesh/auto-dev/commit/86d08408a031f29ce28743b703932bea9f2ef876))
* wrong trigger when user typing from code. ([08f8fbb](https://github.com/unit-mesh/auto-dev/commit/08f8fbb5a9ed06337f22b1a2a852969fa472edeb))


### Features

* **diff-simplifier:** Include binary or large changes in output ([8fc6255](https://github.com/unit-mesh/auto-dev/commit/8fc625507fae4fa26c39d6963fa23c87f06662f3))
* **JavaAutoTestService:** update cocoa-core dependency and remove unused import ([56a6fa3](https://github.com/unit-mesh/auto-dev/commit/56a6fa37a1eb65d6439033a7b2c3cc0dc9ae51c0))
* **service:** add support for creating Maven run configuration [#164](https://github.com/unit-mesh/auto-dev/issues/164) ([cdc003a](https://github.com/unit-mesh/auto-dev/commit/cdc003ae040cbaa20898a3f891295852c4b6f969))
* **smartpaste:** add SmartCopyPasteProcessor ([4b427b4](https://github.com/unit-mesh/auto-dev/commit/4b427b46cb861544f79cd9e10ae5e5ab66e80677))



## [1.8.7-RELEASE](https://github.com/unit-mesh/auto-dev/compare/v1.8.7-SNAPSHOT...v1.8.7-RELEASE) (2024-06-13)


### Bug Fixes

* **java:** simplify JavaVersionProvider isApplicable method ([4c20a81](https://github.com/unit-mesh/auto-dev/commit/4c20a8123cbc19459548cb1732392463eae2e210))



## [1.8.7-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v1.8.6-RELEASE...v1.8.7-SNAPSHOT) (2024-06-06)


### Bug Fixes

* **context:** handle exceptions in ClassContextProvider [#199](https://github.com/unit-mesh/auto-dev/issues/199) ([347c452](https://github.com/unit-mesh/auto-dev/commit/347c4522a1b682f5300aaa1372c1592ef243bf58))


### Features

* **api-test:** add API test request generation template [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([177a66c](https://github.com/unit-mesh/auto-dev/commit/177a66ce551564747a035cbac75b6d0762fe6850))
* **api-test:** add HttpClient API test generation [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([5c982af](https://github.com/unit-mesh/auto-dev/commit/5c982af9264650c0de34f2d9ef2866e791ed6c00))



## [1.8.6-RELEASE](https://github.com/unit-mesh/auto-dev/compare/v1.8.6-SNAPSHOT...v1.8.6-RELEASE) (2024-05-30)


### Bug Fixes

* **flow:** handle null case for base package name ([14b5656](https://github.com/unit-mesh/auto-dev/commit/14b56563fb75424855483aec0630372f9fe01194))
* **flow:** update code creation flag in JvmAutoDevFlow ([c9cebfe](https://github.com/unit-mesh/auto-dev/commit/c9cebfef375a1238f02990b537a9dca1892b1c5b))
* **parser:** update regex pattern for code block [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([f08942b](https://github.com/unit-mesh/auto-dev/commit/f08942bc3b8c5e2c59e730993a52cdab999cac16))
* **runner:** handle null result in test execution [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([b70900c](https://github.com/unit-mesh/auto-dev/commit/b70900c9b7009d80589ce3022ad1ae1622cbc4da))


### Features

* **actions:** add AutoTestInMenuAction for batch testing [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([1719118](https://github.com/unit-mesh/auto-dev/commit/1719118168de5b59fd6b5d805eff62a827720380))
* **actions:** add batch test generation capability [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([7e4f6ed](https://github.com/unit-mesh/auto-dev/commit/7e4f6ed8e86f34c89bc8c5525e1088927548a67a))
* **http-client:** add support for creating HttpRequestRunConfiguration [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([30efd49](https://github.com/unit-mesh/auto-dev/commit/30efd493c3598317b772a155cde4fd4dd81b79c2))
* **http-client:** add support for creating temporary scratch files [#198](https://github.com/unit-mesh/auto-dev/issues/198) && closed [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([71c13c9](https://github.com/unit-mesh/auto-dev/commit/71c13c9912e3c7e48e3bd432f2fe27fcd3813294))
* **http:** add HttpClientProvider interface and implementation [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([8434fd3](https://github.com/unit-mesh/auto-dev/commit/8434fd30eacb3406c59d603292b5f237e4428c91))
* **http:** add support for executing HTTP requests from files [#198](https://github.com/unit-mesh/auto-dev/issues/198) ([2c158ea](https://github.com/unit-mesh/auto-dev/commit/2c158ea3fa5d63608ed89754230ee25e39692eaf))
* **intentions:** add test verification feature [#196](https://github.com/unit-mesh/auto-dev/issues/196) ([4080c38](https://github.com/unit-mesh/auto-dev/commit/4080c38f1ffae9ffbdbc9f3885dce166e3222e1c))
* **prompting:** improve API test request generation ([72225bb](https://github.com/unit-mesh/auto-dev/commit/72225bba7ae2c9f7a18034b9323216b3f372c5ac)), closes [#196](https://github.com/unit-mesh/auto-dev/issues/196)



## [1.8.6-SNAPSHOT](https://github.com/unit-mesh/auto-dev/compare/v1.8.5-SNAPSHOT...v1.8.6-SNAPSHOT) (2024-05-29)


### Bug Fixes

* **compiler:** use FileDocumentManager to get current file in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([871c4df](https://github.com/unit-mesh/auto-dev/commit/871c4dfcc37d7ac1d424d8c763b1a7612c4fc4e8))
* **custom-action:** add selectedRegex field to CustomIntentionConfig [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([c30ac7f](https://github.com/unit-mesh/auto-dev/commit/c30ac7f192a96110c9ee063aff07c9eac9a9ca4a))
* **gui:** use getVirtualFile method for editor document for version-222 ([bdb92b3](https://github.com/unit-mesh/auto-dev/commit/bdb92b30145a2216810d3de347bea8fa1af8d77e))
* **refactoring-tool:** update rename method call in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([bbc1f95](https://github.com/unit-mesh/auto-dev/commit/bbc1f95980fa33868f4c022ae81cf5301ebcdffc))
* **refactoring:** trim input strings and handle null psiFile [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([4a5c634](https://github.com/unit-mesh/auto-dev/commit/4a5c634c9c84e182957b3f08d51be487114a124d))
* remove AI_Copilot from floating code toolbar for [#222](https://github.com/unit-mesh/auto-dev/issues/222) ([8875290](https://github.com/unit-mesh/auto-dev/commit/8875290d08638a32c5725180a0482d0526ba285b))
* **test:** improve error message for test syntax errors ([24f7c16](https://github.com/unit-mesh/auto-dev/commit/24f7c160ec85a3fb8cca4839b0204fe1f2e6e9fc))


### Features

* **action-test:** add extContext support and RAG execution functionality [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([6166f38](https://github.com/unit-mesh/auto-dev/commit/6166f380f3201d47846819465439dca7089eb4f8))
* Add TypeScript refactoring support and improve JavaScript file context builder [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([c3956f2](https://github.com/unit-mesh/auto-dev/commit/c3956f244a479b367a392b4e5bab26196e96aec6))
* **agent:** add enabled flag to CustomAgentConfig [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([0dfaa32](https://github.com/unit-mesh/auto-dev/commit/0dfaa32149f7e572bb8542374f615971065c083f))
* **autodev-core:** update service implementations and action descriptions ([8e07e8e](https://github.com/unit-mesh/auto-dev/commit/8e07e8e90b9c17a04c34d089c16fb8b9f016cb57))
* **chat:** add refactor prompt to static code analysis results [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([30c28a1](https://github.com/unit-mesh/auto-dev/commit/30c28a1ffeb7e8d898cbd292f4a93934b0dc11a8))
* **chat:** update DevIn language instructions in RefactorThisAction [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([5d61d47](https://github.com/unit-mesh/auto-dev/commit/5d61d4738f20cb266e66ee164de8fd011aabccca))
* **chat:** update DevIn language support in RefactorThisAction [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([a08a979](https://github.com/unit-mesh/auto-dev/commit/a08a979beca0296d25282101ac30cf2f79712e9e))
* **custom-action:** add batchFileRegex and batchFiles method [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([b8a2c16](https://github.com/unit-mesh/auto-dev/commit/b8a2c16484a9db45e0ac1895a52bb3f99167e238))
* **editor:** replace findVirtualFile with FileDocumentManager in AutoDevRunDevInsAction ([1f92c36](https://github.com/unit-mesh/auto-dev/commit/1f92c36b247052c362947b20c096e56170720016))
* **ext-context:** add auto-test API endpoint with user stories ([8b7db54](https://github.com/unit-mesh/auto-dev/commit/8b7db54eb34b652881141082adee6720b6c46821))
* **FileGenerateTask:** Add 'codeOnly' option to generate code-only files [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([9a962ed](https://github.com/unit-mesh/auto-dev/commit/9a962ed8959712d5f364b217de9447907f1b26b6))
* **gui:** add RunDevIns action and refactor JavaRefactoringTool [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([d33f1da](https://github.com/unit-mesh/auto-dev/commit/d33f1da7db1b95dcef1f288b16d7e4fa7d0c40d9))
* **gui:** replace getVirtualFile with findVirtualFile and add RunDevIns action [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([1805bf3](https://github.com/unit-mesh/auto-dev/commit/1805bf356a283e460769a06eba2a59aaa16d3030))
* **intention-action:** introduce CustomExtContext and refactor TestCodeGenTask [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([fd12229](https://github.com/unit-mesh/auto-dev/commit/fd122291c4855f35cd0b6668b4b00f05d200c100))
* **JavaRefactoringTool:** enhance renaming functionality [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([929ab39](https://github.com/unit-mesh/auto-dev/commit/929ab39d2872ac5f4208cf98b66a46071746c59d))
* **JavaRefactoringTool:** replace RenameQuickFix with RenameUtil [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([0581307](https://github.com/unit-mesh/auto-dev/commit/058130714540e2af2b357434804bb09fa2ae643b))
* **JavaTypeUtil:** make resolveByType method public and limit class resolution ([39e5988](https://github.com/unit-mesh/auto-dev/commit/39e59887122d6f34d68635f89b42a6ebf442ec41))
* **language:** add refactoring function provider [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([b898971](https://github.com/unit-mesh/auto-dev/commit/b898971000b3e123a98b064b9c330fe6c619faea))
* **refactoring:** add JavaRefactoringTool implementation [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([65913f0](https://github.com/unit-mesh/auto-dev/commit/65913f02b370188cd54893ca291309c032dd15d8))
* **refactoring:** add KotlinRefactoringTool and refactor JavaRefactoringTool [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([0d2afd8](https://github.com/unit-mesh/auto-dev/commit/0d2afd8bcb86ec631a0b4da6fca44f07df65980b))
* **refactoring:** add RefactoringTool interface and extension point [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([62d7513](https://github.com/unit-mesh/auto-dev/commit/62d7513bec2b01e9437563ca54b2fbdaa578b9d0))
* **refactoring:** add RenameElementFix to JavaRefactoringTool [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([bb42c9e](https://github.com/unit-mesh/auto-dev/commit/bb42c9e9efd1e8e6496a46b0141f55247cd06507))
* **refactoring:** enhance Java refactoring support [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([bf1bbc9](https://github.com/unit-mesh/auto-dev/commit/bf1bbc90af56ad08dd2fcf595a1154825c6d88e0))
* **refactoring:** enhance refactoring commands and tools [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([7656757](https://github.com/unit-mesh/auto-dev/commit/7656757dcbda544106a94ad98a1e351cfc7ebfeb))
* **refactoring:** enhance rename functionality in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([154eb0d](https://github.com/unit-mesh/auto-dev/commit/154eb0de52beeb7959c54ea8829c228767259331))
* **refactoring:** enhance rename method with PsiFile parameter [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([6864cda](https://github.com/unit-mesh/auto-dev/commit/6864cda2de61d6be9fc51d8629c56809bef56119))
* **refactoring:** prioritize Java language in RefactorInsCommand [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([b6fe789](https://github.com/unit-mesh/auto-dev/commit/b6fe789fe4b66a045439f7c6b6ac7ae9e6ed07d1))
* **RefactoringTool:** add lookupFile method to interface and implementation [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([16de4cb](https://github.com/unit-mesh/auto-dev/commit/16de4cb4f2fead6cac98ffc01ed2b5fb64c5f6be))
* **RefactoringTool:** enhance rename method to support method renaming [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([0cfd2e8](https://github.com/unit-mesh/auto-dev/commit/0cfd2e854800d9857771fed89b0e003b008771a4))
* **team-exec-task:** enhance task with progress display and dynamic title #[#193](https://github.com/unit-mesh/auto-dev/issues/193) ([e627f6e](https://github.com/unit-mesh/auto-dev/commit/e627f6ec1af1ac5b81c9f06d4baf54e437946db8))
* **team-prompt:** add support for batch file processing and error notification [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([5629753](https://github.com/unit-mesh/auto-dev/commit/5629753d3b4cc5d3c6474ce9bd0f69c181030dc6))
* **team:** Add ReplaceCurrentFile interaction and batch file processing support [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([2b24045](https://github.com/unit-mesh/auto-dev/commit/2b24045ad8c95e41ddd16638251079f832680d10))
* **templating:** add comment prefixes for new languages ([f9498a9](https://github.com/unit-mesh/auto-dev/commit/f9498a99541a0dec6520d708c27c1f0853c17322))
* **test:** add support for custom RAG context [#195](https://github.com/unit-mesh/auto-dev/issues/195) ([efb06f3](https://github.com/unit-mesh/auto-dev/commit/efb06f3287eb010860d12eda6ed1a9def8205532))
* **variables:** add ALL variable and glob matcher for batch files [#193](https://github.com/unit-mesh/auto-dev/issues/193) ([66f4588](https://github.com/unit-mesh/auto-dev/commit/66f4588b72739b587a11393b8fe20a366fa1998b))



## [1.8.5-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.4-RC...v1.8.5-RC) (2024-05-24)


### Bug Fixes

* **lang:** refactor DevIn documentation provider registration && fixed [#191](https://github.com/unit-mesh/auto-dev/issues/191) ([fdf040f](https://github.com/unit-mesh/auto-dev/commit/fdf040ffd9ece69e449d117e723a0204347b4df6))
* **openai:** dsiable system prompt when empty request preparation [#190](https://github.com/unit-mesh/auto-dev/issues/190) ([b2a9c78](https://github.com/unit-mesh/auto-dev/commit/b2a9c78147ed6c6f567865cc17bae1db1b6e6a43))
* **provider:** improve error message and handle package declaration ([a494708](https://github.com/unit-mesh/auto-dev/commit/a49470882c007cf1af09ba06b61ce28ae44b0c42))


### Features

* **provider:** add class insertion if KotlinClass not found ([f1864b5](https://github.com/unit-mesh/auto-dev/commit/f1864b5e33d4524a01f53ff52ba511210c5e8274))
* **rigth-click-actions:** add 'Fix This' action for code refactoring assistance ([ec103c5](https://github.com/unit-mesh/auto-dev/commit/ec103c5c586e5e1b768bb61890427e0798f85bbf))
* **settings:** implement auto-dev update check on startup ([e317c27](https://github.com/unit-mesh/auto-dev/commit/e317c27835828d114e6605572c098e8a6bca0edc))



## [1.8.4-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.3-RC...v1.8.4-RC) (2024-05-23)


### Bug Fixes

* **practise:** correct boolean logic in suggestion filtering ([634fdd9](https://github.com/unit-mesh/auto-dev/commit/634fdd9c9afa7da650bbc29349d5b43edab2257e))
* **settings:** handle null project in LLMSettingComponent and AutoDevCoderConfigurable [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([c4c7ab8](https://github.com/unit-mesh/auto-dev/commit/c4c7ab8278a596028697a849c2ea5b2674638e7a))


### Features

* **chat:** add copy message functionality [#30](https://github.com/unit-mesh/auto-dev/issues/30) ([b3e7af6](https://github.com/unit-mesh/auto-dev/commit/b3e7af6ddff2cee8f4c7e929e443cbd28e9a92d8))
* **git-actions:** move Vcs commit message and release note suggestion actions to ext-git package and update autodev-core.xml to include them in the correct action groups. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([9ac18c0](https://github.com/unit-mesh/auto-dev/commit/9ac18c00f26ea6e7d3d96672c0de35a54f6787d4))
* **json-text-provider:** add new extension point and provider class for JSON text editing support. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([24a4923](https://github.com/unit-mesh/auto-dev/commit/24a4923e45564141f16f6f1cd84cb1cbbdf3ab61))
* **json-text-provider:** refactor to use extension point and remove duplicate code. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([2f04ba2](https://github.com/unit-mesh/auto-dev/commit/2f04ba216794fa5e1909f57ccaa7492ef35c77a0))
* **json-text-provider:** remove unused code to reduce dupcliate to keep same JsonField. [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([33a6491](https://github.com/unit-mesh/auto-dev/commit/33a64911a01237db6c32ced96b3509e8f6d0bcbf))
* **local-bundle:** add new idea-plugin for improved local bundle management [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([d145593](https://github.com/unit-mesh/auto-dev/commit/d14559348f630b2f201156ab199e31bae7fe381e))
* **lookup-manager:** filter suggestions and update plugin version ([b53510b](https://github.com/unit-mesh/auto-dev/commit/b53510bdd394c752054ab08fa1d66f11c8012f2c))
* **openai:** add chat history control based on coderSetting state ([22f54d2](https://github.com/unit-mesh/auto-dev/commit/22f54d2057355cefa50c7227d91f71d42ca6dd6a))
* **openai:** enhance chat history management in OpenAIProvider ([2c893eb](https://github.com/unit-mesh/auto-dev/commit/2c893eb1057e5dd9c31a66f33c870daa0370d641))
* **python:** add __init__ and myfunc to PythonContextTest class and related imports. ([9f19dfb](https://github.com/unit-mesh/auto-dev/commit/9f19dfb4113cf48fa93b730e3b3d989f60576fca))
* **python:** add PythonContextTest class and related imports ([358f045](https://github.com/unit-mesh/auto-dev/commit/358f045c2607ba258462c98062387446db9be7d6))
* **python:** Optimize PythonAutoDevFlow and related components ([e15006e](https://github.com/unit-mesh/auto-dev/commit/e15006e6f9b5c9f6b3a78dc39ee7aff80dc65671))
* **python:** refactor PythonAutoTestService and related classes for improved performance and maintainability. ([d889c77](https://github.com/unit-mesh/auto-dev/commit/d889c77ba43b3fd2e1b440ed75f9d0f02a117e4f))
* **rename:** enhance suggestion filtering in RenameLookupManagerListener ([a167e87](https://github.com/unit-mesh/auto-dev/commit/a167e87db96d7566c3414610b1fea937db4cccfb))



## [1.8.3-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.2-RC...v1.8.3-RC) (2024-05-08)


### Bug Fixes

* **124:** support stop chat ([6ad68aa](https://github.com/unit-mesh/auto-dev/commit/6ad68aa2dfd272b9cf515ae23c65e0e47caff905))
* add missed import ([b604867](https://github.com/unit-mesh/auto-dev/commit/b6048675f198e625a7f3a63e6e3b7a27783c2e34))
* change prompts to fix issue [#156](https://github.com/unit-mesh/auto-dev/issues/156), but this is not the best way [#156](https://github.com/unit-mesh/auto-dev/issues/156) ([e4bb514](https://github.com/unit-mesh/auto-dev/commit/e4bb514a214be3bdb00b22a2844677c8e9700103))
* fix condition issue [#152](https://github.com/unit-mesh/auto-dev/issues/152) ([5e8588d](https://github.com/unit-mesh/auto-dev/commit/5e8588d52b93cb1beccd9c16e9a2b1d6d101d904))
* fix error ([4549dca](https://github.com/unit-mesh/auto-dev/commit/4549dcab2a7d6fad63297ee560df2057b02f9be8))
* fix path issue for [#152](https://github.com/unit-mesh/auto-dev/issues/152) ([62d9e78](https://github.com/unit-mesh/auto-dev/commit/62d9e78d47b4d01cebb5c1677b565bfe488cecfd))
* fix tests ([108fcda](https://github.com/unit-mesh/auto-dev/commit/108fcda2ef976412628ecffae12661a2fcc3aa7a))
* fix typos ([61482a6](https://github.com/unit-mesh/auto-dev/commit/61482a6f19178647fbeef54449c339be2e82d260))
* fix typos for [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([562c868](https://github.com/unit-mesh/auto-dev/commit/562c868ac073f5b9be8996404b4cb613d3e342af))
* **java:** remove unnecessary imports and update method signature for `prepareContainingClassContext` to be more flexible. ([34b4a3c](https://github.com/unit-mesh/auto-dev/commit/34b4a3c69549fd3deaf7f920d5a75ecb80c36720))
* **javascript:** remove JavaScriptContextPrompter and make JavaScript use DefaultContextPrompter [#151](https://github.com/unit-mesh/auto-dev/issues/151) ([4b8a2cd](https://github.com/unit-mesh/auto-dev/commit/4b8a2cd2ef6f908a0bbd5ed6781b640de8ac1fc9))
* **kotlin:** handle source file path in test generation service [#152](https://github.com/unit-mesh/auto-dev/issues/152) ([63e4fe1](https://github.com/unit-mesh/auto-dev/commit/63e4fe162d3c822389a987976a3045472e058c4b))
* **prompt:** refactor to use action instruction for prompt text [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([ea2bac2](https://github.com/unit-mesh/auto-dev/commit/ea2bac296602f496848e933ed6cb11f39a1b626a))
* **serialization:** add kotlinx.serialization support for decoding strings for [#149](https://github.com/unit-mesh/auto-dev/issues/149) ([110fab2](https://github.com/unit-mesh/auto-dev/commit/110fab2d519d27a3234b38e3d84a24a146ab3d7d))
* should not include unselected file when generate commit message [#160](https://github.com/unit-mesh/auto-dev/issues/160) ([97eba22](https://github.com/unit-mesh/auto-dev/commit/97eba22fd6281ca670232ba23a275818f8ebe1c8))
* should not send history when generate documentation ([47c800c](https://github.com/unit-mesh/auto-dev/commit/47c800c8dbda38c5ee0bd94774e833b0eabce1e5))
* should wrap runReadAction when read PsiElment fields [#154](https://github.com/unit-mesh/auto-dev/issues/154) ([1fe918f](https://github.com/unit-mesh/auto-dev/commit/1fe918fe4007fa984871c91a92abcd72c3fd9589))
* **spring:** update SpringMVC library detection logic for core frameworks mapping ([cf1fe51](https://github.com/unit-mesh/auto-dev/commit/cf1fe51e2f3216e85bcf936b1cff2ab54a968e39))
* wrap runReadAction [#154](https://github.com/unit-mesh/auto-dev/issues/154) ([90b99b7](https://github.com/unit-mesh/auto-dev/commit/90b99b7b8e554f0feb70560a954066f7e90b9906))


### Features

* add regex support to custom intention matching for [#174](https://github.com/unit-mesh/auto-dev/issues/174) ([dc1f94e](https://github.com/unit-mesh/auto-dev/commit/dc1f94eccc619b4d86e91b9220c6edd9d3ff28d4))
* can provider genericity parameter to LLM when generate test ([65e5117](https://github.com/unit-mesh/auto-dev/commit/65e5117ba03138b418937c5e4745df3223e30cbc))
* **compiler:** add support for DevInUsed in WriteInsCommand and fixed [#172](https://github.com/unit-mesh/auto-dev/issues/172) ([7bb3d77](https://github.com/unit-mesh/auto-dev/commit/7bb3d776f82f54c4d376859b0a68386e86f12b12))
* **console-action:** refactor FixThisAction to use ErrorPromptBuilder for better error handling and display. [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([3d4ded3](https://github.com/unit-mesh/auto-dev/commit/3d4ded343fc3358531a587880d050afd5a20f51c))
* **console-action:** refactor FixThisAction to use ErrorPromptBuilder for better error handling and display. [#151](https://github.com/unit-mesh/auto-dev/issues/151) ([003c10f](https://github.com/unit-mesh/auto-dev/commit/003c10feaf0d6263125fc15a44fa26925d86ca95))
* **csharp:** remove CsharpContextPrompter and refactor CSharpClassContextBuilder to use DefaultContextPrompter [#151](https://github.com/unit-mesh/auto-dev/issues/151) ([b71e8c3](https://github.com/unit-mesh/auto-dev/commit/b71e8c33603464dbc392e19323e788688d7932c7))
* **devins-lang:** add refactor commands and related test data [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([eaf0add](https://github.com/unit-mesh/auto-dev/commit/eaf0add0187774df05d5b8b065cbe253435de2fe))
* **devins-lang:** add refactor commands and related test data [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([4400d6f](https://github.com/unit-mesh/auto-dev/commit/4400d6fe9e3280a8245a7b8c1f0e09374f05f1a1))
* **devins-lang:** add rename refactor command and related test data [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([c8ff245](https://github.com/unit-mesh/auto-dev/commit/c8ff245e24d8b14c3ed581dc740a26d98f5d6cc6))
* **devins-lang:** add spike result for refactoring implementation for [#181](https://github.com/unit-mesh/auto-dev/issues/181) ([23fd4cf](https://github.com/unit-mesh/auto-dev/commit/23fd4cff137cac9e740b2839dc29eeb36d0ddb7f))
* **devins-lang:** add support for flow control in DevInsPromptProcessor.kt and DevInsCompiler.kt [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([2908e41](https://github.com/unit-mesh/auto-dev/commit/2908e412e409849b304740a75c8ebe26fc5fe30b))
* **exts-git:** extract and rename git plugin support [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([eb55792](https://github.com/unit-mesh/auto-dev/commit/eb5579239858e4123c23d9be41c2693688639d5d))
* **exts-git:** extract git plugin support and rename refactoring implementation for [#183](https://github.com/unit-mesh/auto-dev/issues/183) ([3970a5f](https://github.com/unit-mesh/auto-dev/commit/3970a5f681ae26915d89380c16b496b5511c18b7))
* **kotlin:** add  parameter to Version: ImageMagick 7.1.1-30 Q16-HDRI x86_64 22086 https://imagemagick.org ([84c0700](https://github.com/unit-mesh/auto-dev/commit/84c0700a64f91a515fc3a090bcf2b98be7890916))
* **kotlin:** optimize imports and refactor isService/isController methods for better readability. ([7b8e88a](https://github.com/unit-mesh/auto-dev/commit/7b8e88ad086fc18dae0fbeb92a83042701d7abf5))
* provide current class when generate test ([1cdd5e6](https://github.com/unit-mesh/auto-dev/commit/1cdd5e62c919123aa03c0cddab2d47203a93ae85))
* **rust:** remove RustContextPrompter and use RustVariableContextBuilder for variable context ([9b2b588](https://github.com/unit-mesh/auto-dev/commit/9b2b58898fd9eca360efacfec71af1bdb9ed39c6)), closes [#151](https://github.com/unit-mesh/auto-dev/issues/151)
* **stop:** update icon for [#149](https://github.com/unit-mesh/auto-dev/issues/149) && closed [#124](https://github.com/unit-mesh/auto-dev/issues/124) ([3fe1f1a](https://github.com/unit-mesh/auto-dev/commit/3fe1f1a06c2d8dae4f80f8c0c0140e42127ac37d))
* 增加新特性，可以正则过滤编辑器选中的内容，提问内容更加精简，明确 ([d47aec1](https://github.com/unit-mesh/auto-dev/commit/d47aec107870807f1532722d343c7f2b6bfe908b))



## [1.8.2-RC](https://github.com/unit-mesh/auto-dev/compare/v1.8.1...v1.8.2-RC) (2024-04-15)


### Bug Fixes

* **devins-lang:** optimize directory creation logic for file creation [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([42d1219](https://github.com/unit-mesh/auto-dev/commit/42d1219d3c440250fc68f3520db6350d7b4235e6))
* fix async issue for [#144](https://github.com/unit-mesh/auto-dev/issues/144) ([36923e4](https://github.com/unit-mesh/auto-dev/commit/36923e4e880a4c2b0cef4d70376ea37a3be19736))
* **test:** improve read access for non-identifier owners in TestCodeGenTask for typescript ([ba8c4a2](https://github.com/unit-mesh/auto-dev/commit/ba8c4a2eeb3745e8425277c85be8451cebe6b78a))


### Features

* **agent:** add support for DevIns language in code blocks [#144](https://github.com/unit-mesh/auto-dev/issues/144) ([ddd7565](https://github.com/unit-mesh/auto-dev/commit/ddd75651366a0629164cb0331f25f9d2ca0d7e40))
* **devins-lang:** add code parsing and notification for DevIns response [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([2bf268b](https://github.com/unit-mesh/auto-dev/commit/2bf268b67d13e948236ee8293c63705fec5f2996))
* **devins-lang:** optimize directory creation and add runInEdt for smoother execution [#146](https://github.com/unit-mesh/auto-dev/issues/146) ([7724a74](https://github.com/unit-mesh/auto-dev/commit/7724a74dd7e4328ac761cb2b12949cf6383b0a64))
* **java:** add code parsing for LivingDocumentationTask ([aef1974](https://github.com/unit-mesh/auto-dev/commit/aef197429bfefef04634b748179c9fe55ddabbfa))



## [1.8.1](https://github.com/unit-mesh/auto-dev/compare/v1.8.0...v1.8.1) (2024-04-10)


### Bug Fixes

* **connection:** rest result before flow start ([c4718a3](https://github.com/unit-mesh/auto-dev/commit/c4718a301d523e0656434425e9c6098d0e5e0024))
* **devins:** refactor write action to use runReadAction and WriteCommandAction for better performance and error handling [#143](https://github.com/unit-mesh/auto-dev/issues/143) ([8525a0b](https://github.com/unit-mesh/auto-dev/commit/8525a0b1ece8585e16d6c0d24c6565c85f7859c5))
* fix import issue [#143](https://github.com/unit-mesh/auto-dev/issues/143) ([f6b3fa9](https://github.com/unit-mesh/auto-dev/commit/f6b3fa947f42bb5a8a8a25259438fbb7e727cec1))
* fix the wrong path of prompt templete file path ([f38b53a](https://github.com/unit-mesh/auto-dev/commit/f38b53a5652cbadb0d280d1ec3d1f779133c9798))
* handle null in shortFormat() method ([601120d](https://github.com/unit-mesh/auto-dev/commit/601120d27823f529e8d770bbaf77a7b2bccc2962))
* psi element text should not be null [#123](https://github.com/unit-mesh/auto-dev/issues/123) ([9a442cb](https://github.com/unit-mesh/auto-dev/commit/9a442cbed22b674e1fdeaaaa0547e097f088a9bf))
* **rename:** ensure correct handling of target elements and improve error handling. ([37ceab6](https://github.com/unit-mesh/auto-dev/commit/37ceab619fb02b50b9f926ba3ab1e8116d34fa88))
* should not keep history when complete code ([e79a8b7](https://github.com/unit-mesh/auto-dev/commit/e79a8b79953d980418813b0a3906116929c72e9a))
* should set isIndeterminate to false if fraction exist [#137](https://github.com/unit-mesh/auto-dev/issues/137) ([91f1875](https://github.com/unit-mesh/auto-dev/commit/91f187593b5c686fbb81207eadbfd1a6d033d9fa))
* **typescript:** refactor JSDoc comment creation and insertion logic to handle null values and exceptions more gracefully. ([30f301e](https://github.com/unit-mesh/auto-dev/commit/30f301e688cb9bd8ccb8b047051cb904f4866a9b))


### Features

* **custom:** Refactor variable resolver and prompt service in `CustomActionBaseIntention.kt` to improve efficiency and readability [#136](https://github.com/unit-mesh/auto-dev/issues/136) ([263e1a8](https://github.com/unit-mesh/auto-dev/commit/263e1a85404014d9d367a3405e12c6a503456eee))
* **devins:** enable write new content to file [#143](https://github.com/unit-mesh/auto-dev/issues/143) ([ad029f1](https://github.com/unit-mesh/auto-dev/commit/ad029f10207e6e21636a9fd611b5e9fab7359fcc))
* **devti-lang:** add support for creating files with content in specific directories [#143](https://github.com/unit-mesh/auto-dev/issues/143) ([df38c29](https://github.com/unit-mesh/auto-dev/commit/df38c29f31ee915cf85baca9cbc9d6ae407f8fa5))
* **devti-lang:** improve directory creation logic for multiple levels [#143](https://github.com/unit-mesh/auto-dev/issues/143) ([23a71e1](https://github.com/unit-mesh/auto-dev/commit/23a71e128961664a577f7ef3c2c39acfa4e7275e))
* **devti-lang:** improve directory creation logic for multiple levels [#143](https://github.com/unit-mesh/auto-dev/issues/143) ([32f263e](https://github.com/unit-mesh/auto-dev/commit/32f263ecb62732fb34354078fa28da7fc7e9ba61))
* **exts:database:** Rename and refactor database extension module to `ext-database`, including changes to build.gradle.kts, settings.gradle.kts, and source file names. ([fab0fa5](https://github.com/unit-mesh/auto-dev/commit/fab0fa5d05ae0ed47ca43914799bd7505aad34f7))
* **intentions:** simplify `TestCodeGenTask` and fix `getElementToAction` logic. ([5f139d9](https://github.com/unit-mesh/auto-dev/commit/5f139d94bb16ce62fc283499e7e374457b3c7f74))
* **llm:** add simiple fix option to disable history auto-formatting in LLM stream creation [#141](https://github.com/unit-mesh/auto-dev/issues/141) ([dfc3840](https://github.com/unit-mesh/auto-dev/commit/dfc3840f18932aaa9753a913f604e3d3a6388a91))
* **typescript:** refactor JSDoc comment insertion logic to handle null values and exceptions more gracefully [#2](https://github.com/unit-mesh/auto-dev/issues/2) ([5231b70](https://github.com/unit-mesh/auto-dev/commit/5231b705e14976104c602aaf6549c2113c38afe0))
* **typescript:** simplify insert logic [#2](https://github.com/unit-mesh/auto-dev/issues/2) ([8083b51](https://github.com/unit-mesh/auto-dev/commit/8083b51d02728e062be6fc7ed8207922fd46a3d9))



# [1.8.0](https://github.com/unit-mesh/auto-dev/compare/v1.7.5...v1.8.0) (2024-04-05)


### Bug Fixes

* **actions:** add ActionUpdateThread.EDT to ensure UI updates occur on the new EDT thread for smoother user experience. ([3890a73](https://github.com/unit-mesh/auto-dev/commit/3890a73bc3e4277677f64ab7a4e27f1f0520422b))
* **commit-message:** optimize commit message generation by incorporating the original commit message. ([5d8e08f](https://github.com/unit-mesh/auto-dev/commit/5d8e08f3d78265175440a32ffab1fd5521e4bac5))
* **compiler:** use consistent error message format across commands ([8ce54d0](https://github.com/unit-mesh/auto-dev/commit/8ce54d0bfa0dee4f55e81e70512e12bba57e81c3))
* delete duplicate name in zh.properties file ([f38a77d](https://github.com/unit-mesh/auto-dev/commit/f38a77d4f718136ee0b0c5a0a79595ad1b615366))
* **devins-lang:** remove unnecessary conditionals and simplify response handling ([18abafc](https://github.com/unit-mesh/auto-dev/commit/18abafc71023c1958f494d26bf9889be2bc3b09b))
* **devins-lang:** remove unused imports and refactor compiler creation logic ([2229789](https://github.com/unit-mesh/auto-dev/commit/2229789979cb6cf752959ca6f6eb695e0e3768e8))
* fix import issue ([d9a4762](https://github.com/unit-mesh/auto-dev/commit/d9a4762656bc2a1b9ac5a27b9c2c6b7da61431d7))
* fix package issue ([a6f9cf3](https://github.com/unit-mesh/auto-dev/commit/a6f9cf30f9d3a0da9a8a81c750f5716d409de9e7))
* **java:** add PsiErrorElement handling and collect syntax errors [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([e78eca4](https://github.com/unit-mesh/auto-dev/commit/e78eca49f0f6c1350d5f2c59da91b59652d3a9a4))
* **kotlin-provider:** add PsiErrorElement handling and collect syntax errors ([1a67593](https://github.com/unit-mesh/auto-dev/commit/1a67593b46111696b24fcf91986189190c5cecfb))
* **language-processor:** handle local commands in DevInsCustomAgentResponse ([ae8b4db](https://github.com/unit-mesh/auto-dev/commit/ae8b4db126938266ec022e50bdddf157b2e31b13))
* **llm:** handle null response in CustomSSEProcessor ([5de1db3](https://github.com/unit-mesh/auto-dev/commit/5de1db3ad8e5fe328e290128b59e49301fe66340))
* **llm:** handle null response in CustomSSEProcessor ([41f2c72](https://github.com/unit-mesh/auto-dev/commit/41f2c722cfa2ab1a74ef02619553ce7fd09c69a5))
* **refactor:** add PsiErrorElement handling and collect syntax errors [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([bbc691a](https://github.com/unit-mesh/auto-dev/commit/bbc691a59d5d16c633a5f2bc4db9b3c1a1428ca5))
* **refactor:** refactor rename lookup manager listener to use custom rename lookup element [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([82caa05](https://github.com/unit-mesh/auto-dev/commit/82caa058280256c1df8973a333687542416136b1))
* **rust:** update CargoCommandConfigurationType to support 241 version ([ed892d9](https://github.com/unit-mesh/auto-dev/commit/ed892d987a4963c2abd806136158f08077712b13))
* **terminal:** resolve compatibility issues in version 222 [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([559edb3](https://github.com/unit-mesh/auto-dev/commit/559edb356523e18c571fce81825ca2bb867fb9d0))
* **terminal:** resolve compatibility issues in version 222 by refactoring rename lookup manager listener to use custom rename lookup element and improving shell command input popup. This commit fixes the compatibility issues in version 222 of the terminal extension by refactoring the rename lookup manager listener to use a custom rename lookup element. It also improves the shell command input popup by using more appropriate imports and methods from the UIUtil class. ([c5916cd](https://github.com/unit-mesh/auto-dev/commit/c5916cd4026866a23496245e7d15eddb2974da07))
* **ui:** remove unnecessary todo comments and refactor binding logic for consistency ([dbfa022](https://github.com/unit-mesh/auto-dev/commit/dbfa022f9141200d30da2857f3eecc91d0f292cf))


### Features

* add Chinese to more UI ([0f9cc68](https://github.com/unit-mesh/auto-dev/commit/0f9cc684f9190da36c3c7c5c4bc7fbc89beed920))
* add nature language directory for support Chinese prompts template ([fd6b889](https://github.com/unit-mesh/auto-dev/commit/fd6b8899d3102836ffd723f5338b12906cda6b8a))
* **autodev:** add toggle in naming for [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([44008dc](https://github.com/unit-mesh/auto-dev/commit/44008dc0a538d441f1a4e55cbac6d483b27d029c))
* change default shortcut of inlay complete code ([0de56fe](https://github.com/unit-mesh/auto-dev/commit/0de56fe25999a01fd9c5a8b8dee02eae43e809df))
* **chat:** improve refactoring support by adding post-action support and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([2369148](https://github.com/unit-mesh/auto-dev/commit/23691481036e14e24c1dd8a94926a74af54e6238))
* **chat:** improve refactoring support by adding post-action support and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([e6ec4de](https://github.com/unit-mesh/auto-dev/commit/e6ec4de6e65c68f51e79d60a98d7fc5bbb6f4b8a))
* **commit-message:** improve commit message generation template for Chinese and English users, ensuring clarity and adherence to best practices. ([f80495f](https://github.com/unit-mesh/auto-dev/commit/f80495f555907788573cc66042af441a4f9ca9e6))
* **database:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([7526032](https://github.com/unit-mesh/auto-dev/commit/7526032bc55750aa0e8c02a25d47797a0c5b1807))
* **database:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([53412a9](https://github.com/unit-mesh/auto-dev/commit/53412a9fa1e861ec46b8664bcc68cd884e7413a2))
* **devins-lang:** add support for line info in commands ([72efaef](https://github.com/unit-mesh/auto-dev/commit/72efaef3601b210d7a4f868ff4127b70105041d9))
* **devins-lang:** add support for line info in commands ([d524095](https://github.com/unit-mesh/auto-dev/commit/d524095392ac70cf1d47bda378baffcaa3ed86b8))
* **devins-lang:** add support for line info in commands and improve symbol resolution formatting ([2d8d1f1](https://github.com/unit-mesh/auto-dev/commit/2d8d1f14181b21865f161abac3c4f21bd8f198b5))
* **devins-lang:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([a9b9b77](https://github.com/unit-mesh/auto-dev/commit/a9b9b777b57069ed11d429f0caeda0f3a191425f))
* **devins-lang:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([6872c07](https://github.com/unit-mesh/auto-dev/commit/6872c070529641f854e9f025ae53cf22a25b5d94))
* **devins-lang:** add support for parsing and verifying SQL scripts before inserting them into the editor. ([12b768b](https://github.com/unit-mesh/auto-dev/commit/12b768b12960c4ab32260f18099d1a64cd329dc8))
* **devins-lang:** improve symbol resolution for file commands ([ef0ee46](https://github.com/unit-mesh/auto-dev/commit/ef0ee46bed15b8a41482ad5b41b0e22b4e896de0))
* **devins-lang:** refactor file handling logic to improve performance and add support for line info in commands. ([710e945](https://github.com/unit-mesh/auto-dev/commit/710e94524b05edda68a0bc10236bfe03b68cb559))
* **devins-lang:** try add support for parsing and verifying SQL scripts before inserting them into the editor. ([29b6e61](https://github.com/unit-mesh/auto-dev/commit/29b6e610be4e8daf1c0d913266f1a6bee0930791))
* **docs:** Enhance the commit message generation section of the Git documentation to include an optimized process for generating clear and accurate commit messages. ([1b34335](https://github.com/unit-mesh/auto-dev/commit/1b34335bf1d2bf51b60f85991604f1e4694273bf))
* **docs:** update refactoring documentation with additional examples and suggestions. [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([814493b](https://github.com/unit-mesh/auto-dev/commit/814493be0096a52ace0a0906056f2cdaba74b728))
* **ext-terminal:** add ShellSuggestContext class and documentation [#1235](https://github.com/unit-mesh/auto-dev/issues/1235) ([e815743](https://github.com/unit-mesh/auto-dev/commit/e815743ea5c337c2038dbc195a79bc3974f8af13))
* **ext-terminal:** introduce NewTerminalUiUtil class and refactor suggestCommand method to support new UI context [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([3533b1d](https://github.com/unit-mesh/auto-dev/commit/3533b1d5b2168767fb87efb5e231c9199a5d77fc))
* **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([712d45c](https://github.com/unit-mesh/auto-dev/commit/712d45c2e9a3d599864885933f39673c29981192))
* **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([41b4b7e](https://github.com/unit-mesh/auto-dev/commit/41b4b7e9a59a3f88918f0618c07a5d5f33afad20))
* **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([7d87fd0](https://github.com/unit-mesh/auto-dev/commit/7d87fd07e4d54abd539f54446277a9bafa85928f))
* **ext-terminal:** refactor suggestCommand method to support 241 new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135). ([6d4e800](https://github.com/unit-mesh/auto-dev/commit/6d4e80087fb154ba1961a760899c4a969aa41a2a))
* **ext-terminal:** refactor suggestCommand method to support new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([60d15ee](https://github.com/unit-mesh/auto-dev/commit/60d15eec37c752fa1edea7476a1f9bd13449b6e4))
* **ext-terminal:** refactor suggestCommand method to support new UI context and add TerminalUtil class for message sending [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([633e393](https://github.com/unit-mesh/auto-dev/commit/633e3931eb353bfb6e2de99fffb78e7ae52fe75f))
* **i18n:** update Chinese translations for improved clarity and consistency. [#125](https://github.com/unit-mesh/auto-dev/issues/125) ([dbc4873](https://github.com/unit-mesh/auto-dev/commit/dbc4873f462f3e4c392a1299989d6de2cea96834))
* In generating code, remove markdown chars ([34c5c84](https://github.com/unit-mesh/auto-dev/commit/34c5c8442aa4c6a8bf2d17e5f20dbf14b469d6c1))
* **language-processor:** add support for DevIns language processing ([b2dcf1e](https://github.com/unit-mesh/auto-dev/commit/b2dcf1e92f3f8d90e91f2467f59b71546bb3e4d5))
* **language-processor:** add support for local commands in DevInsCustomAgentResponse ([4726633](https://github.com/unit-mesh/auto-dev/commit/4726633185f152629cdbae620ca3145ac89c254c))
* **prompt:** Enhance Chinese prompt generation logic to ensure generated text is clearer and more accurate, following best practices. ([793041d](https://github.com/unit-mesh/auto-dev/commit/793041de8b72634e8805802c52ceb45da873f4d1))
* **prompting:** improve logging of final prompts for better debugging and remove unnecessary println statement. ([c2b0fed](https://github.com/unit-mesh/auto-dev/commit/c2b0fed3faee0308b27c85db6897329445bcf91a))
* **prompting:** introduce BasicTextPrompt class to simplify prompt text construction and improve code readability [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([ac8fb12](https://github.com/unit-mesh/auto-dev/commit/ac8fb124fbab78fc84cde78a1c54211085b83bbf))
* **refactor:** add post-action support for refactoring and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([18818dc](https://github.com/unit-mesh/auto-dev/commit/18818dcbecf95210484dd99e8bd5f630e4bab93a))
* **refactor:** add PsiErrorElement handling and collect syntax errors [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([14092e0](https://github.com/unit-mesh/auto-dev/commit/14092e08eb25818af985d3a3d8784727e377f553))
* **refactor:** add RenameLookupElement to improve UI support and code completion [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([2d7d81c](https://github.com/unit-mesh/auto-dev/commit/2d7d81c53084eafa7bd58772f28ef9eb9724c0e8))
* **refactor:** add RenameLookupElement to improve ui support and code completion [#132](https://github.com/unit-mesh/auto-dev/issues/132) [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([5caa4c8](https://github.com/unit-mesh/auto-dev/commit/5caa4c8a53a07ff6a7cbb669e00b3595f86b37ec))
* **refactor:** add RenameLookupManagerListener to improve refactoring support by adding post-action support and code completion [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([529b72d](https://github.com/unit-mesh/auto-dev/commit/529b72dd43df26af7f630ee999193135f07caf5d))
* **refactoring:** add RenameLookupElement to improve UI support and code completion [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([6348f1e](https://github.com/unit-mesh/auto-dev/commit/6348f1e16b14f54638ece1a43b63ad39e3606bbe))
* **refactoring:** extract method for readable [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([0d1ac46](https://github.com/unit-mesh/auto-dev/commit/0d1ac460aa8ceff2c908379c9bb1ae141eb97ba9))
* **refactoring:** improve RenameLookupManagerListener by adding RenameLookupElement and using ApplicationManager to invoke the stream function in a non-blocking way, resolving [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([c1cc521](https://github.com/unit-mesh/auto-dev/commit/c1cc521b65fa6758a6fec942e67e1a7154328efb))
* **refactoring:** improve RenameLookupManagerListener by adding RenameLookupElement and using ApplicationManager to invoke the stream function in a non-blocking way, resolving [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([baf8809](https://github.com/unit-mesh/auto-dev/commit/baf880918b49772b6dc3e04b8ab29555caf5a0f3))
* **refactoring:** introduce template-based refactoring suggestions [#129](https://github.com/unit-mesh/auto-dev/issues/129) ([f965194](https://github.com/unit-mesh/auto-dev/commit/f965194a92070f86da9dc1aef7ef26a8bb469dd7))
* **refactoring:** move check flag before run [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([a6a2e63](https://github.com/unit-mesh/auto-dev/commit/a6a2e63b642fb8a962bfbe8c4490cde7d64003ed))
* **refactoring:** try to improve user exp for lookup [#132](https://github.com/unit-mesh/auto-dev/issues/132). ([976e809](https://github.com/unit-mesh/auto-dev/commit/976e80932b0a243f6e0d8a22422375008bd55353))
* rename package from prompts/openai to prompts/default ([5ba2525](https://github.com/unit-mesh/auto-dev/commit/5ba252577ef013fd73e6454111dbcaae03605d1c))
* **rename-suggestion:** improve rename suggestion logic and add logging [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([3324a79](https://github.com/unit-mesh/auto-dev/commit/3324a79cb38f24abdba65f1316e80714b3937f1c))
* **rename-suggestion:** improve rename suggestion logic and add logging [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([c70fea8](https://github.com/unit-mesh/auto-dev/commit/c70fea83ee97231e8989efd44fc327490de7cd7e))
* **rename-suggestion:** improve rename suggestion logic and add logging [#132](https://github.com/unit-mesh/auto-dev/issues/132) ([184174c](https://github.com/unit-mesh/auto-dev/commit/184174ca8f14bb451957239cbf2376c4951c7d48))
* **settings:** add test connection feature for LLM settings ([c2dd6ba](https://github.com/unit-mesh/auto-dev/commit/c2dd6baf0a87924e1374b3d2b993208030ac509f))
* **settings:** rename 'testConnection' to 'testLLMConnection' and update related references. ([9c784d9](https://github.com/unit-mesh/auto-dev/commit/9c784d96162d77b9a74c4660223a0d39519d1f4d))
* **shell-suggest:** add today's date and OS information to context [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([fe3917a](https://github.com/unit-mesh/auto-dev/commit/fe3917a16aaf48240ed70f257e946c804e920dcf))
* should cut down prompts when they exceed the max token lenghth ([aafb936](https://github.com/unit-mesh/auto-dev/commit/aafb9360999bb9da141fa0a6f4b3833cd34f4226))
* should log exception error when bundle failed to set parent ([d130486](https://github.com/unit-mesh/auto-dev/commit/d130486e9fd3caf6d40177935af6898fe65e5378))
* should return en as default prompts template if target language template not exist ([31ffeee](https://github.com/unit-mesh/auto-dev/commit/31ffeeed229c8d63333195efe13704c6bb6eccec))
* simple prompts support Chinese ([a9633b3](https://github.com/unit-mesh/auto-dev/commit/a9633b319aeca3095ebbb3ad7cf78f67fb95fc56))
* support Chinese in UI ([49e773a](https://github.com/unit-mesh/auto-dev/commit/49e773a38dba77f5f74e2c85559b4ff201c5151f))
* **terminal:** add AI-generated shell script support [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([fc977a5](https://github.com/unit-mesh/auto-dev/commit/fc977a59395ce90d4dd0a35112d4e5a24c9fa0df))
* **terminal:** add basic support for AI-generated shell scripts [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([a2244d9](https://github.com/unit-mesh/auto-dev/commit/a2244d9204a72e091b7cefa5abe236248ebc3161))
* **terminal:** add compatibility support for 222 and 233 versions ([85175f8](https://github.com/unit-mesh/auto-dev/commit/85175f88a1bc77b8a8a562ffea5cbadae53ed623))
* **terminal:** add shell command suggestion feature [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([0c9a04b](https://github.com/unit-mesh/auto-dev/commit/0c9a04b347bfdcff728d45cd36a8a58ff0cc8690))
* **terminal:** add shell command suggestion feature [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([aea4114](https://github.com/unit-mesh/auto-dev/commit/aea41145e5ec77811cf73d2493441045d89bd8ce))
* **terminal:** add shell tool detection and context-aware command suggestions [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([5538584](https://github.com/unit-mesh/auto-dev/commit/55385847f2bee6ed6ef4a12f250882885a528687))
* **terminal:** add spike result for [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([e511179](https://github.com/unit-mesh/auto-dev/commit/e511179991a37689c4f6217ea284a7c5e5ecf5ad))
* **terminal:** improve shell command input popup [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([635ad7f](https://github.com/unit-mesh/auto-dev/commit/635ad7fcc7ef4e53c646b079d80d0cb17d4b0469))
* **terminal:** improve shell command suggestion output formatting [#135](https://github.com/unit-mesh/auto-dev/issues/135) ([35bed12](https://github.com/unit-mesh/auto-dev/commit/35bed12e920e4ad2bdf5f361089212354fe186d8))
* **terminal:** improve shell command suggestion output formatting and add support for line commands ([c37ef91](https://github.com/unit-mesh/auto-dev/commit/c37ef910521fb253d41bd179003ec64d6d6d5b1e))
* **terminal:** update relative point to use RelativePoint for improved popup positioning ([5279966](https://github.com/unit-mesh/auto-dev/commit/5279966810ef4bc3595768375cb13ac4f0353093))
* **terminal:** update tooltip text for default message ([ae28f7d](https://github.com/unit-mesh/auto-dev/commit/ae28f7d92ae9c0cd80be33b005e3f588576ae372))
* translate some prompts template to Chinese ([c907ede](https://github.com/unit-mesh/auto-dev/commit/c907eded4acd8c4eb4efba1c18f453ae6238ed96))



## [1.7.5](https://github.com/unit-mesh/auto-dev/compare/v1.7.4...v1.7.5) (2024-03-29)


### Bug Fixes

* **custom-sse-processor:** handle non-standard response format and log parsing errors ([20dda56](https://github.com/unit-mesh/auto-dev/commit/20dda56980c963fce22a92077857f25170ad0ce3))
* **scala-test-service:** comment out code causing compatibility issues in version 222~232 ([92eb05e](https://github.com/unit-mesh/auto-dev/commit/92eb05e8fb52f34dc3a963c40c47490bcd426637))



## [1.7.4](https://github.com/unit-mesh/auto-dev/compare/v1.7.3...v1.7.4) (2024-03-28)


### Bug Fixes

* **core:** handle null response in JsonPath parsing ([7e60675](https://github.com/unit-mesh/auto-dev/commit/7e60675043123e566eb652fdf6acdc77f17670a8))
* **core:** openAI custom model not work as expected ([d4eee77](https://github.com/unit-mesh/auto-dev/commit/d4eee7778e6698db378292733b927f408bed7f78)), closes [#119](https://github.com/unit-mesh/auto-dev/issues/119)
* **devins-cpp:** move test config for Intellij IDEA 223 only, which is C++  test configurations and test discovery [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([fb588e3](https://github.com/unit-mesh/auto-dev/commit/fb588e30ca0d0b65e2c8d4a2c9df23dcf12c7e3b))
* **devins-lang:** add basic handle for exitCode=-1 to recall function ([6bcdf15](https://github.com/unit-mesh/auto-dev/commit/6bcdf159a05a2295895027a86cebc59ec9a78279))
* **devins-lang:** fix process termination listener [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([ff38ea9](https://github.com/unit-mesh/auto-dev/commit/ff38ea9b97d5e801883d889482d2c17d06fce192))
* **devins-lang:** handle nullable inputStream and improve string concatenation for better performance and readability. ([910daa0](https://github.com/unit-mesh/auto-dev/commit/910daa0446b15ddb5b9b8883ff41e3d3f49e7ce1))
* **devins-lang:** improve file content extraction ([5f8dc29](https://github.com/unit-mesh/auto-dev/commit/5f8dc29616978779e995b2fa941038cbe51b02be)), closes [#100](https://github.com/unit-mesh/auto-dev/issues/100)
* **devins-lang:** improve file writing performance [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([6340666](https://github.com/unit-mesh/auto-dev/commit/6340666063c9a1fc9ec395c6e9f9807a79b416b5))
* **error-handling:** ensure correct line range calculation in ErrorMessageProcessor ([fc47e49](https://github.com/unit-mesh/auto-dev/commit/fc47e492de703a33e64b7aba4d63baff3a7ea708))
* fix IDEA 222 error in get changes data ([faaa7c9](https://github.com/unit-mesh/auto-dev/commit/faaa7c922df5dd99e7375e289240cd7b07ca3cf0))
* **java-auto-test:** ensure thread safety when finding and parsing PsiJavaFile ([ee7a79c](https://github.com/unit-mesh/auto-dev/commit/ee7a79c407d2d0d64e4eac1403747c7e1195786b))
* **run-service:** ensure correct process lifecycle handling and remove unnecessary imports ([cdec106](https://github.com/unit-mesh/auto-dev/commit/cdec106daf1cf1413be870bd80cf8454c8fe5ac8))


### Features

* add custom AI engine setting for inlay code complete ([7de0431](https://github.com/unit-mesh/auto-dev/commit/7de0431b7fd49fddfe3817c9762030e12c76bb7a))
* add inlay code complete custom ai engine toggle in dev coder config ([268f309](https://github.com/unit-mesh/auto-dev/commit/268f309f7b798261f31fc85d9e92e43b2bc3edc7))
* **auto-test:** refactor and optimize auto-test service implementations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3c69b77](https://github.com/unit-mesh/auto-dev/commit/3c69b772f29011a3872bb81795cb7cc853fbc6ce))
* **browser:** init tool code [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([5ca636c](https://github.com/unit-mesh/auto-dev/commit/5ca636c7156178403ac8a855c78421df1c4e1b52))
* **devins-android:** init Android test service support ([24a5da1](https://github.com/unit-mesh/auto-dev/commit/24a5da1b2b28d538c2cfc04f418b81c02401e3c9))
* **devins-cpp:** add support for C++ test configurations and test discovery [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([23865dd](https://github.com/unit-mesh/auto-dev/commit/23865dd46f87d780534a91f337a642750524e320))
* **devins-cpp:** add support for IDEA version 222 OCLanguage in test discovery ([551d815](https://github.com/unit-mesh/auto-dev/commit/551d815950e47ae9d73973b8d0dcce598fd29305))
* **devins-cpp:** refactor for factory usage [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([9dd5c48](https://github.com/unit-mesh/auto-dev/commit/9dd5c48e8d4d7b82c66cdffc8b404ff4b8f9cd74))
* **devins-golang:** add support for Golang run configurations and test context provider [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([f1ddea0](https://github.com/unit-mesh/auto-dev/commit/f1ddea0ed6b3c597c3347edeb734d05cef114bfc))
* **devins-kotlin:** refactor RunService to use new ExecutionManager API [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([8e47d2e](https://github.com/unit-mesh/auto-dev/commit/8e47d2e59b7867bb374fd7e4747dedf1e14d41cc))
* **devins-lang:** add docs support for built-in command examples [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([ebacccd](https://github.com/unit-mesh/auto-dev/commit/ebacccd2617746eed607354d8bb80f729b7339bb))
* **devins-lang:** add markdown support for built-in command examples ([8bd3bce](https://github.com/unit-mesh/auto-dev/commit/8bd3bcecbe8228e1ae6fc0596e37a61b5e45527d))
* **devins-lang:** add support for browsing web content with new command `/browse` ([5e8fac4](https://github.com/unit-mesh/auto-dev/commit/5e8fac471a6c65bee450f1aa593fbd0892660c06))
* **devins-lang:** add support for built-in command examples [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([68fd6b6](https://github.com/unit-mesh/auto-dev/commit/68fd6b6afcc58144255494829d3631a041b4b207))
* **devins-lang:** add support for LLM responses in DevInsConversations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([5f9bf7f](https://github.com/unit-mesh/auto-dev/commit/5f9bf7faf520965d7a4250d8011c71942de9a8da))
* **devins-lang:** add support for processing flag comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([13b796f](https://github.com/unit-mesh/auto-dev/commit/13b796f7ba92d102b621ace5b965d88dd9fa8d03))
* **devins-lang:** improve conversation service and compiler [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([261780f](https://github.com/unit-mesh/auto-dev/commit/261780f17cc279599d2fd69e877875a325f995fd))
* **devins-lang:** introduce new ShellRunService to support running shell scripts. This service simplifies the execution of shell commands within the DevIns IDE, enhancing the user experience. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3ce99a7](https://github.com/unit-mesh/auto-dev/commit/3ce99a7799457ad57fadb50e5d659161f8bfffe8))
* **devins-lang:** introduce new ShellRunService to support running shell scripts. This service simplifies the execution of shell commands within the DevIns IDE, enhancing the user experience. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([1c48d01](https://github.com/unit-mesh/auto-dev/commit/1c48d01b0fc64bc99785125c0ab608ddadf57d37))
* **devins-lang:** refactor reorg conversation [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([56db7e5](https://github.com/unit-mesh/auto-dev/commit/56db7e5b6d1f92f2a25be53943837df4440d7785))
* **devins-lang:** refactor reorg conversation [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([fc307a9](https://github.com/unit-mesh/auto-dev/commit/fc307a93a6657846c65a32f0435532c97e307ad3))
* **devins-lang:** remove unused methods [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([cd1bf89](https://github.com/unit-mesh/auto-dev/commit/cd1bf897ee2b7a7af52d291f77af1af4d9cbe808))
* **devins-python:** add support for creating Python run configurations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([691fff5](https://github.com/unit-mesh/auto-dev/commit/691fff55851f71ea439bd6511b109f3ce67bf4cc))
* **devins-rsut:** add support for creating Rust run configurations [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([802d634](https://github.com/unit-mesh/auto-dev/commit/802d634407606674cebeed09093e1698aeafa4dc))
* **devins-run:** add default langauge runner support for configurations and test discovery [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([49e2ae6](https://github.com/unit-mesh/auto-dev/commit/49e2ae698f1dc710fdd4bf12e7a15a3c5ed4ec1f))
* **devins-scala:** add support for Scala run configurations and test context provider [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([b687994](https://github.com/unit-mesh/auto-dev/commit/b6879946b2bd81ffb98d9940d839458d957e61b8))
* **run-service:** add support for specifying a test element when creating run configurations. This enhancement allows for more targeted and efficient execution of tests within the DevIns IDE. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3375f8a](https://github.com/unit-mesh/auto-dev/commit/3375f8ae3298021648f705854ae084a1244beb82))
* **run-service:** introduce new ShellRunService to support running shell scripts. This service simplifies the execution of shell commands within the DevIns IDE, enhancing the user experience. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([44b3859](https://github.com/unit-mesh/auto-dev/commit/44b3859da27a39c13d980c393beae186aed19420))
* **run-service:** refactor createConfiguration method to use PSI file lookup and create RunConfigurationSettings instance. This refactoring improves the readability and maintainability of the RunService class. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([939cfe7](https://github.com/unit-mesh/auto-dev/commit/939cfe77b684453fd0cbde7826d078e6cba9046a))
* **runner:** introduce new RunContext class and refactor RunServiceTask and RunServiceExt to use it. This change simplifies the execution context management and improves code readability. [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3db0623](https://github.com/unit-mesh/auto-dev/commit/3db06232509f173e109d303167a6fe88e64d5e60))
* **scrapy:** add new browser tool and document cleaner ([2b95738](https://github.com/unit-mesh/auto-dev/commit/2b95738bf35d064b73676b4813bf7564499502da))
* **scrapy:** add new browser tool and document cleaner ([abcf8c0](https://github.com/unit-mesh/auto-dev/commit/abcf8c0a6977d7846bca0c75beb5bf33c862d62c))
* **scrapy:** refactor and improve document cleaning logic ([cc9f956](https://github.com/unit-mesh/auto-dev/commit/cc9f956b9ef1015080508f8af6681bcf28045578))
* **scrapy:** refactor and improve document cleaning logic ([041d743](https://github.com/unit-mesh/auto-dev/commit/041d7432bcd1112fddb07d973bf8afc75fd22223))
* **scrapy:** refactor and improve document cleaning logic [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([898f8ed](https://github.com/unit-mesh/auto-dev/commit/898f8ed8fbf51ad06ebc7e2882d522a52365c2d2))
* **scrapy:** refactor and improve document cleaning logic [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([12a0c92](https://github.com/unit-mesh/auto-dev/commit/12a0c92fd9b1a6dbe5cd8b7342dc384a755c1b51))
* should dispose inlay when esc ([b746704](https://github.com/unit-mesh/auto-dev/commit/b74670426f9b0e5b2e10bb8796cba8700ac12a81))
* use custom agent when inlay complete code ([d426ab3](https://github.com/unit-mesh/auto-dev/commit/d426ab3a86e5481ac9826a0ed47e95ed33c432df))



## [1.7.3](https://github.com/unit-mesh/auto-dev/compare/v1.7.2...v1.7.3) (2024-03-22)


### Bug Fixes

* **actions:** fix variable name in CommitMessageSuggestionAction ([edc3e8c](https://github.com/unit-mesh/auto-dev/commit/edc3e8cbeed3506db87e6d3a6de7d8e0b4d100a5))
* **codecomplete:** fix LLMInlayManager imports for 241 version [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([9cdca52](https://github.com/unit-mesh/auto-dev/commit/9cdca524b6c9d2519e4848243c92179ba438dd68))
* **compiler:** fix patch execution race condition [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([ab76784](https://github.com/unit-mesh/auto-dev/commit/ab76784233ace593b9043d15c478f0078c916888))
* **devins-java:** improve symbol resolution logic [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([fd6c134](https://github.com/unit-mesh/auto-dev/commit/fd6c134296c9f1d7051bdfeae6fa9fd274c85faa))
* **devins-lang:** add newline to "Done!" message [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([552d5b0](https://github.com/unit-mesh/auto-dev/commit/552d5b0e91c2f8453f51bb0f35b0fda98d04c754))
* **devins-lang:** correct highlighting for variable, agent, and command identifiers [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f83d305](https://github.com/unit-mesh/auto-dev/commit/f83d305fc5a62c75c3a06387f0f459bee7295824))
* **devins-lang:** fix asynchronous execution issue [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([2fc3b52](https://github.com/unit-mesh/auto-dev/commit/2fc3b5281ded43046b81edba44c98a2e36cde749))
* **devins-lang:** improve error handling and add validation for file-func command ([edbb0c5](https://github.com/unit-mesh/auto-dev/commit/edbb0c570f74f580dd168095140010dbf6a97428)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
* **devins-lang:** improve error message for duplicate agent calls [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9e726bc](https://github.com/unit-mesh/auto-dev/commit/9e726bca6389a233e8784001355d63b6845e5706))
* **devins-lang:** improve file selection message [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([35f950a](https://github.com/unit-mesh/auto-dev/commit/35f950abd95f3028fad0909a6f2c8dccd67dff8d))
* **devins-lang:** improve process handler creation for IDE 222 version ([b21925a](https://github.com/unit-mesh/auto-dev/commit/b21925a4f17aa7578bab68f27c939ed4125c255f))
* **devins-lang:** improve readability of SyntaxHighlighterFactory [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6a20095](https://github.com/unit-mesh/auto-dev/commit/6a20095574de5f67d90bdc65b2008247ccee85f6))
* **devins-lang:** Improve token type string representation and handle whitespace in agent ID regex [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([8e46a8a](https://github.com/unit-mesh/auto-dev/commit/8e46a8a55af8268b6cdc2103479143eca1862e2a))
* **devins-lang:** refactor language injection check [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([0e52bca](https://github.com/unit-mesh/auto-dev/commit/0e52bca6507f1407a86c48e6b7160307394de317))
* **devins-lang:** replace "DevliError" with "DevInsError" for consistency and clarity. ([8fdbba8](https://github.com/unit-mesh/auto-dev/commit/8fdbba8899643751f4d40b7d79a2d5accbc24949))
* **devins-lang:** restrict agent_id to non-whitespace characters [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([388d484](https://github.com/unit-mesh/auto-dev/commit/388d484bede47d5dfcb6a32687c6d06971d8a46e))
* **devins-language:** update ToolHubVariable property names [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([3b12625](https://github.com/unit-mesh/auto-dev/commit/3b12625a3d1e306e8051d5f5ebf9e11553de2b4a))
* **devins-language:** use List instead of Iterable for lookup result [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([26b2581](https://github.com/unit-mesh/auto-dev/commit/26b2581bd45b1b96f81da4cea42e6dc89e56d5b1))
* **devins-linting:** improve detection of duplicate agent IDs in DevInsDuplicateAgentInspection [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([df2bb82](https://github.com/unit-mesh/auto-dev/commit/df2bb82e4af08afd1fe0db9a431cfbf1e923cf2f))
* **editor:** fix TypeOverHandler package name [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([a6a04ce](https://github.com/unit-mesh/auto-dev/commit/a6a04ced029552005eda0495021c9e89ee6011d5))
* fix flow issue ([2458221](https://github.com/unit-mesh/auto-dev/commit/2458221647c6975a9e759f01bd4571de11502ac3))
* fix merge error and typo ([a9e8b06](https://github.com/unit-mesh/auto-dev/commit/a9e8b06033199607bc8fb39e3cf4e7abf62e7b0f))
* **inlay:** fix compatibility issue with IDEA 241 version for [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([2ff3c17](https://github.com/unit-mesh/auto-dev/commit/2ff3c17832c593f22c6f5a06be39045fb940404d))
* **inlay:** update key names and message in LLMInlayManagerImpl.kt ([a5d5e4d](https://github.com/unit-mesh/auto-dev/commit/a5d5e4d039afa5e806813f7215dc73f4602295f5)), closes [#109](https://github.com/unit-mesh/auto-dev/issues/109)
* **java:** improve symbol resolution logic [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ef24e71](https://github.com/unit-mesh/auto-dev/commit/ef24e71350c088515359f944219c2fa98fdaa5b5))
* **java:** simplify package name lookup [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cd8ea46](https://github.com/unit-mesh/auto-dev/commit/cd8ea46f17aad68737bb0d43d209728e6e1c4335))
* pick up presentationUtil [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([216a231](https://github.com/unit-mesh/auto-dev/commit/216a2317af431acb3560a531fe46c32514e5e817))
* **provider:** fix console view initialization [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f402274](https://github.com/unit-mesh/auto-dev/commit/f402274a194201901b9aea3447f1e4e89b1233d7))
* refactor DevInBundle to use non-NLS strings and correct bundle name [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([fdbf0d0](https://github.com/unit-mesh/auto-dev/commit/fdbf0d0a487c847ed0df0854d0d67250fd08b867))
* **runconfig:** remove unnecessary log statements [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([0b976e0](https://github.com/unit-mesh/auto-dev/commit/0b976e0b8638bdaff13a16896bab4c8b9de8508c))
* **service:** fix canonicalName generation in JavaAutoTestService [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9c4da21](https://github.com/unit-mesh/auto-dev/commit/9c4da217aea947247ff56ca061ae65e3f31d4bf9))
* **test:** rename class and modify test case to assert null return for invalid regex pattern. ([b0d0ddf](https://github.com/unit-mesh/auto-dev/commit/b0d0ddf735669e557d6c879aa36ae57efc74a3c3))


### Features

* 222 support inlay code complete [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([26f933a](https://github.com/unit-mesh/auto-dev/commit/26f933a7dbec68f3201dc55f02f5440f40ffe39b))
* change inlay complete code trigger: use shortcut key instead of automatic ([581e56d](https://github.com/unit-mesh/auto-dev/commit/581e56de9e8121f60a0f5f1ac116c6c71faf2321))
* clean markdown chars and remove unused brace“ ([470ec20](https://github.com/unit-mesh/auto-dev/commit/470ec20f8e00a1eeea1122b9033e6b7afcf95ceb))
* **completion:** rename ToolHub to ToolHubVariable and update completion provider ([11cc6df](https://github.com/unit-mesh/auto-dev/commit/11cc6dfae43e6f8594777ae47c76c024b12a7b92))
* **completion:** replace DevInsCompletionProvider with DevInsSymbolProvider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1dc1fa2](https://github.com/unit-mesh/auto-dev/commit/1dc1fa2313ec94cdb046f79d53a04ec96684f34a))
* **devins-compiler:** add support for custom commands [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([fab54c0](https://github.com/unit-mesh/auto-dev/commit/fab54c0f858522bc20c395a7a2fdcc3b20d0221e))
* **devins-compiler:** Use VariableTemplateCompiler for variable compilation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7b79a33](https://github.com/unit-mesh/auto-dev/commit/7b79a33e5e61d96288dc55cb0546c6e3151bd443))
* **devins-documentation:** add support for custom variables in documentation provider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([4911ce2](https://github.com/unit-mesh/auto-dev/commit/4911ce20aeb2bb557ee8291bd21fd42f263619d5))
* **devins-java:** add package name completion [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d00d5a0](https://github.com/unit-mesh/auto-dev/commit/d00d5a0c1bae501b0b6dc80bd5b2dbc26b4916d3))
* **devins-java:** add resolveSymbol method to DevInsSymbolProvider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d20961f](https://github.com/unit-mesh/auto-dev/commit/d20961f579cc4b45a6bd8a7af5a04ad61f3b8e7c))
* **devins-java:** add support for resolving symbols in Java packages [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([da3628a](https://github.com/unit-mesh/auto-dev/commit/da3628a2bd8b7f6e285e9abaa95151f1464d11e1))
* **devins-java:** add support for retrieving module name [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([0cc6b54](https://github.com/unit-mesh/auto-dev/commit/0cc6b544fa3e0f79a2d4c91ac007cf82b0719faa))
* **devins-lang:** add documentation provider and refactor custom agent completion ([4c8a49b](https://github.com/unit-mesh/auto-dev/commit/4c8a49b2de4a2d384378c480ca94c32deb5ecd88)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
* **devins-lang:** add duplicate agent declaration inspection [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d2df012](https://github.com/unit-mesh/auto-dev/commit/d2df0124af9c6c46525316f5508b0c08573fac8e))
* **devins-lang:** add message filtering to console [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([db9b866](https://github.com/unit-mesh/auto-dev/commit/db9b866eba52e2f9bac66094ea4f6b752f9da02e))
* **devins-lang:** add SHELL command and related functionality [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d354989](https://github.com/unit-mesh/auto-dev/commit/d3549893a66b41b41d6084e1323f901b648663e6))
* **devins-lang:** add support for execution environment customization [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([3580aa3](https://github.com/unit-mesh/auto-dev/commit/3580aa31ddb839e1e2066fd23dadc6f1edada486))
* **devins-lang:** add support for file function autocomplete [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1d1552a](https://github.com/unit-mesh/auto-dev/commit/1d1552aa3ae61f74d292a95163192ea4ca4c354d))
* **devins-lang:** add support for highlighting single-line comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([64e5811](https://github.com/unit-mesh/auto-dev/commit/64e58117205c82eaebf37bca60dc9177be056db9))
* **devins-lang:** add support for load custom commands in language completion [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([7a5bb37](https://github.com/unit-mesh/auto-dev/commit/7a5bb371029cf7636474f714466c80c7cf9bd6d0))
* **devins-lang:** add support for single-line comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([b0740cd](https://github.com/unit-mesh/auto-dev/commit/b0740cd992534f9d8ba338c934cf9f3ea974a638))
* **devins-lang:** add support for single-line comments [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([e24f874](https://github.com/unit-mesh/auto-dev/commit/e24f87418d315251aa0db4f4a99041a55f0ff0ff))
* **devins-lang:** add support for system calling with identifiers and colon-separated parameters [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([37b88cf](https://github.com/unit-mesh/auto-dev/commit/37b88cfc878542dd56a88b481c7764ef0b4b70db))
* **devins-lang:** add task creation design [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([53c09f6](https://github.com/unit-mesh/auto-dev/commit/53c09f6eb48df7c1cb82a8dc8ce95a57061c608e))
* **devins-lang:** extract toolhub for variables [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([bb1c989](https://github.com/unit-mesh/auto-dev/commit/bb1c989d9c3451e006c50b4fb6786570b24aa62c))
* **devins-lang:** improve dynamic run configuration creation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f4c33bc](https://github.com/unit-mesh/auto-dev/commit/f4c33bcccc05217c4d336992669b8b46bcb2644f))
* **devins-lang:** refactor completion providers to use new naming convention and improve code readability. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([98e92c9](https://github.com/unit-mesh/auto-dev/commit/98e92c9a8f7cf814914f8a2bcf8be516803dfd81))
* **devins-lang:** refactor extract SymbolInsCommand class and remove old implementation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9219ac8](https://github.com/unit-mesh/auto-dev/commit/9219ac8300effce21cc0186a42592eeb6fc59988))
* **devins-language:** add DevInsRunListener for handling run events [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([e4b4aef](https://github.com/unit-mesh/auto-dev/commit/e4b4aef8ac0d033014eb3be8b1a1a0bd5e256d66))
* **devins-language:** add method to create DevInFile [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([59d5b23](https://github.com/unit-mesh/auto-dev/commit/59d5b236d9a00045b454ccdb285768e7a1f1c8ce))
* **devins-language:** add support for custom agent execution [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([35cfb7b](https://github.com/unit-mesh/auto-dev/commit/35cfb7b011e2682e46943ede5a57f48d91787447))
* **devins-language:** add support for detecting and reporting duplicate agent calls in DevInLanguage [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c03995c](https://github.com/unit-mesh/auto-dev/commit/c03995c8c23df943e48ab87083f9513570e82d53))
* **devins-lang:** update language bundle and related classes to use DevInBundle [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e3b7960](https://github.com/unit-mesh/auto-dev/commit/e3b79600f0fd4dfd1458e09e878e70f00647ccbe))
* **flow:** add support for custom flows [#109](https://github.com/unit-mesh/auto-dev/issues/109) ([4bd0b56](https://github.com/unit-mesh/auto-dev/commit/4bd0b56a0de609c08af9130a0106773442d61036))
* **language:** add support for file function with dynamic file names [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([9dc36e5](https://github.com/unit-mesh/auto-dev/commit/9dc36e5a3950902ed7bca5d702236b551fa19029))
* **language:** add support for tool hub variables [#100](https://github.com/unit-mesh/auto-dev/issues/100) ([2796660](https://github.com/unit-mesh/auto-dev/commit/2796660dcd8dae37db3e9cdf974f3fa5df6ef21f))
* **language:** improve code completion in DevIns language ([1cf4ae3](https://github.com/unit-mesh/auto-dev/commit/1cf4ae3ce2f5c594e6640d0ff3ffc079d878f15f))
* **provider:** add DevInsCompletionProvider and modify references [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6136ed5](https://github.com/unit-mesh/auto-dev/commit/6136ed5480b6fe30125c002015cfdf8372e06376))


### Reverts

* Revert "[liujia]update some idea file to setup project in local" ([e8959a7](https://github.com/unit-mesh/auto-dev/commit/e8959a788740ff6560501d21c077cdade19c4311))
* Revert "refactor: clean inlay model" ([93aa5a8](https://github.com/unit-mesh/auto-dev/commit/93aa5a8ff4dce4ccdb9e1e64a3318871df500f4a))



## [1.7.2](https://github.com/unit-mesh/auto-dev/compare/v1.7.1...v1.7.2) (2024-03-17)


### Bug Fixes

* **compiler:** improve handling of file paths and project roots [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a3d25f6](https://github.com/unit-mesh/auto-dev/commit/a3d25f6bd545ab3ef35772988b7bb0525be71f68))
* **completion:** improve completion provider for DevInTypes.COLON [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7f0c1cb](https://github.com/unit-mesh/auto-dev/commit/7f0c1cb8e4e4928d15aa01b777c7c974ebe69f10))
* **completion:** improve performance by using ReadAction and runBlockingCancellable [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([42449f1](https://github.com/unit-mesh/auto-dev/commit/42449f1927bf82b02074aeb450ff89caab27fc17))
* **completion:** try correct order of completion contributors and add background task for git commit history loading [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([33fea81](https://github.com/unit-mesh/auto-dev/commit/33fea815c08813a1a5c3534ccda7e6074a13b063))
* **custom-schema-provider:** correct class name and resource file reference ([3f2a973](https://github.com/unit-mesh/auto-dev/commit/3f2a973147908922f257de52b3c9d650766765e4))
* **devin-lang:** correct logging and enable action for non-zero offsets [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([b8c0bc2](https://github.com/unit-mesh/auto-dev/commit/b8c0bc21120a18b873b200d6fa7f348260742344))
* **devin-lang:** improve logging and fix compilation errors [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9443239](https://github.com/unit-mesh/auto-dev/commit/9443239fcb3b18bd74d3872502f6a2c0a9e6d04f))
* **devins-compiler:** fix error handling in DevInsCompiler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d38f4d1](https://github.com/unit-mesh/auto-dev/commit/d38f4d133e78054afc5a0cefc73c220ce371cfde))
* **devins-compiler:** fix result checking and code block skipping [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cffde71](https://github.com/unit-mesh/auto-dev/commit/cffde71b0d45be25427adda6fc48196971a6d998))
* **devins-lang:** fix console output formatting [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([97c26e9](https://github.com/unit-mesh/auto-dev/commit/97c26e9db0236a2dd35913f7c07931e27ef13e9c))
* **devins-lang:** fix output equality assertion ([ff4269d](https://github.com/unit-mesh/auto-dev/commit/ff4269dfcfb9cde18cf342b0a36cca22fa8d9d9f))
* **devins-lang:** improve file lookup logic in AutoCommand [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([8c25c78](https://github.com/unit-mesh/auto-dev/commit/8c25c7866eadbf30d74bd37cb2ab2b0caecf1e5b))
* **devins-lang:** modify PatchInsCommand.kt and InsCommand.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ad41f5b](https://github.com/unit-mesh/auto-dev/commit/ad41f5b5314a747c81218cfdaacf2a83342d7e84))
* **devins-lang:** use GenericProgramRunner for DevInProgramRunner [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([91acc81](https://github.com/unit-mesh/auto-dev/commit/91acc81947ca73ab0cb5c87cd8d0acc3383a5138))
* **devins:** fix condition to correctly process commands ([84de6c4](https://github.com/unit-mesh/auto-dev/commit/84de6c4e0b85a804502d610be64d814d23ce51ac))
* **devti-lang:** improve file reference completion provider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ded9c3c](https://github.com/unit-mesh/auto-dev/commit/ded9c3cf0286d1df1435cf0237ad5fb175f0eedc))
* **devti-lang:** improve run configuration handling [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cc8b04e](https://github.com/unit-mesh/auto-dev/commit/cc8b04e3fae3befd1c8770aa7810bd0bfe3d6988))
* **devti:** migrate test data generation action to use AI ([c5fa199](https://github.com/unit-mesh/auto-dev/commit/c5fa19905ec0ce8f8711a47f4f47b4352066bf2c))
* **exts/devin-lang:** Improve parsing and lexing of DevInLang files [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([26f823f](https://github.com/unit-mesh/auto-dev/commit/26f823fc21e991e8f7d072c764cab4fdddc3175f))
* **exts/devin-lang:** improve resource management in RunProfileState ([861b5d8](https://github.com/unit-mesh/auto-dev/commit/861b5d8b1d09819f4c42f1c2d34667d98e2ba89e))
* fix import issue ([9776b57](https://github.com/unit-mesh/auto-dev/commit/9776b576db405c056f0d1e5fb2c9599b7697a049))
* **folding:** correct handling of file references in DevInFileReferenceFoldingBuilder.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ca240d1](https://github.com/unit-mesh/auto-dev/commit/ca240d19d39f3ee5219fabca99d7d5ffd2a39955))
* **folding:** improve file reference folding in DevInFileReferenceFoldingBuilder [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([2a7a330](https://github.com/unit-mesh/auto-dev/commit/2a7a3303a24a0539e32e5585fdef56e999e9d369))
* **git:** fix 222 & 233 version GitUtil class for committing local changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([71c3d69](https://github.com/unit-mesh/auto-dev/commit/71c3d697d23cbbfa9b6488e334169d0e90130856)), closes [#233](https://github.com/unit-mesh/auto-dev/issues/233)
* **language:** update external ID in DevInFile.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7521246](https://github.com/unit-mesh/auto-dev/commit/75212464952be0d9bfec2893f1301732ec2f49d3))
* **runconfig:** update AutoDevConfigurationType to use AutoCRUDConfigurationOptions [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cd98122](https://github.com/unit-mesh/auto-dev/commit/cd981229dcfc8f3354fe2ae343f872807889397c))
* **run:** rename DevInRunFileAction to DevInsRunFileAction [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([85e75fe](https://github.com/unit-mesh/auto-dev/commit/85e75fe5bb8f10a5f2db51040cb54f0b12249aeb))
* **text-block-view:** update text listener registration and text parsing for assistant messages ([011f7ab](https://github.com/unit-mesh/auto-dev/commit/011f7ab8a1cadb4a09dc41fb0ea974ed5bb7f1f4))


### Features

* **chat:** add custom agent response provider [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e599196](https://github.com/unit-mesh/auto-dev/commit/e59919618549eeabffb68f4d8ee2adb76c28b2d5))
* **compiler:** add support for committing changes ([14e9439](https://github.com/unit-mesh/auto-dev/commit/14e943935ad54c15ff73eae478c7cc6e7fe35a63))
* **compiler:** add support for rev auto command [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1ea0bb8](https://github.com/unit-mesh/auto-dev/commit/1ea0bb8bc29839941aa8c47a19af1fa552f98839))
* **completion:** add icons to builtin commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6f0e1e6](https://github.com/unit-mesh/auto-dev/commit/6f0e1e6d96b70bf3a8293e50f5556c26d99bc833))
* **completion:** add support for automatic colon insertion and caret positioning after builtin commands completion. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([5920d9b](https://github.com/unit-mesh/auto-dev/commit/5920d9b850c7c8682341681f67e0a11953a8d9a6))
* **completion:** add support for built-in agent completion [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a09cd0f](https://github.com/unit-mesh/auto-dev/commit/a09cd0f731d68d817f732e94b1d0b18bdf508291))
* **completion:** improve built-in agent support for file and revision references [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([eb60574](https://github.com/unit-mesh/auto-dev/commit/eb605740dfaf089a092e327f1d88fd892f846945))
* **completion:** improve completion provider for DevInTypedHandler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a696be3](https://github.com/unit-mesh/auto-dev/commit/a696be3a3a99330b310957ebdde4ad26cbc1715b))
* **completion:** improve file reference completion by using editor history and project directory. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([78d95fb](https://github.com/unit-mesh/auto-dev/commit/78d95fb98b6c51a8a2db4379b3823af03d7c91a1))
* **completion:** improve file reference completion support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1d2e422](https://github.com/unit-mesh/auto-dev/commit/1d2e4229742653039d0895f74bcff3c084a33e95))
* **completion:** improve file reference provider with project file index [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([32bf931](https://github.com/unit-mesh/auto-dev/commit/32bf931166d1747741a804409ddf9975368e57b6))
* **completion:** refactor completion provider and add support for revision references [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c7eee9a](https://github.com/unit-mesh/auto-dev/commit/c7eee9a059002755c0d9eea2a0baf853ecb641f4))
* **completion:** rename and modify CodeLanguageProvider to support code fence languages ([0ca5616](https://github.com/unit-mesh/auto-dev/commit/0ca56168795f87d8ad8c20d9c8f835b8bfdc0a39))
* **devin-compiler:** add support for builtin commands and agents in DevInCompiler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([81766ff](https://github.com/unit-mesh/auto-dev/commit/81766ff30660467e70a4cad7dd08eb122d128d8a))
* **devin-lang:** add AutoDevRunConfigurationProducer and related classes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6e9efac](https://github.com/unit-mesh/auto-dev/commit/6e9efac7922e44278f370a717daaf8c43c8a4283))
* **devin-lang:** add console output support to DevInRunConfigurationProfileState [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([849582f](https://github.com/unit-mesh/auto-dev/commit/849582f56cef21189784a4b1b3f5fb1ff9acdf64))
* **devin-lang:** add DevInCompilerTest and DevInCompiler classes to support DevInFile compilation and testing. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ed9378b](https://github.com/unit-mesh/auto-dev/commit/ed9378b21086830c6b23135b7f48b813e5730edc))
* **devin-lang:** add DevInRunFileAction and related changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([035a3cb](https://github.com/unit-mesh/auto-dev/commit/035a3cbb893d137e29c5b8fbdcb864b0271a05f2))
* **devin-lang:** add FileAutoCommand and refactor DevInCompiler to support dynamic file content retrieval. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e74019a](https://github.com/unit-mesh/auto-dev/commit/e74019a8aeeb67222c97870330e408bbac5e6b58))
* **devin-lang:** add fullWidth utility function to AutoDevSettingsEditor.kt and remove unused imports from DevInProgramRunner.kt [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([29480d5](https://github.com/unit-mesh/auto-dev/commit/29480d59726a8fceda07484912526d788f12a276))
* **devin-lang:** add highlighting for agent and command identifiers [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([039d053](https://github.com/unit-mesh/auto-dev/commit/039d0535c4a4fefb177e8a4e015dbdf4c66d04df))
* **devin-lang:** add highlighting for property values [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1c58f51](https://github.com/unit-mesh/auto-dev/commit/1c58f513b353ad71c3531ab64da0f70eee4223c2))
* **devin-lang:** add support for agent properties [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([632372d](https://github.com/unit-mesh/auto-dev/commit/632372da1ba126989ee21d762e2c240dd23444da))
* **devin-lang:** add support for DevIn Language in kover and update documentation ([26f1115](https://github.com/unit-mesh/auto-dev/commit/26f1115644824e4efee78a9d652ce164ecae20cf)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
* **devin-lang:** add support for DevInRunConfigurationProfileState and related changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([3a37baa](https://github.com/unit-mesh/auto-dev/commit/3a37baae83f9ba06fa71ea435d04f5784e7fcd20))
* **devin-lang:** add support for file path processing in DevInCompiler [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9909f15](https://github.com/unit-mesh/auto-dev/commit/9909f15e407a559e62b59b2373712b8d22e95400))
* **devin-lang:** add support for script path configuration in run configurations [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([044c6cd](https://github.com/unit-mesh/auto-dev/commit/044c6cdfd937f3e35bb3648acc2ed2893baf5745))
* **devin-lang:** add support for writing content to a file [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6140691](https://github.com/unit-mesh/auto-dev/commit/6140691b44bfcfaf8cfb665a1d3d2328cf145ca0))
* **devin-lang:** extend language identifier regex to support spaces and dots [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9ec0123](https://github.com/unit-mesh/auto-dev/commit/9ec0123de414b1e8956b59de89634a88f2b985a5))
* **devin-lang:** improve run line markers provider ([07a549d](https://github.com/unit-mesh/auto-dev/commit/07a549df15fc9de32b45ad04507726368afe9120))
* **devin-lang:** init design for patch and run commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c6cea24](https://github.com/unit-mesh/auto-dev/commit/c6cea2432a298bcae1d73806f516f31f03f4e42a))
* **devin-lang:** refactor process handling in RunConfigurationProfileState [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([34ca50e](https://github.com/unit-mesh/auto-dev/commit/34ca50e07112042ecef70f45c65796da28262c6b))
* **devin-lang:** update notification group id and add LLM support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a2e96b6](https://github.com/unit-mesh/auto-dev/commit/a2e96b6f9931c1d2fe2f6d11340ce176d2d1343e))
* **devins-compiler:** add support for commit command [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a498580](https://github.com/unit-mesh/auto-dev/commit/a4985808b7437b682eee28d72f60d78802ec68c1))
* **devins-compiler:** add support for writing and auto commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d7232d8](https://github.com/unit-mesh/auto-dev/commit/d7232d863f3291d86896cb12def57bfdb71e6447))
* **devins-lang:** add logging to handle commit loading errors [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ec7e984](https://github.com/unit-mesh/auto-dev/commit/ec7e9845155c2bb5c79d056f63a51951ed016356))
* **devins-lang:** add support for WRITE command [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([a0c228b](https://github.com/unit-mesh/auto-dev/commit/a0c228b166a38334e06237e4ea364fd9e14ad392))
* **devins-lang:** introduce DevIns Lang as the AI Agent language [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e109520](https://github.com/unit-mesh/auto-dev/commit/e10952043652e21a1c087a7ebdfc689d9bfee580))
* **devins-language:** add LineInfo data class and fromString method [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([2798643](https://github.com/unit-mesh/auto-dev/commit/27986430cb0ac24f931377262045cdc9df6f081b)), closes [filepath#L1-L12](https://github.com/filepath/issues/L1-L12)
* **docs:** rename DevIn Input Language to DevIn Agent Language [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([4b504a8](https://github.com/unit-mesh/auto-dev/commit/4b504a86eecfaedf3d1542d637c95bd59cff3d13))
* **exec:** add CommitInsCommand for executing commits [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1870425](https://github.com/unit-mesh/auto-dev/commit/187042531635facab9f27294bcf8f8c70d6a8431))
* **folding:** add file reference folding support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9c1aae4](https://github.com/unit-mesh/auto-dev/commit/9c1aae4430145e7e97c33ab5c29a7cecc7e816bc))
* **git-completion:** add asynchronous loading of git commits [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([b237db7](https://github.com/unit-mesh/auto-dev/commit/b237db70995e4404e87786da720c6ff98d9c75f3))
* **gui:** add support for custom agent file schema validation [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d52cf76](https://github.com/unit-mesh/auto-dev/commit/d52cf7604a29d5a142f21dc79ffda6342b4e337e))
* **language:** add support for flow in DevInRunConfigurationProfileState [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([afc0897](https://github.com/unit-mesh/auto-dev/commit/afc08970979d2b84e94389fc230f97f90d07a604))
* **lexer:** add support for agent value block ([a3b37a1](https://github.com/unit-mesh/auto-dev/commit/a3b37a1b249973fb755048a7f175716d3e576574)), closes [#101](https://github.com/unit-mesh/auto-dev/issues/101)
* **provider:** enable DevIn agent responses [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1efc73c](https://github.com/unit-mesh/auto-dev/commit/1efc73c34f10ce9c65f70c17934726e456dfabc7))
* **run:** add support for DevInRunFileAction and related changes [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ac187cf](https://github.com/unit-mesh/auto-dev/commit/ac187cff8bdd37d76ca5f9deef9cebf63cda33d7))
* **runconfig:** add AutoDevCommandRunner and related configuration types [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1834fed](https://github.com/unit-mesh/auto-dev/commit/1834fedb87e26344c22a3e71c8ef5856d3fbefbf))
* **runconfig:** refactor AutoDevConfigurationFactory and AutoDevConfiguration classes to use inheritance and override methods for better code organization and maintainability. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([84e5033](https://github.com/unit-mesh/auto-dev/commit/84e5033bf06a8267a75781b59f5f27ccccfbe913))
* **schema:** add support for custom prompts schema ([d76bc07](https://github.com/unit-mesh/auto-dev/commit/d76bc07e50b949df6010faa22aacd5b62f7a6e55))
* **testing:** add support for running individual test files [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([68063b1](https://github.com/unit-mesh/auto-dev/commit/68063b106a188f92bde14519cafa9477d1fe7b6f))
* **utils:** add isRepository function to MvcUtil.kt ([9e6343c](https://github.com/unit-mesh/auto-dev/commit/9e6343ceba358289cffd8da0d267cf2ef9a24a11))



## [1.7.1](https://github.com/unit-mesh/auto-dev/compare/v1.7.0...v1.7.1) (2024-03-13)


### Bug Fixes

* **agent chat processor:** add flow collect to improve concurrency ([7bfe9f0](https://github.com/unit-mesh/auto-dev/commit/7bfe9f01e0fa301146cead11e2f2639b5c1fe605))
* append slash to openAI custom host [#98](https://github.com/unit-mesh/auto-dev/issues/98) ([510948e](https://github.com/unit-mesh/auto-dev/commit/510948eabca8172f2f7db4b5323e27555679396e))
* **autodev-core:** disable secondary tool window status ([b28ad10](https://github.com/unit-mesh/auto-dev/commit/b28ad10d4922696547c6ecce62a912e376b0aba1))
* **chat-coding-service:** handle prompt and response with newChatContext as true ([e1b177f](https://github.com/unit-mesh/auto-dev/commit/e1b177f21af88d47fb6507dce59bc457b4b72f88))
* **chat:** ignore empty or newline-only prompts in chat input [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1183caa](https://github.com/unit-mesh/auto-dev/commit/1183caa5187304ce5bcea69a749c7f1a5721a05e))
* **completion:** improve completion contribution for DevInT language [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ae27be4](https://github.com/unit-mesh/auto-dev/commit/ae27be4226ddc70b73a4b2b6974f0c10459b4c77))
* **devin-lang:** add support for variable and command identifiers in grammar and lexer [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([8fc17ec](https://github.com/unit-mesh/auto-dev/commit/8fc17ec2dabc8af6ab13e95da595aec1147012df))
* **devin-lang:** ensure proper handling of language identifiers [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cc45cf2](https://github.com/unit-mesh/auto-dev/commit/cc45cf281a532a87829dc660378e685d87ed2102))
* **devin-lang:** Improve code block parsing and injection logic [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([8a80c12](https://github.com/unit-mesh/auto-dev/commit/8a80c12e509703474e06367ab7fabdc73c5ced0c))
* **devin-lang:** improve code highlighting and completion [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([cf5359e](https://github.com/unit-mesh/auto-dev/commit/cf5359e09b70f0ad2c37254eb9b4f333988018f4))
* **devin-lang:** simplify regex patterns and add support for Java code blocks 101 ([b7e6106](https://github.com/unit-mesh/auto-dev/commit/b7e6106c0af37a344b4e21ebc6ca5d01445a9b9a))
* **devin-language:** update syntax [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1589eb1](https://github.com/unit-mesh/auto-dev/commit/1589eb17652616ac773043c37366c4ad1c07b8ed))
* **devin-lang:** update icon loading and add missing package declaration [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d653226](https://github.com/unit-mesh/auto-dev/commit/d653226af6e218bc7053ac9a1b733836019097f1))
* **devin-lang:** update parser and lexer tokens and rules for improved grammar and tokenization. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([eda58cd](https://github.com/unit-mesh/auto-dev/commit/eda58cdc4668b2065b69cb160b2f4ca7e8135700))
* **devti:** correct language detection in code blocks [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([437d2f1](https://github.com/unit-mesh/auto-dev/commit/437d2f15c540f9329cff01ec23d9a68fff3233ee))
* **ErrorMessageProcessor:** simplify null-check logic and enhance readability. ([418cc29](https://github.com/unit-mesh/auto-dev/commit/418cc299d06d7f4769b8e893b08eb54720342739))
* **exts/devin-lang:** Allow code blocks to start without content [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([2f0b598](https://github.com/unit-mesh/auto-dev/commit/2f0b598602157602426d6ed7676e66779c418287))
* **exts/devin-lang:** Allow code blocks to start without content [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([50cfdc7](https://github.com/unit-mesh/auto-dev/commit/50cfdc78d5f68161caaa6d15e54b078989e9f02a))
* **exts/devin-lang:** enhance CodeBlockElement to correctly handle injection host validations [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([3bcf5a6](https://github.com/unit-mesh/auto-dev/commit/3bcf5a6f200b7af80d284770d3d7e5d629fcae9e))
* **exts/devin-lang:** ensure proper indentation of code blocks in parser grammar [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f3c1cc5](https://github.com/unit-mesh/auto-dev/commit/f3c1cc5ff8d5229a05240248d13562a63abc4ac3))
* **exts/devin-lang:** remove CustomVariable class and refactor VariableProvider to use findLanguage method for language injection [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([648a8c7](https://github.com/unit-mesh/auto-dev/commit/648a8c7c24f41dd73cd735906b16bd185b3a1533))
* fix imports ([5031a54](https://github.com/unit-mesh/auto-dev/commit/5031a54f7c1dc64ac39cd32df9e688b0458c4065))
* fix issue ([45a72cf](https://github.com/unit-mesh/auto-dev/commit/45a72cfacfdf8205b62333c8fdada9230ab5ba3b))
* fix tests [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d65daaf](https://github.com/unit-mesh/auto-dev/commit/d65daaf937030bcbbfedff4c2cae171c530a241c))
* fxi typo ([083b22e](https://github.com/unit-mesh/auto-dev/commit/083b22ea50a1db4a390d840d560ee38398adcdd0))
* **gui:** correct issue submission link and add hover text ([70f24f4](https://github.com/unit-mesh/auto-dev/commit/70f24f414a1a8da1c341b83de2b1892c903769d9))
* **gui:** improve focus handling in AutoDevVariableList [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([f774c6d](https://github.com/unit-mesh/auto-dev/commit/f774c6dfb162082c829fcd059e88403c3611330b))
* **gui:** remove un support call to customize function in ChatCodingPanel.kt ([927561b](https://github.com/unit-mesh/auto-dev/commit/927561b17e9769a1e55dd7e9554af4677ce3930f))
* handle exceptions consistently in JavaVersionProvider.kt ([3d6e91a](https://github.com/unit-mesh/auto-dev/commit/3d6e91a6429b460621ef350c0b2a9726d51016ed))
* **java:** ensure test code insertion with proper annotation and handling of full code blocks ([822324b](https://github.com/unit-mesh/auto-dev/commit/822324b09dc95503c74f6c6543c778187071eeb0))
* **lexer:** handle non-code characters within language identifier [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([fbd0fb9](https://github.com/unit-mesh/auto-dev/commit/fbd0fb9f263e7f8ac95889d5c619c067b1cab399))
* **parser:** remove redundant element type check and unnecessary code for collecting PsiElements [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e7a2fd1](https://github.com/unit-mesh/auto-dev/commit/e7a2fd12fc5517329fabf060022901848547bde7))
* **parsing:** improve regex patterns for DevInParser and DevInLexer to support more complex identifiers and whitespace handling [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7ea39e3](https://github.com/unit-mesh/auto-dev/commit/7ea39e3fd15a66d660d4cca1dcc42c99dfda7dce))
* **plugin:** add dynamic action group for AutoDev Chat ([648de34](https://github.com/unit-mesh/auto-dev/commit/648de34985edfd00681ff23e9eb633f007c440a4))
* **statusbar:** remove unnecessary statusbar service implementation ([cdfc911](https://github.com/unit-mesh/auto-dev/commit/cdfc9112a9637608160ac395fcd1e7ba5851da9a))


### Features

* **auto-test:** introduce auto-test design for prompt-based development ([1bee22b](https://github.com/unit-mesh/auto-dev/commit/1bee22b00153f7888b0e9cb75e3f6997fdec1f8a))
* **autodev-chat:** update group actions and refactor action types ([017810b](https://github.com/unit-mesh/auto-dev/commit/017810b8697a6151ccf8f8ea8078f1d6c2007e1f))
* **autoin-lang:** add basic infrastructure for AutoDev input language support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([02c5a04](https://github.com/unit-mesh/auto-dev/commit/02c5a04a1d3fe3a89ad41dd9343f562f7d97c696))
* **chat-coding-service:** refactor variable template compiler and add support for custom agent state [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0fbdc91](https://github.com/unit-mesh/auto-dev/commit/0fbdc916a5827d33d24e707f62afdadd2f965d00))
* **chat:** improve code readability in ChatWithThisAction.kt by simplifying input setting in contentPanel.setInput(). ([31fe212](https://github.com/unit-mesh/auto-dev/commit/31fe212cfd54a6945d1f6ceaba1c27eeb1d7b001))
* **completion:** add DevInCompletionContributor and remove deprecated TypedHandler from plugin.xml [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c757888](https://github.com/unit-mesh/auto-dev/commit/c7578881bcd6274f3ad8b69766a3af6f4bf90ae0))
* **completion:** add support for code fence language detection and completion [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([7fd13e7](https://github.com/unit-mesh/auto-dev/commit/7fd13e79643d18417e59274a6d74a39a4a55b9a4))
* **devin-lang:** add CodeBlockElement class [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([50f599e](https://github.com/unit-mesh/auto-dev/commit/50f599e27ffc6f6aacc2b384d3400b524547e01d))
* **devin-lang:** add stub support and refactoring to highlighter and parser [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([035f8b7](https://github.com/unit-mesh/auto-dev/commit/035f8b70b9cf731d67f372af523cdc067aafb878))
* **devin-lang:** Add support for DevIn language syntax highlighting and completion. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f7990a6](https://github.com/unit-mesh/auto-dev/commit/f7990a682a3497acdda0930dda7dd6d2f37b510a))
* **devin-lang:** add support for DevInAstFactory and DevInTypedHandler. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([c3a7b94](https://github.com/unit-mesh/auto-dev/commit/c3a7b947b40f8dc32e205ef86b8daa7f0b3df468))
* **devin-lang:** add support for Markdown plugin integration and completion provider implementation. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([0a863f7](https://github.com/unit-mesh/auto-dev/commit/0a863f77d74e0d1e2e48de2099eae9e0315b5091))
* **devin-lang:** add support for variables and commands [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([e36d6f4](https://github.com/unit-mesh/auto-dev/commit/e36d6f4cfacb79bf8c0e392d3448bfad5da30913))
* **devin-lang:** improve code completion for DevInTypes.VARIABLE_ID [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([9f33f89](https://github.com/unit-mesh/auto-dev/commit/9f33f8979b865655fc98d4f5567ef49f5abfc2fa))
* **devin-lang:** improve code fence parsing to support embedded languages [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f07f15f](https://github.com/unit-mesh/auto-dev/commit/f07f15ffa99508c6f54f4d5251c840c7d73e45a4))
* **devin-lang:** improve code input handling [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([beb6f81](https://github.com/unit-mesh/auto-dev/commit/beb6f81a7ccbcb2dbf92b209b41d6891f9fb8a50))
* **devin-lang:** init basic code fence support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d5fa471](https://github.com/unit-mesh/auto-dev/commit/d5fa471303d86b5d62deec763142c401ae1e208e))
* **devin-lang:** introduce support for agent commands and variables [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([03c6d92](https://github.com/unit-mesh/auto-dev/commit/03c6d9267a0432f44d1dc48e0bda6f262fe47c96))
* **devin-lang:** introduce variable identifier and completion support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([6e30a9c](https://github.com/unit-mesh/auto-dev/commit/6e30a9cb698a2370423124aee38ec6c20c4d7904))
* **devin-language:** add support for code language completion and improve completion provider architecture. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([71099a9](https://github.com/unit-mesh/auto-dev/commit/71099a960ec9fe9a86266cfbae97c86bf9a10b7b))
* **devin-lang:** update code_content support [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ca35bca](https://github.com/unit-mesh/auto-dev/commit/ca35bcae91a1473b01bf5b94ef8dc90864f8656f))
* **devin-lang:** update default icon to devin.svg [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([f12af8d](https://github.com/unit-mesh/auto-dev/commit/f12af8d8e78a964a1332e937953799b3059833fe))
* **devin-lang:** update language grammar and lexer to support optional language identifiers in code blocks. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([4b03090](https://github.com/unit-mesh/auto-dev/commit/4b03090aed1a2f3b8c13e817d790a08208e3599a))
* **devin-lang:** update language grammar and lexer to support optional language identifiers in code blocks. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([81ebee4](https://github.com/unit-mesh/auto-dev/commit/81ebee4cc9827d7dbc06cf5def72d4bd40c6e0bd))
* **exts/devin-lang:** add DevInReferenceFoldingBuilder [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([1e4507a](https://github.com/unit-mesh/auto-dev/commit/1e4507a92b28db1402e69fb2f99b9406fb98eead))
* **exts:** rename 'autoin-lang' to 'devin-lang' and update dependencies [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([3a35ac6](https://github.com/unit-mesh/auto-dev/commit/3a35ac61f0ae3e2ea02eb4a0a0432f7c3d477ac1))
* **gui:** add canCloseContents attribute to toolwindow configuration files ([85103d8](https://github.com/unit-mesh/auto-dev/commit/85103d89fcc2aa0910059c407d088df6fde230ac))
* **gui:** add welcome panel with features description and context-aware code generation introduction. ([c5ae948](https://github.com/unit-mesh/auto-dev/commit/c5ae9488be2b94d4327c3f5d6372656a62bccfdf))
* **gui:** improve AutoDevStatusBarWidget functionality ([2d42614](https://github.com/unit-mesh/auto-dev/commit/2d42614b0c5d5fb12d3c71055fc42e1d9b15e973))
* **gui:** optimize chat input section background [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([76a7c47](https://github.com/unit-mesh/auto-dev/commit/76a7c4752959f33b9e7d3e7ebc8516e8f09041f6))
* **java:** refactor code to use runReadAction and replace string operation ([f0e2372](https://github.com/unit-mesh/auto-dev/commit/f0e2372f800d98822a0d40ea1fbad554c2fd578a))
* **language-injector:** add DevInLanguageInjector class to support language injection for code blocks in DevIn language. [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([ae4134a](https://github.com/unit-mesh/auto-dev/commit/ae4134a627c7d41d707a205fd62c3132eec88c96))
* **language-injector:** optimize code block injection logic and add support for whitespace-sensitive language injections  [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([d804142](https://github.com/unit-mesh/auto-dev/commit/d80414281016349838020f2d2da794e11e6a6523))
* **parser:** introduce flex-based lexer and BNF-defined parser for the DevInLanguage [#101](https://github.com/unit-mesh/auto-dev/issues/101) ([b2bfd1b](https://github.com/unit-mesh/auto-dev/commit/b2bfd1b88bb17f492ede63d2abb3e9bc8bfdeac8))
* **recording:** add support for custom model ([d360989](https://github.com/unit-mesh/auto-dev/commit/d360989fa17337dd19bdfb3e7566ad469d26f240))



# [1.7.0](https://github.com/unit-mesh/auto-dev/compare/v1.7.0-snapshot...v1.7.0) (2024-03-07)


### Bug Fixes

* **custom-agent:** fix CustomAgentExecutor requestFormat issue [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([32e7643](https://github.com/unit-mesh/auto-dev/commit/32e764300948d3f227a2891e1a70e864cf382a68))
* **CustomAgentChatProcessor:** handle response actions more robustly and add logging [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([d49b2c0](https://github.com/unit-mesh/auto-dev/commit/d49b2c0e4608b65dabda59c70fd943d6af84c854))
* **error:** rename error template file for clarity ([e661651](https://github.com/unit-mesh/auto-dev/commit/e661651f19316a91f9579c581d466b7814bce715))
* **error:** rename error template file for clarity ([777baf4](https://github.com/unit-mesh/auto-dev/commit/777baf4f8e6ca5855f3a497df5a9af6401c3dd95))
* fix java gen doc return error format issue [#99](https://github.com/unit-mesh/auto-dev/issues/99) ([1bf9fd6](https://github.com/unit-mesh/auto-dev/commit/1bf9fd6418634f174bde262b8bdc42f5e855bc4a))
* fix typos ([c19f879](https://github.com/unit-mesh/auto-dev/commit/c19f87966c0fd4e3b9896466a41ee1094b8ce2f6))
* **gui:** improve focus handling in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([841d833](https://github.com/unit-mesh/auto-dev/commit/841d833f8c3bd5e2b2b30af242fadd36070cf5db))
* **provider:** handle findModuleForFile exceptions ([a0df305](https://github.com/unit-mesh/auto-dev/commit/a0df305e4113e524053ccf76f0f4be178bd7b622))
* **snippet:** fix UpdateThread issu && add logging to AutoDevInsertCodeAction [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([a4e751c](https://github.com/unit-mesh/auto-dev/commit/a4e751c79c53583786942507f8619988475e0322))


* feat!: refactor CustomMessage to align OpenAI format which is rename message -> content ([965b594](https://github.com/unit-mesh/auto-dev/commit/965b5944412bb468fcc59a1f450cf8683eb8ab2f))


### Features

* **agent:** reset default response format string [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([dfd79ed](https://github.com/unit-mesh/auto-dev/commit/dfd79ed134b2100ad1276005d265488836057863))
* **chat:** update custom agent chat and input section [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([451f644](https://github.com/unit-mesh/auto-dev/commit/451f644ff61416e825641a2999f3edb317c97b50))
* **CustomAgentExecutor:** add support for custom format in CustomAgentExecutor [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([583ee94](https://github.com/unit-mesh/auto-dev/commit/583ee94d8ed701d5abbbee90834f435442ada1c2))
* **custom:** update CustomSSEProcessor's Message class content property ([db2e3b5](https://github.com/unit-mesh/auto-dev/commit/db2e3b53df751dadc1c467c8d1e6090a2fce1b25))
* **custom:** update key names in CustomSSEProcessor and add token count support [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4e8f6d3](https://github.com/unit-mesh/auto-dev/commit/4e8f6d31aefcae2784af0538c0b49f5dd00a3edd))
* **docs:** add custom request/response documentation [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([35c7b87](https://github.com/unit-mesh/auto-dev/commit/35c7b8782d467295a472d1322d6871e6c3052ea5))
* **model:** add `ConnectorConfig` to `CustomAgentConfig` and refactor `CustomAgentChatProcessor` to use `logger` [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1754614](https://github.com/unit-mesh/auto-dev/commit/1754614c5c409f7d80af89bee5378af37daa5db9))


### BREAKING CHANGES

* rename `message` -> `content`

- Refactored the `CustomAgentExecutor` class in `CustomAgentExecutor.kt` and the `server.py` file.
- Updated the `requestFormat` property in `CustomAgentExecutor` to an empty string.
- Renamed the `message` property in the `Message` class to `content
* **custom:** modify custom Message role from message field to content field

This commit updates the content property name in the Message class of the CustomSSEProcessor. This change provides more clarity in the class's structure.



# [1.7.0-snapshot](https://github.com/unit-mesh/auto-dev/compare/v1.6.5...v1.7.0-snapshot) (2024-03-06)


### Bug Fixes

* **chat-coding-panel:** improve handling of request intentions [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([9fa1808](https://github.com/unit-mesh/auto-dev/commit/9fa1808256e33d8355494fafa738294a912397b3))
* **chat-coding-service:** handle custom RAG requests [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([46b499e](https://github.com/unit-mesh/auto-dev/commit/46b499eb5dcbfa49dc4fd3261098da66ce8240f1))
* **chat:** handle custom agent state and add support for custom variable compilation [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([93a5e52](https://github.com/unit-mesh/auto-dev/commit/93a5e527df18b03e754ceb3b9b246c3671c2db52))
* **chat:** handle empty ragsJsonConfig in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([df410b1](https://github.com/unit-mesh/auto-dev/commit/df410b1acacbf45db9e7f76d70e68eca48b6bb7b))
* **chat:** hide progress bar after update [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([56a88fc](https://github.com/unit-mesh/auto-dev/commit/56a88fcf3b413b3610097dd635ef9d15fb384dbd))
* **completion:** improve chatbot response handling with JSON parsing enhancements ([561e36a](https://github.com/unit-mesh/auto-dev/commit/561e36ab4dbae1b9dba1a1890cb2da0ad673ef26))
* **CoUnitPromptGenerator:** ensure retrofit service creation is consistent [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([2e0de74](https://github.com/unit-mesh/auto-dev/commit/2e0de7406464f0bcb37bf3dd26bbd967a151a23b))
* **custom-agent:** ensure null safety in agent state resetting [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b6587f2](https://github.com/unit-mesh/auto-dev/commit/b6587f2324b355a91d986fd138f11c837c735060))
* **diff-simplifier:** improve handling of real-world diffs [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4393411](https://github.com/unit-mesh/auto-dev/commit/43934115d772e7a43e738be7c32ef1e5524a57a9))
* **diff:** handle VcsException and log diff before error [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([e94b191](https://github.com/unit-mesh/auto-dev/commit/e94b1915ab8514fbb986065f8cea5be81e6a5149))
* **diff:** handle VcsException and log diff before error [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([6c0eb94](https://github.com/unit-mesh/auto-dev/commit/6c0eb94e964476b45c5c811ff2078d1e75e77979))
* fix import ([4a0a7a4](https://github.com/unit-mesh/auto-dev/commit/4a0a7a4451a66f471d1bb462a39ef843f9109979))
* fix import issue agaian... ([eaae8b9](https://github.com/unit-mesh/auto-dev/commit/eaae8b91158a0737ce81f86ad0c135f4f978f3a0))
* fix typos ([c839c89](https://github.com/unit-mesh/auto-dev/commit/c839c8981c507b3f13bed84b58e9df74fe75c2bf))
* **gui:** ensure correct selection in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1f74f9f](https://github.com/unit-mesh/auto-dev/commit/1f74f9fff2348ff04b770a81da81f4b664b2cd84))
* **gui:** Improve chat input handling [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([f157522](https://github.com/unit-mesh/auto-dev/commit/f15752228244f23290d8fc6e2c90ad36a6f5239e))
* **gui:** improve code block rendering and parsing [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([19a0e97](https://github.com/unit-mesh/auto-dev/commit/19a0e97a1aba87af61bbae27f8ff478440a242b6))
* **gui:** only trigger popup on '$' input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4535dc8](https://github.com/unit-mesh/auto-dev/commit/4535dc81807bc14ca29ea8d745b8bd48fcf0cf9d))
* **gui:** prevent progress bar from resetting after user input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([4dbf66d](https://github.com/unit-mesh/auto-dev/commit/4dbf66dc1e3897da8db48d7cf41d28e338fe7af8))
* **gui:** refactor event dispatcher initialization [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b0bb226](https://github.com/unit-mesh/auto-dev/commit/b0bb226327dddb436e600f4a2221d47ac778bd5d))
* **gui:** remove Dev Portal and Doc options from customRags combobox, set layoutPanel to non-opaque to improve visibility [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0f0d5b9](https://github.com/unit-mesh/auto-dev/commit/0f0d5b95e47a6df83e70e2ed21f507b9e544e49c))
* **gui:** simplify chat coding panel layout [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0b513e4](https://github.com/unit-mesh/auto-dev/commit/0b513e4cf73ad56e8c7b7c5bfbda171bbe9d3b09))
* **jcef:** use official JCEF builder and handle exceptions [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([d116151](https://github.com/unit-mesh/auto-dev/commit/d1161516584801f0b143a30c3ad214ef5d16ceb5))
* **LivingDocPromptBuilder:** handle exceptions when getting context for PsiElement ([d3367fb](https://github.com/unit-mesh/auto-dev/commit/d3367fb4a40bb8f81a6427296833848ef0cc947e))
* **response-handling:** handle empty SSE lines and "ping" events [#97](https://github.com/unit-mesh/auto-dev/issues/97) ([c654495](https://github.com/unit-mesh/auto-dev/commit/c654495d3398145d5fdc59d373e3b8ce7c58df56))
* **sse-starlette:** handle SSE events with data prefixed with ":ping" and fixed[#97](https://github.com/unit-mesh/auto-dev/issues/97) ([e448c28](https://github.com/unit-mesh/auto-dev/commit/e448c283c2bad2c8310cb25786fcf73af98c0136))
* **sse:** handle empty lines and comments in SSE event stream [#97](https://github.com/unit-mesh/auto-dev/issues/97) ([d307861](https://github.com/unit-mesh/auto-dev/commit/d307861c149774a69b0df29264a09e9e7c6c8f51))
* **tasks:** add onFinished() methods to notify application status ([632be81](https://github.com/unit-mesh/auto-dev/commit/632be815fd6e5467851e4afb22a9f3a60452b225))


### Features

* **chat:** add removeLastMessage function to clear chat history [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([247a8ad](https://github.com/unit-mesh/auto-dev/commit/247a8ad2323df3f26017a9c7376787cd2ed45ae2))
* **counit:** rename and refactor to support custom agent functionality [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0375d22](https://github.com/unit-mesh/auto-dev/commit/0375d229de476119e8e5a7242e1ac503f1c72c59))
* **custom_agent:** add state management for custom agent flow [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1a59765](https://github.com/unit-mesh/auto-dev/commit/1a597658c29271d1ed89db0166c2b952dd59c6a2))
* **custom-actions:** improve logging and error handling in CustomActionBaseIntention and CustomAgentChatProcessor. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b900633](https://github.com/unit-mesh/auto-dev/commit/b900633b60672e55d0c4315243e66365c3fb496f))
* **custom-action:** use i18n for family name ([f9695b7](https://github.com/unit-mesh/auto-dev/commit/f9695b7a4836c26776bf691aec5e9f3874ca5830))
* **custom-agent:** add basic support for custom agent execution [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([62e30a1](https://github.com/unit-mesh/auto-dev/commit/62e30a1bf457ee5b5ec89831502cc6854b8f2007))
* **custom-agent:** add custom agent support [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([e089d02](https://github.com/unit-mesh/auto-dev/commit/e089d02f90640cb67861daa0b07aa39b2cbd6f6e))
* **custom-agent:** add support for authentication types in custom agent execution [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([59f54c9](https://github.com/unit-mesh/auto-dev/commit/59f54c9e0402f6874ace09dff460a3d59c8712e5))
* **custom-agent:** add support for custom request format and improve serialization #%1 ([4bd6840](https://github.com/unit-mesh/auto-dev/commit/4bd68404aaf79a4b2dca0e79689a90c783b3a169))
* **custom-agent:** add support for custom web views in chat responses [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([6cd1e2c](https://github.com/unit-mesh/auto-dev/commit/6cd1e2c65f46e5bb20546eccd2e03f1d1aad85eb))
* **custom-agent:** add support for custom webview in chat interface [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([dcae35c](https://github.com/unit-mesh/auto-dev/commit/dcae35ca798e8ac85d750d57f6730b40f9a88250))
* **custom-agent:** add support for OpenAI ChatGPT integration [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ddaa4e2](https://github.com/unit-mesh/auto-dev/commit/ddaa4e271e97c96875d318eba30bdc8a7a5f65b7))
* **custom-agent:** add WebBlockView class and import Component for WebViewWindow class [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([b4fd22b](https://github.com/unit-mesh/auto-dev/commit/b4fd22bd35124e463985ca5221ea7f9751905cba))
* **custom-agent:** refactor and add stream response action [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ef80c30](https://github.com/unit-mesh/auto-dev/commit/ef80c30c65a5bbbc957ebb7ec563b4e1fb740f10))
* **custom-agent:** refactor and add support for custom agent response actions [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([0bfac9e](https://github.com/unit-mesh/auto-dev/commit/0bfac9e633e3052d7b335ff34023602d3690d627))
* **custom-agent:** refactor to use LlmProvider for chat processing [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([88b4633](https://github.com/unit-mesh/auto-dev/commit/88b4633d3293c50c42834e9f56e112dd96249faa))
* **custom-agent:** use agent-specific url for requests [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([65e9e5e](https://github.com/unit-mesh/auto-dev/commit/65e9e5e2d05eac41c7668fc9cf63995dfc73097d))
* **custom-arg:** add support for custom RAG settings and refactor related components and configurations [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([bca1bba](https://github.com/unit-mesh/auto-dev/commit/bca1bba0fb836e517472b82f1f3f2e9276f56e1f))
* **custom-variable:** improve variable list component and add custom variable support [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([bf2440d](https://github.com/unit-mesh/auto-dev/commit/bf2440dc4922044645241e4546aa6fd487eaf149))
* **custom:** add support for custom agent configuration and UI improvements [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([05ecb7a](https://github.com/unit-mesh/auto-dev/commit/05ecb7a97c3a2a1540e7f870f0a6bcb7e2ddee9d))
* **gui:** add AutoDevVariableListComponent and improve popup behavior in AutoDevInputSection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([8b54522](https://github.com/unit-mesh/auto-dev/commit/8b54522cfb60db24aadee0527814c081cdc79663))
* **gui:** Add default rag selection and refactor custom rag loading logic [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([cf7bb77](https://github.com/unit-mesh/auto-dev/commit/cf7bb77dfc6da653548b7137a88deeb9f275b771))
* **gui:** add key listener to AutoDevInputSection for better user experience [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ae6665e](https://github.com/unit-mesh/auto-dev/commit/ae6665e22d15fa45382ac5288cb1def8f3e49401))
* **gui:** add resetAgent() method to clear custom agent selection in chat coding panel and input section. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([768e4c0](https://github.com/unit-mesh/auto-dev/commit/768e4c0aab4dabf2ec22720161f4f517444df12f))
* **gui:** add resetAgent() method to clear custom agent selection in chat coding panel and input section. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([16240e0](https://github.com/unit-mesh/auto-dev/commit/16240e008fab623c2ccc16acef404af997e35ee2))
* **gui:** add support for auto-completion popup in chat input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([741dda8](https://github.com/unit-mesh/auto-dev/commit/741dda8e7d2b6163e77eb3e3a9ada1bdf56b7156))
* **gui:** add support for custom rag apps selection [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([27d1269](https://github.com/unit-mesh/auto-dev/commit/27d12698c5dd1d0bacd9d596477c37aed43ebc71))
* **gui:** add support for custom variables in chat input [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([ccf3967](https://github.com/unit-mesh/auto-dev/commit/ccf39677a00778580b1c0c05d999c67c6e23f0c6))
* **gui:** refactor AutoDevVariableListComponent to use JBList and add support for variable selection popup [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([e5993a4](https://github.com/unit-mesh/auto-dev/commit/e5993a40bc41a57019610446c1807d055f5e9cd8))
* **model:** introduce CustomRagApp and ResponseAction enum, refactor CodePayload to use text instead of payload_type, update AutoDevInputSection to use send icon, add examples to Tooling class as QAExample objects. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([6a7c26a](https://github.com/unit-mesh/auto-dev/commit/6a7c26a939516949d1be9406d77ac9a9eeb07913))
* **server:** add support for HTMLResponse in mock_frontend [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([1cfa154](https://github.com/unit-mesh/auto-dev/commit/1cfa154ffd04a1734404be1004d59a6cc9562bdc))
* **view:** add WebViewWindow class to handle browser events and implement JavaScript communication. [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([00d9169](https://github.com/unit-mesh/auto-dev/commit/00d9169f9de1be098aacb5b7d1a3b716a53a8092))
* **view:** improve web view window background color to JBColor.WHITE [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([467ebbf](https://github.com/unit-mesh/auto-dev/commit/467ebbf7d82c5ffd6cf7516bb7288444a4c13a11))
* **webview:** add support for custom scheme handler and load methods [#51](https://github.com/unit-mesh/auto-dev/issues/51) ([d49734a](https://github.com/unit-mesh/auto-dev/commit/d49734aaa5954394be706e386cafb8dc68a48d7f))


### Reverts

* Revert "refactor(custom-agent): improve response action handling and add removeLastMessage function #51" ([214eb79](https://github.com/unit-mesh/auto-dev/commit/214eb7995c64d3c7c3a44812a10972aff2939599)), closes [#51](https://github.com/unit-mesh/auto-dev/issues/51)



## [1.6.5](https://github.com/unit-mesh/auto-dev/compare/v1.6.4...v1.6.5) (2024-03-03)


### Bug Fixes

* **241:** update action threads for UI actions ([a97f102](https://github.com/unit-mesh/auto-dev/commit/a97f10220275a89ddedb4a65eb7ff688c758ad44))
* **docs:** correct custom request format documentation ([c8ad270](https://github.com/unit-mesh/auto-dev/commit/c8ad270c3ec77a335befa72d825617dc97774366))
* **intentions:** update description for AutoDevIntention and AutoSqlAction ([1a28168](https://github.com/unit-mesh/auto-dev/commit/1a2816879152aeb3c215f5687145699bfc2d4c2d))
* **prepush:** update background thread for action update since of @Deprecated API changed ([4b2e390](https://github.com/unit-mesh/auto-dev/commit/4b2e39024b73c870fb162947f266a8917985ac56))
* **provider:** remove unused logger in LivingDocumentation and HarmonyOsLivingDocumentation ([025cb95](https://github.com/unit-mesh/auto-dev/commit/025cb953db258b32043fe8e402ff344f2cd3997e))
* **SSE response handling:** Skip logging of SSE responses ([8e6affb](https://github.com/unit-mesh/auto-dev/commit/8e6affb963ba25a3da6849cda74f8358016ca57b))


### Features

* **custom-llm:** Add response format validation and success request tracking ([ffb07be](https://github.com/unit-mesh/auto-dev/commit/ffb07bef5d4a48426478e4a3e3ee7752462b75bc))
* **docs:** enhance Moonshot AI custom integration in docs ([fde46b8](https://github.com/unit-mesh/auto-dev/commit/fde46b856374d790f93df568a04bdeec782b5ac7))
* **docs:** update AI configurations and usage guide ([c42eb6d](https://github.com/unit-mesh/auto-dev/commit/c42eb6d9ee028ef8f2b61d4158952f70386ae655))
* **gui:** add auto dev insert to code action ([f56d7de](https://github.com/unit-mesh/auto-dev/commit/f56d7deb50c95119c21ef10557f93bea7d086e07))
* **harmonyos:** improve LinearLayout and RelativeContainer layouts ([9f9b228](https://github.com/unit-mesh/auto-dev/commit/9f9b2287a89480974d19737c4477d84e41e5803b))
* **harmonyos:** modify List component to include new features ([4e40af6](https://github.com/unit-mesh/auto-dev/commit/4e40af648b24576c5ac7514971ef67cd32223104))
* **icon:** add support for auto dev insert file action ([66c2e96](https://github.com/unit-mesh/auto-dev/commit/66c2e964b7b9a948c0f73cc281c1854237419ae1))
* Improve code insertion in AutoDevInsertCodeAction ([a1a63bb](https://github.com/unit-mesh/auto-dev/commit/a1a63bbd7a4f901e663bfd2233570abfa203826d))
* init check for openai hosts ([efeb143](https://github.com/unit-mesh/auto-dev/commit/efeb1432208de1c1802f2a73c5a0365e521b0724))



## [1.6.4](https://github.com/unit-mesh/auto-dev/compare/v1.6.3...v1.6.4) (2024-02-25)


### Bug Fixes

* **harmonyos:** convert ArkUiExample to data class ([0c71a69](https://github.com/unit-mesh/auto-dev/commit/0c71a6948c72cc2fc5585d4b38dd05916651ff48))
* **harmonyos:** update ext-harmonyos README ([37f323d](https://github.com/unit-mesh/auto-dev/commit/37f323d92ca4004ee90cf76fd6053698e730bd3c))


### Features

* **actions:** add new component types ([232dcac](https://github.com/unit-mesh/auto-dev/commit/232dcacbeedbfb66f193604eaea39cf055244bec))
* **arkui:** add support for ArkUi special features, components, and layouts ([998f5dc](https://github.com/unit-mesh/auto-dev/commit/998f5dc25afdc5f8ddae911d37f62e86cfb5ac51))
* **ext-harmonyos:** add android plugin dependency and component class ([62042f1](https://github.com/unit-mesh/auto-dev/commit/62042f13ed51437ac0af420bfb2877ed4580f4a5))
* **extension:** add HarmonyOS extension and API reference ([09494cf](https://github.com/unit-mesh/auto-dev/commit/09494cf7980c6ae466eba7cc36e08f5acf351cd3))
* **harmonyos:** add AutoArkUiFlow class and ArkUiContext data class ([e33509a](https://github.com/unit-mesh/auto-dev/commit/e33509a104b776164feb2636911f02c53da3ee9e))
* **harmonyos:** add JS and CPP PSI modules ([da4852e](https://github.com/unit-mesh/auto-dev/commit/da4852e7ef80b657f8bf1712b487d80909e05eea))
* **harmonyos:** add margin to Button in ArkUiComponentType ([e6f4734](https://github.com/unit-mesh/auto-dev/commit/e6f4734a4889138656dedd3eb37e38fd9a974197))
* **harmonyos:** add support for Android Studio platform ([f611959](https://github.com/unit-mesh/auto-dev/commit/f6119596f0e441803ae507d654e5ed12084427ee))
* **harmonyos:** add support for ArkUi migration ([bf6579e](https://github.com/unit-mesh/auto-dev/commit/bf6579e65b9e9e8f6fd2ae4270cf35221b1af0d2))
* **harmonyos:** add support for ArkUI migration expert ([e670925](https://github.com/unit-mesh/auto-dev/commit/e670925a4d023c53845ae6a75760e4f46456f9a3))
* **harmonyos:** add support for parsing select text ([d5c89cc](https://github.com/unit-mesh/auto-dev/commit/d5c89cca901bd327d2bcb189c88d9414844fb36d))
* **harmonyos:** add support for sending UI to chat panel ([e175992](https://github.com/unit-mesh/auto-dev/commit/e175992779ce93974c223568c9daa0b30f340639))
* **harmonyos:** add supported layout types ([09f6c06](https://github.com/unit-mesh/auto-dev/commit/09f6c06a98ec80e1d50fc65857b3051b9b0f8731))
* **harmonyos:** improve ArkUiLayoutType and ArkUi documentation ([9147ce1](https://github.com/unit-mesh/auto-dev/commit/9147ce1aaefe9269d857a0c1fae2ef7efe621f8d))
* **harmonyos:** improve HarmonyOSChatContextProvider ([2f754ef](https://github.com/unit-mesh/auto-dev/commit/2f754efacf9c1ae674200644886221d55177f426))
* **harmonyos:** update ArkUiFlow design method and add ArkUiLayoutType and ArkUiComponentType ([4ad516d](https://github.com/unit-mesh/auto-dev/commit/4ad516d1fe7e05bd87886e6cab4be8387b01fabb))
* **harmonyos:** update AutoArkUiFlow and related classes ([a6ef010](https://github.com/unit-mesh/auto-dev/commit/a6ef010c95460a10e05882890a0bccb27e8cfd59))
* **harmonyos:** update documentation and chat context provider ([91317d1](https://github.com/unit-mesh/auto-dev/commit/91317d1a47a136d1647cd08e4f065d16098ed032))
* **javascript:** add TypeScript living documentation ([8a0ad02](https://github.com/unit-mesh/auto-dev/commit/8a0ad022d25fc73eb8ad87344be23d26378b919b))
* **living-doc:** add HarmonyOS living documentation support ([d4612d6](https://github.com/unit-mesh/auto-dev/commit/d4612d67445cfbbd17d3419e2d4e5c2615f5463b))
* **plugin:** add support for HarmonyOS ([56f7a99](https://github.com/unit-mesh/auto-dev/commit/56f7a9943ace6ac3ee0cd7145aee87b7d61ec99c))
* **provider:** add HarmonyOS chat context provider ([1eeeb23](https://github.com/unit-mesh/auto-dev/commit/1eeeb235f144cc2b52be4fd79865808a708f68bd))
* **provider:** add logging to HarmonyOsLivingDocumentation ([ebc575f](https://github.com/unit-mesh/auto-dev/commit/ebc575f76b0f8c8c189ea0848979ce1371cd14d4))



## [1.6.3](https://github.com/unit-mesh/auto-dev/compare/v1.6.1...v1.6.3) (2024-02-22)


### Bug Fixes

* **android:** remove space before Android SDK target version ([5f32088](https://github.com/unit-mesh/auto-dev/commit/5f320884626a3fd3b6384a39f9501c29913ae6a8))
* **go:** improve documentation generation process ([90f446f](https://github.com/unit-mesh/auto-dev/commit/90f446ff5fe7d3796808e5d89d60977c34284437))
* **goland:** fix 222 version lost interface issue ([e6def78](https://github.com/unit-mesh/auto-dev/commit/e6def7842c08f02dba2e6b0469987cd0cf845251))


### Features

* **build:** add Gradle IntelliJ Plugin and update version ([45c714a](https://github.com/unit-mesh/auto-dev/commit/45c714acb47e4aa5f180b1e514946744442f4119))
* **chat:** refactor chat action names ([fb307d9](https://github.com/unit-mesh/auto-dev/commit/fb307d912e30debeadee2776151d098c7fc65da5))
* **completion:** add support for text replacement in code completion ([f552e0d](https://github.com/unit-mesh/auto-dev/commit/f552e0d7e14a31e4e83134f60c35dedad8d72ff7))
* **docs:** add compatible strategy documentation ([23581b6](https://github.com/unit-mesh/auto-dev/commit/23581b657b2cc32a3dbd303f6f99431027b6fdfe))
* **docs:** add demo project link to customization guide ([cea4574](https://github.com/unit-mesh/auto-dev/commit/cea4574874efe5ab17f00d734279e7d35cde6257))
* **docs:** add unit-driven design pattern documentation ([3b09c90](https://github.com/unit-mesh/auto-dev/commit/3b09c90601576d0cf2bff2426c24c1798b1bd14f))
* **interaction:** add support for ReplaceSelection prompt type ([e1da93e](https://github.com/unit-mesh/auto-dev/commit/e1da93eb2a91cd5c858def1d72d9ac50ab451aa2))
* **living-docs:** add parameter and return tag instructions ([603a4c2](https://github.com/unit-mesh/auto-dev/commit/603a4c2dd687338f947ffecf19d231f2f9ac0474))
* **provider:** add Android extension support ([2860c85](https://github.com/unit-mesh/auto-dev/commit/2860c85f1451d88d4cbfe56189b0c8693b5f33f9))
* **tests:** add @Ignore annotation to end-point prompt test ([c99ed29](https://github.com/unit-mesh/auto-dev/commit/c99ed29f8080095b248ea7bb65b867cc96c84fc6))



## [1.6.1](https://github.com/unit-mesh/auto-dev/compare/v1.6.0...v1.6.1) (2024-02-18)


### Bug Fixes

* fix error ([7f82ed6](https://github.com/unit-mesh/auto-dev/commit/7f82ed669934f93e6ec7b5a2cfa7d8323bb5aedc))
* fix error ([903f813](https://github.com/unit-mesh/auto-dev/commit/903f813cc51db673e160b2fb76c475e5aaf60475))
* fix naming typo ([1fa11cf](https://github.com/unit-mesh/auto-dev/commit/1fa11cf2d8a5ad098955790eff1661955a28fd15))
* **gui:** modify default prompt for FIX_ISSUE action type ([bffe3a5](https://github.com/unit-mesh/auto-dev/commit/bffe3a5b8a27078ed25679580585955121c9cdff))
* **javascript:** fix file type conversion in JavaScriptTestCodeModifier ([3a1153b](https://github.com/unit-mesh/auto-dev/commit/3a1153ba524bddfc12af73db88d1ce0488fe3fbe))
* **provider:** fix package name retrieval in KotlinWriteTestService ([e4b228f](https://github.com/unit-mesh/auto-dev/commit/e4b228f635f657b556694a38e2eadc7253adc5d1))
* **util:** LLMCoroutineScope use SupervisorJob to avoid coroutines affected each other. ([0e1edf1](https://github.com/unit-mesh/auto-dev/commit/0e1edf155353a1aad73c92e26939d0d49308c333))
* **util:** LLMCoroutineScope use SupervisorJob to avoid coroutines affected each other. print error to intellij platform logger ([8b8f50c](https://github.com/unit-mesh/auto-dev/commit/8b8f50c31e2f78121c4c8d5423b69e808d7efb59))


### Features

* **android:** add Android SDK target version to chat context provider ([fadac55](https://github.com/unit-mesh/auto-dev/commit/fadac55920e938c5c844800bd4a254eb0e877fa7))
* **android:** init method to get project's Android target SDK version ([ee84b6a](https://github.com/unit-mesh/auto-dev/commit/ee84b6ab442a3ea6b9260e21e48021746ce04f44))
* **cpp:** add getProjectName function to CMakefileUtil ([04690e6](https://github.com/unit-mesh/auto-dev/commit/04690e63e6f759bdae4ef6f4d3d3450c6f37843a))
* **database:** add SqlChatContextProvider and SqlContextBuilder ([7a73e5c](https://github.com/unit-mesh/auto-dev/commit/7a73e5c50d1f58b883dbfc0946267026649e1fa4))
* **docs:** add demo video and FAQ page ([f028cb9](https://github.com/unit-mesh/auto-dev/commit/f028cb985f9f0e51259a445b49e8f540845fe85a))
* **docs:** update legacy migration scene title and content ([f4d05c9](https://github.com/unit-mesh/auto-dev/commit/f4d05c9279a49ffc88456fdd2c65eca5ccc402f5))
* **error:** add support for error prompt template rendering ([7206530](https://github.com/unit-mesh/auto-dev/commit/7206530089a6ca350b17a15fb4684de3d48232d0))
* **go:** add GoLivingDocumentationProvider implementation ([9d535de](https://github.com/unit-mesh/auto-dev/commit/9d535dead6cc96f0659bc90fce3d0b6e7317af41))
* **go:** add support for living documentation ([6a80bc8](https://github.com/unit-mesh/auto-dev/commit/6a80bc8d619005c86247b6bb007941e5f39d1aba))
* **gradle:** try config for android plugin version ([3df6644](https://github.com/unit-mesh/auto-dev/commit/3df6644aa0369f63a7b5d68d3d89f13e2963c2c8))
* **gradle:** update platformVersion to 233, add AdSdkFinder ([ea7bb9b](https://github.com/unit-mesh/auto-dev/commit/ea7bb9bb2b84c936c67fe4a3a268f69a0a69299c))
* **kotlin:** add KotlinPsiUtil for Kotlin context collection ([41a9f2d](https://github.com/unit-mesh/auto-dev/commit/41a9f2def9fe984bba8a074da5ee6ab2dc456ddb))
* **kotlin:** add support for handling class types in KotlinTestDataBuilder ([7ac35a5](https://github.com/unit-mesh/auto-dev/commit/7ac35a5530ce3189527f203375f155b7887c9823))
* **plugin:** add basic support for Android extension ([262a0fd](https://github.com/unit-mesh/auto-dev/commit/262a0fd7e390704404f4db04797ddc67d05fc791))
* **plugins:** add support for Android plugin ([51dec2e](https://github.com/unit-mesh/auto-dev/commit/51dec2e9aa2021328548902d6a0980724045fea4))
* **util:** add library check in ProjectFileUtil ([e220aa2](https://github.com/unit-mesh/auto-dev/commit/e220aa2b1d1fdc5f6d7a80bbf0524f2f09932cb0))



# [1.6.0](https://github.com/unit-mesh/auto-dev/compare/v1.5.5...v1.6.0) (2024-01-26)


### Bug Fixes

* **actions:** remove unnecessary null check ([51dc43e](https://github.com/unit-mesh/auto-dev/commit/51dc43e27563b31dc7eba607783bff45b8842d32))
* **docs:** fix workflow steps in auto-page.md ([883528c](https://github.com/unit-mesh/auto-dev/commit/883528c01cb69856f4d43949120d3af56c2483df))
* fix typos ([927939f](https://github.com/unit-mesh/auto-dev/commit/927939f446f99a5acce8fa887f82880aebc5f708))
* **genius:** update SQL generation templates ([20735ee](https://github.com/unit-mesh/auto-dev/commit/20735eef361ee7b7a5e23b4b5f2917a274e1086d))
* **javascript:** add logger statements for null values [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([57527d5](https://github.com/unit-mesh/auto-dev/commit/57527d51ec70754a89290157a036c9bda8326bbe))
* **team:** fix nullability issue in TeamPromptsBuilder ([f36e74d](https://github.com/unit-mesh/auto-dev/commit/f36e74deab9f05efa751959e1103da80c198bd57))
* **ui:** add lost support for idea 222 vertical alignment in grid layout ([dfa73cc](https://github.com/unit-mesh/auto-dev/commit/dfa73cc8073cce0f44e36f94c5a9ad26617be37b))


### Features

* **autodev-pair:** init AutoDev Pair documentation and tool window ([55a103e](https://github.com/unit-mesh/auto-dev/commit/55a103eb6c6af7d462a9c6a8c974e8c9f3e10c51))
* **commit-msg:** update commit message guidelines ([9e1cc3b](https://github.com/unit-mesh/auto-dev/commit/9e1cc3b3e1b31517cea1d04243148e38475529b1))
* **database:** add check for empty table names [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([ec3bf1d](https://github.com/unit-mesh/auto-dev/commit/ec3bf1d5929db58e511f390d5a40fa2a0002d196))
* **database:** add code parsing for SQL script [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([d15ce43](https://github.com/unit-mesh/auto-dev/commit/d15ce43d0571f788715226be4892d1cb32ad496e))
* **database:** add DbContext and DbContextProvider classes [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([6017221](https://github.com/unit-mesh/auto-dev/commit/601722160b86b485cc3bedb4330743241bb925f2))
* **database:** add DbContextActionProvider class [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([737a47a](https://github.com/unit-mesh/auto-dev/commit/737a47a94096f6429db2b26849a8e6f7751e4959))
* **database:** add GenerateEntityAction for PL/SQL to Java for design new workflow ([9f030fa](https://github.com/unit-mesh/auto-dev/commit/9f030fa60c2db6b7707cca258089dd89465d4fb7))
* **database:** add GenerateFunctionAction for testing PL/SQL ([74952dd](https://github.com/unit-mesh/auto-dev/commit/74952ddd26f9bf2b96b0e8d440fd32c09c55a94d))
* **database:** add GenerateUnittestAction ([7329ac6](https://github.com/unit-mesh/auto-dev/commit/7329ac65fc7e1b75bc5956822f7ce46989a9a49a))
* **database:** add GenSqlScriptBySelection action ([f597094](https://github.com/unit-mesh/auto-dev/commit/f5970945e9abe2b991bde1417c3db0c587b42fb5))
* **database:** add PL/SQL migration actions [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([4d849e3](https://github.com/unit-mesh/auto-dev/commit/4d849e37fefa350d5787aa9c5dac8fe161d95684))
* **database:** add prompter for generating SQL script [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([26227ba](https://github.com/unit-mesh/auto-dev/commit/26227babd5cd3e2c9419a06a6d1f21f6146cf68e))
* **database:** add SQL generation functionality [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([0d2e6ce](https://github.com/unit-mesh/auto-dev/commit/0d2e6ce9a987038e267c14859bf29166f3bf51c0))
* **database:** add SQL generation templates [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([62d3e13](https://github.com/unit-mesh/auto-dev/commit/62d3e13fe4769d110c7ac1dcea97c1bdf6c667d4))
* **database:** improve getTableColumns method [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([db58264](https://github.com/unit-mesh/auto-dev/commit/db58264073e24efd0a6515901bfd3fc96daf12c2))
* **database:** init ModularDesignAction and modify VisualSqlAction for design [#80](https://github.com/unit-mesh/auto-dev/issues/80) ([2a11e29](https://github.com/unit-mesh/auto-dev/commit/2a11e2931692df1c8198b74ffd239a8fe2ab5973))
* **docs:** add usecases and workflow documentation [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([9462c15](https://github.com/unit-mesh/auto-dev/commit/9462c1525fa20f22c94cfd9a2d5e33127b556599))
* **flow:** add context parameter to execute method [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([2a88bde](https://github.com/unit-mesh/auto-dev/commit/2a88bde062bae48bb756dcc2003263ca677e168a))
* **flow:** add documentation and comments to TaskFlow interface [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([ecf891e](https://github.com/unit-mesh/auto-dev/commit/ecf891e97660786e092b4a01964f6bd9ae705b67))
* **flow:** add TaskFlow interface and implement in GenSqlFlow [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([dc8abdb](https://github.com/unit-mesh/auto-dev/commit/dc8abdb5e5987afe5416dc96d1602c402431ea10))
* **gui:** add AutoDevPairToolWindow class ([936cd3d](https://github.com/unit-mesh/auto-dev/commit/936cd3d488943587f0b93b510241739a9da81301))
* **javascript:** add AutoPageFlow and AutoPageTask [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([d687152](https://github.com/unit-mesh/auto-dev/commit/d687152063899387be7fc82e90de590b6a1db0d6))
* **javascript:** add FrontendFlow and DsComponent interfaces [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([b50b5df](https://github.com/unit-mesh/auto-dev/commit/b50b5dfeb4573581b3d63fa0e0ee4324c5017932))
* **javascript:** add function signature and props to DsComponent [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([8ab5cd4](https://github.com/unit-mesh/auto-dev/commit/8ab5cd420092ebe1e604795ac2e2f5ecc0c1ea5e))
* **javascript:** add GenComponentAction and GenComponentFlow [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([ddb96de](https://github.com/unit-mesh/auto-dev/commit/ddb96de01e1c8d1c7a114be0cdab3d66325a08eb))
* **javascript:** add language and frameworks to AutoPageContext.build [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([123da5b](https://github.com/unit-mesh/auto-dev/commit/123da5b5f0e151303415ab911f24b8ffa6d407f4))
* **javascript:** add language method to JsDependenciesSnapshot [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([bbb480b](https://github.com/unit-mesh/auto-dev/commit/bbb480b236bbb5e5831e9f3041b41b024a3b0a53))
* **javascript:** add mostPopularFrameworks method to JsDependenciesSnapshot [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([7792907](https://github.com/unit-mesh/auto-dev/commit/7792907e7f5c615c816f6abf93f79dd3854e0b1c))
* **javascript:** add new files and modify existing files [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([e3a53bf](https://github.com/unit-mesh/auto-dev/commit/e3a53bf4c42395679c08aa91838275a58eeaa3c5))
* **javascript:** add ReactFlow pages retrieval ([76bcefc](https://github.com/unit-mesh/auto-dev/commit/76bcefcaefa7873a163a9c74294ee5eba648ce5b))
* **javascript:** add ReactUtil and ReactUtilTest [#81](https://github.com/unit-mesh/auto-dev/issues/81) ([d1323f9](https://github.com/unit-mesh/auto-dev/commit/d1323f9716db1290356688fe9b5cb7d686eeeef5))
* **javascript:** add serialization to DsComponent ([08431fb](https://github.com/unit-mesh/auto-dev/commit/08431fb4676f9c041088357b3d4a385f8bd46dcc))
* **pair:** add LayeredArch and ProjectPackageTree classes ([d71b8d5](https://github.com/unit-mesh/auto-dev/commit/d71b8d54b637ac3e3f9251df7c662f5c4ba3c518))
* **pair:** improve KotlinWriteTestService and TreeNodeTest ([120a59b](https://github.com/unit-mesh/auto-dev/commit/120a59b6b47b56539f1eec678ad632ddac5197b7))
* **tasking:** add Tasking class and test cases [#79](https://github.com/unit-mesh/auto-dev/issues/79) ([f7244e4](https://github.com/unit-mesh/auto-dev/commit/f7244e47b575f18c924429f5c3c2794807bfe814))
* **template:** add overrideTemplate method && closed [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([0f4ef52](https://github.com/unit-mesh/auto-dev/commit/0f4ef526ce1904fb9b004e3582144b55fa66cd57))


### Reverts

* Revert "refactor(project): update module names and file paths" ([092e029](https://github.com/unit-mesh/auto-dev/commit/092e0291eca860c7a0804dde5bce403da521dc79))



## [1.5.5](https://github.com/unit-mesh/auto-dev/compare/v1.5.4...v1.5.5) (2024-01-21)


### Bug Fixes

* **java:** add Java language check in AutoCrudAction ([6669b4b](https://github.com/unit-mesh/auto-dev/commit/6669b4ba8cfa142805760eb894e59ca6a765753b))
* **llm:** add trailing slash to customOpenAiHost && fixed [#77](https://github.com/unit-mesh/auto-dev/issues/77) ([f68d124](https://github.com/unit-mesh/auto-dev/commit/f68d12431e5ed774ec22f6acbaaa33810e72f4a8))
* **test:** add check for now writing test service [#78](https://github.com/unit-mesh/auto-dev/issues/78) ([a4b0d04](https://github.com/unit-mesh/auto-dev/commit/a4b0d04c285a1c702af0d55cc11953fb67beb8ad))


### Features

* **database:** add SQL living documentation support ([08c82bd](https://github.com/unit-mesh/auto-dev/commit/08c82bdd6865bfb824e8008a845335ddf013ebb2))
* **database:** improve finding nearest SQL definition ([6e95d47](https://github.com/unit-mesh/auto-dev/commit/6e95d47186cccadc981369172ceb9ebcf09da9ea))
* **docs:** add basic PL/SQL implementation ([478f1d9](https://github.com/unit-mesh/auto-dev/commit/478f1d9bab7f2347ac34bbc9cdfd0b627e1919e9))
* **java:** add detectLanguageLevel function ([6f7b156](https://github.com/unit-mesh/auto-dev/commit/6f7b156bd6d28ba7fef67f5f21654d948e86501b))
* **provider:** add language level detection ([8cd2584](https://github.com/unit-mesh/auto-dev/commit/8cd25842513a7fb7e999ae2df7b93d9aa01cb326))
* **rust:** add support for EnumContext ([d58b435](https://github.com/unit-mesh/auto-dev/commit/d58b435ddc6bc6e6c5cc0dedca1f0f6bb6efd341))
* **scala:** add ScalaClassContextBuilder and test case ([98ef74f](https://github.com/unit-mesh/auto-dev/commit/98ef74fc68b21399f0e6b58e90f81ffb63dd282d))
* **sql:** add functionality to update living documentation ([c99b21d](https://github.com/unit-mesh/auto-dev/commit/c99b21d09d42532fffd601e82c0d0a41dda88f61))



## [1.5.4](https://github.com/unit-mesh/auto-dev/compare/v1.5.3...v1.5.4) (2024-01-19)


### Bug Fixes

* **context:** fix null pointer exception in MethodContext and JSWriteTestService ([e476620](https://github.com/unit-mesh/auto-dev/commit/e4766201f5c13c5349bba9f985d3d0becea4f98b))
* **cpp:** fix 222 version issue ([fd3adf0](https://github.com/unit-mesh/auto-dev/commit/fd3adf0887cb83c9c7b2a86e7ff7067b39174f5a))
* **cpp:** fix error type issue in cpp ([2f857d9](https://github.com/unit-mesh/auto-dev/commit/2f857d9d12008125a84d66e8114f03c74911e588))
* **cpp:** fix run config issue for temp, make as todos ([a4bce6f](https://github.com/unit-mesh/auto-dev/commit/a4bce6fe1f57276e3e4ea821ee2adac5f5d5da74))
* **cpp:** update test cases for CppContextPrettifyTest, CppClassContextBuilderTest, and CppMethodContextBuilderTest ([be189fb](https://github.com/unit-mesh/auto-dev/commit/be189fb5d105b6ec3ccdfbef4fdd770bf4ba99f4))
* fix import issues ([f298b1d](https://github.com/unit-mesh/auto-dev/commit/f298b1d78007b50ee4198f7fc4370660c48fdf48))
* fix release path issues ([f18f664](https://github.com/unit-mesh/auto-dev/commit/f18f66495dd935f18b48a1f93ed5198ae5a627d4))
* fix typos ([fa14939](https://github.com/unit-mesh/auto-dev/commit/fa1493951d7931f1d526ae54460456af0d40079f))
* **flow:** implement getStories method ([c274f4b](https://github.com/unit-mesh/auto-dev/commit/c274f4bdb032a080bde8ad5069b2f43468749256))
* **i18n:** fix typo and shorten message length ([8e2f120](https://github.com/unit-mesh/auto-dev/commit/8e2f120bbed77e22985c89b45ad0499a0e8837c5))
* **java:** resolve issue with resolving classes in JavaTypeUtil ([e13fc40](https://github.com/unit-mesh/auto-dev/commit/e13fc4008db50baa5337a55ecd9bb3fc0f526e07))
* **javascript:** modify JavaScriptVersionProvider and add JavaScriptVersionProviderTest ([7113149](https://github.com/unit-mesh/auto-dev/commit/7113149cf0eb28a50c6e4af5b47c23824136b5cc))
* **provider:** add logger and remove unnecessary code ([c3bfc53](https://github.com/unit-mesh/auto-dev/commit/c3bfc53b9712d6a2d9034d7567652b3aa5156f14))
* **provider:** improve error message for missing language support ([19b6940](https://github.com/unit-mesh/auto-dev/commit/19b6940c3974bdf1700ebffe4e1a7902b97b662f))
* **rust:** fix 233 typo ([31c5f10](https://github.com/unit-mesh/auto-dev/commit/31c5f10ffe9b78ad0c6eaff558dfbb71458acd14))
* **rust:** fix tests ([c6761e8](https://github.com/unit-mesh/auto-dev/commit/c6761e87bc5bc01b3399350abed2358e739e8500))
* **service:** modify JavaWriteTestService to use JavaLanguage ([ea37212](https://github.com/unit-mesh/auto-dev/commit/ea3721287b020d94e49da33593a213442de9bffb))
* **test:** add error notification when test file creation fails ([45f21b2](https://github.com/unit-mesh/auto-dev/commit/45f21b2fd37d1510b922ae9057bf4f89385ec52c))
* **util:** rename JsUtil package ([f747922](https://github.com/unit-mesh/auto-dev/commit/f747922f58bd84604f0b81e403f0c475af03fd89))


### Features

* **autodev:** add Open Settings action to autodev-core.xml ([b192d30](https://github.com/unit-mesh/auto-dev/commit/b192d3018a209f19c46bdb465853afd56b44e6b7))
* **build:** add kover plugin for code coverage ([1e06da0](https://github.com/unit-mesh/auto-dev/commit/1e06da0e316bba83380255e0e435b386d1833909))
* **ci:** add Codecov coverage report upload ([f67277a](https://github.com/unit-mesh/auto-dev/commit/f67277a0c42e0d7017490f39f3ffd916c027d185))
* **codecov:** add Codecov badges to documentation and README ([b19f3bb](https://github.com/unit-mesh/auto-dev/commit/b19f3bb180e7c89bab406f057234f317c2fb9de7))
* **commit-message:** add guidelines for writing commit messages ([a4108bc](https://github.com/unit-mesh/auto-dev/commit/a4108bc49dece5d10553020a1468369dfa174759))
* **commit:** add commit message generation with template rendering ([ddbead7](https://github.com/unit-mesh/auto-dev/commit/ddbead757cc772f96e11a877e332e12b5f2cd626))
* **context:** add CppVariableContextBuilder ([f98ee90](https://github.com/unit-mesh/auto-dev/commit/f98ee90742e2b88720592d82560ae99bcf9060c6))
* **context:** improve formatting in ClassContext and VariableContext ([274ebc7](https://github.com/unit-mesh/auto-dev/commit/274ebc7a3bc4197306c3acf2534482058765631e))
* **cpp:** add CMakefileUtil and CppBuildSystemProvider ([cd6ccae](https://github.com/unit-mesh/auto-dev/commit/cd6ccaef6777b873083c35131977436479f45e6b))
* **cpp:** add comment about testing framework choice ([4aceadd](https://github.com/unit-mesh/auto-dev/commit/4aceadd079d40f8fdaba40fa5c1e87f53705b229))
* **cpp:** add CppCodeModifier class ([5b374c4](https://github.com/unit-mesh/auto-dev/commit/5b374c49698020d104b58f8eafff219be11ec454))
* **cpp:** add CppContextPrettify utility class and test case ([b3d9dd6](https://github.com/unit-mesh/auto-dev/commit/b3d9dd66292acf6a57d006f47eb1f4dff0c03c73))
* **cpp:** add CppFileContextBuilder for ObjectiveC ([f8208f9](https://github.com/unit-mesh/auto-dev/commit/f8208f909d9a6341a6e23e3a9e2a6125d24cc51a))
* **cpp:** add CppWriteTestService and CppContextPrettify modifications ([cce73c2](https://github.com/unit-mesh/auto-dev/commit/cce73c28c6fcff417ac94b8e1866b0751b4f6e91))
* **cpp:** add support for additional CIDR language constructs ([4b3588f](https://github.com/unit-mesh/auto-dev/commit/4b3588f252c3a310fb263ac721f64f7acecb4431))
* **cpp:** add test file creation logic ([89cda73](https://github.com/unit-mesh/auto-dev/commit/89cda732231887d0540780f6e684905ea0c62ff3))
* **directory:** add AutoDevDirectoryCompletionContributor ([a51de46](https://github.com/unit-mesh/auto-dev/commit/a51de4621fd13187423a0b2721cca32ab7bb7bd8))
* **docs:** add guide for configuring new language plugin ([f6e068b](https://github.com/unit-mesh/auto-dev/commit/f6e068bf47f1c28d1f17e31c25fd5a24522cb296))
* **github-actions:** add workflow generation ([9c018ca](https://github.com/unit-mesh/auto-dev/commit/9c018ca0dbd31e923df52dc20329dfa532516f15))
* **go:** add Go version and target version to chat context ([5ff85e0](https://github.com/unit-mesh/auto-dev/commit/5ff85e068d4ce73e39353538fa7e62806d063ac8))
* **go:** add GoStructContextBuilder and GoStructContextBuilderTest ([8d01da3](https://github.com/unit-mesh/auto-dev/commit/8d01da33423a8cef59a079a29c21462e27683dc2))
* **go:** add GoVariableContextBuilder and GoFileContextBuilder ([584e6c5](https://github.com/unit-mesh/auto-dev/commit/584e6c5f9c8104582a20273dbce29b5f57767414))
* **go:** add GoVersionChatContextProvider and GoWriteTestService ([aa875ec](https://github.com/unit-mesh/auto-dev/commit/aa875ec27d779dd57be3980c39a93e4d5155252a))
* **go:** add method context builder test ([3a5eaf3](https://github.com/unit-mesh/auto-dev/commit/3a5eaf3ae1df0067ebe3b7b3eedd650641b98cca))
* **goland:** add Go language support ([27ff5a6](https://github.com/unit-mesh/auto-dev/commit/27ff5a615949fdc1fae5e1207eac415761518fc2))
* **goland:** add GoMethodContextBuilder and GoPsiUtil ([5b89fc5](https://github.com/unit-mesh/auto-dev/commit/5b89fc579b28ec613e4a21bfd8e79ebd0b905619))
* **goland:** add GoWriteTestService for writing test cases ([2eef8d1](https://github.com/unit-mesh/auto-dev/commit/2eef8d1ad0aea989ebd5fb116b3c67e39a778eeb))
* **java, javascript:** add type resolution in test and fix return type handling ([10d9d87](https://github.com/unit-mesh/auto-dev/commit/10d9d873cc9603c49d30871dea8e29282cfb2590))
* **java:** add code prompter methods and documentation ([f7caff1](https://github.com/unit-mesh/auto-dev/commit/f7caff1567f28003b04592ddddcdf2ad76f70ea2))
* **javascript:** add import statements to TestFileContext ([2ce3bd4](https://github.com/unit-mesh/auto-dev/commit/2ce3bd4fa349643e235be582bc236a52b7d7c2b4))
* **javascript:** add JavaScriptClassContextBuilder and update JSPsiUtil ([8123892](https://github.com/unit-mesh/auto-dev/commit/812389267862b7204166baf07ff8861f00f037cc))
* **javascript:** add logging to JSWriteTestService ([ce00e1d](https://github.com/unit-mesh/auto-dev/commit/ce00e1d2ba20145d548fc3fd2a2fedafd90565d9))
* **javascript:** add test cases for JSWriteTestService ([99d3124](https://github.com/unit-mesh/auto-dev/commit/99d31245123237480d0f335128e4791e535e4dfd))
* **javascript:** improve test file generation ([e7a3a93](https://github.com/unit-mesh/auto-dev/commit/e7a3a938aabe928091694bc1fabba085a368c9f7))
* **provider:** add test element to TestFileContext ([79aba96](https://github.com/unit-mesh/auto-dev/commit/79aba9631a5f4a10072b9e64c640fce0552d1723))
* **python:** add Python language support to WriteTestService ([5e68d61](https://github.com/unit-mesh/auto-dev/commit/5e68d61ca05729072ec4df9e2cc291d96ef46875))
* **rust:** add forbiddenRules to RustLivingDocumentation ([166ec2d](https://github.com/unit-mesh/auto-dev/commit/166ec2d525c7ead698983c24e0d3d563d1bdd722))
* **rust:** add relevant classes to TestFileContext ([3e0c992](https://github.com/unit-mesh/auto-dev/commit/3e0c99273571698a86e6f52b1c1fe484dd25e362))
* **rust:** add Rust plugin to build.gradle.kts ([5611309](https://github.com/unit-mesh/auto-dev/commit/5611309e81a74417cf0b787956a4a428993f20c9))
* **rust:** add RustClassContextBuilderTest and modify RustClassContextBuilder ([5aa76c2](https://github.com/unit-mesh/auto-dev/commit/5aa76c249822610772c2bdd7540ccd96e9686568))
* **rust:** add RustCodeModifier class ([f538223](https://github.com/unit-mesh/auto-dev/commit/f5382233dfbf340d9602174cab11163b056a287b))
* **rust:** add RustFileContextBuilder for file context ([5c7ad6f](https://github.com/unit-mesh/auto-dev/commit/5c7ad6f7c9326d9dd3f4665fae7aac620b034c04))
* **rust:** add RustTestContextProvider and update WriteTestService ([eab438b](https://github.com/unit-mesh/auto-dev/commit/eab438bd15cd8684928ca62a17500723f320ea91))
* **rust:** add RustVariableContextBuilder ([96063c6](https://github.com/unit-mesh/auto-dev/commit/96063c6e2fe8dc5da0249b7109adbc3797468a48))
* **rust:** add support for formatting impl items ([af31942](https://github.com/unit-mesh/auto-dev/commit/af31942071aa314398cf25c7983ea0ef208d848a))
* **rust:** add test code insertion functionality ([7dd4cfd](https://github.com/unit-mesh/auto-dev/commit/7dd4cfd0d1f70d9aebd32387dd6f5a3a49c7ba19))
* **rust:** ignore tests for 2333 ([a4fe3b9](https://github.com/unit-mesh/auto-dev/commit/a4fe3b9f13a530049a131c1111d5987f60c4fce8))
* **rust:** update RustVersionContextProvider, RustTestService, RunCLion.xml, build.gradle.kts, RustMethodContextBuilder ([c41b4b1](https://github.com/unit-mesh/auto-dev/commit/c41b4b15975054a927a0439faf26c625a9648a6f))
* **statusbar:** add AutoDev status bar widget and action ([6747716](https://github.com/unit-mesh/auto-dev/commit/67477165a3330bed63d8ec17e567ca2a01e7f87f))
* **statusbar:** add AutoDev status bar widget factory ([38b37f6](https://github.com/unit-mesh/auto-dev/commit/38b37f657fad20f6f49e3c1fbf73794eb7092798))
* **statusbar:** add AutoDevStatusId to AutoDevStatusBarWidget ([a0993d6](https://github.com/unit-mesh/auto-dev/commit/a0993d66e393314e9d60d3ab6f530b2e5281505a))
* **statusbar:** add AutoDevStatusListener interface ([fab07cd](https://github.com/unit-mesh/auto-dev/commit/fab07cd91c9ebd3e171c87670ea8725b53768d9e))
* **statusbar:** add AutoDevStatusService and AutoDevStatus classes ([54269bc](https://github.com/unit-mesh/auto-dev/commit/54269bcc2a878bab0e548cd3e4196c1baa8b64ba))
* **statusbar:** add new status icons ([be22f9a](https://github.com/unit-mesh/auto-dev/commit/be22f9a5e349faee9a5a309d155a2813bd9154d4))
* **statusbar:** add status event producer ([66ef52f](https://github.com/unit-mesh/auto-dev/commit/66ef52f1c570823b5c9ef083e6edc2259ed9a6c8))
* **statusbar:** add status notifications during test generation ([b2aa8df](https://github.com/unit-mesh/auto-dev/commit/b2aa8dfca661f8ec7b3fb34cf5eceadefcc1a2ba))
* **statusbar:** enable AutoDevStatusBarWidget on status bar ([93bcffe](https://github.com/unit-mesh/auto-dev/commit/93bcffe2e28a3a7e9054e2ef413dc9a2a8baa170))
* **template:** add new AutoDev customize prompt action ([cf0b8d6](https://github.com/unit-mesh/auto-dev/commit/cf0b8d6b851ba4df538219da1004ca38ed704c42))
* **test:** add test case for Test.kt ([5a2d2cc](https://github.com/unit-mesh/auto-dev/commit/5a2d2ccb715b31f778f274f5c09980e5dc28153b))
* **test:** add test code templates ([2af285d](https://github.com/unit-mesh/auto-dev/commit/2af285dfd943b41b6de3b827a3595374fcfaef94))
* **testing:** add flow.collect to TestCodeGenTask ([2bb353e](https://github.com/unit-mesh/auto-dev/commit/2bb353e1894a083aa59e9f93d13783b32e6fda54))
* **testing:** add source code to be tested ([fc1763f](https://github.com/unit-mesh/auto-dev/commit/fc1763fcb57858cdbb97e21ccadc0bb347169586))
* **testing:** add webstorm version to build.gradle.kts ([363329b](https://github.com/unit-mesh/auto-dev/commit/363329b32f9c5631ebb39cb90b5944198c28ca08))
* **util:** add isInProject function ([e806624](https://github.com/unit-mesh/auto-dev/commit/e8066243efcd26f1cc40606519f362b034a4e766))
* **vcs:** add commit message suggestion functionality ([fb3caac](https://github.com/unit-mesh/auto-dev/commit/fb3caaca0d3b31163cb08688938fd462f04ef639))
* **vcs:** add flow.collect to CommitMessageSuggestionAction ([396317b](https://github.com/unit-mesh/auto-dev/commit/396317bfd8de7762caeab4e67cb57f67de4d4b56))
* **webstorm:** add guessTestFrameworkName utility function ([ce4baad](https://github.com/unit-mesh/auto-dev/commit/ce4baad6d94208ed66826120168d61bde0f2cd92))
* **webstorm:** add JavaScript test framework detection ([3e78eee](https://github.com/unit-mesh/auto-dev/commit/3e78eee7c5ed255d0bc9410385d2b7e1236f793a))
* **webstorm:** add JavaScriptVersionProvider ([8dd8902](https://github.com/unit-mesh/auto-dev/commit/8dd890291409f68e73dcbaf2bc8350e055644eca))
* **webstorm:** add support for web chat creation context ([0971813](https://github.com/unit-mesh/auto-dev/commit/09718139bd162e8e9414264d0ca0a541c1bb5002))
* **webstorm:** add utility functions for JavaScript testing ([10cc3ea](https://github.com/unit-mesh/auto-dev/commit/10cc3ea03f755c9223666c10d02eade53f1d272c))



## [1.5.3](https://github.com/unit-mesh/auto-dev/compare/v1.5.2...v1.5.3) (2024-01-12)


### Bug Fixes

* **commit:** empty commit message before generate ([25c4559](https://github.com/unit-mesh/auto-dev/commit/25c4559cb559679680d25ec1ca439bc2b159dcf6))
* **diff:** fix file rename message formatting ([2039103](https://github.com/unit-mesh/auto-dev/commit/203910341cc114310f4a0a2b12ac27a235d7739b))
* ignore test for 222 version ([bcd208d](https://github.com/unit-mesh/auto-dev/commit/bcd208d07847aab542a077a6288c5785122f0c9f))
* **kotlin:** update Kotlin API version message ([e3cee52](https://github.com/unit-mesh/auto-dev/commit/e3cee52e62843d657b22fe8c80ed8dfe4e7d9297))
* **provider:** fix KotlinTestDataBuilderTest and KotlinClassContextBuilder ([d23b9d5](https://github.com/unit-mesh/auto-dev/commit/d23b9d59d2ec772f341cc53fe23db6978ec4e30e))
* **provider:** insert method if code does not contain @Test annotation ([d49f41f](https://github.com/unit-mesh/auto-dev/commit/d49f41f6b095d4be9923c135b302e3d3ecffd199))
* **provider:** refactor KotlinTestContextProvider ([fa3364a](https://github.com/unit-mesh/auto-dev/commit/fa3364a274bba9faa89815e40c3407820f5c70ec))
* **vcs:** fix CommitMessageSuggestionAction not updating editorField text ([ef6c680](https://github.com/unit-mesh/auto-dev/commit/ef6c6802d0a91085d96e66225ddcce65eca80c5a))


### Features

* **code-review:** add action to ChangesViewToolbar ([6ccb3e7](https://github.com/unit-mesh/auto-dev/commit/6ccb3e7b5242793b2ebead802728c71eaf29b460))
* **context:** add support for annotations in ClassContext ([abc5305](https://github.com/unit-mesh/auto-dev/commit/abc53050c13ed5fa99f605bb31b6670f882e9b9b))
* **context:** add support for parsing Java annotations ([520ad09](https://github.com/unit-mesh/auto-dev/commit/520ad095065655298819ce2ec15ed433bfe7e9fc))
* **context:** update ClassContext to include package information ([b0f9fcb](https://github.com/unit-mesh/auto-dev/commit/b0f9fcb5ebdc5a4df1089916b5f2315ce04aecda))
* **diff:** add handling for delete files ([72724da](https://github.com/unit-mesh/auto-dev/commit/72724da5c629c784f0169336467ad969c29fe214))
* **diff:** add test case for DiffSimplifier ([f6dde52](https://github.com/unit-mesh/auto-dev/commit/f6dde52a680a0fb5ab8812700fbbcb40bb70f080))
* **java:** add base route retrieval to JavaTestDataBuilder ([df4d12e](https://github.com/unit-mesh/auto-dev/commit/df4d12eb70e2af7f5db235709af138d9776dae21))
* **java:** add JUnit rule caching mechanism ([21b5ae2](https://github.com/unit-mesh/auto-dev/commit/21b5ae2666c0a444ff25cd1952af42530a72d086))
* **kotlin:** add baseRoute method to KotlinTestDataBuilder ([3da7e16](https://github.com/unit-mesh/auto-dev/commit/3da7e16f9e65215a5383be828ec6a53082b213cb))
* **kotlin:** add getTypeText() function to KotlinContextCollector ([0f29abe](https://github.com/unit-mesh/auto-dev/commit/0f29abeff8cab49ea6d764b992e3e66e77c4e960))
* **kotlin:** add KotlinContextCollector class ([8845285](https://github.com/unit-mesh/auto-dev/commit/8845285a0b03111aa6c251b17afbb0d3d1ecbe45))
* **kotlin:** add KotlinVersionProvider ([c75e0fc](https://github.com/unit-mesh/auto-dev/commit/c75e0fc1ddc330ed6fd0e99246f756c8bb11d901))
* **kotlin:** add KotlinVersionProvider ([92c6611](https://github.com/unit-mesh/auto-dev/commit/92c661160490ceb39f6d9af7abf240d31b452502))
* **kotlin:** add test case for KotlinTestDataBuilder ([bca537e](https://github.com/unit-mesh/auto-dev/commit/bca537e75279b523edf5b798b8a7122bce17ac81))
* **living documentation:** add functionality to build documentation from suggestion ([8bc6677](https://github.com/unit-mesh/auto-dev/commit/8bc66777e2d51947333378fb7485417d48010e70))
* **livingdoc:** improve prompt builder instructions ([e4f319a](https://github.com/unit-mesh/auto-dev/commit/e4f319a35956769c6f782e710439d763ed21c107))
* **livingdoc:** improve prompt builder instructions ([08111fb](https://github.com/unit-mesh/auto-dev/commit/08111fb5be6e9b0cebe6bb1777bfcb849aacc1ea))
* **prompting:** add more file patterns to default ignore list ([19e58e8](https://github.com/unit-mesh/auto-dev/commit/19e58e861e69750471995729fbd97ee27aa9e701))
* **provider:** add imports to TestFileContext ([05419e5](https://github.com/unit-mesh/auto-dev/commit/05419e5ace469bebb425c3c72759539b32d3a9d2))
* **provider:** add isSpringRelated method to KotlinTestContextProvider ([06173db](https://github.com/unit-mesh/auto-dev/commit/06173dbb8f0316f3ab923231f0bb5e9201a39528))
* **provider:** add KotlinTestContextProvider ([c5903df](https://github.com/unit-mesh/auto-dev/commit/c5903df38c859ebe52366bf5b2913280fa3f1a9f))
* **provider:** add KotlinTestDataBuilder implementation ([559fcd4](https://github.com/unit-mesh/auto-dev/commit/559fcd4fe76c3cdcf2c19693050128ccdf77a39a))
* **provider:** add module dependency lookup ([e1a8da6](https://github.com/unit-mesh/auto-dev/commit/e1a8da68531ba12b9b57488c2741367352f3dc30))
* **provider:** add support for Spring annotations ([296169d](https://github.com/unit-mesh/auto-dev/commit/296169dc8b599386b5e96c96ba9009bd3ab269a1))
* **provider:** add templated test prompts ([94d588a](https://github.com/unit-mesh/auto-dev/commit/94d588a1aea6c8d09da86feab3d0d474c6e38c6a))
* **readme:** update installation instructions ([582b337](https://github.com/unit-mesh/auto-dev/commit/582b337169bce343fbb244bc94839f969e8a702b))
* **review:** remove unused methods and refactor computeDiff ([5c59c2e](https://github.com/unit-mesh/auto-dev/commit/5c59c2e6d5ceaceb0515de96ceb4c510f39cc488))
* **runConfigurations:** add BuildPlugin.xml ([3e92547](https://github.com/unit-mesh/auto-dev/commit/3e925470a12835741eba51b765d20c5e9fc990d7))
* **test:** add KotlinMethodContextBuilderTest ([11536f3](https://github.com/unit-mesh/auto-dev/commit/11536f36a2e279b149665db59ad75657f02af1a2))
* **test:** add logging for prompter ([2cae29e](https://github.com/unit-mesh/auto-dev/commit/2cae29eb656236c73313ebff1876b6d801d91251))
* **test:** add test case for MethodController ([776dab5](https://github.com/unit-mesh/auto-dev/commit/776dab52d77c678ea34a923f63d48473b12b7d27))
* **testing:** update test prompt with language information ([ce2abef](https://github.com/unit-mesh/auto-dev/commit/ce2abefdd16b9b742c57551a8f9d3d8a60d54367))
* **tests:** add code snippet to prompter ([b5a24ae](https://github.com/unit-mesh/auto-dev/commit/b5a24ae62a588e7bb155399c1e52bd8c65359f46))
* **tests:** add KotlinTestDataBuilderTest and KotlinTestDataBuilder ([07a0dd9](https://github.com/unit-mesh/auto-dev/commit/07a0dd98fbb59563b522a57cd730192e9fc84222))
* **vcs:** add asynchronous commit message suggestion ([6bfd34c](https://github.com/unit-mesh/auto-dev/commit/6bfd34c59ec431f68c01935afa19098a4bf87ecd))
* **vcs:** add logging for empty diff context ([16ab325](https://github.com/unit-mesh/auto-dev/commit/16ab32516085b3e52dc9a0678692ca50ab8f142f))
* **vcs:** add notification for empty diff context ([f2382c5](https://github.com/unit-mesh/auto-dev/commit/f2382c5d75612fd3fa78520dadff418cec623c15))
* **vcs:** add PrepushReviewAction ([d16bb38](https://github.com/unit-mesh/auto-dev/commit/d16bb38bde4339c8388976fac07b26a4e711c760))
* **vcs:** add VcsPrompting import and service ([d25f92b](https://github.com/unit-mesh/auto-dev/commit/d25f92be86d38635097c1972e0a28414bb3256a7))
* **vcs:** generate commit message with prompt ([8d6932e](https://github.com/unit-mesh/auto-dev/commit/8d6932e252a29898204e65a9bedf501a6e056d54))



## [1.5.2](https://github.com/unit-mesh/auto-dev/compare/1.4.4...v1.5.2) (2024-01-05)


### Bug Fixes

* add lost messages for Azure ([468a5ce](https://github.com/unit-mesh/auto-dev/commit/468a5ce4c3fe27960d31fd73ee9e1818fdf16025))
* **apidata:** disable java in qualified name ([db4646a](https://github.com/unit-mesh/auto-dev/commit/db4646a68960f4c10e228a95e0fca5884439535e))
* **apidata:** fix fieldName erorr for class strcuture ([af5a87b](https://github.com/unit-mesh/auto-dev/commit/af5a87b43c09a47f5fac6e841248335964899510))
* **apidata:** fix simple handle for uppsercase ([6152514](https://github.com/unit-mesh/auto-dev/commit/61525145e6b21d9db8f54410324b47425ddc9fad))
* disable default choice for chat base action ([53a192b](https://github.com/unit-mesh/auto-dev/commit/53a192b7ec18e2cb2a966437d951a838f38f5cda))
* disable file cache ([97a793a](https://github.com/unit-mesh/auto-dev/commit/97a793aba43049726ebe24d336002c99ec987fc5))
* **doc:** update doc by experience ([1b374de](https://github.com/unit-mesh/auto-dev/commit/1b374def5805425592ffdc63dcc38f44cd08e14c))
* fix 222 build issue on import ([fb468a4](https://github.com/unit-mesh/auto-dev/commit/fb468a4dbbaef48e1fe8725b9eb8b415d5711d8a))
* fix auto completion lost request ([7914ec2](https://github.com/unit-mesh/auto-dev/commit/7914ec23531aec21dca5a56a0a52e80996109d14))
* fix axure auto service issue ([2ad6f43](https://github.com/unit-mesh/auto-dev/commit/2ad6f43f2ca456a1fe9c06f4320c4d4dca8aa87b))
* fix build & Depecrated 213 && 221 && closed [#59](https://github.com/unit-mesh/auto-dev/issues/59) ([1221128](https://github.com/unit-mesh/auto-dev/commit/1221128f9a59425bb9f81d79f965f03eb4d8e9e8))
* fix chat panel crash issue ([2818699](https://github.com/unit-mesh/auto-dev/commit/28186993d32c067d8d18ba95cc09c302b26b0b3b))
* fix commit message gen issues ([149069a](https://github.com/unit-mesh/auto-dev/commit/149069a7089eff89e9c0407b3c28a921ba75cad6))
* fix compile issue ([71a7eb1](https://github.com/unit-mesh/auto-dev/commit/71a7eb129feb8353d7d1f45b977750c3ded5efdc))
* fix config issues ([5c75798](https://github.com/unit-mesh/auto-dev/commit/5c75798eec41a1f4e99e93931f9ff88558d6558d))
* fix empty input issue ([aa2846b](https://github.com/unit-mesh/auto-dev/commit/aa2846b245bf2334bc58cbd31300be7003482d64))
* fix erro action again ([9cdadd2](https://github.com/unit-mesh/auto-dev/commit/9cdadd2c25cf1345a86edb635f0ee0dc83cf6d78))
* fix get issues ([02979db](https://github.com/unit-mesh/auto-dev/commit/02979db1b37aeed713e8e9258d30762bd355483a))
* fix import issue ([9dba8db](https://github.com/unit-mesh/auto-dev/commit/9dba8dbfe4a604d296f8e8bd86adc2bf8f9f7f71))
* fix imports ([463dc13](https://github.com/unit-mesh/auto-dev/commit/463dc1355bab485d47a0fb8054ac69c0924d5612))
* fix imports ([36f39d2](https://github.com/unit-mesh/auto-dev/commit/36f39d23f7e6bddfb61a24d3681db004f161cbbf))
* fix imports ([ed3db21](https://github.com/unit-mesh/auto-dev/commit/ed3db21eff80a87e800544cddf87b167468767f0))
* fix int autodev llm server error issue ([04691fc](https://github.com/unit-mesh/auto-dev/commit/04691fcd15cfbb2d707dc82731fcf70b2fbd2015))
* fix kotlin code modifier issue ([501fccc](https://github.com/unit-mesh/auto-dev/commit/501fccc2f6c911e97848c4f599e3a0e8f1f56a91))
* fix kotlin read issue ([96b13cb](https://github.com/unit-mesh/auto-dev/commit/96b13cb14db7bc445931c951d1192ed7e3844f25))
* fix python test isse ([e407586](https://github.com/unit-mesh/auto-dev/commit/e40758644411303351db8e4d18ea0772cde1dd52))
* fix replace issue in comment ([0683e6f](https://github.com/unit-mesh/auto-dev/commit/0683e6f96915c59252d8f4251dda7459de16d416))
* fix strategy issues ([333b75e](https://github.com/unit-mesh/auto-dev/commit/333b75e82974454370ea954950d11a5b68dc6445))
* fix super class lost issue ([31907a6](https://github.com/unit-mesh/auto-dev/commit/31907a6b3d58e78cc3807487ee657018ac2f0110))
* fix test ([7a70f8b](https://github.com/unit-mesh/auto-dev/commit/7a70f8b5c77082e3bd00d59f9d0316b750401aba))
* fix test issues ([4a9ea9c](https://github.com/unit-mesh/auto-dev/commit/4a9ea9c095ef531b7e259edbc106ae99f273b506))
* fix test prompt issue ([ec89161](https://github.com/unit-mesh/auto-dev/commit/ec89161d896c5bc8b68a64d4b2e6ec656ea925d4))
* fix tests ([688499c](https://github.com/unit-mesh/auto-dev/commit/688499c4ec5bf4b1fbd10cdf87e9d907d80378f5))
* fix tests ([f452428](https://github.com/unit-mesh/auto-dev/commit/f452428f8dd43c8b49294009bc558612fc482770))
* fix tests ([ab69050](https://github.com/unit-mesh/auto-dev/commit/ab69050f36f71dcca4b4f2b073babfefad2e5fb7))
* fix typos ([95e1a9f](https://github.com/unit-mesh/auto-dev/commit/95e1a9f78bbba9b75828cd7a9ebd584c3c4e761f))
* is some super class no in project will be crash ([d203734](https://github.com/unit-mesh/auto-dev/commit/d2037344a0e5779466181995aa4e31388b1a4868))
* make output in file better ([6a25cf3](https://github.com/unit-mesh/auto-dev/commit/6a25cf3375a0ae654e1ee20e41501b0b7b1c13c1))
* 修复前一提交 SSE 的问题 ([54457f5](https://github.com/unit-mesh/auto-dev/commit/54457f558ce1f8c939fead5562835a6e7a6c13ae))


### Features

* add catch for not ready action ([79286b0](https://github.com/unit-mesh/auto-dev/commit/79286b022a2d759d7198d912afaa1e37714712f6))
* add cleanup for kotlin code ([dd84795](https://github.com/unit-mesh/auto-dev/commit/dd847957bd08c4bf44ea5da37407591391c09b87))
* add config for disable advanced prompt ([40b61f1](https://github.com/unit-mesh/auto-dev/commit/40b61f16d2ec6b64e35b57f26e7215a72ed08184))
* add filter for caches and skip for popular frameworks ([6e4c205](https://github.com/unit-mesh/auto-dev/commit/6e4c205faa1b18dc34dfc5728764f651f5b2e662))
* add for junit rules ([dc5289d](https://github.com/unit-mesh/auto-dev/commit/dc5289dba409b3ef46752cff95e66869a2e2a9fd))
* add for test file path ([16fc139](https://github.com/unit-mesh/auto-dev/commit/16fc139f1ad8dde10c939e01c8578fcdb250ced9))
* add more for overvide prompts [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([f9b37b8](https://github.com/unit-mesh/auto-dev/commit/f9b37b8f49ba33adbd4b4d7c32736b3a89fe5673))
* add timeout for handler ([687b04a](https://github.com/unit-mesh/auto-dev/commit/687b04aabfe76f8ef9227f721843288e0c6ec843))
* **coder:** add custom for generate test [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([b10a0f5](https://github.com/unit-mesh/auto-dev/commit/b10a0f503f2aa6be02a001e45dc7ddd5dafd6102))
* **coder:** add disable history messages [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([265d343](https://github.com/unit-mesh/auto-dev/commit/265d343ca896ec5206a32ea9df1b02809c35500b))
* **coder:** enable custom txt for prompt ([019ae0d](https://github.com/unit-mesh/auto-dev/commit/019ae0d6a94d8132af8df383f00c9e6e6459a039))
* **coder:** init for setting config ([6db0dc8](https://github.com/unit-mesh/auto-dev/commit/6db0dc8b6b961a5abe39c340b7efe7c35cf95e4f))
* **coder:** init local completion for sample code ([c7b8d21](https://github.com/unit-mesh/auto-dev/commit/c7b8d2141bacc08bee95ece6d25a2568371ef683))
* enable recording datasets works in local ([2fcab1c](https://github.com/unit-mesh/auto-dev/commit/2fcab1c532933f461627504eafa11de48098d27a))
* init recording local param for [#54](https://github.com/unit-mesh/auto-dev/issues/54) ([f24cc6a](https://github.com/unit-mesh/auto-dev/commit/f24cc6a90b2c1200c254cab255869c441cd4ca57))
* **js:** add documentation support ([f6b0136](https://github.com/unit-mesh/auto-dev/commit/f6b0136892b1471a4c6d5c4cb263f32a4926bc79))
* **js:** make doc better ([c69a335](https://github.com/unit-mesh/auto-dev/commit/c69a3350ecfe5b8919c7623c1e1516da7db28d51))
* **python:** add lookup funciton for target ([6a828df](https://github.com/unit-mesh/auto-dev/commit/6a828df2d313a6f3b16dadeffaed01486801ebd8))
* **python:** fix insert issues ([6786175](https://github.com/unit-mesh/auto-dev/commit/6786175c34ea9a50d8f69935c099c880d359355e))
* **python:** init python doc ([c30111d](https://github.com/unit-mesh/auto-dev/commit/c30111de7ea3b43d34e0f4c99c96efc8b3c2586e))
* **rust:** fix for insert issue ([9ce7734](https://github.com/unit-mesh/auto-dev/commit/9ce7734e41943160a153adb3e72bc935d682b143))
* **rust:** init basic documentation ([a63049b](https://github.com/unit-mesh/auto-dev/commit/a63049b672ef8ae1e017dab445f631a305f68523))
* try to add unload listener ([11f7c28](https://github.com/unit-mesh/auto-dev/commit/11f7c287cba32b4d43157a5c84b23e22f9731789))
* try to handle for basic class issues ([2897dd7](https://github.com/unit-mesh/auto-dev/commit/2897dd743638ed9504a3c49752ab1b56b1704b52))
* update java prompts ([d321afe](https://github.com/unit-mesh/auto-dev/commit/d321afe66d2c6e8e87ef2d5f768b6c33dcd34df0))


### Reverts

* Revert "refactor: clean code" ([afe5460](https://github.com/unit-mesh/auto-dev/commit/afe54608c6028b2530b2c734d1e136785300c6b5))



## [1.4.4](https://github.com/unit-mesh/auto-dev/compare/v1.4.3...1.4.4) (2023-12-14)


### Bug Fixes

* customEngineServer -> customOpenAiHost ([cf03a2b](https://github.com/unit-mesh/auto-dev/commit/cf03a2b46607880e20d68763bf8266c1aac1b31f)), closes [#62](https://github.com/unit-mesh/auto-dev/issues/62)
* demo issue ([434cd77](https://github.com/unit-mesh/auto-dev/commit/434cd7717c6f0aaa7e145421a83e1a447d8161bf))
* fix import runblocking issue ([439b136](https://github.com/unit-mesh/auto-dev/commit/439b136b5337ea7205463ee560489b1837283c1b))
* fix lint ([fe52832](https://github.com/unit-mesh/auto-dev/commit/fe52832442bc61642428245246016f8024dd7898))
* fix syntax error issue ([8642d8d](https://github.com/unit-mesh/auto-dev/commit/8642d8d0e070c27039ebef1e8d847fc43f3f5ec9))
* fix 助手回答为空的情况。 ([e0cca51](https://github.com/unit-mesh/auto-dev/commit/e0cca51f80c4488ddab5c4cf74dc277ddd207a76))
* state 改为 getter 避免配置更新不生效 ([d53c6aa](https://github.com/unit-mesh/auto-dev/commit/d53c6aaa24cc5988e2301ee8741fd9fac13a9f2b)), closes [#62](https://github.com/unit-mesh/auto-dev/issues/62)
* state 改为 getter 避免配置更新不生效 ([a3b22ae](https://github.com/unit-mesh/auto-dev/commit/a3b22ae1db2dad0ade8c3d5a4834b39b31ddd28f)), closes [#62](https://github.com/unit-mesh/auto-dev/issues/62)


### Features

* add request format complete ([07bfced](https://github.com/unit-mesh/auto-dev/commit/07bfced3e4960f1dbc013d5f0fcf4aa753adce7c))
* add request format complete ([f341134](https://github.com/unit-mesh/auto-dev/commit/f341134f055a4be599f79906dc7409f938768596))
* add request format config ([e0f040e](https://github.com/unit-mesh/auto-dev/commit/e0f040ece73505afa312a484da0351e57eb5e465))
* add request format config ([ac03af8](https://github.com/unit-mesh/auto-dev/commit/ac03af87f2440a9e10c4b3517eaccc70f71d1fd0))
* add request format logic-1 ([054b587](https://github.com/unit-mesh/auto-dev/commit/054b587f71d794506ce638dafa594e92112ffd7a))
* add request format logic-1 ([20132e4](https://github.com/unit-mesh/auto-dev/commit/20132e4565a87d4d2a6cd434f12142ce1c434fde))
* add xinghuo api version config setting ([d6d035a](https://github.com/unit-mesh/auto-dev/commit/d6d035aace1f423bf6e0ec4c89ef5caeaf6be542))
* **ts:** init basic doc writing listener ([3786575](https://github.com/unit-mesh/auto-dev/commit/3786575ceaa7b37bde31c553b4d0eeb2ef8fcdd9))
* 增加自定义请求。可修改请求 Header 及 reqeust body ([a72f085](https://github.com/unit-mesh/auto-dev/commit/a72f0857bd9bec59a63b335cdea4d033027d850b))



## [1.4.3](https://github.com/unit-mesh/auto-dev/compare/v1.4.1...v1.4.3) (2023-11-20)


### Bug Fixes

* add disposed ([b7e467f](https://github.com/unit-mesh/auto-dev/commit/b7e467f66b60d67cfe1d8573360bd81106d41199))
* align to new api changes ([59999d8](https://github.com/unit-mesh/auto-dev/commit/59999d8b45cc8b7754d8408a590d759aca542a2b))
* fix build typos ([34b9b15](https://github.com/unit-mesh/auto-dev/commit/34b9b15f1141b27b311707b4cf6653f5017918fd))
* fix config isseu ([d9d5133](https://github.com/unit-mesh/auto-dev/commit/d9d5133dea363fd553584ccd8c869d1a1efab018))
* fix deps for 233 ([225cf49](https://github.com/unit-mesh/auto-dev/commit/225cf49b8b928e598a65001cfcc22ca9252c6d60))
* fix testing issue ([7fac889](https://github.com/unit-mesh/auto-dev/commit/7fac88917f15e5cd974efbdcf28dc1f37d50ec64))
* potential IndexOutOfBoundsException when streaming response ([c3f8c43](https://github.com/unit-mesh/auto-dev/commit/c3f8c43bc3501f16d790a0549d893e2094ca7c59))
* try to six string type issue ([f7aee83](https://github.com/unit-mesh/auto-dev/commit/f7aee83c98b8e0b4d5e6fb1637e70c99d5f45cb9))


### Features

* **213:** fix some deps version issues ([aad9032](https://github.com/unit-mesh/auto-dev/commit/aad90321518a0ca3a23ac82fa43dfacecc8fc6ff))
* add collect for class data structure [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([e0d9d89](https://github.com/unit-mesh/auto-dev/commit/e0d9d89f2e5788ce7b1135fb20ade673059b9885))
* add gen for test data [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([dc2bd1a](https://github.com/unit-mesh/auto-dev/commit/dc2bd1a803ec391becf9e4c3a39da255373a5c44))
* add lost boxed type for convert inout bound [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([ed67f2a](https://github.com/unit-mesh/auto-dev/commit/ed67f2ab4f45cce7aface52f1a4ad7ea44071cf0))
* impl output bound data [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([1a2e01e](https://github.com/unit-mesh/auto-dev/commit/1a2e01e973cb5a357914e15523c8200b246eb590))
* init render option for children [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([610e33b](https://github.com/unit-mesh/auto-dev/commit/610e33b143a4d8aea423591152aa039937fdc24e))
* make custom quick action works [#57](https://github.com/unit-mesh/auto-dev/issues/57) ([f7e12e0](https://github.com/unit-mesh/auto-dev/commit/f7e12e0566198817ed14c7841eabda1d693126eb))
* make java lang works and closed [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([b1b7440](https://github.com/unit-mesh/auto-dev/commit/b1b7440db7c08147fb82570e6bf7bf23ef2705b1))
* **migration:** init generate test data enterpoint [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([486e5a0](https://github.com/unit-mesh/auto-dev/commit/486e5a053a2dbcb55ac51a554a030b228322d568))
* thinking in prepare context for input and output [#52](https://github.com/unit-mesh/auto-dev/issues/52) ([1923476](https://github.com/unit-mesh/auto-dev/commit/1923476f344b5ea706815717b0d53900fc524275))
* **writing:** add prompt type for custom action prompt ([0bae8b0](https://github.com/unit-mesh/auto-dev/commit/0bae8b08d1200ef9a9fe94d72a9c5ca7ea0a8401))


### Reverts

* Revert "chore: setjdk to 11 for 213" ([3f34f89](https://github.com/unit-mesh/auto-dev/commit/3f34f89e939206f8a493ab584b84e771e836323f))



## [1.4.1](https://github.com/unit-mesh/auto-dev/compare/v1.4.0...v1.4.1) (2023-10-28)


### Bug Fixes

* fix deps issues ([9bd7e8f](https://github.com/unit-mesh/auto-dev/commit/9bd7e8f3ecee4496bd7c397581dfec007979e653))
* fix isue ([31c0ffe](https://github.com/unit-mesh/auto-dev/commit/31c0ffe44bba839ef76f7ee3bd30230f40fc1e6d))
* fix js/ts scripts issues ([5b6f892](https://github.com/unit-mesh/auto-dev/commit/5b6f89206e240e21b3b9edb61be252fea807e0ec))
* fix method no found issue ([8117c6a](https://github.com/unit-mesh/auto-dev/commit/8117c6a2ad2561a9c16917b480ed1b4842700558))
* fix select element error issue ([a297df8](https://github.com/unit-mesh/auto-dev/commit/a297df87bf0a4ea9bfe64292fa9cc9c11c2ecd4c))
* fix some read run action issue ([0368563](https://github.com/unit-mesh/auto-dev/commit/0368563b7b21927dcc31f06e1d8bc5d00d05ead4))
* fix typos ([6d97635](https://github.com/unit-mesh/auto-dev/commit/6d97635b274a810ee14680bbcf411b995a738b7d))
* fix write to test issues ([6ed83be](https://github.com/unit-mesh/auto-dev/commit/6ed83bec092971cf8116fb4e50c6d7a4a534ce83))


### Features

* **custom:** add support for kotlin language [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([b212ee2](https://github.com/unit-mesh/auto-dev/commit/b212ee22b85a2275d023faa1cd28f275c413a2c2))
* **custom:** init for psi method [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([f7b057f](https://github.com/unit-mesh/auto-dev/commit/f7b057f26d7b61b6758d4863eb7221a8e808d451))
* **custom:** init test method api for java [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([d67bdb1](https://github.com/unit-mesh/auto-dev/commit/d67bdb18befe7befb19db3ae6011a306aa036b27))
* init custom promtp context provider for [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([3d912f2](https://github.com/unit-mesh/auto-dev/commit/3d912f2d26f7d95abc5d49048287800ac3c5a2d5))
* **quick:** add dispose ([f1c2b9e](https://github.com/unit-mesh/auto-dev/commit/f1c2b9e7cf3ae30de260f3ed5aed74c15cfd7715))
* **quick:** add quick keyboard short ([21eea39](https://github.com/unit-mesh/auto-dev/commit/21eea39c162ded942f38e06826d4e71e2f9fe9cd))
* **quick:** init basic code insights ([c1df643](https://github.com/unit-mesh/auto-dev/commit/c1df6438f755c494bc4dc8560a50d03185b33fbe))
* **quick:** init basic quick prompt ([232720f](https://github.com/unit-mesh/auto-dev/commit/232720f4faa4ecfef9250223194ec583d0f1cf06))
* **quick:** init inlay panel layout ([20116b7](https://github.com/unit-mesh/auto-dev/commit/20116b7bedc816315dc150f3b59b025f8be3fbe6))
* **quick:** make quick action works ([efaff71](https://github.com/unit-mesh/auto-dev/commit/efaff7183b922dd3bb9d1f77f88e3c8875f89869))
* **quick:** make simplae code works ([b68f836](https://github.com/unit-mesh/auto-dev/commit/b68f8369e4ba79f122228adf493aca6034c1a8fb))
* **sre:** add lost genius docker file support ([de075e8](https://github.com/unit-mesh/auto-dev/commit/de075e8a40ac35f45142d8f7c7ec4207157be378))
* **sre:** init github actions support ([f7fcb86](https://github.com/unit-mesh/auto-dev/commit/f7fcb86316c1d5608f1d94409f04e53152621c37))
* **sre:** make github action works ([bfd91bb](https://github.com/unit-mesh/auto-dev/commit/bfd91bbf22dd771879df7999cd3654d8f7c19e35))
* **team:** add output to file support [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([094421f](https://github.com/unit-mesh/auto-dev/commit/094421f9c0d7b81fda54a8b2b4e7e4bb983d7974))



# [1.4.0](https://github.com/unit-mesh/auto-dev/compare/v1.3.0...v1.4.0) (2023-10-22)


### Bug Fixes

* add basic handle for error [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([3d0afcc](https://github.com/unit-mesh/auto-dev/commit/3d0afcc7fd12f21e97d5153e2ba83c80114ee46b))
* fix import ([37e1f7f](https://github.com/unit-mesh/auto-dev/commit/37e1f7f87cfebfb8fab6d07882bc008634a1ebd7))
* fix message issue for item ([1fc0b50](https://github.com/unit-mesh/auto-dev/commit/1fc0b50a9f86664ce222f803eddf9796baad6e8b))
* fix nou found issue ([a9c80fd](https://github.com/unit-mesh/auto-dev/commit/a9c80fd340145c30e993d262c8cbec45a06c744a))
* **team:** fix split crash [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([bdda28b](https://github.com/unit-mesh/auto-dev/commit/bdda28b1200e439f45103cca8524c984ef8c8b25))
* update code complete code ([99699a1](https://github.com/unit-mesh/auto-dev/commit/99699a13a87cab66737cf5d9d890502e1813f10a))


### Features

* add config for ramework context [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([09d4fe9](https://github.com/unit-mesh/auto-dev/commit/09d4fe9f0ac346a955f6fced84aed8e313e9618e))
* add more for related code [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([6aefbc5](https://github.com/unit-mesh/auto-dev/commit/6aefbc5ed583886ecd37e91437c50b9ea43d7359))
* add test finder to lookup tests [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([4c4e8ae](https://github.com/unit-mesh/auto-dev/commit/4c4e8ae79cb285f56c4901a44bd8e908e90b5bc1))
* create conttext for keep team prompt context ([ac50592](https://github.com/unit-mesh/auto-dev/commit/ac505929fdd4702c937e917807f68d28f76cc3dd))
* enable keep history parameters for fix issues [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([9200fac](https://github.com/unit-mesh/auto-dev/commit/9200fac46239664a71a8e62120ff03b90f0cacc1))
* enable to get tasking infos ([1e5b35b](https://github.com/unit-mesh/auto-dev/commit/1e5b35ba8cb3791b39d17aae5ee4fc801810f23a))
* init basic context ([dd047fa](https://github.com/unit-mesh/auto-dev/commit/dd047fa6852a5c423597c884c113d03a8b6f3a28))
* init basic insert for curost ([56dc114](https://github.com/unit-mesh/auto-dev/commit/56dc11428144e2f027a0c8ec9093830ce0d6a666))
* init basic workflow for chat with msg [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([2d7aa42](https://github.com/unit-mesh/auto-dev/commit/2d7aa422d493149000295edc5eb9ac63ae9aa1a2))
* init team prompt intentions [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([d7172ef](https://github.com/unit-mesh/auto-dev/commit/d7172ef2013ec1b58f617c0729460df91f1c755f))
* **review:** add code review to changesbrowser ([8adef80](https://github.com/unit-mesh/auto-dev/commit/8adef807f226c394773ba51fbd4eec39b4c1b758))
* **sre:** add basic tasks for design ([4559c2c](https://github.com/unit-mesh/auto-dev/commit/4559c2ce4484defb78b850529822a8320f467782))
* **sre:** add extension point build system provider ([789435e](https://github.com/unit-mesh/auto-dev/commit/789435ea86f74cbe7930692050ad670331d9c849))
* **sre:** add group actions for SRE ([72c0d99](https://github.com/unit-mesh/auto-dev/commit/72c0d9918ab36aee6f08cbeae313dcde810a0923))
* **sre:** init basic template loader for render items ([1c87cc9](https://github.com/unit-mesh/auto-dev/commit/1c87cc9c2b9706dea7c83c1207d9b61fdf40e6b2))
* **sre:** init builder for build system ([f452f94](https://github.com/unit-mesh/auto-dev/commit/f452f940a04010607ad9d71f196f29d20e7f2b90))
* **sre:** init java build system provider ([ce31c9e](https://github.com/unit-mesh/auto-dev/commit/ce31c9eddf640ae74d8ad2b993724a6de7353d0f))
* **sre:** init JavaScript build system provider ([2085c64](https://github.com/unit-mesh/auto-dev/commit/2085c648a5eaf9e81a2c098a624e0e467efa55e9))
* **sre:** init Kotlin build system provider ([7dd2b79](https://github.com/unit-mesh/auto-dev/commit/7dd2b79833da39d8d54250db49cf3f80b3529d35))
* **team:** add basic handler for prompt builder [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([5140049](https://github.com/unit-mesh/auto-dev/commit/51400494d32b653c2cfbdac33ef83a513d0353b6))
* **team:** add parser for action prompt [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([665d36d](https://github.com/unit-mesh/auto-dev/commit/665d36d5058fb49a786670e49dbce3f8a47b39a2))
* **team:** create team prompt intention action [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([71019c6](https://github.com/unit-mesh/auto-dev/commit/71019c61daba56dce68afb7fe73a0375bc1a2b50))
* **team:** init basic config frontmatter for prompt config [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([18f52c7](https://github.com/unit-mesh/auto-dev/commit/18f52c7e36dd8d1cb0c2826c918d1259424d9284))
* **team:** init prompt library for design ([55c8349](https://github.com/unit-mesh/auto-dev/commit/55c8349ce52f22d7513c9a935317427c8902294b))
* try to use appen stream [#49](https://github.com/unit-mesh/auto-dev/issues/49) ([1231a23](https://github.com/unit-mesh/auto-dev/commit/1231a23fd305a78251aefa5d95d8c9dbab0b8732))



# [1.3.0](https://github.com/unit-mesh/auto-dev/compare/v1.2.5...v1.3.0) (2023-10-18)


### Bug Fixes

* fix document erorr issues ([efedf72](https://github.com/unit-mesh/auto-dev/commit/efedf72c0798c15777a7f69cab5fe3cee907d3d5))
* **test:** fix instruction error issue ([f7c9e03](https://github.com/unit-mesh/auto-dev/commit/f7c9e0305d823dbca7e6e8d408d09dd3c5867c4d))
* **test:** fix lost current class for method testing ([7d81858](https://github.com/unit-mesh/auto-dev/commit/7d8185878e0c4d17aa99c15e09c80fbdb3b8af19))


### Features

* init commits for changes ([a6d1d82](https://github.com/unit-mesh/auto-dev/commit/a6d1d82d0ed3ed3f50999d80c94e122ba621c25a))
* **reivew:** init fetch github issue by story ids ([2b79429](https://github.com/unit-mesh/auto-dev/commit/2b79429b67bb311d05514c5035719e382fb4bd42))
* **review:** add chinese context to full context ([8bcd37b](https://github.com/unit-mesh/auto-dev/commit/8bcd37b0b970a6fe24e209b7b778365f071abfcc))
* **review:** init default file ignore for patterns ([48ee07f](https://github.com/unit-mesh/auto-dev/commit/48ee07f8da7040ea23460b6ce5953e65c034ef9b))
* **review:** make fetch github issue works ([285da68](https://github.com/unit-mesh/auto-dev/commit/285da68ef7afd5c24a20b3c47162cfd40364d3e7))



## [1.2.5](https://github.com/unit-mesh/auto-dev/compare/v1.2.3...v1.2.5) (2023-10-15)


### Bug Fixes

* change jsonpath to https://github.com/codeniko/JsonPathKt && fixed [#48](https://github.com/unit-mesh/auto-dev/issues/48) ([7973211](https://github.com/unit-mesh/auto-dev/commit/79732111cb903f3e62d8256de2d66a753bd3702a))
* fix inout empty issue ([13e0ab1](https://github.com/unit-mesh/auto-dev/commit/13e0ab11337c03f44b7d26074b0758cbd61fe2c8))
* fix prompt ([215e7d8](https://github.com/unit-mesh/auto-dev/commit/215e7d85d99c42f73e3c148882b3f2427194d952))


### Features

* **docs:** adjust prompt ([d12ea4a](https://github.com/unit-mesh/auto-dev/commit/d12ea4a71bfb9b53472268ae2238076063bd5f6c))
* **docs:** init kotlin living documentation ([abf3d74](https://github.com/unit-mesh/auto-dev/commit/abf3d743d4a06b4959e6eaba1ca1e505d582ec93))



## [1.2.3](https://github.com/unit-mesh/auto-dev/compare/v1.2.2...v1.2.3) (2023-10-09)


### Bug Fixes

* add fix for [#47](https://github.com/unit-mesh/auto-dev/issues/47) ([04f9c1b](https://github.com/unit-mesh/auto-dev/commit/04f9c1bd694336402c57c00208d5400e6dda5c59))
* fix custom prompt emoji issue ([13e7061](https://github.com/unit-mesh/auto-dev/commit/13e7061a54bab06b2b00c07e0a6d5c9119fd0f3c))



## [1.2.2](https://github.com/unit-mesh/auto-dev/compare/v1.2.1...v1.2.2) (2023-09-24)


### Bug Fixes

* [#40](https://github.com/unit-mesh/auto-dev/issues/40) ([010d142](https://github.com/unit-mesh/auto-dev/commit/010d142d630a23f02cdde153483467ad4dedf493))
* add sleep for debug mode, if machine is slowly ([06a6dbe](https://github.com/unit-mesh/auto-dev/commit/06a6dbe3358f6cc62e413d3e8f8b55dfd19bd55b))


### Features

* **chat:** add delay before rendering last message  The default delay is 20 seconds, but it can be customized. ([17e5585](https://github.com/unit-mesh/auto-dev/commit/17e5585612c7338ab4a17635a2ec1fa35949785e))
* **settings:** add quest delay seconds parameter ([f423caf](https://github.com/unit-mesh/auto-dev/commit/f423caf969f8a6490221d35b5449276549853e38)), closes [#21](https://github.com/unit-mesh/auto-dev/issues/21)



## [1.2.1](https://github.com/unit-mesh/auto-dev/compare/v1.2.0...v1.2.1) (2023-08-25)


### Bug Fixes

* fix load bundle issue ([8f7c245](https://github.com/unit-mesh/auto-dev/commit/8f7c2459daee25a5cfa08d6a53482de8b5d13cd2))



# [1.2.0](https://github.com/unit-mesh/auto-dev/compare/v1.1.4...v1.2.0) (2023-08-25)


### Bug Fixes

* [#36](https://github.com/unit-mesh/auto-dev/issues/36) 添加漏掉的 GitHub Token 选项 ([cfb6a60](https://github.com/unit-mesh/auto-dev/commit/cfb6a608d9bb620e99ba0b4783f712605e7f6e04))
* add clear message for [#35](https://github.com/unit-mesh/auto-dev/issues/35) ([cf60775](https://github.com/unit-mesh/auto-dev/commit/cf607755a4254052461ff0dbf8da5125ebee0fbb))
* change current selected engine not apply(until reopen project) ([01ea78a](https://github.com/unit-mesh/auto-dev/commit/01ea78a031585b24a49f76048f00a1047fdfb7bc))
* **counit:** fix syntax error issue ([f532d1e](https://github.com/unit-mesh/auto-dev/commit/f532d1e6d8fdac56461fe5fac9f1e2d5005eee22))
* Custom LLM engin server not work when change(until reopen project) ([dd488de](https://github.com/unit-mesh/auto-dev/commit/dd488de3e7a1c450c9bc2876f948f9242ed3187e))
* fix ci ([9b8859b](https://github.com/unit-mesh/auto-dev/commit/9b8859b02e8b394db8f2ea4d6f52ef4262e70acc))
* fix lost user information issue ([2bfc82a](https://github.com/unit-mesh/auto-dev/commit/2bfc82ab80616c1aec617762e0569713cec33d01))
* fix role issues ([fe75364](https://github.com/unit-mesh/auto-dev/commit/fe753645bc5e44a90101e5ba914267cb82f4b3f7))
* fix url path issue ([b4c2001](https://github.com/unit-mesh/auto-dev/commit/b4c2001b0434f99b627eb1f8b8e13ed1944f1c55))
* if already used Custom Engine config will not work after changed until reopen project. close [#31](https://github.com/unit-mesh/auto-dev/issues/31) ([624b0bc](https://github.com/unit-mesh/auto-dev/commit/624b0bcfe156012dfb0f26e885c202e0f715da8d))
* 添加漏掉的 custome request format 选项 ([abfd500](https://github.com/unit-mesh/auto-dev/commit/abfd500b64700fc7c141fe9b6749170c30631c67))


### Features

* add clion context   as example ([4489358](https://github.com/unit-mesh/auto-dev/commit/4489358a36522a8d2842a5f2a0015f21abac200b))
* **counit:** align to new APIs datastructure ([1150f54](https://github.com/unit-mesh/auto-dev/commit/1150f54f5ba8cb422aecc36867504400870d0942))
* **counit:** fix format ([a010536](https://github.com/unit-mesh/auto-dev/commit/a0105368d5e9c81cc1968210db78763aee191317))
* **counit:** init basic apis ([e78f864](https://github.com/unit-mesh/auto-dev/commit/e78f864afdc3a5c4e6869d2bc8109fb059cc7247))
* **counit:** init basic prompter ([1a30643](https://github.com/unit-mesh/auto-dev/commit/1a306437275b12c81cd1bc783ec75b0485fbc25d))
* **counit:** init counit setting service ([51bc5fb](https://github.com/unit-mesh/auto-dev/commit/51bc5fb8f74ca873cbe141e55ffaaa22b3fce885))
* **counit:** init for tool panel ([b2215ba](https://github.com/unit-mesh/auto-dev/commit/b2215ba8928d8f83e9d2fa32579eb3e7db00ab49))
* **counit:** make basic work flows ([21c25c7](https://github.com/unit-mesh/auto-dev/commit/21c25c774647eec638a3571de43553da574f9614))
* **document:** revert for json path language because different IDE product issue ([acae8ea](https://github.com/unit-mesh/auto-dev/commit/acae8eadebf335a18a213b7fefd9d6290f718921))
* init cpp module for ruipeng ([f74f094](https://github.com/unit-mesh/auto-dev/commit/f74f0949f3f12aae6dabbeacde86933c8be66987))
* **kanban:** add GitLabIssue implementation ([ab0d91d](https://github.com/unit-mesh/auto-dev/commit/ab0d91d1d7084b3f90f3bad56451d6e4af4eaec6))
* **rust:** init context ([4e7cd19](https://github.com/unit-mesh/auto-dev/commit/4e7cd19ba30f98ecf2649f844e0bcd4e4e085b00))
* **settings:** add GitLab options ([36548b5](https://github.com/unit-mesh/auto-dev/commit/36548b5975710870ba661f44eb67d975410c1991))
* **settings:** add new LLMParam components ([946f5a0](https://github.com/unit-mesh/auto-dev/commit/946f5a083b8a1fde8ac5f39f1ef76a87634d58b1))
* **settings:** add new LLMParam components ([dac557b](https://github.com/unit-mesh/auto-dev/commit/dac557be7ee264907da6a85c9836df008b58a95a))
* **settings:** add XingHuo provider configuration close [#29](https://github.com/unit-mesh/auto-dev/issues/29) ([cca35c6](https://github.com/unit-mesh/auto-dev/commit/cca35c6d8136dc7d4a81edf96e3a38692dbc6746))



## [1.1.4](https://github.com/unit-mesh/auto-dev/compare/v1.1.3...v1.1.4) (2023-08-18)


### Features

* add json path for config ([46efe24](https://github.com/unit-mesh/auto-dev/commit/46efe24a63009dde13142ba941463efa11239293))



## [1.1.3](https://github.com/unit-mesh/auto-dev/compare/v1.1.2...v1.1.3) (2023-08-18)


### Bug Fixes

* add lost for match string ([bf0e1a4](https://github.com/unit-mesh/auto-dev/commit/bf0e1a446ffc94460ec99d9f0d93364a376b507f))
* align cutom LLM server api to OpenAI API ([a718ed1](https://github.com/unit-mesh/auto-dev/commit/a718ed1944faca6554db7b2234d2e95572a6b9dd))
* AutoCrud Action only available when editor has selection ([cf49bd1](https://github.com/unit-mesh/auto-dev/commit/cf49bd1d86f7d56bf714ef5a099a684f10695aa5))
* change default json path lib to Kotlin version && done [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([4d5e573](https://github.com/unit-mesh/auto-dev/commit/4d5e5739562de032997a1ef82f4a9752aad61ccc))
* **doc:** fix format issues ([6e9c1a5](https://github.com/unit-mesh/auto-dev/commit/6e9c1a50746e37700315368c723a97a4b7e9a583))
* fix no returns ([87d688f](https://github.com/unit-mesh/auto-dev/commit/87d688f144af0465bcb9f95bf99a8e6e5936eed6))
* fix typos ([23effbd](https://github.com/unit-mesh/auto-dev/commit/23effbd0337c19a0fa712e252f6ad0778cdadc04))
* openai setting change not affect in same project lifecycle (will affect after reopen project) ([eb74909](https://github.com/unit-mesh/auto-dev/commit/eb74909f08d89d42fbdb10d896df8f8201ea1f3b))
* remove correnct line in duplicated ([6758260](https://github.com/unit-mesh/auto-dev/commit/67582604967a60558af94b8f6ab36ef434a9b48d))
* **similar:** fix lost chunk issues ([e003da9](https://github.com/unit-mesh/auto-dev/commit/e003da9ea192d18ebb99383b37d960c0bea5e1b9))


### Features

* add basic ins cancel logic for code complete ([018a82e](https://github.com/unit-mesh/auto-dev/commit/018a82e7a012cb71ef00db4ef6d437e6106e2823))
* add json path lib as design for [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([2c85f40](https://github.com/unit-mesh/auto-dev/commit/2c85f40b93ff93f9bff56451763646986e22f5d9))
* add lost docs adn closed [#27](https://github.com/unit-mesh/auto-dev/issues/27) ([daa43c1](https://github.com/unit-mesh/auto-dev/commit/daa43c18af93f1b40dd3fd0e67e51429df9a5308))
* add tips for Autocrud mode && closed [#28](https://github.com/unit-mesh/auto-dev/issues/28) ([bc837ca](https://github.com/unit-mesh/auto-dev/commit/bc837ca54352ca95b2cf9b904efbb6e2b7cac9e2))
* **custom:** fix new line issues ([6da2821](https://github.com/unit-mesh/auto-dev/commit/6da282106be8b862514c8fae846c258dc14d264d))
* **doc:** add result as context ([c8ebf40](https://github.com/unit-mesh/auto-dev/commit/c8ebf40dbc9d1954e6e9b8a83672ee3332e79d01))
* **docs:** add living documentation example ([2f9de5d](https://github.com/unit-mesh/auto-dev/commit/2f9de5d38c88a65457239fccc3d91645ba28be8b))
* **docs:** add living documentation examples ([1b8c42f](https://github.com/unit-mesh/auto-dev/commit/1b8c42f18ade8907497ae9ef80feb07049566466))
* init basic doc for [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([1e2199c](https://github.com/unit-mesh/auto-dev/commit/1e2199c13746424ebdae38b9a41ce31090e9989a))
* init custom doc config ([bcee2a5](https://github.com/unit-mesh/auto-dev/commit/bcee2a5210f050893b0e03b49d0adcbe206bdb87))
* init ui config for custom json format [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([b249f72](https://github.com/unit-mesh/auto-dev/commit/b249f72987e11dec62fa8c582ba699c442fd8584))
* **similar:** fix query size issue ([220eb29](https://github.com/unit-mesh/auto-dev/commit/220eb296e2b55adecec11af950b527cfccd6c96d))
* try to add json path && but server crash :crying: [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([88cbb9d](https://github.com/unit-mesh/auto-dev/commit/88cbb9d68865db420ca03dbfd131650529b78801))
* try to add prompot schema ([1aff07e](https://github.com/unit-mesh/auto-dev/commit/1aff07e4e17004815cc80f6ccaa48e623942ed6e))
* update for app settings ui [#25](https://github.com/unit-mesh/auto-dev/issues/25) ([5cef076](https://github.com/unit-mesh/auto-dev/commit/5cef07684a7f5c0ffa7f5b8fc22a66fd0cb27d06))



## [1.1.2](https://github.com/unit-mesh/auto-dev/compare/v1.1.1...v1.1.2) (2023-08-09)


### Bug Fixes

* fix compile issues ([7e2c936](https://github.com/unit-mesh/auto-dev/commit/7e2c936b0db7a475a82242f95629ed3bdbc9fccd))
* fix crash ([24c3cd7](https://github.com/unit-mesh/auto-dev/commit/24c3cd7a0ee9cae68044571473cb6a0778a93889))
* fix permissions ([f85a28d](https://github.com/unit-mesh/auto-dev/commit/f85a28d59d5edefbceba571429227856af4d91b3))
* 修复编译失败 ([ccc539b](https://github.com/unit-mesh/auto-dev/commit/ccc539bb0adba83a72b21e4664a81a255f94eaae))


### Features

* **doc:** add basdic prompt ([4fddd6d](https://github.com/unit-mesh/auto-dev/commit/4fddd6d28777fe973dc2c64f26a3e088feec7ea7))
* **doc:** init basic intention ([3c050e0](https://github.com/unit-mesh/auto-dev/commit/3c050e091dd177f582eddd0e9548db60efd02400))
* **doc:** init java living doc ([9a37dce](https://github.com/unit-mesh/auto-dev/commit/9a37dce7135fa118109b232ba847f9067826e3db))
* **doc:** make it works ([4d65801](https://github.com/unit-mesh/auto-dev/commit/4d658017d68da4215f536add4056a7f7703b5b5c))



## [1.1.1](https://github.com/unit-mesh/auto-dev/compare/v1.1.0...v1.1.1) (2023-08-08)


### Features

* add themes ([0e0b571](https://github.com/unit-mesh/auto-dev/commit/0e0b5716be02be63cad96d0230bcd81530984e8c))
* **custom:** add class input and output ([ae0db27](https://github.com/unit-mesh/auto-dev/commit/ae0db274afd67b4848db722058911a2ad526fb02))
* **custom:** add priority ([57afece](https://github.com/unit-mesh/auto-dev/commit/57afece0ee5e3693eb74a898e8e03d821dc70ef9))
* **custom:** split custom actions ([5c0f42b](https://github.com/unit-mesh/auto-dev/commit/5c0f42bff0ea1077fc9eb57b689d1826feb846b9))



# [1.1.0](https://github.com/unit-mesh/auto-dev/compare/v1.0.2...v1.1.0) (2023-08-08)


### Bug Fixes

* **azure:** fix crash issues ([8d2581d](https://github.com/unit-mesh/auto-dev/commit/8d2581d377381e075cffa8546456f4c96a6f956f))
* disable for veww ([8bb2063](https://github.com/unit-mesh/auto-dev/commit/8bb20630ad5c8fa266a37bf56e20e5ddbb8f5f4e))
* disable for welcome board ([7ab469b](https://github.com/unit-mesh/auto-dev/commit/7ab469b0c617adcffceb329c75ff8b9105687024))
* fix exception issues ([2fa1976](https://github.com/unit-mesh/auto-dev/commit/2fa19766cabeca08f087c485c58ea886f4a83e49))
* fix format issues ([f83b70f](https://github.com/unit-mesh/auto-dev/commit/f83b70fc3fc7c9771dfd022860b3f5cb858c28dc))
* fix lost element issue ([3dd043e](https://github.com/unit-mesh/auto-dev/commit/3dd043e334db3e6595fef97a5fad12ffdb953ee2))
* fix null issue [#19](https://github.com/unit-mesh/auto-dev/issues/19) [#14](https://github.com/unit-mesh/auto-dev/issues/14) ([1fe63fe](https://github.com/unit-mesh/auto-dev/commit/1fe63fe94364e9c434ad661bf6e228174adba7cf))
* fix tests ([ee67cde](https://github.com/unit-mesh/auto-dev/commit/ee67cde53356947d5c46e4e6833db8a5043ebd2e))
* fix width issue ([b94ccc0](https://github.com/unit-mesh/auto-dev/commit/b94ccc0ca443737d54fe31b5b1bcb389d1c1e02a))
* remove dpilicate code ([1a89ed0](https://github.com/unit-mesh/auto-dev/commit/1a89ed03fb4dd22678f25f6f3c66122de1204f66))
* settings not change but show modified ([e3e327d](https://github.com/unit-mesh/auto-dev/commit/e3e327d6b92008de73312106ae2cefdd769aa698))
* try add csharp framework context ([d2e7bfe](https://github.com/unit-mesh/auto-dev/commit/d2e7bfe7423a6824903b5690e591458c5c878563))


### Features

* add csharp mod ([0a49b52](https://github.com/unit-mesh/auto-dev/commit/0a49b52a1fe6a5c1bc6d0a6a8e40c23d1e036014))
* add csharp module ([e5bad15](https://github.com/unit-mesh/auto-dev/commit/e5bad154d612a18f1372de15ca4574d40b75acd8))
* add custom action to prompt ([d3ce0ed](https://github.com/unit-mesh/auto-dev/commit/d3ce0ed8bfef4d7498ac22e684094e26e41e6647))
* add match rule of regex ([58dca56](https://github.com/unit-mesh/auto-dev/commit/58dca56246b151811a2aea422e61285ef15e7134))
* add run rider ([4968c33](https://github.com/unit-mesh/auto-dev/commit/4968c3389aeebd41a8251c343c2f5f1f40654f97))
* add token length config to [#19](https://github.com/unit-mesh/auto-dev/issues/19) ([ba83c80](https://github.com/unit-mesh/auto-dev/commit/ba83c8010a27fbd486e941b1b55907d196581ca3))
* **biz:** make explain biz works ([6068a35](https://github.com/unit-mesh/auto-dev/commit/6068a357416f637c7934b2298ffd9b1322e6e7fd))
* **biz:** reorg code ([e72683b](https://github.com/unit-mesh/auto-dev/commit/e72683b58711d0261f8a5263d99188e8f03499bd))
* **biz:** update for context ([3f5b10a](https://github.com/unit-mesh/auto-dev/commit/3f5b10a4f0888b869821f478b28daa48abb7af3d))
* **co:** add basic api parse ([75e095d](https://github.com/unit-mesh/auto-dev/commit/75e095d271bd73fa9d759f5d5db37cce0e2ee242))
* **co:** add chapi for scan scanner ([cdeff14](https://github.com/unit-mesh/auto-dev/commit/cdeff14eb40a470f9629c14bc2c2d6c492af1b7f))
* **co:** init basic actions ([d9b0d08](https://github.com/unit-mesh/auto-dev/commit/d9b0d082441ec8f70dde3c75a66b434cf7cbf487))
* **co:** split to new module for plugin size ([c180711](https://github.com/unit-mesh/auto-dev/commit/c18071142f33bc6ccde02fc94632f28920fbe2a6))
* **custom:** add basic custom prompts to intentions ([9af81d0](https://github.com/unit-mesh/auto-dev/commit/9af81d03e0a6550ec85c4281f8964efb6160799f))
* **custom:** add basic variable ([9d7aef0](https://github.com/unit-mesh/auto-dev/commit/9d7aef08e68e1419c5d53c19e6ff42cf0defb5d8))
* **custom:** add intention prompt ([6791739](https://github.com/unit-mesh/auto-dev/commit/6791739185306b5be08922533c83b43aacf211fd))
* **custom:** enable for custom by varible ([a626347](https://github.com/unit-mesh/auto-dev/commit/a62634770ebe881369c28ee91ec4292255b25b83))
* **custom:** make custom prompt works ([71393df](https://github.com/unit-mesh/auto-dev/commit/71393df6c94111f95dd64392737931c1829954e4))
* make api works ([3bd2387](https://github.com/unit-mesh/auto-dev/commit/3bd2387d3824889d21dafe07c1afc2554063acc3))
* make azure works with stream ([962b599](https://github.com/unit-mesh/auto-dev/commit/962b599e91e078d866398a0ebe487b64d862a948))
* make custom server works with stream ([a7a9c1c](https://github.com/unit-mesh/auto-dev/commit/a7a9c1c418684105f8bbd198ac4b07fee346a67c))
* **python:** add to add collect ([b67933f](https://github.com/unit-mesh/auto-dev/commit/b67933f81f24b7dd082a4e33f37589f5c37d2b56))
* try with new api ([6a94505](https://github.com/unit-mesh/auto-dev/commit/6a945052ed89c43bc0ca337cf29467db3d7d1b8e))



## [1.0.2](https://github.com/unit-mesh/auto-dev/compare/v1.0.2-beta.1...v1.0.2) (2023-08-07)


### Bug Fixes

* clen deps ([0625702](https://github.com/unit-mesh/auto-dev/commit/062570212a849c292aff0c72d3d2972d023fdc88))
* deplay get display text message time ([46a24e8](https://github.com/unit-mesh/auto-dev/commit/46a24e8ae4b16636937e8917c3c43ab8f29ef1da))
* fix a read issue before other ([5142ec2](https://github.com/unit-mesh/auto-dev/commit/5142ec2cb2ca3ed562baa122ec1b930ddd1d4081))
* fix change sessions ([3ce2a57](https://github.com/unit-mesh/auto-dev/commit/3ce2a575b2a5c01b99808e44038e8cf358aa7ca2))
* fix examples for commits ([469624a](https://github.com/unit-mesh/auto-dev/commit/469624af173055b25a2ad079d80d91f68b5e5038))
* fix format ([0e61d89](https://github.com/unit-mesh/auto-dev/commit/0e61d8998175576fc0b21dd640bad90062bbbff6))
* fix internal api issues ([16449c2](https://github.com/unit-mesh/auto-dev/commit/16449c290cbd4b591ec0c766937be0ad7f663297))
* fix processor issue ([a8fc892](https://github.com/unit-mesh/auto-dev/commit/a8fc892daf26bef9d22ee609ea1be829410d613c))
* fix read issues ([a318041](https://github.com/unit-mesh/auto-dev/commit/a31804159bd88999294ab3d9a1c5712e95c23f9e))
* fix request prompt issue ([08036ac](https://github.com/unit-mesh/auto-dev/commit/08036ac7396c88672a3d672848ace10033b59f2e))
* fix simliar chunk issue ([6b79923](https://github.com/unit-mesh/auto-dev/commit/6b799231dbbaf3fd0a28d8c2dafce775c1478328))
* fix tests ([de6bb26](https://github.com/unit-mesh/auto-dev/commit/de6bb26c845fa26249bf72c2ab7257e603d90ce9))
* fix typos ([109db5e](https://github.com/unit-mesh/auto-dev/commit/109db5e5573c9fd6b45fc2e31c94c293399e10a4))
* try to resolve index issues ([25b2275](https://github.com/unit-mesh/auto-dev/commit/25b2275a4c0d3a7099aafd35aeb24dd15f03664a))


### Features

* add basic for context ([5e6af8e](https://github.com/unit-mesh/auto-dev/commit/5e6af8ee5974c27244c2ee70d8e63b669daa2b9d))
* add basic rust for template ([d892646](https://github.com/unit-mesh/auto-dev/commit/d8926462594b8c66ed8c848d59cb9c4a109eb50f))
* **biz:** add chat with biz ([6723920](https://github.com/unit-mesh/auto-dev/commit/6723920fcb9cc5adb724a6d429efd0ed53c54abd))
* **clion:** try with oc ([4593cb8](https://github.com/unit-mesh/auto-dev/commit/4593cb88b2fa1467b55620438c17fa0b84763c13))
* make clion runnable ([71df852](https://github.com/unit-mesh/auto-dev/commit/71df852c80844a3b3df09521c26c3f371bb5de7c))
* **rust:** try to talk with biz in biz ([d138da7](https://github.com/unit-mesh/auto-dev/commit/d138da73d7b2e21fdfbf08466031e27b1404865e))
* try to use refs ([ce3c671](https://github.com/unit-mesh/auto-dev/commit/ce3c67186ce50f847e968455d14e4ec4f3bcea70))
* update for explain biz logic ([95049b5](https://github.com/unit-mesh/auto-dev/commit/95049b5ebe4b71093e6256fdaa65efe266a54900))



## [1.0.2-beta.1](https://github.com/unit-mesh/auto-dev/compare/1.0.2-beta.1...v1.0.2-beta.1) (2023-08-05)


### Bug Fixes

* fix issues of internal API ([d3e56ab](https://github.com/unit-mesh/auto-dev/commit/d3e56ab7d9720281938a06a0f1178d19f3427dc1))
* **ui:** fix genearete code waring ([6955882](https://github.com/unit-mesh/auto-dev/commit/6955882b49b85f7201ed7ab9a829c2dc9a9a1fd0))
* **ui:** fix magic number ([8554153](https://github.com/unit-mesh/auto-dev/commit/855415349f488b23d2cfc3c221c566a9e832e9fe))


### Features

* add welcome page ([3dc5c19](https://github.com/unit-mesh/auto-dev/commit/3dc5c19686d6958be977203af9a6f399c1e7676c))
* init for chat messages ([fd139df](https://github.com/unit-mesh/auto-dev/commit/fd139df66c88f3698d705f7f8121f78ed48b318d))
* make genearte code here ([3bce491](https://github.com/unit-mesh/auto-dev/commit/3bce4914198e7cc942f8519fed761523356003e7))
* **ui:** thinking in generate in placE ([ec4563d](https://github.com/unit-mesh/auto-dev/commit/ec4563dc7fe601eadc20c9f5a97a2d2d26cdff49))



## [1.0.1](https://github.com/unit-mesh/auto-dev/compare/v1.0.0...v1.0.1) (2023-08-05)


### Bug Fixes

* fix inaly issues ([990f0fa](https://github.com/unit-mesh/auto-dev/commit/990f0fa1cbc97969fded1ff270bdf24937657aa6))
* fix not file issues ([483d647](https://github.com/unit-mesh/auto-dev/commit/483d64794b20e76d2515b7cedbbe4975618fb576))


### Features

* add release scripts ([11edbd9](https://github.com/unit-mesh/auto-dev/commit/11edbd96ea379aafdc2804da818de573daeb7857))
* **ui:** add first version for new chat ([0acbc27](https://github.com/unit-mesh/auto-dev/commit/0acbc277ebc819151759f1106e15f8a16bbfb9e8))
* **ui:** add new chat ([310d3e1](https://github.com/unit-mesh/auto-dev/commit/310d3e1139d89a7541d144bdb4334451610136a9))
* **ui:** change to button style ([178853e](https://github.com/unit-mesh/auto-dev/commit/178853eae57386088151e15b187e0c3290b43b8f))



# [1.0.0](https://github.com/unit-mesh/auto-dev/compare/v0.7.3...v1.0.0) (2023-08-04)


### Bug Fixes

* add lost group id ([99c55c7](https://github.com/unit-mesh/auto-dev/commit/99c55c7e94ec0811ebdf63e966cfbf5ee2778e03))
* fix align issues ([580be9d](https://github.com/unit-mesh/auto-dev/commit/580be9d9cb5bbb20dc64c582ddd99e52429a2ec3))
* fix code complete issue ([d4764d2](https://github.com/unit-mesh/auto-dev/commit/d4764d2146ee5710c924bf2193008ef8cf83d53f))
* fix ddl drop issue ([37809f3](https://github.com/unit-mesh/auto-dev/commit/37809f33c2e3b4549d3db0ae18d275b307431922))
* fix dispose issues ([e03ebc7](https://github.com/unit-mesh/auto-dev/commit/e03ebc7432db8386729733020ba76305fa604622))
* fix document length issues ([2a0a65b](https://github.com/unit-mesh/auto-dev/commit/2a0a65bcfe43d88bf5bf330d168540b433274f61))
* fix dumb issues ([5d19ce6](https://github.com/unit-mesh/auto-dev/commit/5d19ce6ffdf7e68c22a4f03b72f7f98e107713b6))
* fix error level issues ([02f905b](https://github.com/unit-mesh/auto-dev/commit/02f905b445356fbc6b6c2ec66b6b60a1f5cf499e))
* fix file lost contexT ([9970bdc](https://github.com/unit-mesh/auto-dev/commit/9970bdcfdb93ebc4e43a3bd7126efa5ca2373456))
* fix java code issues ([042d078](https://github.com/unit-mesh/auto-dev/commit/042d07860241af64c222641d9fa340efe1ad5282))
* fix mvc issues ([8c9fec0](https://github.com/unit-mesh/auto-dev/commit/8c9fec0d9fcdf48ba29dbc54b718bae4d8a43c7e))
* fix naming issue ([8bff8fc](https://github.com/unit-mesh/auto-dev/commit/8bff8fc686eef747907882a0e666beb24476c377))
* fix new line issues ([ea74c90](https://github.com/unit-mesh/auto-dev/commit/ea74c90de1a8bc5ce826fe2662f7ae40f332d9df))
* fix no project issues ([073e279](https://github.com/unit-mesh/auto-dev/commit/073e2794c6407c20ecefc6a5b0b1554b59670769))
* fix prompt issues ([dda3586](https://github.com/unit-mesh/auto-dev/commit/dda3586415fd35143089abb5088ee9b3de367b58))
* fix renam issues ([aaa097d](https://github.com/unit-mesh/auto-dev/commit/aaa097d376dfff2ed9f9a00c339a7ba476d6b51a))
* fix runtime issues ([e318210](https://github.com/unit-mesh/auto-dev/commit/e318210658dc31cfe3045082cc75e0dfae361ccb))
* fix setContent not input issues ([efc14c1](https://github.com/unit-mesh/auto-dev/commit/efc14c10ad4a498a3576937aff3d0de82b00ccab))
* fix sometimes offsets issues ([90e4055](https://github.com/unit-mesh/auto-dev/commit/90e4055a20e3e4e8c545a8acbc72b082e87b635a))
* fix tests ([48e1f26](https://github.com/unit-mesh/auto-dev/commit/48e1f26f979ec063fad1074257e21ab45c86988c))
* fix tests ([f6c13c6](https://github.com/unit-mesh/auto-dev/commit/f6c13c61559d18b9a255cc82736228906eb63bac))
* fix tests ([a75481a](https://github.com/unit-mesh/auto-dev/commit/a75481acf05e626a049a775594487eed5a72f616))
* fix typos ([f64f748](https://github.com/unit-mesh/auto-dev/commit/f64f748a0aeb1994fba0e3489adaaade1fc60b14))
* fix typos ([cc8cf15](https://github.com/unit-mesh/auto-dev/commit/cc8cf15d4d1045f9892f668e7b95451b8eab6351))
* fix typos ([b974965](https://github.com/unit-mesh/auto-dev/commit/b97496504bf02f0d5d5fc96b740316ecb681fa4b))
* fix typos ([0b2d4e1](https://github.com/unit-mesh/auto-dev/commit/0b2d4e1e589fd580e443c685419a11828e0dc20f))
* fix typos ([3cfcc0a](https://github.com/unit-mesh/auto-dev/commit/3cfcc0afbfd1c400661fe59764fdb52fac263bd7))
* fix typos ([6320e1a](https://github.com/unit-mesh/auto-dev/commit/6320e1a3810ee655fcf4a268dfaed9615a29e8f4))
* fix typos ([d6a9522](https://github.com/unit-mesh/auto-dev/commit/d6a9522555a6924d976bb92a3420d11803593b53))
* fix uppercase issues ([5ae6f7b](https://github.com/unit-mesh/auto-dev/commit/5ae6f7beff71d4629443c1344b454c07734c7608))
* fix warning ([3d20f9f](https://github.com/unit-mesh/auto-dev/commit/3d20f9f9e4800e04cc1948eaec23a701c68fe0db))
* fix when controllers empty ([6baa6b8](https://github.com/unit-mesh/auto-dev/commit/6baa6b8d3c3f09a951c91f9df9a8be2ad86ee37e))
* **go:** disable version for temp ([cd1a46c](https://github.com/unit-mesh/auto-dev/commit/cd1a46c11cbb089a52b7e5d3a1cd7618555eb6e8))
* **js:** fix context issues ([ae19ff4](https://github.com/unit-mesh/auto-dev/commit/ae19ff4ae391ea3139ba60e97ed25e6a261fefcc))
* **js:** fix lost tsx syntax error issue ([e42bc96](https://github.com/unit-mesh/auto-dev/commit/e42bc96b1c5d466ac9e5eb25c79f8eaae07a3350))
* **js:** fix test framework ([58f4ef4](https://github.com/unit-mesh/auto-dev/commit/58f4ef4a70b2ba6decf52415b45f62557b2aecb4))
* **test:** fix for root element issue ([f85b9b3](https://github.com/unit-mesh/auto-dev/commit/f85b9b360148d0fca31d028b0fda018766910bd2))
* **test:** make it pass ([8e2dd9d](https://github.com/unit-mesh/auto-dev/commit/8e2dd9d699923039e1889014540db1911f0a55ce))
* try to fix for auto crud issues ([22828b6](https://github.com/unit-mesh/auto-dev/commit/22828b63b91867e4996b2f0dfa90b4a835fb8d34))
* try to reolsve psi class ref type ([2112b91](https://github.com/unit-mesh/auto-dev/commit/2112b9146ce5c4134c36ac4087b5c06aa52169d5))
* try to resolve editor issues ([76b698c](https://github.com/unit-mesh/auto-dev/commit/76b698c8edc5263a6a12aa57c2402aa6853efe8e))
* try to resolve for file ([8cc1c7f](https://github.com/unit-mesh/auto-dev/commit/8cc1c7fd833258b1c09c50d2e87e2c550d3cb67f))
* update for deps ([ed73604](https://github.com/unit-mesh/auto-dev/commit/ed73604eb33912d21afb15ddb26c3fba10b6a97d))


### Features

* add basic handle for cache ([aee8504](https://github.com/unit-mesh/auto-dev/commit/aee8504d921c3a2ccd626077e79aa128a0b6dcee))
* add chat with this ([ed44a9d](https://github.com/unit-mesh/auto-dev/commit/ed44a9d9262a41a32d0c0d6d4941b659c221918c))
* add context creator ([ccf3939](https://github.com/unit-mesh/auto-dev/commit/ccf39398f7f340c8252f0c9500cf7a7c9ccb3f68))
* add custom llm server example ([8d4373a](https://github.com/unit-mesh/auto-dev/commit/8d4373a956af21ed4a758dce29b892b7771c060f))
* add custom server example ([7766320](https://github.com/unit-mesh/auto-dev/commit/7766320c1f0048ab2140c8d377ca18fc0dbac761))
* add custom server to server ([f175368](https://github.com/unit-mesh/auto-dev/commit/f1753686dc52fccef99834440cc674a3809dcb03))
* add for context ([1af9ccb](https://github.com/unit-mesh/auto-dev/commit/1af9ccb787d091b58214e0dd89ac71331c296a56))
* add for mock provider ([c355211](https://github.com/unit-mesh/auto-dev/commit/c35521154e5e64b55b404815cc203f6d9d1653fe))
* add for more contents ([2819b8a](https://github.com/unit-mesh/auto-dev/commit/2819b8a8641ff05425ee91f7b0612cc38f72ba64))
* add for new code block ([4f9f19d](https://github.com/unit-mesh/auto-dev/commit/4f9f19dbce0f41361e9b9224d2cb042c09181e31))
* add for write test service ([993ec10](https://github.com/unit-mesh/auto-dev/commit/993ec108e60c19a769f92d036b8ab507694248dc))
* add get for explain element ([0363e8c](https://github.com/unit-mesh/auto-dev/commit/0363e8c4722830bdf1c1a2765e9bebf9029f5163))
* add good ui with sender ([4874767](https://github.com/unit-mesh/auto-dev/commit/48747676db8ddcc608ed32ab359407f424d91f76))
* add group for intetion ([a819cfa](https://github.com/unit-mesh/auto-dev/commit/a819cfa2ed7f07db37874883f637b657e1e74233))
* add language toolbar ([e1b5f8d](https://github.com/unit-mesh/auto-dev/commit/e1b5f8da5e57ffd365d33ea3557052f40528e83f))
* add new chat with selectiongst ([164f848](https://github.com/unit-mesh/auto-dev/commit/164f8480cf95126b7ad20e692b8d7cbef2163b34))
* add run after finish testing ([9ac7890](https://github.com/unit-mesh/auto-dev/commit/9ac78908859a4eba5f28ca34c08c29022a22b79a))
* add simple rule for writting test ([5f38256](https://github.com/unit-mesh/auto-dev/commit/5f38256b088952237d5257be8dad37e49eea9377))
* add suffix for items ([fa05a0e](https://github.com/unit-mesh/auto-dev/commit/fa05a0efc93e4f4331208fa18a65ce62adaeaf2b))
* align for input ui ([7543b15](https://github.com/unit-mesh/auto-dev/commit/7543b1552158611dc512feb4dd43fadfb044e44f))
* **context:** try for new provder ([b87af78](https://github.com/unit-mesh/auto-dev/commit/b87af785efac6058ca27c3499ac36f616331dd2a))
* **context:** update for context ([0182c29](https://github.com/unit-mesh/auto-dev/commit/0182c29a17138fcc17d384e0aeaa751cabd70fa2))
* enable autocrud for [#13](https://github.com/unit-mesh/auto-dev/issues/13) ([eb38aaa](https://github.com/unit-mesh/auto-dev/commit/eb38aaa39e6be6939faa1c1723b3db4112daf55f))
* enable context for full classes ([62963e1](https://github.com/unit-mesh/auto-dev/commit/62963e11b3bad6d5d171f957b5cafb17c5226e8f))
* fix context mpety issues ([2fd0695](https://github.com/unit-mesh/auto-dev/commit/2fd06954ac5801994cb051e46d8d6dc1b142750f))
* fix duplicate context issues ([a951ed6](https://github.com/unit-mesh/auto-dev/commit/a951ed6400dfc3a2f7def781bc67dd3f63883b70))
* fix duplicate warning ([85c9d1a](https://github.com/unit-mesh/auto-dev/commit/85c9d1a3ccb877421b4fd132520a267d89f33262))
* init basic js context provider ([5164da0](https://github.com/unit-mesh/auto-dev/commit/5164da09b9906ad21a7814a0f1c6f031da763cbc))
* init basic transform logic ([8262f9b](https://github.com/unit-mesh/auto-dev/commit/8262f9bbc702718c26e2c473230f5b40af72ada2))
* init for package json reader ([a1b5c45](https://github.com/unit-mesh/auto-dev/commit/a1b5c45cd3045e425dd4c6ed24a4485c11472c8a))
* init frameworks informationg ([d268842](https://github.com/unit-mesh/auto-dev/commit/d26884208f27e6a73cbf00bffeb7a156aec03225))
* init goland mods ([880d12d](https://github.com/unit-mesh/auto-dev/commit/880d12d5660c1e1e4b0ae5a1d29e7fc1576f17ce))
* **java:** inline duplicate code ([77232a2](https://github.com/unit-mesh/auto-dev/commit/77232a283101c907ba44afc2c9059c933b3ec59f))
* **javascript:** fix roor package.json error issue ([1d5553a](https://github.com/unit-mesh/auto-dev/commit/1d5553a2228d08cb3c02225ed8cb03b669a8d015))
* **js:** align context builder ([c9ea1d1](https://github.com/unit-mesh/auto-dev/commit/c9ea1d17f539f6c422b4ca015feccc6f79039d7f))
* **js:** disable for insert class ([6fceaf1](https://github.com/unit-mesh/auto-dev/commit/6fceaf18662401e203983eebe048f131fb2d1905))
* **js:** init snapshot util ([447e6d7](https://github.com/unit-mesh/auto-dev/commit/447e6d768e3455f1667a4e99a7f9d5cd749046db))
* **js:** init test service container ([750baff](https://github.com/unit-mesh/auto-dev/commit/750baff5fbe790a1991a832094f887c6e90bc521))
* **js:** make test sowrks ([1989ba1](https://github.com/unit-mesh/auto-dev/commit/1989ba17fdf520566310851d4772249ab0fbc4a6))
* **js:** update for libs ([508a990](https://github.com/unit-mesh/auto-dev/commit/508a990d266d6636f84d315c1dbedd0033a4b7ca))
* **js:** update for test framework ([380ad76](https://github.com/unit-mesh/auto-dev/commit/380ad76b548d990dd0ef924f13d9837763ad092a))
* **kotlin:** add better support for it ([044a30c](https://github.com/unit-mesh/auto-dev/commit/044a30cb8ac10522e3ec426e63db3aba774838ba))
* **kotlin:** add error handle foreditor ([3b5793f](https://github.com/unit-mesh/auto-dev/commit/3b5793fb6c38411158e9812b9a0341a01793e4da))
* **kotlin:** add model gpt-3.5-turbo-16k this is an enhanced model with an increased token limit of 16k, which is more advantageous for handling large document texts ([b6354b6](https://github.com/unit-mesh/auto-dev/commit/b6354b6a1fcfec416eb2d5e767c6c061a71d4493))
* **kotlin:** add support for code ([34ef517](https://github.com/unit-mesh/auto-dev/commit/34ef5179329ee0fe09900366d97beb29e17b9759))
* **kotlin:** init code modifier ([e3a2738](https://github.com/unit-mesh/auto-dev/commit/e3a2738ace7f09fd6137da8b616af46bab40133f))
* **kotlin:** init for kotlin code ([624fd47](https://github.com/unit-mesh/auto-dev/commit/624fd47d73041028e3047e523ffc6acfc02ef770))
* **kotlin:** init inserter api ([9c7086a](https://github.com/unit-mesh/auto-dev/commit/9c7086a0aec14fb4f7572b6e77ba20687c6ccdc7))
* **kotlin:** make context works ([4efe9ba](https://github.com/unit-mesh/auto-dev/commit/4efe9ba45405ff286a0c164003a3d7a792180bc5))
* **kotlin:** make kotlin classes works ([aff005c](https://github.com/unit-mesh/auto-dev/commit/aff005cc3fbd208e1f2c236e46655a5df9198f52))
* **kotlin:** refactor FileContext to ClassContext ([3f20b36](https://github.com/unit-mesh/auto-dev/commit/3f20b36b2dfaba809554b3593e817e3ebfb12d61))
* make it works ([ff97dd8](https://github.com/unit-mesh/auto-dev/commit/ff97dd8a72b6cc9334e4844ee3560378abd6a7e7))
* move up related classes ([ccc6b31](https://github.com/unit-mesh/auto-dev/commit/ccc6b31b1881881fd3d45382307c150a922cac57))
* rename input ([36a9587](https://github.com/unit-mesh/auto-dev/commit/36a95874b08cd517b847ba521ff353cc0c490179))
* **test:** add for create dir ([62111fa](https://github.com/unit-mesh/auto-dev/commit/62111fa3610add08769ef7b5972ab9f44ab9c21f))
* **test:** auto insert to code ([d48e704](https://github.com/unit-mesh/auto-dev/commit/d48e704eb427faad86226272174e90df6644550c))
* **test:** make it works ([fe3cecc](https://github.com/unit-mesh/auto-dev/commit/fe3ceccb47a974485ace83aaa1a446af82e0a940))
* **test:** make model related ([8ddf6a0](https://github.com/unit-mesh/auto-dev/commit/8ddf6a0efb9c5c05c6e8eb53853c723773c9bf86))
* **test:** make not crash ([b610e6b](https://github.com/unit-mesh/auto-dev/commit/b610e6bab1bf46e0636c32e3228cc04e1a9fda61))
* **test:** make test file to project ([628c6ee](https://github.com/unit-mesh/auto-dev/commit/628c6ee8158568cb888fb2d22e36fd099f8f9756))
* **test:** set for indicator ([c5f4a8f](https://github.com/unit-mesh/auto-dev/commit/c5f4a8f7f6fa8c243de4a5a3cabc7aa2ce213ce5))
* **test:** update for sample ([636af2b](https://github.com/unit-mesh/auto-dev/commit/636af2b58e2be0c9156770489144fd40a15f90cf))
* **test:** update samples ([6270725](https://github.com/unit-mesh/auto-dev/commit/627072546b85c62247334154d985639cbd9387b8))
* thinking in bettwer title ([f709844](https://github.com/unit-mesh/auto-dev/commit/f7098444fccac2eac2d56573fb530380f9546e11))
* try to add validator ([459b711](https://github.com/unit-mesh/auto-dev/commit/459b71137ff1241b5d4212fe15f7c2bf43b3fb9c))
* try to resolve for coce ([77d3080](https://github.com/unit-mesh/auto-dev/commit/77d30807df2241a038f74ff582fb86171e90dac2))
* try to use bundle key ([11f277e](https://github.com/unit-mesh/auto-dev/commit/11f277e078ae8b20adbae40a231bb407a11a4a87))
* udpate for actions ([947ffbe](https://github.com/unit-mesh/auto-dev/commit/947ffbec5cc5e37f634ab7f4725554a11afb64ad))
* udpate for roles ([f8903f9](https://github.com/unit-mesh/auto-dev/commit/f8903f9957bdc21b4d31d24ef8ce09d098c21050))
* **ui:** add copy to toolbar ([77d62d2](https://github.com/unit-mesh/auto-dev/commit/77d62d22d11ce477e90a3071ba4e3d241c1755fc))
* **ui:** add foundation for message block ([f1125bc](https://github.com/unit-mesh/auto-dev/commit/f1125bca1dbb4409559fecc590e96dd684994fb7))
* **ui:** add handle for enter with new line ([7466800](https://github.com/unit-mesh/auto-dev/commit/746680073f258700a9f44d06abdb45895406953a))
* **ui:** add i18n to send response ([6aa46db](https://github.com/unit-mesh/auto-dev/commit/6aa46db07afdb0f0aaf9125d00627632ad588d26))
* **ui:** change view part ([4b70ad6](https://github.com/unit-mesh/auto-dev/commit/4b70ad608c2d529651ec8d2859d08d0c87e47bf4))
* **ui:** etract for new method ([51342a2](https://github.com/unit-mesh/auto-dev/commit/51342a26c9ffaca625e5d8e18343cefd3f7c0083))
* **ui:** extract for new ui ([86a5c72](https://github.com/unit-mesh/auto-dev/commit/86a5c7253ea8a883ce85c4e623cfb923de13af2d))
* **ui:** format ([3cbdc70](https://github.com/unit-mesh/auto-dev/commit/3cbdc700a7ad232459e99c7150fcb228f4b240a0))
* **ui:** init for like and dislike ([07301b2](https://github.com/unit-mesh/auto-dev/commit/07301b21b71e873c5acfb15f79366b7de8e83e1b))
* **ui:** init for part message ([0af1217](https://github.com/unit-mesh/auto-dev/commit/0af121716b93d79312e77ced527cc098051d9c19))
* **ui:** make chat works ([61d822d](https://github.com/unit-mesh/auto-dev/commit/61d822d6b080e62c1e10b2d3f1d06b3aeff81d39))
* **ui:** make it works ([7f882a3](https://github.com/unit-mesh/auto-dev/commit/7f882a3f6adff0b169c518d3c30ba5846764efa1))
* **ui:** make new content builder works ([0b7499c](https://github.com/unit-mesh/auto-dev/commit/0b7499c0c272c496fef8ddf566a874e5833cd100))
* **ui:** make panel works ([cf21878](https://github.com/unit-mesh/auto-dev/commit/cf218787e42a63dcdc08323ee0f8b11ff0a1ad1c))
* **ui:** render view after dispatch ([056d643](https://github.com/unit-mesh/auto-dev/commit/056d643604c7360e1d975c8262b4aff87a8cc19e))
* **ui:** update for bottom ([00cc025](https://github.com/unit-mesh/auto-dev/commit/00cc025d34c8c6e9e192a368b543c44c049f63b9))
* **ui:** update for display text ([c7e6e74](https://github.com/unit-mesh/auto-dev/commit/c7e6e74663e2515cbb94645416f19588918fc3b9))
* update for context ([d79a6f6](https://github.com/unit-mesh/auto-dev/commit/d79a6f63190e546c9e2700049a07a66f2aacbc19))
* update for js context ([4c2d883](https://github.com/unit-mesh/auto-dev/commit/4c2d883997f22b6756eedc97ce56aa40756351fd))
* update for libs ([0d68503](https://github.com/unit-mesh/auto-dev/commit/0d68503982d7c10b50a414d266fc9caa1ad7ca49))
* update for promter settings ([560b5e3](https://github.com/unit-mesh/auto-dev/commit/560b5e34fa3d45858fb55f53fba9109545520e45))
* update for samples ([ecffe36](https://github.com/unit-mesh/auto-dev/commit/ecffe365ec0ef613f6f3c22793d681480a683553))
* update for test code ([fdd3943](https://github.com/unit-mesh/auto-dev/commit/fdd3943b0312f426ddbdb5a7cd4d0b0f4bc43aee))



## [0.7.3](https://github.com/unit-mesh/auto-dev/compare/v0.6.1...v0.7.3) (2023-07-25)


### Bug Fixes

* add try catch for chunk ([2ff3a63](https://github.com/unit-mesh/auto-dev/commit/2ff3a633ec54063eda2260e234b4623db4a536a8))
* disable plugin verifier ([a4bcd00](https://github.com/unit-mesh/auto-dev/commit/a4bcd000bc8f755c5ca5701991b1b94867883a99))
* fix action group position issues ([c505638](https://github.com/unit-mesh/auto-dev/commit/c5056389ba486193fd419956a22d736ed4431811))
* fix base issues ([c6574d2](https://github.com/unit-mesh/auto-dev/commit/c6574d2a87723767c232092d6296252e12feda79))
* fix comment code ([f46ac9f](https://github.com/unit-mesh/auto-dev/commit/f46ac9f97436cd0f2ed201ffae7ac8b2cd55f85b))
* fix confilic ([2358565](https://github.com/unit-mesh/auto-dev/commit/2358565a0efa28524c829193ed2090b304fd5bcb))
* fix crash ([003b648](https://github.com/unit-mesh/auto-dev/commit/003b648494e9c86a35ade50950ab04738718c8f8))
* fix duplicated ([164efc8](https://github.com/unit-mesh/auto-dev/commit/164efc88a6d0dbeae9a0e57d10617f9c4bcf5c9c))
* fix indent ([b53ec47](https://github.com/unit-mesh/auto-dev/commit/b53ec479483a05cdeaa9d1c14066b9bb731a67a0))
* fix java langauge error issue ([9d2d4d5](https://github.com/unit-mesh/auto-dev/commit/9d2d4d552575225ebeadd47dd3ea72be9d19e5a5))
* fix not class issuesg ([3403ee8](https://github.com/unit-mesh/auto-dev/commit/3403ee84e6bd9c4cf4979cbd9cc7fbd109938c24))
* fix not controller has simliary chunk issues ([905306a](https://github.com/unit-mesh/auto-dev/commit/905306ae4953284447c17ceb2b57a3750a658ab6))
* fix null issues ([9b4677b](https://github.com/unit-mesh/auto-dev/commit/9b4677bfd8f58fb07d0ec1296af5d0d9b33ad861))
* fix null issues ([a1edce8](https://github.com/unit-mesh/auto-dev/commit/a1edce8be6e40ed98e8bcc6f86370420104ee4a2))
* fix path issues ([c6a87c2](https://github.com/unit-mesh/auto-dev/commit/c6a87c2bdfb580b7562306a22042355e1f9222e4))
* fix plugin duplicated issue ([f797a1c](https://github.com/unit-mesh/auto-dev/commit/f797a1c2df70a90da934c7e92c08cd6d399373f3))
* fix refactor issues ([5264de7](https://github.com/unit-mesh/auto-dev/commit/5264de7fbb271ab674033a59fffe296fce730f64))
* fix services error issues ([e8fccbb](https://github.com/unit-mesh/auto-dev/commit/e8fccbb35ab1b4f7b71c988972b3cf39b7dc2448))
* fix some cofings ([59795e2](https://github.com/unit-mesh/auto-dev/commit/59795e2a6531fa1dd628058720990bda3431a85e))
* fix t ypos ([ff0c12a](https://github.com/unit-mesh/auto-dev/commit/ff0c12a5676906036836a097fe69b5f8c89dd1b8))
* fix tests ([0754581](https://github.com/unit-mesh/auto-dev/commit/0754581db50e309cc020c774631e42ad5651c5bf))
* fix warning ([0237d4a](https://github.com/unit-mesh/auto-dev/commit/0237d4ada11a863f9f913cf7fb1ff6351f925266))
* **python:** fix runtime issues ([f32ab66](https://github.com/unit-mesh/auto-dev/commit/f32ab66d678b0a51ac6452734996f9312e0b96e4))
* remove some librs to idea only [#7](https://github.com/unit-mesh/auto-dev/issues/7) ([8cf9839](https://github.com/unit-mesh/auto-dev/commit/8cf983974f2762789c7c89603353f95c80dac1d1))
* set default layer name ([e88608d](https://github.com/unit-mesh/auto-dev/commit/e88608d77e44c9dc37004f60d75417ea7292d175))
* try to make test case ([e8a5d15](https://github.com/unit-mesh/auto-dev/commit/e8a5d152537e57634f1d1487a6a5022c09455676))
* update for config ([074df5c](https://github.com/unit-mesh/auto-dev/commit/074df5cd4ad6645c6ef42fa63e9ae6a4b32e854e))


### Features

* add first inline ([efe921a](https://github.com/unit-mesh/auto-dev/commit/efe921abc64b86a276cb3efa99b004eda69cb6c7))
* add for hucnk prompt ([73462bb](https://github.com/unit-mesh/auto-dev/commit/73462bb57b21f31da2856c662aff815e8725d3e1))
* add format code ([961b4df](https://github.com/unit-mesh/auto-dev/commit/961b4dfaa8e9140965185fe7a25c309fa671b7ab))
* add offset for handle prompt text ([5bdf188](https://github.com/unit-mesh/auto-dev/commit/5bdf188dd5713bf170473d067354cdd55bfaff88))
* align find issue to JetBrains ([8159a52](https://github.com/unit-mesh/auto-dev/commit/8159a5248b45c6d1a69834f27f7787e5d9a69734))
* align kotlin context ([b9bcbd9](https://github.com/unit-mesh/auto-dev/commit/b9bcbd9e81b9293eda9195d68a6e82ea3b29f05f))
* **changelog:** init for action group ([37841c0](https://github.com/unit-mesh/auto-dev/commit/37841c021f15c71b607d436589bc2b29019a169d))
* **context:** fix compile issue ([1e76a2f](https://github.com/unit-mesh/auto-dev/commit/1e76a2f7ee85fc3c501769805190323e75867619))
* **context:** redesign to uml method for autodev ([00afd26](https://github.com/unit-mesh/auto-dev/commit/00afd2617d609ed16d6e98ebd53727f66c3902cd))
* fix fix tests ([7b66dcd](https://github.com/unit-mesh/auto-dev/commit/7b66dcd138352312f488ab12b3000bb032428f83))
* fix no prompter issues ([e667582](https://github.com/unit-mesh/auto-dev/commit/e6675828d087f0997b63e721976340f4ab606f8f))
* init basic python prompter for context ([ef8d32f](https://github.com/unit-mesh/auto-dev/commit/ef8d32f731eade90e1dce3248c66a0ec145ffc17))
* init builder ([0466e44](https://github.com/unit-mesh/auto-dev/commit/0466e443e29b344fc30de2023a778ee9350b74a3))
* init first code completion intension ([420a0ba](https://github.com/unit-mesh/auto-dev/commit/420a0ba26123ecc8c1e532e4ada8078d5ae8e8e5))
* init for kotlin package ([f999196](https://github.com/unit-mesh/auto-dev/commit/f999196071a48a5a68a2b42b346b7ebcb6c2590a))
* init js class loader ([04a785b](https://github.com/unit-mesh/auto-dev/commit/04a785b07e55224c0cfc70ad37a0ed0514e2f856))
* init llm inlay manager ([5434b42](https://github.com/unit-mesh/auto-dev/commit/5434b4290d3735f32a86d31e9dff44611c9dc156))
* init plugin modules for like clinon ([6179197](https://github.com/unit-mesh/auto-dev/commit/61791972c8f8d3f1e5d4760874129bcfb60157cf))
* init python plugins ([4f75e6b](https://github.com/unit-mesh/auto-dev/commit/4f75e6ba3ad58ce420207e2ee2a3ab93c5f07c42))
* init save and format code ([1e34279](https://github.com/unit-mesh/auto-dev/commit/1e342797e0b2cf99ca241de87ba10724eb85d8a9))
* init some basic methods ([fa9f250](https://github.com/unit-mesh/auto-dev/commit/fa9f250ee48f1d49efb8fd3c1d8bbaf65d41b613))
* init text attributes ([042806e](https://github.com/unit-mesh/auto-dev/commit/042806ecb979914d60f6a4c66fe1590e0a0512ab))
* **inlay:** add autodev core block ([61a7bc8](https://github.com/unit-mesh/auto-dev/commit/61a7bc87a8f596af91f3ad0de53a9c976d25f837))
* **inlay:** add simple editor modified ([88d74e8](https://github.com/unit-mesh/auto-dev/commit/88d74e8ef3da47f7373993cff25f31b097e2c9ca))
* **inlay:** init command listener ([e5c42bd](https://github.com/unit-mesh/auto-dev/commit/e5c42bd9d007345b5c41969857e3348807a460d1))
* **inlay:** make for inserts ([80b06af](https://github.com/unit-mesh/auto-dev/commit/80b06af7924056a3f5a1cd32d9b1895e8b8e1135))
* **inlay:** try to release ([89eb4d5](https://github.com/unit-mesh/auto-dev/commit/89eb4d5299f33d9cec35b3b48702cda5cfb24014))
* **inlay:** try to set for listener ([93c724f](https://github.com/unit-mesh/auto-dev/commit/93c724f88b095a79f1198ffc50f451fbf38d5de2))
* **inlay:** use command listener ([fa46000](https://github.com/unit-mesh/auto-dev/commit/fa4600046b2ba07f8a0d7475d2644980f2308673))
* **inlay:** use inlay mode for code completion ([aa230c7](https://github.com/unit-mesh/auto-dev/commit/aa230c74ee9a69e5e790cc1a44efcbf76e6a395b))
* **js:** init advice ([ee2b14a](https://github.com/unit-mesh/auto-dev/commit/ee2b14a53c6672681e754341d7a0820e262e2ab4))
* **kotlin:** import code to source ([fb7c687](https://github.com/unit-mesh/auto-dev/commit/fb7c687b11e81c49ec59c37deefbcc50036d57ac))
* **kotlin:** init kotlin context prompter ([b3493b1](https://github.com/unit-mesh/auto-dev/commit/b3493b102dca3e98584ad4d930bfc4a51b6be0e5))
* **kotlin:** make context works ([381ec37](https://github.com/unit-mesh/auto-dev/commit/381ec37e85737511576bb9dc6a91a97c6c26fd76))
* make compile works ([ec332fc](https://github.com/unit-mesh/auto-dev/commit/ec332fc12408a88fb5e2cea93ddb9622f662f170))
* make intenstion works ([3141315](https://github.com/unit-mesh/auto-dev/commit/3141315fbdf7d7b82c07f7f16d5cbc3e3d50d887))
* make java simliar chunk works ([174afea](https://github.com/unit-mesh/auto-dev/commit/174afea4a9ffc54637e44870f0b5e51c4b2cae13))
* make prompmt works ([dc76f14](https://github.com/unit-mesh/auto-dev/commit/dc76f14d1b52a071d3f7c70bcdbfe8e47d6e0bab))
* make to extension point ([bf8ac64](https://github.com/unit-mesh/auto-dev/commit/bf8ac64921474ec5f098e5229c3e7b3233544d89))
* **python:** add simliar chunk for testing ([7dc3016](https://github.com/unit-mesh/auto-dev/commit/7dc301653ef51fcb2123dab5e86d0e8fa8860895))
* **python:** init context ([2f24e41](https://github.com/unit-mesh/auto-dev/commit/2f24e4147745668c0ccc43e334fa793ec5f4c779))
* **releasenote:** clean prompter ([65f54cf](https://github.com/unit-mesh/auto-dev/commit/65f54cfcce1c48e2d24aebc5535bdec4c3a1933f))
* **releasenote:** init for change log ([ebf8f22](https://github.com/unit-mesh/auto-dev/commit/ebf8f22ddfe03a4a2c086d162f45dec045ad2aad))
* **releasenote:** make it works ([86732ed](https://github.com/unit-mesh/auto-dev/commit/86732ed3e6de0897863bf3ca1bb16e5015431fde))
* **test:** init intention ([380a972](https://github.com/unit-mesh/auto-dev/commit/380a9723ad341f82836afc12a8679d61b4148e1e))
* **test:** init test context provider ([bff5c33](https://github.com/unit-mesh/auto-dev/commit/bff5c33a383f620cd5af986ef828e867fe86833d))
* try direct to codeS ([78dfa9b](https://github.com/unit-mesh/auto-dev/commit/78dfa9b98aeaa240196a3a98eabb3c93a9689955))
* try to add for displays ([04707c8](https://github.com/unit-mesh/auto-dev/commit/04707c88ac775b94d4d6e3d9469b481f027e3119))
* try to use completion ([c875910](https://github.com/unit-mesh/auto-dev/commit/c8759108552744a994a1b268c775afd98c0de68c))
* try to use simliar chunk ([d3734e3](https://github.com/unit-mesh/auto-dev/commit/d3734e3f3e7261afbc926f2bdd7f583201bb8280))
* try to use text presentation ([3ccdbca](https://github.com/unit-mesh/auto-dev/commit/3ccdbcae6b09bd49d3f28da2bc645df8e46bfb6d))
* udpate for idea plugin ([f103bf8](https://github.com/unit-mesh/auto-dev/commit/f103bf83c69fc29eabc7166fa8f28850b572a1ce))
* **ui:** init text inlay painter ([1d83926](https://github.com/unit-mesh/auto-dev/commit/1d83926361b2e196c1e85e859404e5b572ae69d0))
* update config for run cli ([14782ba](https://github.com/unit-mesh/auto-dev/commit/14782ba3e8dbf78bfd2369f43ffd1b5b1fdc9a32))
* update for comment usages ([a7f6d37](https://github.com/unit-mesh/auto-dev/commit/a7f6d37566b84a23be6dc6e3b81b9e62e775d51a))
* update for listener ([71c426d](https://github.com/unit-mesh/auto-dev/commit/71c426d0d771891adef196e3818aebdcd6ff9c1f))
* update for run action ([47fb8d7](https://github.com/unit-mesh/auto-dev/commit/47fb8d7bcbab12a917c1192109c5ce9207e1029b))
* update for run config ([db9d1e6](https://github.com/unit-mesh/auto-dev/commit/db9d1e66c2e62fef5a747a2079914607cb71abf1))
* update for tasks ([dd808bc](https://github.com/unit-mesh/auto-dev/commit/dd808bc761b3866f8564a68e941e4ae0c66be9ad))
* update for tasks ([8d029da](https://github.com/unit-mesh/auto-dev/commit/8d029dacb43f4b2f974ac19cf9e91868eda6f45e))
* update for tasks ([4cb47f0](https://github.com/unit-mesh/auto-dev/commit/4cb47f035da554e30fc44b2e42e878922d43b69f))
* update for tool Window icon ([9b20afb](https://github.com/unit-mesh/auto-dev/commit/9b20afb7d444d796d63a071c8cfd93dc069be02b))
* use director action ([76f3952](https://github.com/unit-mesh/auto-dev/commit/76f3952f5e83ffbcd45ee228fbb2df9076b2b366))



## [0.6.1](https://github.com/unit-mesh/auto-dev/compare/v0.6.0...v0.6.1) (2023-07-16)


### Bug Fixes

* fix filter error issues ([85ecba6](https://github.com/unit-mesh/auto-dev/commit/85ecba6400add103b98b4604a57226d0925e5a9c))
* fix tests ([8258a1e](https://github.com/unit-mesh/auto-dev/commit/8258a1e09dc47e17e2a67bfb198051aa386442ea))
* fix tests ([10e81d2](https://github.com/unit-mesh/auto-dev/commit/10e81d2336980f0efcafc043aa38d2c40dac2b56))


### Features

* add basic advice ([4de15e8](https://github.com/unit-mesh/auto-dev/commit/4de15e8a7c3359c53d7f94a38dfb431047c0efc3))
* add for method lines ([ad75358](https://github.com/unit-mesh/auto-dev/commit/ad753584d6aac17d31ac1f3914fc1d53843f11e3))
* add some codes for prompt strategy ([6b070f4](https://github.com/unit-mesh/auto-dev/commit/6b070f4518c5ce0ac603a4f88842d7f3edd8797f))
* add test for java file class ([56dd51b](https://github.com/unit-mesh/auto-dev/commit/56dd51b8c564d56c7d467df538e920f856a7323f))
* add test for spring layer char ([e66cc52](https://github.com/unit-mesh/auto-dev/commit/e66cc5269df559e26cdfa9e522e7b583689f0d70))
* init code strategy ([5f5d40d](https://github.com/unit-mesh/auto-dev/commit/5f5d40dbbbf9932ad66cf845224be1cca49e9b87))
* init java string processor ([cb3103c](https://github.com/unit-mesh/auto-dev/commit/cb3103c877cfc1b9f944ed1ad32f4460df295090))
* init service name ([14f7269](https://github.com/unit-mesh/auto-dev/commit/14f7269e4707b56d66a27336c7280501c992c43b))
* inline for pomports ([a7a4d63](https://github.com/unit-mesh/auto-dev/commit/a7a4d6375b0ddcaca7089fd891d21b9e43a9ed75))
* make auto create controller works ([10e441d](https://github.com/unit-mesh/auto-dev/commit/10e441dacbb4ee6a4b4a5e15dc4eb3b7e6a85b74))



# [0.6.0](https://github.com/unit-mesh/auto-dev/compare/v0.5.5...v0.6.0) (2023-07-14)


### Bug Fixes

* add lost codes ([a5bf86b](https://github.com/unit-mesh/auto-dev/commit/a5bf86bd385c0cb3da948c9e9c94b0a8b2ef30d5))
* fix cos ([bda26ca](https://github.com/unit-mesh/auto-dev/commit/bda26ca5ca1729302f923d39d36b94b2f5326476))
* fix format issues ([6fce6f7](https://github.com/unit-mesh/auto-dev/commit/6fce6f79f862b1261348f1488cc30eb8b26404ae))
* fix interface error issues ([9f04d13](https://github.com/unit-mesh/auto-dev/commit/9f04d13856a94f85c11bf35dc6a7ee6b890b4648))
* fix issues for all format ([83e3776](https://github.com/unit-mesh/auto-dev/commit/83e37766602acdbbe76f5c12a1084c9dc6968051))
* fix prompting issue ([01966ec](https://github.com/unit-mesh/auto-dev/commit/01966ec5f8b106458d13f3c9fd026a6d71a519d9))
* fix read issues ([8c6eab0](https://github.com/unit-mesh/auto-dev/commit/8c6eab0155903af651aa2d30eda86a3216158973))
* fix sample errror ([eefb61e](https://github.com/unit-mesh/auto-dev/commit/eefb61e37cda5b8abe9b8066f85fa40fa281949e))
* fix single loading issues ([09d21b7](https://github.com/unit-mesh/auto-dev/commit/09d21b70adf7472efd3eb29f600c754706d2c2f0))
* fix split issues ([690f839](https://github.com/unit-mesh/auto-dev/commit/690f839a21b5ed374f3345d44aae1361d61c5bef))
* fix test ([a241a3c](https://github.com/unit-mesh/auto-dev/commit/a241a3c8cd69c289110c843f865d528716ba82c2))
* make repository works ([0f17226](https://github.com/unit-mesh/auto-dev/commit/0f17226c5b1e065d1d66f490d9e704710fb73345))
* try to update for dtos & entities ([23a7e5d](https://github.com/unit-mesh/auto-dev/commit/23a7e5d059747f538672aca4bafe1010cf5bc7fe))


### Features

* add basic for dto ([94c1b9a](https://github.com/unit-mesh/auto-dev/commit/94c1b9a3ab2036f69e4bb368b63e31afa4d5f653))
* add basic parse for multiple codeblocks ([34926d2](https://github.com/unit-mesh/auto-dev/commit/34926d27c7a8ce204d1ad5d3c735a8010415cd79))
* add condition for check method ([ea6c87a](https://github.com/unit-mesh/auto-dev/commit/ea6c87a1a55ba83c2e84f93dda5e8a51cd810a95))
* add crud processor ([6204066](https://github.com/unit-mesh/auto-dev/commit/62040667aab54fc3d3c0edde0b5c1408e6ad7637))
* add default config for prompt ([c1a2779](https://github.com/unit-mesh/auto-dev/commit/c1a2779fa8f76ca3beedbea5b336d796ce70712b))
* add for custom prompot config ([bb4f0b2](https://github.com/unit-mesh/auto-dev/commit/bb4f0b2b6a5b811d2a4f98d56a9a0b2af71cdd93))
* add handle for start with indent ([191ae4c](https://github.com/unit-mesh/auto-dev/commit/191ae4c425b22584aa5b415c3d02c3d4a1bf9892))
* add lost for define entity ([05e75f1](https://github.com/unit-mesh/auto-dev/commit/05e75f1f98f477f6362dcbfdf758959e8e76baba))
* add requirements to prompot item ([0b07662](https://github.com/unit-mesh/auto-dev/commit/0b07662fe534f166832a49d70ed10d8cc4e5e143))
* add test to keep config same to prompot ([674dfe6](https://github.com/unit-mesh/auto-dev/commit/674dfe66e30ac2da4606e9bce7db6be43eccc4e2))
* enable for scroll ([e0cf097](https://github.com/unit-mesh/auto-dev/commit/e0cf097aaa211109135469e8e5cf8b60dff28fc5))
* init basic code post processor ([4f8652e](https://github.com/unit-mesh/auto-dev/commit/4f8652e40c432d3b5006d93791e86fcab2ff423f))
* init basic service logic ([aab5c0d](https://github.com/unit-mesh/auto-dev/commit/aab5c0d4349af1f07fe1b5f81cc0d4e28a610765))
* init default spec ([0c3d87f](https://github.com/unit-mesh/auto-dev/commit/0c3d87f2c992ad680f6b5f6d65e49e8df0308926))
* init for create dot and entity ([5d37ce5](https://github.com/unit-mesh/auto-dev/commit/5d37ce5bfabcdda0b98eb5242e0400621152fb39))
* init for psi code ([b459fa4](https://github.com/unit-mesh/auto-dev/commit/b459fa487b92c84fb3c3d163917d50c88b21274c))
* make space works ([631c818](https://github.com/unit-mesh/auto-dev/commit/631c818fa213fdbfe566786f198e0a6933562fb5))
* make spec to configureable ([83cf990](https://github.com/unit-mesh/auto-dev/commit/83cf9904c42f468a34b3db78351c30c03d942c3b))
* update for autodev demo ([0e6e759](https://github.com/unit-mesh/auto-dev/commit/0e6e7590ebb1c3bf252d1f622fa6ad1f185ed952))
* update for cursor position ([12feb34](https://github.com/unit-mesh/auto-dev/commit/12feb34a6949de0d99bcd8f9d526b255145992dd))



## [0.5.5](https://github.com/unit-mesh/auto-dev/compare/v0.5.4...v0.5.5) (2023-07-12)


### Bug Fixes

* fix usage issues ([9cff4fa](https://github.com/unit-mesh/auto-dev/commit/9cff4fa531e9b07e0047c4e62115fae4406e6205))


### Features

* add basic stacks ([c5bfc1b](https://github.com/unit-mesh/auto-dev/commit/c5bfc1bf809c2f0c06f2ca3febd5b74ca338676c))
* add commit faeture ([afcaafa](https://github.com/unit-mesh/auto-dev/commit/afcaafae1f988d426c78cdc1aaf47ae58b3a72c2))
* add fix this for issues ([12f418c](https://github.com/unit-mesh/auto-dev/commit/12f418caf7a20f2dc0e49beddaee4ba6d638c89c))
* add gradle to deps ([025d85f](https://github.com/unit-mesh/auto-dev/commit/025d85fb9a012503345afae3e98c1f4c56330e28))
* add simple fix issues ([5eb07f8](https://github.com/unit-mesh/auto-dev/commit/5eb07f869f35f8e4fd05829e824a8bd26a811b61))
* init ddl action ([e76f7e5](https://github.com/unit-mesh/auto-dev/commit/e76f7e59597ce4ec953859842dbfd9a799133d3a))
* init for generate commit message ([377df63](https://github.com/unit-mesh/auto-dev/commit/377df6356fe4f9655555ecb1bed863ac67731259))
* init for sql ddl ([43970b1](https://github.com/unit-mesh/auto-dev/commit/43970b1af79cc45d4ad65e2d90f800b3906e4f62))
* init library data ([60c5d6d](https://github.com/unit-mesh/auto-dev/commit/60c5d6dde6ade2d13ad149d9e5f9a7be5a1a9dfd))
* init stream api ([7bc6889](https://github.com/unit-mesh/auto-dev/commit/7bc688996d2af994b1c9277a654736e9319ae3c3))
* init vcs for detect cchange ([98261d7](https://github.com/unit-mesh/auto-dev/commit/98261d73e5761342a9c10481d84b1760c094a2d5))



## [0.5.4](https://github.com/unit-mesh/auto-dev/compare/v0.4.0...v0.5.4) (2023-07-11)


### Bug Fixes

* fix ci ([32c7f25](https://github.com/unit-mesh/auto-dev/commit/32c7f25e738aaf12bcab26c4a358ed08a42f7c9f))
* fix for context error ([b9a5a54](https://github.com/unit-mesh/auto-dev/commit/b9a5a54c7561da31dd28be038da23ef6130987eb))
* fix formpat issues ([d7c0cbc](https://github.com/unit-mesh/auto-dev/commit/d7c0cbcef95735af0f125e76eb616d8f81219eb5))
* fix gpt versions issues ([6583dd2](https://github.com/unit-mesh/auto-dev/commit/6583dd2af4562142b859e5f179883d5659f8f36a))
* fix model issues ([45aef78](https://github.com/unit-mesh/auto-dev/commit/45aef787f49b3ac2c41307ad6d225130e3b88314))
* fix not split class issues ([72a1cb8](https://github.com/unit-mesh/auto-dev/commit/72a1cb891768369cb59f1d6796665aa5a1f16081))
* fix read issues & fixed [#3](https://github.com/unit-mesh/auto-dev/issues/3) ([467cf04](https://github.com/unit-mesh/auto-dev/commit/467cf04e13e2cfc4bc9f754cddc1f1393cbbd84b))
* fix regex typo ([8677a9e](https://github.com/unit-mesh/auto-dev/commit/8677a9e928a6121916d46f8c66dffaf4acdd22ea))
* fix request issues ([46d31d7](https://github.com/unit-mesh/auto-dev/commit/46d31d73d74b203150833a60ae9dc92a0291a86c))
* fix typo ([3668310](https://github.com/unit-mesh/auto-dev/commit/36683109a9597e394bd87f01e4a4f024a3040e81))
* fix typos ([3badd51](https://github.com/unit-mesh/auto-dev/commit/3badd5106527ca37ec9090c7db8430f4ebad606f))
* fix typos ([75d83d3](https://github.com/unit-mesh/auto-dev/commit/75d83d32c5ebe6a011e11aa1759c63a85f0da630))
* fix typos ([7c1a61c](https://github.com/unit-mesh/auto-dev/commit/7c1a61c7f2959f5a9342538ef3a05bb5030c47ed))
* fix waring ([2ff34ec](https://github.com/unit-mesh/auto-dev/commit/2ff34ecb03523175a86b65794412841f52c8e7fc))
* make class working ([1771aef](https://github.com/unit-mesh/auto-dev/commit/1771aef6c7a0fc1a34cf5e99a56b40289986d1c8))


### Features

* add basic for dt clzss ([75d2998](https://github.com/unit-mesh/auto-dev/commit/75d29984ac5f93d8e807491026fcae17875076f0))
* add better markdown support ([2ab5272](https://github.com/unit-mesh/auto-dev/commit/2ab5272ee9788672495b2ab5996ab7d9cbd34767))
* add code complete ([bf17097](https://github.com/unit-mesh/auto-dev/commit/bf17097b90b572b2e163bb1a20f3887897d2af3a))
* add first version autodeV ([9aa9f9d](https://github.com/unit-mesh/auto-dev/commit/9aa9f9d89a767556c1804ce73baa3b1da61925fb))
* add for load from review ([94a5313](https://github.com/unit-mesh/auto-dev/commit/94a5313d3e999c8250f964f1d173a6fb51bac61e))
* add for selected text ([753c9ca](https://github.com/unit-mesh/auto-dev/commit/753c9ca8c0c7bd377decd305407b1f4b23694afd))
* add for service ([38aaa35](https://github.com/unit-mesh/auto-dev/commit/38aaa3511f80aa39ce3cb3b981eedcbc9eaf8a95))
* add for spring controller sample ([cd3d6af](https://github.com/unit-mesh/auto-dev/commit/cd3d6af13e5a0a2d8cb1da31ac4fd7926a3f23c2))
* add history api supports ([cd3b158](https://github.com/unit-mesh/auto-dev/commit/cd3b158a2c6c2d2241c3b59225ed7070a1192f16))
* add review this ([5da2563](https://github.com/unit-mesh/auto-dev/commit/5da2563532ee3a23b714c4bf0ae83fc0aa3a27b1))
* add write test action ([920f255](https://github.com/unit-mesh/auto-dev/commit/920f25568e3c02112e0297526583d1fbb4659f47))
* change text to multiline ([119b99f](https://github.com/unit-mesh/auto-dev/commit/119b99ffae301fd2458dd4f644057d15eed62f83))
* enable prompt as context ([14fc846](https://github.com/unit-mesh/auto-dev/commit/14fc846aeb04c53ab178321d7adddec6b4904641))
* init basic azure api ([644e675](https://github.com/unit-mesh/auto-dev/commit/644e67509e67996a7c5f740db8c7e3a2d23250ca))
* init for co-mate ([55e2783](https://github.com/unit-mesh/auto-dev/commit/55e278377375700ab2b9fdcefc0b2a7349712897))
* make auto complte can load from contenxt ([3682bb9](https://github.com/unit-mesh/auto-dev/commit/3682bb9c56c45855a9cefff7d386002df0428ed7))
* make controller nabel to call service ([2f5be15](https://github.com/unit-mesh/auto-dev/commit/2f5be1548a80273e0ee9897a70d1033f7745f8ea))
* make refactor this ([70f0860](https://github.com/unit-mesh/auto-dev/commit/70f0860d62eb471653e49efb4b5ec06fe1c86ce3))
* set default cursor for items ([9933b0a](https://github.com/unit-mesh/auto-dev/commit/9933b0a7ea61e1dc4044808437015fc26296a1e4))
* try to use table ext ([957d616](https://github.com/unit-mesh/auto-dev/commit/957d6165a4d8f72f4cfde63eef92bc28a9266a57))
* update for template ([a49e1e0](https://github.com/unit-mesh/auto-dev/commit/a49e1e0802d2ce4283add654c7a297b3493e6d2e))



# [0.4.0](https://github.com/unit-mesh/auto-dev/compare/v0.3.0...v0.4.0) (2023-05-08)


### Bug Fixes

* use password field for sensitive info ([1bec18a](https://github.com/unit-mesh/auto-dev/commit/1bec18a18ac9b755b4295ea57d5bf79c97289d69))


### Features

* add for expalin code ([ec9c1bc](https://github.com/unit-mesh/auto-dev/commit/ec9c1bc71ec2b4eb319e56c95cff35ababc1164b))
* init chat bot ([7acd10f](https://github.com/unit-mesh/auto-dev/commit/7acd10f21882dee9cf157f713ef8b04c11de920d))
* init for expalin in console ([bd5f61c](https://github.com/unit-mesh/auto-dev/commit/bd5f61c94a98f7ae47d80e6fb772fc5a3e183a0b))
* make chinese prompt support ([9d5e338](https://github.com/unit-mesh/auto-dev/commit/9d5e3381f5505108375ab4509fe137ddd724cf81))



# [0.3.0](https://github.com/unit-mesh/auto-dev/compare/v0.2.0...v0.3.0) (2023-04-26)


### Features

* add custom server connector ([a23a8b8](https://github.com/unit-mesh/auto-dev/commit/a23a8b82f5c2009112db912e276f046c6e1ac040))
* add did change ([0c66f65](https://github.com/unit-mesh/auto-dev/commit/0c66f651c09efd13ed5973afa25338091422ff68))
* add engine prompt config ([be15620](https://github.com/unit-mesh/auto-dev/commit/be15620dd7b4a6fcf45c3d4154ffd49456c3e78a))
* add for prompt by text ([e8ac525](https://github.com/unit-mesh/auto-dev/commit/e8ac525241aacb5acca34daf7675aec4bd4b6cdf))
* add handle for response ([2fd358e](https://github.com/unit-mesh/auto-dev/commit/2fd358e40c602c1324421b05bfae2fee428d9fd9))
* add prompt config ([c613ce3](https://github.com/unit-mesh/auto-dev/commit/c613ce31e9880d78a805cfc899b0f3794b6d4eb2))
* init agent model ([b838ad7](https://github.com/unit-mesh/auto-dev/commit/b838ad7a5ee5f369150bb4757f415ac4b00be5b3))
* make enable to send request ([c472173](https://github.com/unit-mesh/auto-dev/commit/c472173a030ab1bc3ddc6631a97411d1f4cf0727))
* update prompot layout ([472b5d0](https://github.com/unit-mesh/auto-dev/commit/472b5d03327cd7cb454a5f4ae8c4fcb1a081d125))



# [0.2.0](https://github.com/unit-mesh/auto-dev/compare/v0.0.8...v0.2.0) (2023-04-23)


### Bug Fixes

* remove unused parameters ([fb68b71](https://github.com/unit-mesh/auto-dev/commit/fb68b71cf8d0bd75603fba9b2329d9cfc6718f97))


### Features

* add find bug support ([f971135](https://github.com/unit-mesh/auto-dev/commit/f971135f59f98cc3665bc7516edbab5f3aed8926))
* add proxy ([e6ef839](https://github.com/unit-mesh/auto-dev/commit/e6ef8399fec5d921564e0b154f2b0c6970fcb097))
* add proxy for items ([ddb5c20](https://github.com/unit-mesh/auto-dev/commit/ddb5c20a0c163ae8348d069430a114d824b06d54))
* add simple suggestion ([7fd8aaa](https://github.com/unit-mesh/auto-dev/commit/7fd8aaa3a64b83bf6358e4d144823100c58daca6))
* init for service template ([a0a1b8a](https://github.com/unit-mesh/auto-dev/commit/a0a1b8aff8970131ffb6aef5f6304b9cee07cb61))
* make custom open api host works ([f4861ce](https://github.com/unit-mesh/auto-dev/commit/f4861ce82e93b3e3614b435da2b93b7305f046d1))



## [0.0.8](https://github.com/unit-mesh/auto-dev/compare/v0.0.7...v0.0.8) (2023-04-21)


### Bug Fixes

* fix default timeout ([9746266](https://github.com/unit-mesh/auto-dev/commit/9746266b5dd7d809c7a619f7b6c4cd8d65d4afa1))
* try to merge configure ([6f0d24b](https://github.com/unit-mesh/auto-dev/commit/6f0d24ba26972eef53815a7ed8da7d3a7cc889ac))
* update template ([69eb06c](https://github.com/unit-mesh/auto-dev/commit/69eb06c2cc26e5b947aa94524bac8581a125b709))


### Features

* add progressbar for code complete action ([a8b3fdc](https://github.com/unit-mesh/auto-dev/commit/a8b3fdc0e0f7465301d336065747ffa6334d07ce))
* update config for xml ([d5c70f6](https://github.com/unit-mesh/auto-dev/commit/d5c70f681b3454fa3b77105f34231f2a6890a44d))



## [0.0.7](https://github.com/unit-mesh/auto-dev/compare/v0.0.3...v0.0.7) (2023-04-19)


### Bug Fixes

* fix lost version ([47e027f](https://github.com/unit-mesh/auto-dev/commit/47e027f831236f02e3113221f7127376e97e9744))
* fix runtime issues ([90d1ca0](https://github.com/unit-mesh/auto-dev/commit/90d1ca0bdf924ccdaffe4882f98b006ff0cdfb1a))
* fix typos ([fe0417f](https://github.com/unit-mesh/auto-dev/commit/fe0417f6575b6358db9336b27300ce88c23ab50a))
* fix typos for xml ([04d5bf5](https://github.com/unit-mesh/auto-dev/commit/04d5bf5cc02abc010e1e5bd88940f5ffcbd408ed))
* fix version again ([f1794c5](https://github.com/unit-mesh/auto-dev/commit/f1794c5a71df443150436133613f6d778b674003))
* modify api usage ([74fb4de](https://github.com/unit-mesh/auto-dev/commit/74fb4dec22a00bdfa9dfa151552a6424def93d74))
* not follow the openai README ([ac2abd1](https://github.com/unit-mesh/auto-dev/commit/ac2abd1122960b147a2cd191341f30cb1b4f49a7))


### Features

* add auto comment support ([f47572d](https://github.com/unit-mesh/auto-dev/commit/f47572db4e062bafb6c12c2cb43817f8807fe976))
* make code comments works ([db369d2](https://github.com/unit-mesh/auto-dev/commit/db369d2df86e28c418c4f812fbdaadab7532e25d))



## [0.0.3](https://github.com/unit-mesh/auto-dev/compare/v0.0.2...v0.0.3) (2023-04-18)


### Bug Fixes

* fix line marker leaft psi issue ([3fbacf3](https://github.com/unit-mesh/auto-dev/commit/3fbacf3e903fc30f56dc24b81b151fbfc3d772ba))
* fix some typos ([14d0b91](https://github.com/unit-mesh/auto-dev/commit/14d0b919aeb4d7497345edd2778b8eb97a036ff1))


### Features

* add brain configures ([d049040](https://github.com/unit-mesh/auto-dev/commit/d04904004a47e2fbc729ca7f32a08c50e7283198))
* add code complete actions ([07bff16](https://github.com/unit-mesh/auto-dev/commit/07bff1656337d094c329d0ed854abbeb7e1f5f08))
* add controller to method name ([93b1808](https://github.com/unit-mesh/auto-dev/commit/93b18088482b2bcad7df4abebb0c49b22db2cbe5))
* add copilot types ([2986a33](https://github.com/unit-mesh/auto-dev/commit/2986a330b586101b6353c74a13cec6d9423a7600))
* add find bug icon ([f5ec8c4](https://github.com/unit-mesh/auto-dev/commit/f5ec8c4dad3b62a6a9fb68f4ae523d2085a164c0))
* add linemarker for testings ([3f0879a](https://github.com/unit-mesh/auto-dev/commit/3f0879a0ca43b80964c0261acf426d0f79f4493f))
* init find bug line marker ([d74d9fb](https://github.com/unit-mesh/auto-dev/commit/d74d9fb11e713a0b367d20e80e24a0bf1664ea4b))
* init settings bar ([45060bf](https://github.com/unit-mesh/auto-dev/commit/45060bf50e450c871894676b7611d96cf16d0263))
* make it works ([4add40c](https://github.com/unit-mesh/auto-dev/commit/4add40c17b7b723fc5f34d43002b0c82700c194d))
* udpate code cmplete ([5e05dab](https://github.com/unit-mesh/auto-dev/commit/5e05dab1e70ef6e43572b80400c392777bbdea12))



## [0.0.2](https://github.com/unit-mesh/auto-dev/compare/v0.0.1...v0.0.2) (2023-04-17)


### Bug Fixes

* fix class empty issues ([a9cf9be](https://github.com/unit-mesh/auto-dev/commit/a9cf9be1a107a11acc2e56f92940c33aeb743b62))
* fix template code error issues ([ef46df4](https://github.com/unit-mesh/auto-dev/commit/ef46df45dad8bd4c22d9b8f9632d5433e1578845))
* fix template dir error issues ([9485bdb](https://github.com/unit-mesh/auto-dev/commit/9485bdb44553e83bc33877d0ea7ad7b18df6ec71))
* fix test warnings ([b8a3685](https://github.com/unit-mesh/auto-dev/commit/b8a36852648c1ea84ae1d3fadd470bace46910f2))
* fix tests ([647eb17](https://github.com/unit-mesh/auto-dev/commit/647eb17a8ec99e74a6d5bd8427ad3d809f821c49))
* fix typos ([7e64357](https://github.com/unit-mesh/auto-dev/commit/7e6435701a7e7abad7924d6f401ea41eee78801d))
* fix warining ([aed6609](https://github.com/unit-mesh/auto-dev/commit/aed66098c196e0595723f13d7354d0f159e6624d))
* set tempature to 0.0 ([f2e066e](https://github.com/unit-mesh/auto-dev/commit/f2e066e86871cb16d999b5fafec4aaf6ed8e2c37))
* update template ([c1749fe](https://github.com/unit-mesh/auto-dev/commit/c1749fe96d7bac007268fb5a6737ce80e7b8267e))


### Features

* add handle for template ([b2a9df5](https://github.com/unit-mesh/auto-dev/commit/b2a9df5396805ac48e1f84f01a77b28125916ac1))



## [0.0.1](https://github.com/unit-mesh/auto-dev/compare/e085cfe3974610d9fe3459ed61639c27dd96af95...v0.0.1) (2023-04-17)


### Bug Fixes

* fix comments issues ([7dd2f48](https://github.com/unit-mesh/auto-dev/commit/7dd2f48aa33c508828517d069af223678c74f3c6))
* fix compile warning ([83e33cc](https://github.com/unit-mesh/auto-dev/commit/83e33cc87ef5eb3a11d2bbc6128dbee11aa87a93))
* fix create from context issues ([e8f05fb](https://github.com/unit-mesh/auto-dev/commit/e8f05fb83b5fe79745b62905731252fb733b051f))
* fix for api engien width issues ([f8eaf64](https://github.com/unit-mesh/auto-dev/commit/f8eaf64b4e51f27d5ab8e6462d7a89c37bba18db))
* fix for kotlin psi issues ([6dd12db](https://github.com/unit-mesh/auto-dev/commit/6dd12dbfdeae457bf2303ce0e00c264cf9f314fc))
* fix for story ([97e6034](https://github.com/unit-mesh/auto-dev/commit/97e60348dfcc759aa252a2f02d85550b19cd2609))
* fix README ([e085cfe](https://github.com/unit-mesh/auto-dev/commit/e085cfe3974610d9fe3459ed61639c27dd96af95))
* fix running issues ([7a84358](https://github.com/unit-mesh/auto-dev/commit/7a84358ae5c06c49edae1c81188d793aa7985307))
* fix some typos ([96fda09](https://github.com/unit-mesh/auto-dev/commit/96fda097fe797cc745ebad93f7724128f549dc76))
* fix story id ([e19e279](https://github.com/unit-mesh/auto-dev/commit/e19e279c7c3d8c45c6729b5cf3c7cadd812ccf94))
* fix tests ([9b3cc72](https://github.com/unit-mesh/auto-dev/commit/9b3cc724028f049effc728d09aed03efeac2c406))
* fix typos ([3891d5d](https://github.com/unit-mesh/auto-dev/commit/3891d5d5c6f0eff389386a443232ff09541dee7f))
* fix typos ([118a655](https://github.com/unit-mesh/auto-dev/commit/118a65566522c6aec94bf01e8a91234325588fed))
* fix ui issues ([da7836b](https://github.com/unit-mesh/auto-dev/commit/da7836b4e84a863a539950c4c49b199fd236c2ce))
* fix writtings issues ([456a4ec](https://github.com/unit-mesh/auto-dev/commit/456a4ecf2e4ca90a1d705939997546273a549977))


### Features

* add first test for samples ([89efd92](https://github.com/unit-mesh/auto-dev/commit/89efd927301030462a32dd23dd2b97a1987da504))
* add github repo to projects ([1301826](https://github.com/unit-mesh/auto-dev/commit/13018264453755ac38a5b7e9e864cffe9c500ecf))
* add indicator ([6b3af63](https://github.com/unit-mesh/auto-dev/commit/6b3af6315a2f30d1cd18a6ce38430f3cb3a0bcb2))
* add init for controller update ([c2f3155](https://github.com/unit-mesh/auto-dev/commit/c2f31552d04ddd7228da1e56497658659c86dab3))
* add more filter for works ([f107637](https://github.com/unit-mesh/auto-dev/commit/f107637296acc53dfaa59c3e6d5d024c42a57683))
* add project info to projecT ([fa255e0](https://github.com/unit-mesh/auto-dev/commit/fa255e0e590d1bbacedbb1576b53d18f3766825d))
* add test for annotator ([e046f4f](https://github.com/unit-mesh/auto-dev/commit/e046f4f26f77100da31a0ccb00b4b6017bc67813))
* add test for controller prompt ([db017b5](https://github.com/unit-mesh/auto-dev/commit/db017b5dc7234a3f20377ff25a21029d0ea8828a))
* init basic bnfgst ([0f96930](https://github.com/unit-mesh/auto-dev/commit/0f96930f35ede61b7e99538bca6889834fc4f464))
* init basic command cli ([783435f](https://github.com/unit-mesh/auto-dev/commit/783435f12e78433ed35c9d9a38dad60432494b3c))
* init basic command runner ([0a8e7f4](https://github.com/unit-mesh/auto-dev/commit/0a8e7f4e4523f612b09afe9d26a134ad46ca1623))
* init basic configure ([9d12a5f](https://github.com/unit-mesh/auto-dev/commit/9d12a5f419b09545e7d787d80e156cebb39c0d27))
* init basic configure editor ([2ee854c](https://github.com/unit-mesh/auto-dev/commit/2ee854c58ed06ab874b1bfb9efb35ae40ba7b336))
* init basic datastruct for github and gpt ([787450e](https://github.com/unit-mesh/auto-dev/commit/787450e4d71be1220bd377f39ef4f0f67358841e))
* init basic format ([e6c3676](https://github.com/unit-mesh/auto-dev/commit/e6c36766e1a1422491629112fd9f82644dade699))
* init basic github actions ([4326f80](https://github.com/unit-mesh/auto-dev/commit/4326f80b86bfc3f53480e3be09c44ec209e13312))
* init basic workflow for devti flow ([4825f55](https://github.com/unit-mesh/auto-dev/commit/4825f5511abc218dc43d03cc63fc996100eb6d22))
* init command ([c797a45](https://github.com/unit-mesh/auto-dev/commit/c797a45e512b67a3267820fa0a77f4d9dfea9fd9))
* init config for button ([9005114](https://github.com/unit-mesh/auto-dev/commit/90051141696cbbdc1d4682d89e1e19cf0fb2b047))
* init display story line ([3cec8c4](https://github.com/unit-mesh/auto-dev/commit/3cec8c4947206875f57c8dffac1e8b6bc6865013))
* init dt command runner ([733eb4e](https://github.com/unit-mesh/auto-dev/commit/733eb4e0bf00be40da0f6e57f990230a092724fc))
* init dt model ext ([08e81f6](https://github.com/unit-mesh/auto-dev/commit/08e81f64ce4383cea8249af88da4f2bb09f92f5e))
* init for devti window ([e6e066f](https://github.com/unit-mesh/auto-dev/commit/e6e066f958062318a4559a34830872794161d238))
* init for update story ([7e582aa](https://github.com/unit-mesh/auto-dev/commit/7e582aa3237bd779223ba8dcf673f5f33a5c22d8))
* init java endpoint fetcher ([452f867](https://github.com/unit-mesh/auto-dev/commit/452f86794b94613041ee7db035dc114622170554))
* init parser ([e3b7e98](https://github.com/unit-mesh/auto-dev/commit/e3b7e98623f4ef5c7d01d0d05b47dcbd13b3866f))
* make code can be re-try ([d9d695c](https://github.com/unit-mesh/auto-dev/commit/d9d695cae10c838086f5a7644538ed14e4e2acdf))
* make filter controller works ([ca90e20](https://github.com/unit-mesh/auto-dev/commit/ca90e2029b854a77acb794874246cebe303446a8))
* make it works ([ec96d01](https://github.com/unit-mesh/auto-dev/commit/ec96d01ae39a977dce1d3f569b4051333cc38dbd))
* make pass data to success ([e5ad825](https://github.com/unit-mesh/auto-dev/commit/e5ad825df9fadc5223dff49692daa89b064022c3))
* try to save configure type ([dd5772f](https://github.com/unit-mesh/auto-dev/commit/dd5772faed142cabe148841926473f361696367d))
* tryt o use old version for grammar kit ([db0c8c4](https://github.com/unit-mesh/auto-dev/commit/db0c8c4fd607d68f78dc66822cc373e94be9507b))
* update for configure ([1eb22b8](https://github.com/unit-mesh/auto-dev/commit/1eb22b8a0dfb9aa6a379aa6fb05dd93bf07c05af))
* use single binding ([9092752](https://github.com/unit-mesh/auto-dev/commit/9092752a4a79ff64d062e089137f427a83db3988))




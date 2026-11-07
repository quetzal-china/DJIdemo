# AI 使用说明（提交要求第 4 项）

> 如实记录本次作业中 AI 工具的使用环节与人工验证方式。提交者对全部提交内容负责，并已/将逐项验证。

## 一、使用了哪些工具、各用于哪些环节

| 工具/模型 | 角色 | 使用环节 |
|---|---|---|
| **ZCode（GLM-5.3-Flash）** | 唯一主力开发与测试 Agent | 工程勘察（阶段0）、M1–M8 全部编码、JUnit 测试编写与运行、故障排查修复、Git 检查点、文档落盘 |
| **Zed（高端模型）** | 核心架构规划与复核 | `docs/ARCHITECTURE_BLUEPRINT.md` v1.0、`IMPLEMENTATION_PLAN.md`、`DECISIONS.md`、需求追踪矩阵初稿；不直接编码 |
| **ChatGPT** | 伴学讲解（按用户节奏） | 用户按模块将 GitHub commit/代码发给 ChatGPT 提问学习（交接入口：`docs/HANDOFF_FOR_CHATGPT.md`）；未参与编码 |

## 二、AI 辅助的关键产出与人工决策点

1. **架构与业务裁决不是 AI 拍板**：分层、包结构、七个工厂/四策略骨架由 Zed 规划；"无封顶满减、按币种汇总、跳过无效订单、幂等生产、进程内序列号、零宽字符清洗、两位小数"等默认方案由**项目所有者逐项确认**后（`docs/DECISIONS.md` §四）才作为实施依据，且全部标注"本次实施采用的默认方案，非原任务书明文规定"。
2. **数据事实来自真实文件而非 AI 记忆**：所有型号拼写、字段结构、地区币种取值、促销期望值均提取自真实 `order.json`、`DJIFactory` 常量、`DJIProductDemo` 源码与 `sysgui.png`（14 笔订单期望值表），未采信任何"想当然"的大疆产品数据。
3. **教师代码零改动约束**：`DJIProductDemo`、`DJIPromotion` 等 8 个教师文件保持原样（Git 基线 `f9ffbea` diff 为空），所有扩展以新增类实现。

## 三、如何验证或修改 AI 输出（可复现的验证链）

1. **测试即验证**：`mvn -B clean test` 一键复跑 62 项测试（退出码 0）；每项原版要求的断言位置见 `docs/01-需求追踪矩阵.md` 第二列/第三列。
2. **业务规则对答案**：14 笔订单的最优促销、优惠额、应付额与参考界面 `sysgui.png` 逐笔断言一致（`PromotionCalculationTest#allFourteenOrdersMatchReferenceScreenshot`）——AI 计算结果被独立来源交叉核对。
3. **运行即验证**：GUI 可按 README 第一节命令启动；教师 `DJIProductDemo` 零修改运行输出 9 架无人机（`java -cp target/classes com.dji.product.DJIProductDemo`）。
4. **AI 犯错实例（已修正，供审阅）**：GUI 冒烟启动暴露 `promoCache::get` 被误用作 `Function<Order,PromotionResult>`（`Map.get` 收 Object 导致编译通过但键型错误）；并发测试初版 `invokeAll`+CountDownLatch 自我死锁。两例的复现—修复—回归记录见 `README.md` §六 与 `docs/VERIFICATION.md`。
5. **修改入口**：所有代码有中文 Javadoc 与模块注释；改业务规则请先看 `docs/DECISIONS.md`（确认是否为"默认方案"可调项），改数据请只动测试 fixtures，勿动教师 `order.json`。

## 四、责任声明

本项目由提交者（liuyuxin）主导并最终负责：AI 产出的一切代码、测试、文档均经过上链可复现的构建/测试验证；GUI 七场景的交互验收由提交者在实机完成；如与课程要求冲突，以原版《G班机试2.md》与任课教师现场要求为准。

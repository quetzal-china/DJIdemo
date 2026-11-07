# PROGRESS｜模块交付事实日志

- 当前阶段：**全部完成**（M1–M8 实施 + GUI G1–G7 实机验收通过 + 已推送 GitHub：main=完整实施史，planning-archive=规划期快照）
- 当前真实 Git commit：见 `git log`（M7、M8 提交追加于 7b78546/0c7ed02 之后）；工作树干净
- 实际完成的源代码改动：M7 新增 `ui/`（DJIOrderApp/MainFrame/OrderTableModel + 测试）；M8 新增 README.md、docs/AI使用说明.md、矩阵状态更新；教师 8 个源文件**零改动**（git diff f9ffbea 为空）
- 最新验证状态：`mvn -B clean test` 退出码 0，Tests run: 62, Failures: 0, Errors: 0（2026-11-07）
- 阻断项：无（唯一开放项 = 用户实机执行 GUI 七场景验收）
- 下一步：用户实机验收 → （可选）Zed 终审（提示词 04）→ 用户推送 GitHub 与提交

## 模块交付记录（续）

```text
### 2026-11-07｜M7：Swing GUI 与异步任务
原版评分映射：任务6(20分)
Git 提交 SHA：见 git log（M7 提交）
真正修改的文件：新增 ui/{DJIOrderApp, MainFrame, OrderTableModel} + ui/OrderTableModelTest
执行的命令与退出码：mvn -B test → 0（62 tests）；GUI 冒烟 ×2（第二次零异常，第一次抓出 promoCache::get 键型 NPE 已修复）
真实测试摘要：OrderTableModelTest 4 项无头验证（14行10列/单元格值与促销 oracle 一致/占位符/筛选联动）；
  SwingWorker×2 + setBusy 防重入 + 常驻状态栏；监听器零业务规则
设计偏差：详情/汇总/错误用三个 Tab 的文本区呈现（计划中的 OrderItemTableModel/ProductionTaskTableModel
  以格式化文本区替代，信息等价可复制）——布局细节自主权范围内
伴学交接：HANDOFF §M7
下一步：M8

### 2026-11-07｜M8：全量回归、设计说明、AI 使用说明
原版评分映射：任务8(5分)+交付材料
Git 提交 SHA：见 git log（M8 提交）
真正修改的文件：README.md（设计说明+运行说明）、docs/AI使用说明.md、docs/01-需求追踪矩阵.md（全量状态更新）、
  docs/PROGRESS.md、docs/VERIFICATION.md、docs/HANDOFF_FOR_CHATGPT.md
执行的命令与退出码：mvn -B clean test → 0（62 tests）
真实测试摘要：62/62 全绿；C1 grep 零浮点；教师文件 diff 为空；需求追踪矩阵每项映射真实类#方法与测试名
设计偏差：无
伴学交接：HANDOFF §M7
下一步：用户实机验收 G1–G7；可选 Zed 终审
```

## 模块交付记录

```text
### 2026-11-07｜M1+M2：序列号单例、产品注册表、核心领域模型
原版评分映射：任务3(10分)+任务1(10分)核心
Git 提交 SHA：8e46b08
真正修改的文件：新增 product/{SerialNumberGenerator, SerialNumberExhaustedException, DroneSpecRegistry}、
  sale/ 全部领域类（5枚举+Customer+OrderItem+Order+PromotionType+PromotionResult+ProductionTask+2异常）；
  教师文件零改动
执行的命令与退出码：mvn -B test → 0（M1+M2 部分 22 tests）
真实测试摘要：序列号格式/并发1000无重复/99999999边界耗尽不回绕/并发耗尽；
  9型号参数与DJIProductDemo 100%一致；枚举严格解析；金额不变量；U+200C清洗
设计偏差：无（严格按蓝图；ProductCategory 为 M1 前置需提前落地）
伴学交接：见 docs/HANDOFF_FOR_CHATGPT.md §M1 §M2
下一步：M3

### 2026-11-07｜M3：七基地抽象工厂与集中路由
原版评分映射：任务4(15分)
Git 提交 SHA：b6db2ab
真正修改的文件：新增 factory/{AbstractBaseFactory, 7个基地工厂, ProductionRouter}
执行的命令与退出码：mvn -B test → 0
真实测试摘要：15种地区×类别组合与任务书职责表逐格一致；9类越权请求显式拒绝；
  空参数无效组合报错；orderDrone 全流程序列号唯一
设计偏差：无
伴学交接：HANDOFF §M3

### 2026-11-07｜M4+M5：促销策略与 JSON 导入校验
原版评分映射：任务5(15分)+任务2(10分)
Git 提交 SHA：432b9b2
真正修改的文件：promotion/ 7 个类；repository/ 4 个类；sale/ 2 个 record；
  fixtures: invalid_orders.json(14类错误)/malformed.json/not_array.json
执行的命令与退出码：mvn -B test → 0
真实测试摘要：真实14笔订单最优方案/优惠额/应付额与 sysgui.png 逐笔一致（含并列竞争0001、
  满减获胜0003-0005）；满减无档数上限(D-001)；并列优先满减>满件>无优惠；大陆非CNY禁满减；
  0元减免归入无优惠；坏数据14处错误全部可定位(订单号+明细序号+字段+原因)
设计偏差：PromotionType 置于 com.dji.sale（依赖单向约束，见 DECISIONS.md 实施备注）
伴学交接：HANDOFF §M4 §M5

### 2026-11-07｜M6：Stream 查询统计与生产编排服务
原版评分映射：任务7(15分)+任务4/6服务层
Git 提交 SHA：7b78546
真正修改的文件：service/ 6 个类 + 3 个测试类
执行的命令与退出码：mvn -B test → 0（全量 58 tests）
真实测试摘要：多条件组合筛选(含U+200C客户名命中)/groupingBy地区统计/分币种优惠汇总
  (CNY 94979.14 等5组精确值)/基地预计需求42架(蓝图§5.4七基地分布)/型号需求降序；
  全量生产42架 SN 1..42连续唯一；幂等跳过；路由失败零消耗；中途失败部分保留+重试补产；
  架构解耦守护（核心六包零Swing/AWT）
设计偏差：无
伴学交接：HANDOFF §M6
```

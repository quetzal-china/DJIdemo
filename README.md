# 大疆全球订单生产管理系统｜README 与设计说明

> Java 21 + Maven + Swing。原版任务书：`../G班机试2.md`（业务/评分权威）；架构基线：`docs/ARCHITECTURE_BLUEPRINT.md` v1.0（已获批准）。
> 本文档同时承担提交要求中的"设计说明"与"自动化测试及运行说明"。

## 一、运行与测试

```bash
# 运行全部 62 项自动化测试（无头环境可跑）
mvn -B clean test

# 启动 GUI（推荐：先把运行期依赖拷到 target/dependency，再用通配符类路径）
mvn -q compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
java -cp "target/classes:target/dependency/*" com.dji.ui.DJIOrderApp

# 或指定订单文件（验收场景 7 可换成含错误数据的 JSON，如 src/test/resources/fixtures/invalid_orders.json）
java -cp "target/classes:target/dependency/*" com.dji.ui.DJIOrderApp path/to/order.json
```

> ⚠️ 不要写成 `-cp "target/classes:~/.m2/..."`——bash 双引号内的 `~` **不会展开**，
> 会导致 `NoClassDefFoundError: com/alibaba/fastjson2/JSONException`。
> 若要直接引用本地仓库 jar，请用 `$HOME` 或绝对路径，且注意 fastjson2 与 junit 等可能不止一个 jar。

测试报告在 `target/surefire-reports/`。

## 二、架构分层（四层六包 + UI）

```text
ui (Swing: DJIOrderApp/MainFrame/OrderTableModel, SwingWorker×2)
 ↓ 调用
service (OrderQueryService / OrderStatisticsService / ProductionService / SystemSummary)
 ↓
promotion (策略模式: PromotionStrategy + 4 策略 + Comparator)   factory (抽象工厂: 7 基地 + ProductionRouter)
 ↓                                                              ↓
sale (领域: Order/OrderItem/Customer/5枚举/PromotionResult/ProductionTask/业务异常)
 ↑
repository (JsonOrderRepository / OrderValidator)   product (DJIProduct 继承树 / SerialNumberGenerator / DroneSpecRegistry)
```

**依赖硬约束**：核心六包严禁 import `javax.swing.*`/`java.awt.*`，由 `ArchitectureDecouplingTest` 编译级守护；GUI 监听器只收集输入→调服务→刷新视图，零业务规则。

**与教师代码的兼容**：`DJIProductDemo`（序列号 API 绑定）与 `DJIPromotion`（`com.dji.sale.Order` 签名绑定）**零改动**——`git diff 8f6993b` 可证教师 8 个文件全部原样。

## 三、设计模式与关键决策

| 模式/决策 | 位置 | 说明 |
|---|---|---|
| **抽象工厂** | `factory.AbstractBaseFactory` + 7 基地工厂；`ProductionRouter` 集中路由 | 每基地只产职责内类别，越权抛 `ProductionRoutingException`；路由表全局唯一（15 组合有测试逐格对照） |
| **策略模式** | `promotion.PromotionStrategy`（双层接口）+ 4 策略 + `PromotionOptionComparator` | 教师 `DJIPromotion#calculatePromotion` 经 default 方法兼容；结构化结果走 `evaluate()` |
| **单例** | `product.SerialNumberGenerator` | AtomicLong 无锁递增，`DJI-00000001..99999999`，耗尽显式异常不回绕；**进程内唯一**（D-008：跨 JVM 重启重新计数，如实披露） |
| **模板方法（教师）** | `DJIFactory#orderDrone` | 装配→静态测试→动态测试→包装，生产编排直接复用 |
| **序列号封装（架构修正1）** | `DroneSpecRegistry#createDrone(type)` | 业务侧唯一创建入口，序列号注册表内部申请；教师构造器仍公开收 sn（API 兼容限制，如实披露） |
| **生产一致性（架构修正2/3）** | `ProductionService` | ①路由前置原子校验（失败零消耗）②中途失败部分入档不回收、不虚报整单成功 ③SUCCESS 幂等跳过、PARTIAL/FAILED 重试只补产 ④汇总区分"预计需求/实际已产"两口径 |
| **BigDecimal 口径** | 全金额链路 | `scale=2, HALF_UP`（5 币种一致，与 sysgui.png 14 笔期望值反证一致）；恒等式 原价−优惠=应付 有测试 |
| **错误策略** | `JsonOrderRepository` | 默认跳过单笔无效订单+结构化错误汇总（D-006）；顶层损坏整份失败；严格模式 `loadOrdersStrict` 备选 |
| **U+200C 清洗（ADR-003）** | `sale.Customer` | `rawName` 审计留原值；`normalizedName` 剥 `\p{Cf}`；查询/分组/展示统一 normalized |

## 四、任务 7 函数式/Stream 位置索引（类#方法）

| 任务 7 子项 | 位置 | 算子 |
|---|---|---|
| 7.1 多条件筛选 | `OrderQueryService#searchOrders`（`OrderFilter` 谓词组合） | filter, sorted, collect |
| 7.2 groupingBy 统计 | `OrderStatisticsService#countOrdersByRegion`（地区→单数）；`#countUnitsByProductionBase`（基地→架数）；`#sumDiscountByCurrency`（币种→优惠额） | groupingBy + counting/summingInt/mapping+reducing |
| 7.3 ≥3 种算子 | 上两行 + `#sumAmountByCurrency`（reduce 累加）、`#sumQuantityByProductModel` | map/flatMap/filter/sorted/reduce/collect 全部 6 种 |
| 7.4 自定义函数式接口 | `service.OrderFilter`（@FunctionalInterface，`and/or/negate` 缺省方法 + `byKeyword/byRegion/byStatus` 静态工厂） | 谓词组合 |
| 7.5 方法引用/Lambda | `Order::getRegion`、`criteria.toFilter()::matches`、`promoCache::get`、`OrderItem::getQuantity`、`PromotionResult::getDiscountAmount` | 排序/映射/回调 |
| 7.6 flatMap 扁平化 | `OrderStatisticsService#sumQuantityByProductModel`；`OrderQueryService#findItemsByCustomerKeyword/#findItemsWithQuantityAtLeast` | flatMap+filter+sorted |

## 五、GUI 线程方案（任务 6.7）

- `DJIOrderApp#main` 经 `EventQueue.invokeLater` 在 EDT 建窗；
- 文件加载 = `OrderLoadWorker extends SwingWorker<OrderLoadResult, Void>`（后台读 JSON+预计算促销，`done()` 在 EDT 刷表/汇总/错误页）；
- 生产处理 = `ProductionWorker extends SwingWorker<BatchProductionResult, ProductionOutcome>`（后台逐单生产，`process()` 在 EDT 实时进度，`done()` 统一刷新）；
- 任务进行中 `setBusy(true)` 禁用 5 个重入按钮；状态栏常驻反馈，弹窗仅用于文件级加载失败（附排查建议），不替代状态反馈。

## 六、经测试发现并修正的真实问题（复现—修复—回归）

1. **并发测试死锁（SerialNumberGeneratorTest）**：初版用 `invokeAll()`（阻塞到全部任务完成）+ `CountDownLatch`（完成后才放行）→ 自我死锁、测试超时挂死。修复：改 `submit()` 收集 Future 后再 `countDown()`。回归：并发 1000 无重复且恰好覆盖 1..1000。
2. **促销缓存键型错误（MainFrame#updateSummary，GUI 冒烟发现）**：`promoCache::get` 被当作 `Function<Order,PromotionResult>` 传入——`Map.get` 参数为 Object 所以编译通过，但实际拿 Order 对象当 key，恒返回 null → 汇总 NPE。无头测试未覆盖该 GUI 路径，WSLg 实机冒烟启动暴露。修复：显式 `order -> promoCache.get(order.getOrderId())` 并留注释。回归：GUI 冒烟 15 秒零异常输出。
3. **测试期望值算错（PromotionCalculationTest）**：订单 0004（79888 元）满件优惠应为 7988.80，首版误写为订单 0005 的 7711.50；2000 元 2 件订单的"档位断言"误用最优选择（满减 1700 合法地击败满件 1720）。修复：档位断言直连单一策略 `evaluate()`；金额逐笔复核。回归：14 笔与 sysgui.png 逐笔一致。
4. **位置断言依赖并发 Map 遍历序（ProductionServiceTest）**：档案存于 ConcurrentHashMap，遍历序≠生产序，`serials.get(0)` 断言脆弱。修复：排序后断言连续覆盖 DJI-00000001..42。

## 七、诚实披露（限制与边界）

- 序列号为**进程内**唯一（D-008 默认方案），跨 JVM 重启从 1 重新计数——未虚称持久化/分布式唯一；
- 教师产品构造器公开接受 sn 参数，无法在不破坏教师 API 的前提下收窄——以"注册表/工厂为唯一业务创建入口"约束并测试；
- G1–G7 交互场景需在真实窗口操作：本环境完成启动冒烟（两次，零异常），**交互验收留待提交者实机执行**（WSLg 截屏工具不可用，无伪造截图）；
- `order.json` 全部 14 笔为干净数据；坏文件校验由 `src/test/resources/fixtures/` 的 14 类错误样本覆盖（未改动教师数据）。

## 八、文档索引

`docs/00-基线与输入清单.md`（真实数据事实与促销 oracle 表）｜`docs/01-需求追踪矩阵.md`（100 分逐项证据）｜`docs/ARCHITECTURE_BLUEPRINT.md`/`IMPLEMENTATION_PLAN.md`（获批架构）｜`docs/DECISIONS.md`（业务裁决与默认方案）｜`docs/PROGRESS.md`（模块日志）｜`docs/VERIFICATION.md`（真实运行证据）｜`docs/HANDOFF_FOR_CHATGPT.md`（伴学交接）｜`docs/AI使用说明.md`（AI 工具使用与验证责任）

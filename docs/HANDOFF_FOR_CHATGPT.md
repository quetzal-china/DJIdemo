# HANDOFF_FOR_CHATGPT｜伴学交接

> 每模块：真实路径、关键类方法、调用链、测试事实、学习锚点。基于 commit `7b78546`（M1–M6 完成态）。
> 学习方式建议：把本文件 + 对应 commit 链接发给 ChatGPT，从"学习锚点"切入提问。

## M1｜序列号单例与产品参数注册（任务 3，10 分）

**文件**：`src/main/java/com/dji/product/` 下 `SerialNumberGenerator.java`、`SerialNumberExhaustedException.java`、`DroneSpecRegistry.java`
**调用链**：GUI/服务 → `DroneSpecRegistry.createDrone(type)` → 内部 `SerialNumberGenerator.getInstance().nextSerialNumber()` → 反射不到的私有构造单例 → 产品构造器（教师类）。
**测试事实**：`SerialNumberGeneratorTest`（并发 1000 恰好覆盖 DJI-00000001..1000；99999999 边界耗尽后持续抛异常不回绕；并发耗尽 49 成功/其余异常）+ `ProductCreationTest`（9 型号参数 100% 复刻 DJIProductDemo）。`java -cp target/classes com.dji.product.DJIProductDemo` 零修改运行。
**学习锚点**：
1. 为什么用 `AtomicLong.incrementAndGet()` 而不是 `synchronized`？CAS 无锁 vs 锁竞争；
2. 为什么耗尽时要 `decrementAndGet()` 回退？（保持耗尽态，不回绕不重复）
3. `DroneSpecRegistry` 为什么是"参数唯一权威"？（防 9 组参数在 7 个工厂里复制走样）

## M2｜核心领域模型（任务 1，10 分）

**文件**：`src/main/java/com/dji/sale/` —— 枚举 `Region/Currency/OrderStatus/ProductCategory/ProductionBase`，实体 `Customer/OrderItem/Order`，值对象 `PromotionResult/ProductionTask`，异常 `OrderValidationException/ProductionRoutingException`。
**关键设计**：全部不可变（`final` 字段+构造期校验）；`Order` 是聚合根，明细列表 `List.copyOf` 只读；`status` 为唯一可变字段（`volatile`+`synchronized`，D-009 流转）。
**测试事实**：`DomainModelTest` 13 项——构造期不变量（明细金额=单价×数量、订单总额=Σ明细）、`U+200C` 零宽字符清洗（真实订单 0009/0010 的客户名）。
**学习锚点**：
1. 为什么校验放构造器而不是 setter？（不变量在对象出生即成立，聚合根保护）
2. `Customer.rawName` vs `normalizedName` 双字段的意义（审计 vs 业务）；
3. 为什么 `equals` 用 `compareTo==0` 而不是 `equals` 比较 BigDecimal（2.0 vs 2.00）。

## M3｜抽象工厂与生产路由（任务 4，15 分）

**文件**：`src/main/java/com/dji/factory/` —— 教师骨架 `DJIFactory` 零改动；新增 `AbstractBaseFactory`（职责校验+委托注册表）+ 7 个基地工厂 + `ProductionRouter`（唯一路由点）。
**调用链**：`ProductionService` → `ProductionRouter.resolveFactory(region, category)`（先规则1农业→顺德，再 switch 地区）→ 基地工厂 `orderDrone(type)`（教师模板方法：装配→静态测试→动态测试→包装）。
**测试事实**：`FactoryRoutingTest` 7 项——15 种组合逐格对照任务书职责表、9 类越权请求、无效组合显式报错（绝不返回 null）。
**学习锚点**：
1. 抽象工厂 vs 简单工厂：为什么"每个基地一个工厂类"而不是一个大 if-else；
2. 路由为什么必须集中（任务书："不在多个业务模块重复实现"）；
3. exhaustive switch 的好处：新增地区枚举时编译器强制处理路由分支。

## M4｜促销策略与最优选择（任务 5，15 分）

**文件**：`src/main/java/com/dji/promotion/` —— `PromotionStrategy extends DJIPromotion`（双层接口，ADR-004）、4 个具体策略、`PromotionOptionComparator`（并列优先：满减 rank1 > 满件 rank2 > 无优惠 rank3）、`PromotionCalculator`。
**硬规则**：全程 BigDecimal（scale=2, HALF_UP）；大陆满减仅 `CHINA_MAINLAND && CNY`；每满 2000 减 300 无档数上限（D-001）；0 元减免不冒充活动。
**测试事实**：`PromotionCalculationTest` 8 项——**真实 14 笔订单与 sysgui.png 期望逐笔一致**（含订单 0001 满件 8.2 折胜满减 7 档的并列竞争、0003-0005 满减胜满件）；3000 元 1 件时两者应付同为 2700 → 满减按并列规则胜出。
**学习锚点**：
1. `divideToIntegralValue` 求"每满"档数为什么对（2000→1 档，1999.99→0 档）；
2. 策略模式的"开闭"体现：新增活动只加类+注册，不改比较器与计算器；
3. `PromotionType` 为什么放 sale 包（依赖单向 PROMO→SALE，见 DECISIONS.md 实施备注）。

## M5｜JSON 导入与可定位校验（任务 2，10 分）

**文件**：`src/main/java/com/dji/repository/` —— `JsonOrderRepository`（宽松=跳过单笔+错误汇总 D-006；严格=整份拒绝）、`OrderValidator`（静态解析器）、`OrderFileParseException/OrderLoadException`；`sale/OrderValidationError`（record，含 describe()）。
**数据事实**：真实 order.json 14 笔 0 错误；`src/test/resources/fixtures/invalid_orders.json` 覆盖 14 类错误（重复单号/空客户/未知地区/未知币种/非法状态/坏时间/近似型号 Mini 4/零数量/负单价/明细金额不符/订单总额不符/空明细/缺总额），每条错误含记录序号+订单号+明细序号+字段名+原因。
**测试事实**：`JsonOrderRepositoryTest` 5 项。
**学习锚点**：
1. 错误定位结构 record 的设计（字段为什么是这五个）；
2. fastjson2 逐元素 JSONObject 解析 vs 直接映射 List<Order> 的取舍（损坏订单可定位）；
3. "型号精确匹配"为什么拒绝 `Mini 4`（任务书明文：不以名称近似视为同一产品）。

## M6｜Stream 查询统计与生产编排（任务 7，15 分 + 服务层）

**文件**：`src/main/java/com/dji/service/` —— `OrderFilter`（自定义函数式接口，and/or/negate 组合）、`OrderSearchCriteria`、`OrderQueryService`、`OrderStatisticsService`、`SystemSummary`、`ProductionService`。
**任务 7 六项对应位置**：
1. 多条件筛选 → `OrderQueryService.searchOrders`（filter+sorted+collect）；
2. groupingBy → `OrderStatisticsService.countOrdersByRegion`（groupingBy+counting）、`sumDiscountByCurrency`（groupingBy+mapping+reducing）；
3. ≥3 种算子 → map/flatMap/filter/sorted/reduce/collect 全有；
4. 自定义函数式接口 → `OrderFilter`（含静态工厂 byKeyword/byRegion/byStatus）；
5. 方法引用/Lambda → `Order::getRegion`、`criteria.toFilter()::matches`、`promo::best` 等；
6. 明细扁平化 → `countUnitsByProductionBase`/`sumQuantityByProductModel`/`findItemsByCustomerKeyword`（flatMap）。
**生产编排语义（架构修正 2/3 的落实）**：
- 路由前置原子校验（任一明细无法路由→整单失败零消耗）→ `processOrder` 前段；
- 中途失败：部分任务如实入档（PARTIAL）、序列号不回收、订单 FAILED、失败原因入档 → `produceItem` catch 块；
- 幂等：SUCCESS 跳过返回既有任务；FAILED/PARTIAL 重试只补产缺失数量 → `processOrder` 前段+`produceItem(existing)`；
- 统计两口径：`OrderStatisticsService.countUnitsByProductionBase`=预计需求；`ProductionService.producedUnitsByBase`=实际已产。
**测试事实**：`OrderStreamServiceTest` 10 项（分币种优惠 CNY 94979.14/USD 13938.00/EUR 7147.15/JPY 10268100.00/BRL 33599.85 与 D-004 备注一致；基地预计 5/2/21/3/2/2/7=42）+ `ProductionServiceTest` 5 项（42 架 SN 1..42 连续唯一；幂等；路由失败零消耗；中途失败 2/5 保留后重试补产至 5/5）+ `ArchitectureDecouplingTest`（核心六包零 Swing/AWT）。
**学习锚点**：
1. `Collectors.reducing` 三参数版与 `reduce` 的关系；
2. 为什么汇总面板要"按币种分列"优惠总额（禁汇率换算→跨币种相加无意义）；
3. 生产档案为什么以"订单+明细序号"为键做部分保留（对应真实场景：产线故障后恢复）。

## M7 预告（待启动）

Swing GUI：`com.dji.ui` 包，`DJIOrderApp`（main 入口，CLI 参数可选文件）+ `MainFrame` + TableModels + SwingWorker 异步（加载/生产）。视觉方案已提交项目所有者决策后开工。

## M7｜Swing GUI 与异步任务（任务 6，20 分）

**文件**：`src/main/java/com/dji/ui/` —— `DJIOrderApp`（main 入口：EventQueue.invokeLater，args 或默认路径）、`MainFrame`（工具栏+订单表+三 Tab 详情/汇总/错误+状态栏）、`OrderTableModel`（10 列，促销列经 Function 注入）。
**调用链**：按钮/表格选择（EDT）→ SwingWorker.doInBackground（后台）→ `OrderQueryService`/`PromotionCalculator`/`ProductionService`/`OrderStatisticsService` → `done()`/`process()`（EDT）→ TableModel/文本区刷新。
**关键实现**：
- `OrderLoadWorker`：后台读 JSON+预计算促销缓存，done() 一次刷全视图；
- `ProductionWorker`：后台逐单生产，`publish()` 逐单回传进度（"生产进行中 i/N"），幂等跳过由服务层保证；
- `setBusy()`：任务中禁用 5 个重入按钮；状态栏常驻，弹窗仅用于文件级加载失败（含排查建议）；
- 详情 Tab 展示全部候选促销（[候选]/[兜底]/←最优）与生产任务 SN 列表、失败原因与补产提示。
**测试事实**：`OrderTableModelTest` 4 项无头验证 + GUI 冒烟（WSLg 真实启动，第二次起零异常）；**G1–G7 交互验收留待用户实机**（诚实披露：本环境截屏不可用）。
**学习锚点**：
1. 为什么 `promoCache::get` 直接当 `Function<Order,...>` 传给汇总服务会埋雷（Map.get 收 Object → 键型错误编译不报错）——GUI 冒烟抓出的真实 NPE；
2. SwingWorker 三段式：doInBackground(后台)/process(EDT 增量)/done(EDT 终态)；为什么按钮要 setBusy 防重入；
3. TableModel 把"数据"与"JTable 视图"分离后，无头 JUnit 也能测 GUI 数据映射。

## 终验状态（M8）

- `mvn -B clean test` → 62/62 全绿（退出码 0）；
- 需求追踪矩阵（`docs/01-需求追踪矩阵.md`）每项均映射真实类#方法与测试名；G1–G7 标注"待用户实机验收"；
- 教师文件零改动可由 `git diff f9ffbea -- <教师文件>` 复核。

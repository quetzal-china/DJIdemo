# 工程事实摘要｜供 Zed 架构定稿阅读

> 由 ZCode 于 2026-11-07 勘察产出。所有条目均已实际验证（文件/命令/脚本），标"静态分析"者除外。
> 详细展开见同目录 `00-基线与输入清单.md`；运行证据见 `VERIFICATION.md`。

## 一、关键文件位置（工程根 = `djProject/`）

```text
G班机试2/                          ← 考试根目录（Windows: D:\...\G班机试2\）
├── G班机试2.md                    ← 业务/评分权威（8 任务 100 分 + GUI 七场景验收）
├── 大疆机试二_AI主导开发与伴学指导书.md ← Agent 工作权威（十阶段、双权威、待核问题清单）
├── 工作区模板/AGENTS.md            ← 三角色协作协议（Zed 只规划；ZCode 唯一主力开发）
├── sysgui.png                     ← 参考界面截图（含全部 14 笔订单促销期望值，可作测试 oracle）
└── djProject/                     ← Maven 工程根（建议 docs/、AGENTS.md 最终合并于此）
    ├── pom.xml                    ← Java 21；fastjson2 2.0.65；junit-jupiter 6.1.3(test)；surefire 3.5.3
    ├── src/main/resources/order.json ← 真实订单数据（14 笔，顶层数组）
    └── src/main/java/com/dji/
        ├── product/  DJIProduct(抽象基类), ConsumerDrone, EnterpriseDrone, AgriculturalDrone, DJIProductDemo(main)
        ├── factory/  DJIFactory(抽象工厂骨架)
        ├── promotion/ DJIPromotion(策略接口，签名绑定 com.dji.sale.Order)
        └── sale/     ★不存在（教师留空）
```

**环境**：WSL2 + WSLg（`DISPLAY=:0`，Swing 可显示）；Temurin JDK 21.0.12.1 + Maven 3.10.0；**无 Git 仓库**。

## 二、既有类/接口与 API 约束（Zed 设计必须兼容的事实）

| 事实 | 内容 | 对架构的约束 |
|---|---|---|
| `DJIProduct` | 抽象基类：productName/sn/weight(double)/maxFlightTime；构造时顺序调用 InstallBody→InstallMotor→InstallFlightControl→InstallAttachment（模板式打印）；`getProductCategory()` 返回**中文字符串**；`getSeries()` 返回 sn | 产品继承树已定，不重建；"产品类别枚举"需与该字符串并存（或适配） |
| `ConsumerDrone` | 构造 7 参 `(name, sn, weight, flightTime, cameraResolution, hasObstacleAvoidance, transmissionSystem)` | 工厂创建消费级时按此签名 |
| `EnterpriseDrone` | 构造 8 参 `(…, payloadType, hasRTKModule, applicationField, ipRating)` | 同上（行业级） |
| `AgriculturalDrone` | 构造 8 参 `(…, sprayTankCapacity L, maxSprayFlowRate L/min, hasMultispectralCamera, radarSystem)` | 同上（农业级） |
| `DJIFactory` | 三组型号常量（与 JSON 完全一致）；抽象 `buildDrone(String)`；**模板方法 `orderDrone(String)`**：build→displayProductInfo→staticTest→flightTest→packaging，`null` 时 throw 裸 `Exception`；静态 isConsumer/isAgricultural/isEnterprise | 七基地工厂应继承它并实现 buildDrone；生产模拟可直接复用 orderDrone 流程；"未定义路由"需升级为业务异常类型 |
| `DJIPromotion` | `BigDecimal calculatePromotion(com.dji.sale.Order order)` | **接口签名绑死 `com.dji.sale` 包与 `Order` 类名**——要么在该包建 Order，要么改动此接口（改动教师文件需记录为决策） |
| `DJIProductDemo` | 唯一 main；引用 `SerialNumberGenerator.getInstance().nextSerialNumber()` | 序列号单例的**API 已被教师定死**：类名、`getInstance()`、`nextSerialNumber()` |

## 三、DJIProductDemo 权威产品参数（产品创建必须使用）

| 型号 | 类别 | 构造参数（原样） |
|---|---|---|
| Mini 4 PRO | 消费级 | 249.0g，34分钟，8K，避障=是，O4 |
| Neo | 消费级 | 135.0g，18分钟，4K，避障=否，O4 |
| Air 3S | 消费级 | 724.0g，45分钟，8K，避障=是，O4 |
| Avata 2 | 消费级 | 377.0g，23分钟，4K，避障=否，O4 |
| Inspire 3 | 行业级 | 3995.0g，28分钟，禅思  X5S（注：原文双空格），RTK=否，影视，IP45 |
| Matrice 400 | 行业级 | 15800.0g，59分钟，禅思H30，RTK=是，应急抢险、电力巡检、测绘，IP55 |
| Mavic 4 PRO | 行业级 | 1063.0g，51分钟，4/3 CMOS 哈苏相机，RTK=否，影视，IP45 |
| T70 | 农业级 | 52000.0g，3.8小时，药箱50.0L，流量30.0L/min，多光谱=是，有源相控阵雷达 |
| T100 | 农业级 | 73000.0g，3.8小时，药箱75.0L，流量40.0L/min，多光谱=是，有源相控阵雷达 |

注意：产品类中**没有价格字段**——单价只来自 order.json；产品创建按 demo 参数 + 单例 SN。

## 四、真实数据事实（order.json，14 笔）

- 字段：订单级 `orderId(字符串13位数字)/customerName/email/phone/region/currency/status/orderDate(yyyy-MM-dd HH:mm:ss.SSS)/totalAmount/items[]`；明细级 `productType/quantity/unitPrice/totalAmount`。
- region 实际值：`CHINA_MAINLAND / NORTH_AMERICA / EUROPE / ASIA / SOUTH_AMERICA`（`ASIA` 即任务书"亚太"）；currency：`CNY/USD/EUR/JPY/BRL`；status 仅 `PENDING`。
- 型号（原文精确拼写）：`Mini 4 PRO, Neo, Air 3S, Avata 2, Inspire 3, Matrice 400, Mavic 4 PRO, T100, T70`（**注意：非任务书示例的 "Mini 4"/"Air 3"**；与 DJIFactory 常量一致）。
- 金额全部整数自洽（14/14 通过 `明细=单价×数量`、`订单总额=Σ明细` 校验）。
- **数据陷阱（已实证）**：0009 客户名以 U+200C 开头（`‌Elizabeth Taylor`）、0010 客户名以 U+200C 结尾（`Emil‌`）——影响关键字查询/分组/去重。
- 大陆非 CNY 订单：不存在。无不可路由的地区×类别组合（五地区全部有工厂可接；农业级一律顺德）。
- 14 笔订单的最优促销期望值表：见 `00-基线与输入清单.md` §2.5（来源 sysgui.png，已抽样静态验算一致）。

## 五、可复现问题（供测试设计直接引用）

1. **基线编译失败**（`mvn -B clean compile` → 退出码 1）：仅两个根因——缺 `com.dji.sale.Order`（DJIPromotion.java:5,22）、缺 `SerialNumberGenerator`（DJIProductDemo 10 处）。补齐即恢复，属教师预设任务。
2. **促销并列竞争实例**：订单 0001（CNY，3 件 14168 元）：满件 8.2 折 → 应付 11617.76 **优于** 满减 7 档 2100 → 12068（测试断言"满件折扣胜出"）；订单 0003/0004/0005 为满减胜过满件的对照组。
3. **零宽字符实例**：按客户名查 "Elizabeth"（含/不含前缀）与 "Emil" 的精确匹配行为，需设计后用 0009/0010 回归。
4. **JPY 两位小数显示**：参考界面 JPY 也按两位小数显示（`JPY 697800.00`），金额 scale 策略需统一。

## 六、需要 Zed 规划 / 用户裁决的设计问题

| # | 问题 | 事实基础 | 建议（待 Zed/用户定，非已批准） |
|---|---|---|---|
| 1 | `com.dji.sale.Order` 包位置 | DJIPromotion 签名绑死该包 | 在 `com.dji.sale` 建 Order 领域类（零改动教师文件）；架构上其他层用中立包 |
| 2 | U+200C 零宽字符处理策略 | 0009/0010 实证存在 | 查询/比较前剥离 Cf 类不可见字符；原值保留展示（需测试固化） |
| 3 | 订单状态枚举全集 | JSON 仅 `PENDING` | 定义最小集（如 PENDING/PROCESSING/COMPLETED/CANCELLED），解析未知值报错而非静默 |
| 4 | "优惠封顶"（任务 8）vs "不设档数上限"（正文）冲突 | 指导书 §1.4 已列 | 按正文实现"无封顶"，测试证"无人为上限"；冲突记录 DECISIONS 待命题方确认 |
| 5 | 跨币种"优惠总额"展示 | 5 种币种，禁汇率换算 | 汇总面板按币种分列合计（sysgui.png 中未见此栏，呈现方式请用户确认） |
| 6 | BigDecimal 舍入/scale | JSON 全整数；折扣产生 2 位小数 | 统一 scale=2、HALF_UP（与 sysgui.png 显示一致）；JPY 是否 0 位请用户确认 |
| 7 | Git 初始化与首个检查点 | 全工作区无 Git | 用户确认仓库根（建议 `djProject/` 或考试根）后由 ZCode 建 Git，提交点=基线勘察完成 |
| 8 | 促销接口形态 | DJIPromotion 只返回 BigDecimal（减免额） | 任务 5 要求结构化结果（方案名/优惠/应付）→ 需在保留原接口的同时新增 richer 策略接口，请 Zed 定形态 |
| 9 | GUI 线程与耗时任务 | Swing 硬约束 + WSLg 可显示 | SwingWorker 标准方案；无需 JavaFX（任务书默认 Swing 验收） |

## 七、缺失/未知项清单（不得虚构，均待补）

- `SerialNumberGenerator`、`com.dji.sale.Order`、全部领域模型/服务/GUI/测试代码（`src/test` 不存在）——属开发任务。
- 错误样例订单文件（验收场景 7）→ 需自建 fixture（不改原 order.json）。
- GUI 实机运行证据（WSLg 条件具备但零实机记录）。
- 订单状态全集、促销呈现细节等业务裁决（见 §六）。

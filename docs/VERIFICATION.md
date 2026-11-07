# VERIFICATION｜实际构建、测试、GUI 证据

> 2026-11-07 起记录真实运行证据。此前无任何运行记录。

| 时间 | commit/工作树 | 运行环境 | 命令/GUI 动作 | 退出码/观测 | 报告/截图路径 | 状态 |
|---|---|---|---|---|---|---|
| 2026-11-07 | 无 Git 仓库 | WSL2 (Ubuntu), Temurin JDK 21.0.12.1, Maven 3.10.0 | `mvn -B clean compile` | **1**（编译失败） | `/tmp/baseline_compile.log` | 实际失败（基线预期） |

## 测试日志追加模板

```text
### YYYY-MM-DD HH:mm｜模块名
Commit/工作树：
JDK/构建环境：
完整执行命令：
退出码：
通过/失败/跳过数（来自真实报告）：
关键结果/失败日志：
对应原版需求：
是否需要 GUI 实机验证：
结论：实际通过 / 实际失败 / 未执行 / 受环境限制
```

## 已追加记录

```text
### 2026-11-07 09:xx｜阶段0：基线编译
Commit/工作树：无 Git 仓库；目录为教师配发原状（源码零改动）
JDK/构建环境：Temurin OpenJDK 21.0.12.1（SDKMAN）+ Apache Maven 3.10.0，WSL2 x64，网络可用（依赖 fastjson2 2.0.65、junit-jupiter 6.1.3 均解析成功）
完整执行命令：cd djProject && mvn -B clean compile
退出码：1
通过/失败/跳过数：不适用（无测试；compile 阶段失败）
关键结果/失败日志（52 行 ERROR 归并为 2 个根因）：
  1) com/dji/promotion/DJIPromotion.java:[5,20] package com.dji.sale does not exist
     同文件 [22,46] cannot find symbol: class Order
     → 缺 com.dji.sale.Order（教师留空，对应任务 1/2 领域模型）
  2) com/dji/product/DJIProductDemo.java 共 10 处 cannot find symbol: variable SerialNumberGenerator
     → 缺 com.dji.product.SerialNumberGenerator（教师留空，对应任务 3）
  其余 5 个既有类（DJIProduct/ConsumerDrone/EnterpriseDrone/AgriculturalDrone/DJIFactory/DJIPromotion 自身）无语法错误。
对应原版需求：无直接分值；为任务 1–8 的前置基线
是否需要 GUI 实机验证：否（本条为纯构建）
结论：实际失败——且为"设计如此"的基线状态（学生任务类未实现前无法完整编译）；不构成回归
```

## 环境条件记录（静态核查，未运行）

- `src/test/` 不存在 → `mvn test` 当前无可执行测试（未执行，不填通过）。
- WSLg 图形条件具备（`DISPLAY=:0`、`WAYLAND_DISPLAY=wayland-0`、`/mnt/wslg` 存在），Swing GUI 理论可显示；**GUI 实机证据为零**，待实施阶段以真实截图补录。
- `order.json` 原文件校验（Python 脚本只读解析）：14 笔订单金额自洽、字段齐全；含 U+200C 零宽字符 2 处（0009 客户名首字符、0010 客户名尾字符）。校验脚本未改动任何文件。

## 原版 GUI 七场景

在 `docs/01-需求追踪矩阵.md` 的 G1–G7 基础上逐项记录**真实操作**和截图/视频/手工可重现步骤。无 GUI 会话不应填"通过"。（当前：全部未验证）

## 已追加记录（实施阶段 M1–M6）

```text
### 2026-11-07 11:30｜M1–M6 全量回归
Commit/工作树：64e718b（M6）＋前序 d835f66/b6cc38c/25586c9；工作树干净
JDK/构建环境：Temurin OpenJDK 21.0.12.1 + Maven 3.10.0，WSL2
完整执行命令：cd djProject && mvn -B test
退出码：0
通过/失败/跳过数：Tests run: 58, Failures: 0, Errors: 0, Skipped: 0
关键结果：
  - SerialNumberGeneratorTest 5/4+1；并发1000次无重复且恰好覆盖1..1000（含并发耗尽桩测）
  - ProductCreationTest 4：9型号参数与 DJIProductDemo 100% 一致（含"禅思  X5S"双空格）
  - DomainModelTest 13：枚举严格解析/金额不变量/U+200C清洗/不可变集合
  - FactoryRoutingTest 7：15组合路由表/越权拒绝/无效组合/orderDrone唯一SN
  - PromotionCalculationTest 8：真实14笔订单与 sysgui.png 期望逐笔一致；
    满减无档数上限（200万→1000档→减30万）；并列同价满减胜满件（3000元1件）
  - JsonOrderRepositoryTest 5：14笔0错；坏数据14处错误全可定位；malformed/not_array 整份失败
  - OrderStreamServiceTest 10：组合筛选/groupingBy/分币种优惠(CNY 94979.14等)/基地预计42架/型号降序
  - ProductionServiceTest 5：42架 SN 1..42 连续唯一/幂等跳过/路由失败零消耗/中途失败部分保留+补产重试
  - ArchitectureDecouplingTest 1：核心六包零 Swing/AWT import
对应原版需求：任务1/2/3/4/5/7/8（GUI 任务6待 M7）
是否需要 GUI 实机验证：否（本批为无头测试）
结论：实际通过
```

```text
### 2026-11-07 11:20｜教师 DJIProductDemo 零修改运行验证（M1 验收项1）
Commit/工作树：d835f66 及之后
完整执行命令：java -cp target/classes com.dji.product.DJIProductDemo
退出码：0
观测：输出9架无人机完整安装/演示信息，序列号 DJI-00000001..09 由单例自动分配；
  教师源文件零改动
结论：实际通过
```

## 修复记录（经测试发现并修正的真实问题）

1. **SerialNumberGeneratorTest 并发死锁**：初版用 `ExecutorService.invokeAll()`
   （阻塞至全部完成）+ `CountDownLatch`（完成后才放行）→ 自我死锁超时。
   修复：改用 `submit()` 收集 Future 后再 `countDown()`。教训：invokeAll 与
   起跑门闩不能同用。
2. **PromotionCalculationTest 档位断言混淆"最优选择"与"单策略"**：
   2000元/2件订单的满件(1720)合法地被满减(1700)击败——断言档位应直接调用
   对应策略 evaluate()，而非最优选择结果。
3. **ProductionServiceTest 位置断言依赖 Map 遍历序**：档案存于
   ConcurrentHashMap，遍历顺序与生产顺序无关；改为排序后断言连续覆盖 1..42。

```text
### 2026-11-07 11:5x｜M7 GUI 冒烟启动（WSLg 实机）
Commit/工作树：M7 提交
JDK/构建环境：Temurin 21 + WSLg（DISPLAY=:0）
完整执行命令：DISPLAY=:0 timeout 15 java -cp "target/classes:<fastjson2>" com.dji.ui.DJIOrderApp
退出码：124（进程存活至超时被杀 = 窗口正常运行）
通过/失败/跳过数：不适用
关键结果：第一次运行暴露 MainFrame#updateSummary NPE——promoCache::get 被用作
  Function<Order,PromotionResult>（Map.get 收 Object，键型错误编译不报错）；
  修复为 order -> promoCache.get(order.getOrderId()) 后重跑：15 秒零异常输出；
  第三次运行 9 秒后尝试 xwd 截屏失败（WSLg X 服务 X_GetImage BadMatch，无截屏能力）
对应原版需求：任务 6（G1 启动加载场景的启动部分）
是否需要 GUI 实机验证：是——G1–G7 交互验收待提交者实机执行（无伪造截图）
结论：启动冒烟实际通过；交互验收待用户
```

```text
### 2026-11-07 12:0x｜M1–M7 最终全量回归（含 OrderTableModelTest）
Commit/工作树：M7/M8 提交
完整执行命令：mvn -B clean test
退出码：0
通过/失败/跳过数：Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
关键结果：新增 ui.OrderTableModelTest 4 项（表格结构/单元格值/占位符与状态刷新/筛选联动）；
  C1 约束 grep：sale/promotion/repository/service 包 double|float 零命中
对应原版需求：任务 1–5、7、8 全量；任务 6 模型层
是否需要 GUI 实机验证：交互场景待用户
结论：实际通过
```

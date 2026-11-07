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

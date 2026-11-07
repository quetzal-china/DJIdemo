package com.dji.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.dji.factory.DJIFactory;
import com.dji.factory.ProductionRouter;
import com.dji.product.DJIProduct;
import com.dji.product.DroneSpecRegistry;
import com.dji.product.SerialNumberTestSupport;
import com.dji.sale.Order;
import com.dji.sale.ProductionBase;
import com.dji.sale.ProductionRoutingException;
import com.dji.sale.ProductionTask;
import com.dji.service.ProductionService.ProductionFailureException;
import com.dji.service.ProductionService.ProductionOutcome;
import com.dji.service.ProductionService.RecordStatus;

/**
 * 任务 4/6｜生产编排测试：42 架全量生产、序列号精确连续、
 * 幂等跳过、路由前置失败不消耗序列号、中途失败部分保留与补产重试。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductionServiceTest {

	private List<Order> orders;

	@BeforeAll
	void loadOrders() {
		orders = new com.dji.repository.JsonOrderRepository()
				.loadOrders(Path.of("src/main/resources/order.json"))
				.validOrders();
	}

	@BeforeEach
	void resetSerials() {
		SerialNumberTestSupport.reset();
	}

	@Test
	@DisplayName("全量生产 14 笔订单 → 恰好 42 架，序列号 DJI-00000001..42 连续唯一，基地分布与预计一致")
	void processAllProducesExactly42() {
		ProductionService service = new ProductionService();
		var batch = service.processAll(orders);

		List<String> serials = service.allTasks().stream()
				.flatMap(task -> task.getSerialNumbers().stream()).toList();
		// 档案存储为并发映射，遍历顺序与生产顺序无关；排序后应恰好连续覆盖 1..42
		List<String> sortedSerials = serials.stream().sorted().toList();
		Map<ProductionBase, Integer> produced = service.producedUnitsByBase();

		assertAll(
				() -> assertEquals(14, batch.totalOrders()),
				() -> assertEquals(14, batch.successCount()),
				() -> assertEquals(0, batch.failedCount()),
				() -> assertEquals(42, serials.size()),
				() -> assertEquals(42, new HashSet<>(serials).size(), "序列号不得重复"),
				() -> assertEquals("DJI-00000001", sortedSerials.get(0)),
				() -> assertEquals("DJI-00000042", sortedSerials.get(41)),
				() -> assertEquals(42, new HashSet<>(sortedSerials).size()),
				() -> assertTrue(serials.containsAll(sortedSerials)),
				() -> assertEquals(42, service.producedUnitsTotal()),
				() -> assertEquals(5, produced.get(ProductionBase.SHENZHEN)),
				() -> assertEquals(2, produced.get(ProductionBase.DONGGUAN)),
				() -> assertEquals(21, produced.get(ProductionBase.SHUNDE)),
				() -> assertEquals(3, produced.get(ProductionBase.NAGOYA)),
				() -> assertEquals(2, produced.get(ProductionBase.AMSTERDAM)),
				() -> assertEquals(2, produced.get(ProductionBase.STUTTGART)),
				() -> assertEquals(7, produced.get(ProductionBase.FREMONT)),
				() -> assertEquals(14, orders.stream()
						.filter(o -> o.getStatus() == com.dji.sale.OrderStatus.PROCESSED).count()));
	}

	@Test
	@DisplayName("幂等：重复全量生产不重复消耗序列号、不翻倍统计")
	void repeatedProcessingIsIdempotent() {
		ProductionService service = new ProductionService();
		service.processAll(orders);
		int serialsAfterFirst = service.allTasks().stream()
				.mapToInt(task -> task.getSerialNumbers().size()).sum();

		var second = service.processAll(orders);
		int serialsAfterSecond = service.allTasks().stream()
				.mapToInt(task -> task.getSerialNumbers().size()).sum();

		assertAll(
				() -> assertEquals(14, second.successCount(), "重复处理全部按幂等跳过成功"),
				() -> assertEquals(serialsAfterFirst, serialsAfterSecond, "不得重复消耗序列号"),
				() -> assertEquals(42, serialsAfterSecond),
				() -> assertTrue(second.outcomes().stream()
						.allMatch(ProductionOutcome::success)),
				() -> assertTrue(second.outcomes().stream()
						.anyMatch(o -> o.message().contains("幂等跳过"))),
				() -> assertEquals(42, service.producedUnitsTotal(), "基地产量不得翻倍"));
	}

	@Test
	@DisplayName("路由前置校验：无法路由的整单失败且零序列号消耗")
	void routingPreCheckConsumesNothing() {
		// 桩路由器：顺德工厂路由整体不可用 → 含农业级明细的订单整单失败
		ProductionRouter brokenRouter = new ProductionRouter() {
			@Override
			public DJIFactory factoryFor(ProductionBase base) {
				if (base == ProductionBase.SHUNDE) {
					throw new ProductionRoutingException("顺德工厂（测试桩）不可用");
				}
				return super.factoryFor(base);
			}
		};
		ProductionService service = new ProductionService(brokenRouter);
		Order agricultural = orders.stream()
				.filter(o -> o.getOrderId().equals("2025102900006"))
				.findFirst().orElseThrow();

		ProductionOutcome outcome = service.processOrder(agricultural);

		assertAll(
				() -> assertTrue(!outcome.success()),
				() -> assertEquals(RecordStatus.FAILED, outcome.recordStatus()),
				() -> assertTrue(outcome.tasksThisRun().isEmpty()),
				() -> assertEquals(0, service.producedUnitsTotal(), "路由失败不得消耗序列号"),
				() -> assertEquals(com.dji.sale.OrderStatus.FAILED, agricultural.getStatus()),
				() -> assertTrue(service.getRecord("2025102900006").getFailureReason().contains("无法路由")));
	}

	@Test
	@DisplayName("中途失败：部分完成如实入档、序列号不回收、重试只补产缺失数量")
	void midProductionFailureKeepsPartialAndRetriesRemainder() {
		// 桩工厂：顺德工厂第 3 次及以后可正常生产（前 2 次正常，制造第 3 件故障一次）
		DJIFactory flakyShunde = new DJIFactory() {
			private int calls = 0;

			@Override
			public DJIProduct buildDrone(String type) {
				calls++;
				if (calls == 3) {
					throw new IllegalStateException("模拟产线设备故障（测试桩）");
				}
				return DroneSpecRegistry.createDrone(type);
			}
		};
		ProductionRouter stubRouter = new ProductionRouter() {
			@Override
			public DJIFactory factoryFor(ProductionBase base) {
				return base == ProductionBase.SHUNDE ? flakyShunde : super.factoryFor(base);
			}
		};
		ProductionService service = new ProductionService(stubRouter);
		Order t100Order = orders.stream()
				.filter(o -> o.getOrderId().equals("2025102900006")) // T100 × 5
				.findFirst().orElseThrow();

		// 第一次处理：前 2 架成功，第 3 架故障 → PARTIAL，保留 2 架
		ProductionFailureException failure = assertThrows(ProductionFailureException.class,
				() -> service.processOrder(t100Order));
		var record = failure.getRecord();
		List<String> partialSerials = record.getTasks().stream()
				.flatMap(task -> task.getSerialNumbers().stream()).toList();

		assertAll("第一次：部分成功不得虚报整单成功",
				() -> assertEquals(RecordStatus.PARTIAL, record.getStatus()),
				() -> assertEquals(2, partialSerials.size()),
				() -> assertEquals("DJI-00000001", partialSerials.get(0)),
				() -> assertEquals("DJI-00000002", partialSerials.get(1)),
				() -> assertEquals(com.dji.sale.OrderStatus.FAILED, t100Order.getStatus()),
				() -> assertTrue(record.getFailureReason().contains("模拟产线设备故障")),
				() -> assertEquals(2, service.producedUnitsTotal(), "已产 2 架如实计入"));

		// 重试：只补产缺失的 3 架（已完成部分不重复生产），序列号继续递增不回收
		ProductionOutcome retry = service.processOrder(t100Order);

		List<String> finalSerials = service.getRecord("2025102900006").getTasks().stream()
				.flatMap(task -> task.getSerialNumbers().stream()).toList();
		assertAll("重试：补产完成",
				() -> assertTrue(retry.success()),
				() -> assertEquals(RecordStatus.SUCCESS, retry.recordStatus()),
				() -> assertEquals(5, finalSerials.size()),
				() -> assertEquals(5, new HashSet<>(finalSerials).size(), "补产序列号不得与已产重复"),
				() -> assertTrue(finalSerials.containsAll(partialSerials), "已完成部分的序列号保留"),
				() -> assertEquals("DJI-00000003", finalSerials.get(2), "补产从耗尽点继续"),
				() -> assertEquals("DJI-00000005", finalSerials.get(4)),
				() -> assertEquals(5, service.producedUnitsTotal()),
				() -> assertEquals(com.dji.sale.OrderStatus.PROCESSED, t100Order.getStatus()));

		// 再重复处理 → 幂等跳过
		ProductionOutcome again = service.processOrder(t100Order);
		assertTrue(again.message().contains("幂等跳过"));
	}

	@Test
	@DisplayName("批量生产中单笔失败不影响后续订单继续处理")
	void batchContinuesAfterSingleFailure() {
		ProductionRouter brokenRouter = new ProductionRouter() {
			@Override
			public DJIFactory factoryFor(ProductionBase base) {
				if (base == ProductionBase.SHUNDE) {
					throw new ProductionRoutingException("顺德工厂（测试桩）不可用");
				}
				return super.factoryFor(base);
			}
		};
		ProductionService service = new ProductionService(brokenRouter);
		var batch = service.processAll(orders);

		// 5 笔含农业级明细的订单失败（0006/0009/0011/0013/0014），其余 9 笔成功
		assertAll(
				() -> assertEquals(9, batch.successCount()),
				() -> assertEquals(5, batch.failedCount()),
				() -> assertTrue(batch.outcomes().stream()
						.filter(o -> !o.success())
						.allMatch(o -> o.message().contains("无法路由"))),
				() -> assertEquals(9, orders.stream()
						.filter(o -> o.getStatus() == com.dji.sale.OrderStatus.PROCESSED).count()));
	}
}

package com.dji.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.dji.factory.ProductionRouter;
import com.dji.repository.JsonOrderRepository;
import com.dji.sale.Currency;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;
import com.dji.sale.ProductionBase;
import com.dji.sale.PromotionResult;
import com.dji.sale.Region;

/**
 * 任务 7｜Stream 与函数式查询统计测试（真实 14 笔订单数据）。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderStreamServiceTest {

	private List<Order> orders;
	private OrderQueryService queryService;
	private OrderStatisticsService statisticsService;
	private PromotionCalculatorHolder promo;
	private ProductionRouter router;

	/** 促销缓存（订单 → 最优结果），供统计函数复用。 */
	private static final class PromotionCalculatorHolder {
		private final com.dji.promotion.PromotionCalculator calculator =
				new com.dji.promotion.PromotionCalculator();

		PromotionResult best(Order order) {
			return calculator.evaluateBestPromotion(order);
		}
	}

	@BeforeAll
	void setUp() {
		orders = new JsonOrderRepository()
				.loadOrders(Path.of("src/main/resources/order.json"))
				.validOrders();
		queryService = new OrderQueryService();
		statisticsService = new OrderStatisticsService();
		promo = new PromotionCalculatorHolder();
		router = new ProductionRouter();
	}

	@Test
	@DisplayName("任务7.1：关键字+地区+状态多条件组合筛选（AND 语义）")
	void multiCriteriaSearch() {
		// 客户"李四" + 中国大陆 + 待处理 → 0002、0005
		List<Order> result = queryService.searchOrders(orders,
				new OrderSearchCriteria("李四", Region.CHINA_MAINLAND, com.dji.sale.OrderStatus.PENDING),
				OrderQueryService.BY_ORDER_ID_ASC);
		assertEquals(List.of("2025102900002", "2025102900005"),
				result.stream().map(Order::getOrderId).toList());

		// 关键字命中订单号前缀 + 地区筛选
		List<Order> byOrderId = queryService.searchOrders(orders,
				new OrderSearchCriteria("202510290001", Region.EUROPE, null),
				OrderQueryService.BY_ORDER_ID_ASC);
		assertEquals(List.of("2025102900010", "2025102900011"),
				byOrderId.stream().map(Order::getOrderId).toList());

		// 组合无匹配 → 空结果（界面应有"无匹配"反馈的数据来源）
		assertTrue(queryService.searchOrders(orders,
				new OrderSearchCriteria("不存在的客户", Region.ASIA, null), null).isEmpty());
	}

	@Test
	@DisplayName("任务7.4+ADR-003：规范化客户名可命中含 U+200C 零宽字符的真实订单")
	void keywordMatchesZeroWidthNormalizedNames() {
		assertAll(
				() -> assertEquals(1, queryService.searchOrders(orders,
						new OrderSearchCriteria("Elizabeth", null, null), null).size()),
				() -> assertEquals(1, queryService.searchOrders(orders,
						new OrderSearchCriteria("emil", null, null), null).size()),
				() -> assertEquals("2025102900010", queryService.searchOrders(orders,
						new OrderSearchCriteria("Emil", null, null), null).get(0).getOrderId()));
	}

	@Test
	@DisplayName("任务7.2：groupingBy 统计各地区订单数")
	void countOrdersByRegion() {
		Map<Region, Long> counted = statisticsService.countOrdersByRegion(orders);
		assertAll(
				() -> assertEquals(6, counted.get(Region.CHINA_MAINLAND)),
				() -> assertEquals(3, counted.get(Region.NORTH_AMERICA)),
				() -> assertEquals(2, counted.get(Region.EUROPE)),
				() -> assertEquals(2, counted.get(Region.ASIA)),
				() -> assertEquals(1, counted.get(Region.SOUTH_AMERICA)),
				() -> assertEquals(14, counted.values().stream().mapToLong(Long::longValue).sum()));
	}

	@Test
	@DisplayName("任务7.6：flatMap 扁平化统计各基地预计需求量（蓝图 §5.4 静态核验表）")
	void plannedUnitsByBase() {
		Map<ProductionBase, Integer> planned =
				statisticsService.countUnitsByProductionBase(orders, router);
		int total = planned.values().stream().mapToInt(Integer::intValue).sum();
		assertAll(
				() -> assertEquals(5, planned.get(ProductionBase.SHENZHEN)),
				() -> assertEquals(2, planned.get(ProductionBase.DONGGUAN)),
				() -> assertEquals(21, planned.get(ProductionBase.SHUNDE)),
				() -> assertEquals(3, planned.get(ProductionBase.NAGOYA)),
				() -> assertEquals(2, planned.get(ProductionBase.AMSTERDAM)),
				() -> assertEquals(2, planned.get(ProductionBase.STUTTGART)),
				() -> assertEquals(7, planned.get(ProductionBase.FREMONT)),
				() -> assertEquals(42, total));
	}

	@Test
	@DisplayName("任务7.2+D-004：分币种汇总优惠总额（严禁跨币种混加）")
	void discountTotalsByCurrency() {
		Map<Currency, BigDecimal> discounts =
				statisticsService.sumDiscountByCurrency(orders, promo::best);
		assertAll(
				() -> assertEquals(0, discounts.get(Currency.CNY).compareTo(new BigDecimal("94979.14"))),
				() -> assertEquals(0, discounts.get(Currency.USD).compareTo(new BigDecimal("13938.00"))),
				() -> assertEquals(0, discounts.get(Currency.EUR).compareTo(new BigDecimal("7147.15"))),
				() -> assertEquals(0, discounts.get(Currency.JPY).compareTo(new BigDecimal("10268100.00"))),
				() -> assertEquals(0, discounts.get(Currency.BRL).compareTo(new BigDecimal("33599.85"))),
				() -> assertEquals(5, discounts.size()));
	}

	@Test
	@DisplayName("任务7.3+7.5：reduce 汇总应付总额（分币种）")
	void payableTotalsByCurrency() {
		Map<Currency, BigDecimal> payables = statisticsService.sumAmountByCurrency(orders,
				order -> promo.best(order).getPayableAmount());
		// CNY 应付合计 = Σ sysgui 应付列 (11617.76+1169.10+8788+68188+65715+271996)
		assertEquals(0, payables.get(Currency.CNY).compareTo(new BigDecimal("427473.86")));
		// USD 应付合计 = 2204.90+15199.05+72244.05
		assertEquals(0, payables.get(Currency.USD).compareTo(new BigDecimal("89648.00")));
	}

	@Test
	@DisplayName("任务7.6：按型号扁平化统计需求量并降序（合计 42 架）")
	void quantityByModelSortedDesc() {
		Map<String, Integer> byModel = statisticsService.sumQuantityByProductModel(orders);
		assertAll(
				() -> assertEquals(11, byModel.get("T100")),
				() -> assertEquals(10, byModel.get("T70")),
				() -> assertEquals(4, byModel.get("Inspire 3")),
				() -> assertEquals(5, byModel.get("Mini 4 PRO")),
				() -> assertEquals(2, byModel.get("Neo")),
				() -> assertEquals(1, byModel.get("Mavic 4 PRO")),
				() -> assertEquals(9, byModel.size()),
				() -> assertEquals(42, byModel.values().stream().mapToInt(Integer::intValue).sum()));
		// 降序校验
		List<Integer> values = List.copyOf(byModel.values());
		assertTrue(values.stream().sorted(java.util.Comparator.reverseOrder()).toList().equals(values),
				"必须按数量降序: " + byModel);
	}

	@Test
	@DisplayName("任务7.6：flatMap 筛选指定客户的全部商品明细")
	void itemsByCustomerKeyword() {
		List<OrderItem> items = queryService.findItemsByCustomerKeyword(orders, "李四");
		assertEquals(2, items.size());
		assertEquals("Neo", items.get(0).getProductType());
		assertEquals("Matrice 400", items.get(1).getProductType());

		// 大额明细筛选（数量 ≥ 5）并降序
		List<OrderItem> bulk = queryService.findItemsWithQuantityAtLeast(orders, 5);
		assertEquals(2, bulk.size()); // T70×6(0009), T100×5(0006)
		assertTrue(bulk.get(0).getQuantity() >= bulk.get(bulk.size() - 1).getQuantity());
	}

	@Test
	@DisplayName("汇总快照：预计需求 42 架 ≠ 实际已生产 0 架（未生产时，架构修正3）")
	void summaryDistinguishesPlannedAndProduced() {
		ProductionService productionService = new ProductionService();
		SystemSummary summary = statisticsService.buildSummary(orders, promo::best, router,
				productionService);
		assertAll(
				() -> assertEquals(42, summary.plannedUnitsTotal()),
				() -> assertEquals(0, summary.producedUnitsTotal()),
				() -> assertTrue(summary.producedUnitsByBase().isEmpty()),
				() -> assertEquals(14, summary.totalOrders()),
				() -> assertEquals(0, summary.processedOrderCount()));
	}

	@Test
	@DisplayName("订单原价合计与订单数一致（防汇总丢单）")
	void amountsCrossCheck() {
		Map<Currency, BigDecimal> originals = statisticsService.sumAmountByCurrency(orders,
				Order::getOriginalAmount);
		// 6 笔 CNY 原价: 14168+1299+9988+79888+77115+339995 = 522453
		assertEquals(0, originals.get(Currency.CNY).compareTo(new BigDecimal("522453.00")));
	}
}

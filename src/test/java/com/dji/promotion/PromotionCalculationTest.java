package com.dji.promotion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.dji.product.DroneSpecRegistry;
import com.dji.repository.JsonOrderRepository;
import com.dji.sale.Currency;
import com.dji.sale.Customer;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;
import com.dji.sale.Region;

/**
 * 任务 5｜促销策略与最优选择测试。
 * 核心断言：真实 order.json 全部 14 笔订单的最优方案、优惠额、应付额
 * 与 sysgui.png 期望表逐笔一致（蓝图中已由静态验算复核的测试 oracle）。</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PromotionCalculationTest {

	private List<Order> orders;
	private Map<String, Order> ordersById;
	private PromotionCalculator calculator;

	@BeforeAll
	void loadRealOrders() {
		calculator = new PromotionCalculator();
		orders = new JsonOrderRepository()
				.loadOrders(Path.of("src/main/resources/order.json"))
				.validOrders();
		ordersById = orders.stream()
				.collect(Collectors.toMap(Order::getOrderId, Function.identity()));
	}

	@Test
	@DisplayName("真实 14 笔订单的最优方案/优惠额/应付额与 sysgui.png 期望表逐笔一致")
	void allFourteenOrdersMatchReferenceScreenshot() {
		record Expected(String orderId, PromotionType type, String discount, String payable) {
		}
		List<Expected> expected = List.of(
				new Expected("2025102900001", PromotionType.MAINLAND_TIERED_DISCOUNT, "2550.24", "11617.76"),
				new Expected("2025102900002", PromotionType.MAINLAND_TIERED_DISCOUNT, "129.90", "1169.10"),
				new Expected("2025102900003", PromotionType.MAINLAND_FULL_REDUCTION, "1200.00", "8788.00"),
				new Expected("2025102900004", PromotionType.MAINLAND_FULL_REDUCTION, "11700.00", "68188.00"),
				new Expected("2025102900005", PromotionType.MAINLAND_FULL_REDUCTION, "11400.00", "65715.00"),
				new Expected("2025102900006", PromotionType.MAINLAND_TIERED_DISCOUNT, "67999.00", "271996.00"),
				new Expected("2025102900007", PromotionType.OVERSEAS_TIERED_DISCOUNT, "389.10", "2204.90"),
				new Expected("2025102900008", PromotionType.OVERSEAS_TIERED_DISCOUNT, "799.95", "15199.05"),
				new Expected("2025102900009", PromotionType.OVERSEAS_TIERED_DISCOUNT, "12748.95", "72244.05"),
				new Expected("2025102900010", PromotionType.OVERSEAS_TIERED_DISCOUNT, "189.70", "1707.30"),
				new Expected("2025102900011", PromotionType.OVERSEAS_TIERED_DISCOUNT, "6957.45", "39425.55"),
				new Expected("2025102900012", PromotionType.OVERSEAS_TIERED_DISCOUNT, "69780.00", "628020.00"),
				new Expected("2025102900013", PromotionType.OVERSEAS_TIERED_DISCOUNT, "10198320.00", "57790480.00"),
				new Expected("2025102900014", PromotionType.OVERSEAS_TIERED_DISCOUNT, "33599.85", "190399.15"));
		assertEquals(14, orders.size());
		assertAll(expected.stream().map(e -> () -> {
			PromotionResult best = calculator.evaluateBestPromotion(ordersById.get(e.orderId()));
			assertAll(e.orderId(),
					() -> assertEquals(e.type(), best.getType(), "方案不符"),
					() -> assertEquals(0, best.getDiscountAmount().compareTo(new BigDecimal(e.discount())),
							"优惠额不符"),
					() -> assertEquals(0, best.getPayableAmount().compareTo(new BigDecimal(e.payable())),
							"应付额不符"),
					() -> assertEquals(0, best.getPayableAmount().compareTo(
							best.getOriginalAmount().subtract(best.getDiscountAmount())),
							"恒等式 原价-优惠=应付 不成立"));
		}));
	}

	@Test
	@DisplayName("大陆满件档位：1/2/3/4+ 件 → 9/8.6/8.2/8 折（4 件封顶）")
	void mainlandTierThresholds() {
		MainlandTieredDiscountStrategy strategy = new MainlandTieredDiscountStrategy();
		assertAll(
				() -> assertStrategyPayable(strategy, order(Region.CHINA_MAINLAND, "M1", 1, "1000"), "900.00"),
				() -> assertStrategyPayable(strategy, order(Region.CHINA_MAINLAND, "M2", 2, "1000"), "1720.00"),
				() -> assertStrategyPayable(strategy, order(Region.CHINA_MAINLAND, "M3", 3, "1000"), "2460.00"),
				() -> assertStrategyPayable(strategy, order(Region.CHINA_MAINLAND, "M4", 4, "1000"), "3200.00"),
				() -> assertStrategyPayable(strategy, order(Region.CHINA_MAINLAND, "M10", 10, "1000"), "8000.00"));
	}

	@Test
	@DisplayName("海外满件档位：1/2/3+ 件 → 9.5/9/8.5 折（3 件封顶）")
	void overseasTierThresholds() {
		OverseasTieredDiscountStrategy strategy = new OverseasTieredDiscountStrategy();
		assertAll(
				() -> assertStrategyPayable(strategy, order(Region.NORTH_AMERICA, "F1", 1, "1000"), "950.00"),
				() -> assertStrategyPayable(strategy, order(Region.NORTH_AMERICA, "F2", 2, "1000"), "1800.00"),
				() -> assertStrategyPayable(strategy, order(Region.NORTH_AMERICA, "F3", 3, "1000"), "2550.00"),
				() -> assertStrategyPayable(strategy, order(Region.NORTH_AMERICA, "F10", 10, "1000"), "8500.00"));
	}

	@Test
	@DisplayName("满减档数：0 档无优惠；每满 2000 减 300；不设档数上限（D-001 默认方案）")
	void fullReductionTiersWithoutCap() {
		assertAll(
				// 0 档：1500 元无满减（0 元减免不作为有效优惠胜出）
				() -> {
					Order small = order(Region.CHINA_MAINLAND, "R0", 1, "1500");
					assertAll(
							() -> assertEquals(0, new MainlandFullReductionStrategy()
									.calculatePromotion(small).compareTo(BigDecimal.ZERO)),
							() -> assertEquals(PromotionType.MAINLAND_TIERED_DISCOUNT,
									calculator.evaluateBestPromotion(small).getType()));
				},
				// 1 档：2000 → 减 300
				() -> assertEquals(0, new MainlandFullReductionStrategy()
						.calculatePromotion(order(Region.CHINA_MAINLAND, "R1", 1, "2000"))
						.compareTo(new BigDecimal("300.00"))),
				// 39 档：79888 → 减 11700（sysgui 订单 0004 同款数字）
				() -> assertEquals(0, new MainlandFullReductionStrategy()
						.calculatePromotion(order(Region.CHINA_MAINLAND, "R39", 1, "79888"))
						.compareTo(new BigDecimal("11700.00"))),
				// 无人为封顶：200 万元 → 1000 档 → 减 30 万；2 亿元 → 减 3000 万
				() -> assertEquals(0, new MainlandFullReductionStrategy()
						.calculatePromotion(order(Region.CHINA_MAINLAND, "R1000", 1, "2000000"))
						.compareTo(new BigDecimal("300000.00"))),
				() -> assertEquals(0, new MainlandFullReductionStrategy()
						.calculatePromotion(order(Region.CHINA_MAINLAND, "RHUGE", 1, "200000000"))
						.compareTo(new BigDecimal("30000000.00"))));
	}

	@Test
	@DisplayName("并列同价时满额立减优先于满件折扣；恒等式保持")
	void tieBreakPrefersFullReduction() {
		// 3000 元 1 件：满件 9 折 = 2700.00；满减 1 档 = 2700.00 → 并列，满额立减胜出
		Order tie = order(Region.CHINA_MAINLAND, "TIE", 1, "3000");
		PromotionResult best = calculator.evaluateBestPromotion(tie);
		assertAll(
				() -> assertEquals(PromotionType.MAINLAND_FULL_REDUCTION, best.getType()),
				() -> assertEquals(0, best.getPayableAmount().compareTo(new BigDecimal("2700.00"))));
	}

	@Test
	@DisplayName("0 元减免不冒充活动：全部候选 0 优惠时归入无优惠")
	void zeroDiscountFallsBackToNoPromotion() {
		// 单价 0 元订单：满件/满减优惠均为 0 → 无优惠
		Order zero = order(Region.CHINA_MAINLAND, "ZERO", 1, "0");
		PromotionResult best = calculator.evaluateBestPromotion(zero);
		assertEquals(PromotionType.NO_PROMOTION, best.getType());
	}

	@Test
	@DisplayName("大陆非 CNY 订单禁用人民币满减，但仍可参与满件折扣")
	void mainlandNonCnyExcludedFromFullReduction() {
		Order usd = order(Region.CHINA_MAINLAND, Currency.USD, "USDM", 1, "3000");
		PromotionResult best = calculator.evaluateBestPromotion(usd);
		assertAll(
				() -> assertEquals(Currency.USD, usd.getCurrency()),
				() -> assertEquals(PromotionType.MAINLAND_TIERED_DISCOUNT, best.getType()),
				() -> assertEquals(0, best.getPayableAmount().compareTo(new BigDecimal("2700.00"))),
				// 教师接口兼容：满减策略对该订单不可用
				() -> assertTrue(!new MainlandFullReductionStrategy().isApplicable(usd)));
	}

	@Test
	@DisplayName("教师接口兼容：PromotionStrategy 是 DJIPromotion，calculatePromotion 返回该策略优惠额")
	void teacherInterfaceCompatibility() {
		Order order = ordersById.get("2025102900004");
		PromotionStrategy reduction = new MainlandFullReductionStrategy();
		assertAll(
				() -> assertInstanceOf(DJIPromotion.class, reduction),
				() -> assertEquals(0, reduction.calculatePromotion(order)
						.compareTo(new BigDecimal("11700.00"))),
				() -> assertEquals(0, new MainlandTieredDiscountStrategy().calculatePromotion(order)
						.compareTo(new BigDecimal("7988.80"))));
	}

	// ---- 辅助：构造合成订单 ----

	private Order order(Region region, String orderId, int quantity, String unitPrice) {
		Currency currency = region == Region.CHINA_MAINLAND ? Currency.CNY : Currency.USD;
		return order(region, currency, orderId, quantity, unitPrice);
	}

	private Order order(Region region, Currency currency, String orderId, int quantity, String unitPrice) {
		// 统一用 Neo 型号构造 qty×unitPrice 的单明细订单
		String type = "Neo";
		BigDecimal price = new BigDecimal(unitPrice);
		List<OrderItem> items = new ArrayList<>();
		items.add(new OrderItem(1, type, DroneSpecRegistry.getCategory(type), quantity,
				price, price.multiply(BigDecimal.valueOf(quantity))));
		BigDecimal total = items.stream().map(OrderItem::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Order(orderId, new Customer("合成测试客户", "synthetic@test.com", "000"),
				region, currency, LocalDateTime.of(2025, 11, 1, 10, 0), items, total);
	}

	private void assertPayable(Order order, String expectedPayable) {
		PromotionResult best = calculator.evaluateBestPromotion(order);
		assertEquals(0, best.getPayableAmount().compareTo(new BigDecimal(expectedPayable)),
				order.getOrderId() + " 应付额不符: " + best);
	}

	/** 直接断言单一策略的应付额（隔离其他策略的合法竞争）。 */
	private void assertStrategyPayable(PromotionStrategy strategy, Order order, String expectedPayable) {
		PromotionResult result = strategy.evaluate(order);
		assertEquals(0, result.getPayableAmount().compareTo(new BigDecimal(expectedPayable)),
				order.getOrderId() + " " + strategy.getType() + " 应付额不符: " + result);
	}
}

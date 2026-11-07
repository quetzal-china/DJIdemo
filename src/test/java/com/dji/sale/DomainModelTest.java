package com.dji.sale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 任务 1｜领域模型测试：枚举严格解析、不变量强制、不可变集合、零宽字符清洗。
 */
class DomainModelTest {

	// ---- 枚举严格解析 ----

	@Test
	@DisplayName("Region.fromCode 严格解析 JSON 原文取值，未知值报错")
	void regionParsing() {
		assertAll(
				() -> assertEquals(Region.CHINA_MAINLAND, Region.fromCode("CHINA_MAINLAND")),
				() -> assertEquals(Region.ASIA, Region.fromCode("ASIA")),
				() -> assertEquals("亚太", Region.ASIA.getDisplayName()),
				() -> assertThrows(OrderValidationException.class, () -> Region.fromCode("AFRICA")),
				() -> assertThrows(OrderValidationException.class, () -> Region.fromCode("asia")),
				() -> assertThrows(OrderValidationException.class, () -> Region.fromCode(null)));
	}

	@Test
	@DisplayName("Currency.fromCode 严格解析，金额格式为 币种代码+两位小数")
	void currencyParsingAndFormatting() {
		assertAll(
				() -> assertEquals(Currency.JPY, Currency.fromCode("JPY")),
				() -> assertThrows(OrderValidationException.class, () -> Currency.fromCode("GBP")),
				() -> assertEquals("CNY 1299.00", Currency.CNY.formatAmount(new BigDecimal("1299"))),
				() -> assertEquals("JPY 697800.00", Currency.JPY.formatAmount(new BigDecimal("697800"))));
	}

	@Test
	@DisplayName("OrderStatus 仅 JSON 可出现值可解析；ProductCategory 对齐教师中文显示名")
	void statusAndCategory() {
		assertAll(
				() -> assertEquals(OrderStatus.PENDING, OrderStatus.fromCode("PENDING")),
				() -> assertThrows(OrderValidationException.class, () -> OrderStatus.fromCode("SHIPPED")),
				() -> assertEquals("消费级无人机", ProductCategory.CONSUMER.getDisplayName()),
				() -> assertEquals(ProductCategory.AGRICULTURAL, ProductCategory.fromDisplayName("农业级无人机")),
				() -> assertThrows(IllegalArgumentException.class, () -> ProductCategory.fromDisplayName("军工级")));
	}

	// ---- 客户零宽字符 ----

	@Test
	@DisplayName("客户名 U+200C 零宽字符被剥离，rawName 保留原文")
	void customerZeroWidthNormalization() {
		Customer elizabeth = new Customer("\u200cElizabeth Taylor", "ccc@ccc.com", "001-445-568-7300");
		Customer emil = new Customer("Emil\u200c", "ggg@ggg.com", "49-176-1234567");
		assertAll(
				() -> assertEquals("\u200cElizabeth Taylor", elizabeth.getRawName()),
				() -> assertEquals("Elizabeth Taylor", elizabeth.getNormalizedName()),
				() -> assertEquals("Emil", emil.getNormalizedName()),
				() -> assertTrue(elizabeth.getNormalizedName().contains("Elizabeth")),
				() -> assertTrue(emil.getNormalizedName().startsWith("Emil")));
	}

	@Test
	@DisplayName("客户名字段缺失或剥空后为空白时报可定位错误")
	void customerBlankRejected() {
		assertAll(
				() -> {
					OrderValidationException e = assertThrows(OrderValidationException.class,
							() -> new Customer("\u200c\u200c", "a@a.com", "123"));
					assertEquals("customerName", e.getFieldName());
				},
				() -> assertThrows(OrderValidationException.class, () -> new Customer("张三", " ", "123")),
				() -> assertThrows(OrderValidationException.class, () -> new Customer("张三", "a@a.com", null)));
	}

	// ---- 明细不变量 ----

	private OrderItem item(int index, String type, int quantity, String price, String total) {
		return new OrderItem(index, type, ProductCategory.CONSUMER, quantity,
				price == null ? null : new BigDecimal(price), total == null ? null : new BigDecimal(total));
	}

	@Test
	@DisplayName("明细金额必须等于单价×数量，错误携带明细序号")
	void itemAmountInvariant() {
		OrderItem valid = item(1, "Mini 4 PRO", 2, "5090", "10180");
		assertEquals(0, valid.getTotalAmount().compareTo(new BigDecimal("10180")));
		assertEquals(0, valid.getUnitPrice().compareTo(new BigDecimal("5090.00")));

		assertAll(
				() -> {
					OrderValidationException e = assertThrows(OrderValidationException.class,
							() -> item(1, "Mini 4 PRO", 2, "5090", "10181"));
					assertEquals(1, e.getItemIndex());
					assertEquals("totalAmount", e.getFieldName());
				},
				() -> {
					OrderValidationException e = assertThrows(OrderValidationException.class,
							() -> item(3, "Neo", 1, "1299", "999"));
					assertEquals(3, e.getItemIndex());
					assertEquals("totalAmount", e.getFieldName());
				});
	}

	@Test
	@DisplayName("明细数量必须为正整数、单价非负")
	void itemBasicInvariants() {
		assertAll(
				() -> assertEquals("quantity", assertThrows(OrderValidationException.class,
						() -> item(1, "Neo", 0, "1299", "0")).getFieldName()),
				() -> assertEquals("quantity", assertThrows(OrderValidationException.class,
						() -> item(1, "Neo", -1, "1299", "-1299")).getFieldName()),
				() -> assertEquals("unitPrice", assertThrows(OrderValidationException.class,
						() -> item(1, "Neo", 1, "-1", "0")).getFieldName()),
				() -> assertEquals("unitPrice", assertThrows(OrderValidationException.class,
						() -> item(1, "Neo", 1, null, null)).getFieldName()));
	}

	// ---- 订单聚合 ----

	private Order validOrder() {
		Customer customer = new Customer("张三", "123@123.com", "86 13500000001");
		List<OrderItem> items = List.of(
				item(1, "Mini 4 PRO", 2, "5090", "10180"),
				item(2, "Avata 2", 1, "3988", "3988"));
		return new Order("2025102900001", customer, Region.CHINA_MAINLAND, Currency.CNY,
				LocalDateTime.of(2025, 10, 29, 15, 28, 57, 638_000_000), items, new BigDecimal("14168"));
	}

	@Test
	@DisplayName("订单总金额必须等于明细之和；总件数为明细数量之和")
	void orderAmountAndQuantity() {
		Order order = validOrder();
		assertAll(
				() -> assertEquals(0, order.getOriginalAmount().compareTo(new BigDecimal("14168.00"))),
				() -> assertEquals(3, order.getTotalQuantity()),
				() -> assertEquals(2, order.getItems().size()));
	}

	@Test
	@DisplayName("订单总额与明细之和不一致时报错；明细列表不可变")
	void orderInvariants() {
		List<OrderItem> items = List.of(item(1, "Neo", 1, "1299", "1299"));
		assertThrows(OrderValidationException.class,
				() -> new Order("O1", new Customer("李四", "a@a.com", "1"), Region.CHINA_MAINLAND,
						Currency.CNY, LocalDateTime.now(), items, new BigDecimal("1300")));
		assertThrows(OrderValidationException.class,
				() -> new Order("O1", new Customer("李四", "a@a.com", "1"), Region.CHINA_MAINLAND,
						Currency.CNY, LocalDateTime.now(), new ArrayList<>(), BigDecimal.ZERO));
		assertThrows(OrderValidationException.class,
				() -> new Order(" ", new Customer("李四", "a@a.com", "1"), Region.CHINA_MAINLAND,
						Currency.CNY, LocalDateTime.now(), items, new BigDecimal("1299")));

		Order order = validOrder();
		assertThrows(UnsupportedOperationException.class,
				() -> order.getItems().add(item(9, "Neo", 1, "1299", "1299")));
	}

	@Test
	@DisplayName("订单状态按 D-009 默认方案流转：PENDING→PROCESSED/FAILED")
	void statusFlow() {
		Order order = validOrder();
		assertEquals(OrderStatus.PENDING, order.getStatus());
		order.markProcessed();
		assertEquals(OrderStatus.PROCESSED, order.getStatus());
		order.markFailed();
		assertEquals(OrderStatus.FAILED, order.getStatus());
	}

	@Test
	@DisplayName("订单相等性基于订单号；PromotionResult 保持原价-优惠=应付恒等式")
	void orderEqualityAndPromotionResult() {
		Order first = validOrder();
		Order second = validOrder();
		assertEquals(first, second);
		assertNotEquals(first.hashCode(), 0);

		PromotionResult result = PromotionResult.of(PromotionType.MAINLAND_TIERED_DISCOUNT,
				new BigDecimal("14168"), new BigDecimal("2550.24"));
		assertAll(
				() -> assertEquals("中国大陆满件折扣", result.getPromotionName()),
				() -> assertEquals(0, result.getPayableAmount().compareTo(new BigDecimal("11617.76"))),
				() -> assertEquals(0, result.getPayableAmount().compareTo(
						result.getOriginalAmount().subtract(result.getDiscountAmount()))),
				() -> assertThrows(IllegalArgumentException.class,
						() -> PromotionResult.of(PromotionType.NO_PROMOTION, new BigDecimal("100"), new BigDecimal("-1"))),
				() -> assertThrows(IllegalArgumentException.class,
						() -> PromotionResult.of(PromotionType.NO_PROMOTION, new BigDecimal("100"), new BigDecimal("101"))));
	}

	@Test
	@DisplayName("并列优先级常量：满额立减 < 满件折扣 < 无优惠")
	void tieBreakRank() {
		assertAll(
				() -> assertSame(1, PromotionType.MAINLAND_FULL_REDUCTION.getTieBreakRank()),
				() -> assertTrue(PromotionType.MAINLAND_FULL_REDUCTION.getTieBreakRank()
						< PromotionType.MAINLAND_TIERED_DISCOUNT.getTieBreakRank()),
				() -> assertTrue(PromotionType.OVERSEAS_TIERED_DISCOUNT.getTieBreakRank()
						< PromotionType.NO_PROMOTION.getTieBreakRank()));
	}

	// ---- 生产任务值对象 ----

	@Test
	@DisplayName("ProductionTask 区分计划数量与实际完成数")
	void productionTaskSemantics() {
		ProductionTask partial = new ProductionTask("O1", 1, ProductionBase.SHENZHEN, "Neo", 5,
				List.of("DJI-00000001", "DJI-00000002"));
		ProductionTask complete = new ProductionTask("O1", 2, ProductionBase.SHUNDE, "T70", 2,
				List.of("DJI-00000003", "DJI-00000004"));
		assertAll(
				() -> assertEquals(5, partial.getQuantity()),
				() -> assertEquals(2, partial.getProducedCount()),
				() -> assertTrue(!partial.isComplete()),
				() -> assertTrue(complete.isComplete()),
				() -> assertThrows(UnsupportedOperationException.class,
						() -> partial.getSerialNumbers().add("DJI-00000009")));
	}
}

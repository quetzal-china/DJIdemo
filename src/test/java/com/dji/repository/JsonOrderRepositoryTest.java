package com.dji.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.dji.sale.Order;
import com.dji.sale.OrderLoadResult;
import com.dji.sale.OrderValidationError;
import com.dji.sale.Region;

/**
 * 任务 2｜JSON 读取、校验与可定位错误测试。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonOrderRepositoryTest {

	private static final Path REAL_FILE = Path.of("src/main/resources/order.json");
	private static final Path INVALID_FILE = Path.of("src/test/resources/fixtures/invalid_orders.json");
	private static final Path MALFORMED_FILE = Path.of("src/test/resources/fixtures/malformed.json");
	private static final Path NOT_ARRAY_FILE = Path.of("src/test/resources/fixtures/not_array.json");

	private JsonOrderRepository repository;
	private OrderLoadResult realResult;
	private OrderLoadResult invalidResult;

	@BeforeAll
	void setUp() {
		repository = new JsonOrderRepository();
		realResult = repository.loadOrders(REAL_FILE);
		invalidResult = repository.loadOrders(INVALID_FILE);
	}

	@Test
	@DisplayName("真实 order.json：14 笔全部有效、0 条错误")
	void realFileLoadsFully() {
		assertAll(
				() -> assertEquals(14, realResult.validOrders().size()),
				() -> assertTrue(realResult.errors().isEmpty(), "真实数据不应有校验错误: "
						+ realResult.describeErrors()));
	}

	@Test
	@DisplayName("首笔订单字段映射正确（含 U+200C 零宽客户名清洗）")
	void realFileFieldMapping() {
		Map<String, Order> byId = realResult.validOrders().stream()
				.collect(Collectors.toMap(Order::getOrderId, Function.identity()));
		Order first = byId.get("2025102900001");
		assertAll(
				() -> assertEquals("张三", first.getCustomer().getNormalizedName()),
				() -> assertEquals(Region.CHINA_MAINLAND, first.getRegion()),
				() -> assertEquals(2, first.getItems().size()),
				() -> assertEquals(3, first.getTotalQuantity()),
				() -> assertEquals(0, first.getOriginalAmount().compareTo(new BigDecimal("14168.00"))),
				() -> assertEquals("Elizabeth Taylor",
						byId.get("2025102900009").getCustomer().getNormalizedName()),
				() -> assertEquals("Emil",
						byId.get("2025102900010").getCustomer().getNormalizedName()),
				() -> assertEquals("\u200cElizabeth Taylor",
						byId.get("2025102900009").getCustomer().getRawName()));
	}

	@Test
	@DisplayName("坏数据文件：14 处错误逐一可定位（订单号+明细序号+字段+原因）")
	void invalidFileErrorsArePrecise() {
		assertAll(
				() -> assertEquals(1, invalidResult.validOrders().size(), "仅 VALID001 有效"),
				() -> assertEquals(14, invalidResult.errors().size()));
		// 期望：recordIndex → (orderId, itemIndex, fieldName)
		record E(int idx, String orderId, Integer itemIndex, String field) {
		}
		List<E> expected = List.of(
				new E(2, "VALID001", null, "orderId"),        // 重复订单号
				new E(3, "BLANK_CUST", null, "customerName"), // 客户名剥空
				new E(4, "MISSING_EMAIL", null, "email"),
				new E(5, "BAD_REGION", null, "region"),
				new E(6, "BAD_CURRENCY", null, "currency"),
				new E(7, "BAD_STATUS", null, "status"),
				new E(8, "BAD_DATE", null, "orderDate"),
				new E(9, "BAD_TYPE", 1, "productType"),       // Mini 4 近似名
				new E(10, "QTY_ZERO", 1, "quantity"),
				new E(11, "PRICE_NEG", 1, "unitPrice"),
				new E(12, "ITEM_MISMATCH", 1, "totalAmount"),
				new E(13, "ORDER_MISMATCH", null, "totalAmount"),
				new E(14, "EMPTY_ITEMS", null, "items"),
				new E(15, "MISSING_TOTAL", null, "totalAmount"));
		assertEquals(15, invalidResult.validOrders().size() + invalidResult.errors().size());
		assertAll(expected.stream().map(e -> () -> {
			OrderValidationError error = invalidResult.errors().stream()
					.filter(x -> x.orderId().equals(e.orderId()))
					.findFirst()
					.orElseThrow(() -> new AssertionError("缺少错误记录: " + e.orderId()));
			assertAll(e.orderId(),
					() -> assertEquals(e.idx(), error.recordIndex(), "记录序号不符"),
					() -> assertEquals(e.field(), error.fieldName(), "字段名不符"),
					() -> assertEquals(e.itemIndex(), error.itemIndex(), "明细序号不符"),
					() -> assertTrue(error.message() != null && !error.message().isBlank(), "缺少原因说明"),
					() -> assertTrue(error.describe().contains(e.orderId())));
		}));
	}

	@Test
	@DisplayName("JSON 语法损坏与顶层非数组：整份失败")
	void malformedFilesFailEntirely() {
		assertAll(
				() -> assertThrows(OrderFileParseException.class, () -> repository.loadOrders(MALFORMED_FILE)),
				() -> assertThrows(OrderFileParseException.class, () -> repository.loadOrders(NOT_ARRAY_FILE)),
				() -> assertThrows(OrderFileParseException.class,
						() -> repository.loadOrders(Path.of("src/test/resources/fixtures/no_such_file.json"))));
	}

	@Test
	@DisplayName("严格模式：真实文件全过；坏文件整份拒绝")
	void strictMode() {
		assertAll(
				() -> assertEquals(14, repository.loadOrdersStrict(REAL_FILE).size()),
				() -> {
					OrderLoadException e = assertThrows(OrderLoadException.class,
							() -> repository.loadOrdersStrict(INVALID_FILE));
					assertTrue(e.getResult().errors().size() == 14);
				});
	}
}

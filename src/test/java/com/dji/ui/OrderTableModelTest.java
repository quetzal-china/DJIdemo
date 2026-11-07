package com.dji.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.dji.promotion.PromotionCalculator;
import com.dji.repository.JsonOrderRepository;
import com.dji.sale.Order;
import com.dji.sale.PromotionResult;

/**
 * 任务 6/8｜表格模型无头测试：GUI 数据映射不依赖 Swing 对等组件即可验证。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderTableModelTest {

	private List<Order> orders;
	private Map<String, PromotionResult> promoCache;

	@BeforeAll
	void loadRealOrdersWithPromotions() {
		orders = new JsonOrderRepository()
				.loadOrders(Path.of("src/main/resources/order.json"))
				.validOrders();
		PromotionCalculator calculator = new PromotionCalculator();
		promoCache = orders.stream().collect(Collectors.toMap(Order::getOrderId,
				calculator::evaluateBestPromotion));
	}

	@Test
	@DisplayName("14 行 10 列，列名与参考界面一致")
	void structure() {
		OrderTableModel model = new OrderTableModel(promoCache::get);
		model.setOrders(orders);
		assertAll(
				() -> assertEquals(14, model.getRowCount()),
				() -> assertEquals(10, model.getColumnCount()),
				() -> assertEquals("订单号", model.getColumnName(0)),
				() -> assertEquals("应付金额", model.getColumnName(9)),
				() -> assertFalse(model.isCellEditable(0, 0)));
	}

	@Test
	@DisplayName("首行单元格值与真实数据/促销一致（金额带币种前缀两位小数）")
	void cellValues() {
		OrderTableModel model = new OrderTableModel(promoCache::get);
		model.setOrders(orders);
		Order first = orders.get(0);
		assertAll(
				() -> assertEquals(first.getOrderId(), model.getValueAt(0, 0)),
				() -> assertEquals("张三", model.getValueAt(0, 1)),
				() -> assertEquals("中国大陆", model.getValueAt(0, 2)),
				() -> assertEquals("CNY", model.getValueAt(0, 3)),
				() -> assertEquals("待处理", model.getValueAt(0, 4)),
				() -> assertEquals(3, model.getValueAt(0, 5)),
				() -> assertEquals("CNY 14168.00", model.getValueAt(0, 6)),
				() -> assertEquals("中国大陆满件折扣", model.getValueAt(0, 7)),
				() -> assertEquals("CNY 2550.24", model.getValueAt(0, 8)),
				() -> assertEquals("CNY 11617.76", model.getValueAt(0, 9)));
	}

	@Test
	@DisplayName("促销未加载时显示占位符；生产后状态列更新")
	void placeholdersAndStatusUpdate() {
		OrderTableModel model = new OrderTableModel(orderId -> null);
		model.setOrders(orders);
		assertAll(
				() -> assertEquals("尚未计算", model.getValueAt(0, 7)),
				() -> assertEquals("—", model.getValueAt(0, 8)));

		// 模拟生产完成后的状态列刷新（仅状态字段变化）
		orders.get(0).markProcessed();
		model.setOrders(orders);
		assertEquals("已生产", model.getValueAt(0, 4));
		orders.get(0).restoreStatus(com.dji.sale.OrderStatus.PENDING); // 还原，避免污染其他测试
	}

	@Test
	@DisplayName("setOrders 筛选刷新与 getOrderAt 联动正确")
	void filteredUpdateAndGetOrderAt() {
		OrderTableModel model = new OrderTableModel(promoCache::get);
		model.setOrders(orders);
		List<Order> europeOnly = orders.stream()
				.filter(order -> order.getRegion() == com.dji.sale.Region.EUROPE)
				.toList();
		model.setOrders(europeOnly);
		assertAll(
				() -> assertEquals(2, model.getRowCount()),
				() -> assertEquals("2025102900010", model.getOrderAt(0).getOrderId()),
				() -> assertEquals("Emil", model.getOrderAt(0).getCustomer().getNormalizedName()),
				() -> assertEquals("EUR 189.70", model.getValueAt(0, 8)),
				() -> assertEquals("EUR 1707.30", model.getValueAt(0, 9)));
		assertTrue(promoCache.size() == 14);
		assertEquals(0, promoCache.get("2025102900011").getPayableAmount()
				.compareTo(new BigDecimal("39425.55")));
	}
}

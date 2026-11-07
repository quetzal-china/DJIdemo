package com.dji.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dji.product.DroneSpecRegistry;
import com.dji.sale.Currency;
import com.dji.sale.Customer;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;
import com.dji.sale.OrderStatus;
import com.dji.sale.OrderValidationException;
import com.dji.sale.ProductCategory;
import com.dji.sale.Region;

/**
 * 单笔订单校验与领域对象构建器。
 *
 * <p>任务 2 校验项（任一失败即抛出携带字段名与明细序号的
 * {@link OrderValidationException}，由仓储层映射为可定位的
 * {@code OrderValidationError}）：</p>
 * <ul>
 *   <li>订单号非空且唯一（跨文件加载过程以 {@code seenOrderIds} 去重）；</li>
 *   <li>客户名/邮箱/电话、地区、币种、时间、状态必需且合法；</li>
 *   <li>明细列表非空；型号与 {@code DroneSpecRegistry} 注册拼写精确一致
 *       （"Mini 4"/"Air 3" 等近似名无效）；数量为正整数；单价非负；</li>
 *   <li>明细金额 = 单价 × 数量；订单总金额 = Σ 明细金额。</li>
 * </ul>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public final class OrderValidator {

	/** 真实 order.json 的下单时间格式。 */
	private static final DateTimeFormatter DATE_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private OrderValidator() {
	}

	/**
	 * 校验并构建一笔订单。
	 *
	 * @param json         该笔订单的 JSON 对象
	 * @param recordIndex  记录序号（1-based，用于错误消息）
	 * @param seenOrderIds 已出现订单号集合（调用方持有，实现跨笔去重）
	 * @return 构建完成的订单聚合
	 * @throws OrderValidationException 任一校验失败（携带字段名与明细序号）
	 */
	public static Order parseOrder(JSONObject json, int recordIndex, Set<String> seenOrderIds) {
		String orderId = json.getString("orderId");
		if (orderId == null || orderId.isBlank()) {
			throw new OrderValidationException("orderId", "订单号不能为空");
		}
		orderId = orderId.trim();
		if (!seenOrderIds.add(orderId)) {
			throw new OrderValidationException("orderId", "订单号重复: " + orderId);
		}

		Customer customer = new Customer(json.getString("customerName"),
				json.getString("email"), json.getString("phone"));
		Region region = Region.fromCode(json.getString("region"));
		Currency currency = Currency.fromCode(json.getString("currency"));
		OrderStatus status = OrderStatus.fromCode(json.getString("status"));
		LocalDateTime orderDate = parseOrderDate(json.getString("orderDate"));

		JSONArray itemArray = json.getJSONArray("items");
		if (itemArray == null || itemArray.isEmpty()) {
			throw new OrderValidationException("items", "订单 " + orderId + " 明细列表为空或缺失");
		}
		List<OrderItem> items = new ArrayList<>();
		for (int i = 0; i < itemArray.size(); i++) {
			Object raw = itemArray.get(i);
			if (!(raw instanceof JSONObject itemJson)) {
				throw new OrderValidationException(i + 1, "items",
						"第 " + (i + 1) + " 条明细不是 JSON 对象");
			}
			items.add(parseItem(i + 1, itemJson));
		}

		Order order = new Order(orderId, customer, region, currency, orderDate, items,
				json.getBigDecimal("totalAmount"));
		order.restoreStatus(status);
		return order;
	}

	private static LocalDateTime parseOrderDate(String dateText) {
		if (dateText == null || dateText.isBlank()) {
			throw new OrderValidationException("orderDate", "下单时间缺失");
		}
		try {
			return LocalDateTime.parse(dateText.trim(), DATE_TIME_FORMAT);
		} catch (DateTimeParseException e) {
			throw new OrderValidationException("orderDate",
					"下单时间格式非法（期望 yyyy-MM-dd HH:mm:ss.SSS）: " + dateText);
		}
	}

	private static OrderItem parseItem(int itemIndex, JSONObject itemJson) {
		String productType = itemJson.getString("productType");
		if (!DroneSpecRegistry.isValidModel(productType)) {
			throw new OrderValidationException(itemIndex, "productType",
					"明细型号无效或未注册（必须精确匹配注册拼写）: " + productType);
		}
		ProductCategory category = DroneSpecRegistry.getCategory(productType);

		BigDecimal quantityValue = itemJson.getBigDecimal("quantity");
		if (quantityValue == null) {
			throw new OrderValidationException(itemIndex, "quantity", "购买数量缺失");
		}
		if (quantityValue.stripTrailingZeros().scale() > 0) {
			throw new OrderValidationException(itemIndex, "quantity",
					"购买数量必须为整数，实际: " + quantityValue.toPlainString());
		}
		int quantity;
		try {
			quantity = quantityValue.intValueExact();
		} catch (ArithmeticException e) {
			throw new OrderValidationException(itemIndex, "quantity",
					"购买数量超出整数范围: " + quantityValue.toPlainString());
		}

		return new OrderItem(itemIndex, productType, category, quantity,
				itemJson.getBigDecimal("unitPrice"), itemJson.getBigDecimal("totalAmount"));
	}
}

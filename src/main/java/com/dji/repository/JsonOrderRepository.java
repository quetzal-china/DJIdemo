package com.dji.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.dji.sale.Order;
import com.dji.sale.OrderLoadResult;
import com.dji.sale.OrderValidationError;
import com.dji.sale.OrderValidationException;

/**
 * JSON 订单文件仓储（基础设施层）。
 *
 * <p>读取策略（DECISIONS.md D-006 默认方案）：
 * {@link #loadOrders(Path)} 为"跳过单笔无效订单 + 结构化错误汇总"——
 * 顶层 JSON 语法损坏或文件不可读时整份失败（{@link OrderFileParseException}）；
 * 语法合法但个别订单校验失败时跳过该笔并记录可定位错误，有效订单照常返回。
 * {@link #loadOrdersStrict(Path)} 为整份失败模式：任一订单有误则全部拒绝。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class JsonOrderRepository {

	private static final String UNKNOWN_ORDER_ID = "<未知订单号>";

	/**
	 * 宽松模式加载：跳过无效订单并汇总错误。
	 *
	 * @param file 订单 JSON 文件
	 * @return 加载结果（有效订单 + 错误清单）
	 * @throws OrderFileParseException 文件不可读或顶层 JSON 语法损坏
	 */
	public OrderLoadResult loadOrders(Path file) {
		JSONArray array = parseFile(file);
		List<Order> validOrders = new ArrayList<>();
		List<OrderValidationError> errors = new ArrayList<>();
		Set<String> seenOrderIds = new HashSet<>();
		for (int i = 0; i < array.size(); i++) {
			int recordIndex = i + 1;
			Object element = array.get(i);
			if (!(element instanceof JSONObject json)) {
				errors.add(new OrderValidationError(recordIndex, UNKNOWN_ORDER_ID, null, "record",
						"第 " + recordIndex + " 笔记录不是 JSON 对象"));
				continue;
			}
			String rawOrderId = json.getString("orderId");
			String orderId = rawOrderId == null || rawOrderId.isBlank()
					? UNKNOWN_ORDER_ID : rawOrderId.trim();
			try {
				validOrders.add(OrderValidator.parseOrder(json, recordIndex, seenOrderIds));
			} catch (OrderValidationException e) {
				errors.add(new OrderValidationError(recordIndex, orderId, e.getItemIndex(),
						e.getFieldName(), e.getMessage()));
			}
		}
		return new OrderLoadResult(file, validOrders, errors);
	}

	/**
	 * 严格模式加载：任一订单有误即整份拒绝（全部有效才返回）。
	 *
	 * @throws OrderLoadException 存在任意校验错误
	 * @throws OrderFileParseException 文件不可读或顶层 JSON 语法损坏
	 */
	public List<Order> loadOrdersStrict(Path file) {
		OrderLoadResult result = loadOrders(file);
		if (result.hasErrors()) {
			throw new OrderLoadException("订单文件存在 " + result.errors().size()
					+ " 处错误，严格模式下整份拒绝:\n" + result.describeErrors(), result);
		}
		return result.validOrders();
	}

	private JSONArray parseFile(Path file) {
		String text;
		try {
			text = Files.readString(file);
		} catch (IOException | UncheckedIOException e) {
			throw new OrderFileParseException("无法读取订单文件: " + file, e);
		}
		try {
			Object root = JSON.parse(text);
			if (!(root instanceof JSONArray array)) {
				throw new OrderFileParseException("订单文件顶层必须是 JSON 数组: " + file);
			}
			return array;
		} catch (JSONException e) {
			throw new OrderFileParseException("订单文件 JSON 语法损坏: " + file + "（"
					+ e.getMessage() + "）", e);
		}
	}
}

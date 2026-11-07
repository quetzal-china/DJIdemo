package com.dji.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.dji.sale.Customer;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;

/**
 * 订单查询服务（任务 7.1/7.3/7.4/7.5 的 Stream 组合筛选主战场）。
 *
 * 纯业务服务，零 Swing 依赖；GUI 只收集输入并呈现结果。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public class OrderQueryService {

	/** 按订单号升序。 */
	public static final Comparator<Order> BY_ORDER_ID_ASC =
			Comparator.comparing(Order::getOrderId);

	/** 按原价金额降序（方法引用 + Lambda 组合）。 */
	public static final Comparator<Order> BY_ORIGINAL_AMOUNT_DESC =
			Comparator.comparing(Order::getOriginalAmount).reversed();

	/**
	 * 多条件组合筛选 + 排序（Stream filter/sorted/collect）。
	 *
	 * @param orders   待筛选订单
	 * @param criteria 组合条件（isEmpty 时返回排序后的全量副本）
	 * @param sort     排序比较器（null 则保持原序）
	 * @return 筛选结果新列表
	 */
	public List<Order> searchOrders(List<Order> orders, OrderSearchCriteria criteria,
			Comparator<Order> sort) {
		Stream<Order> stream = orders.stream()
				.filter(criteria.toFilter()::matches);
		if (sort != null) {
			stream = stream.sorted(sort);
		}
		return stream.collect(Collectors.toList());
	}

	/**
	 * 按订单号精确查找。
	 *
	 * @param orders  订单列表
	 * @param orderId 订单号
	 * @return 命中的订单
	 */
	public Optional<Order> findByOrderId(List<Order> orders, String orderId) {
		return orders.stream()
				.filter(order -> order.getOrderId().equals(orderId))
				.findFirst();
	}

	/**
	 * 扁平化筛选指定客户（规范化名包含关键字）的全部商品明细
	 * （任务 7.6：flatMap 扁平化 + filter + map）。
	 *
	 * @param orders  订单列表
	 * @param keyword 客户名关键字
	 * @return 命中的明细（含所属订单语义，见 OrderItem 字段）
	 */
	public List<OrderItem> findItemsByCustomerKeyword(List<Order> orders, String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return List.of();
		}
		String normalizedKeyword = keyword.trim().toLowerCase(java.util.Locale.ROOT);
		return orders.stream()
				.filter(order -> order.getCustomer().getNormalizedName()
						.toLowerCase(java.util.Locale.ROOT).contains(normalizedKeyword))
				.flatMap(order -> order.getItems().stream())
				.collect(Collectors.toList());
	}

	/**
	 * 扁平化筛选全部订单中购买数量超过阈值的明细并按数量降序
	 * （任务 7.6 示例方向的补充查询）。
	 *
	 * @param orders        订单列表
	 * @param minQuantity   数量下限（含）
	 * @return 按数量降序的明细列表
	 */
	public List<OrderItem> findItemsWithQuantityAtLeast(List<Order> orders, int minQuantity) {
		return orders.stream()
				.flatMap(order -> order.getItems().stream())
				.filter(item -> item.getQuantity() >= minQuantity)
				.sorted(Comparator.comparingInt(OrderItem::getQuantity).reversed())
				.collect(Collectors.toList());
	}

	/** @return 客户规范化名的引用（供 GUI 展示，方法引用用法示例）。 */
	public String displayName(Customer customer) {
		return customer.getNormalizedName();
	}
}

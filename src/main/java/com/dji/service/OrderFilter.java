package com.dji.service;

import java.util.Locale;

import com.dji.sale.Order;
import com.dji.sale.OrderStatus;
import com.dji.sale.Region;

/**
 * 订单筛选谓词（任务 7.4：自定义函数式接口，支持组合）。
 *
 * <p>提供 {@link #and(OrderFilter)} 等缺省方法实现谓词组合，
 * 静态工厂覆盖关键字/地区/状态三类业务条件；关键字匹配订单号与
 * <strong>规范化</strong>客户名（剥离零宽字符、忽略大小写，见 ADR-003），
 * 因此可命中 order.json 中含 U+200C 的 "Elizabeth Taylor"/"Emil"。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
@FunctionalInterface
public interface OrderFilter {

	/**
	 * 判断订单是否匹配本条件。
	 *
	 * @param order 订单
	 * @return 匹配返回 true
	 */
	boolean matches(Order order);

	/** 与另一条件取交集（AND 组合）。 */
	default OrderFilter and(OrderFilter other) {
		return order -> this.matches(order) && other.matches(order);
	}

	/** 与另一条件取并集（OR 组合）。 */
	default OrderFilter or(OrderFilter other) {
		return order -> this.matches(order) || other.matches(order);
	}

	/** 取反条件。 */
	default OrderFilter negate() {
		return order -> !this.matches(order);
	}

	/** @return 恒真条件（不清空其余条件时用作占位）。 */
	static OrderFilter all() {
		return order -> true;
	}

	/**
	 * 按关键字筛选订单（匹配订单号或规范化客户名，忽略大小写；空关键字视为全匹配）。
	 *
	 * @param keyword 订单号或客户名关键字
	 * @return 筛选谓词
	 */
	static OrderFilter byKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return all();
		}
		String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
		return order -> order.getOrderId().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
				|| order.getCustomer().getNormalizedName().toLowerCase(Locale.ROOT)
						.contains(normalizedKeyword);
	}

	/** 按地区筛选（null 视为不限）。 */
	static OrderFilter byRegion(Region region) {
		return region == null ? all() : order -> order.getRegion() == region;
	}

	/** 按订单状态筛选（null 视为不限）。 */
	static OrderFilter byStatus(OrderStatus status) {
		return status == null ? all() : order -> order.getStatus() == status;
	}
}

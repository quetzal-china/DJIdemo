package com.dji.service;

import com.dji.sale.OrderStatus;
import com.dji.sale.Region;

/**
 * 订单组合查询条件（任务 6.3 / 任务 7.1）。
 *
 * 关键字、地区、状态三个条件可任意组合，均可为 null（表示不限），
 * 全部为空时等价于"清除条件"。
 *
 * @param keyword 订单号/客户名关键字（可为 null）
 * @param region  地区（可为 null）
 * @param status  订单状态（可为 null）
 * @author liuyuxin
 * @version 1.0
 */
public record OrderSearchCriteria(String keyword, Region region, OrderStatus status) {

	public static OrderSearchCriteria empty() {
		return new OrderSearchCriteria(null, null, null);
	}

	/** @return 条件是否全部为空（等价于清除条件）。 */
	public boolean isEmpty() {
		return (keyword == null || keyword.isBlank()) && region == null && status == null;
	}

	/** @return 组合筛选谓词（AND 语义）。 */
	public OrderFilter toFilter() {
		return OrderFilter.byKeyword(keyword)
				.and(OrderFilter.byRegion(region))
				.and(OrderFilter.byStatus(status));
	}
}

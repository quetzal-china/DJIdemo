package com.dji.sale;

/**
 * 订单状态枚举。
 * <p>真实 {@code order.json} 中仅出现 {@code PENDING}；按 DECISIONS.md D-009
 * （本次实施采用的默认方案，非任务书明文规定）补充生产模拟所需的
 * {@code PROCESSED}/{@code FAILED} 两个运行期状态以支持 GUI 生产流转与状态筛选。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public enum OrderStatus {

	/** 待处理（JSON 加载后的初始状态） */
	PENDING("待处理"),

	/** 已生产（生产流程全部成功） */
	PROCESSED("已生产"),

	/** 处理失败（路由/生产失败，允许重试） */
	FAILED("处理失败");

	private final String displayName;

	OrderStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 按 JSON 原文严格解析订单状态。
	 *
	 * @param code JSON 中的 status 字符串
	 * @return 状态枚举
	 * @throws OrderValidationException 取值为 null/空白或未定义时抛出
	 */
	public static OrderStatus fromCode(String code) {
		if (code != null) {
			for (OrderStatus status : values()) {
				if (status.name().equals(code)) {
					return status;
				}
			}
		}
		throw new OrderValidationException("status", "未知的订单状态: " + code);
	}
}

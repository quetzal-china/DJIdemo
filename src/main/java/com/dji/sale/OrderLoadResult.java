package com.dji.sale;

import java.nio.file.Path;
import java.util.List;

/**
 * 订单文件加载结果（任务 2 / DECISIONS.md D-006 默认方案）。
 *
 * <p>JSON 语法合法但个别订单字段/金额错误时：跳过无效订单、
 * 有效订单正常返回，全部错误进入 {@code errors} 供界面展示汇总；
 * JSON 顶层语法损坏时由仓储层抛出整份失败异常。</p>
 *
 * @param sourceFile  来源文件路径
 * @param validOrders 校验通过的订单
 * @param errors      全部校验错误（结构化、可定位）
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public record OrderLoadResult(Path sourceFile, List<Order> validOrders,
		List<OrderValidationError> errors) {

	public OrderLoadResult {
		validOrders = List.copyOf(validOrders);
		errors = List.copyOf(errors);
	}

	/** @return 是否存在校验错误。 */
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	/** @return 多行可读的错误汇总文本（无错误时为空串）。 */
	public String describeErrors() {
		StringBuilder sb = new StringBuilder();
		for (OrderValidationError error : errors) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(error.describe());
		}
		return sb.toString();
	}
}

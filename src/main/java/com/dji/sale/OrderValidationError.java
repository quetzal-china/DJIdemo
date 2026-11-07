package com.dji.sale;

/**
 * 订单校验错误结构化记录（任务 2：错误必须可定位）。
 *
 * @param recordIndex JSON 数组中的记录序号（1-based）
 * @param orderId     订单号；缺失时为 "&lt;未知订单号&gt;"
 * @param itemIndex   明细序号（1-based）；订单级错误为 null
 * @param fieldName   出错字段名
 * @param message     具体错误原因（中文，可直接展示）
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public record OrderValidationError(int recordIndex, String orderId, Integer itemIndex,
		String fieldName, String message) {

	/** @return 一行可读的错误描述（含全部定位信息）。 */
	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append("第 ").append(recordIndex).append(" 笔订单 [").append(orderId).append("]");
		if (itemIndex != null) {
			sb.append(" 第 ").append(itemIndex).append(" 条明细");
		}
		sb.append(" 字段 ").append(fieldName).append("：").append(message);
		return sb.toString();
	}
}

package com.dji.sale;

/**
 * 订单领域校验异常。
 * 携带可定位信息：出错的字段名与（若为明细级错误）明细序号，
 * 由仓储层补充记录序号与订单号后形成结构化 OrderValidationError。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class OrderValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 出错的字段名（如 "region"、"unitPrice"）。 */
	private final String fieldName;

	/** 明细序号（1-based）；订单级错误为 null。 */
	private final Integer itemIndex;

	public OrderValidationException(String fieldName, String message) {
		this(null, fieldName, message);
	}

	public OrderValidationException(Integer itemIndex, String fieldName, String message) {
		super(message);
		this.fieldName = fieldName;
		this.itemIndex = itemIndex;
	}

	public String getFieldName() {
		return fieldName;
	}

	/** @return 明细序号（1-based）；订单级错误返回 null。 */
	public Integer getItemIndex() {
		return itemIndex;
	}
}

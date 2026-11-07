package com.dji.repository;

/**
 * 订单文件级异常：文件无法读取或顶层 JSON 语法损坏时抛出（整份失败，
 * DECISIONS.md D-006 默认方案）；单笔订单的字段错误不使用本异常。
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class OrderFileParseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OrderFileParseException(String message) {
		super(message);
	}

	public OrderFileParseException(String message, Throwable cause) {
		super(message, cause);
	}
}

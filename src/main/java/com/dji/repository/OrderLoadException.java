package com.dji.repository;

import com.dji.sale.OrderLoadResult;

/**
 * 严格模式整份加载失败异常：携带完整加载结果供界面展示全部错误。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class OrderLoadException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient OrderLoadResult result;

	public OrderLoadException(String message, OrderLoadResult result) {
		super(message);
		this.result = result;
	}

	public OrderLoadResult getResult() {
		return result;
	}
}

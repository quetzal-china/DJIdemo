package com.dji.sale;

/**
 * 生产路由业务异常。
 * 当地区×产品类别组合没有可用生产基地，或某基地工厂被请求生产
 * 职责范围外的产品时抛出。系统必须明确报告该组合无法路由，
 * 不得返回 null 或静默跳过（任务 4 硬性要求）。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class ProductionRoutingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProductionRoutingException(String message) {
		super(message);
	}

	public ProductionRoutingException(String message, Throwable cause) {
		super(message, cause);
	}
}

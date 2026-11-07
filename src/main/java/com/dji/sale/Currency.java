package com.dji.sale;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单币种枚举。
 * <p>币种以 JSON 原文为准，系统不做任何汇率换算（原版任务书硬约束）。
 * 金额展示格式与参考界面一致：{@code CNY 1299.00}（币种代码 + 两位小数）。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public enum Currency {

	/** 人民币 */
	CNY("人民币"),

	/** 美元 */
	USD("美元"),

	/** 欧元 */
	EUR("欧元"),

	/** 日元 */
	JPY("日元"),

	/** 巴西雷亚尔 */
	BRL("巴西雷亚尔");

	private final String displayName;

	Currency(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 按 JSON 原文严格解析币种。
	 *
	 * @param code JSON 中的 currency 字符串
	 * @return 币种枚举
	 * @throws OrderValidationException 取值为 null/空白或未定义时抛出
	 */
	public static Currency fromCode(String code) {
		if (code != null) {
			for (Currency currency : values()) {
				if (currency.name().equals(code)) {
					return currency;
				}
			}
		}
		throw new OrderValidationException("currency", "未知的订单币种: " + code);
	}

	/**
	 * 按统一口径（scale=2, HALF_UP，见 DECISIONS.md ADR-005）格式化金额。
	 *
	 * @param amount 金额
	 * @return 如 "CNY 1299.00" 的展示字符串
	 */
	public String formatAmount(BigDecimal amount) {
		return name() + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}
}

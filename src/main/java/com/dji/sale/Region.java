package com.dji.sale;

/**
 * 订单地区枚举。
 * <p>枚举常量名严格对齐真实 {@code order.json} 的 {@code region} 字段取值
 * （如亚太地区在 JSON 中拼作 {@code ASIA}）；中文显示名对齐参考界面 sysgui.png。
 * 解析采用严格模式：未知取值立即抛出 {@link OrderValidationException}，绝不静默映射。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public enum Region {

	/** 中国大陆 */
	CHINA_MAINLAND("中国大陆"),

	/** 北美地区 */
	NORTH_AMERICA("北美"),

	/** 欧洲地区 */
	EUROPE("欧洲"),

	/** 亚太地区（JSON 原文为 ASIA） */
	ASIA("亚太"),

	/** 南美地区 */
	SOUTH_AMERICA("南美");

	private final String displayName;

	Region(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 按 JSON 原文严格解析地区。
	 *
	 * @param code JSON 中的 region 字符串
	 * @return 地区枚举
	 * @throws OrderValidationException 取值为 null/空白或未定义时抛出
	 */
	public static Region fromCode(String code) {
		if (code != null) {
			for (Region region : values()) {
				if (region.name().equals(code)) {
					return region;
				}
			}
		}
		throw new OrderValidationException("region", "未知的订单地区: " + code);
	}
}

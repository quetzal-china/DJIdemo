package com.dji.sale;

/**
 * 产品类别枚举。
 * <p>与教师基础工程 {@code DJIProduct#getProductCategory()} 返回的中文类别字符串
 * 一一对应，同时与 {@code DJIFactory} 三组型号常量的分组保持一致。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public enum ProductCategory {

	/** 消费级无人机 */
	CONSUMER("消费级无人机"),

	/** 行业级无人机 */
	ENTERPRISE("行业级无人机"),

	/** 农业及载重级无人机 */
	AGRICULTURAL("农业级无人机");

	private final String displayName;

	ProductCategory(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 获取中文显示名（与 DJIProduct#getProductCategory() 返回值一致）。
	 *
	 * @return 中文类别名
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 按中文显示名解析产品类别（用于对接教师既有 getProductCategory() 字符串）。
	 *
	 * @param displayName 中文类别名，如 "消费级无人机"
	 * @return 对应枚举
	 * @throws IllegalArgumentException 显示名无法识别时抛出
	 */
	public static ProductCategory fromDisplayName(String displayName) {
		for (ProductCategory category : values()) {
			if (category.displayName.equals(displayName)) {
				return category;
			}
		}
		throw new IllegalArgumentException("未知的产品类别: " + displayName);
	}
}

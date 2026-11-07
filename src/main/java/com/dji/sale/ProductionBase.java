package com.dji.sale;

/**
 * 生产基地枚举（七个基地，严格对应原版任务书职责表）。
 * <p>地区×类别 → 基地的路由映射集中维护于
 * {@code com.dji.factory.ProductionRouter}，本枚举只承载基地名称。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public enum ProductionBase {

	/** 深圳基地：中国大陆消费级 */
	SHENZHEN("深圳基地"),

	/** 东莞基地：中国大陆行业级 */
	DONGGUAN("东莞基地"),

	/** 顺德工厂：全球所有地区农业及载重级 */
	SHUNDE("顺德工厂"),

	/** 日本名古屋工厂：亚太地区消费级、行业级 */
	NAGOYA("日本名古屋工厂"),

	/** 荷兰阿姆斯特丹基地：欧洲地区消费级 */
	AMSTERDAM("荷兰阿姆斯特丹基地"),

	/** 德国斯图加特基地：欧洲地区行业级 */
	STUTTGART("德国斯图加特基地"),

	/** 美国加州弗里蒙特工厂：北美、南美地区消费级、行业级 */
	FREMONT("美国加州弗里蒙特工厂");

	private final String displayName;

	ProductionBase(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

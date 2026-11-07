package com.dji.factory;

import com.dji.sale.ProductionBase;
import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionRoutingException;
import com.dji.sale.Region;

/**
 * 生产路由器：地区 × 产品类别 → 生产基地/工厂映射的唯一集中维护点。
 *
 * 任务 4 硬性要求：路由规则不得在其他业务模块重复实现。
 * 路由规则严格对应原版任务书基地职责表：
 *
 *   · 规则 1（最高优先）：全球所有地区的农业及载重级 → 顺德工厂；*   · 规则 2：中国大陆消费级 → 深圳、行业级 → 东莞；*   · 规则 3：亚太地区（JSON 为 ASIA）消费级/行业级 → 日本名古屋工厂；*   · 规则 4：欧洲地区消费级 → 阿姆斯特丹、行业级 → 斯图加特；*   · 规则 5：北美、南美地区消费级/行业级 → 美国加州弗里蒙特工厂。*
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public class ProductionRouter {

	/**
	 * 解析地区与产品类别对应的生产基地。
	 *
	 * @throws ProductionRoutingException 参数为空或组合无可用基地（绝不返回 null）
	 */
	public ProductionBase resolveBase(Region region, ProductCategory category) {
		if (region == null || category == null) {
			throw new ProductionRoutingException("地区或产品类别不能为空");
		}
		// 规则 1：全球所有地区农业及载重级 -> 顺德工厂
		if (category == ProductCategory.AGRICULTURAL) {
			return ProductionBase.SHUNDE;
		}
		// 规则 2~5：按地区 + 消费级/行业级路由
		return switch (region) {
			case CHINA_MAINLAND -> category == ProductCategory.CONSUMER
					? ProductionBase.SHENZHEN : ProductionBase.DONGGUAN;
			case ASIA -> ProductionBase.NAGOYA;
			case EUROPE -> category == ProductCategory.CONSUMER
					? ProductionBase.AMSTERDAM : ProductionBase.STUTTGART;
			case NORTH_AMERICA, SOUTH_AMERICA -> ProductionBase.FREMONT;
		};
	}

	/**
	 * 解析地区与产品类别对应的基地工厂实例。
	 *
	 * @throws ProductionRoutingException 组合无可用基地
	 */
	public DJIFactory resolveFactory(Region region, ProductCategory category) {
		return factoryFor(resolveBase(region, category));
	}

	/**
	 * 按生产基地取工厂实例。
	 *
	 * @throws ProductionRoutingException 基地未绑定工厂（防御性分支）
	 */
	public DJIFactory factoryFor(ProductionBase base) {
		if (base == null) {
			throw new ProductionRoutingException("生产基地不能为空");
		}
		return switch (base) {
			case SHENZHEN -> ShenzhenFactory.INSTANCE;
			case DONGGUAN -> DongguanFactory.INSTANCE;
			case SHUNDE -> ShundeFactory.INSTANCE;
			case NAGOYA -> NagoyaFactory.INSTANCE;
			case AMSTERDAM -> AmsterdamFactory.INSTANCE;
			case STUTTGART -> StuttgartFactory.INSTANCE;
			case FREMONT -> FremontFactory.INSTANCE;
		};
	}
}

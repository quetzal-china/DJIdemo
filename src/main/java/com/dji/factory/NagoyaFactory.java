package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 日本名古屋工厂：亚太地区消费级、行业级无人机。
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class NagoyaFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final NagoyaFactory INSTANCE = new NagoyaFactory();

	public NagoyaFactory() {
		super(ProductionBase.NAGOYA, Set.of(ProductCategory.CONSUMER, ProductCategory.ENTERPRISE));
	}
}

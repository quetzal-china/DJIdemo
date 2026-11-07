package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 美国加州弗里蒙特工厂：北美、南美地区消费级、行业级无人机。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class FremontFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final FremontFactory INSTANCE = new FremontFactory();

	public FremontFactory() {
		super(ProductionBase.FREMONT, Set.of(ProductCategory.CONSUMER, ProductCategory.ENTERPRISE));
	}
}

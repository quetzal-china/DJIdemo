package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 东莞基地工厂：中国大陆行业级无人机。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class DongguanFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final DongguanFactory INSTANCE = new DongguanFactory();

	public DongguanFactory() {
		super(ProductionBase.DONGGUAN, Set.of(ProductCategory.ENTERPRISE));
	}
}

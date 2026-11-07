package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 德国斯图加特基地工厂：欧洲地区行业级无人机。
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class StuttgartFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final StuttgartFactory INSTANCE = new StuttgartFactory();

	public StuttgartFactory() {
		super(ProductionBase.STUTTGART, Set.of(ProductCategory.ENTERPRISE));
	}
}

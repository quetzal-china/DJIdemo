package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 荷兰阿姆斯特丹基地工厂：欧洲地区消费级无人机。
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class AmsterdamFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final AmsterdamFactory INSTANCE = new AmsterdamFactory();

	public AmsterdamFactory() {
		super(ProductionBase.AMSTERDAM, Set.of(ProductCategory.CONSUMER));
	}
}

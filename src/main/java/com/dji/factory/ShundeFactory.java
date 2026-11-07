package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 顺德工厂：全球所有地区的农业及载重级无人机。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class ShundeFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final ShundeFactory INSTANCE = new ShundeFactory();

	public ShundeFactory() {
		super(ProductionBase.SHUNDE, Set.of(ProductCategory.AGRICULTURAL));
	}
}

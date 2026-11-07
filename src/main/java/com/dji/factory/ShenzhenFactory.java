package com.dji.factory;

import java.util.Set;

import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;

/**
 * 深圳基地工厂：中国大陆消费级无人机。
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class ShenzhenFactory extends AbstractBaseFactory {

	/** 共享单例（由 ProductionRouter 统一分发）。 */
	public static final ShenzhenFactory INSTANCE = new ShenzhenFactory();

	public ShenzhenFactory() {
		super(ProductionBase.SHENZHEN, Set.of(ProductCategory.CONSUMER));
	}
}

package com.dji.factory;

import java.util.Collections;
import java.util.Set;

import com.dji.product.DJIProduct;
import com.dji.product.DroneSpecRegistry;
import com.dji.sale.ProductionBase;
import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionRoutingException;

/**
 * 生产基地工厂公共基类。
 *
 * 统一实现"职责范围校验 + 参数权威创建"：
 * 每个基地只能创建其职责范围内的产品类别；型号必须为
 * DroneSpecRegistry 注册的精确拼写；序列号由注册表内部统一申请，
 * 工厂与业务调用方均不得指定（DECISIONS.md 架构修正 1）。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public abstract class AbstractBaseFactory extends DJIFactory {

	private final ProductionBase base;
	private final Set<ProductCategory> allowedCategories;

	protected AbstractBaseFactory(ProductionBase base, Set<ProductCategory> allowedCategories) {
		this.base = base;
		this.allowedCategories = Set.copyOf(allowedCategories);
	}

	public final ProductionBase getBase() {
		return base;
	}

	/** @return 本基地允许生产的产品类别（只读）。 */
	public final Set<ProductCategory> getAllowedCategories() {
		return Collections.unmodifiableSet(allowedCategories);
	}

	/**
	 * 按型号生产无人机：先校验型号有效性与本基地职责范围，再经注册表创建。
	 *
	 * @throws ProductionRoutingException 型号未知，或类别超出本基地职责范围（绝不返回 null）
	 */
	@Override
	public final DJIProduct buildDrone(String type) {
		if (!DroneSpecRegistry.isValidModel(type)) {
			throw new ProductionRoutingException(
					base.getDisplayName() + "无法生产未知或无效的产品型号: " + type);
		}
		ProductCategory category = DroneSpecRegistry.getCategory(type);
		if (!allowedCategories.contains(category)) {
			throw new ProductionRoutingException(base.getDisplayName() + "仅负责 "
					+ allowedCategories.stream().map(ProductCategory::getDisplayName)
							.reduce((a, b) -> a + "、" + b).orElse("")
					+ " 产品，无法生产 " + category.getDisplayName() + " 产品: " + type);
		}
		return DroneSpecRegistry.createDrone(type);
	}
}

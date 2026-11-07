package com.dji.promotion;

import java.math.BigDecimal;

import com.dji.sale.Order;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;

/**
 * 促销策略接口（策略模式，任务 5）。
 *
 * <p>双层设计（DECISIONS.md ADR-004）：继承教师接口 {@link DJIPromotion}
 * 保持签名兼容（{@code calculatePromotion(Order)} 返回减免额），
 * 同时提供结构化计算 {@link #evaluate(Order)}，返回含活动名称、
 * 优惠金额与应付金额的 {@link PromotionResult}。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public interface PromotionStrategy extends DJIPromotion {

	/** @return 本策略对应的促销活动类型。 */
	PromotionType getType();

	/**
	 * 判断本策略是否适用于该订单（地区、币种等适用性规则）。
	 *
	 * @param order 订单
	 * @return 适用返回 true
	 */
	boolean isApplicable(Order order);

	/**
	 * 计算本策略下的结构化促销结果。
	 *
	 * @param order 订单
	 * @return 促销结果（含活动名称、优惠金额、应付金额）
	 */
	PromotionResult evaluate(Order order);

	/** 教师接口兼容：减免金额取自结构化结果的优惠额。 */
	@Override
	default BigDecimal calculatePromotion(Order order) {
		return evaluate(order).getDiscountAmount();
	}
}

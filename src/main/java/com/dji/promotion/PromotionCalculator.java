package com.dji.promotion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dji.sale.Order;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;

/**
 * 促销计算器：对订单统一评估全部适用方案并选出最优方案。
 *
 * <p>特殊边界规则（蓝图 §6.3）：若某适用活动算出的优惠金额为 0.00
 * （如大陆 1500 元订单的 0 档满减），其应付金额与"无优惠"相同；
 * 为防止 0 元减免被错误标为某种活动，0 优惠额的非"无优惠"候选不参与胜出比较，
 * 直接归入"无优惠"结算。</p>
 *
 * <p>本类无状态，可被 GUI 与测试并发共享。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public final class PromotionCalculator {

	private final List<PromotionStrategy> strategies;

	/** 标准策略集：大陆满额立减、大陆满件折扣、其他地区满件折扣、无优惠。 */
	public PromotionCalculator() {
		this(List.of(new MainlandFullReductionStrategy(),
				new MainlandTieredDiscountStrategy(),
				new OverseasTieredDiscountStrategy(),
				new NoPromotionStrategy()));
	}

	public PromotionCalculator(List<PromotionStrategy> strategies) {
		this.strategies = List.copyOf(strategies);
	}

	/**
	 * 评估订单适用的全部促销方案（含"无优惠"兜底，供界面对比展示）。
	 *
	 * @param order 订单
	 * @return 全部适用方案的结构化结果（只读）
	 */
	public List<PromotionResult> evaluateAllApplicable(Order order) {
		List<PromotionResult> candidates = new ArrayList<>();
		for (PromotionStrategy strategy : strategies) {
			if (strategy.isApplicable(order)) {
				candidates.add(strategy.evaluate(order));
			}
		}
		return Collections.unmodifiableList(candidates);
	}

	/**
	 * 选出最优促销方案：应付金额最低；并列时按"满额立减 > 满件折扣 > 无优惠"。
	 *
	 * @param order 订单
	 * @return 最优方案结果
	 */
	public PromotionResult evaluateBestPromotion(Order order) {
		List<PromotionResult> validCandidates = new ArrayList<>();
		for (PromotionResult candidate : evaluateAllApplicable(order)) {
			// 0 元减免的非"无优惠"候选不作为有效优惠胜出（直接归入无优惠）
			if (candidate.getType() == PromotionType.NO_PROMOTION
					|| candidate.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
				validCandidates.add(candidate);
			}
		}
		if (validCandidates.isEmpty()) {
			return PromotionResult.of(PromotionType.NO_PROMOTION, order.getOriginalAmount(), BigDecimal.ZERO);
		}
		return Collections.min(validCandidates, new PromotionOptionComparator());
	}
}

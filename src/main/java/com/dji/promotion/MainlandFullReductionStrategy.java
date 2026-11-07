package com.dji.promotion;

import java.math.BigDecimal;

import com.dji.sale.Currency;
import com.dji.sale.Order;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;
import com.dji.sale.Region;

/**
 * 中国大陆满额立减策略。
 *
 * <p>规则（任务书促销表）：每满 2000 元减 300 元，不设档数上限
 * （DECISIONS.md D-001 默认方案：按正文与 sysgui.png 实算结果实现无人为封顶）。</p>
 *
 * <p>适用硬性防线：仅当订单地区为中国大陆<strong>且</strong>币种为 CNY 时适用
 * （满减金额单位为人民币元；不对外币订单按人民币阈值满减）。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class MainlandFullReductionStrategy implements PromotionStrategy {

	/** 满额门槛：2000 元。 */
	public static final BigDecimal THRESHOLD = new BigDecimal("2000");

	/** 每档减免：300 元。 */
	public static final BigDecimal PER_TIER_REDUCTION = new BigDecimal("300");

	@Override
	public PromotionType getType() {
		return PromotionType.MAINLAND_FULL_REDUCTION;
	}

	@Override
	public boolean isApplicable(Order order) {
		return order.getRegion() == Region.CHINA_MAINLAND
				&& order.getCurrency() == Currency.CNY;
	}

	@Override
	public PromotionResult evaluate(Order order) {
		BigDecimal original = order.getOriginalAmount();
		// 档数 = 原价整除 2000（divideToIntegralValue，不设档数上限）
		BigDecimal tiers = original.divideToIntegralValue(THRESHOLD);
		BigDecimal discount = tiers.multiply(PER_TIER_REDUCTION).setScale(2, java.math.RoundingMode.HALF_UP);
		return PromotionResult.of(getType(), original, discount);
	}
}

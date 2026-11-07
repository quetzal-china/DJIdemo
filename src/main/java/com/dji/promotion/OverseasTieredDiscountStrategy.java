package com.dji.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.dji.sale.Order;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;
import com.dji.sale.Region;

/**
 * 其他地区（中国大陆以外）满件折扣策略。
 *
 * <p>规则（任务书促销表，按整单商品总件数 Q 分档）：
 * Q=1 → 9.5 折；Q=2 → 9 折；Q≥3 → 8.5 折（最高档封顶，D-001 理解口径）。
 * 优惠按订单原价总额计算，币种以 JSON 为准，不做汇率换算。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class OverseasTieredDiscountStrategy implements PromotionStrategy {

	private static final BigDecimal PAY_RATE_Q1 = new BigDecimal("0.95");
	private static final BigDecimal PAY_RATE_Q2 = new BigDecimal("0.90");
	private static final BigDecimal PAY_RATE_Q3_PLUS = new BigDecimal("0.85");

	@Override
	public PromotionType getType() {
		return PromotionType.OVERSEAS_TIERED_DISCOUNT;
	}

	@Override
	public boolean isApplicable(Order order) {
		return order.getRegion() != Region.CHINA_MAINLAND;
	}

	@Override
	public PromotionResult evaluate(Order order) {
		BigDecimal original = order.getOriginalAmount();
		BigDecimal payRate = payRateForQuantity(order.getTotalQuantity());
		BigDecimal payable = original.multiply(payRate).setScale(2, RoundingMode.HALF_UP);
		BigDecimal discount = original.subtract(payable);
		return PromotionResult.of(getType(), original, discount);
	}

	/** 按整单总件数确定应付率（折扣档位封顶在 8.5 折）。 */
	private BigDecimal payRateForQuantity(int totalQuantity) {
		if (totalQuantity <= 1) {
			return PAY_RATE_Q1;
		}
		if (totalQuantity == 2) {
			return PAY_RATE_Q2;
		}
		return PAY_RATE_Q3_PLUS;
	}
}

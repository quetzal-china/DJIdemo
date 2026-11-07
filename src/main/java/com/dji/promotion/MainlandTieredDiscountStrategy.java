package com.dji.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.dji.sale.Order;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;
import com.dji.sale.Region;

/**
 * 中国大陆满件折扣策略。
 *
 * <p>规则（任务书促销表，按整单商品总件数 Q 分档）：
 * Q=1 → 9 折；Q=2 → 8.6 折；Q=3 → 8.2 折；Q≥4 → 8 折（最高档封顶，D-001 理解口径）。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class MainlandTieredDiscountStrategy implements PromotionStrategy {

	private static final BigDecimal PAY_RATE_Q1 = new BigDecimal("0.90");
	private static final BigDecimal PAY_RATE_Q2 = new BigDecimal("0.86");
	private static final BigDecimal PAY_RATE_Q3 = new BigDecimal("0.82");
	private static final BigDecimal PAY_RATE_Q4_PLUS = new BigDecimal("0.80");

	@Override
	public PromotionType getType() {
		return PromotionType.MAINLAND_TIERED_DISCOUNT;
	}

	@Override
	public boolean isApplicable(Order order) {
		return order.getRegion() == Region.CHINA_MAINLAND;
	}

	@Override
	public PromotionResult evaluate(Order order) {
		BigDecimal original = order.getOriginalAmount();
		BigDecimal payRate = payRateForQuantity(order.getTotalQuantity());
		BigDecimal payable = original.multiply(payRate).setScale(2, RoundingMode.HALF_UP);
		BigDecimal discount = original.subtract(payable);
		return PromotionResult.of(getType(), original, discount);
	}

	/** 按整单总件数确定应付率（折扣档位封顶在 8 折）。 */
	private BigDecimal payRateForQuantity(int totalQuantity) {
		if (totalQuantity <= 1) {
			return PAY_RATE_Q1;
		}
		if (totalQuantity == 2) {
			return PAY_RATE_Q2;
		}
		if (totalQuantity == 3) {
			return PAY_RATE_Q3;
		}
		return PAY_RATE_Q4_PLUS;
	}
}

package com.dji.promotion;

import java.math.BigDecimal;

import com.dji.sale.Order;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;

/**
 * 无优惠兜底策略：订单不满足任何活动门槛时按原价结算。
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class NoPromotionStrategy implements PromotionStrategy {

	@Override
	public PromotionType getType() {
		return PromotionType.NO_PROMOTION;
	}

	@Override
	public boolean isApplicable(Order order) {
		return true;
	}

	@Override
	public PromotionResult evaluate(Order order) {
		return PromotionResult.of(getType(), order.getOriginalAmount(), BigDecimal.ZERO);
	}
}

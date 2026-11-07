package com.dji.promotion;

import java.math.BigDecimal;

import com.dji.sale.Order;

/**
 * 大疆打折促销接口
 * 
 * @author liyong
 * @version 1.0
 *
 */
public interface DJIPromotion {

	/**
	 * 根据订单计算减免金额
	 * 
	 * @param order 订单
	 * @return 减免金额
	 */
	public BigDecimal calculatePromotion(Order order);

}

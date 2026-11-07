package com.dji.promotion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

import com.dji.sale.PromotionResult;

/**
 * 促销方案优劣比较器。
 *
 * 任务 5 并列优先规则：应付金额最低者优先；应付金额并列时，
 * 依次优先选择"满额立减"（rank 1）→"满件折扣"（rank 2）→"无优惠"（rank 3）。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public final class PromotionOptionComparator implements Comparator<PromotionResult>, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public int compare(PromotionResult a, PromotionResult b) {
		int priceCmp = a.getPayableAmount().compareTo(b.getPayableAmount());
		if (priceCmp != 0) {
			return priceCmp;
		}
		// 应付金额并列：满额立减(1) > 满件折扣(2) > 无优惠(3)
		return Integer.compare(a.getType().getTieBreakRank(), b.getType().getTieBreakRank());
	}
}

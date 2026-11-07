package com.dji.sale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 促销计算结构化结果值对象（不可变）。
 *
 * 满足任务 5“返回结构化结果，至少包含活动名称、优惠金额、应付金额”的要求，
 * 并保证恒等式 应付金额 = 原价金额 - 优惠金额（scale=2、HALF_UP，ADR-005）。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public final class PromotionResult {

	private final PromotionType type;
	private final BigDecimal originalAmount;
	private final BigDecimal discountAmount;
	private final BigDecimal payableAmount;

	private PromotionResult(PromotionType type, BigDecimal originalAmount,
			BigDecimal discountAmount, BigDecimal payableAmount) {
		this.type = type;
		this.originalAmount = originalAmount;
		this.discountAmount = discountAmount;
		this.payableAmount = payableAmount;
	}

	/**
	 * 按原价与优惠额构造结果，应付金额 = 原价 − 优惠。
	 *
	 * @param type           促销活动类型
	 * @param originalAmount 订单原价总额（scale=2）
	 * @param discountAmount 优惠金额（0 ≤ 优惠 ≤ 原价）
	 * @return 结构化促销结果
	 * @throws IllegalArgumentException 优惠额为负或超过原价
	 */
	public static PromotionResult of(PromotionType type, BigDecimal originalAmount,
			BigDecimal discountAmount) {
		Objects.requireNonNull(type, "type 不能为空");
		BigDecimal original = originalAmount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal discount = discountAmount.setScale(2, RoundingMode.HALF_UP);
		if (discount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("优惠金额不能为负: " + discount.toPlainString());
		}
		if (discount.compareTo(original) > 0) {
			throw new IllegalArgumentException("优惠金额不能超过原价: 优惠="
					+ discount.toPlainString() + "，原价=" + original.toPlainString());
		}
		BigDecimal payable = original.subtract(discount);
		return new PromotionResult(type, original, discount, payable);
	}

	public PromotionType getType() {
		return type;
	}

	/** @return 活动名称（如 “中国大陆满件折扣”）。 */
	public String getPromotionName() {
		return type.getDisplayName();
	}

	/** @return 订单原价总额（scale=2）。 */
	public BigDecimal getOriginalAmount() {
		return originalAmount;
	}

	/** @return 优惠金额（scale=2）。 */
	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}

	/** @return 应付金额 = 原价 − 优惠（scale=2）。 */
	public BigDecimal getPayableAmount() {
		return payableAmount;
	}

	@Override
	public String toString() {
		return type.getDisplayName() + " 优惠 " + discountAmount.toPlainString()
				+ " 应付 " + payableAmount.toPlainString();
	}
}

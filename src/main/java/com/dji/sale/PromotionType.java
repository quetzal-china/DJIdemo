package com.dji.sale;

/**
 * 促销活动类型枚举。
 *
 * <p>并列优先规则（任务 5）：应付金额并列时依次优先选择
 * 满额立减 → 满件折扣 → 无优惠，由 {@code tieBreakRank} 表达（数值越小越优先）。</p>
 *
 * <p><b>归属包说明</b>：按 DECISIONS.md 实施备注，本枚举置于领域层
 * {@code com.dji.sale}（领域结果对象 PromotionResult 需引用它），
 * 以保持依赖单向 PROMO→SALE，不构成架构方向变更。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public enum PromotionType {

	/** 中国大陆满额立减：每满 2000 元减 300 元（不设档数上限，D-001 默认方案） */
	MAINLAND_FULL_REDUCTION(1, "中国大陆每满2000减300"),

	/** 中国大陆满件折扣：满 1 件 9 折、2 件 8.6 折、3 件 8.2 折、4 件及以上 8 折 */
	MAINLAND_TIERED_DISCOUNT(2, "中国大陆满件折扣"),

	/** 其他地区满件折扣：满 1 件 9.5 折、2 件 9 折、3 件及以上 8.5 折 */
	OVERSEAS_TIERED_DISCOUNT(2, "其他地区满件折扣"),

	/** 无优惠（不满足任何活动门槛时按原价结算） */
	NO_PROMOTION(3, "无优惠");

	private final int tieBreakRank;
	private final String displayName;

	PromotionType(int tieBreakRank, String displayName) {
		this.tieBreakRank = tieBreakRank;
		this.displayName = displayName;
	}

	/** @return 并列优先级（1=满额立减 &lt; 2=满件折扣 &lt; 3=无优惠）。 */
	public int getTieBreakRank() {
		return tieBreakRank;
	}

	public String getDisplayName() {
		return displayName;
	}
}

package com.dji.sale;

import java.util.List;

/**
 * 生产任务值对象（不可变）。
 *
 * 记录一笔订单明细在某个生产基地的生产结果：计划数量与实际成功分配的
 * 序列号列表。serialNumbers.size() == quantity 表示该明细生产完成；
 * 小于则为部分完成（生产中途失败，见 DECISIONS.md 架构修正 2）。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public final class ProductionTask {

	private final String orderId;
	private final int itemIndex;
	private final ProductionBase base;
	private final String productType;
	/** 本任务计划生产数量。 */
	private final int quantity;
	/** 实际成功分配的序列号（已消耗的序列号不回收倒退）。 */
	private final List<String> serialNumbers;

	public ProductionTask(String orderId, int itemIndex, ProductionBase base, String productType,
			int quantity, List<String> serialNumbers) {
		this.orderId = orderId;
		this.itemIndex = itemIndex;
		this.base = base;
		this.productType = productType;
		this.quantity = quantity;
		this.serialNumbers = List.copyOf(serialNumbers);
	}

	public String getOrderId() {
		return orderId;
	}

	public int getItemIndex() {
		return itemIndex;
	}

	public ProductionBase getBase() {
		return base;
	}

	public String getProductType() {
		return productType;
	}

	/** @return 计划生产数量。 */
	public int getQuantity() {
		return quantity;
	}

	/** @return 实际成功分配的序列号列表（只读）。 */
	public List<String> getSerialNumbers() {
		return serialNumbers;
	}

	/** @return 实际成功生产的架数。 */
	public int getProducedCount() {
		return serialNumbers.size();
	}

	/** @return 该明细是否已全部生产完成。 */
	public boolean isComplete() {
		return serialNumbers.size() == quantity;
	}

	@Override
	public String toString() {
		return base.getDisplayName() + " | " + productType + " x " + producedCountText()
				+ " | SN" + (serialNumbers.size() == 1 ? ": " + serialNumbers.get(0) : "数: " + serialNumbers.size());
	}

	private String producedCountText() {
		return serialNumbers.size() + "/" + quantity;
	}
}

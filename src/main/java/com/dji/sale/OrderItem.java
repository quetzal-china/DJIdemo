package com.dji.sale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 订单明细值对象（不可变）。
 *
 * <p>不变量（任务 2 硬性要求，构造期强制）：
 * 明细序号 ≥ 1；产品型号非空且类别已解析；数量为正整数；单价非负；
 * 明细金额必须等于单价 × 数量（金额口径 scale=2、HALF_UP，见 ADR-005）。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public final class OrderItem {

	private final int itemIndex;
	private final String productType;
	private final ProductCategory category;
	private final int quantity;
	private final BigDecimal unitPrice;
	private final BigDecimal totalAmount;

	/**
	 * @param itemIndex          明细在订单中的序号（1-based，用于错误定位）
	 * @param productType        精确产品型号（如 "Mini 4 PRO"）
	 * @param category           产品类别（由仓储层经 DroneSpecRegistry 解析后传入）
	 * @param quantity           购买数量（正整数）
	 * @param unitPrice          单价（非负）
	 * @param declaredTotalAmount JSON 声明的明细金额，必须等于单价 × 数量
	 * @throws OrderValidationException 任一不变量被破坏（携带明细序号与字段名）
	 */
	public OrderItem(int itemIndex, String productType, ProductCategory category, int quantity,
			BigDecimal unitPrice, BigDecimal declaredTotalAmount) {
		if (itemIndex < 1) {
			throw new OrderValidationException(itemIndex, "items", "明细序号必须从 1 开始: " + itemIndex);
		}
		if (productType == null || productType.isBlank()) {
			throw new OrderValidationException(itemIndex, "productType", "产品型号不能为空");
		}
		if (category == null) {
			throw new OrderValidationException(itemIndex, "productType",
					"产品型号无法识别或未提供类别: " + productType);
		}
		if (quantity <= 0) {
			throw new OrderValidationException(itemIndex, "quantity",
					"第 " + itemIndex + " 条明细购买数量必须为正整数，实际: " + quantity);
		}
		if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new OrderValidationException(itemIndex, "unitPrice",
					"第 " + itemIndex + " 条明细单价不能为负或缺失，实际: " + unitPrice);
		}
		if (declaredTotalAmount == null) {
			throw new OrderValidationException(itemIndex, "totalAmount",
					"第 " + itemIndex + " 条明细金额缺失");
		}
		BigDecimal computedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal declared = declaredTotalAmount.setScale(2, RoundingMode.HALF_UP);
		if (computedTotal.compareTo(declared) != 0) {
			throw new OrderValidationException(itemIndex, "totalAmount",
					"第 " + itemIndex + " 条明细金额不一致: 单价×数量=" + computedTotal.toPlainString()
							+ "，声明金额=" + declared.toPlainString());
		}
		this.itemIndex = itemIndex;
		this.productType = productType;
		this.category = category;
		this.quantity = quantity;
		this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
		this.totalAmount = computedTotal;
	}

	public int getItemIndex() {
		return itemIndex;
	}

	public String getProductType() {
		return productType;
	}

	public ProductCategory getCategory() {
		return category;
	}

	public int getQuantity() {
		return quantity;
	}

	/** @return 单价（scale=2）。 */
	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	/** @return 明细金额 = 单价 × 数量（scale=2）。 */
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OrderItem item)) {
			return false;
		}
		return itemIndex == item.itemIndex
				&& quantity == item.quantity
				&& productType.equals(item.productType)
				&& category == item.category
				&& unitPrice.compareTo(item.unitPrice) == 0
				&& totalAmount.compareTo(item.totalAmount) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemIndex, productType, category, quantity,
				unitPrice.stripTrailingZeros(), totalAmount.stripTrailingZeros());
	}

	@Override
	public String toString() {
		return productType + " x " + quantity + " @ " + unitPrice.toPlainString();
	}
}

package com.dji.sale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 订单聚合根（兼容教师接口 com.dji.promotion.DJIPromotion 的签名）。
 *
 * 不变量（任务 2 硬性要求，构造期强制）：订单号非空；客户/地区/币种/时间非空；
 * 明细列表非空；订单总金额必须等于全部明细金额之和（scale=2、HALF_UP）。
 * 明细列表对外只读；订单状态默认 OrderStatus#PENDING，
 * 仅允许按 D-009 流转：生产成功 → PROCESSED，失败 → FAILED。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public final class Order {

	private final String orderId;
	private final Customer customer;
	private final Region region;
	private final Currency currency;
	private final LocalDateTime orderDate;
	private final List<OrderItem> items;
	private final BigDecimal totalAmount;

	private volatile OrderStatus status = OrderStatus.PENDING;

	/**
	 * @param orderId             订单号（非空；唯一性由仓储层加载时保证）
	 * @param customer            客户
	 * @param region              地区
	 * @param currency            币种
	 * @param orderDate           下单时间
	 * @param items               明细列表（非空，至少一条）
	 * @param declaredTotalAmount JSON 声明的订单总金额，必须等于明细金额之和
	 * @throws OrderValidationException 任一不变量被破坏
	 */
	public Order(String orderId, Customer customer, Region region, Currency currency,
			LocalDateTime orderDate, List<OrderItem> items, BigDecimal declaredTotalAmount) {
		if (orderId == null || orderId.isBlank()) {
			throw new OrderValidationException("orderId", "订单号不能为空");
		}
		this.orderId = orderId.trim();
		this.customer = Objects.requireNonNull(customer, "customer 不能为空");
		this.region = Objects.requireNonNull(region, "region 不能为空");
		this.currency = Objects.requireNonNull(currency, "currency 不能为空");
		this.orderDate = Objects.requireNonNull(orderDate, "orderDate 不能为空");
		if (items == null || items.isEmpty()) {
			throw new OrderValidationException("items", "订单 " + this.orderId + " 至少需要一条商品明细");
		}
		this.items = List.copyOf(items);
		if (declaredTotalAmount == null) {
			throw new OrderValidationException("totalAmount", "订单 " + this.orderId + " 总金额缺失");
		}
		BigDecimal computedTotal = this.items.stream()
				.map(OrderItem::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal declared = declaredTotalAmount.setScale(2, RoundingMode.HALF_UP);
		if (computedTotal.compareTo(declared) != 0) {
			throw new OrderValidationException("totalAmount",
					"订单 " + this.orderId + " 总金额与明细之和不一致: 明细合计="
							+ computedTotal.toPlainString() + "，声明金额=" + declared.toPlainString());
		}
		this.totalAmount = computedTotal;
	}

	public String getOrderId() {
		return orderId;
	}

	public Customer getCustomer() {
		return customer;
	}

	public Region getRegion() {
		return region;
	}

	public Currency getCurrency() {
		return currency;
	}

	public LocalDateTime getOrderDate() {
		return orderDate;
	}

	/** @return 只读明细列表。 */
	public List<OrderItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	/** @return 订单总金额 = Σ 明细金额（scale=2）。 */
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	/** @return 原价金额（scale=2，语义与任务 5 的“订单商品原价总额”一致）。 */
	public BigDecimal getOriginalAmount() {
		return totalAmount;
	}

	/** @return 整单商品总件数 = Σ 明细数量（促销折扣档位依据）。 */
	public int getTotalQuantity() {
		return items.stream().mapToInt(OrderItem::getQuantity).sum();
	}

	public synchronized OrderStatus getStatus() {
		return status;
	}

	/** 生产流程全部成功后流转为已生产（D-009 默认方案）。 */
	public synchronized void markProcessed() {
		this.status = OrderStatus.PROCESSED;
	}

	/** 生产失败后流转为处理失败（可重试）。 */
	public synchronized void markFailed() {
		this.status = OrderStatus.FAILED;
	}

	/**
	 * 供仓储层从订单文件恢复状态字段（运行期状态，JSON 中通常为 PENDING）。
	 *
	 * @param restored 文件中声明的状态
	 */
	public synchronized void restoreStatus(OrderStatus restored) {
		this.status = Objects.requireNonNull(restored, "restored status 不能为空");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Order order)) {
			return false;
		}
		return orderId.equals(order.orderId);
	}

	@Override
	public int hashCode() {
		return orderId.hashCode();
	}

	@Override
	public String toString() {
		return "Order[" + orderId + " " + customer.getNormalizedName() + " " + region + " "
				+ currency + " " + totalAmount.toPlainString() + "]";
	}
}

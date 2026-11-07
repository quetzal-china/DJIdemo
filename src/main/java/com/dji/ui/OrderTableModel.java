package com.dji.ui;

import java.util.List;
import java.util.function.Function;

import javax.swing.table.AbstractTableModel;

import com.dji.sale.Order;
import com.dji.sale.PromotionResult;

/**
 * 订单列表表格模型（任务 6.2）。
 *
 * 列定义与参考界面 sysgui.png 一致；促销方案/优惠金额/应付金额三列
 * 由外部注入的解析函数（订单号 → 最优促销结果）提供，模型不实现任何
 * 促销规则（UI 与业务解耦）。本类不创建任何 AWT 对等组件，可在
 * 无头（headless）JUnit 环境直接测试。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public class OrderTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	/** 表格列名（10 列）。 */
	public static final String[] COLUMNS = {
			"订单号", "客户", "地区", "币种", "状态", "件数", "原价", "促销方案", "优惠金额", "应付金额"};

	private List<Order> orders = List.of();
	private final transient Function<String, PromotionResult> promoResolver;

	/**
	 * @param promoResolver 订单号 → 最优促销结果（未加载时返回 null，单元格显示占位符）
	 */
	public OrderTableModel(Function<String, PromotionResult> promoResolver) {
		this.promoResolver = promoResolver;
	}

	/** 整体替换展示数据（须在 EDT 调用）。 */
	public void setOrders(List<Order> orders) {
		this.orders = List.copyOf(orders);
		fireTableDataChanged();
	}

	/** @return 行对应订单（供"处理选中订单"与详情联动）。 */
	public Order getOrderAt(int rowIndex) {
		return orders.get(rowIndex);
	}

	/** @return 当前展示的全部订单（供刷新时保留筛选结果）。 */
	public List<Order> currentOrders() {
		return orders;
	}

	@Override
	public int getRowCount() {
		return orders.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMNS[column];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Order order = orders.get(rowIndex);
		return switch (columnIndex) {
			case 0 -> order.getOrderId();
			case 1 -> order.getCustomer().getNormalizedName();
			case 2 -> order.getRegion().getDisplayName();
			case 3 -> order.getCurrency().name();
			case 4 -> order.getStatus().getDisplayName();
			case 5 -> order.getTotalQuantity();
			case 6 -> order.getCurrency().formatAmount(order.getOriginalAmount());
			case 7 -> promotionText(order);
			case 8 -> promotionAmountText(order, PromotionResult::getDiscountAmount);
			case 9 -> promotionAmountText(order, PromotionResult::getPayableAmount);
			default -> "";
		};
	}

	private Object promotionText(Order order) {
		PromotionResult result = promoResolver.apply(order.getOrderId());
		return result == null ? "尚未计算" : result.getPromotionName();
	}

	private Object promotionAmountText(Order order, Function<PromotionResult, java.math.BigDecimal> extractor) {
		PromotionResult result = promoResolver.apply(order.getOrderId());
		return result == null ? "—" : order.getCurrency().formatAmount(extractor.apply(result));
	}
}

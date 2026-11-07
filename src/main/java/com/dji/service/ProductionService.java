package com.dji.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.dji.factory.AbstractBaseFactory;
import com.dji.factory.DJIFactory;
import com.dji.factory.ProductionRouter;
import com.dji.product.DJIProduct;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;
import com.dji.sale.ProductionBase;
import com.dji.sale.ProductionRoutingException;
import com.dji.sale.ProductionTask;

/**
 * 生产编排服务（任务 4/6 的服务层支撑）。
 *
 * 生产一致性语义（DECISIONS.md D-007 + 架构修正 2）：
 *
 *   · 路由前置原子校验：生产前先对整单全部明细做路由校验，
 *       任一明细无法路由则整单失败，不消耗任何序列号；*   · 中途失败不虚报：产品创建或生产流程中途异常时，
 *       已完成明细的任务与序列号如实保留（部分成功 ≠ 整单成功），
 *       订单标记 FAILED，失败原因入档；已消耗序列号不回收倒退；*   · 幂等重试：已 SUCCESS 的订单再次处理直接跳过并返回既有任务
 *       （不重复消耗序列号、不重复累加基地产量）；FAILED/PARTIAL 订单重试时
 *       只补产缺失数量，已完成明细不重复生产。*
 *
 * 统计口径（架构修正 3）：#producedUnitsByBase() 只统计
 * 实际生产成功的架数，与 OrderStatisticsService 的预计需求量分列。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public class ProductionService {

	/** 单笔订单生产档案状态。 */
	public enum RecordStatus {
		/** 全部明细生产完成。 */
		SUCCESS,
		/** 至少有部分序列号已产出但整单未完成（中途失败，可重试补产）。 */
		PARTIAL,
		/** 尚无任何明细成功（路由失败或首件即失败，可重试）。 */
		FAILED
	}

	/** 单笔订单的累计生产档案（以 record 实例为锁粒度）。 */
	public static final class OrderProductionRecord {
		private final String orderId;
		private RecordStatus status = RecordStatus.FAILED;
		private String failureReason;
		private final Map<Integer, ProductionTask> tasksByItemIndex = new LinkedHashMap<>();

		private OrderProductionRecord(String orderId) {
			this.orderId = orderId;
		}

		public String getOrderId() {
			return orderId;
		}

		public synchronized RecordStatus getStatus() {
			return status;
		}

		/** @return 失败原因；成功时为 null。 */
		public synchronized String getFailureReason() {
			return failureReason;
		}

		/** @return 累计生产任务（按明细序号排列，只读）。 */
		public synchronized List<ProductionTask> getTasks() {
			return List.copyOf(tasksByItemIndex.values());
		}
	}

	/** 单笔订单单次处理结果。 */
	public record ProductionOutcome(String orderId, boolean success, String message,
			List<ProductionTask> tasksThisRun, RecordStatus recordStatus) {

		public ProductionOutcome {
			tasksThisRun = List.copyOf(tasksThisRun);
		}
	}

	/** 批量处理结果。 */
	public record BatchProductionResult(int totalOrders, int successCount, int failedCount,
			List<ProductionOutcome> outcomes) {

		public BatchProductionResult {
			outcomes = List.copyOf(outcomes);
		}
	}

	/** 生产中途失败异常（携带档案，供调用方继续处理其余订单）。 */
	public static final class ProductionFailureException extends RuntimeException {

		private static final long serialVersionUID = 1L;
		private final transient OrderProductionRecord record;

		ProductionFailureException(String message, OrderProductionRecord record, Throwable cause) {
			super(message, cause);
			this.record = record;
		}

		public OrderProductionRecord getRecord() {
			return record;
		}
	}

	private final ProductionRouter router;
	private final Map<String, OrderProductionRecord> records = new ConcurrentHashMap<>();

	public ProductionService() {
		this(new ProductionRouter());
	}

	/** 支持注入路由器（测试可注入桩件模拟生产故障）。 */
	public ProductionService(ProductionRouter router) {
		this.router = Objects.requireNonNull(router, "router 不能为空");
	}

	/**
	 * 处理单笔订单生产（幂等）。
	 *
	 * @param order 待生产订单
	 * @return 本次处理结果
	 * @throws ProductionFailureException 生产中途失败（档案保留已完成部分）
	 */
	public ProductionOutcome processOrder(Order order) {
		OrderProductionRecord record =
				records.computeIfAbsent(order.getOrderId(), OrderProductionRecord::new);
		synchronized (record) {
			// 幂等跳过：已成功订单返回既有任务，不再消耗序列号
			if (record.status == RecordStatus.SUCCESS) {
				return new ProductionOutcome(order.getOrderId(), true,
						"该订单已完成生产，无需重复生成序列号（幂等跳过）",
						record.getTasks(), RecordStatus.SUCCESS);
			}

			// 路由前置原子校验：任一明细无法路由 → 整单失败，不消耗序列号
			Map<Integer, DJIFactory> factoryByItemIndex = new LinkedHashMap<>();
			for (OrderItem item : order.getItems()) {
				try {
					factoryByItemIndex.put(item.getItemIndex(),
							router.resolveFactory(order.getRegion(), item.getCategory()));
				} catch (ProductionRoutingException e) {
					String reason = "订单 " + order.getOrderId() + " 第 " + item.getItemIndex()
							+ " 条明细无法路由: " + e.getMessage();
					markRecordFailure(record, reason);
					order.markFailed();
					return new ProductionOutcome(order.getOrderId(), false, reason,
							List.of(), RecordStatus.FAILED);
				}
			}

			// 逐明细生产：已完成明细不重复生产（失败重试只补产缺失数量）
			List<ProductionTask> tasksThisRun = new ArrayList<>();
			for (OrderItem item : order.getItems()) {
				ProductionTask existing = record.tasksByItemIndex.get(item.getItemIndex());
				if (existing != null && existing.isComplete()) {
					continue;
				}
				ProductionTask task = produceItem(order, item,
						factoryByItemIndex.get(item.getItemIndex()), record, existing);
				record.tasksByItemIndex.put(item.getItemIndex(), task);
				tasksThisRun.add(task);
			}

			record.status = RecordStatus.SUCCESS;
			record.failureReason = null;
			order.markProcessed();
			return new ProductionOutcome(order.getOrderId(), true,
					"生产完成：共 " + order.getTotalQuantity() + " 架",
					tasksThisRun, RecordStatus.SUCCESS);
		}
	}

	/**
	 * 生产单条明细（补产模式下从 existing 的已完成数量续产）。
	 * 中途失败时保留已完成部分（已消耗序列号不回收倒退）并抛出 ProductionFailureException。
	 */
	private ProductionTask produceItem(Order order, OrderItem item, DJIFactory factory,
			OrderProductionRecord record, ProductionTask existing) {
		List<String> serials = existing == null
				? new ArrayList<>()
				: new ArrayList<>(existing.getSerialNumbers());
		try {
			for (int i = serials.size(); i < item.getQuantity(); i++) {
				DJIProduct product = factory.orderDrone(item.getProductType());
				serials.add(product.getSeries());
			}
		} catch (ProductionRoutingException e) {
			throw e;
		} catch (Exception e) {
			if (!serials.isEmpty()) {
				record.tasksByItemIndex.put(item.getItemIndex(),
						new ProductionTask(order.getOrderId(), item.getItemIndex(),
								baseOf(factory), item.getProductType(), item.getQuantity(), serials));
			}
			String reason = "订单 " + order.getOrderId() + " 第 " + item.getItemIndex()
					+ " 条明细生产中途失败（型号 " + item.getProductType() + "）: " + e.getMessage();
			markRecordFailure(record, reason);
			order.markFailed();
			throw new ProductionFailureException(reason, record, e);
		}
		return new ProductionTask(order.getOrderId(), item.getItemIndex(), baseOf(factory),
				item.getProductType(), item.getQuantity(), serials);
	}

	private ProductionBase baseOf(DJIFactory factory) {
		return factory instanceof AbstractBaseFactory baseFactory ? baseFactory.getBase() : null;
	}

	/**
	 * 批量处理全部订单（逐笔幂等，单笔失败不影响后续订单）。
	 *
	 * @param orders 待处理订单
	 * @return 批量结果
	 */
	public BatchProductionResult processAll(List<Order> orders) {
		int success = 0;
		int failed = 0;
		List<ProductionOutcome> outcomes = new ArrayList<>();
		for (Order order : orders) {
			try {
				ProductionOutcome outcome = processOrder(order);
				outcomes.add(outcome);
				if (outcome.success()) {
					success++;
				} else {
					failed++;
				}
			} catch (ProductionFailureException e) {
				outcomes.add(new ProductionOutcome(e.getRecord().getOrderId(), false,
						e.getMessage(), e.getRecord().getTasks(), e.getRecord().getStatus()));
				failed++;
			}
		}
		return new BatchProductionResult(orders.size(), success, failed, outcomes);
	}

	/** @return 指定订单的生产档案（无则 null）。 */
	public OrderProductionRecord getRecord(String orderId) {
		return records.get(orderId);
	}

	/** @return 订单是否已有 SUCCESS 档案。 */
	public boolean isProcessed(String orderId) {
		OrderProductionRecord record = records.get(orderId);
		return record != null && record.getStatus() == RecordStatus.SUCCESS;
	}

	/** @return 全部累计生产任务（只读）。 */
	public List<ProductionTask> allTasks() {
		return records.values().stream()
				.flatMap(record -> record.getTasks().stream())
				.toList();
	}

	/**
	 * 实际生产成功量（按基地；部分完成的明细计入其已产部分）。
	 * 统计口径为"实际已生产"，与预计基地需求量分列（架构修正 3）。
	 *
	 * @return 基地 → 实际已生产架数
	 */
	public Map<ProductionBase, Integer> producedUnitsByBase() {
		Map<ProductionBase, Integer> produced = new EnumMap<>(ProductionBase.class);
		for (OrderProductionRecord record : records.values()) {
			for (ProductionTask task : record.getTasks()) {
				if (task.getBase() != null) {
					produced.merge(task.getBase(), task.getProducedCount(), Integer::sum);
				}
			}
		}
		return Collections.unmodifiableMap(produced);
	}

	/** @return 实际已生产总架数。 */
	public int producedUnitsTotal() {
		return allTasks().stream().mapToInt(ProductionTask::getProducedCount).sum();
	}

	private void markRecordFailure(OrderProductionRecord record, String reason) {
		record.status = record.tasksByItemIndex.isEmpty() ? RecordStatus.FAILED : RecordStatus.PARTIAL;
		record.failureReason = reason;
	}
}

package com.dji.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dji.factory.ProductionRouter;
import com.dji.sale.Currency;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;
import com.dji.sale.ProductionBase;
import com.dji.sale.PromotionResult;
import com.dji.sale.Region;

/**
 * 订单统计服务（任务 7.2/7.3/7.6：groupingBy 分组统计与明细扁平化）。
 *
 * <p>统计口径（架构修正 3）：本服务产出的
 * {@link #countUnitsByProductionBase(List, ProductionRouter)} 是
 * <strong>预计基地需求量</strong>（订单明细静态汇总）；
 * 实际生产成功量由 {@code ProductionService#producedUnitsByBase()} 提供，
 * 两者在汇总面板中分开展示，未执行生产时不得把预计量当作已产量。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class OrderStatisticsService {

	/**
	 * 统计各地区订单数（groupingBy + counting）。
	 *
	 * @param orders 订单列表
	 * @return 地区 → 订单数
	 */
	public Map<Region, Long> countOrdersByRegion(List<Order> orders) {
		return orders.stream()
				.collect(Collectors.groupingBy(Order::getRegion, Collectors.counting()));
	}

	/**
	 * 按币种分组汇总优惠总额（严禁跨币种相加，D-004 默认方案）。
	 *
	 * @param orders        订单列表
	 * @param promoResolver 订单 → 最优促销结果的解析函数
	 * @return 币种 → 优惠总额（scale=2）
	 */
	public Map<Currency, BigDecimal> sumDiscountByCurrency(List<Order> orders,
			Function<Order, PromotionResult> promoResolver) {
		return orders.stream()
				.collect(Collectors.groupingBy(Order::getCurrency,
						Collectors.mapping(promoResolver,
								Collectors.reducing(BigDecimal.ZERO,
										PromotionResult::getDiscountAmount, BigDecimal::add))));
	}

	/**
	 * 按币种分组汇总某类金额（原价/应付等），内部使用 reduce 累加。
	 *
	 * @param orders    订单列表
	 * @param extractor 订单 → 金额
	 * @return 币种 → 金额合计（scale=2）
	 */
	public Map<Currency, BigDecimal> sumAmountByCurrency(List<Order> orders,
			Function<Order, BigDecimal> extractor) {
		return orders.stream()
				.collect(Collectors.groupingBy(Order::getCurrency,
						Collectors.reducing(BigDecimal.ZERO,
								order -> extractor.apply(order).setScale(2, java.math.RoundingMode.HALF_UP),
								BigDecimal::add)));
	}

	/**
	 * 扁平化全部订单明细，统计各生产基地承接的产品<strong>预计需求</strong>总架数
	 * （flatMap + groupingBy + summingInt；架构修正 3：这是计划口径）。
	 *
	 * @param orders 订单列表
	 * @param router 集中生产路由器
	 * @return 基地 → 预计需求架数
	 */
	public Map<ProductionBase, Integer> countUnitsByProductionBase(List<Order> orders,
			ProductionRouter router) {
		return orders.stream()
				.flatMap(order -> order.getItems().stream()
						.map(item -> Map.entry(
								router.resolveBase(order.getRegion(), item.getCategory()),
								item.getQuantity())))
				.collect(Collectors.groupingBy(Map.Entry::getKey,
						Collectors.summingInt(Map.Entry::getValue)));
	}

	/**
	 * 扁平化全部订单明细，按型号统计总需求量并降序排列（flatMap + groupingBy + sorted）。
	 *
	 * @param orders 订单列表
	 * @return 型号 → 需求架数（按数量降序的 LinkedHashMap）
	 */
	public Map<String, Integer> sumQuantityByProductModel(List<Order> orders) {
		Map<String, Integer> counted = orders.stream()
				.flatMap(order -> order.getItems().stream())
				.collect(Collectors.groupingBy(OrderItem::getProductType,
						Collectors.summingInt(OrderItem::getQuantity)));
		return counted.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(a, b) -> a, LinkedHashMap::new));
	}

	/**
	 * 汇总系统统计面板数据（任务 6.6；区分预计需求与实际生产两个口径）。
	 *
	 * @param orders        全部有效订单
	 * @param promoResolver 最优促销解析函数
	 * @param router        集中生产路由器
	 * @return 汇总快照
	 */
	public SystemSummary buildSummary(List<Order> orders,
			Function<Order, PromotionResult> promoResolver, ProductionRouter router,
			ProductionService productionService) {
		int processedCount = (int) orders.stream()
				.filter(order -> order.getStatus() == com.dji.sale.OrderStatus.PROCESSED)
				.count();
		int failedCount = (int) orders.stream()
				.filter(order -> order.getStatus() == com.dji.sale.OrderStatus.FAILED)
				.count();
		return new SystemSummary(
				orders.size(),
				processedCount,
				failedCount,
				Map.copyOf(countOrdersByRegion(orders)),
				Map.copyOf(sumDiscountByCurrency(orders, promoResolver)),
				Map.copyOf(countUnitsByProductionBase(orders, router)),
				Map.copyOf(productionService.producedUnitsByBase()),
				// 型号统计保持数量降序（LinkedHashMap），不可用无序的 Map.copyOf
				java.util.Collections.unmodifiableMap(sumQuantityByProductModel(orders)));
	}
}

package com.dji.service;

import java.math.BigDecimal;
import java.util.Map;

import com.dji.sale.Currency;
import com.dji.sale.ProductionBase;
import com.dji.sale.Region;

/**
 * 系统汇总快照（任务 6.6 汇总面板数据源）。
 *
 * 统计口径（架构修正 3）：plannedUnitsByBase 为订单明细
 * 静态推算的预计基地需求量；producedUnitsByBase 为
 * 生产任务实际成功完成的已生产量。未执行生产时后者为空映射，
 * 界面不得把预计量呈现为已产量。
 *
 * @param totalOrders          有效订单总数
 * @param processedOrderCount  已成功生产订单数
 * @param failedOrderCount     生产失败订单数
 * @param orderCountByRegion   地区 → 订单数
 * @param discountByCurrency   币种 → 优惠总额（按币种分列，严禁跨币种相加，D-004）
 * @param plannedUnitsByBase   基地 → 预计需求架数
 * @param producedUnitsByBase  基地 → 实际已生产架数
 * @param quantityByModel      型号 → 需求架数（按数量降序）
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public record SystemSummary(
		int totalOrders,
		int processedOrderCount,
		int failedOrderCount,
		Map<Region, Long> orderCountByRegion,
		Map<Currency, BigDecimal> discountByCurrency,
		Map<ProductionBase, Integer> plannedUnitsByBase,
		Map<ProductionBase, Integer> producedUnitsByBase,
		Map<String, Integer> quantityByModel) {

	/** @return 预计需求总架数。 */
	public int plannedUnitsTotal() {
		return plannedUnitsByBase.values().stream().mapToInt(Integer::intValue).sum();
	}

	/** @return 实际已生产总架数。 */
	public int producedUnitsTotal() {
		return producedUnitsByBase.values().stream().mapToInt(Integer::intValue).sum();
	}
}

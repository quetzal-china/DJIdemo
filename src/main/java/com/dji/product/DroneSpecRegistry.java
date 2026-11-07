package com.dji.product;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.dji.sale.ProductCategory;

/**
 * 大疆无人机权威产品参数注册表。
 *
 * 严格复刻教师 DJIProductDemo 中 9 个型号的精确构造参数（含
 * "Mini 4 PRO"、"Air 3S" 原始拼写与 "禅思  X5S" 双空格），
 * 作为产品创建的唯一参数权威，避免参数在多个工厂间复制走样。
 *
 * 序列号封装约定（架构修正 1，见 docs/DECISIONS.md）：
 * 对业务侧只暴露 #createDrone(String)，序列号由本注册表内部向
 * SerialNumberGenerator 统一申请，外部调用者无法在业务路径上指定序列号。
 * 教师 DJIProduct 系列构造器仍公开接受 sn 参数（保持教师公开 API
 * 兼容，无法在不破坏既有代码的前提下收窄其可见性），该封装限制在设计说明中披露；
 * 本注册表与各基地工厂是唯一合法的产品创建入口。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public final class DroneSpecRegistry {

	/** 型号 → 类别（精确拼写，与真实 order.json / DJIFactory 常量一致）。 */
	private static final Map<String, ProductCategory> CATEGORIES;

	/** 型号 → 产品创建器（sn 由注册表内部申请后传入）。 */
	private static final Map<String, Function<String, DJIProduct>> CREATORS;

	static {
		Map<String, ProductCategory> categories = new LinkedHashMap<>();
		Map<String, Function<String, DJIProduct>> creators = new LinkedHashMap<>();

		// ---- 消费级（参数复刻自 DJIProductDemo，零改动） ----
		categories.put("Mini 4 PRO", ProductCategory.CONSUMER);
		creators.put("Mini 4 PRO", sn -> new ConsumerDrone("Mini 4 PRO", sn, 249.0, "34分钟", "8K", true, "O4"));
		categories.put("Neo", ProductCategory.CONSUMER);
		creators.put("Neo", sn -> new ConsumerDrone("Neo", sn, 135.0, "18分钟", "4K", false, "O4"));
		categories.put("Air 3S", ProductCategory.CONSUMER);
		creators.put("Air 3S", sn -> new ConsumerDrone("Air 3S", sn, 724.0, "45分钟", "8K", true, "O4"));
		categories.put("Avata 2", ProductCategory.CONSUMER);
		creators.put("Avata 2", sn -> new ConsumerDrone("Avata 2", sn, 377.0, "23分钟", "4K", false, "O4"));

		// ---- 行业级 ----
		categories.put("Inspire 3", ProductCategory.ENTERPRISE);
		creators.put("Inspire 3", sn -> new EnterpriseDrone("Inspire 3", sn, 3995.0, "28分钟", "禅思  X5S", false, "影视", "IP45"));
		categories.put("Matrice 400", ProductCategory.ENTERPRISE);
		creators.put("Matrice 400", sn -> new EnterpriseDrone("Matrice 400", sn, 15800.0, "59分钟", "禅思H30", true, "应急抢险、电力巡检、测绘", "IP55"));
		categories.put("Mavic 4 PRO", ProductCategory.ENTERPRISE);
		creators.put("Mavic 4 PRO", sn -> new EnterpriseDrone("Mavic 4 PRO", sn, 1063.0, "51分钟", "4/3 CMOS 哈苏相机", false, "影视", "IP45"));

		// ---- 农业及载重级 ----
		categories.put("T70", ProductCategory.AGRICULTURAL);
		creators.put("T70", sn -> new AgriculturalDrone("T70", sn, 52000.0, "3.8小时", 50.0, 30.0, true, "有源相控阵雷达"));
		categories.put("T100", ProductCategory.AGRICULTURAL);
		creators.put("T100", sn -> new AgriculturalDrone("T100", sn, 73000.0, "3.8小时", 75.0, 40.0, true, "有源相控阵雷达"));

		CATEGORIES = Collections.unmodifiableMap(categories);
		CREATORS = Collections.unmodifiableMap(creators);
	}

	private DroneSpecRegistry() {
	}

	/**
	 * 按型号创建产品实例，序列号由注册表内部统一申请。
	 * 业务调用方不得、也无法通过本方法指定序列号。
	 *
	 * @param productType 精确产品型号，如 "Mini 4 PRO"
	 * @return 已分配全球唯一序列号的产品实例
	 * @throws IllegalArgumentException 型号为 null 或未注册（如 "Mini 4"、"Air 3" 等近似名）
	 * @throws SerialNumberExhaustedException 序列号已耗尽
	 */
	public static DJIProduct createDrone(String productType) {
		Function<String, DJIProduct> creator = CREATORS.get(productType);
		if (creator == null) {
			throw new IllegalArgumentException("未知或无效的产品型号: " + productType
					+ "（型号必须与 order.json / DJIFactory 注册拼写完全一致）");
		}
		String sn = SerialNumberGenerator.getInstance().nextSerialNumber();
		return creator.apply(sn);
	}

	/**
	 * 判断型号是否为注册表内的有效型号（精确匹配，大小写与空格敏感）。
	 *
	 * @param productType 待校验型号
	 * @return 有效返回 true
	 */
	public static boolean isValidModel(String productType) {
		return productType != null && CREATORS.containsKey(productType);
	}

	/**
	 * 查询型号所属产品类别。
	 *
	 * @param productType 精确产品型号
	 * @return 产品类别枚举
	 * @throws IllegalArgumentException 型号未注册
	 */
	public static ProductCategory getCategory(String productType) {
		ProductCategory category = CATEGORIES.get(productType);
		if (category == null) {
			throw new IllegalArgumentException("未知或无效的产品型号: " + productType);
		}
		return category;
	}

	/**
	 * @return 全部受支持型号（只读视图，保持注册顺序）
	 */
	public static Set<String> getSupportedModels() {
		return CREATORS.keySet();
	}
}

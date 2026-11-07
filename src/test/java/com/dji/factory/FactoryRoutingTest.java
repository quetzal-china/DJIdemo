package com.dji.factory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dji.product.SerialNumberTestSupport;
import com.dji.sale.ProductCategory;
import com.dji.sale.ProductionBase;
import com.dji.sale.ProductionRoutingException;
import com.dji.sale.Region;

/**
 * 任务 4｜抽象工厂与生产路由测试：
 * 覆盖任务书职责表全部合法组合、工厂越权拒绝、无效组合显式报错、
 * orderDrone 生产流程与序列号唯一性。
 */
class FactoryRoutingTest {

	private ProductionRouter router;

	@BeforeEach
	void setUp() {
		router = new ProductionRouter();
		SerialNumberTestSupport.reset();
	}

	@Test
	@DisplayName("路由表：15 种地区×类别组合与任务书职责表逐格一致")
	void routingTableMatchesSpecification() {
		Map<String, ProductionBase> expected = new HashMap<>();
		// 中国大陆
		expected.put(key(Region.CHINA_MAINLAND, ProductCategory.CONSUMER), ProductionBase.SHENZHEN);
		expected.put(key(Region.CHINA_MAINLAND, ProductCategory.ENTERPRISE), ProductionBase.DONGGUAN);
		expected.put(key(Region.CHINA_MAINLAND, ProductCategory.AGRICULTURAL), ProductionBase.SHUNDE);
		// 亚太（ASIA）
		expected.put(key(Region.ASIA, ProductCategory.CONSUMER), ProductionBase.NAGOYA);
		expected.put(key(Region.ASIA, ProductCategory.ENTERPRISE), ProductionBase.NAGOYA);
		expected.put(key(Region.ASIA, ProductCategory.AGRICULTURAL), ProductionBase.SHUNDE);
		// 欧洲
		expected.put(key(Region.EUROPE, ProductCategory.CONSUMER), ProductionBase.AMSTERDAM);
		expected.put(key(Region.EUROPE, ProductCategory.ENTERPRISE), ProductionBase.STUTTGART);
		expected.put(key(Region.EUROPE, ProductCategory.AGRICULTURAL), ProductionBase.SHUNDE);
		// 北美 / 南美
		for (Region region : new Region[] {Region.NORTH_AMERICA, Region.SOUTH_AMERICA}) {
			expected.put(key(region, ProductCategory.CONSUMER), ProductionBase.FREMONT);
			expected.put(key(region, ProductCategory.ENTERPRISE), ProductionBase.FREMONT);
			expected.put(key(region, ProductCategory.AGRICULTURAL), ProductionBase.SHUNDE);
		}
		assertEquals(15, expected.size());
		expected.forEach((k, base) -> {
			String[] parts = k.split("\\|", -1);
			Region region = Region.valueOf(parts[0]);
			ProductCategory category = ProductCategory.valueOf(parts[1]);
			assertEquals(base, router.resolveBase(region, category), "路由不匹配: " + k);
		});
	}

	private String key(Region region, ProductCategory category) {
		return region + "|" + category;
	}

	@Test
	@DisplayName("农业级全球归顺德：路由优先级高于地区专属工厂")
	void agriculturalAlwaysShunde() {
		assertAll(
				() -> assertEquals(ProductionBase.SHUNDE,
						router.resolveBase(Region.NORTH_AMERICA, ProductCategory.AGRICULTURAL)),
				() -> assertEquals(ProductionBase.SHUNDE,
						router.resolveBase(Region.EUROPE, ProductCategory.AGRICULTURAL)));
	}

	@Test
	@DisplayName("各基地工厂越权请求被显式拒绝（不返回 null、不静默）")
	void factoriesRejectOutOfScopeModels() {
		assertAll(
				// 深圳只做消费级：行业级、农业级、未知型号全部拒绝
				() -> assertThrows(ProductionRoutingException.class,
						() -> ShenzhenFactory.INSTANCE.buildDrone("Inspire 3")),
				() -> assertThrows(ProductionRoutingException.class,
						() -> ShenzhenFactory.INSTANCE.buildDrone("T100")),
				() -> assertThrows(ProductionRoutingException.class,
						() -> ShenzhenFactory.INSTANCE.buildDrone("Mini 4")), // 近似名无效
				// 东莞只做行业级
				() -> assertThrows(ProductionRoutingException.class,
						() -> DongguanFactory.INSTANCE.buildDrone("Mini 4 PRO")),
				// 顺德只做农业级
				() -> assertThrows(ProductionRoutingException.class,
						() -> ShundeFactory.INSTANCE.buildDrone("Matrice 400")),
				// 名古屋不做农业级
				() -> assertThrows(ProductionRoutingException.class,
						() -> NagoyaFactory.INSTANCE.buildDrone("T70")),
				// 阿姆斯特丹只做消费级
				() -> assertThrows(ProductionRoutingException.class,
						() -> AmsterdamFactory.INSTANCE.buildDrone("Inspire 3")),
				// 斯图加特只做行业级
				() -> assertThrows(ProductionRoutingException.class,
						() -> StuttgartFactory.INSTANCE.buildDrone("Neo")),
				// 弗里蒙特不做农业级
				() -> assertThrows(ProductionRoutingException.class,
						() -> FremontFactory.INSTANCE.buildDrone("T100")));
	}

	@Test
	@DisplayName("无效路由组合显式报错：空参数")
	void invalidRoutingCombinations() {
		assertAll(
				() -> assertThrows(ProductionRoutingException.class,
						() -> router.resolveBase(null, ProductCategory.CONSUMER)),
				() -> assertThrows(ProductionRoutingException.class,
						() -> router.resolveBase(Region.ASIA, null)),
				() -> assertThrows(ProductionRoutingException.class,
						() -> router.factoryFor(null)));
	}

	@Test
	@DisplayName("resolveFactory 与 resolveBase 一致且返回职责匹配的工厂")
	void resolveFactoryMatchesBase() {
		assertAll(
				() -> assertEquals(ShenzhenFactory.INSTANCE,
						router.resolveFactory(Region.CHINA_MAINLAND, ProductCategory.CONSUMER)),
				() -> assertEquals(DongguanFactory.INSTANCE,
						router.resolveFactory(Region.CHINA_MAINLAND, ProductCategory.ENTERPRISE)),
				() -> assertEquals(ShundeFactory.INSTANCE,
						router.resolveFactory(Region.EUROPE, ProductCategory.AGRICULTURAL)),
				() -> assertEquals(NagoyaFactory.INSTANCE,
						router.resolveFactory(Region.ASIA, ProductCategory.CONSUMER)),
				() -> assertEquals(AmsterdamFactory.INSTANCE,
						router.resolveFactory(Region.EUROPE, ProductCategory.CONSUMER)),
				() -> assertEquals(StuttgartFactory.INSTANCE,
						router.resolveFactory(Region.EUROPE, ProductCategory.ENTERPRISE)),
				() -> assertEquals(FremontFactory.INSTANCE,
						router.resolveFactory(Region.SOUTH_AMERICA, ProductCategory.ENTERPRISE)));
	}

	@Test
	@DisplayName("工厂生产职责内产品成功，orderDrone 完整流程返回已创建产品")
	void factoriesProduceInScopeProducts() {
		assertAll(
				() -> assertEquals("Mini 4 PRO",
						ShenzhenFactory.INSTANCE.buildDrone("Mini 4 PRO").getProductName()),
				() -> assertEquals("T70", ShundeFactory.INSTANCE.buildDrone("T70").getProductName()),
				() -> assertEquals("Mavic 4 PRO",
						FremontFactory.INSTANCE.buildDrone("Mavic 4 PRO").getProductName()));
	}

	@Test
	@DisplayName("orderDrone 生产流程产出的序列号全球唯一且格式正确")
	void orderDroneSerialsAreUnique() throws Exception {
		String sn1 = ShenzhenFactory.INSTANCE.orderDrone("Mini 4 PRO").getSeries();
		String sn2 = ShenzhenFactory.INSTANCE.orderDrone("Neo").getSeries();
		String sn3 = ShundeFactory.INSTANCE.orderDrone("T100").getSeries();
		assertAll(
				() -> assertTrue(sn1.matches("DJI-\\d{8}")),
				() -> assertTrue(sn2.matches("DJI-\\d{8}")),
				() -> assertTrue(sn3.matches("DJI-\\d{8}")),
				() -> assertEquals(3, java.util.Set.of(sn1, sn2, sn3).size()));
	}
}

package com.dji.product;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dji.sale.ProductCategory;

/**
 * 任务 3｜产品创建测试：9 个型号经 DroneSpecRegistry 实例化，
 * 类型与全部参数与 DJIProductDemo 权威定义 100% 一致；序列号由注册表内部申请。
 */
class ProductCreationTest {

	@BeforeEach
	void setUp() {
		SerialNumberGenerator.getInstance().resetForTest(0L);
	}

	@Test
	@DisplayName("9 个型号经注册表创建后类型、类别与关键参数正确")
	void allNineModelsCreateCorrectInstances() {
		assertAll(
				() -> checkConsumer(DroneSpecRegistry.createDrone("Mini 4 PRO"), "Mini 4 PRO", 249.0, "34分钟", "8K", true, "O4"),
				() -> checkConsumer(DroneSpecRegistry.createDrone("Neo"), "Neo", 135.0, "18分钟", "4K", false, "O4"),
				() -> checkConsumer(DroneSpecRegistry.createDrone("Air 3S"), "Air 3S", 724.0, "45分钟", "8K", true, "O4"),
				() -> checkConsumer(DroneSpecRegistry.createDrone("Avata 2"), "Avata 2", 377.0, "23分钟", "4K", false, "O4"),
				() -> checkEnterprise(DroneSpecRegistry.createDrone("Inspire 3"), "Inspire 3", 3995.0, "28分钟", "禅思  X5S", false, "影视", "IP45"),
				() -> checkEnterprise(DroneSpecRegistry.createDrone("Matrice 400"), "Matrice 400", 15800.0, "59分钟", "禅思H30", true, "应急抢险、电力巡检、测绘", "IP55"),
				() -> checkEnterprise(DroneSpecRegistry.createDrone("Mavic 4 PRO"), "Mavic 4 PRO", 1063.0, "51分钟", "4/3 CMOS 哈苏相机", false, "影视", "IP45"),
				() -> checkAgricultural(DroneSpecRegistry.createDrone("T70"), "T70", 52000.0, "3.8小时", 50.0, 30.0, true, "有源相控阵雷达"),
				() -> checkAgricultural(DroneSpecRegistry.createDrone("T100"), "T100", 73000.0, "3.8小时", 75.0, 40.0, true, "有源相控阵雷达"));
	}

	private void checkConsumer(DJIProduct product, String name, double weight, String flightTime,
			String camera, boolean avoidance, String transmission) {
		assertAll(
				() -> assertInstanceOf(ConsumerDrone.class, product),
				() -> assertEquals(name, product.getProductName()),
				() -> assertEquals(ProductCategory.CONSUMER.getDisplayName(), product.getProductCategory()),
				() -> assertEquals(weight, product.getWeight()),
				() -> assertEquals(flightTime, product.getMaxFlightTime()),
				() -> assertEquals(camera, ((ConsumerDrone) product).getCameraResolution()),
				() -> assertEquals(avoidance, ((ConsumerDrone) product).hasObstacleAvoidance()),
				() -> assertEquals(transmission, ((ConsumerDrone) product).getTransmissionSystem()),
				() -> assertValidSerial(product));
	}

	private void checkEnterprise(DJIProduct product, String name, double weight, String flightTime,
			String payload, boolean rtk, String application, String ipRating) {
		assertAll(
				() -> assertInstanceOf(EnterpriseDrone.class, product),
				() -> assertEquals(name, product.getProductName()),
				() -> assertEquals(ProductCategory.ENTERPRISE.getDisplayName(), product.getProductCategory()),
				() -> assertEquals(weight, product.getWeight()),
				() -> assertEquals(flightTime, product.getMaxFlightTime()),
				() -> assertEquals(payload, ((EnterpriseDrone) product).getPayloadType()),
				() -> assertEquals(rtk, ((EnterpriseDrone) product).hasRTKModule()),
				() -> assertEquals(application, ((EnterpriseDrone) product).getApplicationField()),
				() -> assertEquals(ipRating, ((EnterpriseDrone) product).getIpRating()),
				() -> assertValidSerial(product));
	}

	private void checkAgricultural(DJIProduct product, String name, double weight, String flightTime,
			double tank, double flowRate, boolean multispectral, String radar) {
		assertAll(
				() -> assertInstanceOf(AgriculturalDrone.class, product),
				() -> assertEquals(name, product.getProductName()),
				() -> assertEquals(ProductCategory.AGRICULTURAL.getDisplayName(), product.getProductCategory()),
				() -> assertEquals(weight, product.getWeight()),
				() -> assertEquals(flightTime, product.getMaxFlightTime()),
				() -> assertEquals(tank, ((AgriculturalDrone) product).getSprayTankCapacity()),
				() -> assertEquals(flowRate, ((AgriculturalDrone) product).getMaxSprayFlowRate()),
				() -> assertEquals(multispectral, ((AgriculturalDrone) product).hasMultispectralCamera()),
				() -> assertEquals(radar, ((AgriculturalDrone) product).getRadarSystem()),
				() -> assertValidSerial(product));
	}

	private void assertValidSerial(DJIProduct product) {
		assertTrue(product.getSeries().matches("DJI-\\d{8}"),
				"序列号必须为 DJI-8位数字: " + product.getSeries());
	}

	@Test
	@DisplayName("注册表自动申请的序列号递增且业务方无法指定")
	void serialsAreAssignedInternallyAndMonotonic() {
		DJIProduct first = DroneSpecRegistry.createDrone("Neo");
		DJIProduct second = DroneSpecRegistry.createDrone("Neo");
		assertEquals("DJI-00000001", first.getSeries());
		assertEquals("DJI-00000002", second.getSeries());
		assertNotEquals(first.getSeries(), second.getSeries());
	}

	@Test
	@DisplayName("近似名/未知型号（Mini 4、Air 3、大小写不符）一律拒绝")
	void unknownModelsAreRejected() {
		assertAll(
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.createDrone("Mini 4")),
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.createDrone("Air 3")),
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.createDrone("air 3s")),
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.createDrone("Mini4Pro")),
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.createDrone("")),
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.createDrone(null)),
				() -> assertThrows(IllegalArgumentException.class, () -> DroneSpecRegistry.getCategory("Mini 4")));
	}

	@Test
	@DisplayName("isValidModel 仅接受注册表中 9 个精确拼写")
	void modelValidation() {
		assertAll(
				() -> assertTrue(DroneSpecRegistry.isValidModel("Mini 4 PRO")),
				() -> assertTrue(DroneSpecRegistry.isValidModel("T100")),
				() -> assertTrue(!DroneSpecRegistry.isValidModel("Mini 4")),
				() -> assertTrue(!DroneSpecRegistry.isValidModel(null)),
				() -> assertEquals(9, DroneSpecRegistry.getSupportedModels().size()));
	}
}

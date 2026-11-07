package com.dji.product;

/**
 * 大疆产品演示类 展示各类产品的使用
 * 
 * @author liyong
 * @version 1.0
 */
public class DJIProductDemo {

	public static void main(String[] args) {

		System.out.println("大疆产品体系演示");
		System.out.println("================\n");

		// 创建消费级无人机实例
		ConsumerDrone mini4Pro = new ConsumerDrone("Mini 4 PRO", SerialNumberGenerator.getInstance().nextSerialNumber(),
				249.0, "34分钟", "8K", true, "O4");
		ConsumerDrone neo = new ConsumerDrone("Neo", SerialNumberGenerator.getInstance().nextSerialNumber(), 135.0,
				"18分钟", "4K", false, "O4");
		ConsumerDrone air3s = new ConsumerDrone("Air 3S", SerialNumberGenerator.getInstance().nextSerialNumber(), 724.0,
				"45分钟", "8K", true, "O4");
		ConsumerDrone avata2 = new ConsumerDrone("Avata 2", SerialNumberGenerator.getInstance().nextSerialNumber(),
				377.0, "23分钟", "4K", false, "O4");

		// 创建行业级无人机实例
		EnterpriseDrone Inspire3 = new EnterpriseDrone("Inspire 3",
				SerialNumberGenerator.getInstance().nextSerialNumber(), 3995.0, "28分钟", "禅思  X5S", false, "影视", "IP45");
		EnterpriseDrone matrice400 = new EnterpriseDrone("Matrice 400",
				SerialNumberGenerator.getInstance().nextSerialNumber(), 15800.0, "59分钟", "禅思H30", true, "应急抢险、电力巡检、测绘",
				"IP55");
		EnterpriseDrone mavic4 = new EnterpriseDrone("Mavic 4 PRO",
				SerialNumberGenerator.getInstance().nextSerialNumber(), 1063.0, "51分钟", "4/3 CMOS 哈苏相机", false, "影视",
				"IP45");

		// 创建农业级无人机实例
		AgriculturalDrone t70 = new AgriculturalDrone("T70", SerialNumberGenerator.getInstance().nextSerialNumber(),
				52000.0, "3.8小时", 50.0, 30.0, true, "有源相控阵雷达");
		AgriculturalDrone t100 = new AgriculturalDrone("T100", SerialNumberGenerator.getInstance().nextSerialNumber(),
				73000.0, "3.8小时", 75.0, 40.0, true, "有源相控阵雷达");

		// 演示各类产品功能
		System.out.println("1. 消费级无人机演示:");
		testConsumerDrone(mini4Pro);
		testConsumerDrone(neo);
		testConsumerDrone(air3s);
		testConsumerDrone(avata2);

		System.out.println("\n2. 行业级无人机演示:");
		testEnterpriseDrone(Inspire3);
		testEnterpriseDrone(matrice400);
		testEnterpriseDrone(mavic4);

		System.out.println("\n3. 农业级无人机演示:");
		testAgriculturalDrone(t70);
		testAgriculturalDrone(t100);

		// 多态演示
		System.out.println("\n4. 多态特性演示:");

		testDJIProduct(mini4Pro);
		testDJIProduct(neo);
		testDJIProduct(air3s);
		testDJIProduct(avata2);
		testDJIProduct(Inspire3);
		testDJIProduct(matrice400);
		testDJIProduct(mavic4);
		testDJIProduct(t70);
		testDJIProduct(t100);

	}

	/**
	 * 消费无人机测试
	 * 
	 * @param consumerDrone
	 */
	public static void testConsumerDrone(ConsumerDrone consumerDrone) {
		consumerDrone.displayProductInfo();
		consumerDrone.takeOff();
		consumerDrone.startFollowMe();
		consumerDrone.takePanorama();
		consumerDrone.land();
	}

	/**
	 * 行业无人机测试
	 * 
	 * @param enterpriseDrone
	 */
	public static void testEnterpriseDrone(EnterpriseDrone enterpriseDrone) {
		enterpriseDrone.displayProductInfo();
		enterpriseDrone.takeOff();
		enterpriseDrone.startMapping();
		enterpriseDrone.returnHome();
	}

	/**
	 * 农业无人机测试
	 * 
	 * @param agriculturalDrone
	 */
	public static void testAgriculturalDrone(AgriculturalDrone agriculturalDrone) {
		agriculturalDrone.displayProductInfo();
		agriculturalDrone.takeOff();
		agriculturalDrone.startSpraying();
		agriculturalDrone.generatePrescriptionMap();
		agriculturalDrone.returnHome();
	}

	/**
	 * 多态演示
	 * 
	 * @param product
	 */
	public static void testDJIProduct(DJIProduct product) {
		System.out.println("产品类别: " + product.getProductCategory());
		System.out.println("产品名称: " + product.getProductName());

	}
}

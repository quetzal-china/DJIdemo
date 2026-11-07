package com.dji.factory;

import java.util.Arrays;

import com.dji.product.DJIProduct;

/**
 * 大疆抽象工厂
 * 
 * @author liyong
 * @version 1.0
 *
 */
public abstract class DJIFactory {

	/**
	 * 大疆产品系列<br/>
	 * 消费领域产品：Mini 4、Neo、Air 3、Avata 2
	 */
	public final static String[] CONSUMER_PRODUCT_TYPE = { "Mini 4 PRO", "Neo", "Air 3S", "Avata 2" };
	/**
	 * 大疆产品系列<br/>
	 * 行业领域：Inspire 3、Matrice 400、Mavic 4 PRO
	 */
	public final static String[] INDUSTRY_PRODUCT_TYPE = { "Inspire 3", "Matrice 400", "Mavic 4 PRO" };
	/**
	 * 大疆产品系列<br/>
	 * 农业及载重领域：T100、T70
	 */
	public final static String[] AGRICULTURE_PRODUCT_TYPE = { "T100", "T70" };

	/**
	 * 根据产品型号生产无人机
	 * 
	 * @param type
	 * @return
	 */
	public abstract DJIProduct buildDrone(String type);

	/**
	 * 根据订单中的型号生产无人机，完成静态、动态测试并打包
	 * 
	 * @param type 无人机型号
	 * @return 无人机
	 * @throws Exception 当产品型号错误试抛出
	 */
	public DJIProduct orderDrone(String type) throws Exception {
		DJIProduct drone = buildDrone(type);
		if (drone == null)
			throw new Exception("无效的无人机型号！");
		drone.displayProductInfo();
		drone.staticTest();
		drone.flightTest();
		drone.packaging();
		return drone;
	}

	/**
	 * 是否消费级无人机
	 * 
	 * @param type 无人机型号
	 * @return 消费级返回true
	 */
	public static boolean isConsumer(String type) {
		return Arrays.stream(CONSUMER_PRODUCT_TYPE).anyMatch(type::equals);
	}

	/**
	 * 是否消农业 无人机
	 * 
	 * @param type 无人机型号
	 * @return 消费级返回true
	 */
	public static boolean isAgricultural(String type) {
		return Arrays.stream(AGRICULTURE_PRODUCT_TYPE).anyMatch(type::equals);
	}

	/**
	 * 是否行业无人机
	 * 
	 * @param type 无人机型号
	 * @return 消费级返回true
	 */
	public static boolean isEnterprise(String type) {
		return Arrays.stream(INDUSTRY_PRODUCT_TYPE).anyMatch(type::equals);
	}
}

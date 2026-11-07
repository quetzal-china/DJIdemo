package com.dji.product;

/**
 * 消费级无人机类 继承自DJIProduct基类
 * 
 * @author liyong
 * @version 1.0
 */
public class ConsumerDrone extends DJIProduct {

	/**
	 * 相机分辨率
	 */
	protected String cameraResolution;
	/**
	 * 是否具有避障系统
	 */
	protected boolean hasObstacleAvoidance;
	/**
	 * 图传系统型号
	 */
	protected String transmissionSystem;

	/**
	 * 构建消费级无人机
	 * 
	 * @param productName          产品名称
	 * @param sn                   产品系列号，全球唯一
	 * @param weight               飞行重量
	 * @param maxFlightTime        最大飞行时间
	 * @param cameraResolution     相机分辨率
	 * @param hasObstacleAvoidance 是否具有避障系统
	 * @param transmissionSystem   图传系统型号
	 */
	public ConsumerDrone(String productName, String sn, double weight, String maxFlightTime, String cameraResolution,
			boolean hasObstacleAvoidance, String transmissionSystem) {
		super(productName, sn, weight, maxFlightTime);
		this.cameraResolution = cameraResolution;
		this.hasObstacleAvoidance = hasObstacleAvoidance;
		this.transmissionSystem = transmissionSystem;
		if (hasObstacleAvoidance)
			InstallObstacleAvoidance();
	}

	@Override
	public void displayProductInfo() {
		System.out.println("=== " + productName + " 消费级无人机 ===");
		System.out.println("系列号: " + sn);
		System.out.println("重量: " + weight + "g");
		System.out.println("续航: " + maxFlightTime);
		System.out.println("相机分辨率: " + cameraResolution);
		System.out.println("避障系统: " + (hasObstacleAvoidance ? "支持" : "不支持"));
		System.out.println("图传系统: " + transmissionSystem);
	}

	@Override
	public String getProductCategory() {
		return "消费级无人机";
	}

	@Override
	public void flightTest() {
		takeOff();
		startFollowMe();
		takePanorama();
		land();

	}

	// 消费级特有功能

	/**
	 * 安装避障系统
	 */
	public void InstallObstacleAvoidance() {
		System.out.println("安装  " + productName + " sn:" + sn + " 避障系统");
	}

	/**
	 * 开始智能跟随
	 */
	public void startFollowMe() {
		System.out.println(productName + " 开始智能跟随！");
	}

	/**
	 * 拍摄全景照片
	 */
	public void takePanorama() {
		System.out.println(productName + " 拍摄全景照片！");
	}

	// Getter方法
	public String getCameraResolution() {
		return cameraResolution;
	}

	public boolean hasObstacleAvoidance() {
		return hasObstacleAvoidance;
	}

	public String getTransmissionSystem() {
		return transmissionSystem;
	}

}

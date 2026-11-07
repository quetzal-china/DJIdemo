package com.dji.product;

/**
 * 行业级无人机类 针对专业领域应用的无人机
 * 
 * @author liyong
 * @version 1.0
 */
public class EnterpriseDrone extends DJIProduct {

	/**
	 * 负载类型
	 */
	protected String payloadType;
	/**
	 * 是否具有RTK模块
	 */
	protected boolean hasRTKModule;
	/**
	 * 应用领域
	 */
	protected String applicationField;
	/**
	 * 防护等级
	 */
	protected String ipRating;

	/**
	 * 构建行业级无人机
	 * 
	 * @param productName      产品名称
	 * @param sn               产品系列号，全球唯一
	 * @param weight           飞行重量
	 * @param maxFlightTime    最大飞行时间
	 * @param payloadType      负载类型
	 * @param hasRTKModule     是否具有RTK模块
	 * @param applicationField 应用领域
	 * @param ipRating         防护等级
	 */
	public EnterpriseDrone(String productName, String sn, double weight, String maxFlightTime, String payloadType,
			boolean hasRTKModule, String applicationField, String ipRating) {
		super(productName, sn, weight, maxFlightTime);
		this.payloadType = payloadType;
		this.hasRTKModule = hasRTKModule;
		this.applicationField = applicationField;
		this.ipRating = ipRating;
		if (hasRTKModule)
			InstallRTKModule();
	}

	@Override
	public void displayProductInfo() {
		System.out.println("=== " + productName + " 行业级无人机 ===");
		System.out.println("系列号: " + sn);
		System.out.println("重量: " + weight + "g");
		System.out.println("续航: " + maxFlightTime);
		System.out.println("负载类型: " + payloadType);
		System.out.println("RTK模块: " + (hasRTKModule ? "集成" : "无"));
		System.out.println("应用领域: " + applicationField);
		System.out.println("防护等级: " + ipRating);
	}

	@Override
	public String getProductCategory() {
		return "行业级无人机";
	}

	@Override
	public void flightTest() {
		takeOff();
		startMapping();
		returnHome();

	}

	// 行业级特有功能

	/**
	 * 安装RTK模块
	 */
	public void InstallRTKModule() {
		System.out.println("安装  " + productName + " sn:" + sn + " RTK模块");
	}

	/**
	 * 测绘
	 */
	public void startMapping() {
		System.out.println(productName + " 开始测绘作业！");
	}

	/**
	 * 热成像
	 */
	public void thermalInspection() {
		System.out.println(productName + " 进行热成像检测！");
	}

	// Getter方法
	public String getPayloadType() {
		return payloadType;
	}

	public boolean hasRTKModule() {
		return hasRTKModule;
	}

	public String getApplicationField() {
		return applicationField;
	}

	public String getIpRating() {
		return ipRating;
	}

}

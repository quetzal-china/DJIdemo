package com.dji.product;

/**
 * 农业及载重无人机类 专门用于农业植保、和农田管理和载重的无人机
 * 
 * @author liyong
 * @version 1.0
 */
public class AgriculturalDrone extends DJIProduct {

	/**
	 * 药箱容量 单位L
	 */
	protected double sprayTankCapacity;
	/**
	 * 最大喷洒流量 单位L/min
	 */
	protected double maxSprayFlowRate;
	/**
	 * 是否具有多光谱相机
	 */
	protected boolean hasMultispectralCamera;
	/**
	 * 雷达系统
	 */
	protected String radarSystem;

	/**
	 * 构建农业无人机
	 * 
	 * @param productName            产品名称
	 * @param sn                     产品系列号，全球唯一
	 * @param weight                 飞行重量
	 * @param maxFlightTime          最大飞行时间
	 * @param sprayTankCapacity      药箱容量 单位L
	 * @param maxSprayFlowRate       最大喷洒流量 单位L/min
	 * @param hasMultispectralCamera 是否具有多光谱相机
	 * @param radarSystem            是否具有多光谱相机
	 */
	public AgriculturalDrone(String productName, String sn, double weight, String maxFlightTime,
			double sprayTankCapacity, double maxSprayFlowRate, boolean hasMultispectralCamera, String radarSystem) {
		super(productName, sn, weight, maxFlightTime);
		this.sprayTankCapacity = sprayTankCapacity;
		this.maxSprayFlowRate = maxSprayFlowRate;
		this.hasMultispectralCamera = hasMultispectralCamera;
		this.radarSystem = radarSystem;
		InstallSprayTank();
		if (hasMultispectralCamera)
			InstallMultispectralCamera();

	}

	@Override
	public void displayProductInfo() {
		System.out.println("=== " + productName + " 农业级无人机 ===");
		System.out.println("系列号: " + sn);
		System.out.println("重量: " + weight + "g");
		System.out.println("续航: " + maxFlightTime);
		System.out.println("药箱容量: " + sprayTankCapacity + "L");
		System.out.println("最大喷洒流量: " + maxSprayFlowRate + "L/min");
		System.out.println("多光谱相机: " + (hasMultispectralCamera ? "标配" : "选配"));
		System.out.println("雷达系统: " + radarSystem);
	}

	@Override
	public String getProductCategory() {
		return "农业级无人机";
	}

	@Override
	public void flightTest() {
		takeOff();
		startSpraying();
		generatePrescriptionMap();

	}

	// 农业级特有功能

	/**
	 * 安装水箱喷泵
	 */
	public void InstallSprayTank() {
		System.out.println("安装  " + productName + " sn:" + sn + " 水箱与喷泵");
	}

	/**
	 * 安装多光谱相机
	 */
	public void InstallMultispectralCamera() {
		System.out.println("安装  " + productName + " sn:" + sn + " 多光谱相机");
	}

	/**
	 * 开始自动喷洒作业
	 */
	public void startSpraying() {
		System.out.println(productName + " 开始自动喷洒作业！");
	}

	/**
	 * 生成农业处方图
	 */
	public void generatePrescriptionMap() {
		System.out.println(productName + " 生成农业处方图！");
	}

	/**
	 * 进行作物生长监测
	 */
	public void cropMonitoring() {
		System.out.println(productName + " 进行作物生长监测！");
	}

	// Getter方法
	public double getSprayTankCapacity() {
		return sprayTankCapacity;
	}

	public double getMaxSprayFlowRate() {
		return maxSprayFlowRate;
	}

	public boolean hasMultispectralCamera() {
		return hasMultispectralCamera;
	}

	public String getRadarSystem() {
		return radarSystem;
	}

}

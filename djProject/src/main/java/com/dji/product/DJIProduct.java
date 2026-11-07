package com.dji.product;

/**
 * 大疆产品基类 定义所有大疆产品的共同属性和方法
 * 
 * @author liyong
 * @version 1.0
 */
public abstract class DJIProduct {

	/**
	 * 产品名称
	 */
	protected String productName;
	/**
	 * 产品系列号，全球唯一
	 */
	protected String sn;
	/**
	 * 重量，单位g
	 */
	protected double weight;
	/**
	 * 最大飞行时间
	 */
	protected String maxFlightTime;

	/**
	 * 构建大疆无人机
	 * 
	 * @param productName   产品名称
	 * @param sn            产品系列号，全球唯一
	 * @param weight        重量
	 * @param maxFlightTime 最大飞行时间
	 */
	public DJIProduct(String productName, String sn, double weight, String maxFlightTime) {
		this.productName = productName;
		this.sn = sn;
		this.weight = weight;
		this.maxFlightTime = maxFlightTime;
		InstallBody();
		InstallMotor();
		InstallFlightControl();
		InstallAttachment();

	}

	/**
	 * 显示产品信息
	 */
	public abstract void displayProductInfo();

	/**
	 * 取得产品分类信息
	 * 
	 * @return
	 */
	public abstract String getProductCategory();

	/**
	 * 动态飞行测试
	 */
	public abstract void flightTest();

	/**
	 * 起飞
	 */
	public void takeOff() {
		System.out.println(productName + " sn:" + sn + " 起飞！");
	}

	/**
	 * 降落
	 */
	public void land() {
		System.out.println(productName + " sn:" + sn + " 降落！");
	}

	/**
	 * 自动返航
	 */
	public void returnHome() {
		System.out.println(productName + " sn:" + sn + " 自动返航！");
	}

	/**
	 * 安装机体
	 */
	public void InstallBody() {
		System.out.println("组装  " + productName + " sn:" + sn + " 机体");
	}

	/**
	 * 安装电机
	 */
	public void InstallMotor() {
		System.out.println("安装  " + productName + " sn:" + sn + " 电机");
	}

	/**
	 * 安装飞控
	 */
	public void InstallFlightControl() {
		System.out.println("安装  " + productName + " sn:" + sn + " 飞控");
	}

	/**
	 * 安装云台
	 */
	public void InstallPanTilt() {
		System.out.println("安装  " + productName + " sn:" + sn + " 云台");
	}

	/**
	 * 其他附件
	 */
	public void InstallAttachment() {
		System.out.println("安装  " + productName + " sn:" + sn + " 其它附件");
	}

	/**
	 * 无人机静态测试
	 */
	public void staticTest() {
		System.out.println("对  " + productName + " sn:" + sn + " 强度测试");
		System.out.println("对  " + productName + " sn:" + sn + " 电源测试");
		System.out.println("对  " + productName + " sn:" + sn + " 平衡测试");
	}

	/**
	 * 成品包装
	 */
	public void packaging() {
		System.out.println("打包  " + productName + " sn:" + sn);
		System.out.println("==========================================");
	}

	// Getter方法
	public String getProductName() {
		return productName;
	}

	public String getSeries() {
		return sn;
	}

	public double getWeight() {
		return weight;
	}

	public String getMaxFlightTime() {
		return maxFlightTime;
	}

}

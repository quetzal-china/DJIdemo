package com.dji.product;

/**
 * 测试专用支撑类（仅存在于测试源码集）。
 * 允许其他包的测试（如 com.dji.service 的生产编排测试）在受控前提下
 * 重置序列号计数器，以断言 "DJI-00000001 起步" 的精确编号序列。
 * 主代码无任何路径可以访问包级私有的 resetForTest。</p>
 */
public final class SerialNumberTestSupport {

	private SerialNumberTestSupport() {
	}

	/** 重置序列号计数器（下一号为 DJI-00000001）。仅测试源码可用。 */
	public static void reset() {
		SerialNumberGenerator.getInstance().resetForTest(0L);
	}
}

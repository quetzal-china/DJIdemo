package com.dji.product;

/**
 * 序列号耗尽异常。
 * 当序列号生成器越过 DJI-99999999 上限时抛出；
 * 生成器保证此时计数器不回绕、不重复，后续调用将持续抛出本异常。
 *
 * @author liuyuxin
 * @version 1.0
 */
public class SerialNumberExhaustedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SerialNumberExhaustedException(String message) {
		super(message);
	}
}

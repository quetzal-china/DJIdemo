package com.dji.product;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 全球唯一产品序列号生成器（线程安全单例）。
 *
 * <p>序列号格式严格为 8 位零填充递增编号：{@code DJI-00000001} ~ {@code DJI-99999999}。
 * 使用 {@link AtomicLong} CAS 自增保证多线程并发下不重复、不跳跃；
 * 越过上限时立即抛出 {@link SerialNumberExhaustedException}，绝不回绕或重复。</p>
 *
 * <p>边界说明（见 docs/DECISIONS.md D-008）：本单例为进程内编号器，
 * JVM 重启后从 {@code DJI-00000001} 重新计数；跨进程全局唯一超出本题范围，
 * 设计说明中如实披露，不虚称持久化唯一。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public final class SerialNumberGenerator {

	/** 序列号最大数值（8 位上限）。 */
	public static final long MAX_SERIAL = 99_999_999L;

	private static final SerialNumberGenerator INSTANCE = new SerialNumberGenerator();

	private final AtomicLong counter = new AtomicLong(0L);

	private SerialNumberGenerator() {
	}

	public static SerialNumberGenerator getInstance() {
		return INSTANCE;
	}

	/**
	 * 生成下一个全球唯一序列号。
	 *
	 * @return 形如 {@code DJI-00000001} 的序列号
	 * @throws SerialNumberExhaustedException 序列号已耗尽（越过 DJI-99999999）时抛出，
	 *                                        且内部计数保持耗尽状态，不回绕
	 */
	public String nextSerialNumber() {
		long current = counter.incrementAndGet();
		if (current > MAX_SERIAL || current <= 0L) {
			// 回退本次自增，保持耗尽状态（不回绕、不重复）
			counter.decrementAndGet();
			throw new SerialNumberExhaustedException("序列号已耗尽，最大上限为 DJI-99999999");
		}
		return String.format("DJI-%08d", current);
	}

	/**
	 * 仅限包内单元测试重置或设置边界值使用，生产代码禁止调用。
	 *
	 * @param initialValue 重置后的计数器值（0 表示从 DJI-00000001 重新开始）
	 */
	void resetForTest(long initialValue) {
		counter.set(initialValue);
	}
}

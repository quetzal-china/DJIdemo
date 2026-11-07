package com.dji.product;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 任务 3｜序列号单例测试：格式递增、并发不重复、上限边界与耗尽不回绕。
 */
class SerialNumberGeneratorTest {

	private SerialNumberGenerator generator;

	@BeforeEach
	void setUp() {
		generator = SerialNumberGenerator.getInstance();
		generator.resetForTest(0L);
	}

	@Test
	@DisplayName("序列号格式严格为 DJI-8位递增零填充")
	void formatAndIncrement() {
		assertEquals("DJI-00000001", generator.nextSerialNumber());
		assertEquals("DJI-00000002", generator.nextSerialNumber());
		assertEquals("DJI-00000003", generator.nextSerialNumber());
	}

	@Test
	@DisplayName("并发 1000 次调用无重复且恰好覆盖 DJI-00000001..1000")
	void concurrentCallsAreUniqueAndContiguous() throws Exception {
		int threads = 32;
		int total = 1000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<String>> futures = new ArrayList<>();
			for (int i = 0; i < total; i++) {
				futures.add(pool.submit(() -> {
					start.await();
					return generator.nextSerialNumber();
				}));
			}
			start.countDown(); // 先放行，再统一收集结果
			Set<String> serials = new HashSet<>();
			for (Future<String> future : futures) {
				serials.add(future.get(10, TimeUnit.SECONDS));
			}
			assertEquals(total, serials.size(), "并发调用不得产生重复序列号");
			for (int i = 1; i <= total; i++) {
				assertTrue(serials.contains(String.format("DJI-%08d", i)),
						"并发结果应恰好连续覆盖 1..1000，缺失: DJI-%08d".formatted(i));
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	@DisplayName("越过 DJI-99999999 立即耗尽并抛异常，不回绕不重复")
	void exhaustionAtBoundaryWithoutWraparound() {
		generator.resetForTest(SerialNumberGenerator.MAX_SERIAL - 1);
		assertEquals("DJI-99999999", generator.nextSerialNumber());
		SerialNumberExhaustedException first = assertThrows(SerialNumberExhaustedException.class,
				generator::nextSerialNumber);
		assertAll(
				() -> assertTrue(first.getMessage().contains("耗尽")),
				// 耗尽后持续抛异常（计数器保持耗尽状态，不回绕）
				() -> assertThrows(SerialNumberExhaustedException.class, generator::nextSerialNumber),
				() -> assertThrows(SerialNumberExhaustedException.class, generator::nextSerialNumber));
	}

	@Test
	@DisplayName("reset 至上限后首次调用即耗尽")
	void exhaustedImmediatelyAtMax() {
		generator.resetForTest(SerialNumberGenerator.MAX_SERIAL);
		assertThrows(SerialNumberExhaustedException.class, generator::nextSerialNumber);
	}

	@Test
	@DisplayName("并发触发耗尽时成功编号不重复、失败调用不吞异常")
	void concurrentExhaustionKeepsUniqueness() throws Exception {
		generator.resetForTest(SerialNumberGenerator.MAX_SERIAL - 49);
		int threads = 64;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<String>> futures = new ArrayList<>();
			for (int i = 0; i < threads * 4; i++) {
				futures.add(pool.submit(() -> {
					start.await();
					try {
						return generator.nextSerialNumber();
					} catch (SerialNumberExhaustedException e) {
						return null; // 耗尽属于预期结果
					}
				}));
			}
			start.countDown(); // 先放行，再统一收集结果
			Set<String> successes = new HashSet<>();
			int failures = 0;
			for (Future<String> future : futures) {
				String serial = future.get(10, TimeUnit.SECONDS);
				if (serial == null) {
					failures++;
				} else {
					successes.add(serial);
				}
			}
			assertEquals(49, successes.size(), "仅 49 个编号可成功分配");
			assertEquals(threads * 4 - 49, failures, "其余调用必须显式收到耗尽异常");
		} finally {
			pool.shutdownNow();
		}
	}
}

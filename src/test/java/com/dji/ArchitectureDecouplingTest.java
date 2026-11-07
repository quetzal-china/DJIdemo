package com.dji;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 架构解耦守护测试：核心六包（sale/product/factory/promotion/repository/service）
 * 严禁依赖 Swing/AWT，保证全部业务服务可在无头 JUnit 环境独立测试（任务 8）。
 */
class ArchitectureDecouplingTest {

	private static final List<String> CORE_PACKAGES = List.of(
			"sale", "product", "factory", "promotion", "repository", "service");

	@Test
	@DisplayName("核心六包源码零 Swing/AWT 依赖（UI 与业务解耦的编译级守护）")
	void corePackagesHaveNoSwingDependencies() throws IOException {
		Path mainJava = Path.of("src/main/java/com/dji");
		assertTrue(Files.isDirectory(mainJava), "未找到源码目录: " + mainJava.toAbsolutePath());
		for (String pkg : CORE_PACKAGES) {
			Path packageDir = mainJava.resolve(pkg);
			assertTrue(Files.isDirectory(packageDir), "缺少核心包: " + pkg);
			try (Stream<Path> sources = Files.walk(packageDir)) {
				sources.filter(p -> p.toString().endsWith(".java")).forEach(source -> {
					try {
						String text = Files.readString(source);
						assertTrue(!text.contains("import javax.swing"),
								"核心包禁止 Swing 依赖: " + source);
						assertTrue(!text.contains("import java.awt"),
								"核心包禁止 AWT 依赖: " + source);
						assertTrue(!text.contains("javax.swing."),
								"核心包禁止 Swing 引用: " + source);
					} catch (IOException e) {
						throw new IllegalStateException("无法读取源文件: " + source, e);
					}
				});
			}
		}
	}
}

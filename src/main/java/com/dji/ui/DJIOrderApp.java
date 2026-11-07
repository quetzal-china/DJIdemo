package com.dji.ui;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 系统主入口（任务 6）。
 *
 * <p>启动方式：</p>
 * <pre>
 *   mvn -q compile
 *   java -cp target/classes com.dji.ui.DJIOrderApp                    # 默认尝试 src/main/resources/order.json
 *   java -cp target/classes com.dji.ui.DJIOrderApp D:\path\order.json # 命令行参数指定订单文件
 * </pre>
 *
 * <p>窗口在 EDT 创建与显示；若默认/指定文件存在则立即触发 SwingWorker
 * 异步加载，文件缺失时保持空界面等待用户通过"打开订单文件"选择。</p>
 *
 * @author liuyuxin (ZCode)
 * @version 1.0
 */
public class DJIOrderApp {

	private DJIOrderApp() {
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			MainFrame frame = new MainFrame();
			frame.setVisible(true);
			Path defaultFile = resolveDefaultFile(args);
			if (defaultFile != null) {
				frame.loadOrdersFromFile(defaultFile);
			}
		});
	}

	private static Path resolveDefaultFile(String[] args) {
		if (args.length > 0) {
			return Path.of(args[0]);
		}
		Path bundled = Path.of("src/main/resources/order.json");
		return Files.exists(bundled) ? bundled : null;
	}
}

package com.dji.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.JTableHeader;

import com.dji.factory.ProductionRouter;
import com.dji.promotion.PromotionCalculator;
import com.dji.repository.JsonOrderRepository;
import com.dji.sale.Order;
import com.dji.sale.OrderItem;
import com.dji.sale.OrderLoadResult;
import com.dji.sale.OrderStatus;
import com.dji.sale.ProductionBase;
import com.dji.sale.PromotionResult;
import com.dji.sale.PromotionType;
import com.dji.sale.Region;
import com.dji.service.OrderQueryService;
import com.dji.service.OrderSearchCriteria;
import com.dji.service.OrderStatisticsService;
import com.dji.service.ProductionService;
import com.dji.service.ProductionService.BatchProductionResult;
import com.dji.service.ProductionService.ProductionFailureException;
import com.dji.service.ProductionService.ProductionOutcome;
import com.dji.service.SystemSummary;

/**
 * 系统主窗口（任务 6，七项界面功能）。
 *
 * 线程约定：全部组件更新都在事件派发线程（EDT）执行——文件加载与生产
 * 处理由 SwingWorker 在后台线程完成，经 done()/process()
 * 回到 EDT 刷新；任务进行中禁用"打开/生产"按钮防止并发重入。事件监听器只
 * 收集输入、调用应用服务并呈现结果，促销与路由规则全部在服务层实现。
 *
 * @author liuyuxin (Gemini 4 Argon)
 * @version 1.0
 */
public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	// ---- 应用服务（UI 与业务解耦：规则全部在服务层） ----
	private final transient JsonOrderRepository repository = new JsonOrderRepository();
	private final transient PromotionCalculator promotionCalculator = new PromotionCalculator();
	private final transient OrderQueryService queryService = new OrderQueryService();
	private final transient OrderStatisticsService statisticsService = new OrderStatisticsService();
	private final transient ProductionRouter router = new ProductionRouter();
	private final transient ProductionService productionService = new ProductionService(router);

	// ---- 界面状态 ----
	private transient List<Order> allOrders = List.of();
	private final transient Map<String, PromotionResult> promoCache = new java.util.LinkedHashMap<>();

	// ---- 组件 ----
	private final OrderTableModel orderTableModel = new OrderTableModel(promoCache::get);
	private final JTable orderTable = new JTable(orderTableModel);
	private final JTextField keywordField = new JTextField(12);
	private final javax.swing.JComboBox<Region> regionCombo = buildRegionCombo();
	private final javax.swing.JComboBox<OrderStatus> statusCombo = buildStatusCombo();
	private final JButton openButton = new JButton("打开订单文件");
	private final JButton filterButton = new JButton("筛选");
	private final JButton clearButton = new JButton("清除条件");
	private final JButton produceSelectedButton = new JButton("处理选中订单");
	private final JButton produceAllButton = new JButton("处理全部订单");
	private final JTextArea detailArea = new JTextArea();
	private final JTextArea summaryArea = new JTextArea();
	private final JTextArea errorArea = new JTextArea();
	private final JLabel statusLabel = new JLabel(
			"未加载订单文件：请点击\"打开订单文件\"，或以命令行参数指定 JSON 路径");

	/** 构建主窗口（必须在 EDT 调用）。 */
	public MainFrame() {
		super("大疆全球订单生产管理系统");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		add(buildToolBar(), BorderLayout.NORTH);
		add(buildCenterSplit(), BorderLayout.CENTER);
		add(statusLabel, BorderLayout.SOUTH);

		// 行选择联动详情（监听器只取输入、调服务、刷新视图）
		orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		orderTable.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				showSelectedOrderDetail();
			}
		});

		openButton.addActionListener(event -> chooseAndLoadFile());
		filterButton.addActionListener(event -> applyFilter());
		clearButton.addActionListener(event -> clearFilter());
		produceSelectedButton.addActionListener(event -> produceSelected());
		produceAllButton.addActionListener(event -> produceAll());

		setSize(1200, 760);
		setLocationRelativeTo(null);
	}

	// ---- 布局构建 ----

	private JPanel buildToolBar() {
		JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		toolBar.add(openButton);
		toolBar.add(new JLabel("关键字:"));
		toolBar.add(keywordField);
		toolBar.add(new JLabel("地区:"));
		toolBar.add(regionCombo);
		toolBar.add(new JLabel("状态:"));
		toolBar.add(statusCombo);
		toolBar.add(filterButton);
		toolBar.add(clearButton);
		toolBar.add(produceSelectedButton);
		toolBar.add(produceAllButton);
		return toolBar;
	}

	private JSplitPane buildCenterSplit() {
		JTableHeader header = orderTable.getTableHeader();
		header.setReorderingAllowed(false);
		JScrollPane tableScroll = new JScrollPane(orderTable);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("订单详情与生产结果", wrapReadOnly(detailArea));
		tabs.addTab("汇总统计", wrapReadOnly(summaryArea));
		tabs.addTab("加载错误汇总", wrapReadOnly(errorArea));

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, tabs);
		split.setResizeWeight(0.55);
		return split;
	}

	private JScrollPane wrapReadOnly(JTextArea area) {
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		return new JScrollPane(area);
	}

	private static javax.swing.JComboBox<Region> buildRegionCombo() {
		javax.swing.JComboBox<Region> combo = new javax.swing.JComboBox<>();
		combo.addItem(null); // 首项 = 全部地区
		for (Region region : Region.values()) {
			combo.addItem(region);
		}
		combo.setRenderer(new NullSafeComboRenderer("全部地区"));
		return combo;
	}

	private static javax.swing.JComboBox<OrderStatus> buildStatusCombo() {
		javax.swing.JComboBox<OrderStatus> combo = new javax.swing.JComboBox<>();
		combo.addItem(null); // 首项 = 全部状态
		for (OrderStatus status : OrderStatus.values()) {
			combo.addItem(status);
		}
		combo.setRenderer(new NullSafeComboRenderer("全部状态"));
		return combo;
	}

	/** null 首项显示为"全部…"的下拉渲染器。 */
	private static final class NullSafeComboRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		private final String nullText;

		private NullSafeComboRenderer(String nullText) {
			this.nullText = nullText;
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			Object display = value == null ? nullText : value;
			if (display instanceof Region region) {
				display = region.getDisplayName();
			} else if (display instanceof OrderStatus status) {
				display = status.getDisplayName();
			}
			return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
		}
	}

	// ---- 文件加载（G1/G7，SwingWorker 异步） ----

	private void chooseAndLoadFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("选择订单 JSON 文件");
		chooser.setCurrentDirectory(Path.of("src/main/resources").toFile());
		int option = chooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			loadOrdersFromFile(chooser.getSelectedFile().toPath());
		}
	}

	/**
	 * 异步加载订单文件（可由按钮触发，也可由 main 启动参数触发）。
	 *
	 * @param file 订单 JSON 文件
	 */
	public void loadOrdersFromFile(Path file) {
		setBusy(true, "加载中: " + file + " ...");
		new OrderLoadWorker(file).execute();
	}

	/** 后台读取解析 JSON 并预计算促销，完成后在 EDT 刷新全部视图。 */
	private final class OrderLoadWorker extends SwingWorker<OrderLoadResult, Void> {

		private final Path file;
		private String failureMessage;

		private OrderLoadWorker(Path file) {
			this.file = file;
		}

		@Override
		protected OrderLoadResult doInBackground() {
			try {
				OrderLoadResult result = repository.loadOrders(file);
				computePromotions(result.validOrders());
				return result;
			} catch (RuntimeException e) {
				// 含 OrderFileParseException（文件不可读/语法损坏）与一切运行期异常
				failureMessage = e.getMessage();
				return null;
			}
		}

		@Override
		protected void done() {
			OrderLoadResult result = null;
			try {
				result = get();
			} catch (Exception e) {
				failureMessage = "加载线程异常: " + e.getMessage();
			}
			setBusy(false, null);
			if (result == null) {
				statusLabel.setText("加载失败: " + file + " — " + failureMessage
						+ "（请确认 JSON 语法与路径后重试）");
				JOptionPane.showMessageDialog(MainFrame.this,
						"订单文件加载失败:\n" + failureMessage
								+ "\n\n排查建议: 1) 确认文件是 JSON 数组格式; 2) 确认路径存在且可读。",
						"加载失败", JOptionPane.ERROR_MESSAGE);
				return;
			}
			allOrders = result.validOrders();
			applyFilterInternal();
			errorArea.setText(result.hasErrors() ? result.describeErrors() : "（本文件无加载错误）");
			updateSummary();
			statusLabel.setText("加载成功: " + file.toAbsolutePath()
					+ "（有效 " + allOrders.size() + " 笔"
					+ (result.hasErrors() ? "，异常 " + result.errors().size() + " 笔" : "")
					+ "）；尚未执行生产");
			selectFirstRowQuietly();
		}
	}

	/** 在后台线程预计算全部订单最优促销（供表格与详情使用）。 */
	private void computePromotions(List<Order> orders) {
		promoCache.clear();
		for (Order order : orders) {
			promoCache.put(order.getOrderId(), promotionCalculator.evaluateBestPromotion(order));
		}
	}

	// ---- 查询筛选（G3） ----

	private void applyFilter() {
		applyFilterInternal();
		if (orderTableModel.getRowCount() == 0 && !currentCriteria().isEmpty()) {
			statusLabel.setText("筛选无匹配结果（条件组合过严），可点击\"清除条件\"恢复全部 "
					+ allOrders.size() + " 笔订单");
		} else {
			statusLabel.setText("筛选完成：显示 " + orderTableModel.getRowCount()
					+ " / " + allOrders.size() + " 笔订单");
		}
	}

	private void clearFilter() {
		keywordField.setText("");
		regionCombo.setSelectedIndex(0);
		statusCombo.setSelectedIndex(0);
		applyFilterInternal();
		statusLabel.setText("已清除筛选条件：显示全部 " + allOrders.size() + " 笔订单");
	}

	private OrderSearchCriteria currentCriteria() {
		return new OrderSearchCriteria(keywordField.getText(),
				(Region) regionCombo.getSelectedItem(),
				(OrderStatus) statusCombo.getSelectedItem());
	}

	private void applyFilterInternal() {
		orderTableModel.setOrders(queryService.searchOrders(allOrders, currentCriteria(),
				OrderQueryService.BY_ORDER_ID_ASC));
	}

	// ---- 生产处理（G5，SwingWorker 异步） ----

	private void produceSelected() {
		int viewRow = orderTable.getSelectedRow();
		if (viewRow < 0) {
			statusLabel.setText("请先在订单表格中选择一行，再执行\"处理选中订单\"");
			return;
		}
		Order order = orderTableModel.getOrderAt(orderTable.convertRowIndexToModel(viewRow));
		setBusy(true, "生产进行中: 订单 " + order.getOrderId() + " ...");
		new ProductionWorker(List.of(order)).execute();
	}

	private void produceAll() {
		if (allOrders.isEmpty()) {
			statusLabel.setText("尚未加载任何订单：请先\"打开订单文件\"再执行生产");
			return;
		}
		setBusy(true, "生产进行中: 0/" + allOrders.size() + " ...");
		new ProductionWorker(allOrders).execute();
	}

	/** 后台逐单生产并逐单回传进度；已生产订单由服务层幂等跳过。 */
	private final class ProductionWorker extends SwingWorker<BatchProductionResult, ProductionOutcome> {

		private final List<Order> targets;
		private int processed;

		private ProductionWorker(List<Order> targets) {
			this.targets = targets;
		}

		@Override
		protected BatchProductionResult doInBackground() {
			int success = 0;
			int failed = 0;
			for (Order order : targets) {
				try {
					ProductionOutcome outcome = productionService.processOrder(order);
					if (outcome.success()) {
						success++;
					} else {
						failed++;
					}
					publish(outcome);
				} catch (ProductionFailureException e) {
					failed++;
					publish(new ProductionOutcome(e.getRecord().getOrderId(), false, e.getMessage(),
							e.getRecord().getTasks(), e.getRecord().getStatus()));
				}
			}
			return new BatchProductionResult(targets.size(), success, failed, List.of());
		}

		@Override
		protected void process(List<ProductionOutcome> chunks) {
			// EDT：实时进度反馈（窗口不冻结）
			processed += chunks.size();
			statusLabel.setText("生产进行中: " + processed + "/" + targets.size() + " ...");
		}

		@Override
		protected void done() {
			try {
				BatchProductionResult result = get();
				orderTableModel.setOrders(orderTableModel.currentOrders()); // 刷新状态列（EDT）
				updateSummary();
				showSelectedOrderDetail();
				setBusy(false, null);
				statusLabel.setText("生产处理完成：成功 " + result.successCount() + " 笔，失败 "
						+ result.failedCount() + " 笔（已生产订单重复处理将自动幂等跳过）");
			} catch (Exception e) {
				setBusy(false, null);
				statusLabel.setText("生产线程异常: " + e.getMessage());
			}
		}
	}

	// ---- 详情与汇总（G4/G6） ----

	private void selectFirstRowQuietly() {
		if (orderTableModel.getRowCount() > 0) {
			orderTable.setRowSelectionInterval(0, 0);
		} else {
			detailArea.setText("（无可显示的订单）");
		}
	}

	private void showSelectedOrderDetail() {
		int viewRow = orderTable.getSelectedRow();
		if (viewRow < 0 || viewRow >= orderTableModel.getRowCount()) {
			return;
		}
		Order order = orderTableModel.getOrderAt(orderTable.convertRowIndexToModel(viewRow));
		PromotionResult best = promoCache.get(order.getOrderId());
		StringBuilder sb = new StringBuilder();
		sb.append("订单号: ").append(order.getOrderId()).append('\n');
		sb.append("客户: ").append(order.getCustomer().getNormalizedName())
				.append(" | ").append(order.getCustomer().getEmail())
				.append(" | ").append(order.getCustomer().getPhone()).append('\n');
		sb.append("地区: ").append(order.getRegion().getDisplayName())
				.append(" | 状态: ").append(order.getStatus().getDisplayName())
				.append(" | 下单时间: ").append(order.getOrderDate()).append('\n');
		sb.append("原价: ").append(order.getCurrency().formatAmount(order.getOriginalAmount()));
		if (best != null) {
			sb.append(" | 优惠: ").append(best.getPromotionName())
					.append(" | 优惠金额: ").append(order.getCurrency().formatAmount(best.getDiscountAmount()))
					.append(" | 应付: ").append(order.getCurrency().formatAmount(best.getPayableAmount()));
		}
		sb.append('\n');
		sb.append("商品明细:\n");
		for (OrderItem item : order.getItems()) {
			sb.append("  ").append(item.getProductType()).append(" x ").append(item.getQuantity())
					.append(" | 单价 ").append(order.getCurrency().formatAmount(item.getUnitPrice()))
					.append(" | 小计 ").append(order.getCurrency().formatAmount(item.getTotalAmount()))
					.append('\n');
		}
		if (best != null) {
			sb.append("促销候选方案:\n");
			for (PromotionResult option : promotionCalculator.evaluateAllApplicable(order)) {
				sb.append("  ").append(option.getType() == PromotionType.NO_PROMOTION ? "[兜底] " : "[候选] ")
						.append(option.getPromotionName())
						.append(" | 优惠 ").append(order.getCurrency().formatAmount(option.getDiscountAmount()))
						.append(" | 应付 ").append(order.getCurrency().formatAmount(option.getPayableAmount()))
						.append(option.getType() == best.getType() ? "  ← 最优" : "")
						.append('\n');
			}
		}
		sb.append("生产结果:\n");
		ProductionService.OrderProductionRecord record = productionService.getRecord(order.getOrderId());
		if (record == null) {
			sb.append("  （尚未执行生产；基地预览: ").append(basePreview(order)).append("）\n");
		} else {
			for (var task : record.getTasks()) {
				sb.append("  ").append(task.getBase().getDisplayName())
						.append(" | ").append(task.getProductType())
						.append(" | 完成 ").append(task.getProducedCount()).append('/').append(task.getQuantity())
						.append(" | SN: ").append(String.join(", ", task.getSerialNumbers()))
						.append('\n');
			}
			if (record.getStatus() == ProductionService.RecordStatus.SUCCESS) {
				sb.append("  状态: 已全部完成\n");
			} else {
				sb.append("  状态: ").append(record.getStatus()).append(" — ").append(record.getFailureReason())
						.append("（可重新点击\"处理选中订单\"补产缺失数量）\n");
			}
		}
		detailArea.setText(sb.toString());
		detailArea.setCaretPosition(0);
	}

	/** 未生产时的基地预览（只读解析，不消耗序列号）。 */
	private String basePreview(Order order) {
		return order.getItems().stream()
				.map(item -> {
					try {
						ProductionBase base = router.resolveBase(order.getRegion(), item.getCategory());
						return base.getDisplayName() + "(" + item.getProductType() + " x " + item.getQuantity() + ")";
					} catch (RuntimeException e) {
						return "无法路由(" + item.getProductType() + ")";
					}
				})
				.collect(Collectors.joining("、"));
	}

	private void updateSummary() {
		// 必须按 orderId 取缓存；直接写 promoCache::get 会把 Order 对象当 key
		// （Map.get 参数是 Object，编译不报错但恒返回 null → 促销列 NPE）
		SystemSummary summary = statisticsService.buildSummary(allOrders,
				order -> promoCache.get(order.getOrderId()), router, productionService);
		StringBuilder sb = new StringBuilder();
		sb.append("订单统计: 共 ").append(summary.totalOrders()).append(" 笔（已生产 ")
				.append(summary.processedOrderCount()).append("，失败 ").append(summary.failedOrderCount())
				.append("，待处理 ")
				.append(summary.totalOrders() - summary.processedOrderCount() - summary.failedOrderCount())
				.append("）\n\n");
		sb.append("优惠总额（按币种分列，不跨币种相加）:\n");
		sb.append(summary.discountByCurrency().entrySet().stream()
				.map(entry -> entry.getKey() + " " + entry.getValue().toPlainString())
				.collect(Collectors.joining("  |  "))).append("\n\n");
		sb.append("各基地 ｜ 预计需求 / 实际已产（架）:\n");
		for (ProductionBase base : ProductionBase.values()) {
			int planned = summary.plannedUnitsByBase().getOrDefault(base, 0);
			int produced = summary.producedUnitsByBase().getOrDefault(base, 0);
			sb.append(String.format("  %-14s: %3d / %3d%n", base.getDisplayName(), planned, produced));
		}
		sb.append(String.format("  %-14s: %3d / %3d%n", "合计",
				summary.plannedUnitsTotal(), summary.producedUnitsTotal()));
		sb.append("\n型号需求量（降序）: ");
		sb.append(summary.quantityByModel().entrySet().stream()
				.map(entry -> entry.getKey() + " x" + entry.getValue())
				.collect(Collectors.joining("  |  ")));
		summaryArea.setText(sb.toString());
		summaryArea.setCaretPosition(0);
	}

	// ---- 界面状态（G7：任务进行中禁用重入按钮） ----

	private void setBusy(boolean busy, String busyMessage) {
		openButton.setEnabled(!busy);
		filterButton.setEnabled(!busy);
		clearButton.setEnabled(!busy);
		produceSelectedButton.setEnabled(!busy);
		produceAllButton.setEnabled(!busy);
		if (busyMessage != null) {
			statusLabel.setText(busyMessage);
		}
	}
}

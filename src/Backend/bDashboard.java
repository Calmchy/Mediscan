/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

/**
 *
 * @author Calmchy
 */
public class bDashboard {
    public static final int LOW_STOCK_LIMIT = 20;
    public static final int CRITICAL_STOCK = 10;
    public static final int EXPIRING_DAYS = 60;
    public static final int SOON_RED_DAYS = 30;
 
    public static final Color TEAL = new Color(43, 138, 130);
    public static final Color NAVY = new Color(30, 58, 95);
    public static final Color BLUE = new Color(79, 172, 255);
    public static final Color AMBER = new Color(221, 161, 40);
    public static final Color SLATE = new Color(144, 161, 196);
    public static final Color RED = new Color(192, 57, 43);
    public static final Color AMBER_TEXT = new Color(154, 103, 0);
    private static final Color GRID = new Color(232, 236, 241);
    private static final Color AXIS_TEXT = new Color(91, 102, 122);
    private static final Color HEADER_BG = new Color(215, 222, 230);
    private static final Color HEADER_FG = new Color(26, 31, 46);
    private static final Color HEADER_LINE = new Color(195, 204, 214);
    private static final Color[] PIE_COLORS = {TEAL, NAVY, BLUE, AMBER, SLATE};
 
    private static final Font CHART_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font CHART_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");
    private static final String PESO = "\u20B1 ";
 
    private static boolean adjusting = false;
 
 
    public static class DashboardFields {
        public JButton todayBtn;
        public JButton sevenDaysBtn;
        public JButton thirtyDaysBtn;
        public com.toedter.calendar.JDateChooser fromDC;
        public com.toedter.calendar.JDateChooser toDC;
 
        public JLabel totalSalesL;
        public JLabel transactionsL;
        public JLabel itemsSoldL;
        public JLabel lowStockL;
        public JLabel expiringL;
        public JLabel expiredL;
 
        public JPanel salesPerDayP;
        public JPanel salesByCategoryP;
        public JPanel top5ProductsP;
 
        public JTable lowStockTable;
        public JTable expiringTable;
    }
 
    public static void init(DashboardFields f) {
        if (bTransaction.isCashier()) {
            return;
        }
        styleTable(f.lowStockTable, 1);
        styleTable(f.expiringTable, 3);
        setPeriod(f, 7);
    }
 
    public static void setPeriod(DashboardFields f, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);
        adjusting = true;
        f.fromDC.setDate(toDate(from));
        f.toDC.setDate(toDate(to));
        adjusting = false;
 
        markActive(f.todayBtn, days == 1);
        markActive(f.sevenDaysBtn, days == 7);
        markActive(f.thirtyDaysBtn, days == 30);
        refresh(f, false);
    }
 
    public static void onDatesChanged(DashboardFields f) {
        if (adjusting) {
            return;
        }
        markActive(f.todayBtn, false);
        markActive(f.sevenDaysBtn, false);
        markActive(f.thirtyDaysBtn, false);
        refresh(f, true);
    }
 
    public static void refresh(DashboardFields f) {
        refresh(f, false);
    }
 
    private static void refresh(DashboardFields f, boolean quiet) {
        LocalDate from = toLocalDate(f.fromDC.getDate());
        LocalDate to = toLocalDate(f.toDC.getDate());
        if (from == null || to == null) {
            return;
        }
        if (from.isAfter(to)) {
            if (!quiet) {
                JOptionPane.showMessageDialog(null, "The From date must not be after the To date.");
            }
            return;
        }
        loadCards(f, from, to);
        loadCharts(f, from, to);
        setLowStock(f);
        setExpiringSoon(f);
    }
 
    private static void markActive(JButton b, boolean active) {
        if (b == null) {
            return;
        }
        b.setOpaque(true);
        b.setBackground(active ? TEAL : UIManager.getColor("Button.background"));
        b.setForeground(active ? Color.WHITE : UIManager.getColor("Button.foreground"));
        b.setFont(b.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
    }
 
    public static void loadCards(DashboardFields f, LocalDate from, LocalDate to) {
        Timestamp start = Timestamp.valueOf(from.atStartOfDay());
        Timestamp end = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
 
        String salesSql = "SELECT COUNT(*) AS txn, COALESCE(SUM(total_amount), 0) AS total "
                + "FROM orders WHERE order_date >= ? AND order_date < ?";
        String itemsSql = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi "
                + "JOIN orders o ON oi.order_id = o.order_id WHERE o.order_date >= ? AND o.order_date < ?";
        String lowSql = "SELECT COUNT(*) FROM (SELECT p.product_id FROM products p "
                + "LEFT JOIN product_stock ps ON ps.product_id = p.product_id "
                + "GROUP BY p.product_id "
                + "HAVING COALESCE(SUM(CASE WHEN ps.status = 'active' "
                + "AND (ps.expiry_date IS NULL OR ps.expiry_date >= CURRENT_DATE) "
                + "THEN ps.quantity END), 0) <= ?) t";
        String expSql = "SELECT "
                + "COALESCE(SUM(CASE WHEN expiry_date < CURRENT_DATE THEN 1 ELSE 0 END), 0) AS expired, "
                + "COALESCE(SUM(CASE WHEN expiry_date >= CURRENT_DATE "
                + "AND expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) THEN 1 ELSE 0 END), 0) AS expiring "
                + "FROM product_stock WHERE quantity > 0 AND expiry_date IS NOT NULL";
 
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(salesSql)) {
                ps.setTimestamp(1, start);
                ps.setTimestamp(2, end);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        f.transactionsL.setText(WHOLE.format(rs.getInt("txn")));
                        f.totalSalesL.setText(PESO + MONEY.format(rs.getDouble("total")));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(itemsSql)) {
                ps.setTimestamp(1, start);
                ps.setTimestamp(2, end);
                try (ResultSet rs = ps.executeQuery()) {
                    f.itemsSoldL.setText(rs.next() ? WHOLE.format(rs.getInt(1)) : "0");
                }
            }
            try (PreparedStatement ps = con.prepareStatement(lowSql)) {
                ps.setInt(1, LOW_STOCK_LIMIT);
                try (ResultSet rs = ps.executeQuery()) {
                    f.lowStockL.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
                }
            }
            try (PreparedStatement ps = con.prepareStatement(expSql)) {
                ps.setInt(1, EXPIRING_DAYS);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        f.expiringL.setText(String.valueOf(rs.getInt("expiring")));
                        f.expiredL.setText(String.valueOf(rs.getInt("expired")));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void loadCharts(DashboardFields f, LocalDate from, LocalDate to) {
        Timestamp start = Timestamp.valueOf(from.atStartOfDay());
        Timestamp end = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
        try (Connection con = DBConnection.getConnection()) {
            Map<String, Double> perDay = loadSalesPerDay(con, from, to, start, end);
            Map<String, Double> byCategory = loadSalesByCategory(con, start, end);
            Map<String, Integer> top5 = loadTopProducts(con, start, end);
 
            boolean hasSales = perDay.values().stream().anyMatch(v -> v > 0);
            if (hasSales) {
                install(f.salesPerDayP, createSalesPerDayChart(perDay));
            } else {
                showMessage(f.salesPerDayP, "No sales in this period");
            }
            if (byCategory.isEmpty()) {
                showMessage(f.salesByCategoryP, "No sales in this period");
            } else {
                install(f.salesByCategoryP, createSalesByCategoryPanel(byCategory));
            }
            if (top5.isEmpty()) {
                showMessage(f.top5ProductsP, "No sales in this period");
            } else {
                install(f.top5ProductsP, createTop5Chart(top5));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    private static String bucketLabel(LocalDate d, long totalDays) {
        if (totalDays <= 31) {
            return d.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
        }
        if (totalDays <= 90) {
            return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
        }
        return YearMonth.from(d).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
    }
 
    private static Map<String, Double> loadSalesPerDay(Connection con, LocalDate from, LocalDate to,
            Timestamp start, Timestamp end) throws Exception {
        long totalDays = ChronoUnit.DAYS.between(from, to) + 1;
        Map<String, Double> buckets = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            buckets.putIfAbsent(bucketLabel(d, totalDays), 0.0);
        }
        String sql = "SELECT DATE(order_date) AS d, SUM(total_amount) AS total FROM orders "
                + "WHERE order_date >= ? AND order_date < ? GROUP BY DATE(order_date)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = bucketLabel(rs.getDate("d").toLocalDate(), totalDays);
                    buckets.merge(key, rs.getDouble("total"), Double::sum);
                }
            }
        }
        return buckets;
    }
 
    private static Map<String, Double> loadSalesByCategory(Connection con, Timestamp start, Timestamp end)
            throws Exception {
        String sql = "SELECT COALESCE(c.category_name, 'Uncategorized') AS cat, SUM(oi.subtotal) AS total "
                + "FROM order_items oi JOIN orders o ON oi.order_id = o.order_id "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE o.order_date >= ? AND o.order_date < ? "
                + "GROUP BY COALESCE(c.category_name, 'Uncategorized') ORDER BY total DESC";
        Map<String, Double> result = new LinkedHashMap<>();
        double others = 0;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (result.size() < 4) {
                        result.put(rs.getString("cat"), rs.getDouble("total"));
                    } else {
                        others += rs.getDouble("total");
                    }
                }
            }
        }
        if (others > 0) {
            result.put("Others", others);
        }
        return result;
    }
    
    private static Map<String, Integer> loadTopProducts(Connection con, Timestamp start, Timestamp end)
            throws Exception {
        String sql = "SELECT p.name, SUM(oi.quantity) AS units FROM order_items oi "
                + "JOIN orders o ON oi.order_id = o.order_id JOIN products p ON oi.product_id = p.product_id "
                + "WHERE o.order_date >= ? AND o.order_date < ? "
                + "GROUP BY p.product_id, p.name ORDER BY units DESC, p.name LIMIT 5";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("name"), rs.getInt("units"));
                }
            }
        }
        return result;
    }
 
    public static ChartPanel createSalesPerDayChart(Map<String, Double> data) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        data.forEach((label, value) -> ds.addValue(value, "Sales", label));
 
        JFreeChart chart = ChartFactory.createBarChart(null, null, null, ds,
                PlotOrientation.VERTICAL, false, false, false);
        CategoryPlot plot = styleCategoryPlot(chart);
        BarRenderer renderer = styleBars(plot, TEAL);
        renderer.setMaximumBarWidth(0.07);
 
        boolean crowded = data.size() > 10;
        renderer.setDefaultItemLabelsVisible(!crowded);
        renderer.setDefaultPositiveItemLabelPosition(
                new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
 
        CategoryAxis domain = plot.getDomainAxis();
        domain.setCategoryLabelPositions(crowded ? CategoryLabelPositions.UP_45 : CategoryLabelPositions.STANDARD);
        domain.setLowerMargin(0.03);
        domain.setUpperMargin(0.03);
 
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double step = niceStep(max / 4);
        range.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(step));
        range.setRange(0, step * Math.ceil(max * 1.1 / step));
        range.setNumberFormatOverride(WHOLE);
        range.setAxisLineVisible(false);
        range.setTickMarksVisible(false);
        range.setTickLabelFont(CHART_FONT);
        range.setTickLabelPaint(AXIS_TEXT);
        return toPanel(chart);
    }
 
    private static double niceStep(double raw) {
        if (raw <= 1) {
            return 1;
        }
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double frac = raw / pow;
        double nice = frac <= 1 ? 1 : frac <= 2 ? 2 : frac <= 2.5 ? 2.5 : frac <= 5 ? 5 : 10;
        return nice * pow;
    }
 
    public static ChartPanel createTop5Chart(Map<String, Integer> data) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        data.forEach((name, units) -> ds.addValue(units, "Units", name));
 
        JFreeChart chart = ChartFactory.createBarChart(null, null, null, ds,
                PlotOrientation.HORIZONTAL, false, false, false);
        CategoryPlot plot = styleCategoryPlot(chart);
        plot.setRangeGridlinesVisible(false);
        BarRenderer renderer = styleBars(plot, NAVY);
        renderer.setMaximumBarWidth(0.09);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultPositiveItemLabelPosition(
                new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT));
 
        plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(0.5f);
        plot.getRangeAxis().setVisible(false);
        plot.getRangeAxis().setUpperMargin(0.2);
        return toPanel(chart);
    }
 
    public static JPanel createSalesByCategoryPanel(Map<String, Double> data) {
        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
 
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        data.forEach((k, v) -> ds.setValue(k, v));
        JFreeChart chart = ChartFactory.createPieChart(null, ds, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        chart.setAntiAlias(true);
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelGenerator(null);
        plot.setCircular(true);
        plot.setInteriorGap(0.04);
        plot.setSectionOutlinesVisible(true);
        plot.setDefaultSectionOutlinePaint(Color.WHITE);
        plot.setDefaultSectionOutlineStroke(new java.awt.BasicStroke(1.5f));
 
        JPanel legend = new JPanel();
        legend.setOpaque(false);
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.add(javax.swing.Box.createVerticalGlue());
        int i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            Color color = PIE_COLORS[Math.min(i, PIE_COLORS.length - 1)];
            if ("Others".equals(e.getKey())) {
                color = PIE_COLORS[PIE_COLORS.length - 1];
            }
            plot.setSectionPaint(e.getKey(), color);
            legend.add(legendRow(color, e.getKey(), Math.round(e.getValue() / total * 100) + "%"));
            i++;
        }
        legend.add(javax.swing.Box.createVerticalGlue());
 
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);
        ChartPanel cp = toPanel(chart);
        panel.add(cp);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));
        right.add(legend, BorderLayout.CENTER);
        panel.add(right);
        return panel;
    }
 
    private static JComponent legendRow(Color color, String name, String percent) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30));
 
        JLabel square = new JLabel();
        square.setOpaque(true);
        square.setBackground(color);
        square.setPreferredSize(new java.awt.Dimension(12, 12));
        JPanel squareHolder = new JPanel(new java.awt.GridBagLayout());
        squareHolder.setOpaque(false);
        squareHolder.add(square);
 
        JLabel nameL = new JLabel(name);
        nameL.setFont(CHART_FONT.deriveFont(12f));
        nameL.setForeground(new Color(70, 76, 86));
        JLabel pctL = new JLabel(percent, SwingConstants.RIGHT);
        pctL.setFont(CHART_FONT_BOLD.deriveFont(12f));
        pctL.setForeground(NAVY);
 
        row.add(squareHolder, BorderLayout.WEST);
        row.add(nameL, BorderLayout.CENTER);
        row.add(pctL, BorderLayout.EAST);
        return row;
    }
 
    private static CategoryPlot styleCategoryPlot(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(GRID);
 
        CategoryAxis domain = plot.getDomainAxis();
        domain.setTickMarksVisible(false);
        domain.setAxisLinePaint(GRID);
        domain.setTickLabelFont(CHART_FONT);
        domain.setTickLabelPaint(AXIS_TEXT);
        return plot;
    }
 
    private static BarRenderer styleBars(CategoryPlot plot, Color color) {
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, color);
        renderer.setDrawBarOutline(false);
        renderer.setItemMargin(0.0);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", WHOLE));
        renderer.setDefaultItemLabelFont(CHART_FONT_BOLD);
        renderer.setDefaultItemLabelPaint(NAVY);
        return renderer;
    }
 
    private static ChartPanel toPanel(JFreeChart chart) {
        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(Color.WHITE);
        cp.setPopupMenu(null);
        cp.setDomainZoomable(false);
        cp.setRangeZoomable(false);
        cp.setMouseWheelEnabled(false);
        cp.setMinimumDrawWidth(0);
        cp.setMinimumDrawHeight(0);
        cp.setMaximumDrawWidth(4000);
        cp.setMaximumDrawHeight(4000);
        return cp;
    }
 
    private static void install(JPanel host, JComponent content) {
        host.removeAll();
        host.setLayout(new BorderLayout());
        host.add(content, BorderLayout.CENTER);
        host.revalidate();
        host.repaint();
    }
 
    private static void showMessage(JPanel host, String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(AXIS_TEXT);
        install(host, l);
    }
 
    public static void setLowStock(DashboardFields f) {
        String sql = "SELECT p.name, COALESCE(SUM(CASE WHEN ps.status = 'active' "
                + "AND (ps.expiry_date IS NULL OR ps.expiry_date >= CURRENT_DATE) "
                + "THEN ps.quantity END), 0) AS units "
                + "FROM products p LEFT JOIN product_stock ps ON ps.product_id = p.product_id "
                + "GROUP BY p.product_id, p.name HAVING units <= ? ORDER BY units ASC, p.name";
        
        DefaultTableModel model = (DefaultTableModel) f.lowStockTable.getModel();
        model.setRowCount(0);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, LOW_STOCK_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getString("name"), rs.getInt("units")});
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void setExpiringSoon(DashboardFields f) {
        String sql = "SELECT p.name, ps.batch_no, ps.expiry_date, DATEDIFF(ps.expiry_date, CURRENT_DATE) AS days_left "
                + "FROM product_stock ps JOIN products p ON ps.product_id = p.product_id "
                + "WHERE ps.quantity > 0 AND ps.expiry_date IS NOT NULL "
                + "AND ps.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL ? DAY) "
                + "ORDER BY ps.expiry_date ASC, p.name";
        DefaultTableModel model = (DefaultTableModel) f.expiringTable.getModel();
        model.setRowCount(0);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, EXPIRING_DAYS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int days = rs.getInt("days_left");
                    String batch = rs.getString("batch_no");
                    model.addRow(new Object[]{
                        rs.getString("name"),
                        batch == null || batch.trim().isEmpty() ? "-" : batch,
                        rs.getDate("expiry_date").toString(),
                        days < 0 ? "EXPIRED" : String.valueOf(days)
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    private static void styleTable(JTable table, int statusColumn) {
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(225, 230, 236));
        table.getTableHeader().setReorderingAllowed(false);
 
        DefaultTableCellRenderer header = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                    int row, int col) {
                super.getTableCellRendererComponent(t, value, false, false, row, col);
                setBackground(HEADER_BG);
                setForeground(HEADER_FG);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, HEADER_LINE));
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(header);
 
        DefaultTableCellRenderer status = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                    int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(getFont().deriveFont(Font.BOLD));
                setForeground(sel ? t.getSelectionForeground() : statusColor(t, statusColumn, value));
                return this;
            }
        };
        table.getColumnModel().getColumn(statusColumn).setCellRenderer(status);
    }
 
    private static Color statusColor(JTable table, int statusColumn, Object value) {
        String text = value == null ? "" : value.toString();
        if (statusColumn == 1) {
            return Integer.parseInt(text) <= CRITICAL_STOCK ? RED : AMBER_TEXT;
        }
        if ("EXPIRED".equals(text)) {
            return RED;
        }
        return Integer.parseInt(text) <= SOON_RED_DAYS ? RED : AMBER_TEXT;
    }
 
    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
 
    private static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}

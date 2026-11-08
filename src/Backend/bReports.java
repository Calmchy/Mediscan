/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Calmchy
 */
public class bReports {
    public enum Type { SALES, STOCK, EXPIRY }
 
    private static final int LOW_STOCK_LEVEL = 10;
    private static final int EXPIRY_WARNING_DAYS = 60;
 
    private static final Color HEADER_BG = new Color(0x2B, 0x8A, 0x7E);
    private static final Color ZEBRA_BG  = new Color(0xF2, 0xF4, 0xF7);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SUB_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private static final Font HEAD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    
    public static void generate(java.awt.Component parent, Type type, Date from, Date to) {
        if (type == Type.SALES && (from == null || to == null)) {
            javax.swing.JOptionPane.showMessageDialog(parent, "Please pick a From and To date first.");
            return;
        }
        if (type == Type.SALES && from.after(to)) {
            javax.swing.JOptionPane.showMessageDialog(parent, "'From' date must not be after 'To' date.");
            return;
        }
 
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Save report as PDF");
        fc.setSelectedFile(new File(type.name().toLowerCase() + "_report_" + stamp + ".pdf"));
        if (fc.showSaveDialog(parent) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new File(out.getParentFile(), out.getName() + ".pdf");
        }
 
        try {
            switch (type) {
                case SALES:  salesReport(out, from, to); break;
                case STOCK:  stockReport(out);           break;
                default:     expiryReport(out);          break;
            }
            int open = javax.swing.JOptionPane.showConfirmDialog(parent,
                    "Report saved:\n" + out.getAbsolutePath() + "\n\nOpen it now?",
                    "Report created", javax.swing.JOptionPane.YES_NO_OPTION);
            if (open == javax.swing.JOptionPane.YES_OPTION && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(parent,
                    "Could not create the report:\n" + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
 
    public static void salesReport(File out, Date from, Date to) throws SQLException, IOException, DocumentException {
        String sql =
            "SELECT o.order_id, o.order_date, "
          + "       CONCAT(u.first_name, ' ', u.last_name) AS cashier, "
          + "       COALESCE(CONCAT(c.first_name, ' ', c.last_name), 'N/A') AS customer, "
          + "       COALESCE(SUM(oi.quantity), 0) AS items, o.total_amount "
          + "FROM orders o "
          + "JOIN users u ON u.user_id = o.user_id "
          + "LEFT JOIN customers c ON c.customer_id = o.customer_id "
          + "LEFT JOIN order_items oi ON oi.order_id = o.order_id "
          + "WHERE DATE(o.order_date) BETWEEN ? AND ? "
          + "GROUP BY o.order_id, o.order_date, cashier, customer, o.total_amount "
          + "ORDER BY o.order_date";
 
        List<String[]> rows = new ArrayList<String[]>();
        double total = 0;
        int items = 0;
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(from.getTime()));
            ps.setDate(2, new java.sql.Date(to.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total += rs.getDouble("total_amount");
                    items += rs.getInt("items");
                    rows.add(new String[]{
                        String.valueOf(rs.getInt("order_id")),
                        dt.format(rs.getTimestamp("order_date")),
                        rs.getString("cashier"),
                        rs.getString("customer"),
                        String.valueOf(rs.getInt("items")),
                        money(rs.getDouble("total_amount"))
                    });
                }
            }
        }
 
        SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd");
        String range = "Period: " + d.format(from) + " to " + d.format(to);
        String summary = "Transactions: " + rows.size() + "     Items sold: " + items
                       + "     Total sales: " + money(total);
 
        buildPdf(out, "Sales Report", range,
                new String[]{"Order #", "Date", "Cashier", "Customer", "Items", "Total"},
                new float[]{1f, 2.2f, 2.4f, 2.4f, 1f, 1.6f},
                new int[]{Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_LEFT,
                          Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT},
                rows, summary);
    }
 
    public static void stockReport(File out) throws SQLException, IOException, DocumentException {
        String sql = "SELECT p.product_id, p.name, COALESCE(p.brand, '') AS brand, "
                + "COALESCE(cat.category_name, 'Uncategorized') AS category, "
                + "COALESCE(SUM(CASE WHEN ps.status = 'active' THEN ps.quantity END), 0) AS qty, p.selling_price "
                + "FROM products p "
                + "LEFT JOIN categories cat ON cat.category_id = p.category_id "
                + "LEFT JOIN product_stock ps ON ps.product_id = p.product_id "
                + "GROUP BY p.product_id, p.name, p.brand, category, p.selling_price "
                + "ORDER BY qty ASC, p.name";
 
        List<String[]> rows = new ArrayList<String[]>();
        double value = 0;
        int low = 0, out0 = 0;
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int qty = rs.getInt("qty");
                double price = rs.getDouble("selling_price");
                value += qty * price;
                String status = "OK";
                if (qty <= 0) { status = "OUT OF STOCK"; out0++; }
                else if (qty <= LOW_STOCK_LEVEL) { status = "LOW"; low++; }
                rows.add(new String[]{
                    String.valueOf(rs.getInt("product_id")),
                    rs.getString("name"),
                    rs.getString("brand"),
                    rs.getString("category"),
                    String.valueOf(qty),
                    money(price),
                    status
                });
            }
        }
 
        String summary = "Products: " + rows.size() + "     Low stock: " + low
                       + "     Out of stock: " + out0 + "     Stock value (selling price): " + money(value);
 
        buildPdf(out, "Stock Report",
                "As of " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())
                        + "   (low stock = " + LOW_STOCK_LEVEL + " units or less)",
                new String[]{"ID", "Product", "Brand", "Category", "Qty", "Price", "Status"},
                new float[]{0.8f, 3f, 1.8f, 2f, 0.9f, 1.4f, 1.6f},
                new int[]{Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT,
                          Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_CENTER},
                rows, summary);
    }
 
    public static void expiryReport(File out) throws SQLException, IOException, DocumentException {
        String sql =
            "SELECT p.name, COALESCE(ps.batch_no, '-') AS batch_no, ps.quantity, ps.expiry_date, "
          + "       DATEDIFF(ps.expiry_date, CURDATE()) AS days_left "
          + "FROM product_stock ps "
          + "JOIN products p ON p.product_id = ps.product_id "
          + "WHERE ps.quantity > 0 AND ps.expiry_date IS NOT NULL "
          + "  AND ps.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) "
          + "ORDER BY ps.expiry_date";
 
        List<String[]> rows = new ArrayList<String[]>();
        int expired = 0, soon = 0;
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, EXPIRY_WARNING_DAYS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int days = rs.getInt("days_left");
                    String status;
                    if (days < 0) { status = "EXPIRED"; expired++; }
                    else { status = "EXPIRING"; soon++; }
                    rows.add(new String[]{
                        rs.getString("name"),
                        rs.getString("batch_no"),
                        String.valueOf(rs.getInt("quantity")),
                        rs.getDate("expiry_date").toString(),
                        String.valueOf(days),
                        status
                    });
                }
            }
        }
 
        String summary = "Expired batches: " + expired + "     Expiring within "
                       + EXPIRY_WARNING_DAYS + " days: " + soon;
 
        buildPdf(out, "Expiry Report",
                "As of " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) 
                        + "   (batches expired or expiring within " + EXPIRY_WARNING_DAYS + " days)",
                new String[]{"Product", "Batch", "Qty", "Expiry", "Days left", "Status"},
                new float[]{3.2f, 1.6f, 1f, 1.8f, 1.2f, 1.6f},
                new int[]{Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_CENTER,
                          Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_CENTER},
                rows, summary);
    }
 
    private static void buildPdf(File out, String title, String subtitle, String[] headers,
            float[] widths, int[] aligns, List<String[]> rows, String summary) 
            throws IOException, DocumentException {
 
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();
 
            doc.add(new Paragraph("MediScan POS", SUB_FONT));
            doc.add(new Paragraph(title, TITLE_FONT));
            doc.add(new Paragraph(subtitle, SUB_FONT));
            String by = Session.fullName != null ? Session.fullName : "-";
            doc.add(new Paragraph("Generated by " + by + " on "
                    + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), SUB_FONT));
            doc.add(new Paragraph(" "));
 
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(widths);
            table.setHeaderRows(1);
 
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, HEAD_FONT));
                cell.setBackgroundColor(HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
 
            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No records found.", BODY_FONT));
                empty.setColspan(headers.length);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(10);
                table.addCell(empty);
            }
 
            boolean zebra = false;
            for (String[] r : rows) {
                for (int i = 0; i < r.length; i++) {
                    PdfPCell cell = new PdfPCell(new Phrase(r[i] == null ? "" : r[i], BODY_FONT));
                    cell.setHorizontalAlignment(aligns[i]);
                    cell.setPadding(4);
                    if (zebra) cell.setBackgroundColor(ZEBRA_BG);
                    table.addCell(cell);
                }
                zebra = !zebra;
            }
            doc.add(table);
 
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(summary, BOLD_FONT));
            doc.close();
        }
    }
 
    private static String money(double v) {
        return String.format("PHP %,.2f", v);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.oned.Code128Writer;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class bPrint {
    private static final String BARCODE_FOLDER_SALES = System.getProperty("user.home") + "/MediScanBarcodes/Sales/";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public static class Line {
        public final String name, unit;
        public final int quantity;
        public final double price, subtotal;
 
        public Line(String name, int quantity, String unit, double price, double subtotal) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.price = price;
            this.subtotal = subtotal;
        }
    }
    
    public static class ReceiptData {
        public int orderId;
        public String dateTime;
        public List<Line> lines = new ArrayList<>();
        public double total, cash, change;
        public boolean reprint;
        public String reprintedAt;
    }
    
    public static void printSale(bSales.CheckoutResult r) {
        ReceiptData d = new ReceiptData();
        d.orderId = r.orderId;
        d.dateTime = LocalDateTime.now().format(DT_FMT);
        d.total = r.total;
        d.cash = r.cash;
        d.change = r.change;
        for (bSales.CartItem it : r.items) {
            d.lines.add(new Line(it.name, it.quantity, it.unit, it.price, it.subtotal()));
        }
        printReceipt(d);
    }

    public static void reprintReceipt(Component parent, int orderId) {
        if (orderId < 0) {
            msg(parent, "Select an order first.");
            return;
        }
        ReceiptData d;
        try {
            d = loadReceipt(orderId);
        } catch (Exception e) {
            msg(parent, "Could not load the order: " + e.getMessage());
            return;
        }
        if (d == null) {
            msg(parent, "Order #" + orderId + " was not found.");
            return;
        }
        d.reprint = true;
        d.reprintedAt = LocalDateTime.now().format(DT_FMT);
        printReceipt(d);
    }

    public static void printReceipt(ReceiptData d) {
        saveBarcodeToFile(String.valueOf(d.orderId), BARCODE_FOLDER_SALES);
        try {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintable(createPrintable(d), pageFormat(pj.defaultPage(), d));
            pj.print();
        } catch (PrinterException ex) {
            ex.printStackTrace();
        }
    }

    public static ReceiptData loadReceipt(int orderId) throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            boolean hasPayment = hasColumn(con, "orders", "payment_amount")
                    && hasColumn(con, "orders", "change_amount");
            String headSql = "SELECT order_date, total_amount, "
                    + (hasPayment ? "payment_amount, change_amount " : "0 AS payment_amount, 0 AS change_amount ")
                    + "FROM orders WHERE order_id = ?";
 
            ReceiptData d = new ReceiptData();
            d.orderId = orderId;
            try (PreparedStatement ps = con.prepareStatement(headSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    d.dateTime = rs.getTimestamp("order_date").toLocalDateTime().format(DT_FMT);
                    d.total = rs.getDouble("total_amount");
                    d.cash = rs.getDouble("payment_amount");
                    d.change = rs.getDouble("change_amount");
                    if (d.cash == 0) {
                        d.cash = d.total;
                        d.change = 0;
                    }
                }
            }
 
            String itemSql = "SELECT p.name, oi.quantity, p.unit_type, oi.unit_price, oi.subtotal "
                    + "FROM order_items oi JOIN products p ON oi.product_id = p.product_id "
                    + "WHERE oi.order_id = ? ORDER BY oi.order_item_id";
            try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        d.lines.add(new Line(rs.getString("name"), rs.getInt("quantity"),
                                rs.getString("unit_type"), rs.getDouble("unit_price"), rs.getDouble("subtotal")));
                    }
                }
            }
            return d;
        }
    }

    public static PageFormat pageFormat(PageFormat pf, ReceiptData d) {
        Paper paper = pf.getPaper();
        double bodyHeight = d.lines.size() + (d.reprint ? 1 : 0);
        double headerHeight = 5.0;
        double footerHeight = 5.0;
        double width = cmToPoints(8);
        double height = cmToPoints(headerHeight + bodyHeight + footerHeight);
 
        paper.setSize(width, height);
        paper.setImageableArea(0, 10, width, height - cmToPoints(1));
        pf.setOrientation(PageFormat.PORTRAIT);
        pf.setPaper(paper);
        return pf;
    }
 
    public static Printable createPrintable(ReceiptData d) {
        return new ReceiptPrintable(d);
    }
 
    private static class ReceiptPrintable implements Printable {
 
        private final ReceiptData d;
 
        ReceiptPrintable(ReceiptData d) {
            this.d = d;
        }
 
        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
 
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());
 
            Font normalFont = new Font("Monospaced", Font.PLAIN, 7);
            Font boldFont = new Font("Monospaced", Font.BOLD, 8);
            g2d.setFont(normalFont);
            FontMetrics fm = g2d.getFontMetrics();
 
            int margin = 12;
            double paperWidth = pageFormat.getImageableWidth();
            double charW = fm.stringWidth(repeatChar('-', 20)) / 20.0;
            int cols = (int) ((paperWidth - (margin * 2)) / charW);
            String divider = repeatChar('-', cols);
            String stars = repeatChar('*', cols);
            int blockWidth = fm.stringWidth(divider);
            int left = (int) ((paperWidth - blockWidth) / 2);
            int right = left + blockWidth;
            int indent = fm.stringWidth("  ");
            int lineH = 10;
            int y = 15;
 
            URL logoUrl = bPrint.class.getResource("/Images/logo-128.png");
            if (logoUrl != null) {
                Image logo = new ImageIcon(logoUrl).getImage();
                int logoSize = 48;
                g2d.drawImage(logo, left + (blockWidth - logoSize) / 2, y, logoSize, logoSize, null);
                y += logoSize + 10;
            }
 
            g2d.setFont(boldFont);
            drawCentered(g2d, "MEDISCAN POS", left, right, y); y += lineH + 1;
            g2d.setFont(normalFont);
            drawCentered(g2d, "Retail Pharmacy", left, right, y); y += lineH;
            drawCentered(g2d, d.dateTime, left, right, y); y += lineH;
            if (d.reprint) {
                g2d.setFont(boldFont);
                drawCentered(g2d, "*** REPRINT ***", left, right, y); y += lineH;
                g2d.setFont(normalFont);
            }
            g2d.drawString(divider, left, y); y += lineH + 4;
 
            for (Line it : d.lines) {
                String name = it.name;
                while (name.length() > 1 && fm.stringWidth(name) > blockWidth) {
                    name = name.substring(0, name.length() - 1);
                }
                g2d.drawString(name, left, y); y += lineH;
 
                String detail = String.format("%d %s x %.2f", it.quantity, it.unit, it.price);
                String subText = String.format("%.2f", it.subtotal);
                g2d.drawString(detail, left + indent, y);
                g2d.drawString(subText, right - fm.stringWidth(subText), y);
                y += lineH + 2;
            }
 
            g2d.drawString(divider, left, y); y += lineH;
            g2d.setFont(boldFont);
            drawLeftRight(g2d, "TOTAL", String.format("%.2f", d.total), left, right, y); y += lineH;
            g2d.setFont(normalFont);
            drawLeftRight(g2d, "CASH", String.format("%.2f", d.cash), left, right, y); y += lineH;
            drawLeftRight(g2d, "CHANGE", String.format("%.2f", d.change), left, right, y); y += lineH;
            g2d.drawString(divider, left, y); y += lineH + 2;
 
            g2d.drawString(stars, left, y); y += lineH;
            drawCentered(g2d, "THANK YOU, COME AGAIN!", left, right, y); y += lineH;
            g2d.drawString(stars, left, y); y += lineH + 6;
 
            String orderIdText = String.valueOf(d.orderId);
            int barcodeW = Math.min(140, blockWidth);
            int barcodeH = 36;
            BufferedImage barcodeImg = generateBarcodeImage(orderIdText, barcodeW * 3, barcodeH * 3);
            if (barcodeImg != null) {
                g2d.drawImage(barcodeImg, left + (blockWidth - barcodeW) / 2, y, barcodeW, barcodeH, null);
                y += barcodeH + 10;
                drawCentered(g2d, orderIdText, left, right, y); y += lineH;
            }
 
            if (d.reprint) {
                y += 4;
                drawCentered(g2d, "Reprinted: " + d.reprintedAt, left, right, y);
            }
 
            return PAGE_EXISTS;
        }

        private static String repeatChar(char c, int count) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(c);
            }
            return sb.toString();
        }
 
        private static void drawCentered(Graphics2D g2d, String text, int left, int right, int y) {
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, left + ((right - left) - textWidth) / 2, y);
        }
 
        private static void drawLeftRight(Graphics2D g2d, String leftText, String rightText, int left, int right, int y) {
            g2d.drawString(leftText, left, y);
            g2d.drawString(rightText, right - g2d.getFontMetrics().stringWidth(rightText), y);
        }
    }
 
    private static BufferedImage generateBarcodeImage(String data, int width, int height) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
 
    private static void saveBarcodeToFile(String data, String folderPath) {
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, 300, 100);
            Path path = new File(folder, data + ".png").toPath();
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        } catch (Exception e) {
            System.out.println("Barcode save failed: " + e.getMessage());
        }
    }
 
    private static boolean hasColumn(Connection con, String table, String column) throws java.sql.SQLException {
        try (ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
 
    private static double cmToPoints(double cm) {
        return cm * 0.393600787 * 72d;
    }
 
    private static void msg(Component parent, String text) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[print] " + text);
            return;
        }
        JOptionPane.showMessageDialog(parent, text);
    }
}

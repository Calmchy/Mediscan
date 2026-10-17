/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;



/**
 *
 * @author Calmchy
 */
public class bSales {
    private static final int C_NO = 0, C_ID = 1, C_NAME = 2, C_BRAND = 3, C_PRICE = 4, C_QTY = 5, C_UNIT = 6, C_SUBTOTAL = 7;
 
    public static class ProductInfo {
        public int productId;
        public String name, brand, category, unit;
        public double price;
        public boolean requiresPrescription, ageRestricted;
        public int minimumAge;
    }
 
    public static class CartItem {
        public final int productId;
        public final String name, brand, unit;
        public final double price;
        public final int quantity;
 
        public CartItem(int productId, String name, String brand, String unit, double price, int quantity) {
            this.productId = productId;
            this.name = name;
            this.brand = brand;
            this.unit = unit;
            this.price = price;
            this.quantity = quantity;
        }
 
        public double subtotal() {
            return round2(price * quantity);
        }
    }
 
    public static class CheckoutResult {
        public final int orderId;
        public final double total, cash, change;
        public final List<CartItem> items;
 
        CheckoutResult(int orderId, double total, double cash, double change, List<CartItem> items) {
            this.orderId = orderId;
            this.total = total;
            this.cash = cash;
            this.change = change;
            this.items = items;
        }
    }
 
    public static ProductInfo getProduct(int productId) throws SQLException {
        String sql = "SELECT p.product_id, p.name, p.brand, c.category_name, p.selling_price, "
                + "p.requires_prescription, p.is_age_restricted, p.minimum_age, p.unit_type "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE p.product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ProductInfo p = new ProductInfo();
                p.productId = rs.getInt("product_id");
                p.name = rs.getString("name");
                p.brand = rs.getString("brand");
                p.category = rs.getString("category_name");
                p.unit = rs.getString("unit_type");
                p.price = rs.getDouble("selling_price");
                p.requiresPrescription = rs.getBoolean("requires_prescription");
                p.ageRestricted = rs.getBoolean("is_age_restricted");
                p.minimumAge = rs.getInt("minimum_age");
                return p;
            }
        }
    }
 
    public static int getAvailableStock(int productId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM product_stock "
                + "WHERE product_id = ? AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
 
    public static String addToCart(JTable cart, int productId, String name, String brand,
            double price, int quantity, String unit) {
        if (quantity <= 0) {
            return "Quantity must be greater than zero.";
        }
        DefaultTableModel model = (DefaultTableModel) cart.getModel();
 
        int existingRow = -1;
        int inCart = 0;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (Integer.parseInt(model.getValueAt(r, C_ID).toString()) == productId) {
                existingRow = r;
                inCart = Integer.parseInt(model.getValueAt(r, C_QTY).toString());
                break;
            }
        }
 
        try {
            int available = getAvailableStock(productId);
            if (inCart + quantity > available) {
                return "Not enough stock for \"" + name + "\". Available: " + available
                        + (inCart > 0 ? " (" + inCart + " already in cart)." : ".");
            }
        } catch (SQLException e) {
            return "Could not check stock: " + e.getMessage();
        }
 
        if (existingRow >= 0) {
            int newQty = inCart + quantity;
            model.setValueAt(newQty, existingRow, C_QTY);
            model.setValueAt(round2(price * newQty), existingRow, C_SUBTOTAL);
        } else {
            model.addRow(new Object[]{
                model.getRowCount() + 1, productId, name, brand, price, quantity, unit,
                round2(price * quantity)
            });
        }
        return null;
    }
 
    public static void renumberRows(JTable cart) {
        DefaultTableModel model = (DefaultTableModel) cart.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(i + 1, i, C_NO);
        }
    }
 
    public static double getCartTotal(JTable cart) {
        DefaultTableModel model = (DefaultTableModel) cart.getModel();
        double total = 0.0;
        for (int r = 0; r < model.getRowCount(); r++) {
            total += Double.parseDouble(model.getValueAt(r, C_SUBTOTAL).toString());
        }
        return round2(total);
    }
 
    public static Double calcChange(String cashText, double total) {
        try {
            return round2(Double.parseDouble(cashText.trim()) - total);
        } catch (Exception e) {
            return null;
        }
    }
 
    public static List<CartItem> readCart(JTable cart) {
        DefaultTableModel model = (DefaultTableModel) cart.getModel();
        List<CartItem> items = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            Object brand = model.getValueAt(r, C_BRAND);
            items.add(new CartItem(
                    Integer.parseInt(model.getValueAt(r, C_ID).toString()),
                    model.getValueAt(r, C_NAME).toString(),
                    brand == null ? "" : brand.toString(),
                    model.getValueAt(r, C_UNIT).toString(),
                    Double.parseDouble(model.getValueAt(r, C_PRICE).toString()),
                    Integer.parseInt(model.getValueAt(r, C_QTY).toString())
            ));
        }
        return items;
    }
 
    public static class SalesFields {
        public JTable cartTable;
        public JTextField idTF;
        public JTextField nameTF;
        public JTextField categoryTF;
        public JTextField brandTF;
        public JTextField priceTF;
        public JTextField unitTF;
        public JTextField qtyTF;
        public JLabel warningTitleL;
        public JTextArea warningDesTA;
        public JLabel totalL;
        public JTextField cashTF;
        public JTextField changeTF;
    }
 
    private static void msg(Component parent, String text, String title, int type) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[" + title + "] " + text);
            return;
        }
        JOptionPane.showMessageDialog(parent, text, title, type);
    }
 
    private static boolean confirm(Component parent, String text) {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        return JOptionPane.showConfirmDialog(parent, text, "Confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }
 
    public static void showWarning(SalesFields f, String title, String description) {
        f.warningTitleL.setText(title);
        f.warningDesTA.setText(description);
    }
 
    public static void clearScanFields(SalesFields f) {
        f.idTF.setText("");
        f.nameTF.setText("");
        f.categoryTF.setText("");
        f.brandTF.setText("");
        f.priceTF.setText("");
        f.unitTF.setText("");
        f.qtyTF.setText("1");
    }
 
    public static void scanProduct(Component parent, SalesFields f) {
        String idText = f.idTF.getText().trim();
        if (idText.isEmpty()) {
            return;
        }
        int productId;
        try {
            productId = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            msg(parent, "Product ID must be a number.", "", JOptionPane.INFORMATION_MESSAGE);
            clearScanFields(f);
            return;
        }
 
        try {
            ProductInfo p = getProduct(productId);
            if (p == null) {
                msg(parent, "No product found with that ID.", "", JOptionPane.INFORMATION_MESSAGE);
                clearScanFields(f);
                return;
            }
            f.nameTF.setText(p.name);
            f.categoryTF.setText(p.category);
            f.brandTF.setText(p.brand);
            f.priceTF.setText(String.valueOf(p.price));
            f.unitTF.setText(p.unit);
            f.qtyTF.setText("1");
 
            if (getAvailableStock(productId) <= 0) {
                msg(parent, "\"" + p.name + "\" is OUT OF STOCK.", "Out of Stock", JOptionPane.WARNING_MESSAGE);
                clearScanFields(f);
                return;
            }
 
            if (p.requiresPrescription && p.ageRestricted) {
                showWarning(f, "Prescription & Age Verification Required",
                        "This item requires a prescription. Please verify it and check the customer's ID. Minimum age: "
                        + p.minimumAge + "+.");
            } else if (p.requiresPrescription) {
                showWarning(f, "Prescription Required",
                        "This item requires a prescription. Please verify before adding.");
            } else if (p.ageRestricted) {
                showWarning(f, "Age Verification Required",
                        "This item is age-restricted (" + p.minimumAge + "+). Please verify ID before adding.");
            } else {
                showWarning(f, "", "");
            }
        } catch (Exception e) {
            msg(parent, "Error looking up product: " + e.getMessage(), "", JOptionPane.ERROR_MESSAGE);
        }
    }
 
    public static void addItem(Component parent, SalesFields f) {
        String idText = f.idTF.getText().trim();
        String name = f.nameTF.getText().trim();
        String brand = f.brandTF.getText().trim();
        String priceText = f.priceTF.getText().trim();
        String unit = f.unitTF.getText().trim();
        String qtyText = f.qtyTF.getText().trim();
 
        if (idText.isEmpty() || name.isEmpty() || priceText.isEmpty() || qtyText.isEmpty()) {
            msg(parent, "Please scan a valid product first.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int productId, quantity;
        double price;
        try {
            productId = Integer.parseInt(idText);
            price = Double.parseDouble(priceText);
            quantity = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            msg(parent, "Quantity and Price must be valid numbers.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
 
        String error = addToCart(f.cartTable, productId, name, brand, price, quantity, unit);
        if (error != null) {
            msg(parent, error, "Cannot add item", JOptionPane.WARNING_MESSAGE);
            return;
        }
        clearScanFields(f);
        recalcTotal(f);
    }
 
    public static void removeSelectedItem(Component parent, SalesFields f) {
        int row = f.cartTable.getSelectedRow();
        if (row == -1) {
            msg(parent, "Please select an item to remove.", "", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ((DefaultTableModel) f.cartTable.getModel()).removeRow(row);
        renumberRows(f.cartTable);
        recalcTotal(f);
    }
 
    public static void removeAllItems(Component parent, SalesFields f) {
        if (!confirm(parent, "Remove all items from the cart?")) {
            return;
        }
        ((DefaultTableModel) f.cartTable.getModel()).setRowCount(0);
        recalcTotal(f);
    }
 
    public static void recalcTotal(SalesFields f) {
        double total = getCartTotal(f.cartTable);
        f.totalL.setText(String.valueOf(total));
        recalcChange(f);
    }
 
    public static void recalcChange(SalesFields f) {
        Double change = calcChange(f.cashTF.getText(), getCartTotal(f.cartTable));
        f.changeTF.setText(change == null ? "" : String.valueOf(change));
    }
 
    public static CheckoutResult checkoutSale(Component parent, SalesFields f) {
        return checkoutSale(parent, f, null, "walk_in");
    }
 
    public static CheckoutResult checkoutSale(Component parent, SalesFields f, Integer customerId, String orderType) {
        if (f.cashTF.getText().trim().isEmpty()) {
            msg(parent, "ENTER AN AMOUNT.", "", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        List<CartItem> cart = readCart(f.cartTable);
        if (cart.isEmpty()) {
            msg(parent, "Cart is empty.", "", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        double cash;
        try {
            cash = Double.parseDouble(f.cashTF.getText().trim());
        } catch (NumberFormatException e) {
            msg(parent, "Invalid amount entered.", "", JOptionPane.WARNING_MESSAGE);
            return null;
        }
 
        try {
            CheckoutResult result = checkout(cart, cash, customerId, orderType);
            f.changeTF.setText(String.valueOf(result.change));
            return result;
        } catch (Exception e) {
            msg(parent, e.getMessage(), "Sale not completed", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }
 
    public static void clearSale(SalesFields f) {
        ((DefaultTableModel) f.cartTable.getModel()).setRowCount(0);
        f.totalL.setText("");
        f.cashTF.setText("");
        f.changeTF.setText("");
    }
 
    public static CheckoutResult checkout(List<CartItem> cart, double cash,
            Integer customerId, String orderType) throws Exception {
 
        if (cart == null || cart.isEmpty()) {
            throw new Exception("Cart is empty.");
        }
        if (orderType == null || orderType.isEmpty()) {
            orderType = "walk_in";
        }
        if (!orderType.equals("walk_in") && !orderType.equals("pickup")) {
            throw new Exception("Invalid order type: " + orderType);
        }
 
        double total = 0.0;
        for (CartItem item : cart) {
            total += item.subtotal();
        }
        total = round2(total);
        cash = round2(cash);
        if (cash < total) {
            throw new Exception("INSUFFICIENT AMOUNT.");
        }
        double change = round2(cash - total);
 
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
 
            int orderId;
            String orderSql = "INSERT INTO orders (user_id, customer_id, order_type, total_amount, "
                    + "payment_amount, change_amount) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, Session.userId);
                if (customerId == null) {
                    ps.setNull(2, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(2, customerId);
                }
                ps.setString(3, orderType);
                ps.setDouble(4, total);
                ps.setDouble(5, cash);
                ps.setDouble(6, change);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new Exception("Could not create the order.");
                    }
                    orderId = keys.getInt(1);
                }
            }
 
            String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, "
                    + "subtotal, id_verified) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                for (CartItem item : cart) {
                    deductStockFEFO(con, item.productId, item.quantity, item.name);
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.productId);
                    ps.setInt(3, item.quantity);
                    ps.setDouble(4, item.price);
                    ps.setDouble(5, item.subtotal());
                    ps.setBoolean(6, false);
                    ps.executeUpdate();
                }
            }
 
            con.commit();
            return new CheckoutResult(orderId, total, cash, change, cart);
 
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignore) {
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
 
    private static void deductStockFEFO(Connection con, int productId, int quantity, String productName)
            throws Exception {
        String selectSql = "SELECT stock_id, quantity FROM product_stock "
                + "WHERE product_id = ? AND quantity > 0 "
                + "AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE) "
                + "ORDER BY expiry_date IS NULL, expiry_date ASC FOR UPDATE";
 
        int remaining = quantity;
        try (PreparedStatement select = con.prepareStatement(selectSql)) {
            select.setInt(1, productId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next() && remaining > 0) {
                    int stockId = rs.getInt("stock_id");
                    int deduct = Math.min(rs.getInt("quantity"), remaining);
                    remaining -= deduct;
                    try (PreparedStatement update = con.prepareStatement("UPDATE product_stock SET quantity = quantity - ? WHERE stock_id = ?")) {
                        update.setInt(1, deduct);
                        update.setInt(2, stockId);
                        update.executeUpdate();
                    }
                }
            }
        }
        if (remaining > 0) {
            throw new Exception("Not enough stock for \"" + productName + "\" (short by " + remaining + ").");
        }
    }
 
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

package Backend;

import java.awt.Component;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;


public class bStocks {
    public static final String ALL_PRODUCTS = "All Products";
    public static final String ALL_STATUSES = "All Statuses";
    public static final int STATUS_COL = 8;
    private static final java.awt.Color BLOCKED_RED = new java.awt.Color(179, 38, 30);
 
    public static class StockFields {
        public JTable stockTable;
        public JTextField searchTF;
        public JComboBox<String> productCB;
        public JComboBox<String> statusCB;
        private boolean loading;
    }
 
    public static class StockFormFields {
        public JComboBox<String> productCB;
        public JTextField batchNoTF;
        public JTextField costPriceTF;
        public JTextField packsReceivedTF;
        public JTextField quantityTF;
        public com.toedter.calendar.JDateChooser expiryDC;
    }
 
    public static void setupTable(StockFields f) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"STOCK ID", "PRODUCT NAME", "BATCH NO", "COST PRICE", "QUANTITY",
                    "UNIT", "EXPIRY DATE", "DATE RECEIVED", "STATUS"}) {
            @Override public Class<?> getColumnClass(int c) {
                switch (c) {
                    case 0: case 4: return Integer.class;
                    case 3: return Double.class;
                    default: return Object.class;
                }
            }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        f.stockTable.setModel(model);
 
        int[] widths = {70, 220, 110, 90, 90, 90, 110, 120, 100};
        for (int i = 0; i < widths.length; i++) {
            f.stockTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
 
        javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer() {
            private final java.text.DecimalFormat money = new java.text.DecimalFormat("0.##");
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, 
                    boolean selected, boolean focus, int row, int col) {
                Object shown = (value instanceof Double) ? money.format(value) : value;
                Component c = super.getTableCellRendererComponent(t, shown, selected, focus, row, col);
                setHorizontalAlignment(value instanceof Number ? javax.swing.SwingConstants.RIGHT : javax.swing.SwingConstants.LEFT);
                Object status = t.getModel().getValueAt(t.convertRowIndexToModel(row), STATUS_COL);
                boolean blocked = status != null && !"ACTIVE".equals(status.toString());
                if (!selected) {
                    c.setForeground(blocked ? BLOCKED_RED : t.getForeground());
                }
                return c;
            }
        };
        f.stockTable.setDefaultRenderer(Object.class, renderer);
        f.stockTable.setDefaultRenderer(Integer.class, renderer);
        f.stockTable.setDefaultRenderer(Double.class, renderer);
    }
 
    public static void loadFilters(StockFields f) {
        f.loading = true;
        try {
            loadProductFilter(f.productCB);
            loadStatusFilter(f.statusCB);
        } finally {
            f.loading = false;
        }
    }
 
    public static void onFilterChanged(StockFields f) {
        if (!f.loading) {
            setStock(f);
        }
    }
 
    private static void loadProductFilter(JComboBox<String> combo) {
        Object previous = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(ALL_PRODUCTS);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT DISTINCT name FROM products ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        if (previous != null) {
            combo.setSelectedItem(previous);
        }
    }
 
    private static void loadStatusFilter(JComboBox<String> combo) {
        Object previous = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(ALL_STATUSES);
        combo.addItem("Active");
        combo.addItem("On Hold");
        combo.addItem("Recalled");
        if (previous != null) {
            combo.setSelectedItem(previous);
        }
    }
 
    private static String statusLabel(String status) {
        return status == null ? "ACTIVE" : status.toUpperCase().replace('_', ' ');
    }
 
    private static String statusValue(String filter) {
        if (filter == null || filter.equals(ALL_STATUSES)) {
            return null;
        }
        return filter.trim().toLowerCase().replace(' ', '_');
    }
 
    public static void setStock(StockFields f) {
        String keyword = f.searchTF.getText();
        String productName = (String) f.productCB.getSelectedItem();
        String statusValue = statusValue((String) f.statusCB.getSelectedItem());
        boolean filterProduct = productName != null && !productName.equals(ALL_PRODUCTS);
 
        String sql = "SELECT ps.stock_id, p.name, ps.batch_no, ps.cost_price, ps.quantity, "
                + "p.unit_type, ps.expiry_date, ps.date_received, ps.status "
                + "FROM product_stock ps JOIN products p ON ps.product_id = p.product_id "
                + "WHERE (ps.batch_no LIKE ? OR p.name LIKE ?) "
                + (filterProduct ? "AND p.name = ? " : "")
                + (statusValue != null ? "AND ps.status = ? " : "")
                + "ORDER BY ps.stock_id";
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            int i = 1;
            ps.setString(i++, like);
            ps.setString(i++, like);
            if (filterProduct) {
                ps.setString(i++, productName);
            }
            if (statusValue != null) {
                ps.setString(i++, statusValue);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) f.stockTable.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("stock_id"),
                        rs.getString("name"),
                        rs.getString("batch_no"),
                        rs.getDouble("cost_price"),
                        rs.getInt("quantity"),
                        rs.getString("unit_type"),
                        rs.getDate("expiry_date"),
                        rs.getDate("date_received"),
                        statusLabel(rs.getString("status"))
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static int getSelectedStockId(Component parent, StockFields f, String action) {
        int row = f.stockTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(parent, "Please select a stock entry to " + action + ".");
            return -1;
        }
        return Integer.parseInt(f.stockTable.getValueAt(row, 0).toString());
    }
 
    public static void deleteStock(Component parent, StockFields f) {
        int stockId = getSelectedStockId(parent, f, "delete");
        if (stockId == -1) {
            return;
        }
        int x = JOptionPane.showConfirmDialog(parent, "DO YOU WANT TO DELETE THIS STOCK ENTRY?",
                "CONFIRMATION", JOptionPane.YES_NO_OPTION);
        if (x != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM product_stock WHERE stock_id = ?")) {
            ps.setInt(1, stockId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Delete successful");
            setStock(f);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Delete failed: " + e.getMessage());
        }
    }
 
    public static void printStock(StockFields f) {
        try {
            setStock(f);
            MessageFormat header = new MessageFormat("Stock List");
            MessageFormat footer = new MessageFormat("-{0}-");
            f.stockTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void holdRelease(Component parent, StockFields f) {
        String role = Session.role == null ? "" : Session.role.toLowerCase();
        if (!role.equals("admin") && !role.equals("pharmacist")) {
            JOptionPane.showMessageDialog(parent, "Only an admin or pharmacist can hold or release a batch.");
            return;
        }
 
        int row = f.stockTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(parent, "Please select a stock batch first.");
            return;
        }
 
        int stockId = Integer.parseInt(f.stockTable.getValueAt(row, 0).toString());
        String product = f.stockTable.getValueAt(row, 1).toString();
        String batch = String.valueOf(f.stockTable.getValueAt(row, 2));
        String status = f.stockTable.getValueAt(row, STATUS_COL).toString();
 
        try {
            if ("RECALLED".equals(status)) {
                JOptionPane.showMessageDialog(parent, "This batch was recalled and cannot be released.");
                return;
            }
 
            boolean isActive = "ACTIVE".equals(status);
            String why = isActive ? null : getStatusReason(stockId);
 
            String[] options = isActive
                    ? new String[]{"Hold (temporary)", "Recall (permanent)", "Cancel"}
                    : new String[]{"Release", "Recall (permanent)", "Cancel"};
            int choice = JOptionPane.showOptionDialog(parent,
                    product + " (batch " + batch + ") is " + status + "."
                    + (why == null ? "" : "\nHeld because: " + why)
                    + "\n\nWhat do you want to do?",
                    "HOLD / RELEASE BATCH", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
 
            String from = isActive ? "active" : "on_hold";
            String to;
            String reason = null;
 
            if (choice == 0 && !isActive) {
                to = "active";
            } else {
                to = (choice == 0) ? "on_hold" : "recalled";
                String what = (choice == 0) ? "holding" : "recalling";
                reason = JOptionPane.showInputDialog(parent, "Reason for " + what + " " + product + " (batch " + batch + "):", 
                        choice == 0 ? "HOLD BATCH" : "RECALL BATCH", JOptionPane.QUESTION_MESSAGE);
                if (reason == null) {
                    return;
                }
                reason = reason.trim();
                if (reason.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "Please enter a reason.");
                    return;
                }
                if (reason.length() > 255) {
                    JOptionPane.showMessageDialog(parent, "Reason is too long (255 characters max).");
                    return;
                }
                if (choice == 1) {
                    int sure = JOptionPane.showConfirmDialog(parent,
                            "A recalled batch can NEVER be sold or released again.\nRecall batch " + batch + "?",
                            "CONFIRM RECALL", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (sure != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            }
 
            if (setStatus(stockId, from, to, reason) == 0) {
                JOptionPane.showMessageDialog(parent, "This batch was changed by someone else. Refreshing the list.");
            } else {
                JOptionPane.showMessageDialog(parent, "Batch " + batch + " is now " + statusLabel(to) + ".");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Could not change the batch status: " + e.getMessage());
        }
        setStock(f);
    }
 
    public static String getStatusReason(int stockId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT status_reason FROM product_stock WHERE stock_id = ?")) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status_reason") : null;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
 
    public static int setStatus(int stockId, String fromStatus, String toStatus, String reason) throws java.sql.SQLException {
        String sql = "UPDATE product_stock SET status = ?, status_reason = ? WHERE stock_id = ? AND status = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, toStatus);
            if (reason == null) ps.setNull(2, java.sql.Types.VARCHAR); else ps.setString(2, reason);
            ps.setInt(3, stockId);
            ps.setString(4, fromStatus);
            return ps.executeUpdate();
        }
    }
 
    public static void setupForm(StockFormFields f) {
        loadProducts(f.productCB);
        f.packsReceivedTF.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                String selectedProduct = (String) f.productCB.getSelectedItem();
                String packsText = f.packsReceivedTF.getText().trim();
                if (selectedProduct == null || packsText.isEmpty()) {
                    return;
                }
                Integer packSize = getPackSizeByProductName(selectedProduct);
                if (packSize == null) {
                    return;
                }
                try {
                    int packs = Integer.parseInt(packsText);
                    f.quantityTF.setText(String.valueOf(packs * packSize));
                } catch (NumberFormatException e) {
                    
                }
            }
        });
    }
 
    private static void loadProducts(JComboBox<String> combo) {
        combo.removeAllItems();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM products");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(rs.getString("name"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    private static Integer getPackSizeByProductName(String productName) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT pack_size FROM products WHERE name = ?")) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object val = rs.getObject("pack_size");
                    if (val != null) {
                        return rs.getInt("pack_size");
                    }
                }
                return null;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
 
    private static int getProductIdByName(Connection con, String productName) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT product_id FROM products WHERE name = ?")) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("product_id") : -1;
            }
        }
    }
 
    public static void loadStock(StockFormFields f, int stockId) {
        String sql = "SELECT p.name, ps.batch_no, ps.quantity, ps.cost_price, ps.expiry_date "
                + "FROM product_stock ps JOIN products p ON ps.product_id = p.product_id "
                + "WHERE ps.stock_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.batchNoTF.setText(rs.getString("batch_no"));
                    f.quantityTF.setText(String.valueOf(rs.getInt("quantity")));
                    f.costPriceTF.setText(String.valueOf(rs.getDouble("cost_price")));
 
                    Date expiry = rs.getDate("expiry_date");
                    if (expiry != null) {
                        f.expiryDC.setDate(expiry);
                    }
                    f.productCB.setSelectedItem(rs.getString("name"));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void clearForm(StockFormFields f) {
        if (f.productCB.getItemCount() > 0) f.productCB.setSelectedIndex(0);
        f.batchNoTF.setText("");
        f.quantityTF.setText("");
        f.costPriceTF.setText("");
        f.expiryDC.setDate(null);
        f.packsReceivedTF.setText("");
    }
 
    public static boolean saveStock(Component parent, StockFormFields f, int stockId) {
        String selectedProduct = (String) f.productCB.getSelectedItem();
        String batchNo = f.batchNoTF.getText().trim();
        String qtyText = f.quantityTF.getText().trim();
        String costText = f.costPriceTF.getText().trim();
 
        if (selectedProduct == null || batchNo.isEmpty() || qtyText.isEmpty()
                || costText.isEmpty() || f.expiryDC.getDate() == null) {
            JOptionPane.showMessageDialog(parent, "Please fill in all fields.");
            return false;
        }
 
        double costPrice;
        try {
            costPrice = Double.parseDouble(costText);
            if (costPrice < 0) {
                JOptionPane.showMessageDialog(parent, "Cost Price cannot be negative.");
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent, "Cost Price must be a valid number.");
            return false;
        }
 
        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(parent, "Quantity must be greater than zero.");
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent, "Quantity must be a whole number.");
            return false;
        }
 
        Date expiryDate = new Date(f.expiryDC.getDate().getTime());
 
        try (Connection con = DBConnection.getConnection()) {
            int productId = getProductIdByName(con, selectedProduct);
            if (productId == -1) {
                JOptionPane.showMessageDialog(parent, "Selected product could not be found.");
                return false;
            }
 
            if (stockId == -1) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO product_stock (product_id, batch_no, quantity, cost_price, expiry_date) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, productId);
                    ps.setString(2, batchNo);
                    ps.setInt(3, quantity);
                    ps.setDouble(4, costPrice);
                    ps.setDate(5, expiryDate);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(parent, "Stock added successfully!");
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE product_stock SET product_id=?, batch_no=?, quantity=?, cost_price=?, expiry_date=? "
                        + "WHERE stock_id=?")) {
                    ps.setInt(1, productId);
                    ps.setString(2, batchNo);
                    ps.setInt(3, quantity);
                    ps.setDouble(4, costPrice);
                    ps.setDate(5, expiryDate);
                    ps.setInt(6, stockId);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(parent, "Stock updated successfully!");
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
            return false;
        }
    }
}

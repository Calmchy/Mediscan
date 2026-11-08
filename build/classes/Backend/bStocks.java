package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

public class bStocks {
    public static final String ALL_PRODUCTS = "All Products";

    public void loadProductFilter(javax.swing.JComboBox<String> combo) {
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
    
    public void setStock(javax.swing.JTable table) {
        setStock(table, "", ALL_PRODUCTS);
    }

    public void setStock(javax.swing.JTable table, String keyword) {
        setStock(table, keyword, ALL_PRODUCTS);
    }
    
    private static String statusLabel(String status) {
        return status == null ? "ACTIVE" : status.toUpperCase().replace('_', ' ');
    }

    public void setStock(javax.swing.JTable table, String keyword, String productName) {
        boolean filterProduct = productName != null && !productName.equals(ALL_PRODUCTS);

        String sql = "SELECT ps.stock_id, p.name, ps.batch_no, ps.cost_price, ps.quantity, "
                + "p.unit_type, ps.expiry_date, ps.date_received, ps.status "
                + "FROM product_stock ps JOIN products p ON ps.product_id = p.product_id "
                + "WHERE (ps.batch_no LIKE ? OR p.name LIKE ?) "
                + (filterProduct ? "AND p.name = ? " : "")
                + "ORDER BY ps.stock_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + keyword.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            if (filterProduct) {
                ps.setString(3, productName);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
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
    
    public String getStatusReason(int stockId) {
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
    
    /**
    * Moves a batch from one status to another ('active', 'on_hold', 'recalled').
    * The WHERE includes the status we expect, so if someone else already changed it nothing is updated.
    * @return rows updated (1 = done, 0 = batch changed or was removed in the meantime)
    */
   public int setStatus(int stockId, String fromStatus, String toStatus, String reason) throws java.sql.SQLException {
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
}

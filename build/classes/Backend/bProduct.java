package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

public class bProduct {
    public static final String ALL_CATEGORIES = "All Categories";
    
    public void loadCategoryFilter(javax.swing.JComboBox<String> combo) {
        Object previous = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(ALL_CATEGORIES);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT category_name FROM categories ORDER BY category_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(rs.getString("category_name"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        if (previous != null) {
            combo.setSelectedItem(previous); // does nothing if it no longer exists -> stays on "All"
        }
    }
    
    public void setProd(javax.swing.JTable table) {
        setProd(table, "", ALL_CATEGORIES);
    }

    public void setProd(javax.swing.JTable table, String keyword) {
        setProd(table, keyword, ALL_CATEGORIES);
    }

    public void setProd(javax.swing.JTable table, String keyword, String category) {
        boolean filterCategory = category != null && !category.equals(ALL_CATEGORIES);

        String sql = "SELECT p.product_id, p.name, p.brand, c.category_name, p.cost_price, "
                + "p.selling_price, p.requires_prescription, p.minimum_age, p.unit_type, p.pack_size "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE (p.name LIKE ? OR p.brand LIKE ? OR c.category_name LIKE ? "
                + "OR CAST(p.product_id AS CHAR) LIKE ?) "
                + (filterCategory ? "AND c.category_name = ? " : "")
                + "ORDER BY p.product_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + keyword.trim() + "%";
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            if (filterCategory) {
                ps.setString(5, category);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    Object packSizeRaw = rs.getObject("pack_size");
                    Integer packSize = (packSizeRaw == null) ? null : rs.getInt("pack_size");

                    model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("category_name"),
                        rs.getDouble("cost_price"),
                        rs.getDouble("selling_price"),
                        rs.getBoolean("requires_prescription"),
                        rs.getInt("minimum_age"),
                        rs.getString("unit_type"),
                        packSize
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

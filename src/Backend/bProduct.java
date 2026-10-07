package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author manatad
 */
public class bProduct {
    
    public void setProd(javax.swing.JTable table) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT p.product_id, p.name, p.brand, c.category_name, p.cost_price, "
                    + "p.selling_price, p.requires_prescription, p.minimum_age, p.unit_type, p.pack_size "
                    + "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
 
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
 
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

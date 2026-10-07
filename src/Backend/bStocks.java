package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author manatad
 */
public class bStocks {
    
    public void setStock(javax.swing.JTable table) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT ps.stock_id, p.name, ps.batch_no, ps.quantity, p.unit_type, "
                    + "ps.expiry_date, ps.date_received "
                    + "FROM product_stock ps JOIN products p ON ps.product_id = p.product_id";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
 
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
 
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("stock_id"),
                    rs.getString("name"),
                    rs.getString("batch_no"),
                    rs.getInt("quantity"),
                    rs.getString("unit_type"),
                    rs.getDate("expiry_date"),
                    rs.getDate("date_received")
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

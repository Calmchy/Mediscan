package Backend;

import Call.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author chyril
 */
public class bCategory {
    // function para sa Category dialog
    public static void loadCategory(javax.swing.JTextField categoryNameTF, int categoryId) {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT category_name FROM categories WHERE category_id = ?");
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();
 
            if (rs.next()) {
                categoryNameTF.setText(rs.getString("category_name"));
            }
 
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static void addCategory(java.awt.Frame parent, javax.swing.JTable categoryTable) {
        Category cat = new Category(parent, true);
        cat.setVisible(true);
        setCategory(categoryTable);
    }
    
    // para maka add ngan update sin categories
    public static boolean addCategory(java.awt.Frame parent, javax.swing.JTextField categoryNameTF, int categoryId) {
        String name = categoryNameTF.getText().trim();
 
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Category name cannot be empty.");
            return false;
        }
 
        try {
            Connection con = DBConnection.getConnection();
 
            if (categoryId == -1) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO categories (category_name) VALUES (?)");
                ps.setString(1, name);
                ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(parent, "Category added successfully!");
            } else {
                PreparedStatement ps = con.prepareStatement(
                    "UPDATE categories SET category_name = ? WHERE category_id = ?");
                ps.setString(1, name);
                ps.setInt(2, categoryId);
                ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(parent, "Category updated successfully!");
            }
 
            con.close();
            return true;
 
        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
            JOptionPane.showMessageDialog(parent, "That category name already exists.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }
    
    public static void editCategory(java.awt.Frame parent, javax.swing.JTable categoryTable) {
        int i = categoryTable.getSelectedRow();
        if (i == -1) {
            JOptionPane.showMessageDialog(null, "Please select a category to edit.");
            return;
        }
        int categoryId = Integer.parseInt(categoryTable.getValueAt(i, 0).toString());
 
        Category cat = new Category(parent, true, categoryId);
        cat.setVisible(true);
        setCategory(categoryTable);
    }
    
    public static void deleteCategory(javax.swing.JTable categoryTable) {
        int i = categoryTable.getSelectedRow();
        if (i == -1) {
            JOptionPane.showMessageDialog(null, "Please select a category to delete.");
            return;
        }
 
        int x = JOptionPane.showConfirmDialog(null, "DO YOU WANT TO DELETE THIS CATEGORY?", "CONFIRMATION", 0);
        if (x != 0) {
            return;
        }
 
        try {
            int categoryId = Integer.parseInt(categoryTable.getValueAt(i, 0).toString());
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM categories WHERE category_id = ?");
            ps.setInt(1, categoryId);
            ps.executeUpdate();
            ps.close();
            con.close();
 
            JOptionPane.showMessageDialog(null, "Delete successful");
            setCategory(categoryTable);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Delete failed: " + e.getMessage());
        }
    }
    
    public static void printCategory(javax.swing.JTable categoryTable) {
        try {
            setCategory(categoryTable);
            MessageFormat header = new MessageFormat("Category List");
            MessageFormat footer = new MessageFormat("-{0}-");
            categoryTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // para pag butang og data sa category table
    public static void setCategory(javax.swing.JTable table) {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT category_id, category_name FROM categories");
            ResultSet rs = ps.executeQuery();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("category_id"),
                    rs.getString("category_name"),
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

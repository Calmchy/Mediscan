package Backend;

import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;


public class bCategory {
 
    public static class CategoryFields {
        public JTable categoryTable;
        public JTextField searchTF;
    }
 
    public static class CategoryFormFields {
        public JTextField nameTF;
    }
 
    public static void setCategory(CategoryFields f) {
        String keyword = f.searchTF.getText();
        String sql = "SELECT category_id, category_name FROM categories "
                   + "WHERE category_name LIKE ? ORDER BY category_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + (keyword == null ? "" : keyword.trim()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) f.categoryTable.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("category_id"),
                        rs.getString("category_name")
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static int getSelectedCategoryId(Component parent, CategoryFields f, String action) {
        int row = f.categoryTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(parent, "Please select a category to " + action + ".");
            return -1;
        }
        return Integer.parseInt(f.categoryTable.getValueAt(row, 0).toString());
    }
 
    public static void deleteCategory(Component parent, CategoryFields f) {
        int categoryId = getSelectedCategoryId(parent, f, "delete");
        if (categoryId == -1) {
            return;
        }
        int x = JOptionPane.showConfirmDialog(parent, "DO YOU WANT TO DELETE THIS CATEGORY?",
                "CONFIRMATION", JOptionPane.YES_NO_OPTION);
        if (x != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM categories WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Delete successful");
            setCategory(f);
        } catch (SQLIntegrityConstraintViolationException fk) {
            JOptionPane.showMessageDialog(parent, "This category is still used by products and can't be deleted.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Delete failed: " + e.getMessage());
        }
    }
 
    public static void printCategory(CategoryFields f) {
        try {
            setCategory(f);
            MessageFormat header = new MessageFormat("Category List");
            MessageFormat footer = new MessageFormat("-{0}-");
            f.categoryTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void loadCategory(CategoryFormFields f, int categoryId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT category_name FROM categories WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.nameTF.setText(rs.getString("category_name"));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void clearForm(CategoryFormFields f) {
        f.nameTF.setText("");
    }
 
    public static boolean saveCategory(Component parent, CategoryFormFields f, int categoryId) {
        String name = f.nameTF.getText().trim();
 
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Category name cannot be empty.");
            return false;
        }
 
        try (Connection con = DBConnection.getConnection()) {
            if (categoryId == -1) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO categories (category_name) VALUES (?)")) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(parent, "Category added successfully!");
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE categories SET category_name = ? WHERE category_id = ?")) {
                    ps.setString(1, name);
                    ps.setInt(2, categoryId);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(parent, "Category updated successfully!");
            }
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            JOptionPane.showMessageDialog(parent, "That category name already exists.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }
}

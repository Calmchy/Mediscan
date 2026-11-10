package Backend;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.text.MessageFormat;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
 
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;


public class bProduct {
        public static final String ALL_CATEGORIES = "All Categories";
    private static final String BARCODE_FOLDER = System.getProperty("user.home") + "/MediScan/Product/";
 
    public static class ProductFields {
        public JTable productsTable;
        public JTextField searchTF;
        public JComboBox<String> categoryCB;
        private boolean loading;
    }
 
    public static class ProductFormFields {
        public JTextField nameTF;
        public JTextField brandTF;
        public JComboBox<String> categoryCB;
        public JTextField sellingPriceTF;
        public JCheckBox requiresRxCB;
        public JCheckBox ageRestrictedCB;
        public JTextField minAgeTF;
        public JComboBox<String> unitTypeCB;
        public JTextField packSizeTF;
    }
    
    public static void loadCategoryFilter(ProductFields f) {
        f.loading = true;
        try {
            Object previous = f.categoryCB.getSelectedItem();
            f.categoryCB.removeAllItems();
            f.categoryCB.addItem(ALL_CATEGORIES);
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT category_name FROM categories ORDER BY category_name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    f.categoryCB.addItem(rs.getString("category_name"));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            if (previous != null) {
                f.categoryCB.setSelectedItem(previous);
            }
        } finally {
            f.loading = false;
        }
    }
 
    public static void onFilterChanged(ProductFields f) {
        if (!f.loading) {
            setProducts(f);
        }
    }
 
    public static void setProducts(ProductFields f) {
        String keyword = f.searchTF.getText();
        String category = (String) f.categoryCB.getSelectedItem();
        boolean filterCategory = category != null && !category.equals(ALL_CATEGORIES);
 
        String sql = "SELECT p.product_id, p.name, p.brand, c.category_name, "
                + "p.selling_price, p.requires_prescription, p.minimum_age, p.unit_type, p.pack_size "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE (p.name LIKE ? OR p.brand LIKE ? OR c.category_name LIKE ? "
                + "OR CAST(p.product_id AS CHAR) LIKE ?) "
                + (filterCategory ? "AND c.category_name = ? " : "")
                + "ORDER BY p.product_id";
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            if (filterCategory) {
                ps.setString(5, category);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) f.productsTable.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    Object packSizeRaw = rs.getObject("pack_size");
                    Integer packSize = (packSizeRaw == null) ? null : rs.getInt("pack_size");
 
                    model.addRow(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getString("category_name"),
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
 
    public static int getSelectedProductId(Component parent, ProductFields f, String action) {
        int row = f.productsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(parent, "Please select a product to " + action + ".");
            return -1;
        }
        return Integer.parseInt(f.productsTable.getValueAt(row, 0).toString());
    }
 
    public static void deleteProduct(Component parent, ProductFields f) {
        int productId = getSelectedProductId(parent, f, "delete");
        if (productId == -1) {
            return;
        }
        int x = JOptionPane.showConfirmDialog(parent, "DO YOU WANT TO DELETE THIS PRODUCT?",
                "CONFIRMATION", JOptionPane.YES_NO_OPTION);
        if (x != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM products WHERE product_id = ?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Delete successful");
            setProducts(f);
        } catch (SQLIntegrityConstraintViolationException fk) {
            JOptionPane.showMessageDialog(parent, "Cannot delete: this product has related stock or order records.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Delete failed: " + e.getMessage());
        }
    }
 
    public static void printProducts(ProductFields f) {
        try {
            setProducts(f);
            MessageFormat header = new MessageFormat("Product List");
            MessageFormat footer = new MessageFormat("-{0}-");
            f.productsTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static String generateBarcode(String productId) {
        try {
            File folder = new File(BARCODE_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(productId, BarcodeFormat.CODE_128, 300, 100);
 
            Path path = new File(folder, productId + ".png").toPath();
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
 
            System.out.println("Barcode saved: " + path);
            return path.toString();
        } catch (Exception e) {
            System.out.println("Barcode generation failed: " + e.getMessage());
            return null;
        }
    }
 
    public static void generateMissingBarcodes(Component parent) {
        int created = 0, skipped = 0;
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
             ResultSet rs = ps.executeQuery()) {
 
            while (rs.next()) {
                String id = String.valueOf(rs.getInt("product_id"));
                if (new File(BARCODE_FOLDER, id + ".png").exists()) {
                    skipped++;
                    continue;
                }
                generateBarcode(id);
                created++;
            }
 
            JOptionPane.showMessageDialog(parent,
                    "Barcodes created: " + created + "\nAlready existed: " + skipped
                    + "\n\nFolder: " + BARCODE_FOLDER);
 
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error generating barcodes: " + e.getMessage());
        }
    }
 
    public static void setupForm(ProductFormFields f) {
        loadCategories(f.categoryCB);
        setupAgeRestrictedToggle(f);
        setupUnitTypeToggle(f);
    }
 
    private static void loadCategories(JComboBox<String> combo) {
        combo.removeAllItems();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT category_name FROM categories");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(rs.getString("category_name"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    private static void setupAgeRestrictedToggle(ProductFormFields f) {
        f.minAgeTF.setEnabled(f.ageRestrictedCB.isSelected());
        f.ageRestrictedCB.addItemListener(e -> {
            boolean checked = f.ageRestrictedCB.isSelected();
            f.minAgeTF.setEnabled(checked);
            if (!checked) {
                f.minAgeTF.setText("");
            }
        });
    }
    
    private static boolean usesPackSize(String unitType) {
        return "tablet".equals(unitType) || "capsule".equals(unitType);
    }
 
    private static void setupUnitTypeToggle(ProductFormFields f) {
        if (f.unitTypeCB.getItemCount() == 0) {
            f.unitTypeCB.addItem("tablet");
            f.unitTypeCB.addItem("capsule");
            f.unitTypeCB.addItem("syrup");
            f.unitTypeCB.addItem("ml");
            f.unitTypeCB.addItem("box");
            f.unitTypeCB.addItem("piece");
        }
 
        f.packSizeTF.setEnabled(usesPackSize((String) f.unitTypeCB.getSelectedItem()));
 
        f.unitTypeCB.addItemListener(e -> {
            boolean packed = usesPackSize((String) f.unitTypeCB.getSelectedItem());
            f.packSizeTF.setEnabled(packed);
            if (!packed) {
                f.packSizeTF.setText("");
            }
        });
    }
 
    public static void loadProduct(ProductFormFields f, int productId) {
        String sql = "SELECT p.name, p.brand, c.category_name, p.selling_price, "
                + "p.requires_prescription, p.is_age_restricted, p.minimum_age, "
                + "p.unit_type, p.pack_size "
                + "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE p.product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.nameTF.setText(rs.getString("name"));
                    f.brandTF.setText(rs.getString("brand"));
                    f.sellingPriceTF.setText(String.valueOf(rs.getDouble("selling_price")));
                    f.requiresRxCB.setSelected(rs.getBoolean("requires_prescription"));
                    f.ageRestrictedCB.setSelected(rs.getBoolean("is_age_restricted"));
                    f.unitTypeCB.setSelectedItem(rs.getString("unit_type"));
 
                    Object packSize = rs.getObject("pack_size");
                    if (packSize != null) {
                        f.packSizeTF.setText(packSize.toString());
                        f.packSizeTF.setEnabled(true);
                    }
 
                    Object minAge = rs.getObject("minimum_age");
                    if (minAge != null) {
                        f.minAgeTF.setText(minAge.toString());
                        f.minAgeTF.setEnabled(true);
                    }
 
                    f.categoryCB.setSelectedItem(rs.getString("category_name"));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void clearForm(ProductFormFields f) {
        f.nameTF.setText("");
        f.brandTF.setText("");
        if (f.categoryCB.getItemCount() > 0) f.categoryCB.setSelectedIndex(0);
        f.sellingPriceTF.setText("");
        f.requiresRxCB.setSelected(false);
        f.ageRestrictedCB.setSelected(false);
        f.minAgeTF.setText("");
        f.minAgeTF.setEnabled(false);
        if (f.unitTypeCB.getItemCount() > 0) f.unitTypeCB.setSelectedIndex(0);
        f.packSizeTF.setText("");
    }
 
    public static boolean saveProduct(Component parent, ProductFormFields f, int productId) {
        String name = f.nameTF.getText().trim();
        String brand = f.brandTF.getText().trim();
        String sellText = f.sellingPriceTF.getText().trim();
        boolean requiresRx = f.requiresRxCB.isSelected();
        boolean ageRestricted = f.ageRestrictedCB.isSelected();
        String minAgeText = f.minAgeTF.getText().trim();
        String selectedCategory = (String) f.categoryCB.getSelectedItem();
        String unitType = (String) f.unitTypeCB.getSelectedItem();
        String packSizeText = f.packSizeTF.getText().trim();
 
        if (name.isEmpty() || selectedCategory == null || sellText.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Please fill in all required fields.");
            return false;
        }
 
        Integer packSize = null;
        if (usesPackSize(unitType) && !packSizeText.isEmpty()) {
            try {
                packSize = Integer.parseInt(packSizeText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(parent, "Pack Size must be a whole number.");
                return false;
            }
        }
 
        double sellingPrice;
        try {
            sellingPrice = Double.parseDouble(sellText);
            if (sellingPrice < 0) {
                JOptionPane.showMessageDialog(parent, "Selling Price cannot be negative.");
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent, "Selling Price must be a valid number.");
            return false;
        }
 
        Integer minimumAge = null;
        if (ageRestricted) {
            if (minAgeText.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Please enter a minimum age for age-restricted products.");
                return false;
            }
            try {
                minimumAge = Integer.parseInt(minAgeText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(parent, "Minimum Age must be a whole number.");
                return false;
            }
        }
 
        try (Connection con = DBConnection.getConnection()) {
            int categoryId = getCategoryIdByName(con, selectedCategory);
            if (categoryId == -1) {
                JOptionPane.showMessageDialog(parent, "Selected category could not be found.");
                return false;
            }
 
            con.setAutoCommit(false);
            try {
                if (productId == -1) {
                    int newId = insertProduct(con, name, brand, categoryId, sellingPrice,
                            requiresRx, ageRestricted, minimumAge, unitType, packSize);
                    if (newId != -1) {
                        logPriceChange(con, newId, null, sellingPrice);
                    }
                    con.commit();
 
                    String message = "Product added successfully!";
                    if (newId != -1) {
                        String barcodePath = generateBarcode(String.valueOf(newId));
                        message += "\n\nProduct ID: " + newId;
                        message += (barcodePath != null)
                                ? "\nBarcode saved to:\n" + barcodePath
                                : "\nBarcode could not be generated.";
                    }
                    JOptionPane.showMessageDialog(parent, message);
                } else {
                    Double oldPrice = getCurrentSellingPrice(con, productId);
                    updateProduct(con, productId, name, brand, categoryId, sellingPrice,
                            requiresRx, ageRestricted, minimumAge, unitType, packSize);
                    if (oldPrice == null || Double.compare(oldPrice, sellingPrice) != 0) {
                        logPriceChange(con, productId, oldPrice, sellingPrice);
                    }
                    con.commit();
                    JOptionPane.showMessageDialog(parent, "Product updated successfully!");
                }
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
            return false;
        }
    }
 
    private static int getCategoryIdByName(Connection con, String categoryName) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT category_id FROM categories WHERE category_name = ?")) {
            ps.setString(1, categoryName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("category_id") : -1;
            }
        }
    }
 
    private static int insertProduct(Connection con, String name, String brand, int categoryId, double sellingPrice,
            boolean requiresRx, boolean ageRestricted, Integer minimumAge, String unitType, Integer packSize) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO products (name, brand, category_id, selling_price, "
                + "requires_prescription, is_age_restricted, minimum_age, unit_type, pack_size) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, brand.isEmpty() ? null : brand);
            ps.setInt(3, categoryId);
            ps.setDouble(4, sellingPrice);
            ps.setBoolean(5, requiresRx);
            ps.setBoolean(6, ageRestricted);
            if (minimumAge != null) ps.setInt(7, minimumAge); else ps.setNull(7, java.sql.Types.INTEGER);
            ps.setString(8, unitType);
            if (packSize != null) ps.setInt(9, packSize); else ps.setNull(9, java.sql.Types.INTEGER);
            ps.executeUpdate();
 
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }
 
    private static void updateProduct(Connection con, int productId, String name, String brand, int categoryId,
            double sellingPrice, boolean requiresRx, boolean ageRestricted, Integer minimumAge,
            String unitType, Integer packSize) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("UPDATE products SET name=?, brand=?, category_id=?, selling_price=?, "
                + "requires_prescription=?, is_age_restricted=?, minimum_age=?, unit_type=?, pack_size=? "
                + "WHERE product_id=?")) {
            ps.setString(1, name);
            ps.setString(2, brand.isEmpty() ? null : brand);
            ps.setInt(3, categoryId);
            ps.setDouble(4, sellingPrice);
            ps.setBoolean(5, requiresRx);
            ps.setBoolean(6, ageRestricted);
            if (minimumAge != null) ps.setInt(7, minimumAge); else ps.setNull(7, java.sql.Types.INTEGER);
            ps.setString(8, unitType);
            if (packSize != null) ps.setInt(9, packSize); else ps.setNull(9, java.sql.Types.INTEGER);
            ps.setInt(10, productId);
            ps.executeUpdate();
        }
    }
 
    private static Double getCurrentSellingPrice(Connection con, int id) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT selling_price FROM products WHERE product_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("selling_price") : null;
            }
        }
    }
 
    private static void logPriceChange(Connection con, int id, Double oldPrice, double newPrice) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO product_price_history (product_id, old_price, new_price, changed_by) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, id);
            if (oldPrice != null) ps.setDouble(2, oldPrice); else ps.setNull(2, java.sql.Types.DECIMAL);
            ps.setDouble(3, newPrice);
            if (Session.userId > 0) ps.setInt(4, Session.userId); else ps.setNull(4, java.sql.Types.INTEGER);
            ps.executeUpdate();
        }
    }
}

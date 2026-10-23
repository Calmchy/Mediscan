/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Calmchy
 */
public class bCustomer {
    private static final int C_ID = 0, C_FULLNAME = 1;
 
    public static class CustomerFields {
        public JTable customerTable;
        public JTextField searchTF;
        public JLabel countL;
    }
 
    public static class AddCustomerFields {
        public JTextField firstNameTF;
        public JTextField lastNameTF;
        public JTextField middleInitialTF;
        public com.toedter.calendar.JDateChooser dateOfBirthDC;
        public JTextField phoneTF;
        public JTextField emailTF;
    }
 
    public static boolean isCashier() {
        return Session.role != null && Session.role.equalsIgnoreCase("cashier");
    }
 
    public static void applyRolePermissions(JButton deleteBtn) {
        boolean allowed = !isCashier();
        deleteBtn.setEnabled(allowed);
        deleteBtn.setToolTipText(allowed ? null : "Cashiers can't delete customers");
    }
 
    private static String fullNameSql(String alias) {
        String mi = "TRIM(TRAILING '.' FROM TRIM(" + alias + ".middle_initial))";
        return "CONCAT_WS(' ', " + alias + ".first_name, "
                + "IF(" + alias + ".middle_initial IS NULL OR " + mi + " = '', NULL, CONCAT(" + mi + ", '.')), "
                + alias + ".last_name)";
    }
    
    public static String getFullName(int customerId) throws java.sql.SQLException {
        String sql = "SELECT " + fullNameSql("c") + " AS full_name FROM customers c WHERE c.customer_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("full_name") : null;
            }
        }
    }
 
    public static void setCustomers(CustomerFields f) {
        String keyword = f.searchTF.getText().trim();
        String sql = "SELECT c.customer_id, " + fullNameSql("c") + " AS full_name, c.date_of_birth, "
                + "TIMESTAMPDIFF(YEAR, c.date_of_birth, CURDATE()) AS age, c.phone_number, c.email, "
                + "CONCAT_WS(' ', u.first_name, u.last_name) AS added_by, c.created_at "
                + "FROM customers c LEFT JOIN users u ON c.created_by = u.user_id "
                + "WHERE (" + fullNameSql("c") + " LIKE ? OR CONCAT_WS(' ', c.first_name, c.last_name) LIKE ? "
                + "OR c.phone_number LIKE ? OR c.email LIKE ?) "
                + "ORDER BY c.customer_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) f.customerTable.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    java.sql.Date dob = rs.getDate("date_of_birth");
                    String addedBy = rs.getString("added_by");
                    model.addRow(new Object[]{
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        dob == null ? "-" : dob.toString(),
                        dob == null ? (Object) "-" : (Object) rs.getInt("age"),
                        orDash(rs.getString("phone_number")),
                        orDash(rs.getString("email")),
                        (addedBy == null || addedBy.trim().isEmpty()) ? "-" : addedBy,
                        rs.getTimestamp("created_at").toLocalDateTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        updateCount(f);
    }
 
    public static void updateCount(CustomerFields f) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                f.countL.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void refresh(CustomerFields f) {
        f.searchTF.setText("");
        setCustomers(f);
    }
 
    public static int getSelectedCustomerId(JTable customerTable) {
        int row = customerTable.getSelectedRow();
        if (row == -1) {
            return -1;
        }
        return Integer.parseInt(customerTable.getValueAt(row, C_ID).toString());
    }
 
    public static void deleteCustomer(java.awt.Component parent, CustomerFields f) {
        if (isCashier()) {
            JOptionPane.showMessageDialog(parent, "Cashiers can't delete customers.");
            return;
        }
        int row = f.customerTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(parent, "Please select a customer to delete.");
            return;
        }
        int customerId = getSelectedCustomerId(f.customerTable);
        String name = f.customerTable.getValueAt(row, C_FULLNAME).toString();
        int x = JOptionPane.showConfirmDialog(parent,
                "DO YOU WANT TO DELETE CUSTOMER \"" + name + "\"?", "CONFIRMATION", JOptionPane.YES_NO_OPTION);
        if (x != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Delete successful");
            setCustomers(f);
        } catch (SQLIntegrityConstraintViolationException fk) {
            JOptionPane.showMessageDialog(parent,
                    "This customer already has transactions or prescriptions and can't be deleted.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Delete failed: " + e.getMessage());
        }
    }
 
    public static void printCustomers(CustomerFields f) {
        try {
            MessageFormat header = new MessageFormat("Customer List");
            MessageFormat footer = new MessageFormat("-{0}-");
            f.customerTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void clearAddFields(AddCustomerFields f) {
        f.firstNameTF.setText("");
        f.lastNameTF.setText("");
        f.middleInitialTF.setText("");
        f.dateOfBirthDC.setDate(null);
        f.phoneTF.setText("");
        f.emailTF.setText("");
        f.firstNameTF.requestFocus();
    }
 
    public static String checkCustomerFields(AddCustomerFields f) {
        String first = f.firstNameTF.getText().trim();
        String last = f.lastNameTF.getText().trim();
        String mi = bAccount.normalizeMiddleInitial(f.middleInitialTF.getText());
        String phone = normalizePhone(f.phoneTF.getText());
        String email = f.emailTF.getText().trim();
        Date dob = f.dateOfBirthDC.getDate();
 
        if (first.isEmpty() || last.isEmpty()) {
            return "Please enter the first name and last name.";
        }
        if (!mi.isEmpty() && !Character.isLetter(mi.charAt(0))) {
            return "Middle initial must be a letter.";
        }
        if (dob != null && dob.after(new Date())) {
            return "Date of birth can't be in the future.";
        }
        if (!phone.isEmpty() && !phone.matches("\\+?\\d{7,15}")) {
            return "Phone number is not valid (digits only, 7-15 numbers).";
        }
        if (!email.isEmpty() && !email.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) {
            return "Email address is not valid.";
        }
        return null;
    }
 
    public static boolean addCustomer(java.awt.Component parent, AddCustomerFields f) {
        String error = checkCustomerFields(f);
        if (error != null) {
            JOptionPane.showMessageDialog(parent, error);
            return false;
        }
        String duplicate = findDuplicate(f, -1);
        if (duplicate != null) {
            JOptionPane.showMessageDialog(parent, duplicate);
            return false;
        }
 
        String sql = "INSERT INTO customers (first_name, last_name, middle_initial, date_of_birth, "
                + "email, phone_number, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindFields(ps, f);
            ps.setInt(7, Session.userId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Customer added successfully!");
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }

    public static void loadCustomer(AddCustomerFields f, int customerId) {
        String sql = "SELECT first_name, last_name, middle_initial, date_of_birth, email, phone_number "
                + "FROM customers WHERE customer_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.firstNameTF.setText(rs.getString("first_name"));
                    f.lastNameTF.setText(rs.getString("last_name"));
                    f.middleInitialTF.setText(bAccount.normalizeMiddleInitial(rs.getString("middle_initial")));
                    f.dateOfBirthDC.setDate(rs.getDate("date_of_birth"));
                    f.phoneTF.setText(rs.getString("phone_number") == null ? "" : rs.getString("phone_number"));
                    f.emailTF.setText(rs.getString("email") == null ? "" : rs.getString("email"));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static boolean updateCustomer(java.awt.Component parent, AddCustomerFields f, int customerId) {
        String error = checkCustomerFields(f);
        if (error != null) {
            JOptionPane.showMessageDialog(parent, error);
            return false;
        }
        String duplicate = findDuplicate(f, customerId);
        if (duplicate != null) {
            JOptionPane.showMessageDialog(parent, duplicate);
            return false;
        }
 
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, middle_initial = ?, "
                + "date_of_birth = ?, email = ?, phone_number = ? WHERE customer_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindFields(ps, f);
            ps.setInt(7, customerId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Customer updated successfully!");
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }
 
    private static void bindFields(PreparedStatement ps, AddCustomerFields f) throws java.sql.SQLException {
        String mi = bAccount.normalizeMiddleInitial(f.middleInitialTF.getText());
        Date dob = f.dateOfBirthDC.getDate();
        String email = f.emailTF.getText().trim();
        String phone = normalizePhone(f.phoneTF.getText());
 
        ps.setString(1, f.firstNameTF.getText().trim());
        ps.setString(2, f.lastNameTF.getText().trim());
        if (mi.isEmpty()) {
            ps.setNull(3, java.sql.Types.CHAR);
        } else {
            ps.setString(3, mi);
        }
        if (dob == null) {
            ps.setNull(4, java.sql.Types.DATE);
        } else {
            ps.setDate(4, new java.sql.Date(dob.getTime()));
        }
        if (email.isEmpty()) {
            ps.setNull(5, java.sql.Types.VARCHAR);
        } else {
            ps.setString(5, email);
        }
        if (phone.isEmpty()) {
            ps.setNull(6, java.sql.Types.VARCHAR);
        } else {
            ps.setString(6, phone);
        }
    }
 
    public static String normalizePhone(String text) {
        return text == null ? "" : text.replaceAll("[\\s\\-()]", "");
    }
 
    public static String findDuplicate(AddCustomerFields f, int excludeCustomerId) {
        String phone = normalizePhone(f.phoneTF.getText());
        String email = f.emailTF.getText().trim();
        if (!phone.isEmpty() && valueExists("phone_number", phone, excludeCustomerId)) {
            return "That phone number is already used by another customer.";
        }
        if (!email.isEmpty() && valueExists("email", email, excludeCustomerId)) {
            return "That email is already used by another customer.";
        }
        return null;
    }
 
    private static boolean valueExists(String column, String value, int excludeCustomerId) {
        String sql = "SELECT 1 FROM customers WHERE " + column + " = ? AND customer_id <> ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, excludeCustomerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
 
    private static String orDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }
}

package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class bAccount {
    public static final String ALL_ROLES = "All Roles";
    public static final String[] ROLES = {"admin", "pharmacist", "cashier"};
    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final String MASK = "********";
 
    private static final int C_ID = 0, C_USERNAME = 1;
 
    public static class AccountFields {
        public JTable accountTable;
        public JTextField searchTF;
        public JComboBox<String> roleCB;
        public JLabel totalL;
        public JLabel adminL;
        public JLabel pharmacistL;
        public JLabel cashierL;
    }
 
    public static class AddAccountFields {
        public JTextField firstNameTF;
        public JTextField lastNameTF;
        public JTextField middleInitialTF;
        public JTextField usernameTF;
        public JComboBox<String> roleCB;
        public JPasswordField passwordTF;
        public JCheckBox showPassCB;
    }
 
    public static class ResetPasswordFields {
        public JTextField accountTF;
        public JPasswordField newPasswordTF;
        public JCheckBox showPassCB;
    }
 
    public static void loadRoleFilter(AccountFields f) {
        f.roleCB.removeAllItems();
        f.roleCB.addItem(ALL_ROLES);
        for (String role : ROLES) {
            f.roleCB.addItem(role);
        }
    }
 
    public static void setAccounts(AccountFields f) {
        String keyword = f.searchTF.getText().trim();
        Object selected = f.roleCB.getSelectedItem();
        String role = (selected == null || ALL_ROLES.equals(selected.toString())) ? "" : selected.toString();
 
        StringBuilder sql = new StringBuilder(
                "SELECT u.user_id, u.username, CONCAT(u.first_name, ' ', u.last_name) AS full_name, "
              + "u.role, CONCAT(c.first_name, ' ', c.last_name) AS created_by_name, u.created_at "
              + "FROM users u LEFT JOIN users c ON u.created_by = c.user_id "
              + "WHERE (u.username LIKE ? OR u.first_name LIKE ? OR u.last_name LIKE ? "
              + "OR CONCAT(u.first_name, ' ', u.last_name) LIKE ?) ");
        if (!role.isEmpty()) {
            sql.append("AND u.role = ? ");
        }
        sql.append("ORDER BY u.user_id");
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            if (!role.isEmpty()) {
                ps.setString(5, role);
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) f.accountTable.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    String createdBy = rs.getString("created_by_name");
                    model.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        createdBy == null ? "-" : createdBy,
                        rs.getTimestamp("created_at").toString(),
                        MASK                                  
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        updateCounts(f);
    }
 
    public static void updateCounts(AccountFields f) {
        String sql = "SELECT COUNT(*) AS total, "
                + "COALESCE(SUM(role = 'admin'), 0) AS admins, "
                + "COALESCE(SUM(role = 'pharmacist'), 0) AS pharmacists, "
                + "COALESCE(SUM(role = 'cashier'), 0) AS cashiers FROM users";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                f.totalL.setText(String.valueOf(rs.getInt("total")));
                f.adminL.setText(String.valueOf(rs.getInt("admins")));
                f.pharmacistL.setText(String.valueOf(rs.getInt("pharmacists")));
                f.cashierL.setText(String.valueOf(rs.getInt("cashiers")));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void refresh(AccountFields f) {
        f.searchTF.setText("");
        if (f.roleCB.getItemCount() > 0) {
            f.roleCB.setSelectedIndex(0);
        }
        setAccounts(f);
    }
 
    public static int getSelectedUserId(JTable accountTable) {
        int row = accountTable.getSelectedRow();
        if (row == -1) {
            return -1;
        }
        return Integer.parseInt(accountTable.getValueAt(row, C_ID).toString());
    }
 
    public static String getSelectedUsername(JTable accountTable) {
        int row = accountTable.getSelectedRow();
        return row == -1 ? null : accountTable.getValueAt(row, C_USERNAME).toString();
    }
 
    public static void deleteAccount(java.awt.Component parent, AccountFields f) {
        if (!requireAdmin(parent)) {
            return;
        }
        int userId = getSelectedUserId(f.accountTable);
        if (userId == -1) {
            JOptionPane.showMessageDialog(parent, "Please select an account to delete.");
            return;
        }
        if (userId == Session.userId) {
            JOptionPane.showMessageDialog(parent, "You can't delete the account you are logged in with.");
            return;
        }
        String username = getSelectedUsername(f.accountTable);
        int x = JOptionPane.showConfirmDialog(parent,
                "DO YOU WANT TO DELETE ACCOUNT \"" + username + "\"?", "CONFIRMATION", JOptionPane.YES_NO_OPTION);
        if (x != JOptionPane.YES_OPTION) {
            return;
        }
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Delete successful");
            setAccounts(f);
        } catch (SQLIntegrityConstraintViolationException fk) {
            JOptionPane.showMessageDialog(parent,
                    "This account already has transactions and can't be deleted.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Delete failed: " + e.getMessage());
        }
    }
 
    public static void printAccounts(AccountFields f) {
        try {
            MessageFormat header = new MessageFormat("Account List");
            MessageFormat footer = new MessageFormat("-{0}-");
            f.accountTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void loadRoles(JComboBox<String> roleCB) {
        roleCB.removeAllItems();
        for (String role : ROLES) {
            roleCB.addItem(role);
        }
        roleCB.setSelectedItem("cashier");
    }
 
    public static void togglePassword(JPasswordField passwordTF, JCheckBox showPassCB) {
        passwordTF.setEchoChar(showPassCB.isSelected() ? (char) 0 : '\u2022');
    }
 
    /** CLEAR button. */
    public static void clearAddFields(AddAccountFields f) {
        f.firstNameTF.setText("");
        f.lastNameTF.setText("");
        f.middleInitialTF.setText("");
        f.usernameTF.setText("");
        f.passwordTF.setText("");
        if (f.roleCB.getItemCount() > 0) {
            f.roleCB.setSelectedItem("cashier");
        }
        f.showPassCB.setSelected(false);
        togglePassword(f.passwordTF, f.showPassCB);
        f.firstNameTF.requestFocus();
    }
 
    public static String checkAddFields(AddAccountFields f) {
        String first = f.firstNameTF.getText().trim();
        String last = f.lastNameTF.getText().trim();
        String mi = f.middleInitialTF.getText().trim();
        String username = f.usernameTF.getText().trim();
        String password = new String(f.passwordTF.getPassword());
 
        if (first.isEmpty() || last.isEmpty() || username.isEmpty() || password.isEmpty()
                || f.roleCB.getSelectedItem() == null) {
            return "Please fill in all required fields.";
        }
        if (mi.length() > 2) {
            return "Middle initial must be 1-2 characters.";
        }
        if (username.contains(" ")) {
            return "Username must not contain spaces.";
        }
        if (username.length() < 4) {
            return "Username must be at least 4 characters.";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Temporary password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        return null;
    }
 
    public static boolean usernameExists(String username) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
 
    public static boolean addAccount(java.awt.Component parent, AddAccountFields f) {
        if (!requireAdmin(parent)) {
            return false;
        }
        String error = checkAddFields(f);
        if (error != null) {
            JOptionPane.showMessageDialog(parent, error);
            return false;
        }
 
        String username = f.usernameTF.getText().trim();
        if (usernameExists(username)) {
            JOptionPane.showMessageDialog(parent, "That username already exists.");
            return false;
        }
 
        String sql = "INSERT INTO users (username, password, first_name, last_name, middle_initial, "
                + "role, must_change_password, created_by) VALUES (?, ?, ?, ?, ?, ?, TRUE, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String mi = f.middleInitialTF.getText().trim();
            ps.setString(1, username);
            ps.setString(2, bAuth.hash(new String(f.passwordTF.getPassword())));
            ps.setString(3, f.firstNameTF.getText().trim());
            ps.setString(4, f.lastNameTF.getText().trim());
            if (mi.isEmpty()) {
                ps.setNull(5, java.sql.Types.CHAR);
            } else {
                ps.setString(5, mi);
            }
            ps.setString(6, f.roleCB.getSelectedItem().toString());
            ps.setInt(7, Session.userId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent,
                    "Account added successfully!\nThe user must change the password on first login.");
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            JOptionPane.showMessageDialog(parent, "That username already exists.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }
 
    public static void loadAccount(AddAccountFields f, int userId) {
        String sql = "SELECT username, first_name, last_name, middle_initial, role FROM users WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.firstNameTF.setText(rs.getString("first_name"));
                    f.lastNameTF.setText(rs.getString("last_name"));
                    String mi = rs.getString("middle_initial");
                    f.middleInitialTF.setText(mi == null ? "" : mi.trim());
                    f.usernameTF.setText(rs.getString("username"));
                    f.roleCB.setSelectedItem(rs.getString("role"));
                    f.passwordTF.setText("");
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static boolean updateAccount(java.awt.Component parent, AddAccountFields f, int userId) {
        if (!requireAdmin(parent)) {
            return false;
        }
        String first = f.firstNameTF.getText().trim();
        String last = f.lastNameTF.getText().trim();
        String mi = f.middleInitialTF.getText().trim();
        String username = f.usernameTF.getText().trim();
 
        if (first.isEmpty() || last.isEmpty() || username.isEmpty() || f.roleCB.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(parent, "Please fill in all required fields.");
            return false;
        }
        if (mi.length() > 2) {
            JOptionPane.showMessageDialog(parent, "Middle initial must be 1-2 characters.");
            return false;
        }
        if (username.contains(" ") || username.length() < 4) {
            JOptionPane.showMessageDialog(parent, "Username must be at least 4 characters with no spaces.");
            return false;
        }
        String newRole = f.roleCB.getSelectedItem().toString();
        if (userId == Session.userId && !newRole.equals(Session.role)) {
            JOptionPane.showMessageDialog(parent, "You can't change the role of your own account.");
            return false;
        }
 
        String sql = "UPDATE users SET username = ?, first_name = ?, last_name = ?, middle_initial = ?, role = ? "
                + "WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, first);
            ps.setString(3, last);
            if (mi.isEmpty()) {
                ps.setNull(4, java.sql.Types.CHAR);
            } else {
                ps.setString(4, mi);
            }
            ps.setString(5, newRole);
            ps.setInt(6, userId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(parent, "Account updated successfully!");
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            JOptionPane.showMessageDialog(parent, "That username already exists.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }
 
    public static void loadResetAccount(ResetPasswordFields f, int userId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.accountTF.setText(rs.getString("username"));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static String checkTempPassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            return "Please enter a new temporary password.";
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            return "Temporary password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        return null;
    }
 
    public static boolean resetPassword(java.awt.Component parent, ResetPasswordFields f, int userId) {
        if (!requireAdmin(parent)) {
            return false;
        }
        String newPassword = new String(f.newPasswordTF.getPassword());
        String error = checkTempPassword(newPassword);
        if (error != null) {
            JOptionPane.showMessageDialog(parent, error);
            return false;
        }
 
        String sql = "UPDATE users SET password = ?, must_change_password = TRUE WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bAuth.hash(newPassword));
            ps.setInt(2, userId);
            if (ps.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(parent, "Account not found.");
                return false;
            }
            JOptionPane.showMessageDialog(parent,
                    "Password reset successfully!\nThe user must change it on next login.");
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return false;
    }
 
    public static void clearResetFields(ResetPasswordFields f) {
        f.newPasswordTF.setText("");
        f.showPassCB.setSelected(false);
        togglePassword(f.newPasswordTF, f.showPassCB);
    }
 
    public static boolean requireAdmin(java.awt.Component parent) {
        if (!"admin".equals(Session.role)) {
            JOptionPane.showMessageDialog(parent, "Only an admin can manage accounts.");
            return false;
        }
        return true;
    }
}

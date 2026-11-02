package Backend;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class bAuth {
    public boolean login(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, first_name, last_name, role, must_change_password "
                + "FROM users WHERE username = ? AND password = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hash(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Session.userId = rs.getInt("user_id");
                    Session.username = rs.getString("username");
                    Session.fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    Session.role = rs.getString("role");
                    Session.mustChangePassword = rs.getBoolean("must_change_password");
                    return true;
                }
            }
        }
        return false;
    }

    public String checkNewPassword(String current, String newPass, String confirm) {
        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            return "Please fill in all fields.";
        }
        if (newPass.length() < 8) {
            return "New password must be at least 8 characters.";
        }
        if (newPass.equals(current)) {
            return "New password must be different from the current password.";
        }
        if (!newPass.equals(confirm)) {
            return "New password and confirm password do not match.";
        }
        return null;
    }

    public boolean changePassword(int userId, String currentPassword, String newPassword) throws SQLException {
        String checkSql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
        String updateSql = "UPDATE users SET password = ?, must_change_password = FALSE WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setInt(1, userId);
                check.setString(2, hash(currentPassword));
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }

            try (PreparedStatement update = con.prepareStatement(updateSql)) {
                update.setString(1, hash(newPassword));
                update.setInt(2, userId);
                update.executeUpdate();
            }
        }
        return true;
    }

    public static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

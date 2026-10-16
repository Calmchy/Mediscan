package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Calmchy
 */
public class bTransaction {
    public static final String ALL_CASHIERS = "All Cashiers";
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public void loadCashierFilter(JComboBox<String> combo) {
        Object previous = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(ALL_CASHIERS);
        String sql = "SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users ORDER BY first_name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(rs.getString("full_name"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        if (previous != null) {
            combo.setSelectedItem(previous);
        }
    }
}

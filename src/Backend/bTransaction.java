package Backend;

import java.awt.print.PrinterException;
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
import java.util.logging.Level;
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
    
    private LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setOrders(JTable table, String orderId, Date from, Date to, String cashier) {
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id, o.order_date, CONCAT(u.first_name, ' ', u.last_name) AS cashier, "
              + "COALESCE(SUM(oi.quantity), 0) AS items, o.total_amount "
              + "FROM orders o "
              + "JOIN users u ON o.user_id = u.user_id "
              + "LEFT JOIN order_items oi ON oi.order_id = o.order_id "
              + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
 
        if (orderId != null && !orderId.trim().isEmpty()) {
            sql.append("AND o.order_id = ? ");
            try {
                params.add(Integer.parseInt(orderId.trim()));
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Order ID must be a number.");
                return;
            }
        }
        if (from != null) {
            sql.append("AND o.order_date >= ? ");
            params.add(Timestamp.valueOf(toLocalDate(from).atStartOfDay()));
        }
        if (to != null) {
            sql.append("AND o.order_date < ? "); // up to the end of the "To" day
            params.add(Timestamp.valueOf(toLocalDate(to).plusDays(1).atStartOfDay()));
        }
        if (cashier != null && !cashier.equals(ALL_CASHIERS)) {
            sql.append("AND CONCAT(u.first_name, ' ', u.last_name) = ? ");
            params.add(cashier);
        }
        sql.append("GROUP BY o.order_id, o.order_date, u.first_name, u.last_name, o.total_amount "
                 + "ORDER BY o.order_date DESC");
 
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("order_id"),
                        rs.getTimestamp("order_date").toLocalDateTime().format(DT_FMT),
                        rs.getString("cashier"),
                        rs.getInt("items"),
                        rs.getDouble("total_amount")
                    });
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    public int getSelectedOrderId(JTable orderTable) {
        int row = orderTable.getSelectedRow();
        if (row < 0) {
            return -1;
        }
        return (int) orderTable.getValueAt(row, 0); // column 0 = Order ID
    }

    public void loadOrderDetails(int orderId, JTable detailsTable,
            JLabel idL, JLabel dateTimeL, JLabel cashierL, JLabel customerL,
            JLabel paymentL, JLabel changeL, JLabel orderTypeL, JLabel totalL) {
 
        String headSql = "SELECT o.order_id, o.order_date, o.order_type, o.total_amount, "
                + "o.payment_amount, o.change_amount, "
                + "CONCAT(u.first_name, ' ', u.last_name) AS cashier, "
                + "CONCAT(c.first_name, ' ', c.last_name) AS customer "
                + "FROM orders o "
                + "JOIN users u ON o.user_id = u.user_id "
                + "LEFT JOIN customers c ON o.customer_id = c.customer_id "
                + "WHERE o.order_id = ?";
 
        String itemSql = "SELECT p.name, oi.quantity, p.unit_type, oi.unit_price, oi.subtotal "
                + "FROM order_items oi JOIN products p ON oi.product_id = p.product_id "
                + "WHERE oi.order_id = ? ORDER BY oi.order_item_id";
 
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(headSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idL.setText(String.valueOf(rs.getInt("order_id")));
                        dateTimeL.setText(rs.getTimestamp("order_date").toLocalDateTime().format(DT_FMT));
                        cashierL.setText(rs.getString("cashier"));
                        String cust = rs.getString("customer");
                        customerL.setText(cust == null ? "Walk-in" : cust);
                        paymentL.setText(MONEY.format(rs.getDouble("payment_amount")));
                        changeL.setText(MONEY.format(rs.getDouble("change_amount")));
                        orderTypeL.setText(rs.getString("order_type"));
                        totalL.setText("₱ " + MONEY.format(rs.getDouble("total_amount")));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    DefaultTableModel model = (DefaultTableModel) detailsTable.getModel();
                    model.setRowCount(0);
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getString("unit_type"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("subtotal")
                        });
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void clearOrderDetails(JTable detailsTable, JLabel... labels) {
        ((DefaultTableModel) detailsTable.getModel()).setRowCount(0);
        for (JLabel l : labels) {
            l.setText("");
        }
    }

    public void reprintReceipt(int orderId, JTable detailsTable, String dateTime, String cashier, String customer, String payment, String change, String total) {
        if (orderId < 0) {
            JOptionPane.showMessageDialog(null, "Select an order first.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("        MEDISCAN POS\n");
        sb.append("     *** RECEIPT (REPRINT) ***\n");
        sb.append("--------------------------------\n");
        sb.append("Order #: ").append(orderId).append("\n");
        sb.append("Date   : ").append(dateTime).append("\n");
        sb.append("Cashier: ").append(cashier).append("\n");
        sb.append("Customer: ").append(customer).append("\n");
        sb.append("--------------------------------\n");
 
        DefaultTableModel m = (DefaultTableModel) detailsTable.getModel();
        for (int i = 0; i < m.getRowCount(); i++) {
            sb.append(m.getValueAt(i, 0)).append("\n");
            sb.append(String.format("  %d x %s = %s\n",
                    (int) m.getValueAt(i, 1),
                    MONEY.format((double) m.getValueAt(i, 3)),
                    MONEY.format((double) m.getValueAt(i, 4))));
        }
        sb.append("--------------------------------\n");
        sb.append("TOTAL  : ").append(total).append("\n");
        sb.append("PAYMENT: ").append(payment).append("\n");
        sb.append("CHANGE : ").append(change).append("\n");
        sb.append("--------------------------------\n");
        sb.append("   Thank you! Please come again.\n");
 
        try {
            JTextArea area = new JTextArea(sb.toString());
            area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 10));
            area.print(null, null, true, null, null, true);
        } catch (PrinterException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     *
     * @param orderTable
     */
    public void printList(JTable orderTable) {
        try {
            MessageFormat header = new MessageFormat("Transaction List");
            MessageFormat footer = new MessageFormat("-{0}-");
            orderTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (PrinterException e) {
            System.out.println(e.getMessage());
        }
    }

    void setOrders(javax.swing.JTable transOrderListTable, String string, Object object, Object object0, String ALL) {
       throw new UnsupportedOperationException("Not supported yet.");
   }
}

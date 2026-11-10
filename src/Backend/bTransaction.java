package Backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class bTransaction {
    public static final String ALL_CASHIERS = "All Cashiers";
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // order status values stored in orders.status
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_VOIDED = "voided";

    // column positions of the transaction list table (transOrderListTable)
    public static final int COL_ORDER_ID = 0;
    public static final int COL_DATE = 1;
    public static final int COL_CASHIER = 2;
    public static final int COL_CUSTOMER = 3;
    public static final int COL_ITEMS = 4;
    public static final int COL_TOTAL = 5;
    public static final int COL_STATUS = 6;

    private static final String[] VOID_REASONS = {
        "Cashier error / accidental checkout",
        "Wrong item or quantity",
        "Wrong price",
        "Customer cancelled the purchase",
        "Wrong payment amount"
    };
 
    public static boolean isCashier() {
        return Session.role != null && Session.role.equalsIgnoreCase("cashier");
    }

    /** Only admin and pharmacist may void. Cashiers cannot. */
    public static boolean canVoid() {
        return Session.role != null
                && (Session.role.equalsIgnoreCase("admin") || Session.role.equalsIgnoreCase("pharmacist"));
    }

    /** "completed" -> "COMPLETED", "voided" -> "VOIDED" (what the STATUS column shows). */
    public static String formatStatus(String dbValue) {
        return STATUS_VOIDED.equalsIgnoreCase(dbValue) ? "VOIDED" : "COMPLETED";
    }

    public static boolean isVoidedLabel(Object statusCell) {
        return statusCell != null && "VOIDED".equalsIgnoreCase(statusCell.toString());
    }

    /**
     * Full customer name with middle initial: "Juan D. Cruz".
     * Middle initial is optional; "D", "d" and "D." all become "D.".
     * Returns "N/A" when there is no customer (walk-in without a customer record).
     */
    public static String formatCustomerName(String first, String middleInitial, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.trim().isEmpty()) {
            sb.append(first.trim());
        }
        if (middleInitial != null) {
            String mi = middleInitial.replace(".", "").trim();
            if (!mi.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(mi.charAt(0))).append('.');
            }
        }
        if (last != null && !last.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(last.trim());
        }
        return sb.length() == 0 ? "N/A" : sb.toString();
    }
    
    public static void loadCashierFilter(JComboBox<String> combo) {
        if (isCashier()) {
            combo.removeAllItems();
            combo.addItem(Session.fullName);
            combo.setSelectedIndex(0);
            combo.setEnabled(false);
            return;
        }

        combo.setEnabled(true);
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
    
    /** Count + total only include COMPLETED orders. Voided rows stay in the list but are not counted. */
    public static void updateSummary(JTable orderTable, JLabel countL, JLabel totalSalesL) {
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        double sum = 0;
        int count = 0;
        for (int r = 0; r < model.getRowCount(); r++) {
            if (isVoidedLabel(model.getValueAt(r, COL_STATUS))) {
                continue;
            }
            sum += Double.parseDouble(model.getValueAt(r, COL_TOTAL).toString());
            count++;
        }
        countL.setText(String.valueOf(count));
        totalSalesL.setText("\u20b1 " + MONEY.format(sum));
    }

    /** Draws VOIDED rows in red so they stand out in the list. Call once after initComponents(). */
    public static void installStatusRenderer(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                boolean voided = isVoidedLabel(t.getValueAt(row, COL_STATUS));
                if (!selected) {
                    c.setForeground(voided ? new Color(192, 57, 43) : t.getForeground());
                }
                return c;
            }
        });
    }
 
    public static void setOrders(JTable table, String orderId, Date from, Date to, String cashier) {
        StringBuilder sql = new StringBuilder("SELECT o.order_id, o.order_date, CONCAT(u.first_name, ' ', u.last_name) AS cashier, "
                + "c.first_name AS c_first, c.middle_initial AS c_mi, c.last_name AS c_last, "
                + "COALESCE(SUM(oi.quantity), 0) AS items, o.total_amount, o.status "
                + "FROM orders o "
                + "JOIN users u ON o.user_id = u.user_id "
                + "LEFT JOIN customers c ON o.customer_id = c.customer_id "
                + "LEFT JOIN order_items oi ON oi.order_id = o.order_id "
                + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
 
        if (orderId != null && !orderId.trim().isEmpty()) {
            sql.append("AND o.order_id = ? ");
            try {
                params.add(Integer.parseInt(orderId.trim()));
            } catch (NumberFormatException e) {
                ((DefaultTableModel) table.getModel()).setRowCount(0);
                return;
            }
        }
        if (from != null) {
            sql.append("AND o.order_date >= ? ");
            params.add(Timestamp.valueOf(toLocalDate(from).atStartOfDay()));
        }
        if (to != null) {
            sql.append("AND o.order_date < ? ");
            params.add(Timestamp.valueOf(toLocalDate(to).plusDays(1).atStartOfDay()));
        }
        if (cashier != null && !cashier.equals(ALL_CASHIERS)) {
            sql.append("AND CONCAT(u.first_name, ' ', u.last_name) = ? ");
            params.add(cashier);
        }
        if (isCashier()) {
            sql.append("AND o.user_id = ? ");
            params.add(Session.userId);
        }
        sql.append("GROUP BY o.order_id, o.order_date, u.first_name, u.last_name, "
                 + "c.first_name, c.middle_initial, c.last_name, o.total_amount, o.status "
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
                    // order must match COL_* constants / the table's column order
                    model.addRow(new Object[]{
                        rs.getInt("order_id"),
                        rs.getTimestamp("order_date").toLocalDateTime().format(DT_FMT),
                        rs.getString("cashier"),
                        formatCustomerName(rs.getString("c_first"), rs.getString("c_mi"), rs.getString("c_last")),
                        rs.getInt("items"),
                        rs.getDouble("total_amount"),
                        formatStatus(rs.getString("status"))
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("setOrders: " + e.getMessage());
        }
    }
 
    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
 
    public static int getSelectedOrderId(JTable orderTable) {
        int row = orderTable.getSelectedRow();
        if (row < 0) {
            return -1;
        }
        return (int) orderTable.getValueAt(row, 0);
    }
    
    public static String formatOrderType(String dbValue) {
        if ("pickup".equalsIgnoreCase(dbValue)) {
            return "Pick-up";
        }
        if ("walk_in".equalsIgnoreCase(dbValue)) {
            return "Walk-in";
        }
        return dbValue == null ? "" : dbValue;
    }
 
    public static void loadOrderDetails(int orderId, JTable detailsTable,
            JLabel idL, JLabel dateTimeL, JLabel cashierL, JLabel customerL,
            JLabel paymentL, JLabel changeL, JLabel orderTypeL, JLabel totalL, JLabel statusL) {
 
        String itemSql = "SELECT p.name, oi.quantity, p.unit_type, oi.unit_price, oi.subtotal "
                + "FROM order_items oi JOIN products p ON oi.product_id = p.product_id "
                + "WHERE oi.order_id = ? ORDER BY oi.order_item_id";
 
        try (Connection con = DBConnection.getConnection()) {
            boolean hasPayment = hasColumn(con, "orders", "payment_amount")
                    && hasColumn(con, "orders", "change_amount");
 
            String headSql = "SELECT o.order_id, o.order_date, o.order_type, o.total_amount, "
                    + (hasPayment ? "o.payment_amount, o.change_amount, "
                                  : "0 AS payment_amount, 0 AS change_amount, ")
                    + "CONCAT(u.first_name, ' ', u.last_name) AS cashier, "
                    + "c.first_name AS c_first, c.middle_initial AS c_mi, c.last_name AS c_last, "
                    + "o.status, o.void_reason, o.voided_at, "
                    + "CONCAT(vu.first_name, ' ', vu.last_name) AS voided_by_name "
                    + "FROM orders o "
                    + "JOIN users u ON o.user_id = u.user_id "
                    + "LEFT JOIN users vu ON o.voided_by = vu.user_id "
                    + "LEFT JOIN customers c ON o.customer_id = c.customer_id "
                    + "WHERE o.order_id = ?";
 
            try (PreparedStatement ps = con.prepareStatement(headSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double total = rs.getDouble("total_amount");
                        double payment = rs.getDouble("payment_amount");
                        double change = rs.getDouble("change_amount");
                        if (payment == 0) {
                            payment = total;
                            change = 0;
                        }
                        idL.setText(String.valueOf(rs.getInt("order_id")));
                        dateTimeL.setText(rs.getTimestamp("order_date").toLocalDateTime().format(DT_FMT));
                        cashierL.setText(rs.getString("cashier"));
                        String cust = formatCustomerName(rs.getString("c_first"),
                                rs.getString("c_mi"), rs.getString("c_last"));
                        String type = rs.getString("order_type");
                        customerL.setText(cust);
                        orderTypeL.setText(formatOrderType(type));
                        paymentL.setText(MONEY.format(payment));
                        changeL.setText(MONEY.format(change));
                        totalL.setText("\u20b1 " + MONEY.format(total));

                        if (statusL != null) {
                            boolean voided = STATUS_VOIDED.equalsIgnoreCase(rs.getString("status"));
                            statusL.setText(formatStatus(rs.getString("status")));
                            statusL.setForeground(voided ? new Color(192, 57, 43) : new Color(39, 174, 96));
                            if (voided) {
                                Timestamp at = rs.getTimestamp("voided_at");
                                String by = rs.getString("voided_by_name");
                                statusL.setToolTipText("<html>Voided"
                                        + (by != null ? " by " + by : "")
                                        + (at != null ? " on " + at.toLocalDateTime().format(DT_FMT) : "")
                                        + "<br>Reason: " + rs.getString("void_reason") + "</html>");
                            } else {
                                statusL.setToolTipText(null);
                            }
                        }
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
            System.out.println("loadOrderDetails: " + e.getMessage());
        }
    }
 
    private static boolean hasColumn(Connection con, String table, String column) throws java.sql.SQLException {
        try (ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
 
    // ------------------------------------------------------------------ VOID

    public static class VoidResult {
        public final int unitsRestored;
        public final int unitsNotRestored;

        VoidResult(int restored, int notRestored) {
            this.unitsRestored = restored;
            this.unitsNotRestored = notRestored;
        }
    }

    /**
     * Voids a completed order. Nothing is deleted: the order is marked 'voided' with who/when/why,
     * and every unit goes back to the exact stock batch it was taken from (order_item_batches).
     * Runs in one DB transaction, so it either fully happens or not at all.
     */
    public static VoidResult voidOrder(int orderId, String reason) throws Exception {
        if (!canVoid()) {
            throw new Exception("Only an admin or pharmacist can void a transaction.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new Exception("A reason is required to void a transaction.");
        }
        reason = reason.trim();
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // lock the order row so two people can't void the same sale at the same time
            String status;
            try (PreparedStatement ps = con.prepareStatement("SELECT status FROM orders WHERE order_id = ? FOR UPDATE")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new Exception("Order #" + orderId + " was not found.");
                    }
                    status = rs.getString("status");
                }
            }
            if (STATUS_VOIDED.equalsIgnoreCase(status)) {
                throw new Exception("Order #" + orderId + " is already voided.");
            }

            int soldUnits = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(quantity), 0) FROM order_items WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        soldUnits = rs.getInt(1);
                    }
                }
            }

            // put the units back into the batches they came from
            int restored = 0;
            String batchSql = "SELECT oib.stock_id, SUM(oib.quantity) AS qty "
                    + "FROM order_item_batches oib JOIN order_items oi ON oib.order_item_id = oi.order_item_id "
                    + "WHERE oi.order_id = ? AND oib.stock_id IS NOT NULL GROUP BY oib.stock_id";
            try (PreparedStatement ps = con.prepareStatement(batchSql);
                 PreparedStatement restore = con.prepareStatement(
                         "UPDATE product_stock SET quantity = quantity + ? WHERE stock_id = ?")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        restore.setInt(1, rs.getInt("qty"));
                        restore.setInt(2, rs.getInt("stock_id"));
                        if (restore.executeUpdate() == 1) {
                            restored += rs.getInt("qty");
                        }
                    }
                }
            }

            // status = 'completed' in WHERE is a second guard against double-restoring stock
            String markSql = "UPDATE orders SET status = 'voided', void_reason = ?, voided_by = ?, voided_at = NOW() "
                    + "WHERE order_id = ? AND status = 'completed'";
            try (PreparedStatement ps = con.prepareStatement(markSql)) {
                ps.setString(1, reason);
                ps.setInt(2, Session.userId);
                ps.setInt(3, orderId);
                if (ps.executeUpdate() != 1) {
                    throw new Exception("Order #" + orderId + " could not be voided (it may already be voided).");
                }
            }

            con.commit();
            return new VoidResult(restored, Math.max(0, soldUnits - restored));
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ignore) {
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Button handler for "VOID TRANSACTION": checks permission, asks for a reason,
     * confirms, voids, and tells the person what happened.
     *
     * @return true if the order was voided (caller should refresh the list)
     */
    public static boolean promptVoid(Component parent, int orderId, Object statusCell) {
        if (!canVoid()) {
            JOptionPane.showMessageDialog(parent, "Only an admin or pharmacist can void a transaction.",
                    "Not allowed", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (orderId < 0) {
            JOptionPane.showMessageDialog(parent, "Select a transaction first.");
            return false;
        }
        if (isVoidedLabel(statusCell)) {
            JOptionPane.showMessageDialog(parent, "Order #" + orderId + " is already voided.");
            return false;
        }

        JComboBox<String> reasonCB = new JComboBox<>(VOID_REASONS);
        reasonCB.setEditable(true);
        reasonCB.setSelectedItem("");
        reasonCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        reasonCB.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, reasonCB.getPreferredSize().height));

        // fixed-width HTML so the label's height is the same when measured and when drawn
        // (a width-less HTML label made JOptionPane size the dialog too short and clip OK/Cancel)
        JLabel info = new JLabel("<html><div style='width:320px'>Void <b>Order #" + orderId + "</b>?<br>"
                + "The sale stays in the records as VOIDED and its items are returned to stock. "
                + "This cannot be undone.</div></html>");
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel reasonL = new JLabel("Reason (required):");
        reasonL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(info);
        panel.add(Box.createVerticalStrut(12));
        panel.add(reasonL);
        panel.add(Box.createVerticalStrut(4));
        panel.add(reasonCB);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        panel.setPreferredSize(new java.awt.Dimension(340, panel.getPreferredSize().height));

        int choice = JOptionPane.showConfirmDialog(parent, panel, "Void Transaction",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return false;
        }
        Object picked = reasonCB.getEditor().getItem();
        String reason = picked == null ? "" : picked.toString().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Please enter a reason for voiding.",
                    "Reason required", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            VoidResult r = voidOrder(orderId, reason);
            String msg = "Order #" + orderId + " was voided.\n" + r.unitsRestored + " unit(s) returned to stock.";
            if (r.unitsNotRestored > 0) {
                msg += "\n\nWarning: " + r.unitsNotRestored + " unit(s) could not be traced to a stock batch "
                        + "(older sale or deleted batch) and were NOT returned. Adjust stock manually.";
            }
            JOptionPane.showMessageDialog(parent, msg, "Voided",
                    r.unitsNotRestored > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Void failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Enables the void button only when the user may void AND the selected row is still completed. */
    public static void updateVoidButton(JButton btn, Object statusCell, int orderId) {
        boolean allowed = canVoid();
        btn.setEnabled(allowed && orderId >= 0 && !isVoidedLabel(statusCell));
        btn.setToolTipText(allowed ? null : "Only an admin or pharmacist can void a transaction");
    }

    public static void clearOrderDetails(JTable detailsTable, JLabel... labels) {
        ((DefaultTableModel) detailsTable.getModel()).setRowCount(0);
        for (JLabel l : labels) {
            l.setText("");
        }
    }
 
    public static void reprintReceipt(int orderId, JTable detailsTable,
            String dateTime, String cashier, String customer,
            String payment, String change, String total) {
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
            sb.append(String.format("  %d x %s = %s\n", (int) m.getValueAt(i, 1), 
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
 
    public static void printList(JTable orderTable) {
        try {
            MessageFormat header = new MessageFormat("Transaction List");
            MessageFormat footer = new MessageFormat("-{0}-");
            orderTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

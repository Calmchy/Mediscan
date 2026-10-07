/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Call;

import Backend.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
/**
 *
 * @author chyril
 */
public class Stock extends javax.swing.JDialog {
    private int stockId = -1;
    /**
     * Creates new form addStock
     */
    public Stock(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("ADD STOCK");
        Title.setText("ADD STOCK");
        loadProducts();
        setupPacksAutoCalc();
    }
    
    public Stock(java.awt.Frame parent, boolean modal, int stockId) {
        super(parent, modal);
        initComponents();
        this.stockId = stockId;
        setTitle("EDIT STOCK");
        Title.setText("EDIT STOCK");
        loadProducts();
        setupPacksAutoCalc();
        loadStockData();
    }
    
    private Integer getPackSizeByProductName(String productName) {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT pack_size FROM products WHERE name = ?");
            ps.setString(1, productName);
            ResultSet rs = ps.executeQuery();
 
            Integer packSize = null;
            if (rs.next()) {
                Object val = rs.getObject("pack_size");
                if (val != null) packSize = rs.getInt("pack_size");
            }
 
            rs.close();
            ps.close();
            con.close();
            return packSize;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    private void setupPacksAutoCalc() {
        stockPReceivedTF.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                String selectedProduct = (String) stockProductNameCB.getSelectedItem();
                String packsText = stockPReceivedTF.getText().trim();
 
                if (selectedProduct == null || packsText.isEmpty()) {
                    return;
                }
 
                Integer packSize = getPackSizeByProductName(selectedProduct);
                if (packSize == null) {
                    return; // no pack_size for this product (e.g. unit_type = piece) — nothing to calculate
                }
 
                try {
                    int packs = Integer.parseInt(packsText);
                    stockQtyTF.setText(String.valueOf(packs * packSize));
                } catch (NumberFormatException e) {
                    // still typing — ignore
                }
            }
        });
    }
    
    private void loadProducts() {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT name FROM products");
            ResultSet rs = ps.executeQuery();
 
            stockProductNameCB.removeAllItems();
            while (rs.next()) {
                stockProductNameCB.addItem(rs.getString("name"));
            }
 
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private int getProductIdByName(String productName) throws Exception {
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT product_id FROM products WHERE name = ?");
        ps.setString(1, productName);
        ResultSet rs = ps.executeQuery();
 
        int id = -1;
        if (rs.next()) {
            id = rs.getInt("product_id");
        }
 
        rs.close();
        ps.close();
        con.close();
        return id;
    }
    
    private void loadStockData() {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mediscan_pos", "root", "");
            PreparedStatement ps = con.prepareStatement("SELECT p.name, ps.batch_no, ps.quantity, ps.expiry_date "
                    + "FROM product_stock ps JOIN products p ON ps.product_id = p.product_id "
                    + "WHERE ps.stock_id = ?");
            
            ps.setInt(1, stockId);
            ResultSet rs = ps.executeQuery();
 
            if (rs.next()) {
                stockBatchNoTF.setText(rs.getString("batch_no"));
                stockQtyTF.setText(String.valueOf(rs.getInt("quantity")));
 
                Date expiry = rs.getDate("expiry_date");
                if (expiry != null) {
                    stockExpiryDateFF.setText(expiry.toString());
                }
 
                stockProductNameCB.setSelectedItem(rs.getString("name"));
            }
 
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        Title = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        stockBatchNoTF = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        stockQtyTF = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        stockProductNameCB = new javax.swing.JComboBox<>();
        jLabel16 = new javax.swing.JLabel();
        stockExpiryDateFF = new javax.swing.JFormattedTextField();
        stockAddBtn = new javax.swing.JButton();
        stockClearBtn = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        stockPReceivedTF = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(43, 138, 130));

        Title.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        Title.setForeground(new java.awt.Color(255, 255, 255));
        Title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Title.setText("ADD STOCK");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(Title)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 85, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(Title)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));

        jLabel11.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(26, 32, 44));
        jLabel11.setText("BATCH NO");

        jLabel12.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(26, 32, 44));
        jLabel12.setText("PRODUCT");

        stockQtyTF.setToolTipText("ddd");

        jLabel14.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(26, 32, 44));
        jLabel14.setText("QUANTITY");

        jLabel16.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(26, 32, 44));
        jLabel16.setText("EXPIRY DATE ( yyyy-MM-dd )");

        stockExpiryDateFF.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.DateFormatter(new java.text.SimpleDateFormat("yyyy-MM-dd"))));
        stockExpiryDateFF.setToolTipText("Format: yyyy-MM-dd (e.g. 2027-05-01)");
        stockExpiryDateFF.setName(""); // NOI18N

        stockAddBtn.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        stockAddBtn.setText("ADD");
        stockAddBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockAddBtnActionPerformed(evt);
            }
        });

        stockClearBtn.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        stockClearBtn.setText("CLEAR");
        stockClearBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockClearBtnActionPerformed(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(26, 32, 44));
        jLabel13.setText("PACKS RECEIVED");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                        .addComponent(stockAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stockClearBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(stockExpiryDateFF, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                                .addComponent(stockBatchNoTF, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                                .addComponent(stockQtyTF, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                                .addComponent(jLabel11)
                                .addComponent(jLabel12)
                                .addComponent(jLabel14)
                                .addComponent(stockProductNameCB, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel16)
                                .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(stockPReceivedTF, javax.swing.GroupLayout.PREFERRED_SIZE, 376, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stockProductNameCB, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stockBatchNoTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stockPReceivedTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stockQtyTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stockExpiryDateFF, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(stockClearBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stockAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        stockExpiryDateFF.getAccessibleContext().setAccessibleName("");
        stockExpiryDateFF.getAccessibleContext().setAccessibleDescription("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void stockAddBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stockAddBtnActionPerformed
        String selectedProduct = (String) stockProductNameCB.getSelectedItem();
        String batchNo = stockBatchNoTF.getText().trim();
        String qtyText = stockQtyTF.getText().trim();
        String expiryText = stockExpiryDateFF.getText().trim();
 
        if (selectedProduct == null || batchNo.isEmpty() || qtyText.isEmpty() || expiryText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }
 
        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.");
            return;
        }
 
        Date expiryDate;
        try {
            expiryDate = Date.valueOf(expiryText);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Expiry Date must be in yyyy-MM-dd format (e.g. 2027-05-01).");
            return;
        }
 
        try {
            int productId = getProductIdByName(selectedProduct);
            if (productId == -1) {
                JOptionPane.showMessageDialog(this, "Selected product could not be found.");
                return;
            }
 
            Connection con = DBConnection.getConnection();
 
            if (stockId == -1) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO product_stock (product_id, batch_no, quantity, expiry_date) "
                        + "VALUES (?, ?, ?, ?)");
                
                ps.setInt(1, productId);
                ps.setString(2, batchNo);
                ps.setInt(3, quantity);
                ps.setDate(4, expiryDate);
                ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(this, "Stock added successfully!");
            } else {
                PreparedStatement ps = con.prepareStatement("UPDATE product_stock "
                        + "SET product_id=?, batch_no=?, quantity=?, expiry_date=? WHERE stock_id=?");
                
                ps.setInt(1, productId);
                ps.setString(2, batchNo);
                ps.setInt(3, quantity);
                ps.setDate(4, expiryDate);
                ps.setInt(5, stockId);
                ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(this, "Stock updated successfully!");
            }
 
            con.close();
            dispose();
 
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_stockAddBtnActionPerformed

    private void stockClearBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stockClearBtnActionPerformed
        if (stockProductNameCB.getItemCount() > 0) stockProductNameCB.setSelectedIndex(0);
        stockBatchNoTF.setText("");
        stockQtyTF.setText("");
        stockExpiryDateFF.setText("");
        stockPReceivedTF.setText("");
    }//GEN-LAST:event_stockClearBtnActionPerformed
        
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Stock.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Stock dialog = new Stock(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Title;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton stockAddBtn;
    private javax.swing.JTextField stockBatchNoTF;
    private javax.swing.JButton stockClearBtn;
    private javax.swing.JFormattedTextField stockExpiryDateFF;
    private javax.swing.JTextField stockPReceivedTF;
    private javax.swing.JComboBox<String> stockProductNameCB;
    private javax.swing.JTextField stockQtyTF;
    // End of variables declaration//GEN-END:variables
}

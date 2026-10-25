package Frontend;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Call.Product;
import Call.Stock;

import Backend.Session;
import Backend.bSales;
import Backend.bCategory;
import Backend.bPrint;
import Backend.bProduct;
import Backend.bStocks;
import Backend.bTransaction;
import Backend.bCustomer;
import Backend.bAccount;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.nio.file.Path;

/**
 *
 * @author chyril
 */
public class Dashboard extends javax.swing.JFrame {
    private int orderId;
    private static final String BARCODE_FOLDER_PRODUCT = System.getProperty("user.home") + "/MediScanBarcodes/Product/";
    private boolean loadingFilters = false;
    private final bSales.SalesFields salesUI = new bSales.SalesFields();
    private final bCustomer.CustomerFields customerUI = new bCustomer.CustomerFields();
    private final bAccount.AccountFields accountUI = new bAccount.AccountFields();
    
    bProduct bProd = new bProduct();
    bStocks bSto = new bStocks();
    //bTransaction bTrans = new bTransaction();
    /**
     * Creates new form Cashier
     */
    public Dashboard() {
        //this.setUndecorated(true); // para waray window border
        initComponents();
        bTransaction.loadCashierFilter(transCashierCB);
        bTransaction.clearOrderDetails(transactionDetailsTable, transOrderIDL, transDateTimeL, transCashierFNL, transCustFNL, transPaymentL, transChangeL, transOrderTypeL, transTotalTF);
        
        addSearchListener(transOrderIDTF, () -> reloadOrders());     // your existing helper
        transFromDC.addPropertyChangeListener("date", e -> reloadOrders());
        transToDC.addPropertyChangeListener("date", e -> reloadOrders());
        transCashierCB.addActionListener(e -> reloadOrders());
        reloadOrders(); 
        //this.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH); // para ma full screen
        setTitle("Dashboard"); 
        applyRolePermissions();
        loadFilterCombos(); 
        setupSearchListeners(); 
        setCategoryTable();
        setProductTable();
        setStockTable();
        
        
        transOrderListTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedOrder();
            }
        });

        salesProductIDScanTF.setFocusable(true);
        salesProductIDScanTF.requestFocus();
        
        salesUI.cartTable = salesTable;
        salesUI.idTF = salesProductIDScanTF;
        salesUI.nameTF = salesProductNameTF;
        salesUI.categoryTF = salesCategoryTF;
        salesUI.brandTF = salesPriceTF1;
        salesUI.priceTF = salesPriceTF;
        salesUI.unitTF = salesUnitTF;
        salesUI.qtyTF = salesQuantityTF;
        salesUI.warningTitleL = salesWarningRequireL;
        salesUI.warningDesTA = salesWarningDesTA;
        salesUI.totalL = totalSalesTF;
        salesUI.cashTF = cashAmountSalesTF;
        salesUI.changeTF = changeSalesTF;
        salesUI.customerIdTF = salesCustomerIDTF;
        salesUI.customerNameL = salesCustomerFNL;
        
        customerUI.customerTable = customerTable;
        customerUI.searchTF = customerSearchTF;
        customerUI.countL = customerCountL;
        bCustomer.applyRolePermissions(customerDeleteBtn);
        bCustomer.setCustomers(customerUI);
        addSearchListener(customerSearchTF, () -> bCustomer.setCustomers(customerUI));
        
        accountUI.accountTable = adminAccountTable;
        accountUI.searchTF  = transOrderIDTF1;
        accountUI.roleCB = transCashierCB1;
        accountUI.totalL = adminAccountsCountL;
        accountUI.adminL = adminAccountsCountL1;
        accountUI.pharmacistL = adminAccountsCountL2;
        accountUI.cashierL = adminAccountsCountL3;
        bAccount.loadRoleFilter(accountUI);
        bAccount.setAccounts(accountUI);
        addSearchListener(transOrderIDTF1, () -> bAccount.setAccounts(accountUI));
        transCashierCB1.addActionListener(e -> bAccount.setAccounts(accountUI));
    }
    
    private void showSelectedOrder() {
        int id = bTransaction.getSelectedOrderId(transOrderListTable);
        if (id < 0) {
            bTransaction.clearOrderDetails(transactionDetailsTable, transOrderIDL, transDateTimeL,
                    transCashierFNL, transCustFNL, transPaymentL, transChangeL, transOrderTypeL, transTotalTF);
            return;
        }
        bTransaction.loadOrderDetails(id, transactionDetailsTable,
                transOrderIDL, transDateTimeL, transCashierFNL, transCustFNL,
                transPaymentL, transChangeL, transOrderTypeL, transTotalTF);
    }
    
    private void reloadOrders() {
        int keepId = bTransaction.getSelectedOrderId(transOrderListTable);
        bTransaction.setOrders(transOrderListTable, transOrderIDTF.getText(),
                transFromDC.getDate(), transToDC.getDate(),
                (String) transCashierCB.getSelectedItem());
        bTransaction.updateSummary(transOrderListTable, transactionsCountL, transTotalSalesL);
        for (int r = 0; r < transOrderListTable.getRowCount(); r++) {
            if ((int) transOrderListTable.getValueAt(r, 0) == keepId) {
                transOrderListTable.setRowSelectionInterval(r, r);
                break;
            }
        }
    }
    
    private void generateMissingBarcodes() {
        int created = 0, skipped = 0;

        try (Connection con = Backend.DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = String.valueOf(rs.getInt("product_id"));
                File file = new File(BARCODE_FOLDER_PRODUCT, id + ".png");

                if (file.exists()) {
                    skipped++;
                    continue;
                }
                saveBarcodeToFile(id, BARCODE_FOLDER_PRODUCT);
                created++;
            }

            JOptionPane.showMessageDialog(this,
                    "Barcodes created: " + created + "\nAlready existed: " + skipped
                    + "\n\nFolder: " + BARCODE_FOLDER_PRODUCT);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(this, "Error generating barcodes: " + e.getMessage());
        }
    }
    
    private void applyRolePermissions() {
        String role = Session.role == null ? "" : Session.role.toLowerCase();
        userFullNameL.setText(Session.fullName);
        userRoleL.setText(role);

        java.util.List<String> allowed;
        switch (role) {
            case "admin":
                allowed = java.util.Arrays.asList("DASHBOARD", "CATEGORY", "PRODUCTS", "STOCK", "SALES", "TRANSACTION", "CUSTOMERS", "PRESCRIPTIONS", "ADMIN");
                tabPannel.setSelectedIndex(0);
                break;
            case "pharmacist":
                allowed = java.util.Arrays.asList("DASHBOARD", "CATEGORY", "PRODUCTS", "STOCK", "SALES", "TRANSACTION", "CUSTOMERS", "PRESCRIPTIONS");
                tabPannel.setSelectedIndex(0);
                break;
            case "cashier":
                allowed = java.util.Arrays.asList("SALES", "TRANSACTION", "CUSTOMERS", "PRESCRIPTIONS");
                tabPannel.setSelectedIndex(4);
                break;
            default:
                allowed = java.util.Collections.emptyList();
        }

        int firstEnabled = -1;
        for (int i = 0; i < tabPannel.getTabCount(); i++) {
            boolean ok = allowed.contains(tabPannel.getTitleAt(i));
            tabPannel.setEnabledAt(i, ok);
            if (ok && firstEnabled == -1) {
                firstEnabled = i;
            }
        }

        if (firstEnabled != -1 && !tabPannel.isEnabledAt(tabPannel.getSelectedIndex())) {
            tabPannel.setSelectedIndex(firstEnabled);
        }
    }
    
    public void setCategoryTable() {
        bCategory.setCategory(categoryTable, categorySearchTF.getText());
    }
    
    public void setProductTable() {
        bProd.setProd(productsTable, productSearchTF.getText(), (String) productCategoryCB.getSelectedItem());
    }
    
    public void setStockTable() {
        bSto.setStock(stockTable, stockSearchTF.getText(), (String) stockFilterProductCB.getSelectedItem());
    }
    
    private void loadFilterCombos() {
        loadingFilters = true;
        try {
            bProd.loadCategoryFilter(productCategoryCB);
            bSto.loadProductFilter(stockFilterProductCB);
        } finally {
            loadingFilters = false;
        }
    }
    
    private void setupSearchListeners() {
        addSearchListener(categorySearchTF, () -> setCategoryTable());
        addSearchListener(productSearchTF, () -> setProductTable());
        addSearchListener(stockSearchTF, () -> setStockTable());

        productCategoryCB.addActionListener(e -> { if (!loadingFilters) setProductTable(); });
        stockFilterProductCB.addActionListener(e -> { if (!loadingFilters) setStockTable(); });

        tabPannel.addChangeListener(e -> {
            loadFilterCombos();
            setProductTable();
            setStockTable();
        });
    }

    private void addSearchListener(javax.swing.JTextField field, Runnable reload) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { reload.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { reload.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { reload.run(); }
        });
    }
    
    private void saveBarcodeToFile(String data, String FOLDER) {
        try {
            File folder = new File(FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, 300, 100);

            Path path = new File(folder, data + ".png").toPath();
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

            System.out.println("Barcode saved: " + path.toString());
        } catch (Exception e) {
            System.out.println("Barcode save failed: " + e.getMessage());
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

        background = new javax.swing.JPanel();
        header = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        logoutDashboardBtn = new javax.swing.JButton();
        userRoleL = new javax.swing.JLabel();
        userFullNameL = new javax.swing.JLabel();
        tabPannel = new javax.swing.JTabbedPane();
        mainDashboardTab = new javax.swing.JPanel();
        dashboardBG = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        dashRefreshBtn = new javax.swing.JButton();
        jLabel43 = new javax.swing.JLabel();
        dashTodayBtn = new javax.swing.JButton();
        dash7DaysBtn = new javax.swing.JButton();
        dash30DaysBtn = new javax.swing.JButton();
        jLabel44 = new javax.swing.JLabel();
        dashFromDC = new com.toedter.calendar.JDateChooser();
        jLabel45 = new javax.swing.JLabel();
        dashToDC = new com.toedter.calendar.JDateChooser();
        jPanel26 = new javax.swing.JPanel();
        jLabel60 = new javax.swing.JLabel();
        dashSalesPerDayP = new javax.swing.JPanel();
        jPanel27 = new javax.swing.JPanel();
        jLabel61 = new javax.swing.JLabel();
        dashSalesByCategoryP = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jLabel59 = new javax.swing.JLabel();
        dashTop5ProductsP = new javax.swing.JPanel();
        jPanel29 = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        dashLowStockTable = new javax.swing.JTable();
        jLabel57 = new javax.swing.JLabel();
        jPanel30 = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        dashExpiringSoonTable = new javax.swing.JTable();
        jLabel58 = new javax.swing.JLabel();
        jPanel31 = new javax.swing.JPanel();
        dashSalesReportBtn = new javax.swing.JButton();
        jLabel56 = new javax.swing.JLabel();
        dashStockReportBtn = new javax.swing.JButton();
        dashExpiryReportBtn = new javax.swing.JButton();
        jPanel32 = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        jPanel25 = new javax.swing.JPanel();
        jLabel54 = new javax.swing.JLabel();
        dashExpiringLeftTF = new javax.swing.JLabel();
        dashExpiringLeftTF1 = new javax.swing.JLabel();
        dashExpiredRightTF = new javax.swing.JLabel();
        jPanel22 = new javax.swing.JPanel();
        jPanel23 = new javax.swing.JPanel();
        jLabel52 = new javax.swing.JLabel();
        dashLowStockCatTF = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        jLabel50 = new javax.swing.JLabel();
        dashItemSoldTF = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jLabel48 = new javax.swing.JLabel();
        dashTransactionsTF = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel46 = new javax.swing.JLabel();
        dashTotalSalesTF = new javax.swing.JLabel();
        categoryTab = new javax.swing.JPanel();
        categoryBG = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        categoryTable = new javax.swing.JTable();
        jPanel10 = new javax.swing.JPanel();
        printCategoryBtn = new javax.swing.JButton();
        deleteCategoryBtn = new javax.swing.JButton();
        editCategoryBtn = new javax.swing.JButton();
        addCategoryBtn = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        categoryRefreshBtn = new javax.swing.JButton();
        categorySearchTF = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        productTab = new javax.swing.JPanel();
        productBG = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        productsTable = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        genBarProductBtn = new javax.swing.JButton();
        printProductBtn = new javax.swing.JButton();
        deleteProductBtn = new javax.swing.JButton();
        editProductBtn = new javax.swing.JButton();
        addProductBtn = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        productsRefreshBtn = new javax.swing.JButton();
        productCategoryCB = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        productSearchTF = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        stockTab = new javax.swing.JPanel();
        stockBG = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        stockTable = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        printStockBtn = new javax.swing.JButton();
        deleteStockBtn = new javax.swing.JButton();
        editStockBtn = new javax.swing.JButton();
        addStockBtn = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        stockFilterProductCB = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        stockSearchTF = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        stockRefreshBtn = new javax.swing.JButton();
        salesTab = new javax.swing.JPanel();
        salesBG = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        salesTable = new javax.swing.JTable();
        salesCheckoutBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        salesProductNameTF = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        salesCategoryTF = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        salesPriceTF = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        salesQuantityTF = new javax.swing.JTextField();
        salesAddBtn = new javax.swing.JButton();
        salesCancelBtn = new javax.swing.JButton();
        salesProductIDScanTF = new javax.swing.JTextField();
        salesPriceTF1 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        salesUnitTF = new javax.swing.JTextField();
        jScrollPane7 = new javax.swing.JScrollPane();
        salesWarningDesTA = new javax.swing.JTextArea();
        salesWarningRequireL = new javax.swing.JLabel();
        changeSalesTF = new javax.swing.JTextField();
        cashAmountSalesTF = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        salesRemoveItemBtn = new javax.swing.JButton();
        salesRemoveAllBtn = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        totalSalesTF = new javax.swing.JLabel();
        salesCustomerIDTF = new javax.swing.JTextField();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        salesCustomerFNL = new javax.swing.JLabel();
        transactionTab = new javax.swing.JPanel();
        transactionBG = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        transOrderListTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        transactionDetailsTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        transOrderTypeL = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        transChangeL = new javax.swing.JLabel();
        transPaymentL = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        transCustFNL = new javax.swing.JLabel();
        transCashierFNL = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        transDateTimeL = new javax.swing.JLabel();
        transOrderIDL = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        transTotalTF = new javax.swing.JLabel();
        transReprintReceiptBtn = new javax.swing.JButton();
        transPrintListBtn = new javax.swing.JButton();
        transTotalSalesL = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        transactionsCountL = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        transRefreshBtn = new javax.swing.JButton();
        transCashierCB = new javax.swing.JComboBox<>();
        jLabel21 = new javax.swing.JLabel();
        transToDC = new com.toedter.calendar.JDateChooser();
        jLabel20 = new javax.swing.JLabel();
        transFromDC = new com.toedter.calendar.JDateChooser();
        jLabel19 = new javax.swing.JLabel();
        transOrderIDTF = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        customersTab = new javax.swing.JPanel();
        customerBG = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        customerSearchTF = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        customerCountL = new javax.swing.JLabel();
        customerRefreshBtn = new javax.swing.JButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        customerTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        customerAddBtn = new javax.swing.JButton();
        customerEditBtn = new javax.swing.JButton();
        customerDeleteBtn = new javax.swing.JButton();
        customerPrintBtn = new javax.swing.JButton();
        prescriptionTab = new javax.swing.JPanel();
        accountsTab = new javax.swing.JPanel();
        accountsBG = new javax.swing.JPanel();
        headBG = new javax.swing.JPanel();
        adminRefreshBtn = new javax.swing.JButton();
        adminAccountsCountL3 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        adminAccountsCountL2 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        adminAccountsCountL1 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        adminAccountsCountL = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        transCashierCB1 = new javax.swing.JComboBox<>();
        jLabel32 = new javax.swing.JLabel();
        transOrderIDTF1 = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        footer = new javax.swing.JPanel();
        adminAddBtn = new javax.swing.JButton();
        adminEditBtn = new javax.swing.JButton();
        adminDeleteBtn = new javax.swing.JButton();
        adminResetPassBtn = new javax.swing.JButton();
        adminPrintBtn = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        adminAccountTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1024, 768));

        background.setBackground(new java.awt.Color(245, 247, 250));
        background.setPreferredSize(new java.awt.Dimension(1053, 768));

        header.setBackground(new java.awt.Color(43, 138, 130));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 48)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("MediScan POS");

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/logo-64.png"))); // NOI18N
        jLabel3.setText(" ");

        logoutDashboardBtn.setText("Logout");
        logoutDashboardBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutDashboardBtnActionPerformed(evt);
            }
        });

        userRoleL.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        userRoleL.setForeground(new java.awt.Color(255, 255, 255));
        userRoleL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        userRoleL.setText("admin");

        userFullNameL.setForeground(new java.awt.Color(255, 255, 255));
        userFullNameL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        userFullNameL.setText("Admin Full Name");

        javax.swing.GroupLayout headerLayout = new javax.swing.GroupLayout(header);
        header.setLayout(headerLayout);
        headerLayout.setHorizontalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(logoutDashboardBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(userRoleL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(userFullNameL, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        headerLayout.setVerticalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(headerLayout.createSequentialGroup()
                        .addComponent(userFullNameL)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(userRoleL)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(logoutDashboardBtn)))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        tabPannel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        tabPannel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabPannelStateChanged(evt);
            }
        });

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        dashRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dashRefreshBtn.setText("REFRESH");
        dashRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dashRefreshBtnActionPerformed(evt);
            }
        });

        jLabel43.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel43.setText("Search:");

        dashTodayBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dashTodayBtn.setText("TODAY");
        dashTodayBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dashTodayBtnActionPerformed(evt);
            }
        });

        dash7DaysBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dash7DaysBtn.setText("7 DAYS");
        dash7DaysBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dash7DaysBtnActionPerformed(evt);
            }
        });

        dash30DaysBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dash30DaysBtn.setText("30 DAYS");
        dash30DaysBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dash30DaysBtnActionPerformed(evt);
            }
        });

        jLabel44.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel44.setText("From:");

        jLabel45.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel45.setText("To:");

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel43)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashTodayBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dash7DaysBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dash30DaysBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel44)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashFromDC, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel45, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashToDC, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(dashRefreshBtn)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dashToDC, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(dashRefreshBtn)
                        .addComponent(jLabel43)
                        .addComponent(dashTodayBtn)
                        .addComponent(dash7DaysBtn)
                        .addComponent(dash30DaysBtn)
                        .addComponent(jLabel44)
                        .addComponent(jLabel45))
                    .addComponent(dashFromDC, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel26.setBackground(new java.awt.Color(255, 255, 255));
        jPanel26.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jLabel60.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel60.setText("TOP 5 PRODUCTS");

        javax.swing.GroupLayout dashSalesPerDayPLayout = new javax.swing.GroupLayout(dashSalesPerDayP);
        dashSalesPerDayP.setLayout(dashSalesPerDayPLayout);
        dashSalesPerDayPLayout.setHorizontalGroup(
            dashSalesPerDayPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        dashSalesPerDayPLayout.setVerticalGroup(
            dashSalesPerDayPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 167, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel26Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dashSalesPerDayP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel60, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel26Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel60)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashSalesPerDayP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel27.setBackground(new java.awt.Color(255, 255, 255));
        jPanel27.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jLabel61.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel61.setText("TOP 5 PRODUCTS");

        javax.swing.GroupLayout dashSalesByCategoryPLayout = new javax.swing.GroupLayout(dashSalesByCategoryP);
        dashSalesByCategoryP.setLayout(dashSalesByCategoryPLayout);
        dashSalesByCategoryPLayout.setHorizontalGroup(
            dashSalesByCategoryPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        dashSalesByCategoryPLayout.setVerticalGroup(
            dashSalesByCategoryPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel27Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dashSalesByCategoryP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel61, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel61)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashSalesByCategoryP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel28.setBackground(new java.awt.Color(255, 255, 255));
        jPanel28.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jLabel59.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel59.setText("TOP 5 PRODUCTS");

        javax.swing.GroupLayout dashTop5ProductsPLayout = new javax.swing.GroupLayout(dashTop5ProductsP);
        dashTop5ProductsP.setLayout(dashTop5ProductsPLayout);
        dashTop5ProductsPLayout.setHorizontalGroup(
            dashTop5ProductsPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        dashTop5ProductsPLayout.setVerticalGroup(
            dashTop5ProductsPLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel28Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dashTop5ProductsP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel59, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel28Layout.setVerticalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel28Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel59)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashTop5ProductsP, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel29.setBackground(new java.awt.Color(255, 255, 255));
        jPanel29.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        dashLowStockTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PRODUCT", "UNITS LEFT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane10.setViewportView(dashLowStockTable);

        jLabel57.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel57.setText("LOW STOCK");

        javax.swing.GroupLayout jPanel29Layout = new javax.swing.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel29Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabel57, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel29Layout.setVerticalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel29Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel57)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel30.setBackground(new java.awt.Color(255, 255, 255));
        jPanel30.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        dashExpiringSoonTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PRODUCT", "BATCH", "EXPIRY", "DAYS LEFT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane11.setViewportView(dashExpiringSoonTable);

        jLabel58.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel58.setText("EXPIRING SOON");

        javax.swing.GroupLayout jPanel30Layout = new javax.swing.GroupLayout(jPanel30);
        jPanel30.setLayout(jPanel30Layout);
        jPanel30Layout.setHorizontalGroup(
            jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel30Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabel58, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel30Layout.setVerticalGroup(
            jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel30Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel58)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel31.setBackground(new java.awt.Color(255, 255, 255));

        dashSalesReportBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dashSalesReportBtn.setText("SALES REPORT");
        dashSalesReportBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dashSalesReportBtnActionPerformed(evt);
            }
        });

        jLabel56.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel56.setText("REPORTS (PDF):");

        dashStockReportBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dashStockReportBtn.setText("STOCK REPORT");
        dashStockReportBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dashStockReportBtnActionPerformed(evt);
            }
        });

        dashExpiryReportBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        dashExpiryReportBtn.setText("EXPIRY REPORT");
        dashExpiryReportBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dashExpiryReportBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel31Layout = new javax.swing.GroupLayout(jPanel31);
        jPanel31.setLayout(jPanel31Layout);
        jPanel31Layout.setHorizontalGroup(
            jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel31Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel56)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashSalesReportBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashStockReportBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashExpiryReportBtn)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel31Layout.setVerticalGroup(
            jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel31Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dashSalesReportBtn)
                    .addComponent(jLabel56)
                    .addComponent(dashStockReportBtn)
                    .addComponent(dashExpiryReportBtn))
                .addContainerGap())
        );

        jPanel24.setBackground(new java.awt.Color(255, 255, 255));
        jPanel24.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jPanel25.setBackground(new java.awt.Color(192, 57, 43));

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel54.setText("EXPIRING ≤ 60 DAYS / EXPIRED");

        dashExpiringLeftTF.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashExpiringLeftTF.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        dashExpiringLeftTF.setText("0");

        dashExpiringLeftTF1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashExpiringLeftTF1.setForeground(new java.awt.Color(255, 0, 0));
        dashExpiringLeftTF1.setText("/");

        dashExpiredRightTF.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashExpiredRightTF.setForeground(new java.awt.Color(255, 0, 0));
        dashExpiredRightTF.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        dashExpiredRightTF.setText("0");

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel24Layout.createSequentialGroup()
                .addComponent(jPanel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel54, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel24Layout.createSequentialGroup()
                        .addComponent(dashExpiringLeftTF, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dashExpiringLeftTF1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dashExpiredRightTF, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel24Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel54)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dashExpiringLeftTF, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(dashExpiringLeftTF1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(dashExpiredRightTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel22.setBackground(new java.awt.Color(255, 255, 255));
        jPanel22.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jPanel23.setBackground(new java.awt.Color(221, 161, 40));

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel52.setText("LOW STOCK PRODUCTS");

        dashLowStockCatTF.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashLowStockCatTF.setText("0");

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dashLowStockCatTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel52, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel52)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashLowStockCatTF, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel18.setBackground(new java.awt.Color(255, 255, 255));
        jPanel18.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jPanel19.setBackground(new java.awt.Color(43, 138, 130));

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel50.setText("ITEMS SOLD");

        dashItemSoldTF.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashItemSoldTF.setText("0");

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dashItemSoldTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel50, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel50)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashItemSoldTF, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel16.setBackground(new java.awt.Color(255, 255, 255));
        jPanel16.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jPanel17.setBackground(new java.awt.Color(43, 138, 130));

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel48.setText("TRANSACTIONS");

        dashTransactionsTF.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashTransactionsTF.setText("0");

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dashTransactionsTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel48, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel48)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashTransactionsTF, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel14.setBackground(new java.awt.Color(255, 255, 255));
        jPanel14.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(180, 195, 211)));

        jPanel15.setBackground(new java.awt.Color(43, 138, 130));

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel46.setText("TOTAL SALES");

        dashTotalSalesTF.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        dashTotalSalesTF.setForeground(new java.awt.Color(30, 58, 95));
        dashTotalSalesTF.setText("₱ 0");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dashTotalSalesTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel46, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel46)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dashTotalSalesTF, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel32Layout = new javax.swing.GroupLayout(jPanel32);
        jPanel32.setLayout(jPanel32Layout);
        jPanel32Layout.setHorizontalGroup(
            jPanel32Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel32Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel32Layout.setVerticalGroup(
            jPanel32Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel32Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel32Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout dashboardBGLayout = new javax.swing.GroupLayout(dashboardBG);
        dashboardBG.setLayout(dashboardBGLayout);
        dashboardBGLayout.setHorizontalGroup(
            dashboardBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(dashboardBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dashboardBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dashboardBGLayout.createSequentialGroup()
                        .addComponent(jPanel28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel29, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel26, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dashboardBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel30, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(jPanel31, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel32, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        dashboardBGLayout.setVerticalGroup(
            dashboardBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardBGLayout.createSequentialGroup()
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel32, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dashboardBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dashboardBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel29, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel28, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel30, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(11, 11, 11)
                .addComponent(jPanel31, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout mainDashboardTabLayout = new javax.swing.GroupLayout(mainDashboardTab);
        mainDashboardTab.setLayout(mainDashboardTabLayout);
        mainDashboardTabLayout.setHorizontalGroup(
            mainDashboardTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(dashboardBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mainDashboardTabLayout.setVerticalGroup(
            mainDashboardTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(dashboardBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("DASHBOARD", mainDashboardTab);

        categoryTable.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        categoryTable.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        categoryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "CATEGORY ID", "CATEGORY NAME"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane5.setViewportView(categoryTable);

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));

        printCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        printCategoryBtn.setText("PRINT");
        printCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printCategoryBtnActionPerformed(evt);
            }
        });

        deleteCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        deleteCategoryBtn.setText("DELETE");
        deleteCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteCategoryBtnActionPerformed(evt);
            }
        });

        editCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        editCategoryBtn.setText("EDIT");
        editCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editCategoryBtnActionPerformed(evt);
            }
        });

        addCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        addCategoryBtn.setText("ADD");
        addCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCategoryBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deleteCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(printCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(printCategoryBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(addCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(editCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(deleteCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        categoryRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        categoryRefreshBtn.setText("REFRESH");
        categoryRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categoryRefreshBtnActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel7.setText("Search:");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addGap(3, 3, 3)
                .addComponent(categorySearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(categoryRefreshBtn)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(categorySearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(categoryRefreshBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout categoryBGLayout = new javax.swing.GroupLayout(categoryBG);
        categoryBG.setLayout(categoryBGLayout);
        categoryBGLayout.setHorizontalGroup(
            categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(categoryBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 1031, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        categoryBGLayout.setVerticalGroup(
            categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, categoryBGLayout.createSequentialGroup()
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout categoryTabLayout = new javax.swing.GroupLayout(categoryTab);
        categoryTab.setLayout(categoryTabLayout);
        categoryTabLayout.setHorizontalGroup(
            categoryTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(categoryBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        categoryTabLayout.setVerticalGroup(
            categoryTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(categoryBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("CATEGORY", categoryTab);

        productTab.setBackground(new java.awt.Color(204, 204, 204));

        productsTable.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        productsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "NAME", "BRAND", "CATEGORY", "COST", "PRICE", "RX", "MINIMUM AGE", "UNIT", "PACK SIZE"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(productsTable);

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));

        genBarProductBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        genBarProductBtn.setText("GENERATE BARCODE");
        genBarProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genBarProductBtnActionPerformed(evt);
            }
        });

        printProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        printProductBtn.setText("PRINT");
        printProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printProductBtnActionPerformed(evt);
            }
        });

        deleteProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        deleteProductBtn.setText("DELETE");
        deleteProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteProductBtnActionPerformed(evt);
            }
        });

        editProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        editProductBtn.setText("EDIT");
        editProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProductBtnActionPerformed(evt);
            }
        });

        addProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        addProductBtn.setText("ADD");
        addProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addProductBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deleteProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(printProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(genBarProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(printProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(genBarProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));

        productsRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        productsRefreshBtn.setText("REFRESH");
        productsRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                productsRefreshBtnActionPerformed(evt);
            }
        });

        jLabel6.setText("Category:");

        jLabel5.setText("Search:");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addGap(3, 3, 3)
                .addComponent(productSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addGap(3, 3, 3)
                .addComponent(productCategoryCB, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 357, Short.MAX_VALUE)
                .addComponent(productsRefreshBtn)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(productSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(productCategoryCB, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(productsRefreshBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        productSearchTF.getAccessibleContext().setAccessibleName("");

        javax.swing.GroupLayout productBGLayout = new javax.swing.GroupLayout(productBG);
        productBG.setLayout(productBGLayout);
        productBGLayout.setHorizontalGroup(
            productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(productBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        productBGLayout.setVerticalGroup(
            productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(productBGLayout.createSequentialGroup()
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout productTabLayout = new javax.swing.GroupLayout(productTab);
        productTab.setLayout(productTabLayout);
        productTabLayout.setHorizontalGroup(
            productTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(productBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        productTabLayout.setVerticalGroup(
            productTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(productBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("PRODUCTS", productTab);

        stockTable.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        stockTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "STOCK ID", "PRODUCT NAME", "BATCH NO", "QUANTITY", "UNIT", "EXPIRY DATE", "DATE RECEIVED"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(stockTable);

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));

        printStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        printStockBtn.setText("PRINT");
        printStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printStockBtnActionPerformed(evt);
            }
        });

        deleteStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        deleteStockBtn.setText("DELETE");
        deleteStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteStockBtnActionPerformed(evt);
            }
        });

        editStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        editStockBtn.setText("EDIT");
        editStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStockBtnActionPerformed(evt);
            }
        });

        addStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        addStockBtn.setText("ADD");
        addStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStockBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deleteStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(printStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(265, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(printStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(deleteStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(editStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(addStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setText("Filter by Product:");

        jLabel2.setText("Search Batch No:");

        stockRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        stockRefreshBtn.setText("REFRESH");
        stockRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockRefreshBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(6, 6, 6)
                .addComponent(stockSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(3, 3, 3)
                .addComponent(stockFilterProductCB, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(stockRefreshBtn)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stockFilterProductCB, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel2)
                    .addComponent(stockSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stockRefreshBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout stockBGLayout = new javax.swing.GroupLayout(stockBG);
        stockBG.setLayout(stockBGLayout);
        stockBGLayout.setHorizontalGroup(
            stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, stockBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4)
                .addContainerGap())
        );
        stockBGLayout.setVerticalGroup(
            stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stockBGLayout.createSequentialGroup()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout stockTabLayout = new javax.swing.GroupLayout(stockTab);
        stockTab.setLayout(stockTabLayout);
        stockTabLayout.setHorizontalGroup(
            stockTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(stockBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        stockTabLayout.setVerticalGroup(
            stockTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(stockBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("STOCK", stockTab);

        salesTable.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        salesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "#", "ID", "NAME", "BRAND", "PRICE", "QTY", "UNIT", "SUBTOTAL"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Integer.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(salesTable);
        if (salesTable.getColumnModel().getColumnCount() > 0) {
            salesTable.getColumnModel().getColumn(0).setMinWidth(20);
            salesTable.getColumnModel().getColumn(0).setPreferredWidth(40);
            salesTable.getColumnModel().getColumn(0).setMaxWidth(60);
            salesTable.getColumnModel().getColumn(1).setMinWidth(60);
            salesTable.getColumnModel().getColumn(1).setPreferredWidth(90);
            salesTable.getColumnModel().getColumn(1).setMaxWidth(120);
            salesTable.getColumnModel().getColumn(5).setMinWidth(30);
            salesTable.getColumnModel().getColumn(5).setPreferredWidth(50);
            salesTable.getColumnModel().getColumn(5).setMaxWidth(70);
            salesTable.getColumnModel().getColumn(6).setMinWidth(100);
            salesTable.getColumnModel().getColumn(6).setPreferredWidth(100);
            salesTable.getColumnModel().getColumn(6).setMaxWidth(100);
        }

        salesCheckoutBtn.setText("CHECKOUT");
        salesCheckoutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salesCheckoutBtnActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel8.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("PRODUCT ID (SCAN)");

        jLabel9.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel9.setText("PRODUCT NAME");

        salesProductNameTF.setEditable(false);
        salesProductNameTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel10.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel10.setText("CATEGORY");

        salesCategoryTF.setEditable(false);
        salesCategoryTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel11.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel11.setText("PRICE");

        salesPriceTF.setEditable(false);
        salesPriceTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel12.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel12.setText("QUANTITY");

        salesQuantityTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        salesQuantityTF.setText("2");

        salesAddBtn.setText("ADD");
        salesAddBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salesAddBtnActionPerformed(evt);
            }
        });

        salesCancelBtn.setText("CANCEL");
        salesCancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salesCancelBtnActionPerformed(evt);
            }
        });

        salesProductIDScanTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        salesProductIDScanTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                salesProductIDScanTFKeyReleased(evt);
            }
        });

        salesPriceTF1.setEditable(false);
        salesPriceTF1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel13.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel13.setText("BRAND");

        jLabel17.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel17.setText("UNIT");

        salesUnitTF.setEditable(false);
        salesUnitTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jScrollPane7.setBorder(null);

        salesWarningDesTA.setEditable(false);
        salesWarningDesTA.setColumns(20);
        salesWarningDesTA.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        salesWarningDesTA.setForeground(new java.awt.Color(255, 0, 51));
        salesWarningDesTA.setLineWrap(true);
        salesWarningDesTA.setRows(3);
        salesWarningDesTA.setTabSize(4);
        salesWarningDesTA.setWrapStyleWord(true);
        salesWarningDesTA.setBorder(null);
        salesWarningDesTA.setDisabledTextColor(new java.awt.Color(255, 0, 0));
        salesWarningDesTA.setFocusable(false);
        salesWarningDesTA.setRequestFocusEnabled(false);
        jScrollPane7.setViewportView(salesWarningDesTA);

        salesWarningRequireL.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        salesWarningRequireL.setForeground(new java.awt.Color(255, 0, 51));
        salesWarningRequireL.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        salesWarningRequireL.setText(" ");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7)
                    .addComponent(salesPriceTF1)
                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(salesUnitTF, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(salesProductIDScanTF)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(salesAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(salesCancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(salesProductNameTF)
                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(salesCategoryTF)
                            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(salesPriceTF)
                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(salesQuantityTF))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(salesWarningRequireL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesProductIDScanTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesProductNameTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesCategoryTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesPriceTF1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesPriceTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesUnitTF, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesQuantityTF, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(salesAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(salesCancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addComponent(salesWarningRequireL, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(59, Short.MAX_VALUE))
        );

        changeSalesTF.setEditable(false);
        changeSalesTF.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        changeSalesTF.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        cashAmountSalesTF.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        cashAmountSalesTF.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        cashAmountSalesTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                cashAmountSalesTFKeyReleased(evt);
            }
        });

        jLabel14.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("TOTAL");

        jLabel15.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("CASH AMOUNT:");

        salesRemoveItemBtn.setText("REMOVE ITEM");
        salesRemoveItemBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salesRemoveItemBtnActionPerformed(evt);
            }
        });

        salesRemoveAllBtn.setText("REMOVE ALL");
        salesRemoveAllBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salesRemoveAllBtnActionPerformed(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("CHANGE:");

        totalSalesTF.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        totalSalesTF.setForeground(new java.awt.Color(43, 138, 130));
        totalSalesTF.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        totalSalesTF.setText("0");

        salesCustomerIDTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        salesCustomerIDTF.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        salesCustomerIDTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                salesCustomerIDTFKeyReleased(evt);
            }
        });

        jLabel41.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel41.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel41.setText("CUSTOMER ID:");

        jLabel42.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel42.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel42.setText("FULLNAME:");

        salesCustomerFNL.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        salesCustomerFNL.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        salesCustomerFNL.setText(" ");

        javax.swing.GroupLayout salesBGLayout = new javax.swing.GroupLayout(salesBG);
        salesBG.setLayout(salesBGLayout);
        salesBGLayout.setHorizontalGroup(
            salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(salesBGLayout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(salesBGLayout.createSequentialGroup()
                        .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(salesBGLayout.createSequentialGroup()
                                    .addGap(1, 1, 1)
                                    .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(salesRemoveItemBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(salesRemoveAllBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(salesBGLayout.createSequentialGroup()
                                            .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGroup(salesBGLayout.createSequentialGroup()
                                    .addComponent(cashAmountSalesTF, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(changeSalesTF, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(totalSalesTF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addComponent(salesCheckoutBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(salesBGLayout.createSequentialGroup()
                                .addComponent(jLabel41)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(salesCustomerIDTF, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel42)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(salesCustomerFNL, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 48, Short.MAX_VALUE)))
                .addContainerGap())
        );
        salesBGLayout.setVerticalGroup(
            salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(salesBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel41)
                    .addComponent(salesCustomerIDTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel42)
                    .addComponent(salesCustomerFNL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(salesRemoveItemBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(salesRemoveAllBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cashAmountSalesTF, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(changeSalesTF, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(totalSalesTF, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(salesCheckoutBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout salesTabLayout = new javax.swing.GroupLayout(salesTab);
        salesTab.setLayout(salesTabLayout);
        salesTabLayout.setHorizontalGroup(
            salesTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(salesBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        salesTabLayout.setVerticalGroup(
            salesTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(salesBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("SALES", salesTab);

        transOrderListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Oder ID", "DATE & TIME", "CASHIER", "ITEMS", "TOTAL"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(transOrderListTable);
        if (transOrderListTable.getColumnModel().getColumnCount() > 0) {
            transOrderListTable.getColumnModel().getColumn(0).setMinWidth(30);
            transOrderListTable.getColumnModel().getColumn(0).setPreferredWidth(55);
            transOrderListTable.getColumnModel().getColumn(0).setMaxWidth(80);
            transOrderListTable.getColumnModel().getColumn(1).setMinWidth(125);
            transOrderListTable.getColumnModel().getColumn(1).setPreferredWidth(125);
            transOrderListTable.getColumnModel().getColumn(1).setMaxWidth(125);
        }

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        transactionDetailsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PRODUCT", "QTY", "UNIT", "PRICE", "SUBTOTAL"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane6.setViewportView(transactionDetailsTable);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        transOrderTypeL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transOrderTypeL.setText("walk_in");

        jLabel39.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel39.setText("Order type");

        jLabel29.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel29.setText("Change");

        transChangeL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transChangeL.setText("0");

        transPaymentL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transPaymentL.setText("69696969.69");

        jLabel28.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel28.setText("Payment");

        jLabel27.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel27.setText("Customer");

        transCustFNL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transCustFNL.setText("FirstN LastN");

        transCashierFNL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transCashierFNL.setText("Liza Gomez");

        jLabel26.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel26.setText("Cashier");

        jLabel24.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel24.setText("Date & Time");

        transDateTimeL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transDateTimeL.setText("2026-10-10");

        transOrderIDL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transOrderIDL.setText("69");

        jLabel23.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel23.setText("Order ID");

        jLabel22.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel22.setText("TRANSACTION DETAILS");

        jLabel37.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel37.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel37.setText("TOTAL:");

        transTotalTF.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        transTotalTF.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        transTotalTF.setText("₱ 0");

        transReprintReceiptBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        transReprintReceiptBtn.setText("REPRINT RECEIPT");
        transReprintReceiptBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transReprintReceiptBtnActionPerformed(evt);
            }
        });

        transPrintListBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        transPrintListBtn.setText("PRINT LIST");
        transPrintListBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transPrintListBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel29)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transChangeL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel22)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transPaymentL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(10, 10, 10))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel23)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transOrderIDL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel24)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transDateTimeL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel26)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transCashierFNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel27)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transCustFNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel39)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(transOrderTypeL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(transReprintReceiptBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transPrintListBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(transTotalTF, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel37, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel22)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(transOrderIDL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(transDateTimeL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(transCashierFNL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel27)
                    .addComponent(transCustFNL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(transPaymentL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(transChangeL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel39)
                    .addComponent(transOrderTypeL))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel37)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(transTotalTF)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transReprintReceiptBtn)
                    .addComponent(transPrintListBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addComponent(jScrollPane6)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addGap(9, 9, 9))
        );

        transTotalSalesL.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        transTotalSalesL.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        transTotalSalesL.setText("₱ 69.69");

        jLabel31.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel31.setText("TOTAL SALES:");

        transactionsCountL.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        transactionsCountL.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        transactionsCountL.setText("69");

        jLabel33.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel33.setText("TRANSACTIONS:");

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        transRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        transRefreshBtn.setText("REFRESH");
        transRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transRefreshBtnActionPerformed(evt);
            }
        });

        jLabel21.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel21.setText("Cashier:");

        transToDC.setDateFormatString("yyyy-MM-dd");

        jLabel20.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel20.setText("To:");

        transFromDC.setDateFormatString("yyyy-MM-dd");

        jLabel19.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel19.setText("From:");

        transOrderIDTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel18.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel18.setText("Order ID:");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18)
                .addGap(2, 2, 2)
                .addComponent(transOrderIDTF, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(jLabel19)
                .addGap(3, 3, 3)
                .addComponent(transFromDC, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addGap(1, 1, 1)
                .addComponent(transToDC, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(jLabel21)
                .addGap(4, 4, 4)
                .addComponent(transCashierCB, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(transRefreshBtn)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(transToDC, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transFromDC, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transOrderIDTF, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transCashierCB, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transRefreshBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        transFromDC.getAccessibleContext().setAccessibleName("");

        javax.swing.GroupLayout transactionBGLayout = new javax.swing.GroupLayout(transactionBG);
        transactionBG.setLayout(transactionBGLayout);
        transactionBGLayout.setHorizontalGroup(
            transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transactionBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transactionBGLayout.createSequentialGroup()
                        .addGap(571, 571, 571)
                        .addComponent(jLabel33, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transactionsCountL, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel31, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transTotalSalesL, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        transactionBGLayout.setVerticalGroup(
            transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transactionBGLayout.createSequentialGroup()
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addGroup(transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transTotalSalesL)
                    .addComponent(jLabel31)
                    .addComponent(transactionsCountL)
                    .addComponent(jLabel33))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout transactionTabLayout = new javax.swing.GroupLayout(transactionTab);
        transactionTab.setLayout(transactionTabLayout);
        transactionTabLayout.setHorizontalGroup(
            transactionTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transactionBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        transactionTabLayout.setVerticalGroup(
            transactionTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transactionBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("TRANSACTION", transactionTab);

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));

        jLabel30.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel30.setText("Search:");

        customerSearchTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        customerSearchTF.setToolTipText("name, phone or email");
        customerSearchTF.setName(""); // NOI18N

        jLabel34.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel34.setText("CUSTOMERS:");

        customerCountL.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        customerCountL.setText("0");

        customerRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        customerRefreshBtn.setText("REFRESH");
        customerRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customerRefreshBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel30)
                .addGap(2, 2, 2)
                .addComponent(customerSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel34)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customerCountL, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(customerRefreshBtn)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(customerSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30)
                    .addComponent(jLabel34)
                    .addComponent(customerCountL)
                    .addComponent(customerRefreshBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        customerSearchTF.getAccessibleContext().setAccessibleDescription("");

        customerTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "FULL NAME", "BIRTH DATE", "AGE", "PHONE", "EMAIL", "ADDED BY", "DATE ADDED"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane9.setViewportView(customerTable);

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));

        customerAddBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        customerAddBtn.setText("ADD");
        customerAddBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customerAddBtnActionPerformed(evt);
            }
        });

        customerEditBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        customerEditBtn.setText("EDIT");
        customerEditBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customerEditBtnActionPerformed(evt);
            }
        });

        customerDeleteBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        customerDeleteBtn.setText("DELETE");
        customerDeleteBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customerDeleteBtnActionPerformed(evt);
            }
        });

        customerPrintBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        customerPrintBtn.setText("PRINT");
        customerPrintBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customerPrintBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(customerAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customerEditBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customerDeleteBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customerPrintBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(279, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(customerAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customerEditBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customerDeleteBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customerPrintBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout customerBGLayout = new javax.swing.GroupLayout(customerBG);
        customerBG.setLayout(customerBGLayout);
        customerBGLayout.setHorizontalGroup(
            customerBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(customerBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9)
                .addContainerGap())
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        customerBGLayout.setVerticalGroup(
            customerBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customerBGLayout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout customersTabLayout = new javax.swing.GroupLayout(customersTab);
        customersTab.setLayout(customersTabLayout);
        customersTabLayout.setHorizontalGroup(
            customersTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customerBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        customersTabLayout.setVerticalGroup(
            customersTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customerBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("CUSTOMERS", customersTab);

        javax.swing.GroupLayout prescriptionTabLayout = new javax.swing.GroupLayout(prescriptionTab);
        prescriptionTab.setLayout(prescriptionTabLayout);
        prescriptionTabLayout.setHorizontalGroup(
            prescriptionTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1051, Short.MAX_VALUE)
        );
        prescriptionTabLayout.setVerticalGroup(
            prescriptionTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 644, Short.MAX_VALUE)
        );

        tabPannel.addTab("PRESCRIPTIONS", prescriptionTab);

        headBG.setBackground(new java.awt.Color(255, 255, 255));

        adminRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        adminRefreshBtn.setText("REFRESH");
        adminRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminRefreshBtnActionPerformed(evt);
            }
        });

        adminAccountsCountL3.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        adminAccountsCountL3.setText("0");

        jLabel40.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel40.setText("CASHIER:");

        adminAccountsCountL2.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        adminAccountsCountL2.setText("0");

        jLabel38.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel38.setText("PHARMACIST:");

        adminAccountsCountL1.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        adminAccountsCountL1.setText("0");

        jLabel36.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel36.setText("ADMIN:");

        adminAccountsCountL.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        adminAccountsCountL.setText("0");

        jLabel35.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel35.setText("ACCOUNTS:");

        jLabel32.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel32.setText("Role:");

        transOrderIDTF1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel25.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel25.setText("Search:");

        javax.swing.GroupLayout headBGLayout = new javax.swing.GroupLayout(headBG);
        headBG.setLayout(headBGLayout);
        headBGLayout.setHorizontalGroup(
            headBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel25)
                .addGap(2, 2, 2)
                .addComponent(transOrderIDTF1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jLabel32)
                .addGap(1, 1, 1)
                .addComponent(transCashierCB1, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(37, 37, 37)
                .addComponent(jLabel35)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminAccountsCountL, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel36)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminAccountsCountL1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel38)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminAccountsCountL2, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel40)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminAccountsCountL3, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                .addComponent(adminRefreshBtn)
                .addContainerGap())
        );
        headBGLayout.setVerticalGroup(
            headBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(headBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel32)
                    .addComponent(transCashierCB1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transOrderIDTF1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25)
                    .addComponent(jLabel35)
                    .addComponent(adminRefreshBtn)
                    .addComponent(adminAccountsCountL)
                    .addGroup(headBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel36)
                        .addComponent(adminAccountsCountL1)
                        .addComponent(jLabel38)
                        .addComponent(adminAccountsCountL2)
                        .addComponent(jLabel40)
                        .addComponent(adminAccountsCountL3)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        footer.setBackground(new java.awt.Color(255, 255, 255));

        adminAddBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        adminAddBtn.setText("ADD");
        adminAddBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminAddBtnActionPerformed(evt);
            }
        });

        adminEditBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        adminEditBtn.setText("EDIT");
        adminEditBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminEditBtnActionPerformed(evt);
            }
        });

        adminDeleteBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        adminDeleteBtn.setText("DELETE");
        adminDeleteBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminDeleteBtnActionPerformed(evt);
            }
        });

        adminResetPassBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        adminResetPassBtn.setText("RESET PASSWORD");
        adminResetPassBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminResetPassBtnActionPerformed(evt);
            }
        });

        adminPrintBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        adminPrintBtn.setText("PRINT");
        adminPrintBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminPrintBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout footerLayout = new javax.swing.GroupLayout(footer);
        footer.setLayout(footerLayout);
        footerLayout.setHorizontalGroup(
            footerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(footerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(adminAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminEditBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminDeleteBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminResetPassBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(adminPrintBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        footerLayout.setVerticalGroup(
            footerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(footerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(footerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(adminAddBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(adminEditBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(adminDeleteBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(adminResetPassBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(adminPrintBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        adminAccountTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "USERNAME", "FULLNAME", "ROLE", "CREATED BY", "CREATED AT", "PASSWORD"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane8.setViewportView(adminAccountTable);

        javax.swing.GroupLayout accountsBGLayout = new javax.swing.GroupLayout(accountsBG);
        accountsBG.setLayout(accountsBGLayout);
        accountsBGLayout.setHorizontalGroup(
            accountsBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(headBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(footer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(accountsBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8)
                .addContainerGap())
        );
        accountsBGLayout.setVerticalGroup(
            accountsBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, accountsBGLayout.createSequentialGroup()
                .addComponent(headBG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(footer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout accountsTabLayout = new javax.swing.GroupLayout(accountsTab);
        accountsTab.setLayout(accountsTabLayout);
        accountsTabLayout.setHorizontalGroup(
            accountsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(accountsBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        accountsTabLayout.setVerticalGroup(
            accountsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(accountsBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabPannel.addTab("ADMIN", accountsTab);

        javax.swing.GroupLayout backgroundLayout = new javax.swing.GroupLayout(background);
        background.setLayout(backgroundLayout);
        backgroundLayout.setHorizontalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(header, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(tabPannel)
        );
        backgroundLayout.setVerticalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundLayout.createSequentialGroup()
                .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabPannel))
        );

        tabPannel.getAccessibleContext().setAccessibleDescription("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, 1056, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, 771, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void salesRemoveAllBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesRemoveAllBtnActionPerformed
        bSales.removeAllItems(this, salesUI);
    }//GEN-LAST:event_salesRemoveAllBtnActionPerformed

    private void salesRemoveItemBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesRemoveItemBtnActionPerformed
        bSales.removeSelectedItem(this, salesUI);
    }//GEN-LAST:event_salesRemoveItemBtnActionPerformed

    private void cashAmountSalesTFKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cashAmountSalesTFKeyReleased
        bSales.recalcChange(salesUI);
    }//GEN-LAST:event_cashAmountSalesTFKeyReleased

    private void salesProductIDScanTFKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_salesProductIDScanTFKeyReleased
        bSales.scanProduct(this, salesUI);
    }//GEN-LAST:event_salesProductIDScanTFKeyReleased

    private void salesCancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesCancelBtnActionPerformed
        bSales.clearScanFields(salesUI);
        salesProductIDScanTF.requestFocus();
    }//GEN-LAST:event_salesCancelBtnActionPerformed

    private void salesAddBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesAddBtnActionPerformed
        bSales.addItem(this, salesUI);
        salesProductIDScanTF.requestFocus();
    }//GEN-LAST:event_salesAddBtnActionPerformed

    private void salesCheckoutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesCheckoutBtnActionPerformed
        bSales.CheckoutResult result = bSales.checkoutSale(this, salesUI);
        if (result == null) return;

        orderId = result.orderId;
        setStockTable();
        bPrint.printSale(result);
        bSales.clearSale(salesUI);
        salesProductIDScanTF.requestFocus();
    }//GEN-LAST:event_salesCheckoutBtnActionPerformed

    private void stockRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stockRefreshBtnActionPerformed
        setStockTable();
    }//GEN-LAST:event_stockRefreshBtnActionPerformed

    private void printStockBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printStockBtnActionPerformed
        try {
            setStockTable();
            MessageFormat header = new MessageFormat("Stock List");
            MessageFormat footer = new MessageFormat("-{0}-");
            stockTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_printStockBtnActionPerformed

    private void deleteStockBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteStockBtnActionPerformed
        int i = stockTable.getSelectedRow();
        if (i == -1) {
            JOptionPane.showMessageDialog(null, "Please select a stock entry to delete.");
            return;
        }
 
        int x = JOptionPane.showConfirmDialog(null, "DO YOU WANT TO DELETE THIS STOCK ENTRY?", "CONFIRMATION", 0);
        if (x != 0) {
            return;
        }
 
        try {
            int stockId = Integer.parseInt(stockTable.getValueAt(i, 0).toString());
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mediscan_pos", "root", "");
            PreparedStatement ps = con.prepareStatement("DELETE FROM product_stock WHERE stock_id = ?");
            ps.setInt(1, stockId);
            ps.executeUpdate();
            ps.close();
            con.close();
 
            JOptionPane.showMessageDialog(null, "Delete successful");
            setStockTable();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Delete failed: " + e.getMessage());
        }
    }//GEN-LAST:event_deleteStockBtnActionPerformed

    private void editStockBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editStockBtnActionPerformed
        int i = stockTable.getSelectedRow();
        if (i == -1) {
            JOptionPane.showMessageDialog(null, "Please select a stock entry to edit.");
            return;
        }
        int stockId = Integer.parseInt(stockTable.getValueAt(i, 0).toString());
 
        Stock stk = new Stock(this, true, stockId);
        stk.setVisible(true);
        setStockTable();
    }//GEN-LAST:event_editStockBtnActionPerformed

    private void addStockBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStockBtnActionPerformed
        Stock stk = new Stock(this, true);
        stk.setVisible(true);
        setStockTable();
    }//GEN-LAST:event_addStockBtnActionPerformed

    private void productsRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_productsRefreshBtnActionPerformed
        setCategoryTable();
    }//GEN-LAST:event_productsRefreshBtnActionPerformed

    private void printProductBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printProductBtnActionPerformed
        try {
            setProductTable();
            MessageFormat header = new MessageFormat("Product List");
            MessageFormat footer = new MessageFormat("-{0}-");
            productsTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_printProductBtnActionPerformed

    private void deleteProductBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteProductBtnActionPerformed
        int i = productsTable.getSelectedRow();
        if (i == -1) {
            JOptionPane.showMessageDialog(null, "Please select a product to delete.");
            return;
        }
 
        int x = JOptionPane.showConfirmDialog(null, "DO YOU WANT TO DELETE THIS PRODUCT?", "CONFIRMATION", 0);
        if (x != 0) {
            return;
        }
 
        try {
            int productId = Integer.parseInt(productsTable.getValueAt(i, 0).toString());
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mediscan_pos", "root", "");
            PreparedStatement ps = con.prepareStatement("DELETE FROM products WHERE product_id = ?");
            ps.setInt(1, productId);
            ps.executeUpdate();
            ps.close();
            con.close();
 
            JOptionPane.showMessageDialog(null, "Delete successful");
            setProductTable();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Cannot delete: this product has related stock or order records.");
        }
    }//GEN-LAST:event_deleteProductBtnActionPerformed

    private void editProductBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProductBtnActionPerformed
        int i = productsTable.getSelectedRow();
        if (i == -1) {
            JOptionPane.showMessageDialog(null, "Please select a product to edit.");
            return;
        }
        int productId = Integer.parseInt(productsTable.getValueAt(i, 0).toString());
 
        Product prod = new Product(this, true, productId);
        prod.setVisible(true);
        setProductTable();
    }//GEN-LAST:event_editProductBtnActionPerformed

    private void addProductBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addProductBtnActionPerformed
        Product prod = new Product(this, true);
        prod.setVisible(true);
        setProductTable();
    }//GEN-LAST:event_addProductBtnActionPerformed

    private void categoryRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categoryRefreshBtnActionPerformed
        setCategoryTable();
    }//GEN-LAST:event_categoryRefreshBtnActionPerformed

    private void printCategoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printCategoryBtnActionPerformed
        bCategory.printCategory(categoryTable);
    }//GEN-LAST:event_printCategoryBtnActionPerformed

    private void deleteCategoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteCategoryBtnActionPerformed
       bCategory.deleteCategory(categoryTable);
    }//GEN-LAST:event_deleteCategoryBtnActionPerformed

    private void editCategoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editCategoryBtnActionPerformed
        bCategory.editCategory(this, categoryTable);
    }//GEN-LAST:event_editCategoryBtnActionPerformed

    private void addCategoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCategoryBtnActionPerformed
        bCategory.addCategory(this, categoryTable);
    }//GEN-LAST:event_addCategoryBtnActionPerformed

    private void logoutDashboardBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutDashboardBtnActionPerformed
        Session.clear();
        new Login().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_logoutDashboardBtnActionPerformed

    private void genBarProductBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genBarProductBtnActionPerformed
        generateMissingBarcodes();
    }//GEN-LAST:event_genBarProductBtnActionPerformed

    private void transRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transRefreshBtnActionPerformed
        bTransaction.setOrders(transOrderListTable, transOrderIDTF.getText(),
            transFromDC.getDate(), transToDC.getDate(),
            (String) transCashierCB.getSelectedItem());
        reloadOrders();
    }//GEN-LAST:event_transRefreshBtnActionPerformed

    private void transReprintReceiptBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transReprintReceiptBtnActionPerformed
         bPrint.reprintReceipt(this, bTransaction.getSelectedOrderId(transOrderListTable));
    }//GEN-LAST:event_transReprintReceiptBtnActionPerformed

    private void transPrintListBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transPrintListBtnActionPerformed
        bTransaction.printList(transOrderListTable);
    }//GEN-LAST:event_transPrintListBtnActionPerformed

    private void tabPannelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPannelStateChanged
        int selectedIndex = tabPannel.getSelectedIndex();
        if (tabPannel.getTitleAt(selectedIndex) == "SALES") {
            salesProductIDScanTF.requestFocus();
        }
    }//GEN-LAST:event_tabPannelStateChanged

    private void adminRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminRefreshBtnActionPerformed
        bAccount.refresh(accountUI);
    }//GEN-LAST:event_adminRefreshBtnActionPerformed

    private void adminAddBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminAddBtnActionPerformed
        new Call.addAccount(this, true).setVisible(true);
        bAccount.setAccounts(accountUI);
    }//GEN-LAST:event_adminAddBtnActionPerformed

    private void adminEditBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminEditBtnActionPerformed
        int id = bAccount.getSelectedUserId(adminAccountTable);
        if (id == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please select an account to edit.");
            return;
        }
        new Call.addAccount(this, true, id).setVisible(true);
        bAccount.setAccounts(accountUI);
    }//GEN-LAST:event_adminEditBtnActionPerformed

    private void adminDeleteBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminDeleteBtnActionPerformed
        bAccount.deleteAccount(this, accountUI);
    }//GEN-LAST:event_adminDeleteBtnActionPerformed

    private void adminResetPassBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminResetPassBtnActionPerformed
        int id = bAccount.getSelectedUserId(adminAccountTable);
        if (id == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please select an account to reset.");
            return;
        }
        new Call.resetPassword(this, true, id).setVisible(true);
        bAccount.setAccounts(accountUI);
    }//GEN-LAST:event_adminResetPassBtnActionPerformed

    private void adminPrintBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminPrintBtnActionPerformed
        bAccount.printAccounts(accountUI);
    }//GEN-LAST:event_adminPrintBtnActionPerformed

    private void customerRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customerRefreshBtnActionPerformed
        bCustomer.refresh(customerUI);
    }//GEN-LAST:event_customerRefreshBtnActionPerformed

    private void customerAddBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customerAddBtnActionPerformed
        new Call.addCustomer(this, true).setVisible(true);
        bCustomer.setCustomers(customerUI);
    }//GEN-LAST:event_customerAddBtnActionPerformed

    private void customerEditBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customerEditBtnActionPerformed
        int id = bCustomer.getSelectedCustomerId(customerTable);
        if (id == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please select a customer to edit.");
            return;
        }
        new Call.addCustomer(this, true, id).setVisible(true);
        bCustomer.setCustomers(customerUI);
    }//GEN-LAST:event_customerEditBtnActionPerformed

    private void customerDeleteBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customerDeleteBtnActionPerformed
        bCustomer.deleteCustomer(this, customerUI);
    }//GEN-LAST:event_customerDeleteBtnActionPerformed

    private void customerPrintBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customerPrintBtnActionPerformed
        bCustomer.printCustomers(customerUI);
    }//GEN-LAST:event_customerPrintBtnActionPerformed

    private void salesCustomerIDTFKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_salesCustomerIDTFKeyReleased
        bSales.lookupCustomer(salesUI);
    }//GEN-LAST:event_salesCustomerIDTFKeyReleased

    private void dashRefreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashRefreshBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dashRefreshBtnActionPerformed

    private void dashTodayBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashTodayBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dashTodayBtnActionPerformed

    private void dash7DaysBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dash7DaysBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dash7DaysBtnActionPerformed

    private void dash30DaysBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dash30DaysBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dash30DaysBtnActionPerformed

    private void dashSalesReportBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashSalesReportBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dashSalesReportBtnActionPerformed

    private void dashStockReportBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashStockReportBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dashStockReportBtnActionPerformed

    private void dashExpiryReportBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dashExpiryReportBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dashExpiryReportBtnActionPerformed

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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new Dashboard().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accountsBG;
    private javax.swing.JPanel accountsTab;
    private javax.swing.JButton addCategoryBtn;
    private javax.swing.JButton addProductBtn;
    private javax.swing.JButton addStockBtn;
    private javax.swing.JTable adminAccountTable;
    private javax.swing.JLabel adminAccountsCountL;
    private javax.swing.JLabel adminAccountsCountL1;
    private javax.swing.JLabel adminAccountsCountL2;
    private javax.swing.JLabel adminAccountsCountL3;
    private javax.swing.JButton adminAddBtn;
    private javax.swing.JButton adminDeleteBtn;
    private javax.swing.JButton adminEditBtn;
    private javax.swing.JButton adminPrintBtn;
    private javax.swing.JButton adminRefreshBtn;
    private javax.swing.JButton adminResetPassBtn;
    private javax.swing.JPanel background;
    private javax.swing.JTextField cashAmountSalesTF;
    private javax.swing.JPanel categoryBG;
    private javax.swing.JButton categoryRefreshBtn;
    private javax.swing.JTextField categorySearchTF;
    private javax.swing.JPanel categoryTab;
    private javax.swing.JTable categoryTable;
    private javax.swing.JTextField changeSalesTF;
    private javax.swing.JButton customerAddBtn;
    private javax.swing.JPanel customerBG;
    private javax.swing.JLabel customerCountL;
    private javax.swing.JButton customerDeleteBtn;
    private javax.swing.JButton customerEditBtn;
    private javax.swing.JButton customerPrintBtn;
    private javax.swing.JButton customerRefreshBtn;
    private javax.swing.JTextField customerSearchTF;
    private javax.swing.JTable customerTable;
    private javax.swing.JPanel customersTab;
    private javax.swing.JButton dash30DaysBtn;
    private javax.swing.JButton dash7DaysBtn;
    private javax.swing.JLabel dashExpiredRightTF;
    private javax.swing.JLabel dashExpiringLeftTF;
    private javax.swing.JLabel dashExpiringLeftTF1;
    private javax.swing.JTable dashExpiringSoonTable;
    private javax.swing.JButton dashExpiryReportBtn;
    private com.toedter.calendar.JDateChooser dashFromDC;
    private javax.swing.JLabel dashItemSoldTF;
    private javax.swing.JLabel dashLowStockCatTF;
    private javax.swing.JTable dashLowStockTable;
    private javax.swing.JButton dashRefreshBtn;
    private javax.swing.JPanel dashSalesByCategoryP;
    private javax.swing.JPanel dashSalesPerDayP;
    private javax.swing.JButton dashSalesReportBtn;
    private javax.swing.JButton dashStockReportBtn;
    private com.toedter.calendar.JDateChooser dashToDC;
    private javax.swing.JButton dashTodayBtn;
    private javax.swing.JPanel dashTop5ProductsP;
    private javax.swing.JLabel dashTotalSalesTF;
    private javax.swing.JLabel dashTransactionsTF;
    private javax.swing.JPanel dashboardBG;
    private javax.swing.JButton deleteCategoryBtn;
    private javax.swing.JButton deleteProductBtn;
    private javax.swing.JButton deleteStockBtn;
    private javax.swing.JButton editCategoryBtn;
    private javax.swing.JButton editProductBtn;
    private javax.swing.JButton editStockBtn;
    private javax.swing.JPanel footer;
    private javax.swing.JButton genBarProductBtn;
    private javax.swing.JPanel headBG;
    private javax.swing.JPanel header;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JButton logoutDashboardBtn;
    private javax.swing.JPanel mainDashboardTab;
    private javax.swing.JPanel prescriptionTab;
    private javax.swing.JButton printCategoryBtn;
    private javax.swing.JButton printProductBtn;
    private javax.swing.JButton printStockBtn;
    private javax.swing.JPanel productBG;
    private javax.swing.JComboBox<String> productCategoryCB;
    private javax.swing.JTextField productSearchTF;
    private javax.swing.JPanel productTab;
    private javax.swing.JButton productsRefreshBtn;
    private javax.swing.JTable productsTable;
    private javax.swing.JButton salesAddBtn;
    private javax.swing.JPanel salesBG;
    private javax.swing.JButton salesCancelBtn;
    private javax.swing.JTextField salesCategoryTF;
    private javax.swing.JButton salesCheckoutBtn;
    private javax.swing.JLabel salesCustomerFNL;
    private javax.swing.JTextField salesCustomerIDTF;
    private javax.swing.JTextField salesPriceTF;
    private javax.swing.JTextField salesPriceTF1;
    private javax.swing.JTextField salesProductIDScanTF;
    private javax.swing.JTextField salesProductNameTF;
    private javax.swing.JTextField salesQuantityTF;
    private javax.swing.JButton salesRemoveAllBtn;
    private javax.swing.JButton salesRemoveItemBtn;
    private javax.swing.JPanel salesTab;
    private javax.swing.JTable salesTable;
    private javax.swing.JTextField salesUnitTF;
    private javax.swing.JTextArea salesWarningDesTA;
    private javax.swing.JLabel salesWarningRequireL;
    private javax.swing.JPanel stockBG;
    private javax.swing.JComboBox<String> stockFilterProductCB;
    private javax.swing.JButton stockRefreshBtn;
    private javax.swing.JTextField stockSearchTF;
    private javax.swing.JPanel stockTab;
    private javax.swing.JTable stockTable;
    private javax.swing.JTabbedPane tabPannel;
    private javax.swing.JLabel totalSalesTF;
    private javax.swing.JComboBox<String> transCashierCB;
    private javax.swing.JComboBox<String> transCashierCB1;
    private javax.swing.JLabel transCashierFNL;
    private javax.swing.JLabel transChangeL;
    private javax.swing.JLabel transCustFNL;
    private javax.swing.JLabel transDateTimeL;
    private com.toedter.calendar.JDateChooser transFromDC;
    private javax.swing.JLabel transOrderIDL;
    private javax.swing.JTextField transOrderIDTF;
    private javax.swing.JTextField transOrderIDTF1;
    private javax.swing.JTable transOrderListTable;
    private javax.swing.JLabel transOrderTypeL;
    private javax.swing.JLabel transPaymentL;
    private javax.swing.JButton transPrintListBtn;
    private javax.swing.JButton transRefreshBtn;
    private javax.swing.JButton transReprintReceiptBtn;
    private com.toedter.calendar.JDateChooser transToDC;
    private javax.swing.JLabel transTotalSalesL;
    private javax.swing.JLabel transTotalTF;
    private javax.swing.JPanel transactionBG;
    private javax.swing.JTable transactionDetailsTable;
    private javax.swing.JPanel transactionTab;
    private javax.swing.JLabel transactionsCountL;
    private javax.swing.JLabel userFullNameL;
    private javax.swing.JLabel userRoleL;
    // End of variables declaration//GEN-END:variables
}

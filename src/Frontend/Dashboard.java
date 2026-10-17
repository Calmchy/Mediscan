package Frontend;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Backend.Session;
import Call.Category;
import Call.Product;
import Call.Stock;
import Backend.bSales;

import Backend.bCategory;
import Backend.bProduct;
import Backend.bStocks;
import Backend.bTransaction;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import static java.awt.print.Printable.NO_SUCH_PAGE;
import static java.awt.print.Printable.PAGE_EXISTS;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import static java.util.logging.Level.ALL;

/**
 *
 * @author chyril
 */
public class Dashboard extends javax.swing.JFrame {
    private double bHeight = 0.0;
    private int orderId;
    private static final String BARCODE_FOLDER_PRODUCT = System.getProperty("user.home") + "/MediScanBarcodes/Product/";
    private static final String BARCODE_FOLDER_SALES = System.getProperty("user.home") + "/MediScanBarcodes/Sales/";
    private boolean loadingFilters = false;
    private final bSales.SalesFields salesUI = new bSales.SalesFields();
    
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
                allowed = java.util.Arrays.asList("DASHBOARD", "CATEGORY", "PRODUCTS", "STOCK", "SALES", "TRANSACTION", "ADMIN");
                tabPannel.setSelectedIndex(0);
                break;
            case "pharmacist":
                allowed = java.util.Arrays.asList("DASHBOARD", "CATEGORY", "PRODUCTS", "STOCK", "SALES", "TRANSACTION");
                tabPannel.setSelectedIndex(0);
                break;
            case "cashier":
                allowed = java.util.Arrays.asList("SALES", "TRANSACTION");
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
    
    
    
    private BufferedImage generateBarcodeImage(String data, int width, int height) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
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
    
    public PageFormat getPageFormat(PrinterJob pj) {
        PageFormat pf = pj.defaultPage();
        Paper paper = pf.getPaper();

        double bodyHeight = bHeight;
        double headerHeight = 5.0;
        double footerHeight = 5.0;
        double width = cm_to_pp(8);
        double height = cm_to_pp(headerHeight + bodyHeight + footerHeight);

        paper.setSize(width, height);
        paper.setImageableArea(0, 10, width, height - cm_to_pp(1));
        pf.setOrientation(PageFormat.PORTRAIT);
        pf.setPaper(paper);

        return pf;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        background = new javax.swing.JPanel();
        header = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        logoutDashboardBtn = new javax.swing.JButton();
        userRoleL = new javax.swing.JLabel();
        userFullNameL = new javax.swing.JLabel();
        tabPannel = new javax.swing.JTabbedPane();
        mainDashboardTab = new javax.swing.JPanel();
        categoryTab = new javax.swing.JPanel();
        categoryBG = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        categoryTable = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        categorySearchTF = new javax.swing.JTextField();
        addCategoryBtn = new javax.swing.JButton();
        editCategoryBtn = new javax.swing.JButton();
        deleteCategoryBtn = new javax.swing.JButton();
        printCategoryBtn = new javax.swing.JButton();
        categoryRefreshBtn = new javax.swing.JButton();
        productTab = new javax.swing.JPanel();
        productBG = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        productsTable = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        productSearchTF = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        productCategoryCB = new javax.swing.JComboBox<>();
        addProductBtn = new javax.swing.JButton();
        editProductBtn = new javax.swing.JButton();
        deleteProductBtn = new javax.swing.JButton();
        printProductBtn = new javax.swing.JButton();
        productsRefreshBtn = new javax.swing.JButton();
        genBarProductBtn = new javax.swing.JButton();
        stockTab = new javax.swing.JPanel();
        stockBG = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        stockTable = new javax.swing.JTable();
        stockSearchTF = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        stockFilterProductCB = new javax.swing.JComboBox<>();
        addStockBtn = new javax.swing.JButton();
        editStockBtn = new javax.swing.JButton();
        deleteStockBtn = new javax.swing.JButton();
        printStockBtn = new javax.swing.JButton();
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
        transactionTab = new javax.swing.JPanel();
        transactionBG = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        transOrderIDTF = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        transRefreshBtn = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        transCashierCB = new javax.swing.JComboBox<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        transOrderListTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        transactionDetailsTable = new javax.swing.JTable();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        transOrderIDL = new javax.swing.JLabel();
        transDateTimeL = new javax.swing.JLabel();
        transCashierFNL = new javax.swing.JLabel();
        transCustFNL = new javax.swing.JLabel();
        transPaymentL = new javax.swing.JLabel();
        transChangeL = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        transTotalTF = new javax.swing.JLabel();
        transReprintReceiptBtn = new javax.swing.JButton();
        transPrintListBtn = new javax.swing.JButton();
        jLabel39 = new javax.swing.JLabel();
        transOrderTypeL = new javax.swing.JLabel();
        transFromDC = new com.toedter.calendar.JDateChooser();
        transToDC = new com.toedter.calendar.JDateChooser();
        accountsTab = new javax.swing.JPanel();
        accountsBG = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        transOrderIDTF1 = new javax.swing.JTextField();
        jLabel32 = new javax.swing.JLabel();
        transCashierCB1 = new javax.swing.JComboBox<>();

        jMenuItem1.setText("jMenuItem1");

        jMenu1.setText("jMenu1");

        jMenu2.setText("jMenu2");

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

        javax.swing.GroupLayout mainDashboardTabLayout = new javax.swing.GroupLayout(mainDashboardTab);
        mainDashboardTab.setLayout(mainDashboardTabLayout);
        mainDashboardTabLayout.setHorizontalGroup(
            mainDashboardTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1048, Short.MAX_VALUE)
        );
        mainDashboardTabLayout.setVerticalGroup(
            mainDashboardTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 641, Short.MAX_VALUE)
        );

        tabPannel.addTab("DASHBOARD", mainDashboardTab);

        categoryTable.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        categoryTable.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
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

        jLabel7.setText("Search:");

        addCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        addCategoryBtn.setText("ADD");
        addCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCategoryBtnActionPerformed(evt);
            }
        });

        editCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        editCategoryBtn.setText("EDIT");
        editCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editCategoryBtnActionPerformed(evt);
            }
        });

        deleteCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        deleteCategoryBtn.setText("DELETE");
        deleteCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteCategoryBtnActionPerformed(evt);
            }
        });

        printCategoryBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        printCategoryBtn.setText("PRINT");
        printCategoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printCategoryBtnActionPerformed(evt);
            }
        });

        categoryRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        categoryRefreshBtn.setText("REFRESH");
        categoryRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categoryRefreshBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout categoryBGLayout = new javax.swing.GroupLayout(categoryBG);
        categoryBG.setLayout(categoryBGLayout);
        categoryBGLayout.setHorizontalGroup(
            categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(categoryBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5)
                    .addGroup(categoryBGLayout.createSequentialGroup()
                        .addComponent(addCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(printCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 252, Short.MAX_VALUE))
                    .addGroup(categoryBGLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(3, 3, 3)
                        .addComponent(categorySearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(categoryRefreshBtn)))
                .addContainerGap())
        );
        categoryBGLayout.setVerticalGroup(
            categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, categoryBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(categorySearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(categoryRefreshBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(printCategoryBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(categoryBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(addCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(editCategoryBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(deleteCategoryBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
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

        jLabel5.setText("Search:");

        jLabel6.setText("Category:");

        addProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        addProductBtn.setText("ADD");
        addProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addProductBtnActionPerformed(evt);
            }
        });

        editProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        editProductBtn.setText("EDIT");
        editProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProductBtnActionPerformed(evt);
            }
        });

        deleteProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        deleteProductBtn.setText("DELETE");
        deleteProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteProductBtnActionPerformed(evt);
            }
        });

        printProductBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        printProductBtn.setText("PRINT");
        printProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printProductBtnActionPerformed(evt);
            }
        });

        productsRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        productsRefreshBtn.setText("REFRESH");
        productsRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                productsRefreshBtnActionPerformed(evt);
            }
        });

        genBarProductBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        genBarProductBtn.setText("GENERATE BARCODE");
        genBarProductBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genBarProductBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout productBGLayout = new javax.swing.GroupLayout(productBG);
        productBG.setLayout(productBGLayout);
        productBGLayout.setHorizontalGroup(
            productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(productBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(productBGLayout.createSequentialGroup()
                        .addComponent(addProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(printProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(genBarProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 43, Short.MAX_VALUE))
                    .addGroup(productBGLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(3, 3, 3)
                        .addComponent(productSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addGap(3, 3, 3)
                        .addComponent(productCategoryCB, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(productsRefreshBtn)))
                .addContainerGap())
        );
        productBGLayout.setVerticalGroup(
            productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(productBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(productSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(productCategoryCB, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(productsRefreshBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(productBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(printProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(genBarProductBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        productSearchTF.getAccessibleContext().setAccessibleName("");

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

        jLabel2.setText("Search Batch No:");

        jLabel4.setText("Filter by Product:");

        addStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        addStockBtn.setText("ADD");
        addStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStockBtnActionPerformed(evt);
            }
        });

        editStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        editStockBtn.setText("EDIT");
        editStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editStockBtnActionPerformed(evt);
            }
        });

        deleteStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        deleteStockBtn.setText("DELETE");
        deleteStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteStockBtnActionPerformed(evt);
            }
        });

        printStockBtn.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        printStockBtn.setText("PRINT");
        printStockBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printStockBtnActionPerformed(evt);
            }
        });

        stockRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        stockRefreshBtn.setText("REFRESH");
        stockRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stockRefreshBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout stockBGLayout = new javax.swing.GroupLayout(stockBG);
        stockBG.setLayout(stockBGLayout);
        stockBGLayout.setHorizontalGroup(
            stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, stockBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, stockBGLayout.createSequentialGroup()
                        .addComponent(addStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(printStockBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 252, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, stockBGLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel2)
                        .addGap(6, 6, 6)
                        .addComponent(stockSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addGap(3, 3, 3)
                        .addComponent(stockFilterProductCB, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(stockRefreshBtn)))
                .addContainerGap())
        );
        stockBGLayout.setVerticalGroup(
            stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stockBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stockFilterProductCB, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel2)
                    .addComponent(stockSearchTF, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stockRefreshBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(stockBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(printStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(deleteStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(editStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(addStockBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
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

        salesProductNameTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel10.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel10.setText("CATEGORY");

        salesCategoryTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel11.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel11.setText("PRICE");

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

        salesPriceTF1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel13.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel13.setText("BRAND");

        jLabel17.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel17.setText("UNIT");

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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        salesBGLayout.setVerticalGroup(
            salesBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(salesBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
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

        jLabel18.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel18.setText("Order ID:");

        transOrderIDTF.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel19.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel19.setText("From:");

        jLabel20.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel20.setText("To:");

        transRefreshBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        transRefreshBtn.setText("REFRESH");
        transRefreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transRefreshBtnActionPerformed(evt);
            }
        });

        jLabel21.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel21.setText("Cashier:");

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

        jLabel22.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel22.setText("TRANSACTION DETAILS");

        jLabel23.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel23.setText("Order ID");

        jLabel24.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel24.setText("Date & TIME");

        jLabel26.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel26.setText("Cashier");

        jLabel27.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel27.setText("Customer");

        jLabel28.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel28.setText("Payment");

        jLabel29.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel29.setText("Change");

        transOrderIDL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transOrderIDL.setText("69");

        transDateTimeL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transDateTimeL.setText("2026-10-10");

        transCashierFNL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transCashierFNL.setText("Liza Gomez");

        transCustFNL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transCustFNL.setText("FirstN LastN");

        transPaymentL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transPaymentL.setText("69696969.69");

        transChangeL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transChangeL.setText("0");

        jLabel37.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel37.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel37.setText("TOTAL:");

        transTotalTF.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        transTotalTF.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        transTotalTF.setText("₱ 69696969.69");

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

        jLabel39.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel39.setText("Order type");

        transOrderTypeL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        transOrderTypeL.setText("walk_in");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addGap(29, 29, 29)
                        .addComponent(transOrderIDL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transDateTimeL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel26)
                        .addGap(32, 32, 32)
                        .addComponent(transCashierFNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addGap(26, 26, 26)
                        .addComponent(transCustFNL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addGap(33, 33, 33)
                        .addComponent(transPaymentL, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel29)
                        .addGap(20, 20, 20)
                        .addComponent(transChangeL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel39)
                        .addGap(20, 20, 20)
                        .addComponent(transOrderTypeL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(transReprintReceiptBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transPrintListBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(34, 34, 34)
                        .addComponent(jLabel37, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(transTotalTF, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 739, Short.MAX_VALUE)
                        .addGap(1, 1, 1)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23)
                            .addComponent(transOrderIDL))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel24)
                            .addComponent(transDateTimeL))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel26)
                            .addComponent(transCashierFNL))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel27)
                            .addComponent(transCustFNL))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel28)
                            .addComponent(transPaymentL))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel29)
                            .addComponent(transChangeL))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel39)
                            .addComponent(transOrderTypeL))
                        .addGap(0, 19, Short.MAX_VALUE))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(transPrintListBtn)
                    .addComponent(transReprintReceiptBtn)
                    .addComponent(transTotalTF)
                    .addComponent(jLabel37, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        transFromDC.setDateFormatString("yyyy-MM-dd");

        transToDC.setDateFormatString("yyyy-MM-dd");

        javax.swing.GroupLayout transactionBGLayout = new javax.swing.GroupLayout(transactionBG);
        transactionBG.setLayout(transactionBGLayout);
        transactionBGLayout.setHorizontalGroup(
            transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transactionBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, transactionBGLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addGap(2, 2, 2)
                        .addComponent(transOrderIDTF, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel19)
                        .addGap(3, 3, 3)
                        .addComponent(transFromDC, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel20)
                        .addGap(1, 1, 1)
                        .addComponent(transToDC, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel21)
                        .addGap(4, 4, 4)
                        .addComponent(transCashierCB, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(transRefreshBtn)))
                .addContainerGap())
        );
        transactionBGLayout.setVerticalGroup(
            transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transactionBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transactionBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(transToDC, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transFromDC, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transOrderIDTF, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transCashierCB, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transRefreshBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addGap(65, 65, 65)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        transFromDC.getAccessibleContext().setAccessibleName("");

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

        jLabel25.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel25.setText("Search:");

        transOrderIDTF1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jLabel32.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel32.setText("Role:");

        javax.swing.GroupLayout accountsBGLayout = new javax.swing.GroupLayout(accountsBG);
        accountsBG.setLayout(accountsBGLayout);
        accountsBGLayout.setHorizontalGroup(
            accountsBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountsBGLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel25)
                .addGap(2, 2, 2)
                .addComponent(transOrderIDTF1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jLabel32)
                .addGap(1, 1, 1)
                .addComponent(transCashierCB1, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(669, Short.MAX_VALUE))
        );
        accountsBGLayout.setVerticalGroup(
            accountsBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accountsBGLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(accountsBGLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel32)
                    .addComponent(transCashierCB1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transOrderIDTF1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25))
                .addContainerGap(601, Short.MAX_VALUE))
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
            .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        ArrayList<String> rItemName = new ArrayList<>(), rQty = new ArrayList<>(), rPrice = new ArrayList<>(), rUnit = new ArrayList<>(), rSubtotal = new ArrayList<>();
        for (bSales.CartItem it : result.items) {
            rItemName.add(it.name);
            rQty.add(String.valueOf(it.quantity));
            rPrice.add(String.valueOf(it.price));
            rUnit.add(it.unit);
            rSubtotal.add(String.valueOf(it.subtotal()));
        }

        bHeight = rItemName.size();
        PrinterJob pj = PrinterJob.getPrinterJob();
        saveBarcodeToFile(String.valueOf(orderId), BARCODE_FOLDER_SALES);
        pj.setPrintable(new BillPrintable(rItemName, rQty, rPrice, rUnit, rSubtotal,
                result.total, result.cash, result.change, orderId), getPageFormat(pj));
        try {
            pj.print();
        } catch (PrinterException ex) {
            ex.printStackTrace();
        }

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
         bTransaction.reprintReceipt(bTransaction.getSelectedOrderId(transOrderListTable), transactionDetailsTable,
            transDateTimeL.getText(), transCashierFNL.getText(), transCustFNL.getText(),
            transPaymentL.getText(), transChangeL.getText(), transTotalTF.getText());
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

    public class BillPrintable implements Printable {
        private ArrayList<String> itemName, quantity, itemPrice, unit, subtotal;
        private double total, cash, change;
        private int orderId;

        public BillPrintable(ArrayList<String> itemName, ArrayList<String> quantity, ArrayList<String> itemPrice,
                ArrayList<String> unit, ArrayList<String> subtotal, double total, double cash, double change, int orderId) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.itemPrice = itemPrice;
            this.unit = unit;
            this.subtotal = subtotal;
            this.total = total;
            this.cash = cash;
            this.change = change;
            this.orderId = orderId;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());

            Font normalFont = new Font("Monospaced", Font.PLAIN, 7);
            Font boldFont = new Font("Monospaced", Font.BOLD, 8);
            g2d.setFont(normalFont);
            java.awt.FontMetrics fm = g2d.getFontMetrics();

            // every line on the receipt shares this ONE left edge and ONE right edge
            int margin = 12;
            double paperWidth = pageFormat.getImageableWidth();
            double charW = fm.stringWidth(repeatChar('-', 20)) / 20.0;
            int cols = (int) ((paperWidth - (margin * 2)) / charW);
            String divider = repeatChar('-', cols);
            String stars = repeatChar('*', cols);
            int blockWidth = fm.stringWidth(divider);
            int left = (int) ((paperWidth - blockWidth) / 2);
            int right = left + blockWidth;
            int indent = fm.stringWidth("  ");
            int lineH = 10;
            int y = 15;

            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // logo, centered
            java.net.URL logoUrl = getClass().getResource("/Images/logo-128.png");
            if (logoUrl != null) {
                java.awt.Image logo = new ImageIcon(logoUrl).getImage();
                int logoSize = 48;
                g2d.drawImage(logo, left + (blockWidth - logoSize) / 2, y, logoSize, logoSize, null);
                y += logoSize + 10;
            }

            // header, centered
            g2d.setFont(boldFont);
            drawCentered(g2d, "MEDISCAN POS", left, right, y); y += lineH + 1;
            g2d.setFont(normalFont);
            drawCentered(g2d, "Retail Pharmacy", left, right, y); y += lineH;
            drawCentered(g2d, currentDateTime, left, right, y); y += lineH;
            g2d.drawString(divider, left, y); y += lineH + 4;

            // items: name on line 1, "qty unit x price ........ subtotal" on line 2
            for (int i = 0; i < itemName.size(); i++) {
                int qty = Integer.parseInt(quantity.get(i));
                double price = Double.parseDouble(itemPrice.get(i));
                double sub = Double.parseDouble(subtotal.get(i));

                String name = itemName.get(i);
                while (name.length() > 1 && fm.stringWidth(name) > blockWidth) {
                    name = name.substring(0, name.length() - 1);
                }
                g2d.drawString(name, left, y); y += lineH;

                String detail = String.format("%d %s x %.2f", qty, unit.get(i), price);
                String subText = String.format("%.2f", sub);
                g2d.drawString(detail, left + indent, y);
                g2d.drawString(subText, right - fm.stringWidth(subText), y);
                y += lineH + 2;
            }

            // totals: label on the left edge, amount on the right edge
            g2d.drawString(divider, left, y); y += lineH;
            g2d.setFont(boldFont);
            drawLeftRight(g2d, "TOTAL", String.format("%.2f", total), left, right, y); y += lineH;
            g2d.setFont(normalFont);
            drawLeftRight(g2d, "CASH", String.format("%.2f", cash), left, right, y); y += lineH;
            drawLeftRight(g2d, "CHANGE", String.format("%.2f", change), left, right, y); y += lineH;
            g2d.drawString(divider, left, y); y += lineH + 2;

            // thank you
            g2d.drawString(stars, left, y); y += lineH;
            drawCentered(g2d, "THANK YOU, COME AGAIN!", left, right, y); y += lineH;
            g2d.drawString(stars, left, y); y += lineH + 6;

            // barcode, centered and never wider than the receipt block
            String orderIdText = String.valueOf(orderId);
            int barcodeW = Math.min(140, blockWidth);
            int barcodeH = 36;
            BufferedImage barcodeImg = generateBarcodeImage(orderIdText, barcodeW * 3, barcodeH * 3);
            if (barcodeImg != null) {
                g2d.drawImage(barcodeImg, left + (blockWidth - barcodeW) / 2, y, barcodeW, barcodeH, null);
                y += barcodeH + 10;
                drawCentered(g2d, orderIdText, left, right, y);
            }

            return PAGE_EXISTS;
        }

        private String repeatChar(char c, int count) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(c);
            }
            return sb.toString();
        }

        private void drawCentered(Graphics2D g2d, String text, int left, int right, int y) {
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, left + ((right - left) - textWidth) / 2, y);
        }

        private void drawLeftRight(Graphics2D g2d, String leftText, String rightText, int left, int right, int y) {
            g2d.drawString(leftText, left, y);
            g2d.drawString(rightText, right - g2d.getFontMetrics().stringWidth(rightText), y);
        }
    }

    protected static double cm_to_pp(double cm) {
        return toPPI(cm * 0.393600787);
    }

    protected static double toPPI(double inch) {
        return inch * 72d;
    }

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
    private javax.swing.JPanel background;
    private javax.swing.JTextField cashAmountSalesTF;
    private javax.swing.JPanel categoryBG;
    private javax.swing.JButton categoryRefreshBtn;
    private javax.swing.JTextField categorySearchTF;
    private javax.swing.JPanel categoryTab;
    private javax.swing.JTable categoryTable;
    private javax.swing.JTextField changeSalesTF;
    private javax.swing.JButton deleteCategoryBtn;
    private javax.swing.JButton deleteProductBtn;
    private javax.swing.JButton deleteStockBtn;
    private javax.swing.JButton editCategoryBtn;
    private javax.swing.JButton editProductBtn;
    private javax.swing.JButton editStockBtn;
    private javax.swing.JButton genBarProductBtn;
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
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JButton logoutDashboardBtn;
    private javax.swing.JPanel mainDashboardTab;
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
    private javax.swing.JLabel transTotalTF;
    private javax.swing.JPanel transactionBG;
    private javax.swing.JTable transactionDetailsTable;
    private javax.swing.JPanel transactionTab;
    private javax.swing.JLabel userFullNameL;
    private javax.swing.JLabel userRoleL;
    // End of variables declaration//GEN-END:variables
}

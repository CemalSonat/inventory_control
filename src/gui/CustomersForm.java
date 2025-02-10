package gui;

import database.Database;
import models.Customer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;

public class CustomersForm {
    private JTabbedPane customersPane;
    private JPanel panel1;
    private JTextField isim;       // Maps to firstName
    private JTextField soyisim;    // Maps to lastName
    private JTextField contactNo;  // Maps to contactNumber
    private JTextField adres;      // Maps to address
    private JButton temizleButton;
    private JButton kaydetButton;
    private JTable tümMüşteriler;  // JTable to show all customers
    private JTextField müşteriName;   // Maps to firstName for deletion
    private JTextField müşteriSurname; // Maps to lastName for deletion
    private JButton temizleButton1;    // Button to clear fields for deletion
    private JButton silButton;         // Button to delete the customer
    private Database dbConnection;

    public CustomersForm() {
        dbConnection = new Database(); // Initialize the Database connection

        // Set up the customer table
        setUpCustomerTable();

        // Button actions
        temizleButton.addActionListener(e -> clearInputFields());
        temizleButton1.addActionListener(e -> clearDeletionFields());
        kaydetButton.addActionListener(e -> saveCustomer());
        silButton.addActionListener(e -> deleteCustomer());
    }

    private void clearInputFields() {
        isim.setText("");
        soyisim.setText("");
        contactNo.setText("");
        adres.setText("");
    }

    private void clearDeletionFields() {
        müşteriName.setText("");
        müşteriSurname.setText("");
    }

    private void saveCustomer() {
        String firstName = isim.getText().trim();
        String lastName = soyisim.getText().trim();
        String contactNumber = contactNo.getText().trim();
        String address = adres.getText().trim();

        // Validate fields
        if (firstName.isEmpty() || lastName.isEmpty() || contactNumber.isEmpty() || address.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Lütfen tüm alanları doldurun!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create a new Customer object
        Customer customer = new Customer(firstName, lastName, contactNumber, address);

        // Save the customer to the database
        if (dbConnection.connect()) {
            try {
                dbConnection.addCustomer(customer);
                JOptionPane.showMessageDialog(null, "Müşteri başarıyla kaydedildi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                clearInputFields();
                loadCustomerData(); // Reload the customer table data after saving
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Müşteri kaydedilemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                dbConnection.disconnect();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCustomer() {
        String firstName = müşteriName.getText().trim();
        String lastName = müşteriSurname.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Lütfen müşteri adını ve soyadını giriniz!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (dbConnection.connect()) {
            try (Connection connection = dbConnection.getConnection();
                 PreparedStatement pstmt = connection.prepareStatement("DELETE FROM customers WHERE first_name = ? AND last_name = ?")) {

                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Müşteri başarıyla silindi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                    clearDeletionFields();
                    loadCustomerData(); // Reload the customer table data after deletion
                } else {
                    JOptionPane.showMessageDialog(null, "Belirtilen isimde bir müşteri bulunamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Müşteri silinirken bir hata oluştu: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                dbConnection.disconnect();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setUpCustomerTable() {
        // Initialize the JTable with a default table model
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Müşteri No", "İsim", "Soyisim", "Telefon Numarası", "Adres"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Disallow editing the ID column
                return column != 0; // 0 is the ID column, we don't want it editable
            }
        };

        tümMüşteriler.setModel(model);
        loadCustomerData(); // Load data from the database and populate the table

        // Add a TableModelListener to handle updates
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();

                    // Get updated value, column name, and customer ID
                    Object newValue = model.getValueAt(row, column);
                    int customerId = (int) model.getValueAt(row, 0); // ID is in the first column
                    String columnName = model.getColumnName(column);

                    // Update the database
                    try {
                        updateCustomerColumnInDatabase(customerId, columnName, newValue);
                        JOptionPane.showMessageDialog(null, columnName + " başarıyla güncellendi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(null, "Veritabanı güncellemesi sırasında hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Add MouseListener for right-click menu
        tümMüşteriler.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tümMüşteriler.rowAtPoint(e.getPoint());
                    int column = tümMüşteriler.columnAtPoint(e.getPoint());
                    if (row >= 0 && column >= 0) {
                        // Get customer ID from the selected row (assuming it's in the "Müşteri No" column)
                        Object customerId = tümMüşteriler.getValueAt(row, 0); // Assuming customer ID is in the 1st column
                        showContextMenu(e, (int) customerId);
                    }
                }
            }
        });
    }

    private void showContextMenu(MouseEvent e, int customerId) {
        // Create JPopupMenu
        JPopupMenu contextMenu = new JPopupMenu();

        // Add menu item for viewing purchase history
        JMenuItem viewPurchasesItem = new JMenuItem("Satış Geçmişi");
        viewPurchasesItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // Trigger method to show customer purchase history
                try {
                    showCustomerPurchaseHistory(customerId);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Add menu item to context menu
        contextMenu.add(viewPurchasesItem);

        // Show context menu at the point where the right-click occurred
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showCustomerPurchaseHistory(int customerId) throws SQLException {
        // Ensure the connection is open
        if (dbConnection.getConnection() == null || dbConnection.getConnection().isClosed()) {
            if (!dbConnection.connect()) {
                JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Query to get the specific customer purchase history
        String query = "SELECT c.first_name, c.last_name, p.name AS product_name, p.id AS product_id, s.quantity, s.sale_date, s.total_price, s.remaining_balance "
                + "FROM sales s "
                + "JOIN customers c ON s.customer_id = c.id "
                + "JOIN products p ON s.product_id = p.id "
                + "WHERE s.customer_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                // Define column names
                String[] columnNames = {"Müşteri Adı Soyadı", "Ürün Adı", "Ürün No", "Adet", "Satış Tarihi", "Toplam Fiyat", "Kalan Borç"};

                // Build the table model
                DefaultTableModel model = new DefaultTableModel(columnNames, 0);

                // Iterate through the result set and populate the model
                while (rs.next()) {
                    String customerName = rs.getString("first_name") + " " + rs.getString("last_name");
                    String productName = rs.getString("product_name");
                    int productId = rs.getInt("product_id");
                    int quantity = rs.getInt("quantity");

                    // Format sale date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    String saleDate = dateFormat.format(rs.getTimestamp("sale_date"));

                    double totalPrice = rs.getDouble("total_price");
                    double remainingBalance = rs.getDouble("remaining_balance");

                    // Add row to the table model
                    model.addRow(new Object[] {
                            customerName, productName, productId, quantity, saleDate, totalPrice, remainingBalance
                    });
                }

                // Create the table with the model
                JTable historyTable = new JTable(model);
                // Make the table scrollable
                JScrollPane scrollPane = new JScrollPane(historyTable);
                scrollPane.setPreferredSize(new java.awt.Dimension(1200, 700)); // Adjust the size as needed

                // Create a panel to hold the scrollable table and set layout if needed
                JPanel panel = new JPanel();
                panel.setLayout(new java.awt.BorderLayout());
                panel.add(scrollPane, java.awt.BorderLayout.CENTER);

                // Show the dialog with the table
                JOptionPane.showMessageDialog(null, panel,
                        "Müşteri Satın Alma Geçmişi", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching purchase history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        // Build table model from the ResultSet
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Get column names
        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }

        // Get row data
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }

        return model;
    }

    private void loadCustomerData() {
        if (dbConnection.connect()) {
            Connection connection = dbConnection.getConnection();

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, first_name, last_name, phone_number, address FROM customers")) {

                DefaultTableModel model = (DefaultTableModel) tümMüşteriler.getModel();
                model.setRowCount(0); // Clear any existing data

                // Loop through the result set and add rows to the table
                while (rs.next()) {
                    Object[] row = {
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("phone_number"),
                            rs.getString("address")
                    };
                    model.addRow(row);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCustomerColumnInDatabase(int customerId, String displayedColumnName, Object newValue) throws SQLException {
        Connection connection = dbConnection.getConnection();
        if (connection == null || connection.isClosed()) {
            // If connection is closed or null, try reconnecting
            if (!dbConnection.connect()) {
                JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            connection = dbConnection.getConnection(); // Get a valid connection again
        }

        // Map displayed column names to database column names
        String dbColumnName = mapDisplayedToDbColumn(displayedColumnName);

        if (dbColumnName == null) {
            JOptionPane.showMessageDialog(null, "Invalid column name: " + displayedColumnName, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construct the SQL query
        String query = "UPDATE customers SET `" + dbColumnName + "` = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newValue.toString()); // Set the new value
            pstmt.setInt(2, customerId); // Set the customer ID
            pstmt.executeUpdate(); // Execute the update query
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error during update: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        // Don't disconnect in this method, it's handled by dbConnection.disconnect() when appropriate
    }

    private String mapDisplayedToDbColumn(String displayedColumnName) {
        // Map JTable column names to database column names
        switch (displayedColumnName) {
            case "First Name":
                return "first_name";
            case "Last Name":
                return "last_name";
            case "Phone Number":
                return "phone_number";
            case "Address":
                return "address";
            default:
                return null; // Unknown column name
        }
    }

    public JTabbedPane getCustomersPane() {
        return customersPane;
    }
}
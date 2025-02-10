package gui;

import database.Database;
import models.Product;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.sql.*;

public class ProductForm {
    private JTabbedPane productPane; // Already defined in the .form file
    private JPanel panel1;          // Already defined in the .form file
    private JTable table1;          // JTable defined in the .form file
    private JTextField ürünİsmi;
    private JTextField markaİsmi;
    private JTextField alışFiyatı;
    private JTextField satışFiyatı;
    private JTextField stokAdeti;
    private JButton ekleButton;
    private JButton temizleButton;
    private JTextField isim;
    private JButton silButton;
    private JButton temizleButton1;

    public ProductForm() {
        // Set up the JTable with column names
        String[] columnNames = {"Ürün No", "Ürün Adı", "Marka", "Alış Fiyatı", "Satış Fiyatı", "Stok Adeti"};
        DefaultTableModel tableModel = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing for all columns except the "ID" column (index 0)
                return column != 0;
            }
        };
        table1.setModel(tableModel); // Bind table1 to the model

        // Load data into the table
        loadProductData();

        // Add TableModelListener to track changes in the table
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();

                    // Get updated value, column name, and product ID
                    Object newValue = tableModel.getValueAt(row, column);
                    int productId = (int) tableModel.getValueAt(row, 0); // ID is in the first column
                    String columnName = tableModel.getColumnName(column);

                    // Update the database
                    try {
                        updateProductColumnInDatabase(productId, columnName, newValue);
                        JOptionPane.showMessageDialog(null, columnName + " başarıyla güncellendi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(null, "Veritabanı güncellemesi sırasında hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });

        productPane.addComponentListener(new ComponentAdapter() {
        });

        temizleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearInputFields();
            }
        });

        ekleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Parse input values
                    String name = ürünİsmi.getText().trim();
                    String brand = markaİsmi.getText().trim();
                    double buyingPrice = Double.parseDouble(alışFiyatı.getText().trim());
                    double sellingPrice = Double.parseDouble(satışFiyatı.getText().trim());
                    int stock = Integer.parseInt(stokAdeti.getText().trim());

                    // Validate input
                    if (name.isEmpty() || brand.isEmpty() || buyingPrice <= 0 || sellingPrice <= 0 || stock < 0) {
                        JOptionPane.showMessageDialog(null, "Lütfen geçerli bilgiler girin!", "Hata", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Create a product object
                    Product product = new Product(name, brand, buyingPrice, sellingPrice, stock);

                    // Insert product into the database
                    Database dbConnection = new Database();
                    if (dbConnection.connect()) {
                        dbConnection.insertProduct(product);
                        JOptionPane.showMessageDialog(null, "Ürün başarıyla eklendi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);

                        // Clear input fields
                        clearInputFields();

                        // Reload the product table
                        loadProductData();
                    } else {
                        JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Lütfen geçerli sayısal değerler girin!", "Hata", JOptionPane.ERROR_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Veritabanına ekleme sırasında bir hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        silButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String productNameToDelete = isim.getText().trim();

                // Validate the input
                if (productNameToDelete.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Lütfen silmek istediğiniz ürün adını girin!", "Hata", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Confirm deletion
                int confirmation = JOptionPane.showConfirmDialog(null,
                        "Bu ürünü silmek istediğinize emin misiniz?",
                        "Ürün Sil",
                        JOptionPane.YES_NO_OPTION);

                if (confirmation == JOptionPane.YES_OPTION) {
                    try {
                        // Delete the product from the database using the new method
                        Database dbConnection = new Database();
                        if (dbConnection.connect()) {
                            boolean success = dbConnection.deleteProduct(productNameToDelete);  // Call the deleteProduct method

                            if (success) {
                                // Reload the product table after deletion
                                loadProductData();
                                JOptionPane.showMessageDialog(null, "Ürün başarıyla silindi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(null, "Ürün veritabanında bulunamadı ya da bu ürünün satışı gerçekleşmiş!", "Hata", JOptionPane.ERROR_MESSAGE);
                            }

                            // Clear the input field
                            isim.setText("");
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(null, "Veritabanı hatası: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        temizleButton1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isim.setText("");
            }
        });
    }

    private void clearInputFields() {
        ürünİsmi.setText("");
        markaİsmi.setText("");
        alışFiyatı.setText("");
        satışFiyatı.setText("");
        stokAdeti.setText("");
    }

    private void loadProductData() {
        Database dbConnection = new Database();
        if (dbConnection.connect()) {
            try (Connection connection = dbConnection.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, brand, buying_price, selling_price, stock_quantity FROM products")) {

                DefaultTableModel model = (DefaultTableModel) table1.getModel();
                model.setRowCount(0); // Clear the table before populating it

                while (rs.next()) {
                    Object[] row = {
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("brand"),
                            rs.getDouble("buying_price"),
                            rs.getDouble("selling_price"),
                            rs.getInt("stock_quantity")
                    };
                    model.addRow(row);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                dbConnection.disconnect();
            }
        }
    }

    private void updateProductColumnInDatabase(int productId, String displayedColumnName, Object newValue) throws SQLException {
        Database dbConnection = new Database();
        if (dbConnection.connect()) {
            // Map displayed column names to database column names
            String dbColumnName = mapDisplayedToDbColumn(displayedColumnName);

            if (dbColumnName == null) {
                JOptionPane.showMessageDialog(null, "Invalid column name: " + displayedColumnName, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Construct the SQL query
            String query = "UPDATE products SET `" + dbColumnName + "` = ? WHERE id = ?";
            try (Connection connection = dbConnection.getConnection();
                 PreparedStatement pstmt = connection.prepareStatement(query)) {

                // Set the appropriate value type for the column
                if (dbColumnName.equals("name") || dbColumnName.equals("brand")) {
                    pstmt.setString(1, newValue.toString());
                } else if (dbColumnName.equals("buying_price") || dbColumnName.equals("selling_price")) {
                    pstmt.setDouble(1, Double.parseDouble(newValue.toString()));
                } else if (dbColumnName.equals("stock_quantity")) {
                    pstmt.setInt(1, Integer.parseInt(newValue.toString()));
                }

                pstmt.setInt(2, productId); // Set the product ID
                pstmt.executeUpdate(); // Execute the update query
            } finally {
                dbConnection.disconnect(); // Close the database connection
            }
        }
    }

    private String mapDisplayedToDbColumn(String displayedColumnName) {
        // Map JTable column names to database column names
        switch (displayedColumnName) {
            case "Ürün Adı":
                return "name";
            case "Marka":
                return "brand";
            case "Alış Fiyatı":
                return "buying_price";
            case "Satış Fiyatı":
                return "selling_price";
            case "Stok Adeti":
                return "stock_quantity";
            default:
                return null; // Unknown column name
        }
    }

    public JTabbedPane getProductPane() {
        return productPane;
    }
}
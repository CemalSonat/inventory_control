package gui;

import database.Database;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class SaleForm {
    private JPanel panel2;
    private JTabbedPane salePane;
    private JTable satışTablosu;
    private JTextField ürünİsmi;
    private JTextField müşteriAdı;
    private JTextField müşteriSoyadı;
    private JTextField ürünAdeti;
    private JButton kaydetButton;
    private JTextField satışID;
    private JButton temizleButton;
    private JButton temizleButton2;
    private JButton silButton;
    private JCheckBox taksitliÖdemeCheckBox;
    private JTextField peşinatTextField;

    private Database database;

    public SaleForm() {
        database = new Database();
        if (database.connect()) {
            loadSalesTable();
        }

        // Disable the peşinatTextField initially
        peşinatTextField.setEnabled(false);

        kaydetButton.addActionListener(e -> recordSale());
        temizleButton.addActionListener(e -> clearFields());
        temizleButton2.addActionListener(e -> clearFieldsInDelete());
        silButton.addActionListener(e -> deleteSale());
        taksitliÖdemeCheckBox.addActionListener(e -> peşinatTextField.setEnabled(taksitliÖdemeCheckBox.isSelected()));
    }

    private void recordSale() {
        String customerFirstName = müşteriAdı.getText().trim();
        String customerLastName = müşteriSoyadı.getText().trim();
        String productName = ürünİsmi.getText().trim();
        String quantityStr = ürünAdeti.getText().trim();
        boolean isInstallment = taksitliÖdemeCheckBox.isSelected();
        String peşinatStr = peşinatTextField.getText().trim();

        // Validate required fields
        if (productName.isEmpty() || customerFirstName.isEmpty() || customerLastName.isEmpty() || quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Lütfen tüm alanları doldurun!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate quantity
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Ürün adeti geçerli bir sayı olmalıdır ve 0'dan büyük olmalıdır.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate downpayment (peşinat) if installment is selected
        double peşinat = 0;
        if (isInstallment) {
            try {
                peşinat = Double.parseDouble(peşinatStr);
                if (peşinat < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Peşinat geçerli bir sayı olmalıdır ve negatif olamaz.", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            // Retrieve customer ID
            String customerQuery = "SELECT id FROM customers WHERE first_name = ? AND last_name = ?";
            int customerId;
            try (var pstmt = database.getConnection().prepareStatement(customerQuery)) {
                pstmt.setString(1, customerFirstName);
                pstmt.setString(2, customerLastName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        customerId = rs.getInt("id");
                    } else {
                        JOptionPane.showMessageDialog(null, "Müşteri bulunamadı! Önce müşteriyi ekleyin.", "Hata", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Retrieve product information
            String stockQuery = "SELECT id, stock_quantity, selling_price FROM products WHERE name = ?";
            int productId = 0;
            int stock = 0;
            double sellingPrice = 0;
            try (var pstmt = database.getConnection().prepareStatement(stockQuery)) {
                pstmt.setString(1, productName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        productId = rs.getInt("id");
                        stock = rs.getInt("stock_quantity");
                        sellingPrice = rs.getDouble("selling_price");

                        if (stock < quantity) {
                            JOptionPane.showMessageDialog(null, "Yeterli stok bulunmamaktadır!", "Hata", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Ürün bulunamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Calculate total price
            double totalPrice = quantity * sellingPrice;

            // Calculate remaining balance if installment is selected
            BigDecimal remainingBalance;
            if (isInstallment) {
                remainingBalance = new BigDecimal(totalPrice - peşinat).setScale(2, RoundingMode.HALF_UP);
            } else {
                remainingBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            // Insert sale into the sales table with downpayment
            String saleQuery = "INSERT INTO sales (product_id, customer_id, quantity, sale_date, total_price, sale_price, downpayment, remaining_balance) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)";
            int saleId = 0;
            try (var salePstmt = database.getConnection().prepareStatement(saleQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                salePstmt.setInt(1, productId);
                salePstmt.setInt(2, customerId);
                salePstmt.setInt(3, quantity);
                salePstmt.setDouble(4, totalPrice);  // Store total price
                salePstmt.setDouble(5, sellingPrice); // Store sale price
                salePstmt.setDouble(6, peşinat); // Store downpayment
                salePstmt.setBigDecimal(7, remainingBalance); // Store remaining balance
                salePstmt.executeUpdate();

                // Retrieve the generated sale ID
                try (var generatedKeys = salePstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        saleId = generatedKeys.getInt(1);
                    } else {
                        JOptionPane.showMessageDialog(null, "Satış No alınamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Update stock in the products table
            String updateStockQuery = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
            try (var updateStockPstmt = database.getConnection().prepareStatement(updateStockQuery)) {
                updateStockPstmt.setInt(1, quantity);
                updateStockPstmt.setInt(2, productId);
                updateStockPstmt.executeUpdate();
            }

            // Insert into installments table if installment is selected
            if (isInstallment) {
                String installmentQuery = "INSERT INTO installments (customer_id, remaining_balance, sale_id) VALUES (?, ?, ?)";
                try (var installmentPstmt = database.getConnection().prepareStatement(installmentQuery)) {
                    installmentPstmt.setInt(1, customerId);
                    installmentPstmt.setBigDecimal(2, remainingBalance);  // Use remaining_balance instead of amount_paid
                    installmentPstmt.setInt(3, saleId);
                    installmentPstmt.executeUpdate();
                }
            }

            // Success message
            JOptionPane.showMessageDialog(null, "Satış başarıyla kaydedildi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            loadSalesTable();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Satış kaydedilirken hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    private void clearFields() {
        ürünİsmi.setText("");
        ürünAdeti.setText("");
        peşinatTextField.setText("");
        taksitliÖdemeCheckBox.setSelected(false);
        peşinatTextField.setEnabled(false);
    }

    private void clearFieldsInDelete() {
        satışID.setText("");
    }

    private void loadSalesTable() {
        String[] columnNames = {"Satış No", "Ürün Adı", "Müşteri Adı Soyadı", "Adet", "Toplam Tutar", "Satış Tarihi", "Kalan Borç"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

        String query = """
SELECT s.id, p.name AS product_name, c.first_name, c.last_name, s.quantity,
       s.sale_price, s.sale_date, s.remaining_balance
FROM sales s
LEFT JOIN products p ON s.product_id = p.id
LEFT JOIN customers c ON s.customer_id = c.id
""";

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        try (var stmt = database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int quantity = rs.getInt("quantity");
                double salePrice = rs.getDouble("sale_price");
                double totalPrice = quantity * salePrice;  // Calculate the total price based on the sale_price

                // Format sale date
                String formattedDate = dateFormat.format(rs.getTimestamp("sale_date"));

                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("product_name"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getInt("quantity"),
                        totalPrice,  // Use the dynamically calculated total price
                        formattedDate,
                        rs.getDouble("remaining_balance")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Satış tablosu yüklenirken hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }

        satışTablosu.setModel(tableModel);
    }

    private void deleteSale() {
        String saleIdText = satışID.getText().trim();
        if (saleIdText.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Lütfen bir satış ID girin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int saleId;
        try {
            saleId = Integer.parseInt(saleIdText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Satış ID geçerli bir sayı olmalıdır.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String deleteQuery = "DELETE FROM sales WHERE id = ?";
        String updateStockQuery = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = (SELECT product_id FROM sales WHERE id = ?)";

        try (var deletePstmt = database.getConnection().prepareStatement(deleteQuery);
             var updateStockPstmt = database.getConnection().prepareStatement(updateStockQuery)) {
            // Get product id and quantity sold
            String getProductQuery = "SELECT product_id, quantity FROM sales WHERE id = ?";
            try (var pstmt = database.getConnection().prepareStatement(getProductQuery)) {
                pstmt.setInt(1, saleId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int productId = rs.getInt("product_id");
                        int quantitySold = rs.getInt("quantity");

                        // Update stock quantity
                        updateStockPstmt.setInt(1, quantitySold);
                        updateStockPstmt.setInt(2, saleId);
                        updateStockPstmt.executeUpdate();

                        // Delete sale record
                        deletePstmt.setInt(1, saleId);
                        deletePstmt.executeUpdate();

                        JOptionPane.showMessageDialog(null, "Satış başarıyla silindi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                        loadSalesTable();
                    } else {
                        JOptionPane.showMessageDialog(null, "Satış bulunamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Satış silinirken hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public JPanel getPanel2() {
        return panel2;
    }
}


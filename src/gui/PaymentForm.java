package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import database.Database;

public class PaymentForm {
    private JTabbedPane installmentPane;
    private JPanel panel1;
    private JTextField müşteri_id_TextField;
    private JTextField satış_id_TextField;
    private JTextField ödenen_tutar_TextField;
    private JButton temizleButton;
    private JButton kaydetButton;
    private JTable table1;
    private JTable table2;

    public PaymentForm() {
        // Set up the JTable model for remaining balances (table1)
        DefaultTableModel tableModel1 = new DefaultTableModel();
        tableModel1.setColumnIdentifiers(new String[]{"Müşteri No", "Müşteri Adı Soyadı", "Satış No", "Kalan Borç", "En Son Yapılan Ödeme Tarihi"});
        table1.setModel(tableModel1); // Bind the table to the model

        // Set up the JTable model for past payments (table2)
        DefaultTableModel tableModel2 = new DefaultTableModel();
        tableModel2.setColumnIdentifiers(new String[]{"Müşteri No", "Müşteri Adı Soyadı", "Satış No", "Ödenen Tutar", "Ödeme Tarihi", "Kalan Borç"});
        table2.setModel(tableModel2); // Bind the table to the model

        // Load data into both tables
        loadRemainingBalances();
        loadPastPayments();

        // Add action listeners
        temizleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });

        kaydetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveInstallment();
            }
        });
    }

    private void clearFields() {
        müşteri_id_TextField.setText("");
        satış_id_TextField.setText("");
        ödenen_tutar_TextField.setText("");
    }

    private void saveInstallment() {
        int customerId, saleId;
        double paymentAmount;

        try {
            customerId = Integer.parseInt(müşteri_id_TextField.getText().trim());
            saleId = Integer.parseInt(satış_id_TextField.getText().trim());
            paymentAmount = Double.parseDouble(ödenen_tutar_TextField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Lütfen geçerli bir sayı girin!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Database dbConnection = new Database();
        if (dbConnection.connect()) {
            try (Connection connection = dbConnection.getConnection()) {
                // Get the current remaining balance for the sale
                String getBalanceQuery = "SELECT remaining_balance FROM sales WHERE id = ?";
                double remainingBalance = 0;

                try (PreparedStatement balanceStmt = connection.prepareStatement(getBalanceQuery)) {
                    balanceStmt.setInt(1, saleId);
                    ResultSet rs = balanceStmt.executeQuery();
                    if (rs.next()) {
                        remainingBalance = rs.getDouble("remaining_balance");
                    } else {
                        JOptionPane.showMessageDialog(null, "Satış ID bulunamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Ensure the payment is not greater than the remaining balance
                if (paymentAmount > remainingBalance) {
                    JOptionPane.showMessageDialog(null, "Ödenen tutar kalan bakiyeden fazla olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update the remaining balance
                double newRemainingBalance = remainingBalance - paymentAmount;
                String updateSaleQuery = "UPDATE sales SET remaining_balance = ? WHERE id = ?";

                try (PreparedStatement updateStmt = connection.prepareStatement(updateSaleQuery)) {
                    updateStmt.setDouble(1, newRemainingBalance);
                    updateStmt.setInt(2, saleId);
                    updateStmt.executeUpdate();
                }

                // Insert the installment payment record
                String insertInstallmentQuery = """
                INSERT INTO installments (customer_id, sale_id, amount_paid, remaining_balance, payment_date) 
                VALUES (?, ?, ?, ?, NOW())
            """;

                try (PreparedStatement insertStmt = connection.prepareStatement(insertInstallmentQuery)) {
                    insertStmt.setInt(1, customerId);
                    insertStmt.setInt(2, saleId);
                    insertStmt.setDouble(3, paymentAmount);
                    insertStmt.setDouble(4, newRemainingBalance);
                    insertStmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(null, "Ödeme başarıyla kaydedildi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadRemainingBalances();
                loadPastPayments();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Veritabanı hatası: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            } finally {
                dbConnection.disconnect();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadRemainingBalances() {
        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        Database dbConnection = new Database();

        if (dbConnection.connect()) {
            try (Connection connection = dbConnection.getConnection()) {
                String query = """
                    SELECT s.customer_id, c.first_name, c.last_name, s.id AS sale_id, s.remaining_balance, 
                           (SELECT MAX(payment_date) FROM installments i WHERE i.sale_id = s.id) AS last_payment_date
                    FROM sales s
                    LEFT JOIN customers c ON s.customer_id = c.id
                    WHERE s.remaining_balance > 0
                """;

                try (PreparedStatement pstmt = connection.prepareStatement(query);
                     ResultSet resultSet = pstmt.executeQuery()) {

                    model.setRowCount(0); // Clear the table before populating it

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                    while (resultSet.next()) {
                        int customerId = resultSet.getInt("customer_id");
                        String customerName = resultSet.getString("first_name") + " " + resultSet.getString("last_name");
                        int saleId = resultSet.getInt("sale_id");
                        double remainingBalance = resultSet.getDouble("remaining_balance");
                        Timestamp lastPaymentTimestamp = resultSet.getTimestamp("last_payment_date");

                        String formattedDate = (lastPaymentTimestamp != null) ? dateFormat.format(lastPaymentTimestamp) : "N/A";

                        model.addRow(new Object[]{customerId, customerName, saleId, remainingBalance, formattedDate});
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Veritabanı hatası: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            } finally {
                dbConnection.disconnect();
            }
        }
    }

    private void loadPastPayments() {
        DefaultTableModel model = (DefaultTableModel) table2.getModel();
        Database dbConnection = new Database();

        if (dbConnection.connect()) {
            try (Connection connection = dbConnection.getConnection()) {
                String query = """
                SELECT customer_id, first_name, last_name, sale_id, amount_paid, remaining_balance, payment_date 
                FROM (
                    -- Get all installment payments, but prioritize downpayment if it exists
                    SELECT i.customer_id, c.first_name, c.last_name, i.sale_id, 
                           CASE 
                               WHEN s.downpayment > 0 AND i.amount_paid = 0 THEN s.downpayment 
                               ELSE i.amount_paid 
                           END AS amount_paid,
                           i.remaining_balance, i.payment_date
                    FROM installments i
                    LEFT JOIN customers c ON i.customer_id = c.id
                    LEFT JOIN sales s ON i.sale_id = s.id
                    
                    UNION ALL
                    
                    -- Get standalone down payments from sales (if no installment exists)
                    SELECT s.customer_id, c.first_name, c.last_name, s.id AS sale_id, 
                           s.downpayment AS amount_paid, 
                           s.remaining_balance, s.sale_date AS payment_date
                    FROM sales s
                    LEFT JOIN customers c ON s.customer_id = c.id
                    WHERE s.downpayment > 0
                    AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.sale_id = s.id)

                ) AS payments
                ORDER BY payment_date DESC
            """;

                try (PreparedStatement pstmt = connection.prepareStatement(query);
                     ResultSet resultSet = pstmt.executeQuery()) {

                    model.setRowCount(0); // Clear the table before populating it

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                    while (resultSet.next()) {
                        int customerId = resultSet.getInt("customer_id");
                        String customerName = resultSet.getString("first_name") + " " + resultSet.getString("last_name");
                        int saleId = resultSet.getInt("sale_id");
                        double amountPaid = resultSet.getDouble("amount_paid");
                        double remainingBalance = resultSet.getDouble("remaining_balance");
                        Timestamp paymentTimestamp = resultSet.getTimestamp("payment_date");

                        // Format the payment date
                        String formattedDate = (paymentTimestamp != null) ? dateFormat.format(paymentTimestamp) : "N/A";

                        model.addRow(new Object[]{
                                customerId,
                                customerName,
                                saleId,
                                amountPaid,
                                formattedDate,
                                remainingBalance
                        });
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Veritabanı hatası: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            } finally {
                dbConnection.disconnect();
            }
        }
    }

    public JTabbedPane getInstallmentPane() {
        return installmentPane;
    }
}
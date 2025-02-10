package gui;

import database.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatisticsForm {

    private JPanel panel1;
    private JComboBox<String> comboBoxMonth; // Month selection
    private JButton gösterButton; // Fetch button
    private JTextField textField2; // Total Revenue display
    private JTable table1; // Sales table
    private JTextField textField1; // Net Profit display
    private JComboBox<String> comboBoxYear; // Year selection
    private JTextField textField3; // Remaining Balance display

    private Database database;

    public StatisticsForm() {
        database = new Database();

        // ActionListener for the button to fetch statistics
        gösterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchStatistics();
            }
        });
    }

    private void fetchStatistics() {
        String selectedMonth = (String) comboBoxMonth.getSelectedItem();
        String selectedYear = (String) comboBoxYear.getSelectedItem();

        if (selectedMonth == null || selectedYear == null) {
            JOptionPane.showMessageDialog(null, "Lütfen ay ve yıl seçin!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!database.connect()) {
            JOptionPane.showMessageDialog(null, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Updated query to fetch `sale_price` from `sales` table
        String query = """
        SELECT s.id AS sales_id,
               p.name AS product_name,
               CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, '')) AS customer_name,
               s.quantity,
               p.buying_price,
               s.sale_price,  -- Fetch sale price from sales table
               (s.quantity * s.sale_price) AS total_price,  -- Use sale_price here
               (s.quantity * (s.sale_price - p.buying_price)) AS net_profit,  -- Use sale_price for net profit calculation
               s.remaining_balance
        FROM sales s
        JOIN products p ON s.product_id = p.id
        JOIN customers c ON s.customer_id = c.id
        """;

        boolean filterByMonth = !selectedMonth.equals("Tüm Aylar");
        boolean filterByYear = !selectedYear.equals("Tüm Yıllar");

        if (filterByMonth || filterByYear) {
            query += " WHERE ";
            if (filterByMonth) query += "MONTH(s.sale_date) = ? ";
            if (filterByMonth && filterByYear) query += "AND ";
            if (filterByYear) query += "YEAR(s.sale_date) = ? ";
        }

        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            int paramIndex = 1;
            if (filterByMonth) pstmt.setInt(paramIndex++, getMonthNumber(selectedMonth));
            if (filterByYear) pstmt.setInt(paramIndex, Integer.parseInt(selectedYear));

            try (ResultSet rs = pstmt.executeQuery()) {
                DefaultTableModel model = new DefaultTableModel(
                        new String[]{"Satış No", "Ürün Adı", "Müşteri Adı Soyadı", "Adet", "Alış Fiyatı", "Satış Fiyatı", "Toplam Fiyat", "Net Kazanç", "Kalan Bakiye"}, 0
                );

                double totalRevenue = 0;
                double totalNetProfit = 0;
                double totalRemainingBalance = 0;

                while (rs.next()) {
                    int id = rs.getInt("sales_id");
                    String productName = rs.getString("product_name");
                    String customerName = rs.getString("customer_name");
                    int quantity = rs.getInt("quantity");
                    double buyingPrice = rs.getDouble("buying_price");
                    double salePrice = rs.getDouble("sale_price");  // Fetch sale price
                    double totalPrice = rs.getDouble("total_price");
                    double netProfit = rs.getDouble("net_profit");
                    double remainingBalance = rs.getDouble("remaining_balance");  // Directly from `sales`

                    totalRevenue += totalPrice;
                    totalNetProfit += netProfit;
                    totalRemainingBalance += remainingBalance;

                    model.addRow(new Object[]{id, productName, customerName, quantity, buyingPrice, salePrice, totalPrice, netProfit, remainingBalance});
                }

                table1.setModel(model);
                textField1.setText(String.format("%.2f", totalRevenue));
                textField2.setText(String.format("%.2f", totalNetProfit));
                textField3.setText(String.format("%.2f", totalRemainingBalance));  // Display total remaining balance
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error fetching statistics: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getMonthNumber(String monthName) {
        return switch (monthName) {
            case "Ocak" -> 1;
            case "Şubat" -> 2;
            case "Mart" -> 3;
            case "Nisan" -> 4;
            case "Mayıs" -> 5;
            case "Haziran" -> 6;
            case "Temmuz" -> 7;
            case "Ağustos" -> 8;
            case "Eylül" -> 9;
            case "Ekim" -> 10;
            case "Kasım" -> 11;
            case "Aralık" -> 12;
            default -> -1;
        };
    }

    public JPanel getPanel1() {
        return panel1;
    }
}
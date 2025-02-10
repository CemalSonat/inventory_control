package database;

import models.Customer;
import models.Product;

import javax.swing.*;
import java.sql.*;

public class Database {
    private Connection connection;
    private static final String URL = "jdbc:mysql://inventory-db.chwccm2eg6bt.eu-central-1.rds.amazonaws.com:3306/inventory-db";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "to6c3V1d5B20ElufRg2Q";

    // Method to connect to the database
    public boolean connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to the database.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
            return false;
        }
    }

    // Method to disconnect from the database
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Disconnected from the database.");
            } catch (SQLException e) {
                System.err.println("Error disconnecting from the database: " + e.getMessage());
            }
        }
    }

    // Getter for the connection
    public Connection getConnection() {
        return connection;
    }

    // Method to insert a product into the database
    public void insertProduct(Product product) throws SQLException {
        String query = "INSERT INTO products (name, brand, buying_price, selling_price, stock_quantity) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getBrand());
            pstmt.setDouble(3, product.getBuyingPrice());
            pstmt.setDouble(4, product.getSellingPrice());
            pstmt.setInt(5, product.getStockQuantity());
            pstmt.executeUpdate();
            System.out.println("Product added successfully.");
        } catch (SQLException e) {
            System.err.println("Error inserting product: " + e.getMessage());
            throw e;
        }
    }

    // Method to delete a product from the database by name
    public boolean deleteProduct(String productName) throws SQLException {
        // First, check if the product exists in the sales records
        String checkSalesQuery = "SELECT COUNT(*) FROM sales WHERE product_id IN (SELECT id FROM products WHERE name = ?)";
        try (PreparedStatement pstmtCheck = connection.prepareStatement(checkSalesQuery)) {
            pstmtCheck.setString(1, productName);
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("This product has been sold and cannot be deleted.");
                    return false;  // Prevent deletion if the product has sales records
                }
            }
        }

        // If the product does not have any sales, proceed with deletion
        String query = "DELETE FROM products WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, productName);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Product deleted successfully.");
                return true;
            } else {
                System.err.println("No product found with the name: " + productName);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            throw e;
        }
    }

    // Method to add a customer to the database
    public void addCustomer(Customer customer) throws SQLException {
        String query = "INSERT INTO customers (first_name, last_name, phone_number, address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customer.getFirstName());
            pstmt.setString(2, customer.getLastName());
            pstmt.setString(3, customer.getContactNumber());
            pstmt.setString(4, customer.getAddress());
            pstmt.executeUpdate();
            System.out.println("Customer added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding customer: " + e.getMessage());
            throw e;
        }
    }

    // Method to delete a customer from the database by name (firstName + lastName)
    public boolean deleteCustomer(String firstName, String lastName) throws SQLException {
        String checkSalesQuery = "SELECT COUNT(*) FROM sales WHERE customer_first_name = ? AND customer_last_name = ?";
        try (PreparedStatement pstmtCheck = connection.prepareStatement(checkSalesQuery)) {
            pstmtCheck.setString(1, firstName);
            pstmtCheck.setString(2, lastName);
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("This customer has sales records and cannot be deleted.");
                    return false;  // Prevent deletion if the customer has sales records
                }
            }
        }

        // If no sales, proceed with deletion
        String query = "DELETE FROM customers WHERE first_name = ? AND last_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Customer deleted successfully.");
                return true;
            } else {
                System.err.println("No customer found with the name: " + firstName + " " + lastName);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting customer: " + e.getMessage());
            throw e;
        }
    }

    public void updateCustomer(Customer customer) throws SQLException {
        String query = "UPDATE customers SET first_name = ?, last_name = ?, phone_number = ?, address = ? WHERE id = ?";

        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);  // Disable auto-commit
        }

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customer.getFirstName());
            pstmt.setString(2, customer.getLastName());
            pstmt.setString(3, customer.getContactNumber());
            pstmt.setString(4, customer.getAddress());
            pstmt.setInt(5, customer.getId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                connection.commit();  // Commit the transaction
                System.out.println("Customer updated successfully.");
            } else {
                connection.rollback();  // Rollback if no rows were affected
                System.err.println("No rows affected. Rollback.");
            }
        } catch (SQLException e) {
            connection.rollback();  // Rollback in case of an exception
            System.err.println("Error updating customer: " + e.getMessage());
            throw e;  // Rethrow the exception
        } finally {
            connection.setAutoCommit(true);  // Restore auto-commit mode
        }
    }

    public void recordSale(String productName, int quantitySold, String customerFirstName, String customerLastName, boolean taksitli) throws SQLException {
        String productQuery = "SELECT id, selling_price, stock_quantity FROM products WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(productQuery)) {
            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Product not found.");
            }

            int productId = rs.getInt("id");
            double sellingPrice = rs.getDouble("selling_price");
            int stockQuantity = rs.getInt("stock_quantity");

            if (stockQuantity < quantitySold) {
                throw new SQLException("Not enough stock available.");
            }

            double totalAmount = sellingPrice * quantitySold;
            double remainingBalance = taksitli ? totalAmount : 0.0;  // Set remaining balance based on "Taksitli" flag

            // Check if the customer exists
            String customerQuery = "SELECT id FROM customers WHERE first_name = ? AND last_name = ?";
            int customerId;
            try (PreparedStatement pstmtCustomer = connection.prepareStatement(customerQuery)) {
                pstmtCustomer.setString(1, customerFirstName);
                pstmtCustomer.setString(2, customerLastName);
                ResultSet customerRs = pstmtCustomer.executeQuery();

                if (customerRs.next()) {
                    customerId = customerRs.getInt("id");
                } else {
                    throw new SQLException("Customer not found.");
                }
            }

            // Record the sale
            String saleQuery = "INSERT INTO sales (product_id, customer_id, quantity, total_amount, remaining_balance, sale_date) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement pstmtSale = connection.prepareStatement(saleQuery)) {
                pstmtSale.setInt(1, productId);
                pstmtSale.setInt(2, customerId);
                pstmtSale.setInt(3, quantitySold);
                pstmtSale.setDouble(4, totalAmount);
                pstmtSale.setDouble(5, remainingBalance);  // Save remaining balance
                pstmtSale.executeUpdate();
                System.out.println("Sale recorded successfully.");
            }

            // Update stock quantity
            String updateStockQuery = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
            try (PreparedStatement pstmtStock = connection.prepareStatement(updateStockQuery)) {
                pstmtStock.setInt(1, quantitySold);
                pstmtStock.setInt(2, productId);
                pstmtStock.executeUpdate();
            }
        }
    }

    public boolean deleteSale(int saleId) {
        try {
            connection.setAutoCommit(false);

            String getSaleDetailsQuery = "SELECT product_id, quantity FROM sales WHERE id = ?";
            int productId = 0;
            int quantitySold = 0;
            try (PreparedStatement pstmt = connection.prepareStatement(getSaleDetailsQuery)) {
                pstmt.setInt(1, saleId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        productId = rs.getInt("product_id");
                        quantitySold = rs.getInt("quantity");
                    } else {
                        return false;
                    }
                }
            }

            String updateStockQuery = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateStockQuery)) {
                pstmt.setInt(1, quantitySold);
                pstmt.setInt(2, productId);
                pstmt.executeUpdate();
            }

            String deleteSaleQuery = "DELETE FROM sales WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteSaleQuery)) {
                pstmt.setInt(1, saleId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    connection.commit();
                    System.out.println("Sale deleted successfully and stock updated.");
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                System.err.println("Error rolling back transaction: " + e.getMessage());
            }
            System.err.println("Error deleting sale: " + ex.getMessage());
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit: " + e.getMessage());
            }
        }
    }
}
package models;

import java.util.Date;

public class Sale {

    private int id;
    private int productId;  // Keep the productId for relational integrity
    private String productName;  // Store productName for display
    private String customerName;
    private String customerEmail;
    private int quantity;
    private double totalAmount;
    private Date saleDate;
    private double remainingBalance;  // Add remaining_balance field

    // Default Constructor
    public Sale() {
        this.id = 0;
        this.productId = 0;
        this.productName = "";
        this.customerName = "";
        this.customerEmail = "";
        this.quantity = 0;
        this.totalAmount = 0.0;
        this.saleDate = new Date();  // Default to current date
        this.remainingBalance = 0.0;  // Initialize remainingBalance to 0
    }

    // Constructor with parameters
    public Sale(int id, int productId, String productName, String customerName, String customerEmail, int quantity, double totalAmount, Date saleDate, double remainingBalance) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.saleDate = saleDate;
        this.remainingBalance = remainingBalance;  // Initialize remainingBalance
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(Date saleDate) {
        this.saleDate = saleDate;
    }

    public double getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", saleDate=" + saleDate +
                ", remainingBalance=" + remainingBalance +  // Include remainingBalance in toString
                '}';
    }
}
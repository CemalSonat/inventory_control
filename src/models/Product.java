package models;

public class Product {

    private int id;
    private String name;
    private String brand;
    private double buyingPrice;
    private double sellingPrice;
    private int stockQuantity;

    // Default Constructor
    public Product() {
        this.name = "Null";
        this.brand = "Null";
        this.buyingPrice = 0;
        this.sellingPrice = 0;
        this.stockQuantity = 0;
    }

    // Constructor without ID (used for inserting new products)
    public Product(String name, String brand, double buyingPrice, double sellingPrice, int stockQuantity) {
        this.name = name;
        this.brand = brand;
        this.buyingPrice = buyingPrice;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
    }

    // Constructor with ID (used for querying existing products or updates)
    public Product(int id, String name, String brand, double buyingPrice, double sellingPrice, int stockQuantity) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.buyingPrice = buyingPrice;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", buyingPrice=" + buyingPrice +
                ", sellingPrice=" + sellingPrice +
                ", stockQuantity=" + stockQuantity +
                '}';
    }
}

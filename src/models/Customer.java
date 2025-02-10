package models;

public class Customer {

    private int id;
    private String firstName;
    private String lastName;
    private String contactNumber;
    private String address;

    // Default Constructor
    public Customer() {
        this.id = 0; // Default ID (can be updated later after inserting into the database)
        this.firstName = "None";
        this.lastName = "None";
        this.contactNumber = "None";
        this.address = "None";
    }

    // Constructor with all parameters including ID
    public Customer(int id, String firstName, String lastName, String contactNumber, String address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.address = address;
    }

    // Constructor without ID (ID will be assigned automatically later)
    public Customer(String firstName, String lastName, String contactNumber, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.id = 0; // Set to 0 initially, can be updated after insertion into DB
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
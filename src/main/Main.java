package main;

import database.Database;
import gui.MainGUI;

public class Main {
    public static void main(String[] args) {
        // Step 1: Initialize the database
        Database db = new Database();
        if (db.connect()) {
            System.out.println("Database connection established.");
        } else {
            System.err.println("Failed to connect to the database. Exiting...");
            return;
        }

        // Step 2: Launch the GUI
        MainGUI gui = new MainGUI();
        gui.launch();

        // Optional: Close database connection on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.disconnect();
            System.out.println("Database connection closed.");
        }));
    }
}
package gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainGUI {

    private JPanel mainPanel;
    private JButton ürünYönetButton;
    private JButton satışKaydetButton;
    private JButton müşteriYönetButton;
    private JButton ödemeKaydetButton;
    private JPanel rightPanel;
    private JButton istatistiklerButton;

    public MainGUI() {

        // Add ActionListeners for the buttons
        ürünYönetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showProductForm();
            }
        });

        satışKaydetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSaleForm();
            }
        });

        müşteriYönetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCustomerForm();
            }
        });

        ödemeKaydetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPaymentForm();
            }
        });

        istatistiklerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showStatisticsForm();
            }
        });
    }

    // Show the product management form
    public void showProductForm() {
        ProductForm productForm = new ProductForm();
        rightPanel.removeAll();
        rightPanel.add(productForm.getProductPane()); // Add the product pane
        rightPanel.repaint();
        rightPanel.revalidate();
    }

    // Show the sale form
    public void showSaleForm() {
        SaleForm saleForm = new SaleForm();
        rightPanel.removeAll();
        rightPanel.add(saleForm.getPanel2()); // Ensure SaleForm has a getSalePane() method
        rightPanel.repaint();
        rightPanel.revalidate();
    }

    // Show the customer management form
    public void showCustomerForm() {
        CustomersForm customerForm = new CustomersForm();
        rightPanel.removeAll();
        rightPanel.add(customerForm.getCustomersPane());
        rightPanel.repaint();
        rightPanel.revalidate();
    }

    // Show the payment form
    public void showPaymentForm() {
        PaymentForm paymentForm = new PaymentForm();
        rightPanel.removeAll();
        rightPanel.add(paymentForm.getInstallmentPane());
        rightPanel.repaint();
        rightPanel.revalidate();
    }

    // Show the statistics form
    public void showStatisticsForm() {
        StatisticsForm statisticsForm = new StatisticsForm();
        rightPanel.removeAll();
        rightPanel.add(statisticsForm.getPanel1());
        rightPanel.repaint();
        rightPanel.revalidate();
    }

    // Return the main panel
    public JPanel getMainPanel() {
        return mainPanel;
    }

    // Launch the main GUI
    public void launch() {
        JFrame frame = new JFrame("Stok Kontrol Sistemi");
        frame.setContentPane(this.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 900); // Adjust size to accommodate all components
        frame.setVisible(true);
    }
}
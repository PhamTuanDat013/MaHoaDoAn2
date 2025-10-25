package a51;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class A51UI extends JFrame {
    private final JTextField keyField;
    private final JTextField frameField;
    private final JTextArea inputArea;
    private final JTextArea outputArea;
    private final JTextArea ksArea;
    private final JButton initBtn;
    private final JButton encryptBtn;
    private final JButton decryptBtn;
    private final JButton genKsBtn;
    private final A51Cipher cipher;

    public A51UI() {
        setTitle("A5/1 Cipher - Demo (Java Swing)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        cipher = new A51Cipher();

        // Top panel for key/frame
        JPanel top = new JPanel(new GridLayout(2, 3, 6, 6));
        top.add(new JLabel("Key (16 hex bytes = 64 bits):"));
        keyField = new JTextField("0123456789ABCDEF"); // sample
        top.add(keyField);
        initBtn = new JButton("Init (mix)");
        top.add(initBtn);

        top.add(new JLabel("Frame number (0..(2^22-1)) :"));
        frameField = new JTextField("0");
        top.add(frameField);
        genKsBtn = new JButton("Gen 114-bit keystream (for GSM burst)");
        top.add(genKsBtn);

        // Center: input and output areas
        inputArea = new JTextArea();
        outputArea = new JTextArea();
        ksArea = new JTextArea();
        ksArea.setEditable(false);

        JPanel center = new JPanel(new GridLayout(1, 3, 6, 6));
        center.add(new JScrollPane(inputArea));
        center.add(new JScrollPane(outputArea));
        center.add(new JScrollPane(ksArea));

        // Bottom buttons
        JPanel bot = new JPanel();
        encryptBtn = new JButton("Encrypt (XOR)");
        decryptBtn = new JButton("Decrypt (XOR)");
        bot.add(encryptBtn);
        bot.add(decryptBtn);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(bot, BorderLayout.SOUTH);

        // Actions
        initBtn.addActionListener(e -> doInit());
        genKsBtn.addActionListener(e -> doGenKs());
        encryptBtn.addActionListener(e -> doXor(true));
        decryptBtn.addActionListener(e -> doXor(false));
    }

    private void doInit() {
        try {
            String hex = keyField.getText().trim();
            if (hex.length() < 16) {
                JOptionPane.showMessageDialog(this, "Key hex must be at least 16 hex chars (64 bits).");
                return;
            }
            byte[] keyBytes = A51Cipher.hexToBytes(hex.substring(0, 16));
            long key64 = 0L;
            for (int i = 0; i < Math.min(8, keyBytes.length); i++) {
                key64 |= ((long) (keyBytes[i] & 0xFF)) << (8 * i); // little-endian bit ordering as used in cipher init
            }
            int frame = Integer.parseInt(frameField.getText().trim());
            cipher.init(key64, frame);
            JOptionPane.showMessageDialog(this, "Initialization done (key mixed).");
        } catch (HeadlessException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Init error: " + ex.getMessage());
        }
    }

    private void doGenKs() {
        try {
            // Generate 114 bits (15 bytes = 120 bits) or user-chosen; we'll generate 15 bytes
            byte[] ks = cipher.getKeystreamBytes(15);
            ksArea.setText(A51Cipher.bytesToHex(ks) + "\n(printed as hex, 15 bytes = 120 bits; use first 114 bits for a GSM burst)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gen KS error: " + ex.getMessage());
        }
    }

    private void doXor(boolean encrypt) {
        try {
            String input = inputArea.getText();
            if (input == null) input = "";
            byte[] plain = input.getBytes(StandardCharsets.UTF_8);
            byte[] ciphered = cipher.encryptBytes(plain); // symmetric
            outputArea.setText(A51Cipher.bytesToHex(ciphered));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "XOR error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new A51UI().setVisible(true));
    }
}

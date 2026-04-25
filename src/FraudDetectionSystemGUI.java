import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.*;
import java.util.*;

class Transaction {
    String txId;
    String cardId;
    LocalDateTime time;
    double amount;

    Transaction(String txId, String cardId, String time, double amount) {
        this.txId = txId.trim();
        this.cardId = cardId.trim().toUpperCase();
        this.time = LocalDateTime.parse(time.trim());
        this.amount = amount;
    }
}

class FraudEngine {

    ArrayList<Transaction> allTx = new ArrayList<>();
    HashMap<String, ArrayList<Transaction>> map = new HashMap<>();

    // ================= SMART CSV LOADER =================
    void loadCSV(String path) {

        System.out.println("\n===== CSV LOADING START =====");

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            String line = br.readLine(); // skip header
            int count = 0;

            while ((line = br.readLine()) != null) {

                System.out.println("READ LINE: " + line);

                if (line.trim().isEmpty()) continue;

                // 🔥 supports comma + tab + double comma
                String[] raw = line.split("[,\t]", -1);

                try {
                    String txId = raw[0];
                    String cardId = raw[1];
                    String time = raw[2];

                    double amount;

                    // handle cases like ,, (empty column)
                    if (raw.length >= 5 && raw[3].isEmpty()) {
                        amount = Double.parseDouble(raw[4]);
                    } else {
                        amount = Double.parseDouble(raw[3]);
                    }

                    Transaction tx = new Transaction(txId, cardId, time, amount);

                    allTx.add(tx);
                    map.putIfAbsent(tx.cardId, new ArrayList<>());
                    map.get(tx.cardId).add(tx);

                    count++;

                } catch (Exception e) {
                    System.out.println("❌ PARSE ERROR: " + line);
                }
            }

            System.out.println("✔ TOTAL LOADED: " + count);
            System.out.println("✔ CARDS FOUND: " + map.keySet());
            System.out.println("===== CSV LOADING END =====\n");

        } catch (Exception e) {
            System.out.println("❌ FILE ERROR: " + e.getMessage());
        }
    }

    // ================= FRAUD CHECK =================
    String check(String card) {

        card = card.trim().toUpperCase();

        if (!map.containsKey(card)) {
            return "NO DATA";
        }

        ArrayList<Transaction> list = map.get(card);

        double mean = allTx.stream()
                .mapToDouble(t -> t.amount)
                .average().orElse(0);

        double std = Math.sqrt(allTx.stream()
                .mapToDouble(t -> Math.pow(t.amount - mean, 2))
                .average().orElse(0));

        if (std == 0) std = 1;

        for (Transaction t : list) {

            double z = (t.amount - mean) / std;
            int velocity = list.size();

            double risk = Math.abs(z) + velocity * 0.2;

            System.out.println("TX: " + t.txId + " Amt: " + t.amount + " Risk: " + risk);

            // 🔥 HARD RULE (fix your issue)
            if (t.amount > 10000) return "FRAUD";

            if (risk > 3) return "FRAUD";
        }

        return "NORMAL";
    }
}

public class FraudDetectionSystemGUI {

    public static void main(String[] args) {

        FraudEngine engine = new FraudEngine();

        // 🔥 ABSOLUTE PATH (safe)
        engine.loadCSV("/Users/lalithgundu/Documents/FraudDetectionSystem/data/transactions.csv");

        JFrame frame = new JFrame("Fraud Detection System");
        frame.setSize(420, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1));

        JLabel title = new JLabel("Enter Card ID", JLabel.CENTER);
        JTextField input = new JTextField();
        JButton btn = new JButton("Check Fraud");
        JLabel result = new JLabel("", JLabel.CENTER);

        panel.add(title);
        panel.add(input);
        panel.add(btn);
        panel.add(result);

        frame.add(panel);

        btn.addActionListener(e -> {

            String res = engine.check(input.getText());

            if (res.equals("FRAUD")) {
                panel.setBackground(Color.RED);
                result.setText("🚨 FRAUD DETECTED");
            } else if (res.equals("NORMAL")) {
                panel.setBackground(Color.GREEN);
                result.setText("✔ NORMAL");
            } else {
                panel.setBackground(Color.GRAY);
                result.setText("NO DATA FOUND");
            }
        });

        frame.setVisible(true);
    }
}
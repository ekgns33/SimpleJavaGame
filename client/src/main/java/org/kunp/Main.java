package org.kunp;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String TempSessionId = args[0];
                new Client(TempSessionId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}


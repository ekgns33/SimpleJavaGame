package org.kunp;

import javax.swing.*;

public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        new Client();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}


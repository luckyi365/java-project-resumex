package com.resumex;

import com.formdev.flatlaf.FlatDarkLaf;
import com.resumex.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

/**
 * Modern Application Entry Point with FlatLaf styling.
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Apply Modern Flat Dark Look & Feel
            FlatDarkLaf.setup();
            
            // Custom modern UI polish: Rounded corners, sleek paddings
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
        } catch (Exception ex) {
            System.err.println("Notice: FlatLaf initialization failed, falling back to system defaults: " + ex.getMessage());
        }
        
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            frame.startPrompts();
        });
    }
}

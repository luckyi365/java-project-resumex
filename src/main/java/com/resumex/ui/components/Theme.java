package com.resumex.ui.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Modern design system for ResumeX with dark glassmorphism aesthetics.
 */
public final class Theme {

    // ---- Color Palette ----
    public static final Color BG           = new Color(0x0B0F19); // Ultra-deep slate
    public static final Color SIDEBAR      = new Color(0x111625); // Sleek sidebar
    public static final Color CARD         = new Color(0x181F33); // Elevation 1
    public static final Color CARD_HI      = new Color(0x222B45); // Elevation 2
    public static final Color CARD_BORDER   = new Color(0x2D3754); // Subtle outline
    public static final Color INPUT_BG     = new Color(0x13182A); // Form input fill
    
    // Accents & Gradients
    public static final Color ACCENT       = new Color(0x6366F1); // Modern Indigo
    public static final Color ACCENT_HOVER = new Color(0x4F46E5);
    public static final Color ACCENT2      = new Color(0x06B6D4); // Cyber Cyan
    public static final Color ACCENT_PURPLE= new Color(0x8B5CF6);
    
    // Semantic Status
    public static final Color GOOD         = new Color(0x10B981); // Emerald Green
    public static final Color WARN         = new Color(0xF59E0B); // Amber Warning
    public static final Color BAD          = new Color(0xEF4444); // Crimson Error
    
    // Text Hierarchy
    public static final Color TEXT         = new Color(0xF3F4F6); // Crisp white
    public static final Color TEXT_SECONDARY=new Color(0xD1D5DB);
    public static final Color MUTED        = new Color(0x9CA3AF); // Subdued gray
    public static final Color MUTED_DARK   = new Color(0x4B5563);

    // ---- Typography ----
    public static final String FAMILY = pickFont();
    public static final Font DISPLAY = new Font(FAMILY, Font.BOLD, 28);
    public static final Font H1      = new Font(FAMILY, Font.BOLD, 22);
    public static final Font H2      = new Font(FAMILY, Font.BOLD, 16);
    public static final Font H3      = new Font(FAMILY, Font.BOLD, 14);
    public static final Font BODY    = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font BODY_BOLD=new Font(FAMILY, Font.BOLD, 13);
    public static final Font SMALL   = new Font(FAMILY, Font.PLAIN, 11);
    public static final Font SMALL_BOLD=new Font(FAMILY, Font.BOLD, 11);

    private Theme() { /* utility class */ }

    // ---- Factory helpers ----

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static Border roundedBorder(Color color, int radius) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        );
    }

    public static Graphics2D hq(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    public static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0x3B4568);
                trackColor = BG;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    private static String pickFont() {
        Set<String> names = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                                   .getAvailableFontFamilyNames()));
        for (String f : new String[]{
                "Segoe UI", "Inter", "SF Pro Display",
                "Helvetica Neue", "Ubuntu", "Arial"})
            if (names.contains(f)) return f;
        return Font.SANS_SERIF;
    }
}

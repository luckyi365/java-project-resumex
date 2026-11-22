package com.resumex.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A small pill-shaped tag/chip label used to display skills, verbs, etc.
 */
public class Chip extends JLabel {

    private final Color color;

    /**
     * @param text  chip label
     * @param color accent colour (used for text, border and translucent fill)
     */
    public Chip(String text, Color color) {
        super(text);
        this.color = color;
        setFont(new Font(Theme.FAMILY, Font.BOLD, 12));
        setForeground(color);
        setBorder(new EmptyBorder(6, 14, 6, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        // Translucent fill
        g2.setColor(new Color(color.getRed(), color.getGreen(),
                              color.getBlue(), 40));
        g2.fill(new RoundRectangle2D.Float(
                0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
        // Subtle border
        g2.setColor(new Color(color.getRed(), color.getGreen(),
                              color.getBlue(), 120));
        g2.draw(new RoundRectangle2D.Float(
                0, 0, getWidth() - 1, getHeight() - 1,
                getHeight(), getHeight()));
        g2.dispose();
        super.paintComponent(g);
    }
}

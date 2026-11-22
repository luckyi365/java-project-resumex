package com.resumex.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern card container with rounded corners and subtle border outline.
 */
public class RoundPanel extends JPanel {

    private final Color color;
    private final int arc;
    private Color borderColor;

    public RoundPanel(Color color, int arc) {
        this(color, arc, Theme.CARD_BORDER);
    }

    public RoundPanel(Color color, int arc, Color borderColor) {
        this.color = color;
        this.arc = arc;
        this.borderColor = borderColor;
        setOpaque(false);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        
        // Background fill
        g2.setColor(color);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));

        // Subtle crisp border outline
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, arc, arc));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

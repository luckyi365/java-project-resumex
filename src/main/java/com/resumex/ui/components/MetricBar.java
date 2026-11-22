package com.resumex.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Modern labelled progress metric bar for ATS breakdown categories.
 */
public class MetricBar extends JComponent {

    private final String name;
    private final int value;

    public MetricBar(String name, int value) {
        this.name = name;
        this.value = Math.max(0, Math.min(100, value));
        setPreferredSize(new Dimension(300, 36));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);

        // Label
        g2.setFont(Theme.BODY);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString(name, 0, 13);

        // Value tag
        g2.setFont(Theme.BODY_BOLD);
        Color valueColor = value >= 75 ? Theme.GOOD : value >= 55 ? Theme.WARN : Theme.BAD;
        g2.setColor(valueColor);
        String v = value + "%";
        g2.drawString(v, getWidth() - g2.getFontMetrics().stringWidth(v), 13);

        // Track bar
        int y = 22, h = 7;
        g2.setColor(Theme.CARD_HI);
        g2.fillRoundRect(0, y, getWidth(), h, h, h);

        // Filled bar with gradient
        if (value > 0) {
            int fillWidth = Math.max(h, (int) (getWidth() * (value / 100.0)));
            Color gradStart = valueColor.darker();
            g2.setPaint(new GradientPaint(0, 0, gradStart, fillWidth, 0, valueColor));
            g2.fillRoundRect(0, y, fillWidth, h, h, h);
        }

        g2.dispose();
    }
}

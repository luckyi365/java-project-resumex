package com.resumex.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Modern animated circular gauge that displays an ATS score from 0 to 100.
 */
public class ScoreGauge extends JComponent {

    private final int target;
    private double shown = 0;

    public ScoreGauge(int target) {
        this.target = Math.max(0, Math.min(100, target));
        setPreferredSize(new Dimension(190, 160));
    }

    public void animate() {
        shown = 0;
        Timer t = new Timer(15, null);
        t.addActionListener(e -> {
            shown = Math.min(target, shown + 2.0);
            repaint();
            if (shown >= target) ((Timer) e.getSource()).stop();
        });
        t.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);

        int size = Math.min(getWidth(), getHeight()) - 24;
        int x = (getWidth() - size) / 2;
        int y = 14;

        // Background track ring
        g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.CARD_HI);
        g2.draw(new Arc2D.Double(x, y, size, size, 90, -360, Arc2D.OPEN));

        // Color based on target score
        Color startCol;
        Color endCol;
        if (target >= 80) {
            startCol = Theme.GOOD;
            endCol = Theme.ACCENT2;
        } else if (target >= 60) {
            startCol = Theme.WARN;
            endCol = new Color(0xFBBF24);
        } else {
            startCol = Theme.BAD;
            endCol = new Color(0xF87171);
        }

        // Active progress arc
        g2.setPaint(new GradientPaint(x, y, startCol, x + size, y + size, endCol));
        g2.draw(new Arc2D.Double(x, y, size, size, 90, -360 * (shown / 100.0), Arc2D.OPEN));

        // Score number text
        g2.setColor(Theme.TEXT);
        g2.setFont(new Font(Theme.FAMILY, Font.BOLD, 36));
        String s = String.valueOf((int) shown);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, y + size / 2 + fm.getAscent() / 3);

        // Subtitle
        g2.setFont(Theme.SMALL_BOLD);
        g2.setColor(Theme.MUTED);
        String sub = "ATS COMPATIBILITY";
        g2.drawString(sub, (getWidth() - g2.getFontMetrics().stringWidth(sub)) / 2, y + size / 2 + 26);

        g2.dispose();
    }
}

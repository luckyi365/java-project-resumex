package com.resumex.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern sidebar navigation item with active indicator pill.
 */
public class NavButton extends JButton {

    private boolean active;
    private boolean over;

    public NavButton(String text) {
        super(text);
        setFont(Theme.H3);
        setHorizontalAlignment(LEFT);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setBorder(new EmptyBorder(12, 18, 12, 18));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(Theme.MUTED);
        
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { over = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { over = false; repaint(); }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        setForeground(active ? Color.WHITE : Theme.MUTED);
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        
        if (active) {
            // Active background gradient
            g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, getWidth(), 0, new Color(0x4338CA)));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            
            // Left active glowing bar
            g2.setColor(Theme.ACCENT2);
            g2.fill(new RoundRectangle2D.Float(0, 8, 4, getHeight() - 16, 4, 4));
        } else if (over) {
            g2.setColor(Theme.CARD);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

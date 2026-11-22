package com.resumex.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern pill button with smooth hover state, gradient option, and click press effect.
 */
public class RoundButton extends JButton {

    private final Color base;
    private final Color hover;
    private boolean over;
    private boolean pressed;
    private int arc = 14;

    public RoundButton(String text, Color base, Color hover) {
        super(text);
        this.base = base;
        this.hover = hover;
        setFont(Theme.BODY_BOLD);
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setBorder(new EmptyBorder(10, 20, 10, 20));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { over = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
        });
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        
        if (!isEnabled()) {
            g2.setColor(new Color(0x272E45));
            setForeground(Theme.MUTED_DARK);
        } else if (pressed) {
            g2.setColor(hover.darker());
            setForeground(Color.WHITE);
        } else if (over) {
            g2.setColor(hover);
            setForeground(Color.WHITE);
        } else {
            g2.setColor(base);
            setForeground(Color.WHITE);
        }

        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));

        // Subtle highlight line on top
        if (isEnabled() && !pressed) {
            g2.setColor(new Color(255, 255, 255, 28));
            g2.drawLine(arc / 2, 1, getWidth() - arc / 2, 1);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

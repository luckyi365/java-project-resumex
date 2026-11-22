package com.resumex.ui;

import com.resumex.dao.Database;
import com.resumex.dao.UserDao;
import com.resumex.model.User;
import com.resumex.ui.components.NavButton;
import com.resumex.ui.components.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean application window for ResumeX.
 * Streamlined navigation, zero blocking popups, and confidential background execution.
 */
public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final List<NavButton> navButtons = new ArrayList<>();

    private User currentUser;
    private final UserDao userDao = new UserDao();
    
    // Core Application Panels
    private AnalyzerPanel analyzerPanel;
    private InterviewPanel interviewPanel;
    private HistoryPanel historyPanel;

    public MainFrame() {
        setTitle("ResumeX - Smart Resume Analyzer & AI Mock Interviewer");
        setSize(1180, 780);
        setMinimumSize(new Dimension(980, 640));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Silent background session
        initDefaultUser();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildSidebar(), BorderLayout.WEST);

        // Instantiate view panels
        analyzerPanel = new AnalyzerPanel(currentUser, this);
        interviewPanel = new InterviewPanel(currentUser, this);
        historyPanel = new HistoryPanel(currentUser, analyzerPanel, interviewPanel, this);

        content.setBackground(Theme.BG);
        content.add(analyzerPanel, "analyzer");
        content.add(interviewPanel, "interview");
        content.add(historyPanel, "history");
        root.add(content, BorderLayout.CENTER);
        
        setContentPane(root);
        navButtons.get(0).setActive(true);
        cards.show(content, "analyzer");
    }

    /**
     * Called once after frame is visible. Silent database bootstrap without popups.
     */
    public void startPrompts() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (Database.isAvailable()) {
                    currentUser = userDao.findOrCreate("User", "user@resumex.com");
                }
            } catch (Exception ignored) {}

            analyzerPanel.setCurrentUser(currentUser);
            interviewPanel.setCurrentUser(currentUser);
            historyPanel.setCurrentUser(currentUser);
        });
    }

    private void initDefaultUser() {
        currentUser = new User("User", "user@resumex.com");
        currentUser.setId(1);
    }
    
    public void navigateTo(String cardName) {
        cards.show(content, cardName);
        for (int i = 0; i < navButtons.size(); i++) {
            boolean isTarget = false;
            if (i == 0 && cardName.equals("analyzer")) isTarget = true;
            if (i == 1 && cardName.equals("interview")) isTarget = true;
            if (i == 2 && cardName.equals("history")) isTarget = true;
            navButtons.get(i).setActive(isTarget);
        }
        if (cardName.equals("interview")) {
            interviewPanel.refreshResumeList();
        } else if (cardName.equals("history")) {
            historyPanel.refreshData();
        }
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(230, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(26, 18, 22, 18));

        // Clean Brand Logo (No "AI PRO" badge)
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel logo = new JLabel("Resume") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                g2.setFont(getFont());
                g2.setColor(Theme.TEXT);
                g2.drawString("Resume", 0, 24);
                int w = g2.getFontMetrics().stringWidth("Resume");
                g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, 40, 0, Theme.ACCENT2));
                g2.drawString("X", w, 24);
                g2.dispose();
            }
        };
        logo.setFont(new Font(Theme.FAMILY, Font.BOLD, 26));
        logo.setPreferredSize(new Dimension(140, 32));
        logoPanel.add(logo);

        side.add(logoPanel);
        side.add(Box.createVerticalStrut(4));

        JLabel tag = Theme.label("Career Acceleration Toolkit", Theme.SMALL, Theme.MUTED);
        tag.setAlignmentX(LEFT_ALIGNMENT);
        side.add(tag);
        side.add(Box.createVerticalStrut(28));

        // Navigation Buttons
        side.add(nav("Resume Analyzer", "analyzer"));
        side.add(Box.createVerticalStrut(8));
        side.add(nav("Mock Interview", "interview"));
        side.add(Box.createVerticalStrut(8));
        side.add(nav("History & Stats", "history"));
        
        side.add(Box.createVerticalGlue());

        // Minimal, elegant footer (No exposing API key, mode, or developer boxes)
        JLabel footer = Theme.label("ResumeX v1.0", Theme.SMALL, Theme.MUTED_DARK);
        footer.setAlignmentX(LEFT_ALIGNMENT);
        side.add(footer);

        return side;
    }

    private NavButton nav(String text, String card) {
        NavButton b = new NavButton(text);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.addActionListener(e -> {
            cards.show(content, card);
            navButtons.forEach(n -> n.setActive(n == b));
            if (card.equals("history") && historyPanel != null) {
                historyPanel.refreshData();
            } else if (card.equals("interview") && interviewPanel != null) {
                interviewPanel.refreshResumeList();
            }
        });
        navButtons.add(b);
        return b;
    }
}

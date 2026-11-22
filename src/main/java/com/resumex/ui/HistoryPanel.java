package com.resumex.ui;

import com.resumex.dao.AnalysisDao;
import com.resumex.dao.InterviewDao;
import com.resumex.dao.ResumeDao;
import com.resumex.model.AnalysisResult;
import com.resumex.model.InterviewSession;
import com.resumex.model.ResumeRecord;
import com.resumex.model.User;
import com.resumex.ui.components.RoundButton;
import com.resumex.ui.components.RoundPanel;
import com.resumex.ui.components.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Modern History & Analytics panel tracking resume scores and mock interview outcomes.
 */
public class HistoryPanel extends JPanel {

    private User currentUser;
    private final AnalysisDao analysisDao = new AnalysisDao();
    private final ResumeDao resumeDao = new ResumeDao();
    private final InterviewDao interviewDao = new InterviewDao();

    private final AnalyzerPanel analyzerPanel;
    private final InterviewPanel interviewPanel;
    private final MainFrame mainFrame;

    private JTable analysisTable;
    private DefaultTableModel analysisModel;
    
    private JTable interviewTable;
    private DefaultTableModel interviewModel;

    public HistoryPanel(User currentUser, AnalyzerPanel analyzerPanel, InterviewPanel interviewPanel, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.analyzerPanel = analyzerPanel;
        this.interviewPanel = interviewPanel;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);

        JPanel titleGroup = new JPanel(new GridLayout(0, 1, 0, 4));
        titleGroup.setOpaque(false);
        titleGroup.add(Theme.label("History & Analytics", Theme.H1, Theme.TEXT));
        titleGroup.add(Theme.label("View past evaluations, review progress, or double-click to reload a report.", Theme.BODY, Theme.MUTED));
        head.add(titleGroup, BorderLayout.WEST);

        RoundButton refreshBtn = new RoundButton("Refresh", Theme.CARD_HI, Theme.CARD_BORDER);
        refreshBtn.addActionListener(e -> refreshData());
        head.add(refreshBtn, BorderLayout.EAST);

        add(head, BorderLayout.NORTH);

        // Analysis Table
        analysisModel = new DefaultTableModel(new Object[]{"ID", "Date", "Resume File", "Score", "Verdict"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        analysisTable = createStyledTable(analysisModel);
        analysisTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && analysisTable.getSelectedRow() != -1) {
                    loadAnalysis(analysisTable.getSelectedRow());
                }
            }
        });
        
        // Interview Table
        interviewModel = new DefaultTableModel(new Object[]{"ID", "Date", "Resume File", "Final Score", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        interviewTable = createStyledTable(interviewModel);

        JPanel tablesGrid = new JPanel(new GridLayout(2, 1, 0, 16));
        tablesGrid.setOpaque(false);
        
        RoundPanel p1 = new RoundPanel(Theme.CARD, 16);
        p1.setLayout(new BorderLayout(0, 10));
        p1.setBorder(new EmptyBorder(14, 16, 14, 16));
        p1.add(Theme.label("Past Resume Evaluations", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        JScrollPane sp1 = new JScrollPane(analysisTable);
        Theme.styleScroll(sp1);
        p1.add(sp1, BorderLayout.CENTER);
        tablesGrid.add(p1);

        RoundPanel p2 = new RoundPanel(Theme.CARD, 16);
        p2.setLayout(new BorderLayout(0, 10));
        p2.setBorder(new EmptyBorder(14, 16, 14, 16));
        p2.add(Theme.label("Past Mock Interviews", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        JScrollPane sp2 = new JScrollPane(interviewTable);
        Theme.styleScroll(sp2);
        p2.add(sp2, BorderLayout.CENTER);
        tablesGrid.add(p2);

        add(tablesGrid, BorderLayout.CENTER);
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(Theme.INPUT_BG);
        table.setForeground(Theme.TEXT);
        table.setFont(Theme.BODY);
        table.setRowHeight(34);
        table.setSelectionBackground(Theme.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header Styling
        table.getTableHeader().setBackground(Theme.CARD);
        table.getTableHeader().setForeground(Theme.TEXT_SECONDARY);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Theme.INPUT_BG : Theme.CARD);
                }
                return c;
            }
        });

        return table;
    }
    
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void refreshData() {
        if (currentUser == null || currentUser.getId() <= 0) return;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Refresh Analyses
        analysisModel.setRowCount(0);
        try {
            List<AnalysisResult> analyses = analysisDao.findByUser(currentUser.getId());
            for (AnalysisResult a : analyses) {
                Optional<ResumeRecord> r = resumeDao.findById(a.getResumeId());
                String fName = r.map(ResumeRecord::getFileName).orElse("Resume.pdf");
                String verdict = a.getScore() >= 80 ? "Excellent" : a.getScore() >= 60 ? "Competitive" : "Needs Work";
                analysisModel.addRow(new Object[]{
                    a.getId(),
                    a.getCreatedAt() != null ? a.getCreatedAt().format(dtf) : "Recent",
                    fName,
                    a.getScore() + "/100",
                    verdict
                });
            }
        } catch (Exception ignored) {}

        // Refresh Interviews
        interviewModel.setRowCount(0);
        try {
            List<InterviewSession> sessions = interviewDao.findSessionsByUser(currentUser.getId());
            for (InterviewSession s : sessions) {
                Optional<ResumeRecord> r = resumeDao.findById(s.getResumeId());
                String fName = r.map(ResumeRecord::getFileName).orElse("Resume.pdf");
                String fScore = s.getFinalScore() != null ? (s.getFinalScore() + "/100") : "Incomplete";
                String status = s.getFinalScore() != null ? "Completed" : "In Progress";
                interviewModel.addRow(new Object[]{
                    s.getId(),
                    s.getStartedAt() != null ? s.getStartedAt().format(dtf) : "Recent",
                    fName,
                    fScore,
                    status
                });
            }
        } catch (Exception ignored) {}
        
        interviewPanel.refreshResumeList();
    }

    private void loadAnalysis(int row) {
        int id = (int) analysisModel.getValueAt(row, 0);
        String fName = (String) analysisModel.getValueAt(row, 2);
        
        Optional<AnalysisResult> result = analysisDao.findById(id);
        if (result.isPresent()) {
            analyzerPanel.displayLoadedAnalysis(result.get(), fName);
            mainFrame.navigateTo("analyzer");
        } else {
            JOptionPane.showMessageDialog(this, "Could not load analysis details.", "Notice", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

package com.resumex.ui;

import com.resumex.model.AnalysisResult;
import com.resumex.model.User;
import com.resumex.service.AnalysisService;
import com.resumex.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

/**
 * Modern UI for Resume Analyzer with drag-and-drop, role presets,
 * ATS scoring, and seamless interview hand-off.
 */
public class AnalyzerPanel extends JPanel {

    private User currentUser;
    private final AnalysisService analysisService;
    private final MainFrame mainFrame;
    
    private File selectedFile;
    private JLabel fileTitleLabel;
    private JLabel fileSubtitleLabel;
    private JButton clearFileBtn;
    private RoundButton analyzeBtn;
    private JTextArea jobDescArea;
    private JPanel resultsHost;
    private JProgressBar progressBar;

    public AnalyzerPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.analysisService = new AnalysisService();
        
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // Header + Control Zone
        JPanel topContainer = new JPanel();
        topContainer.setOpaque(false);
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        // Header Title Banner
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JPanel titleText = new JPanel(new GridLayout(0, 1, 0, 4));
        titleText.setOpaque(false);
        JLabel title = Theme.label("AI Resume Analyzer", Theme.H1, Theme.TEXT);
        JLabel sub = Theme.label("Evaluate your ATS compatibility, uncover skill gaps, and optimize for recruiter screening.", Theme.BODY, Theme.MUTED);
        titleText.add(title);
        titleText.add(sub);
        titleRow.add(titleText, BorderLayout.WEST);

        topContainer.add(titleRow);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(buildInputSection());
        add(topContainer, BorderLayout.NORTH);

        // Scrollable Results Area
        resultsHost = new JPanel(new BorderLayout());
        resultsHost.setOpaque(false);
        resultsHost.add(buildWelcomePlaceholder(), BorderLayout.CENTER);
        
        JScrollPane sp = new JScrollPane(resultsHost);
        Theme.styleScroll(sp);
        add(sp, BorderLayout.CENTER);
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    private JPanel buildInputSection() {
        JPanel container = new JPanel(new GridLayout(1, 2, 16, 0));
        container.setOpaque(false);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        container.setPreferredSize(new Dimension(900, 210));

        // Left Card: Upload & Drop Zone
        RoundPanel uploadCard = new RoundPanel(Theme.CARD, 16);
        uploadCard.setLayout(new BorderLayout(12, 12));
        uploadCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel uploadHeader = new JPanel(new BorderLayout());
        uploadHeader.setOpaque(false);
        uploadHeader.add(Theme.label("Resume Document", Theme.H2, Theme.TEXT), BorderLayout.WEST);

        clearFileBtn = new JButton("Clear");
        clearFileBtn.setFont(Theme.SMALL_BOLD);
        clearFileBtn.setForeground(Theme.MUTED);
        clearFileBtn.setContentAreaFilled(false);
        clearFileBtn.setBorderPainted(false);
        clearFileBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFileBtn.setVisible(false);
        clearFileBtn.addActionListener(e -> resetFileSelection());
        uploadHeader.add(clearFileBtn, BorderLayout.EAST);
        uploadCard.add(uploadHeader, BorderLayout.NORTH);

        // Drop zone inner container
        RoundPanel dropZone = new RoundPanel(Theme.INPUT_BG, 12, Theme.CARD_BORDER);
        dropZone.setLayout(new GridBagLayout());
        dropZone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel dropContent = new JPanel();
        dropContent.setOpaque(false);
        dropContent.setLayout(new BoxLayout(dropContent, BoxLayout.Y_AXIS));

        // Clean vector icon guaranteed to render sharply across all platforms
        JComponent vectorDocIcon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int cy = h / 2;
                
                int docW = 32;
                int docH = 38;
                int dx = cx - docW / 2;
                int dy = cy - docH / 2;
                
                // Document shape with folded corner
                g2.setColor(Theme.CARD_HI);
                g2.fillRoundRect(dx, dy, docW, docH, 8, 8);
                
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(dx, dy, docW, docH, 8, 8);
                
                // Upload arrow
                g2.setColor(Theme.ACCENT2);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx, cy + 7, cx, cy - 6);
                g2.drawLine(cx, cy - 6, cx - 5, cy - 1);
                g2.drawLine(cx, cy - 6, cx + 5, cy - 1);
                
                g2.dispose();
            }
        };
        vectorDocIcon.setPreferredSize(new Dimension(46, 44));
        vectorDocIcon.setMaximumSize(new Dimension(46, 44));
        vectorDocIcon.setAlignmentX(CENTER_ALIGNMENT);

        fileTitleLabel = Theme.label("Click or Drag & Drop Resume", Theme.BODY_BOLD, Theme.TEXT);
        fileTitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        fileSubtitleLabel = Theme.label("PDF, DOCX, or TXT up to 10MB", Theme.SMALL, Theme.MUTED);
        fileSubtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        dropContent.add(vectorDocIcon);
        dropContent.add(Box.createVerticalStrut(6));
        dropContent.add(fileTitleLabel);
        dropContent.add(Box.createVerticalStrut(2));
        dropContent.add(fileSubtitleLabel);

        dropZone.add(dropContent);
        dropZone.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                handleBrowseFile();
            }
        });

        // Drag & drop support
        dropZone.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport s) {
                return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport s) {
                try {
                    List<File> files = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) setFile(files.get(0));
                    return true;
                } catch (Exception ex) { return false; }
            }
        });

        uploadCard.add(dropZone, BorderLayout.CENTER);

        JPanel bottomUploadRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomUploadRow.setOpaque(false);
        RoundButton browseBtn = new RoundButton("Browse Files...", Theme.CARD_HI, new Color(0x323D61));
        browseBtn.addActionListener(e -> handleBrowseFile());
        bottomUploadRow.add(browseBtn);
        uploadCard.add(bottomUploadRow, BorderLayout.SOUTH);

        // Right Card: Target Job Description (No "(Optional)")
        RoundPanel jobCard = new RoundPanel(Theme.CARD, 16);
        jobCard.setLayout(new BorderLayout(0, 8));
        jobCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel jobHeader = new JPanel(new BorderLayout());
        jobHeader.setOpaque(false);
        jobHeader.add(Theme.label("Target Job Description", Theme.H2, Theme.TEXT), BorderLayout.WEST);

        // Preset chips for rapid testing
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        presets.setOpaque(false);
        presets.add(createPresetChip("Java Backend", "Senior Java Backend Engineer with Spring Boot, Microservices, MySQL, REST API, Redis, Kafka, and Docker experience."));
        presets.add(createPresetChip("Full Stack", "Full Stack Developer proficient in Java, React, TypeScript, Node.js, SQL, AWS, and CI/CD pipelines."));
        presets.add(createPresetChip("Cloud/DevOps", "DevOps Engineer experienced with Kubernetes, Docker, Terraform, AWS, Linux, and automated monitoring."));
        jobHeader.add(presets, BorderLayout.EAST);
        jobCard.add(jobHeader, BorderLayout.NORTH);

        jobDescArea = new JTextArea();
        jobDescArea.setFont(Theme.BODY);
        jobDescArea.setBackground(Theme.INPUT_BG);
        jobDescArea.setForeground(Theme.TEXT);
        jobDescArea.setCaretColor(Theme.TEXT);
        jobDescArea.setLineWrap(true);
        jobDescArea.setWrapStyleWord(true);
        jobDescArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane jdScroll = new JScrollPane(jobDescArea);
        jdScroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        Theme.styleScroll(jdScroll);
        jobCard.add(jdScroll, BorderLayout.CENTER);

        // Action trigger bar
        JPanel actionRow = new JPanel(new BorderLayout(10, 0));
        actionRow.setOpaque(false);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(180, 8));
        progressBar.setBackground(Theme.CARD_HI);
        progressBar.setForeground(Theme.ACCENT);
        actionRow.add(progressBar, BorderLayout.WEST);

        analyzeBtn = new RoundButton("Run AI Analysis", Theme.ACCENT, Theme.ACCENT_HOVER);
        analyzeBtn.setEnabled(false);
        analyzeBtn.addActionListener(e -> runAnalysisLogic());
        actionRow.add(analyzeBtn, BorderLayout.EAST);

        jobCard.add(actionRow, BorderLayout.SOUTH);

        container.add(uploadCard);
        container.add(jobCard);
        return container;
    }

    private JButton createPresetChip(String title, String text) {
        JButton b = new JButton(title);
        b.setFont(Theme.SMALL);
        b.setForeground(Theme.ACCENT2);
        b.setBackground(Theme.INPUT_BG);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1, true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> jobDescArea.setText(text));
        return b;
    }

    private void handleBrowseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Your Resume");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Supported Resumes (*.pdf, *.docx, *.txt)", "pdf", "docx", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            setFile(fc.getSelectedFile());
        }
    }

    private void setFile(File f) {
        if (f == null || !f.exists()) return;
        selectedFile = f;
        fileTitleLabel.setText(f.getName());
        fileSubtitleLabel.setText(String.format("%.1f KB  |  Ready for evaluation", f.length() / 1024.0));
        clearFileBtn.setVisible(true);
        analyzeBtn.setEnabled(true);
    }

    private void resetFileSelection() {
        selectedFile = null;
        fileTitleLabel.setText("Click or Drag & Drop Resume");
        fileSubtitleLabel.setText("PDF, DOCX, or TXT up to 10MB");
        clearFileBtn.setVisible(false);
        analyzeBtn.setEnabled(false);
    }

    private void runAnalysisLogic() {
        if (selectedFile == null) return;

        analyzeBtn.setEnabled(false);
        progressBar.setVisible(true);

        resultsHost.removeAll();
        resultsHost.add(buildLoadingState(), BorderLayout.CENTER);
        resultsHost.revalidate();
        resultsHost.repaint();

        SwingWorker<AnalysisResult, Void> worker = new SwingWorker<>() {
            @Override protected AnalysisResult doInBackground() throws Exception {
                String jobDesc = jobDescArea.getText().trim();
                int uid = (currentUser != null && currentUser.getId() > 0) ? currentUser.getId() : -1;
                return analysisService.analyzeResume(uid, selectedFile, jobDesc);
            }

            @Override protected void done() {
                progressBar.setVisible(false);
                analyzeBtn.setEnabled(true);
                try {
                    AnalysisResult res = get();
                    showResults(res);
                } catch (Exception ex) {
                    resultsHost.removeAll();
                    resultsHost.add(buildErrorPlaceholder(ex.getMessage()), BorderLayout.CENTER);
                    resultsHost.revalidate();
                    resultsHost.repaint();
                }
            }
        };
        worker.execute();
    }

    public void displayLoadedAnalysis(AnalysisResult a, String fileName) {
        selectedFile = null;
        fileTitleLabel.setText(fileName);
        fileSubtitleLabel.setText("Loaded from history database");
        clearFileBtn.setVisible(false);
        analyzeBtn.setEnabled(false);
        showResults(a);
    }

    private void showResults(AnalysisResult a) {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // Top Row: ATS Scorecard + Metric Breakdown
        JPanel row1 = new JPanel(new GridBagLayout());
        row1.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        // Overall Score Card
        RoundPanel gaugeCard = new RoundPanel(Theme.CARD, 16);
        gaugeCard.setLayout(new BorderLayout(0, 10));
        gaugeCard.setBorder(new EmptyBorder(18, 18, 18, 18));
        
        JPanel gaugeHeader = new JPanel(new BorderLayout());
        gaugeHeader.setOpaque(false);
        gaugeHeader.add(Theme.label("ATS Overall Score", Theme.H2, Theme.TEXT), BorderLayout.WEST);
        
        // Tier badge
        String tierText = a.getScore() >= 80 ? "EXCELLENT" : a.getScore() >= 60 ? "COMPETITIVE" : "NEEDS WORK";
        Color tierCol = a.getScore() >= 80 ? Theme.GOOD : a.getScore() >= 60 ? Theme.WARN : Theme.BAD;
        JLabel tierBadge = new JLabel("  " + tierText + "  ");
        tierBadge.setFont(Theme.SMALL_BOLD);
        tierBadge.setForeground(tierCol);
        tierBadge.setBackground(Theme.CARD_HI);
        tierBadge.setOpaque(true);
        tierBadge.setBorder(BorderFactory.createLineBorder(tierCol, 1, true));
        gaugeHeader.add(tierBadge, BorderLayout.EAST);
        gaugeCard.add(gaugeHeader, BorderLayout.NORTH);

        ScoreGauge gauge = new ScoreGauge(a.getScore());
        gaugeCard.add(gauge, BorderLayout.CENTER);

        // Direct Action: Start Interview from this score
        RoundButton launchInterviewBtn = new RoundButton("Start Mock Interview", Theme.ACCENT, Theme.ACCENT_HOVER);
        launchInterviewBtn.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.navigateTo("interview");
            }
        });
        gaugeCard.add(launchInterviewBtn, BorderLayout.SOUTH);

        c.gridx = 0; c.weightx = 0.38; c.insets = new Insets(0, 0, 0, 14);
        row1.add(gaugeCard, c);

        // Metric Breakdown Card
        RoundPanel breakdown = new RoundPanel(Theme.CARD, 16);
        breakdown.setLayout(new GridLayout(0, 1, 0, 8));
        breakdown.setBorder(new EmptyBorder(18, 20, 18, 20));
        breakdown.add(Theme.label("Performance Breakdown", Theme.H2, Theme.TEXT));
        if (a.getBreakdown() != null) {
            a.getBreakdown().forEach((k, v) -> breakdown.add(new MetricBar(k, v)));
        }
        c.gridx = 1; c.weightx = 0.62; c.insets = new Insets(0, 0, 0, 0);
        row1.add(breakdown, c);

        row1.setAlignmentX(LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        body.add(row1);
        body.add(Box.createVerticalStrut(14));

        // Keywords & Skills Chips
        if (a.getSkillsFound() != null && !a.getSkillsFound().isEmpty()) {
            body.add(chipCard("Technical Skills Verified", a.getSkillsFound(), Theme.GOOD));
            body.add(Box.createVerticalStrut(12));
        }
        if (a.getMissingSkills() != null && !a.getMissingSkills().isEmpty()) {
            body.add(chipCard("High-Priority Keywords to Add", a.getMissingSkills(), Theme.WARN));
            body.add(Box.createVerticalStrut(12));
        }
        if (a.getStrongVerbs() != null && !a.getStrongVerbs().isEmpty()) {
            body.add(chipCard("Impact Action Verbs Detected", a.getStrongVerbs(), Theme.ACCENT2));
            body.add(Box.createVerticalStrut(12));
        }

        // Suggestions Card
        if (a.getSuggestions() != null && !a.getSuggestions().isEmpty()) {
            RoundPanel tips = new RoundPanel(Theme.CARD, 16);
            tips.setLayout(new BoxLayout(tips, BoxLayout.Y_AXIS));
            tips.setBorder(new EmptyBorder(18, 20, 18, 20));
            tips.setAlignmentX(LEFT_ALIGNMENT);
            tips.add(Theme.label("Actionable Recommendations", Theme.H2, Theme.TEXT));
            tips.add(Box.createVerticalStrut(10));
            for (String s : a.getSuggestions()) {
                JPanel tipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                tipRow.setOpaque(false);
                JLabel bullet = new JLabel("-");
                bullet.setForeground(Theme.ACCENT);
                bullet.setFont(Theme.BODY_BOLD);
                JLabel text = Theme.label("<html><body style='width:680px'>" + s + "</body></html>", Theme.BODY, Theme.TEXT_SECONDARY);
                tipRow.add(bullet);
                tipRow.add(text);
                tips.add(tipRow);
            }
            body.add(tips);
        }

        // Executive Summary Card
        if (a.getSummary() != null && !a.getSummary().isBlank()) {
            body.add(Box.createVerticalStrut(12));
            RoundPanel summary = new RoundPanel(Theme.CARD, 16);
            summary.setLayout(new BorderLayout(0, 8));
            summary.setBorder(new EmptyBorder(16, 20, 16, 20));
            summary.setAlignmentX(LEFT_ALIGNMENT);
            summary.add(Theme.label("Executive Recruiter Verdict", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
            summary.add(Theme.label("<html><body style='width:680px; line-height:1.4'>" + a.getSummary() + "</body></html>", Theme.BODY, Theme.TEXT_SECONDARY), BorderLayout.CENTER);
            body.add(summary);
        }

        body.add(Box.createVerticalStrut(20));

        resultsHost.removeAll();
        resultsHost.add(body, BorderLayout.NORTH);
        resultsHost.revalidate();
        resultsHost.repaint();
        gauge.animate();
    }

    private RoundPanel chipCard(String title, List<String> items, Color color) {
        RoundPanel card = new RoundPanel(Theme.CARD, 16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(Theme.label(title, Theme.H3, Theme.TEXT), BorderLayout.NORTH);
        
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        flow.setOpaque(false);
        if (items.isEmpty()) {
            flow.add(Theme.label("None detected", Theme.SMALL, Theme.MUTED));
        } else {
            items.forEach(i -> flow.add(new Chip(i, color)));
        }
        card.add(flow, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 60));
        return card;
    }

    private JComponent buildWelcomePlaceholder() {
        RoundPanel p = new RoundPanel(Theme.CARD, 16);
        p.setLayout(new GridBagLayout());
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel h = Theme.label("Ready to Supercharge Your Resume?", Theme.H2, Theme.TEXT);
        h.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = Theme.label("Upload your resume above and optionally paste a job description to get instant ATS grading.", Theme.BODY, Theme.MUTED);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        content.add(h);
        content.add(Box.createVerticalStrut(8));
        content.add(desc);

        p.add(content);
        return p;
    }

    private JComponent buildLoadingState() {
        RoundPanel p = new RoundPanel(Theme.CARD, 16);
        p.setLayout(new GridBagLayout());
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel h = Theme.label("AI Analysis in Progress...", Theme.H2, Theme.ACCENT2);
        h.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = Theme.label("Parsing structure, checking keyword density, and benchmarking against ATS criteria.", Theme.BODY, Theme.MUTED);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        content.add(h);
        content.add(Box.createVerticalStrut(8));
        content.add(desc);

        p.add(content);
        return p;
    }

    private JComponent buildErrorPlaceholder(String error) {
        RoundPanel p = new RoundPanel(Theme.CARD, 16);
        p.setLayout(new GridBagLayout());
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel h = Theme.label("Analysis Encountered an Issue", Theme.H2, Theme.BAD);
        h.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = Theme.label(error != null ? error : "An unknown error occurred.", Theme.BODY, Theme.TEXT_SECONDARY);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        content.add(h);
        content.add(Box.createVerticalStrut(8));
        content.add(desc);

        p.add(content);
        return p;
    }
}

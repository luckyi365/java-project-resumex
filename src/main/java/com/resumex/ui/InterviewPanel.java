package com.resumex.ui;

import com.google.gson.JsonObject;
import com.resumex.dao.ResumeDao;
import com.resumex.model.InterviewSession;
import com.resumex.model.ResumeRecord;
import com.resumex.model.User;
import com.resumex.service.InterviewService;
import com.resumex.ui.components.RoundButton;
import com.resumex.ui.components.RoundPanel;
import com.resumex.ui.components.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Modern AI Mock Interview interface simulating real-time recruiter interviews.
 */
public class InterviewPanel extends JPanel {

    private User currentUser;
    private final InterviewService interviewService;
    private final ResumeDao resumeDao;
    private final MainFrame mainFrame;
    
    private InterviewSession currentSession;

    private JPanel chatList;
    private JScrollPane chatScroll;
    private JTextField chatInput;
    private RoundButton sendBtn;
    private JComboBox<ResumeItem> resumeCombo;
    private RoundButton startBtn;
    private JLabel progressLabel;

    public InterviewPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.interviewService = new InterviewService();
        this.resumeDao = new ResumeDao();

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // Top Control Header
        JPanel topHeader = new JPanel();
        topHeader.setOpaque(false);
        topHeader.setLayout(new BoxLayout(topHeader, BoxLayout.Y_AXIS));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JPanel titleText = new JPanel(new GridLayout(0, 1, 0, 4));
        titleText.setOpaque(false);
        titleText.add(Theme.label("AI Mock Interviewer", Theme.H1, Theme.TEXT));
        titleText.add(Theme.label("Interactive voice-like technical interview based directly on your resume background.", Theme.BODY, Theme.MUTED));
        titleRow.add(titleText, BorderLayout.WEST);

        topHeader.add(titleRow);
        topHeader.add(Box.createVerticalStrut(14));

        // Session controls bar
        RoundPanel controlCard = new RoundPanel(Theme.CARD, 14);
        controlCard.setLayout(new BorderLayout(12, 0));
        controlCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftControls.setOpaque(false);
        leftControls.add(Theme.label("Select Resume:", Theme.BODY_BOLD, Theme.TEXT));
        
        resumeCombo = new JComboBox<>();
        resumeCombo.setFont(Theme.BODY);
        resumeCombo.setBackground(Theme.INPUT_BG);
        resumeCombo.setForeground(Theme.TEXT);
        resumeCombo.setPreferredSize(new Dimension(280, 32));
        resumeCombo.addActionListener(e -> {
            ResumeItem selected = (ResumeItem) resumeCombo.getSelectedItem();
            startBtn.setEnabled(selected != null && selected.id > 0);
        });
        leftControls.add(resumeCombo);

        controlCard.add(leftControls, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        progressLabel = Theme.label("Status: Idle", Theme.SMALL_BOLD, Theme.MUTED);
        rightControls.add(progressLabel);

        startBtn = new RoundButton("Start Interview", Theme.ACCENT, Theme.ACCENT_HOVER);
        startBtn.addActionListener(e -> startInterview());
        rightControls.add(startBtn);

        controlCard.add(rightControls, BorderLayout.EAST);
        topHeader.add(controlCard);

        add(topHeader, BorderLayout.NORTH);

        // Chat Container
        RoundPanel chatFrame = new RoundPanel(Theme.CARD, 16);
        chatFrame.setLayout(new BorderLayout());
        chatFrame.setBorder(new EmptyBorder(12, 14, 12, 14));

        chatList = new JPanel();
        chatList.setLayout(new BoxLayout(chatList, BoxLayout.Y_AXIS));
        chatList.setOpaque(false);

        JPanel chatWrap = new JPanel(new BorderLayout());
        chatWrap.setOpaque(false);
        chatWrap.add(chatList, BorderLayout.NORTH);

        chatScroll = new JScrollPane(chatWrap);
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        Theme.styleScroll(chatScroll);
        chatFrame.add(chatScroll, BorderLayout.CENTER);

        // Bottom Input Area
        RoundPanel inputBar = new RoundPanel(Theme.INPUT_BG, 14, Theme.CARD_BORDER);
        inputBar.setLayout(new BorderLayout(10, 0));
        inputBar.setBorder(new EmptyBorder(6, 12, 6, 8));

        chatInput = new JTextField();
        chatInput.setFont(Theme.BODY);
        chatInput.setBackground(Theme.INPUT_BG);
        chatInput.setForeground(Theme.TEXT);
        chatInput.setCaretColor(Theme.TEXT);
        chatInput.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        chatInput.setEnabled(false);
        chatInput.addActionListener(e -> handleChatSend());

        sendBtn = new RoundButton("Send", Theme.ACCENT, Theme.ACCENT_HOVER);
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(e -> handleChatSend());

        inputBar.add(chatInput, BorderLayout.CENTER);
        inputBar.add(sendBtn, BorderLayout.EAST);
        chatFrame.add(inputBar, BorderLayout.SOUTH);

        add(chatFrame, BorderLayout.CENTER);

        refreshResumeList();
        addAiMessage("Welcome to your Mock Interview! Select your resume from the dropdown above and click 'Start Interview' to begin.", null, null);
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        refreshResumeList();
    }

    public void refreshResumeList() {
        resumeCombo.removeAllItems();
        if (currentUser != null && currentUser.getId() > 0) {
            try {
                List<ResumeRecord> resumes = resumeDao.findByUser(currentUser.getId());
                for (ResumeRecord r : resumes) {
                    resumeCombo.addItem(new ResumeItem(r.getId(), r.getFileName()));
                }
            } catch (Exception ignored) {}
        }
        
        // When no resumes are uploaded yet, show clear prompt placeholder
        if (resumeCombo.getItemCount() == 0) {
            resumeCombo.addItem(new ResumeItem(-1, "Select a resume..."));
            startBtn.setEnabled(false);
        } else {
            startBtn.setEnabled(true);
        }
    }

    private void startInterview() {
        ResumeItem item = (ResumeItem) resumeCombo.getSelectedItem();
        if (item == null || item.id <= 0) {
            JOptionPane.showMessageDialog(this, "Please upload a resume in the Resume Analyzer first.", "No Resume Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        chatList.removeAll();
        chatList.revalidate();
        chatList.repaint();

        startBtn.setEnabled(false);
        resumeCombo.setEnabled(false);
        progressLabel.setText("Question 1 of 5");
        progressLabel.setForeground(Theme.ACCENT2);

        try {
            currentSession = interviewService.startSession(item.id);
            addAiMessage("Reviewing your resume profile... Preparing tailored technical questions.", null, null);
            doAiTurn("ready");
        } catch (Exception ex) {
            addAiMessage("Unable to initialize session: " + ex.getMessage(), null, null);
            startBtn.setEnabled(true);
            resumeCombo.setEnabled(true);
            progressLabel.setText("Status: Idle");
        }
    }

    private void handleChatSend() {
        String text = chatInput.getText().trim();
        if (text.isEmpty() || currentSession == null) return;
        
        chatInput.setText("");
        addUserMessage(text);
        doAiTurn(text);
    }

    private void doAiTurn(String userText) {
        JLabel typingLabel = addAiMessage("Evaluating response and formulating next question...", null, null);
        chatInput.setEnabled(false);
        sendBtn.setEnabled(false);

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                return interviewService.handleTurn(currentSession, userText);
            }

            @Override
            protected void done() {
                try {
                    JsonObject aiJson = get();
                    String feedback = aiJson.has("feedback") && !aiJson.get("feedback").isJsonNull() ? aiJson.get("feedback").getAsString() : "";
                    Integer score = aiJson.has("answer_score") && !aiJson.get("answer_score").isJsonNull() ? aiJson.get("answer_score").getAsInt() : null;
                    String nextQuestion = aiJson.has("next_question") && !aiJson.get("next_question").isJsonNull() ? aiJson.get("next_question").getAsString() : null;
                    String summary = aiJson.has("summary") && !aiJson.get("summary").isJsonNull() ? aiJson.get("summary").getAsString() : null;

                    // Remove temporary typing placeholder
                    Container parent = typingLabel.getParent().getParent();
                    chatList.remove(parent);

                    if (nextQuestion != null) {
                        int qNum = currentSession.getQuestionCount() + 1;
                        progressLabel.setText("Question " + Math.min(qNum, 5) + " of 5");
                        addAiMessage(nextQuestion, feedback, score);
                        chatInput.setEnabled(true);
                        sendBtn.setEnabled(true);
                        chatInput.requestFocusInWindow();
                    } else {
                        // Finished
                        progressLabel.setText("Interview Completed");
                        progressLabel.setForeground(Theme.GOOD);
                        addAiMessage("Interview Completed! " + (summary != null ? summary : "Great job completing your mock interview!"), feedback, score);
                        startBtn.setEnabled(true);
                        resumeCombo.setEnabled(true);
                    }
                } catch (Exception ex) {
                    typingLabel.setText("Error: " + ex.getMessage());
                    chatInput.setEnabled(true);
                    sendBtn.setEnabled(true);
                }
                scrollChatToBottom();
            }
        };
        worker.execute();
    }

    private void addUserMessage(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        row.setOpaque(false);

        RoundPanel bubble = new RoundPanel(Theme.ACCENT, 14, Theme.ACCENT_HOVER);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel msg = new JLabel("<html><body style='width:420px; color:#FFFFFF;'>" + escape(text) + "</body></html>");
        msg.setFont(Theme.BODY);
        bubble.add(msg);

        row.add(bubble);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 10));
        chatList.add(row);
        chatList.revalidate();
        scrollChatToBottom();
    }

    private JLabel addAiMessage(String mainContent, String feedback, Integer score) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        row.setOpaque(false);

        RoundPanel bubble = new RoundPanel(Theme.CARD_HI, 14, Theme.CARD_BORDER);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Recruiter label
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel name = Theme.label("AI Recruiter", Theme.SMALL_BOLD, Theme.ACCENT2);
        header.add(name, BorderLayout.WEST);

        if (score != null && score > 0) {
            Color sCol = score >= 8 ? Theme.GOOD : score >= 6 ? Theme.WARN : Theme.BAD;
            JLabel scorePill = new JLabel(" Answer: " + score + "/10 ");
            scorePill.setFont(Theme.SMALL_BOLD);
            scorePill.setForeground(sCol);
            scorePill.setBackground(Theme.INPUT_BG);
            scorePill.setOpaque(true);
            scorePill.setBorder(BorderFactory.createLineBorder(sCol, 1, true));
            header.add(scorePill, BorderLayout.EAST);
        }
        bubble.add(header);
        bubble.add(Box.createVerticalStrut(6));

        // Feedback section if available
        if (feedback != null && !feedback.isBlank()) {
            JPanel fbCard = new JPanel(new BorderLayout());
            fbCard.setBackground(Theme.INPUT_BG);
            fbCard.setBorder(new EmptyBorder(8, 10, 8, 10));
            JLabel fbLabel = Theme.label("<html><body style='width:460px; color:#9CA3AF;'><i>Feedback: " + escape(feedback) + "</i></body></html>", Theme.SMALL, Theme.MUTED);
            fbCard.add(fbLabel);
            bubble.add(fbCard);
            bubble.add(Box.createVerticalStrut(8));
        }

        // Question or summary text
        JLabel textLabel = new JLabel("<html><body style='width:460px; line-height:1.4; color:#F3F4F6;'>" + escape(mainContent) + "</body></html>");
        textLabel.setFont(Theme.BODY);
        bubble.add(textLabel);

        row.add(bubble);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 10));
        chatList.add(row);
        chatList.revalidate();
        scrollChatToBottom();
        return textLabel;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    private void scrollChatToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = chatScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private static class ResumeItem {
        final int id;
        final String name;
        ResumeItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}

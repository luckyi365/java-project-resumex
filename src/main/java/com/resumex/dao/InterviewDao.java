package com.resumex.dao;

import com.resumex.model.ChatMessage;
import com.resumex.model.InterviewSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access object for the {@code interview_sessions} and
 * {@code interview_messages} tables.
 */
public class InterviewDao {

    private static final Logger LOG = Logger.getLogger(InterviewDao.class.getName());

    // ---- Sessions ----

    /**
     * Inserts a new session and sets the generated ID.
     */
    public int insertSession(InterviewSession s) {
        String sql = "INSERT INTO interview_sessions (resume_id) VALUES (?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getResumeId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    s.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to insert session: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Updates an existing session with the final score and summary.
     */
    public void updateSession(InterviewSession s) {
        if (s.getId() == 0) return;
        String sql = "UPDATE interview_sessions SET final_score = ?, summary = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (s.getFinalScore() != null) ps.setInt(1, s.getFinalScore());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, s.getSummary());
            ps.setInt(3, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to update session: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches all sessions for a user.
     */
    public List<InterviewSession> findSessionsByUser(int userId) {
        List<InterviewSession> list = new ArrayList<>();
        String sql = "SELECT s.id, s.resume_id, s.started_at, s.final_score, s.summary " +
                     "FROM interview_sessions s JOIN resumes r ON s.resume_id = r.id " +
                     "WHERE r.user_id = ? ORDER BY s.started_at DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InterviewSession s = new InterviewSession();
                    s.setId(rs.getInt("id"));
                    s.setResumeId(rs.getInt("resume_id"));
                    Timestamp ts = rs.getTimestamp("started_at");
                    if (ts != null) s.setStartedAt(ts.toLocalDateTime());
                    int fs = rs.getInt("final_score");
                    if (!rs.wasNull()) s.setFinalScore(fs);
                    s.setSummary(rs.getString("summary"));
                    // We don't hydrate questionCount here since it's transient
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to list sessions: " + e.getMessage(), e);
        }
        return list;
    }

    // ---- Messages ----

    /**
     * Inserts a single chat message.
     */
    public int insertMessage(ChatMessage m) {
        String sql = "INSERT INTO interview_messages (session_id, role, content, answer_score) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getSessionId());
            ps.setString(2, m.getRole());
            ps.setString(3, m.getContent());
            if (m.getAnswerScore() != null) ps.setInt(4, m.getAnswerScore());
            else ps.setNull(4, Types.INTEGER);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    m.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to insert message: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Fetches all messages for a session in chronological order.
     */
    public List<ChatMessage> findMessagesBySession(int sessionId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT id, session_id, role, content, answer_score, created_at " +
                     "FROM interview_messages WHERE session_id = ? ORDER BY created_at ASC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage m = new ChatMessage();
                    m.setId(rs.getInt("id"));
                    m.setSessionId(rs.getInt("session_id"));
                    m.setRole(rs.getString("role"));
                    m.setContent(rs.getString("content"));
                    int as = rs.getInt("answer_score");
                    if (!rs.wasNull()) m.setAnswerScore(as);
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to list messages: " + e.getMessage(), e);
        }
        return list;
    }
}

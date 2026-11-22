package com.resumex.dao;

import com.resumex.model.ResumeRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access object for the {@code resumes} table.
 */
public class ResumeDao {

    private static final Logger LOG = Logger.getLogger(ResumeDao.class.getName());

    /**
     * Inserts a resume record and sets the generated ID.
     *
     * @param r the resume record to persist
     * @return the generated primary key, or -1 on failure
     */
    public int insert(ResumeRecord r) {
        String sql = "INSERT INTO resumes (user_id, file_name, extracted_text) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setString(2, r.getFileName());
            ps.setString(3, r.getExtractedText());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    r.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to insert resume: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Fetches a resume by its primary key.
     *
     * @param id the resume ID
     * @return the record, or empty if not found
     */
    public Optional<ResumeRecord> findById(int id) {
        String sql = "SELECT id, user_id, file_name, extracted_text, uploaded_at FROM resumes WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find resume: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns all resumes for a given user, newest first.
     *
     * @param userId the owning user's ID
     * @return list of resume records
     */
    public List<ResumeRecord> findByUser(int userId) {
        List<ResumeRecord> list = new ArrayList<>();
        String sql = "SELECT id, user_id, file_name, extracted_text, uploaded_at " +
                     "FROM resumes WHERE user_id = ? ORDER BY uploaded_at DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to list resumes: " + e.getMessage(), e);
        }
        return list;
    }

    private ResumeRecord mapRow(ResultSet rs) throws SQLException {
        ResumeRecord r = new ResumeRecord();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setFileName(rs.getString("file_name"));
        r.setExtractedText(rs.getString("extracted_text"));
        Timestamp ts = rs.getTimestamp("uploaded_at");
        if (ts != null) r.setUploadedAt(ts.toLocalDateTime());
        return r;
    }
}

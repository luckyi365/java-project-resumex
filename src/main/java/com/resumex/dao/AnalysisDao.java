package com.resumex.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.resumex.model.AnalysisResult;

import java.sql.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access object for the {@code analyses} table.
 * Breakdown, missing-skills and suggestions are serialised as JSON strings.
 */
public class AnalysisDao {

    private static final Logger LOG = Logger.getLogger(AnalysisDao.class.getName());
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    /**
     * Persists an analysis result and sets the generated ID.
     *
     * @param a the analysis to save
     * @return the generated primary key, or -1 on failure
     */
    public int insert(AnalysisResult a) {
        String sql = "INSERT INTO analyses (resume_id, score, breakdown_json, " +
                     "missing_skills, suggestions_json, raw_response) VALUES (?,?,?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getResumeId());
            ps.setInt(2, a.getScore());
            ps.setString(3, GSON.toJson(a.getBreakdown()));
            ps.setString(4, GSON.toJson(a.getMissingSkills()));
            ps.setString(5, GSON.toJson(a.getSuggestions()));
            ps.setString(6, a.getRawResponse());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    a.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to insert analysis: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Fetches all analyses for the resumes belonging to a given user,
     * newest first.
     *
     * @param userId the user ID
     * @return list of analysis results
     */
    public List<AnalysisResult> findByUser(int userId) {
        List<AnalysisResult> list = new ArrayList<>();
        String sql = "SELECT a.id, a.resume_id, a.score, a.breakdown_json, " +
                     "a.missing_skills, a.suggestions_json, a.raw_response, a.created_at " +
                     "FROM analyses a JOIN resumes r ON a.resume_id = r.id " +
                     "WHERE r.user_id = ? ORDER BY a.created_at DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to list analyses: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Fetches a single analysis by ID.
     *
     * @param id the analysis primary key
     * @return the result, or empty if not found
     */
    public Optional<AnalysisResult> findById(int id) {
        String sql = "SELECT id, resume_id, score, breakdown_json, missing_skills, " +
                     "suggestions_json, raw_response, created_at FROM analyses WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find analysis: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ---- Internal ----

    private AnalysisResult mapRow(ResultSet rs) throws SQLException {
        AnalysisResult a = new AnalysisResult();
        a.setId(rs.getInt("id"));
        a.setResumeId(rs.getInt("resume_id"));
        a.setScore(rs.getInt("score"));

        String bj = rs.getString("breakdown_json");
        if (bj != null && !bj.isBlank()) {
            a.setBreakdown(GSON.fromJson(bj, MAP_TYPE));
        }

        String ms = rs.getString("missing_skills");
        if (ms != null && !ms.isBlank()) {
            a.setMissingSkills(GSON.fromJson(ms, LIST_TYPE));
        }

        String sj = rs.getString("suggestions_json");
        if (sj != null && !sj.isBlank()) {
            a.setSuggestions(GSON.fromJson(sj, LIST_TYPE));
        }

        // Reconstruct additional fields from raw_response if available
        String raw = rs.getString("raw_response");
        a.setRawResponse(raw != null ? raw : "");
        if (raw != null && !raw.isBlank()) {
            try {
                var map = GSON.fromJson(raw, new TypeToken<Map<String, Object>>() {}.getType());
                if (map instanceof Map<?,?> m) {
                    @SuppressWarnings("unchecked")
                    var rawMap = (Map<String, Object>) m;
                    if (rawMap.containsKey("strong_verbs")) {
                        @SuppressWarnings("unchecked")
                        var verbs = (List<String>) rawMap.get("strong_verbs");
                        a.setStrongVerbs(verbs != null ? verbs : new ArrayList<>());
                    }
                    if (rawMap.containsKey("skills_found")) {
                        @SuppressWarnings("unchecked")
                        var skills = (List<String>) rawMap.get("skills_found");
                        a.setSkillsFound(skills != null ? skills : new ArrayList<>());
                    }
                    if (rawMap.containsKey("summary")) {
                        a.setSummary(String.valueOf(rawMap.get("summary")));
                    }
                    if (rawMap.containsKey("fallback")) {
                        a.setFallback(Boolean.TRUE.equals(rawMap.get("fallback")));
                    }
                }
            } catch (Exception e) {
                LOG.fine("Could not parse raw_response for extra fields: " + e.getMessage());
            }
        }

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
        return a;
    }
}

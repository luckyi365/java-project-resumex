package com.resumex.dao;

import com.resumex.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access object for the {@code users} table.
 */
public class UserDao {

    private static final Logger LOG = Logger.getLogger(UserDao.class.getName());

    /**
     * Inserts a new user and sets the generated ID on the model.
     *
     * @param user the user to insert (must have name and email)
     * @return the generated primary key, or -1 on failure
     */
    public int insert(User user) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    user.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to insert user: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Finds a user by email address.
     *
     * @param email the unique email to search for
     * @return the user, or empty if not found
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, name, email, created_at FROM users WHERE email = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find user by email: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns all registered users, ordered by name.
     *
     * @return list of users (may be empty)
     */
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, name, email, created_at FROM users ORDER BY name";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to list users: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Finds or creates a user by email. If the email already exists,
     * the existing user is returned; otherwise a new row is inserted.
     *
     * @param name  display name (used only for creation)
     * @param email unique email address
     * @return the resolved User (may have id == -1 if DB is down)
     */
    public User findOrCreate(String name, String email) {
        Optional<User> existing = findByEmail(email);
        if (existing.isPresent()) return existing.get();
        User u = new User(name, email);
        insert(u);
        return u;
    }

    // ---- Internal ----

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}

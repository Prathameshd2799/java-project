package automailpro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryDB {

    private static final String DB_URL = "jdbc:sqlite:history.db";

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "purpose TEXT," +
                    "tone TEXT," +
                    "receiver TEXT," +
                    "points TEXT," +
                    "email TEXT," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "is_favorite INTEGER DEFAULT 0," +
                    "template_type TEXT DEFAULT ''" +
                    ");";

    public static void init() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            Class.forName("org.sqlite.JDBC");
            stmt.execute(CREATE_TABLE_SQL);

            try { stmt.execute("ALTER TABLE history ADD COLUMN is_favorite INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE history ADD COLUMN template_type TEXT DEFAULT '';"); } catch (SQLException ignored) {}

            System.out.println("✅ SQLite database ready: " + new java.io.File("history.db").getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ DB init error: " + e.getMessage());
        }
    }

    public static void save(String purpose, String tone, String receiver,
                            String points, String email, String templateType) {

        String sql = "INSERT INTO history (purpose, tone, receiver, points, email, template_type, is_favorite) VALUES (?,?,?,?,?,?,0)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, purpose);
            stmt.setString(2, tone);
            stmt.setString(3, receiver);
            stmt.setString(4, points);
            stmt.setString(5, email);
            stmt.setString(6, templateType == null ? "" : templateType);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Insert Error: " + e.getMessage());
        }
    }

    public static void toggleFavorite(int id) {
        String sql = "UPDATE history SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Toggle Favorite Error: " + e.getMessage());
        }
    }

    public static void deleteById(int id) {
        String sql = "DELETE FROM history WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Delete Error: " + e.getMessage());
        }
    }

    public static void clearAll() {
        String sql = "DELETE FROM history";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Clear History Error: " + e.getMessage());
        }
    }

    public static List<HistoryItem> list() {
        List<HistoryItem> list = new ArrayList<>();
        String sql = "SELECT * FROM history ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new HistoryItem(
                        rs.getInt("id"),
                        rs.getString("purpose"),
                        rs.getString("tone"),
                        rs.getString("receiver"),
                        rs.getString("points"),
                        rs.getString("email"),
                        rs.getString("created_at"),
                        rs.getInt("is_favorite"),
                        rs.getString("template_type")
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ Fetch Error: " + e.getMessage());
        }

        return list;
    }
}

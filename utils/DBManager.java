package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.ResultSet;

public class DBManager {
    private static final String DB_URL = "jdbc:sqlite:searchengine.db";
    
    public static Connection connect() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
            return null;
        }
    }

    // 1. Create tables (updated with links and pagerank columns)
    public static void initDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();

            String pagesSql = """
                CREATE TABLE IF NOT EXISTS pages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    url TEXT UNIQUE,
                    title TEXT,
                    content TEXT,
                    domain TEXT,
                    links TEXT,               -- ✅ stores outbound links
                    pagerank REAL DEFAULT 1.0,-- ✅ PageRank score
                    crawled_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;

            String invertedSql = """
                CREATE TABLE IF NOT EXISTS inverted_index (
                    term TEXT,
                    url TEXT,
                    score REAL
                );
            """;

            stmt.execute(pagesSql);
            stmt.execute(invertedSql);

            System.out.println("✅ Tables 'pages' and 'inverted_index' are ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. Save a page with outbound links
    public static void savePage(String url, String title, String content, List<String> links) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String domain = new URL(url).getHost();
            String sql = "INSERT OR REPLACE INTO pages (url, title, content, domain, links) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, url);
            pstmt.setString(2, title);
            pstmt.setString(3, content);
            pstmt.setString(4, domain);
            pstmt.setString(5, String.join(",", links));
            pstmt.executeUpdate();
            System.out.println("✅ Saved page: " + url);
        } catch (Exception e) {
            System.out.println("⚠️ DB insert failed for: " + url);
        }
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:searchengine.db");
     Statement stmt = conn.createStatement()) {
    stmt.executeUpdate("ALTER TABLE pages ADD COLUMN pagerank REAL DEFAULT 0.0");
    System.out.println("✅ 'pagerank' column added to pages table.");
} catch (SQLException e) {
    if (!e.getMessage().contains("duplicate column")) {
        e.printStackTrace();  // ignore if already added
    }
}

    }

    // 3. Create inverted index table
    public static void createInvertedIndexTable() {
    try (Connection conn = connect();
         Statement stmt = conn.createStatement()) {

        String sql = "CREATE TABLE IF NOT EXISTS pages (" +
             "url TEXT PRIMARY KEY, " +
             "title TEXT, " +
             "content TEXT, " +
             "pagerank REAL DEFAULT 0" +     // ✅ Add this line
             ");";

        stmt.execute(sql);
    } catch (SQLException e) {
        System.out.println("❌ Failed to create inverted_index table: " + e.getMessage());
    }
}



    // 4. User query table setup
    public static void createUserQueryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_queries (" +
                     "user_id TEXT, " +
                     "query TEXT, " +
                     "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("📘 User queries table ready.");
        } catch (SQLException e) {
            System.out.println("❌ Error creating user query table: " + e.getMessage());
        }
    }

    // 5. Save user query
    public static void saveUserQuery(String userId, String query) {
        String sql = "INSERT INTO user_queries (user_id, query) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, query);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error saving user query: " + e.getMessage());
        }
    }

    // 6. Get user query history
    public static List<String> getUserQueryHistory(String userId) {
        List<String> queries = new ArrayList<>();
        String sql = "SELECT query FROM user_queries WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                queries.add(rs.getString("query").toLowerCase());
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to fetch user query history: " + e.getMessage());
        }
        return queries;
    }

    // 7. Fetch URL → Outbound Links map for PageRank
    public static Map<String, List<String>> getAllPagesWithLinks() {
        Map<String, List<String>> linkGraph = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT url, links FROM pages");
            while (rs.next()) {
                String url = rs.getString("url");
                String linkStr = rs.getString("links");
                List<String> outLinks = new ArrayList<>();
                if (linkStr != null && !linkStr.isBlank()) {
                    for (String link : linkStr.split(",")) {
                        outLinks.add(link.trim());
                    }
                }
                linkGraph.put(url, outLinks);
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching links: " + e.getMessage());
        }
        return linkGraph;
    }

    // 8. Update PageRank score for a page
    public static void updatePageRank(String url, double score) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "UPDATE pages SET pagerank = ? WHERE url = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, score);
            pstmt.setString(2, url);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("❌ Failed to update PageRank: " + e.getMessage());
        }
    }
}

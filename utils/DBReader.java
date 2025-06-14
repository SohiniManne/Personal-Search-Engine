package utils;

import java.sql.*;
import java.util.Map;
import java.util.HashMap;

public class DBReader {
    private static final String DB_URL = "jdbc:sqlite:searchengine.db";

    public static void readCrawledData() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT url, title FROM pages")) {

            System.out.println("🗂 Crawled Pages:");
            while (rs.next()) {
                String url = rs.getString("url");
                String title = rs.getString("title");
                System.out.println("🔗 " + url);
                System.out.println("📄 " + title);
                System.out.println("------------------------------------------------");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error reading DB: " + e.getMessage());
        }
    }
    
    public static Map<String, String> getAllPages() {
    Map<String, String> pages = new HashMap<>();
    String DB_URL = "jdbc:sqlite:searchengine.db"; // adjust if needed

    try (Connection conn = DriverManager.getConnection(DB_URL);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT url, content FROM pages")) {

        while (rs.next()) {
            String url = rs.getString("url");
            String content = rs.getString("content");
            pages.put(url, content);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return pages;
}

}

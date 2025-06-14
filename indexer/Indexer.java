package indexer;

import java.sql.*;
import java.util.*;
import autocomplete.AutocompleteTrie;
import utils.DBManager;
import utils.DBReader;

public class Indexer {
    private static final String DB_URL = "jdbc:sqlite:searchengine.db";

    private Map<String, Map<String, Double>> index = new HashMap<>();
    private Map<String, String> urlToSnippet = new HashMap<>();
    private AutocompleteTrie trie = new AutocompleteTrie();

    public void buildIndex() {
        Map<String, String> docs = loadDocuments();
        Map<String, String> pages = DBReader.getAllPages();  // or DBManager.getAllPages()
        Map<String, Integer> docFreq = new HashMap<>();
        Map<String, Map<String, Integer>> termFreqs = new HashMap<>();

        int totalDocs = docs.size();

        for (Map.Entry<String, String> entry : docs.entrySet()) {
            String url = entry.getKey();
            String content = entry.getValue().toLowerCase().replaceAll("[^a-z0-9 ]", " ");
            String[] words = content.split("\\s+");

            Map<String, Integer> tf = new HashMap<>();
            Set<String> seen = new HashSet<>();

            for (String word : words) {
                if (word.isBlank()) continue;
                tf.put(word, tf.getOrDefault(word, 0) + 1);
                if (seen.add(word)) {
                    docFreq.put(word, docFreq.getOrDefault(word, 0) + 1);
                }
            }
            termFreqs.put(url, tf);
            urlToSnippet.put(url, extractSnippet(content, words));
        }

        for (String word : docFreq.keySet()) {
            Map<String, Double> tfidfScores = new HashMap<>();
            for (String url : termFreqs.keySet()) {
                Map<String, Integer> tf = termFreqs.get(url);
                if (!tf.containsKey(word)) continue;

                double tfVal = tf.get(word);
                double idfVal = Math.log((double) totalDocs / docFreq.get(word));
                tfidfScores.put(url, tfVal * idfVal);
            }
            index.put(word, tfidfScores);
            trie.insert(word);
        }

        System.out.println("✅ Index size: " + index.size());
        System.out.println("🔍 Total pages fetched from DB: " + pages.size());

        saveIndexToDB();
    }

    private Map<String, String> loadDocuments() {
        Map<String, String> docs = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT url, content FROM pages");
            while (rs.next()) {
                docs.put(rs.getString("url"), rs.getString("content"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return docs;
    }

    private void saveIndexToDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.createStatement().executeUpdate("DELETE FROM inverted_index");

            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO inverted_index (term, url, score) VALUES (?, ?, ?)"
            );

            for (String term : index.keySet()) {
                for (Map.Entry<String, Double> entry : index.get(term).entrySet()) {
                    pstmt.setString(1, term);
                    pstmt.setString(2, entry.getKey());
                    pstmt.setDouble(3, entry.getValue());
                    pstmt.addBatch();
                }
            }

            pstmt.executeBatch();
            System.out.println("✅ Inverted index saved to database.");
        } catch (SQLException e) {
            System.out.println("❌ Error saving inverted index: " + e.getMessage());
        }
    }

    public List<String> getAutocompleteSuggestions(String prefix) {
        return trie.autocomplete(prefix.toLowerCase());
    }

    private String extractSnippet(String content, String[] words) {
        for (String word : words) {
            if (word.isBlank()) continue;
            int idx = content.indexOf(word);
            if (idx != -1) {
                int start = Math.max(0, idx - 60);
                int end = Math.min(content.length(), idx + 100);
                return content.substring(start, end).replaceAll("\\s+", " ");
            }
        }
        return content.substring(0, Math.min(160, content.length())).replaceAll("\\s+", " ");
    }

    private boolean isUrlAllowed(String url, String domainFilter, String afterDate) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT domain, crawled_at FROM pages WHERE url = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, url);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String domain = rs.getString("domain");
                String crawledAt = rs.getString("crawled_at");

                if (domainFilter != null && !domain.contains(domainFilter)) return false;
                if (afterDate != null && crawledAt.compareTo(afterDate) < 0) return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public List<String> search(String query, String userId, String domainFilter, String afterDate) {
        query = query.toLowerCase();
        String[] words = query.split("\\s+");

        Map<String, Double> scores = new HashMap<>();

        for (String word : words) {
            Map<String, Double> tfidfScores = index.getOrDefault(word, new HashMap<>());
            for (Map.Entry<String, Double> entry : tfidfScores.entrySet()) {
                String url = entry.getKey();
                if (!isUrlAllowed(url, domainFilter, afterDate)) continue;
                scores.put(url, scores.getOrDefault(url, 0.0) + entry.getValue());
            }
        }

        // Personalization
        if (userId != null) {
            List<String> pastQueries = DBManager.getUserQueryHistory(userId);
            Set<String> pastTerms = new HashSet<>();
            for (String q : pastQueries) {
                for (String w : q.split("\\s+")) {
                    pastTerms.add(w);
                }
            }
            for (String word : pastTerms) {
                Map<String, Double> pastScores = index.getOrDefault(word, new HashMap<>());
                for (Map.Entry<String, Double> entry : pastScores.entrySet()) {
                    String url = entry.getKey();
                    scores.put(url, scores.getOrDefault(url, 0.0) + entry.getValue() * 0.5);
                }
            }
        }

        // ✅ Boost with PageRank (BEFORE sorting)
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT pagerank FROM pages WHERE url = ?");
            for (String url : scores.keySet()) {
                ps.setString(1, url);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double pr = rs.getDouble("pagerank");
                    scores.put(url, scores.get(url) + pr * 0.2);  // optional scaling
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Sort and return
        List<String> results = new ArrayList<>(scores.keySet());
        results.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));

        for (String url : results) {
            String snippet = urlToSnippet.getOrDefault(url, "(No snippet available)");
            for (String word : words) {
                snippet = snippet.replaceAll("(?i)(" + word + ")", "\u001B[1m$1\u001B[0m");
            }
            System.out.println("🔗 " + url);
            System.out.println("📝 " + snippet);
            System.out.println();
        }

        return results;
    }

    public void computePageRank() {
        final double d = 0.85;
        final int iterations = 20;
        final Map<String, Set<String>> incomingLinks = new HashMap<>();
        final Map<String, Set<String>> outgoingLinks = new HashMap<>();
        final Map<String, Double> pageRank = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT url, links FROM pages");
            while (rs.next()) {
                String url = rs.getString("url");
                String linksStr = rs.getString("links");
                Set<String> links = new HashSet<>();
                if (linksStr != null && !linksStr.isBlank()) {
                    links.addAll(Arrays.asList(linksStr.split(",")));
                }
                outgoingLinks.put(url, links);
                pageRank.put(url, 1.0);
                for (String link : links) {
                    incomingLinks.computeIfAbsent(link, k -> new HashSet<>()).add(url);
                }
            }

            for (int i = 0; i < iterations; i++) {
                Map<String, Double> newRanks = new HashMap<>();
                for (String page : pageRank.keySet()) {
                    double rank = 1 - d;
                    Set<String> incoming = incomingLinks.getOrDefault(page, Collections.emptySet());
                    for (String inPage : incoming) {
                        int outCount = outgoingLinks.getOrDefault(inPage, Collections.emptySet()).size();
                        if (outCount > 0) {
                            rank += d * (pageRank.get(inPage) / outCount);
                        }
                    }
                    newRanks.put(page, rank);
                }
                pageRank.putAll(newRanks);
            }

            PreparedStatement pstmt = conn.prepareStatement("UPDATE pages SET pagerank = ? WHERE url = ?");
            for (Map.Entry<String, Double> entry : pageRank.entrySet()) {
                pstmt.setDouble(1, entry.getValue());
                pstmt.setString(2, entry.getKey());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            System.out.println("📊 PageRank computation complete.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

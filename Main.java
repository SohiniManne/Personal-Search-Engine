import crawler.WebCrawler;
import utils.DBManager;
import indexer.Indexer;
import search.SearchEngine;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Step 1: Crawl websites and save to DB
        WebCrawler crawler = new WebCrawler();
        crawler.crawl("https://example.com", 1);  // Crawl first ✅

        // Step 2: Build the index
        Indexer indexer = new Indexer();
        indexer.buildIndex();  // Build index after pages are in DB ✅

        // Step 3: Use search engine to query
        SearchEngine engine = new SearchEngine();  // Loads inverted index

        String userId = "user123";
        List<String> results = engine.search(userId, "example domain");

        // Step 4: Autocomplete suggestions
        List<String> suggestions = engine.autocomplete("ex");
        System.out.println("💡 Autocomplete suggestions for 'ex': " + suggestions);

        System.out.println("🔍 Search results for 'example domain':");
        for (String url : results) {
            System.out.println("🔗 " + url);
        }
    }
}

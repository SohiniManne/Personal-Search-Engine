package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import utils.DBManager;

public class WebCrawler {
    private Set<String> visited = new HashSet<>();

    public void crawl(String url, int depth) {
        if (depth == 0 || visited.contains(url)) return;

        try {
            visited.add(url);
            Document doc = Jsoup.connect(url).userAgent("Mozilla").get();

            // 1. Page title
            String title = doc.title();

            // 2. Full visible text (no scripts, ads, etc.)
            String text = doc.body().text();

            // 3. All absolute links on the page
            Set<String> linksSet = new HashSet<>();
            Elements anchorTags = doc.select("a[href]");
            for (Element a : anchorTags) {
                String link = a.absUrl("href");
                if (link.startsWith("http") && !visited.contains(link)) {
                    linksSet.add(link);
                }
            }

            List<String> linksList = new ArrayList<>(linksSet);

            // ✅ Save page to database (including links)
            DBManager.savePage(url, title, text, linksList);

            // Output basic info
            System.out.println("📄 Title: " + title);
            System.out.println("🔗 Links found: " + linksList.size());
            System.out.println("📝 Text snippet: " + text.substring(0, Math.min(300, text.length())) + "...");
            System.out.println("------------------------------------------------");

            // Recursive crawl
            for (String link : linksList) {
                crawl(link, depth - 1);
            }

        } catch (IOException e) {
            System.out.println("⚠️ Failed to crawl: " + url);
        }
    }
}

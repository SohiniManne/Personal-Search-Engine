package search;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;

public class SearchGUI {
    private SearchEngine engine;

    public SearchGUI() {
        engine = new SearchEngine();  // ✅ Your backend search engine
        createUI();
    }

    private void createUI() {
        JFrame frame = new JFrame("Personal Search Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        JPanel panel = new JPanel(new BorderLayout());

        // 🔎 Top: search bar
        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // 📄 Center: clickable results area
        JEditorPane resultPane = new JEditorPane();
        resultPane.setContentType("text/html");
        resultPane.setEditable(false);

        // 🌐 Enable hyperlink clicks to open browser
        resultPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultPane);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 🔍 Search button action
        searchButton.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                List<String> results = engine.search("user123", query);  // your search method
                if (results.isEmpty()) {
                    resultPane.setText("<html><body>No results found.</body></html>");
                } else {
                    StringBuilder html = new StringBuilder("<html><body>");
                    for (String url : results) {
                        html.append("🔗 <a href='").append(url).append("'>").append(url).append("</a><br>");
                    }
                    html.append("</body></html>");
                    resultPane.setText(html.toString());
                }
            }
        });

        frame.add(panel);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SearchGUI::new);
    }
}

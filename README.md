# 🔍 Personal Search Engine

A desktop-based mini search engine built with Java, SQLite, and Swing. It crawls, indexes, stores, and retrieves web pages locally. The GUI allows you to perform searches with autocomplete, ranked results (via PageRank), and see page titles, snippets, and clickable URLs.

---

## ✨ Features

- ✅ Full-text search using an inverted index
- ✅ Boosted results with PageRank scoring
- ✅ Titles, snippets, and clickable links in results
- ✅ GUI interface using Java Swing
- ✅ Autocomplete suggestions while typing
- ✅ Query logging per user in the database
- ✅ Local SQLite database storage

---


---

## 💾 Database Schema

- **pages**  
  Stores crawled web pages.  
  `url TEXT PRIMARY KEY, title TEXT, content TEXT, pagerank REAL`

- **inverted_index**  
  Stores word-to-URL mappings for search.  
  `term TEXT, url TEXT`

- **user_queries**  
  Logs user queries.  
  `userid TEXT, query TEXT, timestamp DATETIME`

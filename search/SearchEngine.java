package search;

import indexer.Indexer;
import utils.DBManager;
import java.util.List;

public class SearchEngine {
    private final Indexer indexer;

    public SearchEngine() {
        DBManager.initDB();
        DBManager.createInvertedIndexTable();
        DBManager.createUserQueryTable();
        indexer = new Indexer();
        indexer.buildIndex();
    }

    public List<String> search(String userId, String query) {
    DBManager.saveUserQuery(userId, query);                 
    return indexer.search(query, userId, null, null);       
}


    public List<String> autocomplete(String prefix) {
        return indexer.getAutocompleteSuggestions(prefix);
    }
}

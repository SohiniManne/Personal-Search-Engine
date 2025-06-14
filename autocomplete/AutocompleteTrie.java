package autocomplete;

import java.util.*;

public class AutocompleteTrie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isWord = false;
    }

    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.isWord = true;
    }

    public List<String> autocomplete(String prefix) {
        List<String> results = new ArrayList<>();
        TrieNode node = root;

        for (char ch : prefix.toCharArray()) {
            node = node.children.get(ch);
            if (node == null) return results;
        }

        dfs(node, new StringBuilder(prefix), results);
        return results;
    }

    private void dfs(TrieNode node, StringBuilder prefix, List<String> results) {
        if (node.isWord) results.add(prefix.toString());
        for (char ch : node.children.keySet()) {
            prefix.append(ch);
            dfs(node.children.get(ch), prefix, results);
            prefix.setLength(prefix.length() - 1);
        }
    }
}

package frengine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class FileWorker implements Runnable {
    private final List<Integer> docIDs;
    private final Map<Integer, String> idToPath;
    private final IndexStore store;
    private static final Pattern WORD = Pattern.compile("[a-zA-Z0-9_-]{4,}");

    public FileWorker(List<Integer> docIDs, Map<Integer, String> idToPath, IndexStore store) {
        this.docIDs = docIDs;
        this.idToPath = idToPath;
        this.store = store;
    }

    @Override
    public void run() {
        for (int docId : docIDs) {
            String path = idToPath.get(docId);
            if (path == null) continue;
            Map<String, Integer> termFreq = new HashMap<>();
            try {
                String content = Files.readString(Paths.get(path));
                Matcher m = WORD.matcher(content);
                while (m.find()) {
                    String t = m.group().toLowerCase();
                    termFreq.put(t, termFreq.getOrDefault(t, 0) + 1);
                }
                if (!termFreq.isEmpty()) {
                    store.updateIndex(docId, termFreq);
                }
            } catch (IOException e) {
                System.err.println("Failed to read " + path + ": " + e.getMessage());
            }
        }
    }
}

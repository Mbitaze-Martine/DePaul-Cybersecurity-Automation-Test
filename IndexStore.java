package frengine;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

public class IndexStore {
    // DocumentMap: docPath -> docID
    private final Map<String, Integer> docPathToId = new HashMap<>();
    private final Map<Integer, String> idToDocPath = new HashMap<>();
    private final AtomicInteger nextDocId = new AtomicInteger(1);
    private final ReadWriteLock docMapLock = new ReentrantReadWriteLock();

    // TermInvertedIndex: term -> list of (docID, freq)
    private final Map<String, List<TermEntry>> inverted = new HashMap<>();
    private final ReadWriteLock invertedLock = new ReentrantReadWriteLock();

    // Single putDocument (thread-safe)
    public int putDocument(String path) {
        docMapLock.writeLock().lock();
        try {
            if (docPathToId.containsKey(path)) return docPathToId.get(path);
            int id = nextDocId.getAndIncrement();
            docPathToId.put(path, id);
            idToDocPath.put(id, path);
            return id;
        } finally {
            docMapLock.writeLock().unlock();
        }
    }

    // getDocument
    public String getDocument(int docID) {
        docMapLock.readLock().lock();
        try {
            return idToDocPath.get(docID);
        } finally {
            docMapLock.readLock().unlock();
        }
    }

    // updateIndex: receives docID and map<term, freq>; must be thread-safe
    public void updateIndex(int docID, Map<String, Integer> termFreqs) {
        invertedLock.writeLock().lock();
        try {
            for (Map.Entry<String, Integer> e : termFreqs.entrySet()) {
                String term = e.getKey();
                int freq = e.getValue();
                List<TermEntry> list = inverted.computeIfAbsent(term, k -> new ArrayList<>());
                list.add(new TermEntry(docID, freq));
            }
        } finally {
            invertedLock.writeLock().unlock();
        }
    }

    // lookupIndex: returns list of (docID, freq) for a term
    public List<TermEntry> lookupIndex(String term) {
        invertedLock.readLock().lock();
        try {
            List<TermEntry> arr = inverted.get(term);
            if (arr == null) return Collections.emptyList();
            // return a copy to avoid concurrent modification surprises
            return new ArrayList<>(arr);
        } finally {
            invertedLock.readLock().unlock();
        }
    }

    // Helper class
    public static class TermEntry {
        public final int docId;
        public final int freq;
        public TermEntry(int d, int f) { this.docId = d; this.freq = f; }
    }

    // For reporting / debugging
    public int documentCount() {
        docMapLock.readLock().lock();
        try { return docPathToId.size(); }
        finally { docMapLock.readLock().unlock(); }
    }
}

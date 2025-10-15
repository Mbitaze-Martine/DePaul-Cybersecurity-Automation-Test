package frengine;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class ProcessingEngine {
    private final IndexStore store;
    private final int workerCount;

    public ProcessingEngine(IndexStore s, int workers) {
        if (workers <= 0) throw new IllegalArgumentException("workers must be >= 1");
        this.store = s;
        this.workerCount = workers;
    }

    
    // Crawl directory recursively and collect all .txt files from subfolders
    private List<Path> crawlFolder(String folder) throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get(folder))) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .collect(Collectors.toList());
        }
    }


    /**
     * Index the folder using workerCount worker threads.
     * Returns a Map with keys: "timeSec" (Double), "throughputMBs" (Double),
     * "totalBytes" (Long) and "files" (Integer).
     */
    public Map<String, Object> indexFolder(String folderPath) throws IOException, InterruptedException {
        List<Path> files = crawlFolder(folderPath);
        if (files.isEmpty()) {
            System.out.println("No .txt files found in folder: " + folderPath);
            Map<String, Object> empty = new HashMap<>();
            empty.put("timeSec", 0.0);
            empty.put("throughputMBs", 0.0);
            empty.put("totalBytes", 0L);
            empty.put("files", 0);
            return empty;
        }

        // Register documents and obtain docIDs
        Map<Integer, String> idToPath = new HashMap<>();
        for (Path p : files) {
            int id = store.putDocument(p.toString());
            idToPath.put(id, p.toString());
        }

        // compute dataset size
        long totalBytes = 0L;
        for (Path p : files) {
            try {
                totalBytes += Files.size(p);
            } catch (IOException e) {
                // skip file size if unreadable, but print a warning
                System.err.println("Warning: cannot get size for " + p + ": " + e.getMessage());
            }
        }

        // Partition docIDs among worker threads (avoid creating empty thread pool tasks)
        List<Integer> allDocIds = new ArrayList<>(idToPath.keySet());
        Collections.sort(allDocIds);

        // If fewer documents than workers, reduce actualThreads
        int actualThreads = Math.min(Math.max(1, workerCount), Math.max(1, allDocIds.size()));

        List<List<Integer>> partitions = partitionList(allDocIds, actualThreads);

        ExecutorService exec = Executors.newFixedThreadPool(actualThreads);
        long start = System.nanoTime();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (List<Integer> part : partitions) {
                if (part.isEmpty()) continue; // safety
                FileWorker w = new FileWorker(part, idToPath, store);
                futures.add(exec.submit(w));
            }
            // wait for completion and propagate exceptions
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException ee) {
                    // unwrap and rethrow as runtime exception so caller sees cause
                    throw new RuntimeException("Worker thread failed: " + ee.getCause(), ee.getCause());
                }
            }
        } finally {
            exec.shutdownNow();
            // Wait a short time for termination (best-effort)
            try {
                if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                    // if still not terminated, proceed — threads should be done or interrupted
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            }
        }
        long end = System.nanoTime();

        double seconds = (end - start) / 1e9;
        double mb = totalBytes / (1024.0 * 1024.0);
        double throughput = seconds > 0 ? (mb / seconds) : 0.0;

        System.out.printf("Indexed %d files (%.2f MB) with %d threads in %.3f s -> %.3f MB/s%n",
                files.size(), mb, actualThreads, seconds, throughput);

        Map<String, Object> stats = new HashMap<>();
        stats.put("timeSec", seconds);
        stats.put("throughputMBs", throughput);
        stats.put("totalBytes", totalBytes);
        stats.put("files", files.size());
        return stats;
    }

    /**
     * Search with up to 3-term AND query (terms already cleaned/split by caller).
     * Returns list of (path, accumulatedFrequency) sorted descending by frequency.
     */
    public List<Map.Entry<String, Integer>> search(String[] terms) {
        if (terms == null || terms.length == 0) return Collections.emptyList();
        String[] qterms = Arrays.stream(terms).map(String::toLowerCase).toArray(String[]::new);

        List<Map<Integer, Integer>> termMaps = new ArrayList<>();
        for (String t : qterms) {
            if (t == null || t.isBlank()) {
                termMaps.add(Collections.emptyMap());
                continue;
            }
            List<IndexStore.TermEntry> list = store.lookupIndex(t);
            Map<Integer, Integer> m = new HashMap<>();
            for (IndexStore.TermEntry e : list) m.put(e.docId, e.freq);
            termMaps.add(m);
        }

        // Intersection with accumulation
        Map<Integer, Integer> accum = new HashMap<>();
        if (termMaps.isEmpty()) return Collections.emptyList();

        Map<Integer, Integer> first = termMaps.get(0);
        accum.putAll(first);

        for (int i = 1; i < termMaps.size(); ++i) {
            Map<Integer, Integer> m = termMaps.get(i);
            Iterator<Map.Entry<Integer, Integer>> it = accum.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> e = it.next();
                int docId = e.getKey();
                if (m.containsKey(docId)) {
                    e.setValue(e.getValue() + m.get(docId));
                } else {
                    it.remove();
                }
            }
        }

        // sort by accumulated frequency desc and take top 10
        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(accum.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int limit = Math.min(10, sorted.size());
        List<Map.Entry<String, Integer>> results = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            int docId = sorted.get(i).getKey();
            int freq = sorted.get(i).getValue();
            String path = store.getDocument(docId);
            results.add(new AbstractMap.SimpleEntry<>(path, freq));
        }
        return results;
    }

    // Helper partitioner: splits list into N roughly equal parts (round-robin)
    private static <T> List<List<T>> partitionList(List<T> src, int parts) {
        List<List<T>> res = new ArrayList<>();
        if (parts <= 0) parts = 1;
        for (int i = 0; i < parts; ++i) res.add(new ArrayList<>());
        int n = src.size();
        for (int i = 0; i < n; ++i) {
            res.get(i % parts).add(src.get(i));
        }
        return res;
    }
}

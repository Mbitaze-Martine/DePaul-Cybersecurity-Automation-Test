package frengine;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    private static final Pattern QUERY_SPLIT = Pattern.compile("\\s+AND\\s+", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -jar filere_eng-1.0-SNAPSHOT-jar-with-dependencies.jar <num_worker_threads>");
            return;
        }
        int workers = Integer.parseInt(args[0]);
        System.out.println("Starting File Retrieval Engine with " + workers + " worker threads.");

        IndexStore store = new IndexStore();
        ProcessingEngine engine = new ProcessingEngine(store, workers);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        System.out.print("> ");
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) { System.out.print("> "); continue; }
            if (line.equalsIgnoreCase("quit")) {
                System.out.println("Quitting gracefully.");
                break;
            } else if (line.toLowerCase().startsWith("index ")) {
                String folder = line.substring(6).trim();
                try {
                    Map<String, Object> stats = engine.indexFolder(folder);
                    System.out.printf("Indexing finished. Time: %.3f s, Throughput: %.3f MB/s, Files: %d%n",
                            (double)stats.get("timeSec"),
                            (double)stats.get("throughputMBs"),
                            (int)stats.get("files"));
                } catch (Exception e) {
                    System.err.println("Indexing failed: " + e.getMessage());
                }
            } else if (line.toLowerCase().startsWith("search ")) {
                String q = line.substring(7).trim();
                String[] terms = QUERY_SPLIT.split(q);
                if (terms.length == 0 || terms.length > 3) {
                    System.out.println("Search supports 1 to 3 terms combined with AND. Example: cats AND dogs");
                } else {
                    List<Map.Entry<String, Integer>> results = engine.search(terms);
                    if (results.isEmpty()) {
                        System.out.println("No documents match the query.");
                    } else {
                        System.out.println("Top results:");
                        int rank = 1;
                        for (Map.Entry<String, Integer> r : results) {
                            System.out.printf("%d. [%d] %s%n", rank++, r.getValue(), r.getKey());
                        }
                    }
                }
            } else {
                System.out.println("Unknown command. Supported: index <folderPath>, search <term1 AND term2...>, quit");
            }
            System.out.print("> ");
        }
    }
}

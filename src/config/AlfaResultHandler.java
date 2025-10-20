package config;

import java.util.List;

public interface AlfaResultHandler {
    /**
     * Method called when a log line matching the filter criteria is found.
     * @param logLine The filtered log line.
     * @param keyword The keyword that caused the filtering (useful to know why it matched).
     */
    void onLogFiltered(String logLine, String keyword);

    /**
     * (Optional) Method that can be called when batch processing is complete.
     * @param filteredLines The list of all log lines filtered in this batch.
     */
    default void onBatchComplete(List<String> filteredLines) {
        System.out.println("Batch processing done. Found " + filteredLines.size() + " lines.");
    }
}

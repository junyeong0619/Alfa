package main.config;

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
    default void onBatchComplete(List<String> filteredLines,String symbol) {
        System.out.println(symbol+": Batch processing done. Found " + filteredLines.size() + " lines.");
    }

    /**
     * (Optional) Method called when an error occurs during log processing.
     * @param pathSymbol The path symbol of the file that caused the error (e.g., "APP_LOG")
     * @param e The exception object that occurred
     */
    default void onError(String pathSymbol, Exception e) {
        System.err.println("[AlfaAgent] Error processing path: " + pathSymbol);
        e.printStackTrace();
    }
}

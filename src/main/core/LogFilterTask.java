package main.core;

import main.config.AlfaConfig;

import java.util.List;

public class LogFilterTask implements Runnable {

    private AlfaConfig config;
    private String pathSymbol;
    private FilterHandler filterHandler;

    /**
     * Constructor: Receives the main.config and the path symbol this task is responsible for.
     */
    public LogFilterTask(AlfaConfig config, String pathSymbol, FilterHandler filterHandler) {
        this.config = config;
        this.pathSymbol = pathSymbol;
        this.filterHandler = filterHandler;
    }

    @Override
    public void run() {
        try {
            List<String> filteredLines = filterHandler.doFilter();

            config.getResultHandler().onBatchComplete(filteredLines, pathSymbol);

        } catch (Exception e) {
            config.getResultHandler().onError(pathSymbol, e);
        }
    }

    public void close() {
        if (filterHandler != null) {
            filterHandler.close();
        }
    }
}
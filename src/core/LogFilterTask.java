package core;

import config.AlfaConfig;
import java.io.IOException;
import java.util.List;

public class LogFilterTask implements Runnable {

    private AlfaConfig config;
    private String pathSymbol;
    private FilterHandler filterHandler;

    /**
     * Constructor: Receives the config and the path symbol this task is responsible for.
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
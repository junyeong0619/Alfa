package core;

import config.AlfaConfig;
import java.io.IOException;

public class Thread implements Runnable {

    private AlfaConfig config;
    private String pathSymbol;

    /**
     * Constructor: Receives the config and the path symbol this task is responsible for.
     */
    public Thread(AlfaConfig config, String pathSymbol) {
        this.config = config;
        this.pathSymbol = pathSymbol;
    }

    @Override
    public void run() {
        // Creates a new FilterHandler when this thread runs.
        FilterHandler filterHandler = new FilterHandler(config);
        try {
            // Performs the filtering task using its assigned pathSymbol.
            filterHandler.doFilter(pathSymbol);
        } catch (IOException e) {

            config.getResultHandler().onError(pathSymbol, e);
        }
    }
}
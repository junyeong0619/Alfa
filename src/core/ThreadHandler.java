package core;

import config.AlfaConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ThreadHandler {

    // The threads list holds core.Thread (Runnable) objects.
    private List<LogFilterTask> threads = new ArrayList<>();
    private AlfaConfig config;

    public ThreadHandler(AlfaConfig config) {
        this.config = config;
        Set<String> absPathSymbols = config.getAbsPathSymbols();

        // For every configured file path (symbol)
        for (String symbol : absPathSymbols) {
            // Create a core.Thread (Runnable) object (modified in step 1) and add it to the list.
            threads.add(new LogFilterTask(config, symbol));
        }
    }

    // Add a getter so that external classes (e.g., BatchHandler) can retrieve the list of tasks.
    public List<LogFilterTask> getRunnableTasks() {
        return threads;
    }
}
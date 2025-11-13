package main.core;

import main.config.AlfaConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ThreadHandler {

    // The threads list holds main.core.Thread (Runnable) objects.
    private List<LogFilterTask> tasks = new ArrayList<>();
    private AlfaConfig config;

    public ThreadHandler(AlfaConfig config) {
        this.config = config;
    }

    public void initializeTasks() {
        Set<String> absPathSymbols = config.getAbsPathSymbols();

        for (String symbol : absPathSymbols) {
            try {
                FilterHandler handler = new FilterHandler(config, symbol);

                tasks.add(new LogFilterTask(config, symbol, handler));

            } catch (IOException e) {
                config.getResultHandler().onError(symbol, e);
            }
        }
    }

    // Add a getter so that external classes (e.g., BatchHandler) can retrieve the list of tasks.
    public List<LogFilterTask> getRunnableTasks() {
        return tasks;
    }

    public void closeTasks() {
        for (LogFilterTask task : tasks) {
            task.close();
        }
    }
}
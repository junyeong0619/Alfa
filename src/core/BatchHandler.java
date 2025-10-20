package core;

import config.AlfaConfig;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BatchHandler {
    private ThreadHandler threadHandler;
    private ScheduledExecutorService scheduler;
    private AlfaConfig config;

    public BatchHandler(ThreadHandler threadHandler, AlfaConfig config) {
        this.threadHandler = threadHandler;
        this.config = config;
        int poolSize = threadHandler.getRunnableTasks().size();
        this.scheduler = Executors.newScheduledThreadPool(poolSize > 0 ? poolSize : 1);
    }

    /**
     * Starts the periodic batch processing.
     */
    public void startBatchProcessing() {
        int batchTime = config.getBatchTime();
        for (Runnable task : threadHandler.getRunnableTasks()) {
            scheduler.scheduleAtFixedRate(task, 0, batchTime, TimeUnit.SECONDS);
        }
    }

    /**
     * Shuts down the scheduler.
     */
    public void stopBatchProcessing() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * [Overloaded method]
     * Runs the agent for a specified duration (in seconds) and then stops automatically.
     */
    public void agentOn(Integer runTime, boolean completeMessage) {
        // Pass null for the callback to maintain original behavior
        this.agentOn(runTime, completeMessage, null);
    }

    /**
     * [New method with callback]
     * Runs the agent for a specified duration, then executes a callback upon completion.
     * @param runTime The execution duration in seconds.
     * @param completeMessage Whether to print a completion message.
     * @param onCompleteCallback Callback to run on completion (e.g., update AlfaAgent's state).
     */
    public void agentOn(Integer runTime, boolean completeMessage, Runnable onCompleteCallback) {
        // 1. Validation
        if (runTime == null || runTime <= 0) {
            System.err.println("[AlfaAgent] Error: 'runTime' must be a positive integer.");
            return;
        }

        System.out.println("[AlfaAgent] Starting execution for " + runTime + " seconds...");

        // 2. Start periodic batch processing.
        startBatchProcessing();

        // 3. Schedule the stop task to run after 'runTime' seconds.
        Runnable stopTask = () -> {
            stopBatchProcessing(); // Shuts down the scheduler.

            if (completeMessage) {
                System.out.println("[AlfaAgent] Completed execution after " + runTime + " seconds and stopping.");
            }

            // 4. Execute the callback (e.g., to update AlfaAgent's isRunning state)
            if (onCompleteCallback != null) {
                onCompleteCallback.run();
            }
        };

        // Schedule stopTask to run once after 'runTime' seconds.
        this.scheduler.schedule(stopTask, runTime, TimeUnit.SECONDS);
    }
}
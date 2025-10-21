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
        // Read the pool size from the configuration (default is 10)
        int poolSize = config.getThreadPoolSize();
        // Create the thread pool with the size specified in the config
        this.scheduler = Executors.newScheduledThreadPool(poolSize);
    }

    /**
     * Starts the periodic batch processing.
     */
    public void startBatchProcessing() {
        if(scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(config.getThreadPoolSize());
        }
        int batchTime = config.getBatchTime();
        for (Runnable task : threadHandler.getRunnableTasks()) {
            scheduler.scheduleAtFixedRate(task, 0, batchTime, TimeUnit.SECONDS);
        }
    }

    /**
     * Shuts down the scheduler.
     */
    public void stopBatchProcessing() {
        if (scheduler != null ) {
            scheduler.shutdown();
            try{
                if(!scheduler.awaitTermination(5,TimeUnit.SECONDS)){
                    scheduler.shutdownNow();
                    scheduler.awaitTermination(5,TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        threadHandler.closeTasks();
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
        if (runTime == null || runTime <= 0) {
            System.err.println("[AlfaAgent] Error: 'runTime' must be a positive integer.");
            return;
        }

        System.out.println("[AlfaAgent] Starting execution for " + runTime + " seconds...");

        startBatchProcessing();

        Runnable stopTask = () -> {
            stopBatchProcessing();

            if (completeMessage) {
                System.out.println("[AlfaAgent] Completed execution after " + runTime + " seconds and stopping.");
            }

            if (onCompleteCallback != null) {
                onCompleteCallback.run();
            }
        };

        // Schedule stopTask to run once after 'runTime' seconds.
        this.scheduler.schedule(()->{
            Thread stopThread = new Thread(stopTask, "AlfaAgentStopper");
            stopThread.setDaemon(true);
            stopThread.start();
        }, runTime, TimeUnit.SECONDS);
    }
}
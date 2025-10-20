package agent;

import config.AlfaConfig;
import core.BatchHandler;
import core.PathHandler;
import core.ThreadHandler;

/**
 * User can use Alfa by this class
 */
public class AlfaAgent {

    private AlfaConfig config;
    private PathHandler pathHandler;
    private ThreadHandler threadHandler;
    private BatchHandler batchHandler;

    //this field can be approached by several threads
    private volatile boolean isRunning = false;

    /**
     * Create AlfaAgent
     * Initializes all core components in the constructor.
     * @param config All configurations required to run the agent.
     */
    public AlfaAgent(AlfaConfig config) {
        if (config == null || config.getResultHandler() == null) {
            throw new IllegalArgumentException("AlfaConfig and AlfaResultHandler cannot be null.");
        }
        this.config = config;

        // 1. Initialize PathHandler
        this.pathHandler = new PathHandler(this.config);
        // 2. Initialize ThreadHandler (Runnable tasks)
        this.threadHandler = new ThreadHandler(this.config);
        // 3. Initialize BatchHandler (Scheduler)
        this.batchHandler = new BatchHandler(this.threadHandler, this.config);
    }

    /**
     * Starts the agent. (Indefinite execution)
     * Throws RuntimeException if path verification fails.
     */
    public void start() {
        if (isRunning) {
            System.out.println("[AlfaAgent] Agent is already running.");
            return;
        }

        try {
            System.out.println("[AlfaAgent] Starting path verification...");
            pathHandler.verifyAllPaths(); // 1. Validate file paths
            System.out.println("[AlfaAgent] Path verification complete.");
        } catch (RuntimeException e) {
            System.err.println("[AlfaAgent] Path verification failed: " + e.getMessage());
            return; // Do not start
        }

        System.out.println("[AlfaAgent] Starting agent (indefinite execution)...");
        batchHandler.startBatchProcessing(); // 2. Start batch processing
        isRunning = true;
    }

    /**
     * Starts the agent for a specified duration (in seconds) and then automatically stops.
     * Does not start if path verification fails.
     * @param durationInSeconds The execution duration in seconds.
     */
    public void start(int durationInSeconds) {
        if (isRunning) {
            System.out.println("[AlfaAgent] Agent is already running.");
            return;
        }

        try {
            System.out.println("[AlfaAgent] Starting path verification...");
            pathHandler.verifyAllPaths(); // 1. Validate file paths
            System.out.println("[AlfaAgent] Path verification complete.");
        } catch (RuntimeException e) {
            System.err.println("[AlfaAgent] Path verification failed: " + e.getMessage());
            return; // Do not start
        }

        // 2. Define a callback to set 'isRunning' to false upon stopping
        Runnable onStopCallback = () -> {
            this.isRunning = false;
            System.out.println("[AlfaAgent] Auto-stop task complete.");
        };

        // 3. Call BatchHandler's agentOn (with callback)
        batchHandler.agentOn(durationInSeconds, true, onStopCallback);
        isRunning = true; // Considered 'running' once agentOn is called
    }

    /**
     * Manually stops the running agent.
     */
    public void stop() {
        if (!isRunning) {
            System.out.println("[AlfaAgent] Agent is not running.");
            return;
        }
        System.out.println("[AlfaAgent] Manually stopping agent...");
        batchHandler.stopBatchProcessing();
        isRunning = false;
        System.out.println("[AlfaAgent] Agent stop complete.");
    }

    /**
     * Returns the current running state of the agent.
     * @return true if running
     */
    public boolean isRunning() {
        return isRunning;
    }
}
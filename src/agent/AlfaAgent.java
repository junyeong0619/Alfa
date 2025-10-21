package agent;

import config.AlfaConfig;
import core.BatchHandler;
import core.PathHandler;
import core.ThreadHandler;

import java.util.concurrent.TimeUnit;

/**
 * User can use Alfa by this class
 */
public class AlfaAgent {

    private AlfaConfig config;
    private PathHandler pathHandler;
    private ThreadHandler threadHandler;
    private BatchHandler batchHandler;
    private AlfaNotifier alfaNotifier;
    private Thread notifierThread;

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

        //  Initialize PathHandler
        this.pathHandler = new PathHandler(this.config);
        //  Initialize ThreadHandler (Runnable tasks)
        this.threadHandler = new ThreadHandler(this.config);
        // Initialize BatchHandler (Scheduler)
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
            pathHandler.verifyAllPaths();
            System.out.println("[AlfaAgent] Path verification complete.");
        } catch (RuntimeException e) {
            System.err.println("[AlfaAgent] Path verification failed: " + e.getMessage());
            return;
        }

        System.out.println("[AlfaAgent] Starting file existence notifier...");
        alfaNotifier = new AlfaNotifier(this.config, this);
        notifierThread = new Thread(alfaNotifier);
        notifierThread.setName("AlfaNotifierThread");
        notifierThread.setDaemon(true);
        notifierThread.start();

        System.out.println("[AlfaAgent] Initializing tasks and file resources...");
        threadHandler.initializeTasks();
        System.out.println("[AlfaAgent] Initialization complete.");

        System.out.println("[AlfaAgent] Starting agent (indefinite execution)...");
        batchHandler.startBatchProcessing();
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
            return;
        }

        System.out.println("[AlfaAgent] Starting file existence notifier...");
        alfaNotifier = new AlfaNotifier(this.config, this);
        notifierThread = new Thread(alfaNotifier);
        notifierThread.setName("AlfaNotifierThread");
        notifierThread.setDaemon(true);
        notifierThread.start();

        System.out.println("[AlfaAgent] Initializing tasks and file resources...");
        threadHandler.initializeTasks();
        System.out.println("[AlfaAgent] Initialization complete.");

        Runnable onStopCallback = () -> {
            this.isRunning = false;
            System.out.println("[AlfaAgent] Auto-stop task complete.");
        };

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

        // 1. Notifier 스레드 중지 신호 및 대기
        if (alfaNotifier != null) {
            System.out.println("[AlfaAgent] Stopping file existence notifier...");
            alfaNotifier.stopNotifier();
        }
        if (notifierThread != null && notifierThread.isAlive()) {
            try {
                notifierThread.interrupt();
                notifierThread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[AlfaAgent] Interrupted while waiting for notifier thread to stop.");
            }
        }
        alfaNotifier = null;
        notifierThread = null;

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
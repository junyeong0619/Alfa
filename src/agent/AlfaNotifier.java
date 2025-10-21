package agent; // agent 패키지에 넣는 것을 추천합니다.

import config.AlfaConfig;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Monitors the existence of log files defined in AlfaConfig.
 * If a file that previously existed is found missing (likely due to log rotation),
 * it triggers a restart of the AlfaAgent.
 */
public class AlfaNotifier implements Runnable {

    private final AlfaConfig config;
    private final AlfaAgent agent;
    private final Map<String, Boolean> fileExistenceState;
    private volatile boolean isRunning = true;
    private final long checkIntervalSeconds = 60;

    public AlfaNotifier(AlfaConfig config, AlfaAgent agent) {
        this.config = config;
        this.agent = agent;
        this.fileExistenceState = new HashMap<>();
        initializeFileStates();
    }

    /**
     * Records the initial existence state of all configured log files.
     */
    private void initializeFileStates() {
        Set<String> symbols = config.getAbsPathSymbols();
        Map<String, String> paths = config.getAbsPaths();
        for (String symbol : symbols) {
            String path = paths.get(symbol);
            if (path != null) {
                fileExistenceState.put(path, Files.exists(Paths.get(path)));
            }
        }
    }

    @Override
    public void run() {
        System.out.println("[AlfaNotify] Starting log file monitoring...");
        while (isRunning) {
            try {
                TimeUnit.SECONDS.sleep(checkIntervalSeconds);

                Set<String> symbols = config.getAbsPathSymbols();
                Map<String, String> paths = config.getAbsPaths();
                boolean restartNeeded = false;

                for (String symbol : symbols) {
                    String path = paths.get(symbol);
                    if (path == null) continue;

                    boolean previouslyExisted = fileExistenceState.getOrDefault(path, false);
                    boolean currentlyExists = Files.exists(Paths.get(path));

                    if (previouslyExisted && !currentlyExists) {
                        System.out.println("[AlfaNotify] Log file missing, likely rotated: " + path);
                        restartNeeded = true;
                        break;
                    }

                    fileExistenceState.put(path, currentlyExists);
                }

                if (restartNeeded) {
                    System.out.println("[AlfaNotify] Triggering AlfaAgent restart...");
                    agent.stop();
                    TimeUnit.SECONDS.sleep(2);
                    agent.start();
                    initializeFileStates();
                    System.out.println("[AlfaNotify] AlfaAgent restarted. Resuming monitoring...");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
                System.out.println("[AlfaNotify] Monitoring interrupted.");
            } catch (Exception e) {
                System.err.println("[AlfaNotify] Error during monitoring check: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("[AlfaNotify] Log file monitoring stopped.");
    }

    /**
     * Signals the notifier thread to stop its monitoring loop.
     */
    public void stopNotifier() {
        this.isRunning = false;
    }
}
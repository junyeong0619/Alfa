package config;

import java.util.*;

public class AlfaConfig {


    // user must implement it so that Alfa can know what it should do.
    private AlfaResultHandler resultHandler;

    // <"customer", "/main/customer.log"> user should add paths that they want to manage.
    private Map<String,String> absPaths = new HashMap<String,String>();

    // default batch time is 20 seconds.
    private int batchTime = 20;

    // Default thread pool size is 10.
    private int threadPoolSize = 10;

    // from absPaths ex) <"customer", "/main/customer.log"> path symbols like customer will be collected in it.
    private final  Set<String> absPathSymbols = new HashSet<>();

    // in FilterHandler it will be used by filtering log.
    private Map<String,Set<String>> filterOpts;

    private final Map<String, Long> lastReadPositions = new HashMap<>();

    public AlfaConfig(AlfaResultHandler resultHandler, Map<String, String> absPaths,
                      Map<String, Set<String>> filterOpts, Integer batchTime, Integer threadPoolSize) {
        this.resultHandler = resultHandler;
        this.absPaths = absPaths;
        this.absPathSymbols.addAll(absPaths.keySet());
        this.filterOpts = filterOpts;
        if (batchTime != null) {
            this.batchTime = batchTime;
        }
        if (threadPoolSize != null) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public Map<String, Long> getLastReadPositions() {
        return lastReadPositions;
    }

    public AlfaResultHandler getResultHandler() {
        return resultHandler;
    }

    public void setResultHandler(AlfaResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

    public Map<String, String> getAbsPaths() {
        return absPaths;
    }

    public void setAbsPaths(Map<String, String> absPaths) {
        this.absPaths = absPaths;
    }

    public int getBatchTime() {
        return batchTime;
    }

    public void setBatchTime(int batchTime) {
        this.batchTime = batchTime;
    }

    public Set<String> getAbsPathSymbols() {
        return absPathSymbols;
    }

    public Map<String, Set<String>> getFilterOpts() {
        return filterOpts;
    }

    public void setFilterOpts(Map<String, Set<String>> filterOpts) {
        this.filterOpts = filterOpts;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        // A minimum of 1 thread must be guaranteed.
        if (threadPoolSize <= 0) {
            this.threadPoolSize = 1;
        } else {
            this.threadPoolSize = threadPoolSize;
        }
    }
}

package config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, Long> lastReadPositions = new ConcurrentHashMap<>();

    private Charset fileEncoding = StandardCharsets.UTF_8;

    private boolean notifierEnabled = true;

    private int notifierInterval = 60;

    public AlfaConfig(AlfaResultHandler resultHandler, Map<String, String> absPaths,
                      Map<String, Set<String>> filterOpts, Integer batchTime, Integer threadPoolSize,
                      Integer notifierInterval,Charset fileEncoding, Boolean notifierEnabled) {
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
        if (fileEncoding != null) {
            this.fileEncoding = fileEncoding;
        }
        if (notifierEnabled != null) {
            this.notifierEnabled = notifierEnabled;
        }
        if (notifierInterval != null) {
            this.notifierInterval = notifierInterval;
        }
    }

    public int getNotifierInterval() {
        return notifierInterval;
    }

    public Charset getFileEncoding() {
        return fileEncoding;
    }

    public boolean isNotifierEnabled() {
        return notifierEnabled;
    }

    public void setNotifierEnabled(boolean notifierEnabled) {
        this.notifierEnabled = notifierEnabled;
    }

    public Map<String, Long> getLastReadPositions() {
        return lastReadPositions;
    }

    public AlfaResultHandler getResultHandler() {
        return resultHandler;
    }


    public Map<String, String> getAbsPaths() {
        return absPaths;
    }


    public int getBatchTime() {
        return batchTime;
    }


    public Set<String> getAbsPathSymbols() {
        return absPathSymbols;
    }

    public Map<String, Set<String>> getFilterOpts() {
        return filterOpts;
    }


    public int getThreadPoolSize() {
        return threadPoolSize;
    }

}

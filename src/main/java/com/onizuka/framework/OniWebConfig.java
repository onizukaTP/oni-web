package com.onizuka.framework;

import com.onizuka.framework.util.OniLogger;

public class OniWebConfig {

    private int port = 8080;
    private int workerThreads = Runtime.getRuntime().availableProcessors();
    private boolean enableCors = true;
    private boolean enableRateLimit = true;
    private int maxRequestsPerMinute = 100;
    private String staticDir = "static";
    private OniLogger.Level logLevel = OniLogger.Level.INFO;

    public int getPort() { return port; }
    public OniWebConfig setPort(int port) { this.port = port; return this; }

    public int getWorkerThreads() { return workerThreads; }
    public OniWebConfig setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; return this; }

    public boolean isEnableCors() { return enableCors; }
    public OniWebConfig setEnableCors(boolean enableCors) { this.enableCors = enableCors; return this; }

    public boolean isEnableRateLimit() { return enableRateLimit; }
    public OniWebConfig setEnableRateLimit(boolean enableRateLimit) { this.enableRateLimit = enableRateLimit; return this; }

    public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public OniWebConfig setMaxRequestsPerMinute(int maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; return this; }

    public String getStaticDir() { return staticDir; }
    public OniWebConfig setStaticDir(String staticDir) { this.staticDir = staticDir; return this; }

    public OniLogger.Level getLogLevel() { return logLevel; }
    public OniWebConfig setLogLevel(OniLogger.Level logLevel) { this.logLevel = logLevel; return this; }
}

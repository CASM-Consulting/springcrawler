package com.casm.acled.crawler.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.norconex.jef4.log.FileLogManager;
import org.apache.log4j.Appender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CustomLoggerRepository extends Hierarchy {

    private static final Map<String, String> REGISTRY = new HashMap<>();

    private final Cache<String, Logger> loggerCache;
    private final Cache<String, Appender> appenderCache;
    private final Cache<String, FileLogManager> managerCache;

    private final Path workingDir;

    private final String LOGS = "logs";

    /**
     * Create a new logger hierarchy.
     *
     * @param root The root of the new hierarchy.
     */
    public CustomLoggerRepository(Logger root, Path workingDir) {
        super(root);

        appenderCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
        managerCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
        loggerCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        this.workingDir = workingDir;
    }

    public static void register(String threadGroup, String id) {
        REGISTRY.put(threadGroup, id);
    }


    @Override
    public
    Logger getLogger(String name) {
        String threadGroup = Thread.currentThread().getThreadGroup().getName();

        String appenderName = name + "-" + threadGroup;

        //get a logger as per config
        Logger logger = super.getLogger(appenderName);

        synchronized (loggerCache) {

            if (loggerCache.getIfPresent(appenderName) == null
                    && REGISTRY.containsKey(threadGroup)
                    && !name.equals("com.norconex.jef4.log.FileLogManager")) {

                try {

                    FileLogManager fileLogManager = managerCache.get(threadGroup, () -> {
                        String id = REGISTRY.get(threadGroup);
                        FileLogManager manager = new FileLogManager(workingDir.resolve(id).resolve(LOGS).toString());
                        return manager;
                    });

                    Appender appender = appenderCache.get(appenderName, () -> {
                        String id = REGISTRY.get(threadGroup);
                        return fileLogManager.createAppender(id);
                    });

                    logger.addAppender(appender);
                    loggerCache.put(appenderName, logger);

                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

            }

            return logger;
        }
    }
}

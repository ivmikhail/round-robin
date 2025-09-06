package com.github.ivmikhail.common;

import com.github.ivmikhail.common.http.HttpServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceHelper {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorServiceHelper.class);

    public static void shutdown(ExecutorService executor) {
        String executorType = executor.getClass().getSimpleName();
        try {
            executor.shutdown();
            var isStopped = executor.awaitTermination(20, TimeUnit.SECONDS);
            logger.info("{} stopped: {}", executorType, isStopped);
            if (!isStopped) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            logger.warn("{} shutdown interrupted", executorType, e);
        }
    }
}

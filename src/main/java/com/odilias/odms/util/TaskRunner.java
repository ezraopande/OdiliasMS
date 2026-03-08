package com.odilias.odms.util;

import javafx.application.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class TaskRunner {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("ODMS-Background-Task");
            return t;
        }
    });

    public static void run(Runnable backgroundTask, Runnable uiSuccessTask,
            java.util.function.Consumer<Throwable> uiErrorTask) {
        executor.submit(() -> {
            try {
                backgroundTask.run();
                if (uiSuccessTask != null) {
                    Platform.runLater(uiSuccessTask);
                }
            } catch (Throwable t) {
                Logger.error("Background task execution failed", t);
                if (uiErrorTask != null) {
                    Platform.runLater(() -> uiErrorTask.accept(t));
                }
            }
        });
    }

    public static void shutDown() {
        executor.shutdownNow();
    }
}

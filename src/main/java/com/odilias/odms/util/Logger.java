package com.odilias.odms.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + File.separator + "error.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void log(String message) {
        write("INFO", message, null);
    }

    public static void error(String message, Throwable throwable) {
        write("ERROR", message, throwable);
    }

    private static synchronized void write(String level, String message, Throwable throwable) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
                PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(dtf);
            pw.println(String.format("[%s] [%s] %s", timestamp, level, message));

            if (throwable != null) {
                throwable.printStackTrace(pw);
            }
            pw.println("--------------------------------------------------");

        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}

package com.onizuka.framework.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OniLogger {

    public enum Level {
        DEBUG(1, "\u001B[36m"), // Cyan
        INFO(2, "\u001B[32m"),  // Green
        WARN(3, "\u001B[33m"),  // Yellow
        ERROR(4, "\u001B[31m"); // Red

        final int severity;
        final String color;

        Level(int severity, String color) {
            this.severity = severity;
            this.color = color;
        }
    }

    private static final String RESET = "\u001B[0m";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static Level globalLevel = Level.INFO;

    private final String name;

    private OniLogger(String name) {
        this.name = name;
    }

    public static OniLogger get(Class<?> clazz) {
        return new OniLogger(clazz.getSimpleName());
    }

    public static void setGlobalLevel(Level level) {
        globalLevel = level;
    }

    public void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    public void info(String message) {
        log(Level.INFO, message, null);
    }

    public void warn(String message) {
        log(Level.WARN, message, null);
    }

    public void error(String message) {
        log(Level.ERROR, message, null);
    }

    public void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    private void log(Level level, String message, Throwable throwable) {
        if (level.severity < globalLevel.severity) return;

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String threadName = Thread.currentThread().getName();
        String formatted = String.format("%s [%-5s] [%s] [%s] - %s%s",
                level.color,
                level.name(),
                timestamp,
                threadName,
                name,
                message + RESET
        );

        if (level == Level.ERROR) {
            System.err.println(formatted);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(formatted);
        }
    }
}

package upr.famnit.util;


import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    // Color codes for different log levels
    public static final String RESET = "\u001B[0m";
    public static final String ERROR = "\u001B[31m";
    public static final String SUCCESS = "\u001B[32m";
    public static final String INFO = "\u001B[33m";
    public static final String WARN = "\u001B[34m";
    public static final String NETWORK = "\u001B[35m";
    public static final String STATUS = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Date formatter for the log timestamp
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Method to log messages with different log levels, including colored thread names and timestamp
    public static void log(String msg, LogLevel type) {
        String threadName = Thread.currentThread().getName();
        String timestamp = dateFormat.format(new Date());
        String logMessage;
        switch (type) {
            case warn -> logMessage = WARN + "[" + timestamp + "][" + threadName + "] Warn: " + RESET + msg;
            case error -> logMessage = ERROR + "[" + timestamp + "][" + threadName + "] Error: " + RESET + msg;
            case info -> logMessage = INFO + "[" + timestamp + "][" + threadName + "] Info: " + RESET + msg;
            case network -> logMessage = NETWORK + "[" + timestamp + "][" + threadName + "] Network: " + RESET + msg;
            case status -> logMessage = STATUS + "[" + timestamp + "][" + threadName + "] Status: " + RESET + msg;
            case success -> logMessage = SUCCESS + "[" + timestamp + "][" + threadName + "] Success: " + RESET + msg;
            default -> logMessage = WHITE + "[" + timestamp + "][" + threadName + "] " + msg + RESET;
        }
        System.out.println(logMessage);
    }

    public static void log(String msg) {
        log(msg, LogLevel.success);
    }

    public static void warn(String msg) {
        log(msg, LogLevel.warn);
    }
    public static void error(String msg) {
        log(msg, LogLevel.error);
    }
    public static void info(String msg) {
        log(msg, LogLevel.info);
    }
    public static void network(String msg) {
        log(msg, LogLevel.network);
    }
    public static void status(String msg) {
        log(msg, LogLevel.status);
    }
    public static void success(String msg) {
        log(msg, LogLevel.success);
    }

}

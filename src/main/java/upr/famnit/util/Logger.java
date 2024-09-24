package upr.famnit.util;


import upr.famnit.components.LogLevel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    // Color codes for different log levels
    public static final String RESET = "\u001B[0m";
    public static final String ERROR = "\u001B[31m";
    public static final String WARN = "\u001B[32m";
    public static final String INFO = "\u001B[33m";
    public static final String MISC = "\u001B[34m";
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
            case warn:
                logMessage = WARN + "[" + timestamp + "][" + threadName + "]" + RESET + " " + WARN + "Warn: " + RESET + msg;
                break;
            case error:
                logMessage = ERROR + "[" + timestamp + "][" + threadName + "]" + RESET + " " + ERROR + "Error: " + RESET + msg;
                break;
            case info:
                logMessage = INFO + "[" + timestamp + "][" + threadName + "]" + RESET + " " + INFO + "Info: " + RESET + msg;
                break;
            case network:
                logMessage = NETWORK + "[" + timestamp + "][" + threadName + "]" + RESET + " " + NETWORK + "Network: " + RESET + msg;
                break;
            case status:
                logMessage = STATUS + "[" + timestamp + "][" + threadName + "]" + RESET + " " + STATUS + "Status: " + RESET + msg;
                break;
            default:
                logMessage = WHITE + "[" + timestamp + "][" + threadName + "]" + RESET + " " + WHITE + msg + RESET;
                break;
        }

        System.out.println(logMessage);
    }

    // Default log method that uses LogLevel.INFO
    public static void log(String msg) {
        log(msg, LogLevel.info);
    }
}

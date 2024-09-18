package upr.famnit.util;


public class Logger {

        public static final String RESET = "\u001B[0m";
        public static final String ERROR = "\u001B[31m Error: \u001B[0m ";
        public static final String WARN = "\u001B[32m Warn: \u001B[0m ";
        public static final String INFO = "\u001B[33m Info: \u001B[0m ";
        public static final String MISC = "\u001B[34m Note: \u001B[0m";
        public static final String NETWORK = "\u001B[35m Network \u001B[0m ";
        public static final String STATUS = "\u001B[36m Status \u001B[0m ";
        public static final String WHITE = "\u001B[37m ";

        public static void log(String msg, LogLevel type) {
            switch (type){
                case warn -> System.out.println(WARN + msg);
                case error -> System.out.println(ERROR + msg);
                case info -> System.out.println(INFO + msg);
                case network -> System.out.println(NETWORK + msg);
                case status -> System.out.println(STATUS + msg);
                default -> System.out.println(WHITE + msg);
            }
        }


        public static void log(String msg) {
            log(msg, LogLevel.info);
        }
}

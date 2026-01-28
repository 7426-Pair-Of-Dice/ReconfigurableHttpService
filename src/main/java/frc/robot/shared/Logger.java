package frc.robot.shared;

import static java.util.logging.Level.*;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

    // Private constructor to hide the implicit public one
    private Logger() {
    }

    private static final java.util.logging.Logger robotLogger = java.util.logging.Logger.getLogger(Logger.class.getName());

    public static void printf(String str, double dbl) {
        if (robotLogger.isLoggable(INFO)) {
            robotLogger.log(INFO, String.format(str, dbl));
        }
    }

    public static void printf(String str, int i) {
        if (robotLogger.isLoggable(INFO)) {
            robotLogger.log(INFO, String.format(str, i));
        }
    }
    public static void printf(String str) {
        if (robotLogger.isLoggable(INFO)) {
            robotLogger.log(INFO, str);
        }
    }

    public static void println(String str) {
        if (robotLogger.isLoggable(INFO)) {
            robotLogger.log(INFO, String.format(str));
        }
    }

    public static void debug(String str) {
        if (robotLogger.isLoggable(FINE)) {
            robotLogger.log(FINE, String.format(str));
        }
    }

    public static void error(String str) {
        if (robotLogger.isLoggable(SEVERE)) {
            robotLogger.log(SEVERE, String.format(str));
        }
    }

    public static void error(Throwable t){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        sw.append(t.getMessage());
        sw.append("\n");
        t.printStackTrace(pw);
        robotLogger.log(SEVERE, sw.toString());
    }

    public static boolean isLogging() {
        return robotLogger.isLoggable(INFO);
    }

    
}

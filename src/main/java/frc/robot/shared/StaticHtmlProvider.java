package frc.robot.shared;

public interface StaticHtmlProvider {
    /**
     * Returns the context path (e.g. "/static") to serve the HTML files under.
     */
    String getContextPath();

    /**
     * Returns the classpath location (e.g. "/frc/robot/shared/defaulthtml") where HTML files are found.
     */
    String getClasspathLocation();
}
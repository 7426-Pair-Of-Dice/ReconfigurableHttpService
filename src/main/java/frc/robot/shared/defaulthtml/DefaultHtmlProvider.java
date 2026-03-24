package frc.robot.shared.defaulthtml;

import frc.robot.shared.StaticHtmlProvider;

public class DefaultHtmlProvider implements StaticHtmlProvider {
    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getClasspathLocation() {
        return "/frc/robot/shared/defaulthtml";
    }
}
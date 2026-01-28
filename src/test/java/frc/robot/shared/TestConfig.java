package frc.robot.shared;

public class TestConfig implements ReconfigurableConfig {

    public static int ONE = 1 ;
    public static float TWO = 2.0f ;

    @Override
    public void reconfigure() {
        // No operation for test configuration
    }
    
}

package frc.robot.shared;


public class TestConfig implements ReconfigurableConfig {

    public static int INT_VAL = 10;
    public static double DOUBLE_VAL = 2;
    public static String STRING_VAL = "x";
    public static float FLOAT_VAL = 1.1f ;
    public static boolean BOOL_VAL = true;
    public static int ONE = 1 ;
    public static float TWO = 2.0f ;

    private static String reconfigureTimes = "0 times";
    private static int times = 0 ;


    public static int getTimes() {
        return times;
    }

    public static void setTimes(int times) {
        TestConfig.times = times;
    }

    public static String getReconfigureTimes() {
        return reconfigureTimes;
    }

    public static void setReconfigureTimes(String reconfigureTimes) {
        TestConfig.reconfigureTimes = reconfigureTimes;
    }


    @Override
    public void reconfigure() {
        reconfigureTimes = (++times) + " times" ;
    }
    
}

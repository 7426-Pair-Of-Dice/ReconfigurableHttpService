package frc.robot.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface ReconfigurableConfig {
    public abstract void reconfigure()  ;

    public static List<Class<?>> RECONFIGS = new ArrayList<>();

    public static void addReconfigs(List<Class<?>> reconfigClasses) {
        RECONFIGS.addAll(reconfigClasses) ;
    }
    
    public static List<Class<?>> findReconfigurableConfigs() {
        findReconfigurableClasses() ;
        return RECONFIGS;
    }

    public static boolean isContainReconfig(String className) {
        findReconfigurableClasses() ;
        for (Class<?> reconfig : RECONFIGS) {
            if(reconfig.getName().equals(className)){
                return true ;
            }
        }
        return false ;

    }

     // Static method to populate RECONFIGS
    public static void findReconfigurableClasses() {
        try {
            // Use ServiceLoader to find all implementations of ReconfigurableConfig
            ServiceLoader<ReconfigurableConfig> loader = ServiceLoader.load(ReconfigurableConfig.class);
            for (ReconfigurableConfig instance : loader) {
                Class<? extends ReconfigurableConfig> clazz = instance.getClass();
                if (!RECONFIGS.contains(clazz)) {
                    RECONFIGS.add(clazz);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


   
}

package frc.robot.shared;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public interface ReconfigurableConfig {
    public abstract void reconfigure()  ;

    public static Set<Class<?>> RECONFIGS = new HashSet<>();

    public static void addReconfigs(Set<Class<?>> reconfigClasses) {
        RECONFIGS.addAll(reconfigClasses) ;
    }
    
    public static Set<Class<?>> findReconfigurableConfigs() {
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
    public static void findReconfigurableClassesOLD() {
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

    static void findReconfigurableClasses() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .ignoreClassVisibility()
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesImplementing(ReconfigurableConfig.class.getName())) {

                Class<? extends ReconfigurableConfig> clazz = classInfo.loadClass(ReconfigurableConfig.class);

                RECONFIGS.add(clazz); // Prefer a Set to avoid duplicates
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
}


   
}

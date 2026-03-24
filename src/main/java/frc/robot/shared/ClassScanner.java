package frc.robot.shared;

import java.util.HashSet;
import java.util.Set;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class ClassScanner {
    public static Set<Class<?>> findImplementations(Class<?> iface) {
        Set<Class<?>> result = new HashSet<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .ignoreClassVisibility()
                .scan()) {
            for (ClassInfo classInfo : scanResult.getClassesImplementing(iface.getName())) {
                result.add(classInfo.loadClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

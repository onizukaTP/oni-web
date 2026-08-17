package com.onizuka.framework.util;

import com.onizuka.framework.annotations.Controller;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassScanner {

    private static final OniLogger log = OniLogger.get(ClassScanner.class);

    public static List<Class<?>> findControllers(Class<?> rootClass) {
        List<Class<?>> controllers = new ArrayList<>();
        String packageName = rootClass.getPackageName();
        String path = packageName.replace('.', '/');

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);
            List<File> dirs = new ArrayList<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8)));
            }

            for (File directory : dirs) {
                controllers.addAll(findClasses(directory, packageName));
            }
        } catch (Exception e) {
            log.error("Failed to scan classpath for controllers", e);
        }

        return controllers;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> controllers = new ArrayList<>();
        if (!directory.exists()) return controllers;

        File[] files = directory.listFiles();
        if (files == null) return controllers;

        for (File file : files) {
            if (file.isDirectory()) {
                controllers.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz);
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return controllers;
    }
}

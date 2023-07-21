package com.github.tezvn.starpvp.core.utils;

import com.google.common.collect.Lists;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class ClassFinder {

    private final Plugin plugin;

    private String packagePath = "";

    private List<String> excludes = Lists.newArrayList();

    public ClassFinder(Plugin plugin) {
        this.plugin = plugin;
    }

    public List<Class<?>> find() {
        return find0().stream().filter(s -> {
            String path = s.substring(0, s.lastIndexOf("."));
            if(!path.equalsIgnoreCase(packagePath))
                return false;
            return excludes.stream().noneMatch(c -> c.equals(s));
        }).map(c -> {
            try {
                return Class.forName(c);
            }catch (Exception e) {
                return null;
            }
        }).collect(Collectors.toList());
    }

    public ClassFinder removeClass(Class<?>... classes) {
        this.excludes.addAll(Arrays.stream(classes).map(Class::getName).toList());
        return this;
    }

    public ClassFinder setPackage(Class<?> clazz) {
        return setPackage(clazz.getPackage());
    }

    public ClassFinder setPackage(Package pkg) {
        return setPackage(pkg.getName());
    }

    public ClassFinder setPackage(String path) {
        this.packagePath = path;
        return this;
    }

    private List<String> find0() {
        List<String> classes = Lists.newArrayList();
        try {
            JarInputStream is = new JarInputStream(new FileInputStream(Objects.requireNonNull(getFile())));
            JarEntry entry;
            while ((entry = is.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".class") && !name.startsWith("META-INF")) {
                    String classPath = name.substring(0, entry.getName().length() - 6).replaceAll("/", ".");
                    classes.add(classPath);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return classes;
    }

    private File getFile() {
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);
            return (File) method.invoke(plugin);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}

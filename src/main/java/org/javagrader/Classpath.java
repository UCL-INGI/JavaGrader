package org.javagrader;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class Classpath {

    public static final String SYSTEM_PROPERTY = "java.class.path";
    public final Set<PathElement> pathElements;

    public ClassLoader newClassloader() {
        ClassLoader parent = this.getClass().getClassLoader().getParent();
        return new RestrictedClassLoader(
                pathElements.stream().map(PathElement::toUrl).toArray(URL[]::new),
                parent);
    }

    public ClassLoader newClassloader(Set<String> forbids) {
        ClassLoader parent = this.getClass().getClassLoader().getParent();
        return new RestrictedClassLoader(
                pathElements.stream().map(PathElement::toUrl).toArray(URL[]::new),
                parent, forbids);
    }

    public ClassLoader newClassloader(Set<String> forbid, Set<String> allow) {
        ClassLoader parent = this.getClass().getClassLoader().getParent();
        return new RestrictedClassLoader(
                pathElements.stream().map(PathElement::toUrl).toArray(URL[]::new),
                parent, forbid, allow);
    }

    private Classpath(Set<PathElement> pathElements) {
        this.pathElements = Collections.unmodifiableSet(new TreeSet<>(pathElements));
    }

    public static Classpath current() {
        String rawClasspath = System.getProperty(SYSTEM_PROPERTY);
        Set<PathElement> pathElements = stream(rawClasspath.split(File.pathSeparator))
                .map(PathElement::create)
                .collect(Collectors.toSet());
        return new Classpath(pathElements);
    }

}

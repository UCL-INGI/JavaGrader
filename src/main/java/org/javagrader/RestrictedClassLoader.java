package org.javagrader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class RestrictedClassLoader extends URLClassLoader {

    Set<String> blacklist;
    Set<String> whitelist;

    static Set<String> defaultBlackList = Set.of("java.lang.Thread", "java.lang.ClassLoader");
    static Set<String> defaultWhitelist = null;

    public RestrictedClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, defaultBlackList, defaultWhitelist);
    }

    public RestrictedClassLoader(URL[] urls, ClassLoader parent, Set<String> blacklists) {
        this(urls, parent, blacklists, defaultWhitelist);
    }

    public RestrictedClassLoader(URL[] urls, ClassLoader parent, Set<String> blacklists, Set<String> whitelist) {
        super(urls, parent);
        this.blacklist = new HashSet<>();
        this.blacklist.addAll(blacklists);
        this.blacklist.addAll(defaultBlackList);
        this.whitelist = whitelist;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (isForbidden(name))
            throw new ClassNotFoundException(String.format("%s is forbidden and cannot be imported", name));
        return super.loadClass(name);
    }

    public boolean isForbidden(String name) {
        return (!(whitelist != null && whitelist.contains(name)) && blacklist != null && blacklist.contains(name));
    }
}
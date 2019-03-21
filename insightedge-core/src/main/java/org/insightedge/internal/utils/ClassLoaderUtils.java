package org.insightedge.internal.utils;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.start.ClasspathBuilder;
import com.gigaspaces.start.SystemInfo;
import org.jini.rio.boot.ServiceClassLoader;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.logging.Logger;

/**
 * @author Niv Ingberg
 * @since 14.2
 */
@InternalApi
public class ClassLoaderUtils {
    private static final Logger logger = Logger.getLogger(ClassLoaderUtils.class.getName());

    public static void addSparkJars(ServiceClassLoader classLoader) {
        logger.fine("Adding spark jars");
        try {
            classLoader.addURLs(getSparkClassPath().toURLs());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to load spark jars", e);
        }
    }

    public static ClasspathBuilder getSparkClassPath() {
        return getSparkClassPath(ClassLoaderUtils::sparkJarsFilter);
    }

    public static ClasspathBuilder getSparkClassPath(FileFilter sparkJarsFilter) {
        return new ClasspathBuilder()
                //.appendPlatform("scala")
                .append(path(SystemInfo.singleton().locations().getSparkHome(), "jars"), sparkJarsFilter);
    }

    private static boolean sparkJarsFilter(File path) {
        String jarName = path.getName();
        return  !jarName.startsWith("xerces") &&
                !jarName.startsWith("log4j-") &&
                !jarName.contains("slf4j-");
    }

    private static String path(String ... tokens) {
        return String.join(File.separator, tokens);
    }
}

/*
 * Copyright (c) 2019, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.insightedge.internal.utils;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.start.ClasspathBuilder;
import com.gigaspaces.start.SystemInfo;
import org.jini.rio.boot.ServiceClassLoader;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.nio.file.Paths;
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
                .append(Paths.get(SystemInfo.singleton().locations().getSparkHome(), "jars"), sparkJarsFilter);
    }

    private static boolean sparkJarsFilter(File path) {
        String jarName = path.getName();
        return  !jarName.startsWith("xerces") &&
                !jarName.startsWith("log4j-") &&
                !jarName.contains("slf4j-");
    }
}

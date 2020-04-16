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
package org.insightedge.spark;

import com.gigaspaces.internal.io.IOUtils;
import org.apache.spark.TaskContext;
import org.apache.spark.sql.SparkSession;
import org.insightedge.internal.utils.ClassLoaderUtils;
import org.jini.rio.boot.ServiceClassLoader;
import scala.Option;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Alon Shoham
 * @since 14.2
 */
public class SparkSessionProvider implements Externalizable {

    private static final long serialVersionUID = 1L;
    private static final boolean CLOSABLE_ENABLED = !Boolean.getBoolean("com.gs.spark.session.auto-close-disabled");

    private String master;
    private Map<String, Object> configOptions = new HashMap<>();
    private boolean enableHiveSupport;
    private String logLevel;

    private transient Wrapper wrapper;

    static  {
        ServiceClassLoader.appendIfContext(ClassLoaderUtils::getSparkClassPath);
    }

    public SparkSessionProvider() {
    }

    protected SparkSessionProvider(Builder builder) {
        this.master = builder.master;
        this.configOptions = builder.configOptions;
        this.enableHiveSupport = builder.enableHiveSupport;
        this.logLevel = builder.logLevel;
    }

    public String getMaster() {
        return master;
    }

    public boolean isEnableHiveSupport() {
        return enableHiveSupport;
    }

    public Map<String, Object> getConfigOptions() {
        return configOptions;
    }

    public synchronized Wrapper getOrCreate(){
        if (wrapper != null) return wrapper.reuse();

        if (master != null && !master.isEmpty()) {
            SparkSession.Builder builder = SparkSession.builder();
            builder.master(master);
            if (enableHiveSupport) {
                builder.enableHiveSupport();
            }
            configOptions.forEach((key, value) -> config(builder, key, value));

            if (logLevel != null) {
                java.util.logging.Logger.getLogger("org.apache").setLevel(java.util.logging.Level.parse(logLevel));
            }

            SparkSession activeSession = getIfExists(SparkSession.getActiveSession());
            SparkSession defaultSession = getIfExists(SparkSession.getDefaultSession());
            SparkSession sparkSession = builder.getOrCreate();
            boolean closable = sparkSession != activeSession && sparkSession != defaultSession;
            wrapper = new Wrapper(sparkSession, closable && CLOSABLE_ENABLED);
            return wrapper;
        }

        Option<SparkSession> activeSession = SparkSession.getActiveSession();
        if (activeSession.isDefined()) {
            wrapper = new Wrapper(activeSession.get(), false);
            return wrapper;
        }

        throw new IllegalStateException("Spark session is not configured and no active session was found");
    }

    private static <T> T getIfExists(Option<T> o) {
        return o.isDefined() ? o.get() : null;
    }

    private void config(SparkSession.Builder builder, String key, Object value) {
        if (value instanceof String)
            builder.config(key, (String)value);
        else if (value instanceof Boolean)
            builder.config(key, (Boolean)value);
        else if (value instanceof Integer)
            builder.config(key, ((Integer) value).longValue());
        else if (value instanceof Long)
            builder.config(key, (Long)value);
        else if (value instanceof Double)
            builder.config(key, (Double)value);
        else
            throw new IllegalArgumentException(String.format("config key %s has unsupported type: %s", key, value.getClass()));
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeString(out, master);
        IOUtils.writeObject(out, configOptions);
        out.writeBoolean(enableHiveSupport);
        IOUtils.writeString(out, logLevel);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.master = IOUtils.readString(in);
        this.configOptions = IOUtils.readObject(in);
        this.enableHiveSupport = in.readBoolean();
        this.logLevel = IOUtils.readString(in);
    }

    public boolean hasSparkTaskContext() {
        return TaskContext.get() != null;
    }

    public static class Builder {
        private String master;
        private Map<String,Object> configOptions = new HashMap<>();
        private boolean enableHiveSupport;
        private String logLevel;

        public SparkSessionProvider create() {
            return new SparkSessionProvider(this);
        }

        public Builder master(String master) {
            this.master = master;
            return this;
        }

        public Builder configOptions(Map<String,Object> configOptions) {
            this.configOptions = configOptions;
            return this;
        }

        public Builder config(String key, String value) {
            configOptions.put(key, value);
            return this;
        }


        public Builder enableHiveSupport(boolean enableHiveSupport) {
            this.enableHiveSupport = enableHiveSupport;
            return this;
        }

        public Builder logLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }
    }

    public static class Wrapper implements Closeable {
        private final SparkSession sparkSession;
        private final boolean closable;
        private final AtomicInteger referenceCounter;

        private Wrapper(SparkSession sparkSession, boolean closable) {
            this.sparkSession = sparkSession;
            this.closable = closable;
            this.referenceCounter = new AtomicInteger(1);
        }

        public SparkSession get() {
            return sparkSession;
        }

        private Wrapper reuse() {
            referenceCounter.incrementAndGet();
            return this;
        }

        @Override
        public void close() throws IOException {
            if (referenceCounter.decrementAndGet() == 0) {
                if (closable)
                    sparkSession.close();
            }
        }
    }
}

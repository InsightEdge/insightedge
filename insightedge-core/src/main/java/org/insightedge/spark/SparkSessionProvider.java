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
import org.apache.spark.sql.SparkSession;
import org.insightedge.internal.utils.ClassLoaderUtils;
import org.jini.rio.boot.ServiceClassLoader;
import scala.Option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alon Shoham
 * @since 14.2
 */
public class SparkSessionProvider implements Externalizable {

    private static final long serialVersionUID = 1L;

    private String master;
    private Map<String, Object> configOptions = new HashMap<>();
    private boolean enableHiveSupport;

    private transient SparkSession sparkSession;

    static  {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof ServiceClassLoader) {
            ClassLoaderUtils.addSparkJars((ServiceClassLoader) classLoader);
        }
    }

    public SparkSessionProvider() {
    }

    protected SparkSessionProvider(Builder builder) {
        this.master = builder.master;
        this.configOptions = builder.configOptions;
        this.enableHiveSupport = builder.enableHiveSupport;
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

    public synchronized SparkSession getOrCreate(){
        if (sparkSession != null) return sparkSession;

        if (master != null && !master.isEmpty()) {
            SparkSession.Builder builder = SparkSession.builder();
            builder.master(master);
            if (enableHiveSupport) {
                builder.enableHiveSupport();
            }
            configOptions.forEach((key, value) -> config(builder, key, value));

            sparkSession = builder.getOrCreate();
            return sparkSession;
        }

        Option<SparkSession> activeSession = SparkSession.getActiveSession();
        if (activeSession.isDefined()) {
            sparkSession = activeSession.get();
            return sparkSession;
        }

        throw new IllegalStateException("Spark session is not configured and no active session was found");
    }

    private void config(SparkSession.Builder builder, String key, Object value) {
        if (value instanceof String)
            builder.config(key, (String)value);
        else if (value instanceof Boolean)
            builder.config(key, (Boolean)value);
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
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.master = IOUtils.readString(in);
        this.configOptions = IOUtils.readObject(in);
        this.enableHiveSupport = in.readBoolean();
    }

    public static class Builder {
        private String master;
        private Map<String,Object> configOptions = new HashMap<>();
        private boolean enableHiveSupport;

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
    }
}

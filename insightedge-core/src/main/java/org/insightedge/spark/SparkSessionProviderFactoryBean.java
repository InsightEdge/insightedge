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

import org.springframework.beans.factory.FactoryBean;

import java.util.Map;

/***
 * @author Niv Ingberg
 * @since 14.2
 */
public class SparkSessionProviderFactoryBean implements FactoryBean<SparkSessionProvider> {

    private final SparkSessionProvider.Builder builder = new SparkSessionProvider.Builder();
    private SparkSessionProvider instance;

    @Override
    public SparkSessionProvider getObject() throws Exception {
        if (instance == null)
            instance = builder.create();
        return instance;
    }

    @Override
    public Class<?> getObjectType() {
        return SparkSessionProvider.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setMaster(String master) {
        builder.master(master);
    }

    public void setConfigOptions(Map<String,Object> configOptions) {
        builder.configOptions(configOptions);
    }

    public void setEnableHiveSupport(boolean enableHiveSupport) {
        builder.enableHiveSupport(enableHiveSupport);
    }
}

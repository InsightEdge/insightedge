/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
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

package org.insightedge;

import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.pu.container.ProcessingUnitContainer;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainerProvider;
import org.openspaces.pu.container.spi.ApplicationContextProcessingUnitContainer;
import org.openspaces.pu.container.support.CompoundProcessingUnitContainer;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Leonid_Poliakov
 */
public class TestCluster {
    private static final Logger LOG = LoggerFactory.getLogger(TestCluster.class);

    private String configPath;
    private String schema;
    private Integer numberOfInstances;
    private Integer numberOfBackups;

    public TestCluster() {
    }

    public TestCluster(String configPath, String schema, Integer numberOfInstances, Integer numberOfBackups) {
        this.configPath = configPath;
        this.schema = schema;
        this.numberOfInstances = numberOfInstances;
        this.numberOfBackups = numberOfBackups;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setNumberOfInstances(Integer numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public void setNumberOfBackups(Integer numberOfBackups) {
        this.numberOfBackups = numberOfBackups;
    }

    public void init() {
        Assert.notNull(schema, "'schema' property must be set");
        Assert.notNull(numberOfInstances, "'numberOfInstances' property must be set");
        Assert.notNull(numberOfBackups, "'numberOfBackups' property must be set");
        Assert.hasText(configPath, "'configPath' property must be set");

        LOG.info("Starting the cluster: config = {}", configPath);
        long time = System.currentTimeMillis();

        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setSchema(schema);
        clusterInfo.setNumberOfInstances(numberOfInstances);
        clusterInfo.setNumberOfBackups(numberOfBackups);

        IntegratedProcessingUnitContainerProvider provider = new IntegratedProcessingUnitContainerProvider();
        try {
            provider.addConfigLocation(configPath);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read cluster member config from path: " + configPath, exception);
        }
        provider.setClusterInfo(clusterInfo);
        ProcessingUnitContainer container = provider.createContainer();

        checkContext(container);

        time = System.currentTimeMillis() - time;
        LOG.info("Cluster initialization finished in {} seconds", oneDigit(time / 1000.0));
    }

    private void checkContext(ProcessingUnitContainer container) {
        if (container instanceof CompoundProcessingUnitContainer) {
            for (ProcessingUnitContainer actualContainer : ((CompoundProcessingUnitContainer) container).getProcessingUnitContainers()) {
                checkContext(actualContainer);
            }
        } else if (container instanceof ApplicationContextProcessingUnitContainer) {
            ResourceApplicationContext context = (ResourceApplicationContext) ((ApplicationContextProcessingUnitContainer) container).getApplicationContext();
            LOG.info("Cluster member context state: active = {}, running = {}", context.isActive(), context.isRunning());
        }
    }

    private static String oneDigit(double value) {
        return new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

}
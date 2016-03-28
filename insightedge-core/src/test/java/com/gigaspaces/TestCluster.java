package com.gigaspaces;

import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.pu.container.ProcessingUnitContainer;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainerProvider;
import org.openspaces.pu.container.spi.ApplicationContextProcessingUnitContainer;
import org.openspaces.pu.container.support.CompoundProcessingUnitContainer;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.openspaces.pu.sla.SLA;
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
    private SLA sla;

    public TestCluster() {
    }

    public TestCluster(String configPath, SLA sla) {
        this.sla = sla;
        this.configPath = configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void setSla(SLA sla) {
        this.sla = sla;
    }

    public void init() {
        Assert.notNull(sla, "'sla' property must be set");
        Assert.hasText(configPath, "'configPath' property must be set");

        LOG.info("Starting the cluster: config = {}, sla = {}", configPath, sla);
        long time = System.currentTimeMillis();

        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setSchema(sla.getClusterSchema());
        clusterInfo.setNumberOfInstances(sla.getNumberOfInstances());
        clusterInfo.setNumberOfBackups(sla.getNumberOfBackups());

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

    public static String oneDigit(double value) {
        return new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

}
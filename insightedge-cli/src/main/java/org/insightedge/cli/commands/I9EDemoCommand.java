package org.insightedge.cli.commands;

import com.gigaspaces.start.SystemInfo;
import org.gigaspaces.cli.CliCommand;
import org.gigaspaces.cli.commands.AbstractRunCommand;
import org.gigaspaces.cli.commands.SpaceRunCommand;
import org.gigaspaces.cli.commands.utils.XapCliUtils;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Command(name = "demo", headerHeading = I9EMainCommand.HEADER, header = "Runs manager, webui, spark master, spark worker and zeppelin. It also deploys a 2,1 partitioned space")
public class I9EDemoCommand extends CliCommand {

    @Override
    protected void execute() throws Exception {
        String host=System.getenv("SPARK_LOCAL_IP");
        if(host == null){
            host = SystemInfo.singleton().network().getHostId();
        }

        String port = System.getenv("SPARK_MASTER_PORT");
        if (port == null) {
            port="7077";
        }

        String sparkMasterUrl = "spark://"+host+":"+port;
        List<ProcessBuilder> processBuilders = new ArrayList<ProcessBuilder>();
        processBuilders.addAll(spaceProcessBuilder());
        processBuilders.add(sparkMasterBuilder(host));
        processBuilders.add(sparkWorkerBuilder(sparkMasterUrl, host));
        processBuilders.add(zeppelinBuilder());
        XapCliUtils.executeProcesses(processBuilders);
    }


    private ProcessBuilder sparkMasterBuilder(String sparkMasterHost) {
        String s = File.separator;
        String scriptHome = SystemInfo.singleton().locations().bin();
        String xapHomeFWSlash = scriptHome + s + "..";
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        String script = scriptHome + s + ".." + s + "insightedge" + s + "spark" + s + "bin" + s
                + (isWindows ? "spark-class2.cmd" : "spark-class");
        String[] args = new String[]{
                script,
                "org.apache.spark.deploy.master.Master",
                "--host",
                sparkMasterHost
        };


        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().put("SPARK_MASTER_OPTS",
                "-Dxap.home=" + xapHomeFWSlash +
                        " -Dspark.role=spark-master" +
                        " -Dlog4j.configuration=file:"+xapHomeFWSlash+"/insightedge/conf/spark_log4j.properties");

        processBuilder.inheritIO();
        return processBuilder;
    }

    private ProcessBuilder sparkWorkerBuilder(String sparkMasterUrl, String sparkWorkerHost) {
        String s = File.separator;
        String scriptHome = SystemInfo.singleton().locations().bin();
        String xapHomeFWSlash = scriptHome + s + "..";
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        String script = scriptHome + s + ".." + s + "insightedge" + s + "spark" + s + "bin" + s
                + (isWindows ? "spark-class2.cmd" : "spark-class");
        String[] args = new String[]{
                script,
                "org.apache.spark.deploy.worker.Worker",
                sparkMasterUrl,
                "--host",
                sparkWorkerHost
        };


        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().put("SPARK_WORKER_OPTS",
                "-Dxap.home="+xapHomeFWSlash+
                        " -Dspark.role=spark-worker " +
                        " -Dlog4j.configuration=file:"+xapHomeFWSlash+"/insightedge/conf/spark_log4j.properties");

        processBuilder.inheritIO();

        return processBuilder;
    }

    private ProcessBuilder zeppelinBuilder() {
        String s = File.separator;
        String scriptHome = SystemInfo.singleton().locations().bin();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        String script = scriptHome + s + ".." + s + "insightedge" + s + "zeppelin" + s + "bin" + s
                + (isWindows ? "zeppelin.cmd" : "zeppelin.sh");


        ProcessBuilder processBuilder = new ProcessBuilder(Collections.singletonList(script));

        processBuilder.inheritIO();

        return processBuilder;
    }


    private List<ProcessBuilder> spaceProcessBuilder() {
        String spaceName = "insightedge-space";
        boolean ha = true;
        int partitionsCount = 2;

        List<ProcessBuilder> processBuilders = new ArrayList<ProcessBuilder>();
        processBuilders.add(AbstractRunCommand.buildStartLookupServiceCommand());
        processBuilders.add(SpaceRunCommand.buildPartitionedSpaceCommand(1, spaceName, ha, partitionsCount));
        processBuilders.add(SpaceRunCommand.buildPartitionedBackupSpaceCommand(1, spaceName, ha, partitionsCount));
        processBuilders.add(SpaceRunCommand.buildPartitionedSpaceCommand(2, spaceName, ha, partitionsCount));
        processBuilders.add(SpaceRunCommand.buildPartitionedBackupSpaceCommand(2, spaceName, ha, partitionsCount));
        return processBuilders;
    }


}

package org.insightedge.cli.commands;

import com.gigaspaces.internal.jvm.JavaUtils;
import com.gigaspaces.start.SystemInfo;
import com.gigaspaces.start.SystemLocations;
import org.gigaspaces.cli.CliCommand;
import org.gigaspaces.cli.commands.SpaceRunCommand;
import org.gigaspaces.cli.commands.utils.ProcessBuilderWrapper;
import org.gigaspaces.cli.commands.utils.XapCliUtils;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Command(name = "demo", header = "Run Spark in standalone mode (Master, Worker and Zeppelin) and run a Space in high availability mode (2 primaries with backup each).")
public class I9EDemoCommand extends CliCommand {

    @Override
    protected void execute() throws Exception {
        String host = System.getenv("SPARK_LOCAL_IP");
        if (host == null) {
            host = SystemInfo.singleton().network().getHostId();
        }

        String port = System.getenv("SPARK_MASTER_PORT");
        if (port == null) {
            port = "7077";
        }

        String sparkMasterUrl = "spark://" + host + ":" + port;
        List<ProcessBuilderWrapper> processBuilders = new ArrayList<ProcessBuilderWrapper>();
        //wrap space Builder
        processBuilders.addAll(wrapList(spaceProcessBuilder()));
        processBuilders.add(sparkMasterBuilder(host));
        processBuilders.add(sparkWorkerBuilder(sparkMasterUrl, host));
        processBuilders.add(zeppelinBuilder());
        XapCliUtils.executeProcessesWrapper(processBuilders);

    }

    private List<ProcessBuilderWrapper> wrapList(List<ProcessBuilder> lst) {
        List<ProcessBuilderWrapper> wrappedList = new ArrayList<ProcessBuilderWrapper>();
        for (ProcessBuilder cur : lst) {
            wrappedList.add(new ProcessBuilderWrapper(cur));
        }
        return wrappedList;
    }


    private ProcessBuilderWrapper sparkMasterBuilder(String sparkMasterHost) {
        String xapHomeFWSlash = SystemLocations.singleton().homeFwdSlash();
        String[] args = new String[]{
                getSparkClassScript(),
                "org.apache.spark.deploy.master.Master",
                "--host",
                sparkMasterHost
        };


        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().put("SPARK_MASTER_OPTS",
                "-Dxap.home=" + xapHomeFWSlash +
                        " -Dspark.role=spark-master" +
                        " -Dlog4j.configuration=file:" + xapHomeFWSlash + "/insightedge/conf/spark_log4j.properties");

        processBuilder.inheritIO();

        return new ProcessBuilderWrapper(processBuilder);
    }

    private ProcessBuilderWrapper sparkWorkerBuilder(String sparkMasterUrl, String sparkWorkerHost) {
        String xapHomeFWSlash = SystemLocations.singleton().homeFwdSlash();
        String[] args = new String[]{
                getSparkClassScript(),
                "org.apache.spark.deploy.worker.Worker",
                sparkMasterUrl,
                "--host",
                sparkWorkerHost
        };


        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().put("SPARK_WORKER_OPTS",
                "-Dxap.home=" + xapHomeFWSlash +
                        " -Dspark.role=spark-worker " +
                        " -Dlog4j.configuration=file:" + xapHomeFWSlash + "/insightedge/conf/spark_log4j.properties");

        processBuilder.inheritIO();
        return new SparkWorkerProcessBuilderWrapper(processBuilder);
    }

    private String getSparkClassScript() {
        String scriptName = JavaUtils.isWindows() ? "spark-class2.cmd" : "spark-class";
        return SystemLocations.singleton().sparkHome().resolve("bin").resolve(scriptName).toString();
    }

    private class SparkWorkerProcessBuilderWrapper extends ProcessBuilderWrapper {
        public SparkWorkerProcessBuilderWrapper(ProcessBuilder processBuilder) {
            super(processBuilder);
        }

        @Override
        public boolean allowToStart() {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                return false;
            }
            return true;

        }
    }

    private ProcessBuilderWrapper zeppelinBuilder() {
        String scriptName = JavaUtils.isWindows() ? "zeppelin.cmd" : "zeppelin.sh";
        String script = SystemLocations.singleton().home("insightedge", "zeppelin", "bin", scriptName).toString();
        ProcessBuilder processBuilder = new ProcessBuilder(Collections.singletonList(script));
        processBuilder.inheritIO();
        return new ProcessBuilderWrapper(processBuilder);
    }


    private List<ProcessBuilder> spaceProcessBuilder() {
		SpaceRunCommand command  = new SpaceRunCommand();
		command.name = "demo";
        command.partitions = 2;
        command.ha = true;
		command.lus = true;
        return command.toProcessBuilders();
    }
}

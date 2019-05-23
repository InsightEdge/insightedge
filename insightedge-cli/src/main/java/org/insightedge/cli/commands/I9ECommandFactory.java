package org.insightedge.cli.commands;

import com.gigaspaces.start.GsCommandFactory;
import com.gigaspaces.start.JavaCommandBuilder;
import com.gigaspaces.start.SystemInfo;

public class I9ECommandFactory extends GsCommandFactory {
    public static void main(String[] args) {
        execute(args, new I9ECommandFactory());
    }

    @Override
    protected JavaCommandBuilder generate(String id) {
        switch (id.toLowerCase()) {
            case "cli-insightedge": return cliInsightEdge();
            default: return super.generate(id);
        }
    }

    private JavaCommandBuilder cliInsightEdge() {
        command.mainClass("org.insightedge.cli.commands.I9EMainCommand");
        command.classpathFromPath(SystemInfo.singleton().getXapHome(), "tools", "cli", "*");
        command.classpathFromPath(SystemInfo.singleton().locations().getLibPlatform(), "blueprints", "*");
        command.classpathFromEnv("INSIGHTEDGE_CLASSPATH");
        appendSigarClassPath();
        addOshiAndLoggerToClasspath();
        appendXapOptions();
        command.optionsFromEnv("XAP_CLI_OPTIONS");

        return command;
    }
}

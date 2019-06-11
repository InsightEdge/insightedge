package org.insightedge.cli.commands;

import com.gigaspaces.start.GsCommandFactory;
import com.gigaspaces.start.JavaCommandBuilder;

public class I9ECommandFactory extends GsCommandFactory {
    public static void main(String[] args) {
        execute(args, new I9ECommandFactory());
    }

    protected JavaCommandBuilder cli() {
        super.cli();
        command.mainClass("org.insightedge.cli.commands.I9EMainCommand");
        // Class path:
        command.classpathFromEnv("INSIGHTEDGE_CLASSPATH");
        appendMetricToolsClassPath();

        return command;
    }
}

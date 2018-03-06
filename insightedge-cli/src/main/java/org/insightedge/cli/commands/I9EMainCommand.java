package org.insightedge.cli.commands;

import org.gigaspaces.cli.CliExecutor;
import org.gigaspaces.cli.commands.*;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.Collection;


@Command(name="insightedge", headerHeading = I9EMainCommand.HEADER, customSynopsis = "insightedge [global-options] command [options] [parameters]")
public class I9EMainCommand extends XapMainCommand {
    public static final String HEADER =
        "@|green   _____           _       _     _   ______    _            |@%n"+
        "@|green  |_   _|         (_)     | |   | | |  ____|  | |           |@%n"+
        "@|green    | |  _ __  ___ _  __ _| |__ | |_| |__   __| | __ _  ___ |@%n"+
        "@|green    | | | '_ \\/ __| |/ _` | '_ \\| __|  __| / _` |/ _` |/ _ \\|@%n"+
        "@|green   _| |_| | | \\__ \\ | (_| | | | | |_| |___| (_| | (_| |  __/|@%n"+
        "@|green  |_____|_| |_|___/_|\\__, |_| |_|\\__|______\\__,_|\\__, |\\___||@%n"+
        "@|green                      __/ |                       __/ |     |@%n"+
        "@|green                     |___/                       |___/   |@%n" +
                    "%n";

    public static void main(String[] args) {
        CliExecutor.execute(new I9EMainCommand(), args);
    }

    @Override
    public Collection<Object> getSubCommands() {
        ArrayList<Object> coll = new ArrayList<Object>(super.getSubCommands());
        coll.add(new I9EDemoCommand());
        return coll;
    }
}

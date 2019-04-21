package org.insightedge.cli.commands;

import org.gigaspaces.cli.CliExecutor;

import java.io.IOException;

public class Autocomplete {

    public static void main(final String[] args) throws IOException {
        CliExecutor.generateAutoComplete(new I9EMainCommand(), args);
    }
}
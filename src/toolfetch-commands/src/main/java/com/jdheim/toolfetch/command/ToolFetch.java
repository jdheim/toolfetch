/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command;

import com.jdheim.toolfetch.command.execution.CompositeStrategy;
import com.jdheim.toolfetch.command.execution.DebugStrategy;
import com.jdheim.toolfetch.command.execution.ExecutionExceptionHandler;
import com.jdheim.toolfetch.command.execution.ValidateStrategy;
import com.jdheim.toolfetch.command.info.ToolFetchVersionInfoProvider;
import com.jdheim.toolfetch.command.subcommand.Install;
import picocli.CommandLine;

@CommandLine.Command(name = "toolfetch", versionProvider = ToolFetchVersionInfoProvider.class,
        description = "CLI for fetching and installing external tools from release URLs (e.g. GitHub releases) using a YAML configuration file",
        subcommands = Install.class)
public final class ToolFetch {

    /// Populated reflectively by PicoCLI when it handles a help request
    @SuppressWarnings("UnusedVariable")
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    private boolean helpRequested;

    /// Populated reflectively by PicoCLI when it handles a version request
    @SuppressWarnings("UnusedVariable")
    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "Show version information and exit")
    private boolean versionRequested;

    public static int execute(String[] args) {
        return ToolFetch.commandLine().execute(args);
    }

    public static CommandLine commandLine() {
        CompositeStrategy compositeStrategy = new CompositeStrategy(new ValidateStrategy(),
                parseResult -> new DebugStrategy().execute(parseResult),
                new CommandLine.RunLast());
        return new CommandLine(new ToolFetch()).setExecutionStrategy(compositeStrategy)
                .setExecutionExceptionHandler(new ExecutionExceptionHandler());
    }

}

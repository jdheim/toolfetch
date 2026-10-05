/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command;

import com.jdheim.toolfetch.command.execution.CompositeStrategy;
import com.jdheim.toolfetch.command.execution.DebugStrategy;
import com.jdheim.toolfetch.command.execution.ValidateStrategy;
import com.jdheim.toolfetch.command.info.ToolFetchVersionInfoProvider;
import com.jdheim.toolfetch.command.subcommand.Install;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.service.log.LogHelper;
import org.apache.commons.lang3.ArrayUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "toolfetch", versionProvider = ToolFetchVersionInfoProvider.class,
        description = "CLI for fetching and installing external tools from release URLs (e.g. GitHub releases) using a YAML configuration file",
        subcommands = Install.class)
public final class ToolFetch {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(ToolFetch.class);

    private static final Object[] HELP_VERSION_OPTIONS = {"-h", "--help", "-v", "--version"};

    private static final String TOOLFETCH_SHUTDOWN_HOOK = "toolfetch-shutdown-hook";

    /// Populated reflectively by PicoCLI when it handles a help request
    @SuppressWarnings("UnusedVariable")
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    private boolean helpRequested;

    /// Populated reflectively by PicoCLI when it handles a version request
    @SuppressWarnings("UnusedVariable")
    @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "Show version information and exit")
    private boolean versionRequested;

    public static int execute(long startTime, String[] args) {
        boolean containsHelpOrVersionArgs = ArrayUtils.containsAny(args, HELP_VERSION_OPTIONS);
        if (!containsHelpOrVersionArgs) {
            addShutdownHook(startTime);
        }
        int exitCode = ToolFetch.commandLine().execute(args);
        if (!containsHelpOrVersionArgs) {
            LOGGER.log("command.exitcode", exitCode);
        }
        return exitCode;
    }

    public static CommandLine commandLine() {
        CompositeStrategy compositeStrategy = new CompositeStrategy(new DebugStrategy(), new ValidateStrategy(),
                new CommandLine.RunLast());
        return new CommandLine(new ToolFetch()).setExecutionStrategy(compositeStrategy);
    }

    private static void addShutdownHook(long startTime) {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name(TOOLFETCH_SHUTDOWN_HOOK).unstarted(() -> {
            String elapsedTime = LogHelper.elapsedTime(startTime);
            LOGGER.log("command.completed", elapsedTime);
        }));
    }

}

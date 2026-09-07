/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import com.jdheim.toolfetch.command.convert.PathTrimConverter;
import com.jdheim.toolfetch.command.execution.CompositeStrategy;
import com.jdheim.toolfetch.command.execution.DebugStrategy;
import com.jdheim.toolfetch.command.execution.ValidateStrategy;
import com.jdheim.toolfetch.command.info.ToolFetchVersionInfoProvider;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.config.ConfigurationService;
import com.jdheim.toolfetch.service.config.YamlConfigurationService;
import com.jdheim.toolfetch.service.install.ArchiveInstallationService;
import com.jdheim.toolfetch.service.install.InstallationService;
import com.jdheim.toolfetch.service.log.LogHelper;
import org.apache.commons.lang3.ArrayUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "toolfetch", versionProvider = ToolFetchVersionInfoProvider.class, mixinStandardHelpOptions = true,
        description = "CLI for fetching and installing external tools from release URLs (e.g. GitHub releases) using a YAML configuration file")
public final class ToolFetch implements Callable<Integer> {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(ToolFetch.class);

    private static final Object[] HELP_VERSION_OPTIONS = {"-h", "--help", "-V", "--version"};

    private static final String TOOLFETCH_SHUTDOWN_HOOK = "toolfetch-shutdown-hook";

    private final ConfigurationService configurationService;

    private final InstallationService installationService;

    /// [@Patch jspecify#431](https://github.com/jspecify/jspecify/issues/431)
    /// and [@Patch NullAway#313](https://github.com/uber/NullAway/issues/313)
    @SuppressWarnings("NullAway.Init")
    @CommandLine.Option(names = {"-c", "--config"}, required = true, description = "Path to toolfetch.yaml",
            converter = PathTrimConverter.class)
    private Path configPath;

    private ToolFetch() {
        configurationService = new YamlConfigurationService();
        installationService = new ArchiveInstallationService();
    }

    public static int execute(long startTime, String[] args) {
        if (!ArrayUtils.containsAny(args, HELP_VERSION_OPTIONS)) {
            addShutdownHook(startTime);
        }
        int exitCode = ToolFetch.commandLine().execute(args);
        LOGGER.log("command.exitcode", exitCode);
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

    @Override
    public Integer call() {
        return configurationService.parse(getConfigPath())
                .map(this::toInstallationExitCode)
                .orElse(CommandLine.ExitCode.SOFTWARE);
    }

    public Path getConfigPath() {
        return configPath;
    }

    private int toInstallationExitCode(Configuration configuration) {
        installationService.install(configuration);
        return CommandLine.ExitCode.OK;
    }

}

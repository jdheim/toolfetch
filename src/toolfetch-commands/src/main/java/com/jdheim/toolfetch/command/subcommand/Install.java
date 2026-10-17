/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.subcommand;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import com.jdheim.toolfetch.command.convert.PathTrimConverter;
import com.jdheim.toolfetch.command.execution.option.ConfigPathDefaultValueProvider;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.config.ConfigurationService;
import com.jdheim.toolfetch.service.config.YamlConfigurationService;
import com.jdheim.toolfetch.service.install.ArchiveInstallationService;
import com.jdheim.toolfetch.service.install.InstallationService;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@CommandLine.Command(name = "install", description = "Install tools", defaultValueProvider = ConfigPathDefaultValueProvider.class)
public final class Install implements Callable<Integer> {

    /// [@Patch jspecify#431](https://github.com/jspecify/jspecify/issues/431)
    /// and [@Patch NullAway#313](https://github.com/uber/NullAway/issues/313)
    @SuppressWarnings("NullAway.Init")
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    /// Populated reflectively by PicoCLI when it handles a help request
    @SuppressWarnings("UnusedVariable")
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    private boolean helpRequested;

    /// [@Patch jspecify#431](https://github.com/jspecify/jspecify/issues/431)
    /// and [@Patch NullAway#313](https://github.com/uber/NullAway/issues/313)
    @SuppressWarnings("NullAway.Init")
    @CommandLine.Option(names = {"-c", "--config"}, description = {
            "Path to a YAML configuration file", "Autodetected as toolfetch.yaml or toolfetch.yml in the current directory"
    }, converter = PathTrimConverter.class)
    private Path configPath;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
    private @Nullable ToolSelection selection;

    public Path configPath() {
        return configPath;
    }

    @Override
    public Integer call() {
        ConfigurationService configurationService = new YamlConfigurationService();
        return configurationService.parse(configPath()).map(configuration -> toInstallationExitCode(selectTools(configuration)))
                .orElse(CommandLine.ExitCode.SOFTWARE);
    }

    private Configuration selectTools(Configuration configuration) {
        if (selection == null) {
            return configuration;
        }
        boolean only = !selection.only.isEmpty();
        Set<String> ids = only ? selection.only : selection.skip;
        Set<String> configuredIds = configuration.tools().stream().map(Tool::id).collect(Collectors.toSet());
        ids.forEach(id -> {
            if (!configuredIds.contains(id)) {
                throw new CommandLine.ParameterException(commandSpec.commandLine(),
                        ToolFetchLogger.message("exception.install.unknown-tool-id", id));
            }
        });
        List<Tool> tools = configuration.tools().stream().filter(tool -> ids.contains(tool.id()) == only).toList();
        if (tools.isEmpty()) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                    ToolFetchLogger.message("exception.install.no-tools-selected"));
        }
        return new Configuration(configuration.destination(), tools, configuration.http());
    }

    private int toInstallationExitCode(Configuration configuration) {
        InstallationService installationService = new ArchiveInstallationService();
        installationService.install(configuration);
        return CommandLine.ExitCode.OK;
    }

    private static final class ToolSelection {

        @CommandLine.Option(names = "--only", paramLabel = "<id>", description = "Install only this tool ID (repeatable)")
        private Set<String> only = new LinkedHashSet<>();

        @CommandLine.Option(names = "--skip", paramLabel = "<id>",
                description = "Skip this tool ID and install all others (repeatable)")
        private Set<String> skip = new LinkedHashSet<>();

    }

}

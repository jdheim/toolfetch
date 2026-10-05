/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.subcommand;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import com.jdheim.toolfetch.command.convert.PathTrimConverter;
import com.jdheim.toolfetch.command.execution.option.ConfigPathDefaultValueProvider;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.config.ConfigurationService;
import com.jdheim.toolfetch.service.config.YamlConfigurationService;
import com.jdheim.toolfetch.service.install.ArchiveInstallationService;
import com.jdheim.toolfetch.service.install.InstallationService;
import picocli.CommandLine;

@CommandLine.Command(name = "install", description = "Install tools", defaultValueProvider = ConfigPathDefaultValueProvider.class)
public final class Install implements Callable<Integer> {

    private final ConfigurationService configurationService;

    private final InstallationService installationService;

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

    private Install() {
        configurationService = new YamlConfigurationService();
        installationService = new ArchiveInstallationService();
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

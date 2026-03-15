/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import com.jdheim.toolfetch.command.picocli.convert.PathTrimConverter;
import com.jdheim.toolfetch.command.version.ToolFetchVersionProvider;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.config.ConfigurationService;
import com.jdheim.toolfetch.service.config.YamlConfigurationService;
import com.jdheim.toolfetch.service.install.ArchiveInstallationService;
import com.jdheim.toolfetch.service.install.InstallationService;
import picocli.CommandLine;

@CommandLine.Command(name = "toolfetch", versionProvider = ToolFetchVersionProvider.class, mixinStandardHelpOptions = true,
        description = "CLI for fetching and installing external tools from release URLs (e.g. GitHub releases) using a YAML configuration file")
public final class ToolFetch implements Callable<Integer> {

    private final ConfigurationService configurationService;

    private final InstallationService installationService;

    /// [@Patch jspecify#431](https://github.com/jspecify/jspecify/issues/431)
    /// and [@Patch NullAway#313](https://github.com/uber/NullAway/issues/313)
    @SuppressWarnings("NullAway.Init")
    @CommandLine.Option(names = {"-c", "--config"}, required = true, description = "Path to toolfetch.yaml",
            converter = PathTrimConverter.class)
    private Path configPath;

    public ToolFetch() {
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

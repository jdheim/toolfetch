/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution.option;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

/// Supplies the configuration file detected in the current working directory
public final class ConfigPathDefaultValueProvider implements CommandLine.IDefaultValueProvider {

    static final String YAML_FILE_NAME = "toolfetch.yaml";

    static final String YML_FILE_NAME = "toolfetch.yml";

    private static final String CONFIG_OPTION_NAME = "--config";

    private static boolean isConfigOption(CommandLine.Model.ArgSpec argSpec) {
        return argSpec instanceof CommandLine.Model.OptionSpec optionSpec && CONFIG_OPTION_NAME.equals(optionSpec.longestName());
    }

    static Path autoDetect(Path workingDirectory) {
        Path yamlPath = workingDirectory.resolve(YAML_FILE_NAME);
        if (Files.exists(yamlPath)) {
            return yamlPath;
        }
        Path ymlPath = workingDirectory.resolve(YML_FILE_NAME);
        return Files.exists(ymlPath) ? ymlPath : yamlPath;
    }

    @Override
    public @Nullable String defaultValue(CommandLine.Model.ArgSpec argSpec) {
        if (isConfigOption(argSpec)) {
            return autoDetect(Path.of(SystemUtils.USER_DIR)).toString();
        }
        return null;
    }

}

/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution.option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

public enum ConfigPathValidationRule {

    IS_PRESENT("Missing required parameter for option '--config' (<configPath>)") {
        @Override
        protected boolean validate(Path configPath) {
            return StringUtils.isNotEmpty(configPath.toString());
        }
    },
    EXISTS("File \"%s\" does not exist") {
        @Override
        protected boolean validate(Path configPath) {
            return Files.exists(configPath);
        }
    },
    IS_REGULAR_FILE("File \"%s\" is not a regular file") {
        @Override
        protected boolean validate(Path configPath) {
            return Files.isRegularFile(configPath);
        }
    },
    IS_READABLE("File \"%s\" is not readable") {
        @Override
        protected boolean validate(Path configPath) {
            return Files.isReadable(configPath);
        }
    },
    HAS_YAML_EXTENSION("File \"%s\" is not a YAML file") {
        @Override
        protected boolean validate(Path configPath) {
            return Optional.of(configPath)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(String::toLowerCase)
                    .filter(fileName -> fileName.endsWith(".yaml") || fileName.endsWith(".yml"))
                    .isPresent();
        }
    },
    HAS_MAX_SIZE("File \"%s\" is larger than 1 MB") {
        @Override
        protected boolean validate(Path configPath) {
            try {
                return Files.size(configPath) <= MAX_SIZE_1MB;
            } catch (IOException _) {
                return false;
            }
        }
    };

    private static final long MAX_SIZE_1MB = 1024L * 1024L;

    private final String logMessage;

    ConfigPathValidationRule(String logMessage) {
        this.logMessage = logMessage;
    }

    public static void validateAll(CommandLine commandLine, Path configPath) {
        for (ConfigPathValidationRule configPathValidationRule : ConfigPathValidationRule.values()) {
            if (!configPathValidationRule.validate(configPath)) {
                throw new CommandLine.ParameterException(commandLine,
                        configPathValidationRule.getLogMessage().formatted(configPath));
            }
        }
    }

    private String getLogMessage() {
        return logMessage;
    }

    protected abstract boolean validate(Path configPath);

}

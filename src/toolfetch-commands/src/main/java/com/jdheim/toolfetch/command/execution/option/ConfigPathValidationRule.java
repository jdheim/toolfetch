/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution.option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

public enum ConfigPathValidationRule {

    IS_PRESENT("exception.config-path.missing") {
        @Override
        protected boolean validate(Path configPath) {
            return StringUtils.isNotEmpty(configPath.toString());
        }
    },
    EXISTS("exception.config-path.not-found") {
        @Override
        protected boolean validate(Path configPath) {
            return Files.exists(configPath);
        }
    },
    IS_REGULAR_FILE("exception.config-path.not-regular-file") {
        @Override
        protected boolean validate(Path configPath) {
            return Files.isRegularFile(configPath);
        }
    },
    IS_READABLE("exception.config-path.not-readable") {
        @Override
        protected boolean validate(Path configPath) {
            return Files.isReadable(configPath);
        }
    },
    HAS_YAML_EXTENSION("exception.config-path.not-yaml") {
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
    HAS_MAX_SIZE("exception.config-path.too-large") {
        @Override
        protected boolean validate(Path configPath) {
            try {
                return Files.size(configPath) <= MAX_SIZE_1MB;
            } catch (IOException e) {
                ToolFetchLogger.getLogger(ConfigPathValidationRule.class).log(e);
                return false;
            }
        }
    };

    private static final long MAX_SIZE_1MB = 1024L * 1024L;

    private final String messageKey;

    ConfigPathValidationRule(String messageKey) {
        this.messageKey = messageKey;
    }

    public static void validateAll(CommandLine commandLine, Path configPath) {
        for (ConfigPathValidationRule configPathValidationRule : ConfigPathValidationRule.values()) {
            if (!configPathValidationRule.validate(configPath)) {
                throw new CommandLine.ParameterException(commandLine,
                        ToolFetchLogger.message(configPathValidationRule.messageKey, configPath));
            }
        }
    }

    protected abstract boolean validate(Path configPath);

}

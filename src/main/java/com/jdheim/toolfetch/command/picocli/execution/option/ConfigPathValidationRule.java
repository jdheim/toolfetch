/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdheim.toolfetch.command.picocli.execution.option;

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

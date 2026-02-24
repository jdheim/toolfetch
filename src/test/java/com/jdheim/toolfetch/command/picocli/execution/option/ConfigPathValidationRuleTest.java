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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Stream;
import com.jdheim.toolfetch.command.ToolFetch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

/// OOC Tests for [ConfigPathValidationRule]
class ConfigPathValidationRuleTest {

    static final int MAX_SIZE_1MB = 1024 * 1024;

    static final int EXCEEDS_MAX_SIZE_1MB = 1024 * 1024 + 1;

    @TempDir
    static Path tempDir;

    CommandLine commandLine;

    static Stream<Arguments> validateConfigPathTestCases() {
        return Stream.of(Arguments.of(tempDir.resolve("toolfetch.yaml"), null),
                Arguments.of(tempDir.resolve("toolfetch.yml"), null), Arguments.of(tempDir.resolve("TOOLFETCH.YAML"), null),
                Arguments.of(tempDir.resolve("TOOLFETCH.YML"), null),
                Arguments.of(Path.of(""), "Missing required parameter for option '--config' (<configPath>)"),
                Arguments.of(Path.of("not-exists.yaml"), "File \"not-exists.yaml\" does not exist"),
                Arguments.of(tempDir, "File \"%s\" is not a regular file".formatted(tempDir)),
                Arguments.of(tempDir.resolve("not-readable.yaml"),
                        "File \"%s\" is not readable".formatted(tempDir.resolve("not-readable.yaml"))),
                Arguments.of(tempDir.resolve("toolfetch.txt"),
                        "File \"%s\" is not a YAML file".formatted(tempDir.resolve("toolfetch.txt"))),
                Arguments.of(tempDir.resolve("max-size-bytes.yaml"), null),
                Arguments.of(tempDir.resolve("exceeds-max-size-bytes.yaml"),
                        "File \"%s\" is larger than 1 MB".formatted(tempDir.resolve("exceeds-max-size-bytes.yaml"))));
    }

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new ToolFetch());
    }

    @ParameterizedTest
    @MethodSource("validateConfigPathTestCases")
    void testValidateConfigPath(Path configPath, String message) throws IOException {
        if (configPath.startsWith(tempDir) && !configPath.equals(tempDir)) {
            Files.createFile(configPath);
            if (configPath.endsWith("not-readable.yaml")) {
                Files.setPosixFilePermissions(configPath, Set.of(PosixFilePermission.OWNER_WRITE));
            } else if (configPath.endsWith("max-size-bytes.yaml")) {
                Files.write(configPath, new byte[MAX_SIZE_1MB]);
            } else if (configPath.endsWith("exceeds-max-size-bytes.yaml")) {
                Files.write(configPath, new byte[EXCEEDS_MAX_SIZE_1MB]);
            }
        }
        Throwable exception = catchThrowable(() -> ConfigPathValidationRule.validateAll(commandLine, configPath));
        if (message == null) {
            assertThat(exception).isNull();
        } else {
            assertThat(exception).isInstanceOf(CommandLine.ParameterException.class).hasMessage(message);
        }
    }

    @Test
    void testHasMaxSize_IOException() {
        boolean isValid = ConfigPathValidationRule.HAS_MAX_SIZE.validate(Path.of("not-exists.yaml"));
        assertThat(isValid).isFalse();
    }

}

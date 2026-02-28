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

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.command.ToolFetch;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [ToolFetch] -c/--config
@WireMockTest
class ToolFetchConfigUnhappySmokeTest extends ToolFetchTestBase {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testUnknownOption(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        ExecResult execResult = execute(option, toolfetchConfigPath.toString(), "-z");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unknown option: '-z'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testUnmatchedArgument(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        ExecResult execResult = execute(option, toolfetchConfigPath.toString(), "arg");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unmatched argument at index 2: 'arg'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testMissingConfigPath(String option) throws Exception {
        ExecResult execResult = execute(option);
        assertMissingRequiredParameterConfigPath(execResult);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testEmptyConfigPath(String option) throws Exception {
        ExecResult execResult = execute(option, StringUtils.EMPTY);
        assertMissingRequiredParameterConfigPath(execResult);
    }

    private void assertMissingRequiredParameterConfigPath(ExecResult execResult) {
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Missing required parameter for option '--config' (<configPath>)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testParsingError(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.createFile(toolfetchConfigPath);
        }
        ExecResult execResult = execute(option, toolfetchConfigPath.toString());
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertAnyMatch(execResult,
                "[%s] Error occurred when parsing YAML configuration: should not be empty".formatted(Level.ERROR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testJsonSchemaError(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.writeString(toolfetchConfigPath, "destination: /tmp");
        }
        ExecResult execResult = execute(option, toolfetchConfigPath.toString());
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertAnyMatch(execResult, "[%s] Config does not conform to schema:".formatted(Level.ERROR));
        assertAnyMatch(execResult, "[%s] - required property 'tools' not found".formatted(Level.ERROR));
    }

}

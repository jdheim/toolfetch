/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.command.subcommand.Install;
import com.jdheim.toolfetch.logging.LogLevel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// E2E Tests for [Install]
@WireMockTest
class ToolFetchInstallUnhappyE2E extends ToolFetchTestBase {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {"-v", "--version"})
    void testVersionUnknownOption(String option) throws Exception {
        ExecResult execResult = execute("install", option);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unknown option: '%s'".formatted(option));
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testConfigUnknownOption(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        ExecResult execResult = execute("install", option, toolfetchConfigPath.toString(), "-z");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unknown option: '-z'");
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testConfigUnmatchedArgument(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        ExecResult execResult = execute("install", option, toolfetchConfigPath.toString(), "arg");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unmatched argument at index 3: 'arg'");
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testConfigMissingConfigPath(String option) throws Exception {
        ExecResult execResult = execute("install", option);
        assertMissingRequiredParameterConfigPath(execResult);
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testConfigEmptyConfigPath(String option) throws Exception {
        ExecResult execResult = execute("install", option, StringUtils.EMPTY);
        assertMissingRequiredParameterConfigPath(execResult);
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    private void assertMissingRequiredParameterConfigPath(ExecResult execResult) {
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Missing required parameter for option '--config' (<configPath>)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testConfigParsingError(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.createFile(toolfetchConfigPath);
        }
        ExecResult execResult = execute("install", option, toolfetchConfigPath.toString());
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertAnyMatch(execResult,
                "[%s] YAML configuration is empty. Add configuration settings".formatted(LogLevel.ERROR));
        if (runtimeModeNativeImage()) {
            assertOneMatch(execResult, "Command exit code: " + CommandLine.ExitCode.SOFTWARE);
            assertOneMatch(execResult, "[INFO] Command completed in ");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testConfigJsonSchemaError(String option) throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.writeString(toolfetchConfigPath, "destination: /tmp");
        }
        ExecResult execResult = execute("install", option, toolfetchConfigPath.toString());
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertAnyMatch(execResult, "[%s] Configuration does not match the schema:".formatted(LogLevel.ERROR));
        assertAnyMatch(execResult, "[%s] - required property 'tools' not found".formatted(LogLevel.ERROR));
        if (runtimeModeNativeImage()) {
            assertOneMatch(execResult, "Command exit code: " + CommandLine.ExitCode.SOFTWARE);
            assertOneMatch(execResult, "[INFO] Command completed in ");
        }
    }

    @Test
    void testAutoDetectedConfigNotFound() throws Exception {
        ExecResult execResult = execute("install");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        Path configPath = Path.of(SystemUtils.USER_DIR, "toolfetch.yaml");
        assertAnyMatch(execResult, "File \"%s\" does not exist".formatted(configPath));
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

}

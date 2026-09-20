/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution.option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jdheim.toolfetch.command.ToolFetch;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// OOC Tests for [ConfigPathDefaultValueProvider]
class ConfigPathDefaultValueProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void testAutoDetectYaml() throws IOException {
        Path yamlConfigPath = tempDir.resolve(ConfigPathDefaultValueProvider.YAML_FILE_NAME);
        Files.createFile(yamlConfigPath);
        Path ymlConfigPath = tempDir.resolve(ConfigPathDefaultValueProvider.YML_FILE_NAME);
        Files.createFile(ymlConfigPath);

        Path configPath = ConfigPathDefaultValueProvider.autoDetect(tempDir);

        assertThat(configPath).isEqualTo(yamlConfigPath);
    }

    @Test
    void testAutoDetectYml() throws IOException {
        Path ymlConfigPath = tempDir.resolve(ConfigPathDefaultValueProvider.YML_FILE_NAME);
        Files.createFile(ymlConfigPath);

        Path configPath = ConfigPathDefaultValueProvider.autoDetect(tempDir);

        assertThat(configPath).isEqualTo(ymlConfigPath);
    }

    @Test
    void testAutoDetectWithoutConfig() {
        Path configPath = ConfigPathDefaultValueProvider.autoDetect(tempDir);

        assertThat(configPath).isEqualTo(tempDir.resolve(ConfigPathDefaultValueProvider.YAML_FILE_NAME));
    }

    @Test
    void testAutoDetectWithMissingConfigOption() {
        CommandLine commandLine = ToolFetch.commandLine();

        commandLine.parseArgs();

        ToolFetch toolFetch = commandLine.getCommand();
        Path configPath = Path.of(SystemUtils.USER_DIR, ConfigPathDefaultValueProvider.YAML_FILE_NAME);
        assertThat(toolFetch.getConfigPath()).isEqualTo(configPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testNoAutoDetectWithConfigOption(String configOption) {
        CommandLine commandLine = ToolFetch.commandLine();
        Path configPath = tempDir.resolve("custom.yaml");

        commandLine.parseArgs(configOption, configPath.toString());

        ToolFetch toolFetch = commandLine.getCommand();
        assertThat(toolFetch.getConfigPath()).isEqualTo(configPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-h", "--help"})
    void testNoAutoDetectWithHelpOption(String helpOption) {
        CommandLine commandLine = ToolFetch.commandLine();

        int exitCode = commandLine.execute(helpOption);

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-v", "--version"})
    void testNoAutoDetectWithVersionOption(String versionOption) {
        CommandLine commandLine = ToolFetch.commandLine();

        int exitCode = commandLine.execute(versionOption);

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
    }

    @Test
    void testDefaultValueForOtherOption() {
        CommandLine commandLine = ToolFetch.commandLine();
        ConfigPathDefaultValueProvider defaultValueProvider = new ConfigPathDefaultValueProvider();

        String defaultValue = defaultValueProvider.defaultValue(commandLine.getCommandSpec().findOption("--help"));

        assertThat(defaultValue).isNull();
    }

    @Test
    void testDefaultValueForPositionalParameter() {
        ConfigPathDefaultValueProvider defaultValueProvider = new ConfigPathDefaultValueProvider();
        CommandLine.Model.ArgSpec argSpec = mock();

        String defaultValue = defaultValueProvider.defaultValue(argSpec);

        assertThat(defaultValue).isNull();
    }

}

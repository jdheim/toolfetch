/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.subcommand;

import static com.jdheim.toolfetch.builder.TestConfigurationBuilder.configuration;
import static com.jdheim.toolfetch.builder.TestHttpBuilder.http;
import static com.jdheim.toolfetch.builder.TestToolBuilder.tool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.install.ArchiveInstallationService;
import com.jdheim.toolfetch.step.config.ConfigurationSteps;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

/// OOC Tests for [Install]
class InstallTest {

    @TempDir
    Path tempDir;

    Path configPath;

    CommandLine commandLine;

    StringWriter output;

    StringWriter error;

    TestLogListAppenderSteps testLogListAppenderSteps;

    @BeforeEach
    void setUp() throws IOException {
        commandLine = ToolFetch.commandLine();
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
        output = new StringWriter();
        error = new StringWriter();
        commandLine.setOut(new PrintWriter(output, true)).setErr(new PrintWriter(error, true));
        configPath = tempDir.resolve("toolfetch.yaml");
        Configuration configuration = configuration().addTools(
                        tool().id("toolfetch").url("https://example.com/toolfetch.zip").build(),
                        tool().id("firefox").url("https://example.com/firefox.zip").build(),
                        tool().id("dbeaver").url("https://example.com/dbeaver.zip").build())
                .http(http().trustStorePath("custom/cacerts").withDefaultTrustStoreType().build())
                .build();
        ConfigurationSteps.saveToConfigFile(configuration, configPath);
    }

    @Test
    void testInstallAllByDefault() {
        assertThat(installedConfiguration().tools()).extracting(Tool::id).containsExactly("toolfetch", "firefox", "dbeaver");
    }

    @Test
    void testOnlySelectedTool() {
        assertThat(installedConfiguration("--only", "firefox").tools()).extracting(Tool::id).containsExactly("firefox");
    }

    @Test
    void testOnlyPreservesYamlOrder() {
        assertThat(installedConfiguration("--only", "firefox", "--only", "toolfetch").tools()).extracting(Tool::id)
                .containsExactly("toolfetch", "firefox");
    }

    @Test
    void testSkipSelectedTool() {
        assertThat(installedConfiguration("--skip", "firefox").tools()).extracting(Tool::id)
                .containsExactly("toolfetch", "dbeaver");
    }

    @Test
    void testRepeatedSkip() {
        assertThat(installedConfiguration("--skip", "firefox", "--skip", "toolfetch").tools()).extracting(Tool::id)
                .containsExactly("dbeaver");
    }

    @ParameterizedTest
    @ValueSource(strings = {"--only", "--skip"})
    void testDuplicateIdsAreIgnored(String option) {
        Configuration once = installedConfiguration(option, "firefox");
        assertThat(installedConfiguration(option, "firefox", option, "firefox")).isEqualTo(once);
    }

    @Test
    void testPreservesConfigurationSettings() {
        Configuration configuration = installedConfiguration("--only", "firefox");
        assertThat(configuration.destination()).isEqualTo("/tmp");
        assertThat(configuration.http()).isEqualTo(http().trustStorePath("custom/cacerts").withDefaultTrustStoreType().build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"--only", "--skip"})
    void testUnknownIdPreventsInstallation(String option) {
        assertUsageError(option, "firefox", option, "unknown");
        assertThat(error.toString()).contains("Unknown tool ID: 'unknown'");
    }

    @Test
    void testCaseSensitiveIds() {
        assertUsageError("--only", "Firefox");
        assertThat(error.toString()).contains("Unknown tool ID: 'Firefox'");
    }

    @Test
    void testMutuallyExclusiveOptions() {
        assertUsageError("--only", "firefox", "--skip", "dbeaver");
        assertThat(error.toString()).contains("Error: --only=<id>, --skip=<id> are mutually exclusive (specify only one)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"--only", "--skip"})
    void testMissingId(String option) {
        assertUsageError(option);
        assertThat(error.toString()).contains("Missing required parameter for option '%s' (<id>)".formatted(option));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--only", "--skip"})
    void testEmptyId(String option) {
        assertUsageError(option, "");
        assertThat(error.toString()).contains("Unknown tool ID: ''");
    }

    @Test
    void testSkippingAllToolsPreventsInstallation() {
        assertUsageError("--skip", "firefox", "--skip", "toolfetch", "--skip", "dbeaver");
        assertThat(error.toString()).contains("No tools selected");
    }

    @Test
    void testInvalidConfigurationPreventsInstallation() throws IOException {
        Files.writeString(configPath, "tools: invalid");
        assertUsageError(CommandLine.ExitCode.SOFTWARE, "--only", "firefox");
        testLogListAppenderSteps.assertAnyMatch(LogLevel.ERROR.toString(), "Configuration does not match the schema:");
    }

    private Configuration installedConfiguration(String... options) {
        try (MockedConstruction<ArchiveInstallationService> installation = mockConstruction(ArchiveInstallationService.class)) {
            assertThat(commandLine.execute(arguments(options))).isEqualTo(CommandLine.ExitCode.OK);
            assertThat(installation.constructed()).hasSize(1);
            ArgumentCaptor<Configuration> configuration = captor();
            verify(installation.constructed().getFirst()).install(configuration.capture());
            return configuration.getValue();
        }
    }

    private void assertUsageError(String... options) {
        assertUsageError(CommandLine.ExitCode.USAGE, options);
    }

    private void assertUsageError(int expectedExitCode, String... options) {
        try (MockedConstruction<ArchiveInstallationService> installation = mockConstruction(ArchiveInstallationService.class)) {
            assertThat(commandLine.execute(arguments(options))).isEqualTo(expectedExitCode);
            assertThat(installation.constructed()).isEmpty();
        }
    }

    private String[] arguments(String... options) {
        List<String> args = new ArrayList<>(List.of("install", "--config", configPath.toString()));
        args.addAll(List.of(options));
        return args.toArray(String[]::new);
    }

}

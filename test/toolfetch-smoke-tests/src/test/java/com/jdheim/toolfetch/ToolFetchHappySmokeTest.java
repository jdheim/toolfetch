/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.service.info.OsRelease;
import com.jdheim.toolfetch.service.util.OperatingSystemPredicates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [ToolFetch]
class ToolFetchHappySmokeTest extends ToolFetchTestBase {

    @TempDir
    Path tempDir;

    @Test
    void testDebugStrategy() throws Exception {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.createFile(toolfetchConfigPath);
        }
        ExecResult execResult = execute("install", "-c", toolfetchConfigPath.toString());
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertLogbackInit(execResult);
        assertAnyMatch(execResult, "[%s] ToolFetch".formatted(LogLevel.DEBUG));
        assertAnyMatch(execResult, "[%s] Build Time:".formatted(LogLevel.DEBUG));
        assertAnyMatch(execResult, "[%s] Build Revision:".formatted(LogLevel.DEBUG));
        assertAnyMatch(execResult, "[%s] Build GraalVM:".formatted(LogLevel.DEBUG));
        assertAnyMatch(execResult, debug("Executable"));
        assertAnyMatch(execResult, debug("Runtime Mode"));
        assertAnyMatch(execResult, debug("Java Home"));
        assertAnyMatch(execResult, debug("User Home"));
        assertAnyMatch(execResult, debug("Working Directory"));
        assertAnyMatch(execResult, debug("Operating System"));
        assertAnyMatch(execResult, debug("OS Version"));
        assertAnyMatch(execResult, debug("OS Architecture"));
        if (OperatingSystemPredicates.isLinux()) {
            Map<String, String> osRelease = OsRelease.get();
            assertAnyMatch(execResult, debug("Distro", osRelease.get(OsRelease.NAME)));
            assertAnyMatch(execResult, debug("Distro Version", osRelease.get(OsRelease.VERSION)));
        }
        if (runtimeModeNativeImage()) {
            assertOneMatch(execResult, "Command exit code: " + CommandLine.ExitCode.SOFTWARE);
            assertOneMatch(execResult, "[INFO] Command completed in ");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-v", "--version", "-h", "--help"})
    void testVersionAndHelp(String option) throws Exception {
        ExecResult execResult = execute(option);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.OK);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-h", "--help"})
    void testInstallHelp(String option) throws Exception {
        ExecResult execResult = execute("install", option);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
        assertAnyMatch(execResult, "Usage: toolfetch install");
        assertAnyMatch(execResult, "Install tools");
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.OK);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

}

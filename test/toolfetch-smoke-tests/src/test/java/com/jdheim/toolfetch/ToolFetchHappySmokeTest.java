/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.service.info.OsRelease;
import com.jdheim.toolfetch.service.util.OperatingSystemPredicates;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
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
        ExecResult execResult = execute("-c", tempDir.resolve("missing.yaml").toString());
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
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
        if (runtimeModeNativeImage(execResult)) {
            assertAnyMatch(execResult, debug("Command exit code"));
        }
    }

    private boolean runtimeModeNativeImage(ExecResult execResult) {
        return execResult.logs().stream().anyMatch(line -> line.contains(debug("Runtime Mode", "Native Image")));
    }

    private String debug(String label) {
        return debug(label, null);
    }

    private String debug(String label, @Nullable Object value) {
        return "[%s] %s%s".formatted(LogLevel.DEBUG, StringUtils.rightPad(label + ":", 19),
                value == null ? StringUtils.EMPTY : value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-v", "--version", "-h", "--help"})
    void testVersionAndHelp(String option) throws Exception {
        ExecResult execResult = execute(option);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
    }

}

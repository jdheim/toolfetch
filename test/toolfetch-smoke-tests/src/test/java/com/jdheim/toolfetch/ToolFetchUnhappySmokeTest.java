/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import com.jdheim.toolfetch.command.ToolFetch;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/// Smoke Tests for [ToolFetch]
class ToolFetchUnhappySmokeTest extends ToolFetchTestBase {

    @Test
    void testAutoDetectedConfigNotFound() throws Exception {
        ExecResult execResult = execute();
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        Path configPath = Path.of(SystemUtils.USER_DIR, "toolfetch.yaml");
        assertAnyMatch(execResult, "File \"%s\" does not exist".formatted(configPath));
    }

    @Test
    void testUnknownOption() throws Exception {
        ExecResult execResult = execute("-z");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unknown option: '-z'");
    }

}

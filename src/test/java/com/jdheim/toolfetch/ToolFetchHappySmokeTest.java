/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import com.jdheim.toolfetch.command.ToolFetch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [ToolFetch]
class ToolFetchHappySmokeTest extends ToolFetchTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"-V", "--version", "-h", "--help"})
    void testVersionAndHelp(String option) throws Exception {
        ExecResult execResult = execute(option);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
        assertLogbackInit(execResult);
    }

}

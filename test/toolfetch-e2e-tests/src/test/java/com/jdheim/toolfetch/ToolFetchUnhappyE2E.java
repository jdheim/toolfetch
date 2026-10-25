/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static org.assertj.core.api.Assertions.assertThat;

import com.jdheim.toolfetch.command.ToolFetch;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/// E2E Tests for [ToolFetch]
class ToolFetchUnhappyE2E extends ToolFetchTestBase {

    @Test
    void testMissingSubcommand() throws Exception {
        ExecResult execResult = execute();
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Missing required subcommand");
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @Test
    void testUnknownRootOption() throws Exception {
        ExecResult execResult = execute("-z");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unknown option: '-z'");
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

    @Test
    void testUnknownCommand() throws Exception {
        ExecResult execResult = execute("abc");
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Unmatched argument at index 0: 'abc'");
        assertAnyMatch(execResult, "Usage: toolfetch [-hv] [COMMAND]");
        assertNoMatch(execResult, "Command exit code: " + CommandLine.ExitCode.USAGE);
        assertNoMatch(execResult, "[INFO] Command completed in ");
    }

}

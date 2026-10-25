/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import picocli.CommandLine;

/// Component Tests for [ExecutionExceptionHandler]
class ExecutionExceptionHandlerTest {

    ExecutionExceptionHandler handler;

    @RegisterExtension
    final TestLogListAppenderSteps testLogListAppenderSteps = new TestLogListAppenderSteps();

    @BeforeEach
    void setUp() {
        handler = new ExecutionExceptionHandler();
    }

    @Test
    void testExecutionException() {
        CommandLine commandLine = mock();
        CommandLine.Model.CommandSpec commandSpec = mock();
        CommandLine.ParseResult parseResult = mock();
        doReturn(commandSpec).when(commandLine).getCommandSpec();
        doReturn(CommandLine.ExitCode.SOFTWARE).when(commandSpec).exitCodeOnExecutionException();

        int exitCode = handler.handleExecutionException(new IllegalStateException("Failed"), commandLine, parseResult);

        testLogListAppenderSteps.assertExceptionLogged(IllegalStateException.class);
        testLogListAppenderSteps.assertAnyMatch(LogLevel.ERROR.toString(),
                "Command failed: \"java.lang.IllegalStateException: Failed\"");
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        verify(commandLine, never()).getErr();
    }

}

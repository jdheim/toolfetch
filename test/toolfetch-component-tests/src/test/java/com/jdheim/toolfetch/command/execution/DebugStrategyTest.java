/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import picocli.CommandLine;

/// Component Tests for [DebugStrategy]
class DebugStrategyTest {

    @TempDir
    Path tempDir;

    DebugStrategy strategy;

    @RegisterExtension
    final TestLogListAppenderSteps logs = new TestLogListAppenderSteps();

    @BeforeEach
    void setUp() {
        strategy = new DebugStrategy();
    }

    @Test
    void testExecutablePathFallbackLogsExceptionAtDebug() throws IOException {
        Path executable = spy(Files.createFile(tempDir.resolve("java")));
        doThrow(new IOException("Cannot resolve executable")).when(executable).toRealPath();
        ProcessHandle process = mock();
        ProcessHandle.Info info = mock();
        when(process.info()).thenReturn(info);
        String command = executable.toString();
        when(info.command()).thenReturn(Optional.of(command));
        CommandLine.ParseResult parseResult = mock();
        try (MockedStatic<ProcessHandle> processes = mockStatic();
             MockedStatic<Path> paths = mockStatic(CALLS_REAL_METHODS)) {
            processes.when(ProcessHandle::current).thenReturn(process);
            paths.when(() -> Path.of(command)).thenReturn(executable);

            assertThat(strategy.execute(parseResult)).isEqualTo(CommandLine.ExitCode.OK);

            logs.assertExceptionLogged(IOException.class);
            logs.assertNoErrorNoWarn();
        }
    }

}

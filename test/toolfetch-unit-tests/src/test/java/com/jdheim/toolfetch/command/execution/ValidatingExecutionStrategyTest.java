/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/// OOC Tests for [ValidatingExecutionStrategy]
class ValidatingExecutionStrategyTest {

    ValidatingExecutionStrategy validatingExecutionStrategy;

    @BeforeEach
    void setUp() {
        validatingExecutionStrategy = new ValidatingExecutionStrategy();
    }

    @Test
    void testCommandNotSupported() {
        CommandLine.ParseResult parseResultMock = mock(CommandLine.ParseResult.class);
        CommandLine.Model.CommandSpec commandSpecMock = mock(CommandLine.Model.CommandSpec.class);
        CommandLine commandLine = mock(CommandLine.class);
        doReturn(commandSpecMock).when(parseResultMock).commandSpec();
        doReturn(commandLine).when(commandSpecMock).commandLine();
        doReturn("").when(commandSpecMock).userObject();
        assertThatExceptionOfType(CommandLine.ParameterException.class).isThrownBy(
                () -> validatingExecutionStrategy.execute(parseResultMock)).withMessage("Command String not supported");
    }

}

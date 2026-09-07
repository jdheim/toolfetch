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

/// OOC Tests for [ValidateStrategy]
class ValidateStrategyTest {

    ValidateStrategy validateStrategy;

    @BeforeEach
    void setUp() {
        validateStrategy = new ValidateStrategy();
    }

    @Test
    void testCommandNotSupported() {
        CommandLine.ParseResult parseResultMock = mock();
        CommandLine.Model.CommandSpec commandSpecMock = mock();
        CommandLine commandLine = mock();
        doReturn(commandSpecMock).when(parseResultMock).commandSpec();
        doReturn(commandLine).when(commandSpecMock).commandLine();
        doReturn("").when(commandSpecMock).userObject();
        assertThatExceptionOfType(CommandLine.ParameterException.class).isThrownBy(() -> validateStrategy.execute(parseResultMock))
                .withMessage("Command String not supported");
    }

}

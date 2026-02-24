/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdheim.toolfetch.command.picocli.execution;

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

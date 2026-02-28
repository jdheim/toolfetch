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
    }

}

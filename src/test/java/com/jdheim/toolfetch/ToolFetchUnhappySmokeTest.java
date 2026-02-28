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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [ToolFetch]
class ToolFetchUnhappySmokeTest extends ToolFetchTestBase {

    @ParameterizedTest
    @ValueSource(strings = {StringUtils.EMPTY, "-z"})
    void testMissingRequiredOption(String option) throws Exception {
        String[] args = StringUtils.EMPTY.equals(option) ? new String[]{} : new String[]{option};
        ExecResult execResult = execute(args);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
        assertAnyMatch(execResult, "Missing required option: '--config=<configPath>'");
    }

}

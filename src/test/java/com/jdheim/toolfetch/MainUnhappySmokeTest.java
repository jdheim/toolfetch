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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.service.config.parse.YamlParserService;
import com.jdheim.toolfetch.service.config.validation.JsonSchemaValidationService;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [Main]
class MainUnhappySmokeTest {

    CommandLine toolFetch;

    StringWriter log;

    TestLogListAppender testLogListAppender;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        toolFetch = Main.commandLine();
        log = new StringWriter();
        toolFetch.setOut(new PrintWriter(log));
        toolFetch.setErr(new PrintWriter(log));
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(YamlParserService.class, JsonSchemaValidationService.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {StringUtils.EMPTY, "-z"})
    void testToolFetch_MissingRequiredOption(String option) {
        String[] args = StringUtils.EMPTY.equals(option) ? new String[]{} : new String[]{option};
        int exitCode = toolFetch.execute(args);
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
        assertThat(log.toString()).contains("Missing required option: '--config=<configPath>'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testToolFetch_UnknownOption(String option) throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        int exitCode = toolFetch.execute(option, toolfetchConfigPath.toString(), "-z");
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
        assertThat(log.toString()).contains("Unknown option: '-z'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testToolFetch_UnmatchedArgument(String option) throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        int exitCode = toolFetch.execute(option, toolfetchConfigPath.toString(), "arg");
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
        assertThat(log.toString()).contains("Unmatched argument at index 2: 'arg'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testToolFetch_MissingConfigPath(String option) {
        int exitCode = toolFetch.execute(option);
        assertMissingRequiredParameterConfigPath(exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testToolFetch_EmptyConfigPath(String option) {
        int exitCode = toolFetch.execute(option, StringUtils.EMPTY);
        assertMissingRequiredParameterConfigPath(exitCode);
    }

    private void assertMissingRequiredParameterConfigPath(int exitCode) {
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
        assertThat(log.toString()).contains("Missing required parameter for option '--config' (<configPath>)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testToolFetch_ParsingError(String option) throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.createFile(toolfetchConfigPath);
        }
        int exitCode = toolFetch.execute(option, toolfetchConfigPath.toString());
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        testLogListAppender.assertAnyMatch(Level.ERROR, "Error occurred when parsing YAML configuration: should not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testToolFetch_JsonSchemaError(String option) throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        if (!Files.exists(toolfetchConfigPath)) {
            Files.writeString(toolfetchConfigPath, "destination: /tmp");
        }
        int exitCode = toolFetch.execute(option, toolfetchConfigPath.toString());
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
        testLogListAppender.assertAnyMatch(Level.ERROR, "- required property 'tools' not found");
    }

}

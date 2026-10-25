/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// Component Tests for [YamlParserService#validateConfig(Object)]
class YamlParserServiceUnhappyTest {

    YamlParserService parserService;

    @RegisterExtension
    final TestLogListAppenderSteps testLogListAppenderSteps = new TestLogListAppenderSteps();

    @TempDir
    Path tempDir;

    static Stream<Arguments> validateConfigTestCases() {
        return Stream.of(Arguments.of(true, Map.of("testKey", "testValue"), null),
                Arguments.of(false, "Not a Map", "YAML configuration must use 'key: value' entries, such as 'destination: /path/to/tools'"));
    }

    @BeforeEach
    void setUp() {
        parserService = new YamlParserService();
    }

    @Test
    void testParseLogsExceptionAtDebugLevel() {
        Path toolfetchConfigPath = tempDir.resolve("missing.yaml");
        assertThat(parserService.parse(toolfetchConfigPath)).isEmpty();
        testLogListAppenderSteps.assertAnyMatch(LogLevel.ERROR.toString(),
                "Cannot parse YAML configuration: \"java.nio.file.NoSuchFileException: %s\"".formatted(toolfetchConfigPath));
        testLogListAppenderSteps.assertExceptionLogged(NoSuchFileException.class);
    }

    @Test
    void testLogIfNull() throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        parserService.parse(toolfetchConfigPath);
        testLogListAppenderSteps.assertAnyMatch(LogLevel.ERROR.toString(),
                "YAML configuration is empty. Add configuration settings");
    }

    @ParameterizedTest
    @MethodSource("validateConfigTestCases")
    void testValidateConfig(boolean expectedValid, Object rawConfiguration, String message) {
        boolean valid = parserService.validateConfig(rawConfiguration);
        assertThat(valid).isEqualTo(expectedValid);
        if (!expectedValid) {
            testLogListAppenderSteps.assertAnyMatch(LogLevel.ERROR.toString(), message);
        }
    }

}

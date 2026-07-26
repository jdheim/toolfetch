/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// OOC Tests for [YamlParserService#validateConfig(Object)]
class YamlParserServiceUnhappyTest {

    YamlParserService parserService;

    TestLogListAppenderSteps testLogListAppenderSteps;

    @TempDir
    Path tempDir;

    static Stream<Arguments> validateConfigTestCases() {
        return Stream.of(Arguments.of(true, Map.of("testKey", "testValue"), null),
                Arguments.of(false, "Not a Map", "Error occurred when parsing YAML configuration: should be a map object"));
    }

    @BeforeEach
    void setUp() {
        parserService = new YamlParserService();
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start(YamlParserService.class);
    }

    @Test
    void testLogIfNull() throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        parserService.parse(toolfetchConfigPath);
        testLogListAppenderSteps.assertAnyMatch(Level.ERROR,
                "Error occurred when parsing YAML configuration: should not be empty");
    }

    @ParameterizedTest
    @MethodSource("validateConfigTestCases")
    void testValidateConfig(boolean expectedValid, Object rawConfiguration, String message) {
        boolean valid = parserService.validateConfig(rawConfiguration);
        assertThat(valid).isEqualTo(expectedValid);
        if (!expectedValid) {
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, message);
        }
    }

}

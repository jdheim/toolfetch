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

package com.jdheim.toolfetch.service.config.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// OOC Tests for [YamlParserService#validateConfig(Object)]
class YamlParserServiceUnhappyTest {

    YamlParserService parserService;

    TestLogListAppender testLogListAppender;

    @TempDir
    Path tempDir;

    static Stream<Arguments> validateConfigTestCases() {
        return Stream.of(Arguments.of(true, Map.of("testKey", "testValue"), null),
                Arguments.of(false, "Not a Map", "Error occurred when parsing YAML configuration: should be a map object"));
    }

    @BeforeEach
    void setUp() {
        parserService = new YamlParserService();
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(YamlParserService.class);
    }

    @Test
    void testLogIfNull() throws IOException {
        Path toolfetchConfigPath = tempDir.resolve("toolfetch.yaml");
        Files.createFile(toolfetchConfigPath);
        parserService.parse(toolfetchConfigPath);
        testLogListAppender.assertAnyMatch(Level.ERROR, "Error occurred when parsing YAML configuration: should not be empty");
    }

    @ParameterizedTest
    @MethodSource("validateConfigTestCases")
    void testValidateConfig(boolean expectedValid, Object rawConfiguration, String message) {
        boolean valid = parserService.validateConfig(rawConfiguration);
        assertThat(valid).isEqualTo(expectedValid);
        if (!expectedValid) {
            testLogListAppender.assertAnyMatch(Level.ERROR, message);
        }
    }

}

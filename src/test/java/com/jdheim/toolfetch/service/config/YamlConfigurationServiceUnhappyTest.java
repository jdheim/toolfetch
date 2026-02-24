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

package com.jdheim.toolfetch.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Optional;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.config.parse.YamlParserService;
import com.jdheim.toolfetch.service.config.validation.JsonSchemaValidationService;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// OOC Tests for [YamlConfigurationService]
class YamlConfigurationServiceUnhappyTest {

    static final String OOC_CLASS_PATH = YamlConfigurationServiceHappyTest.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();

    static final String UNHAPPY_PATH = "config/unhappy";

    ConfigurationService configurationService;

    TestLogListAppender testLogListAppender;

    @BeforeEach
    void setUp() {
        configurationService = new YamlConfigurationService();
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(YamlParserService.class, JsonSchemaValidationService.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            StringUtils.EMPTY, "toolfetch_not-exists.yaml"
    })
    void testConfigPath(String configPath) {
        parseConfigPath(configPath);
        testLogListAppender.assertAnyMatch(Level.ERROR, "Error occurred when parsing YAML configuration:");
    }

    @Test
    void testRequiredPropertyNotFound() {
        parseConfigPath("toolfetch_required-property-not-found.yaml");
        testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
        testLogListAppender.assertAnyMatch(Level.ERROR, "- required property 'destination' not found");
    }

    @Test
    void testPropertyNotDefined() {
        parseConfigPath("toolfetch_property-not-defined.yaml");
        testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
        testLogListAppender.assertAnyMatch(Level.ERROR,
                "- property 'incorrect' is not defined in the schema and the schema does not allow additional properties");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "toolfetch_id-null.yaml", "toolfetch_id-empty.yaml", "toolfetch_id-space.yaml", "toolfetch_id-space-inside.yaml",
            "toolfetch_id-space-trim.yaml", "toolfetch_id-space-trim-beginning.yaml", "toolfetch_id-space-trim-end.yaml"
    })
    void testId(String configPath) {
        parseConfigPath("id/" + configPath);
        if (configPath.contains("-null")) {
            testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppender.assertAnyMatch(Level.ERROR, "- null found, string expected");
        } else if (configPath.contains("-empty")) {
            testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppender.assertAnyMatch(Level.ERROR, "- must be at least 1 characters long");
        } else {
            testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppender.assertAnyMatch(Level.ERROR, "- does not match the regex pattern ^\\S+$");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "toolfetch_url-empty.yaml", "toolfetch_url-no-variable-with-version.yaml", "toolfetch_url-regex-mismatch_file.yaml",
            "toolfetch_url-regex-mismatch_ftp.yaml", "toolfetch_url-regex-mismatch_version_as-var.yaml",
            "toolfetch_url-regex-mismatch_version_empty.yaml", "toolfetch_url-regex-mismatch_version_incomplete.yaml",
            "toolfetch_url-regex-mismatch_version_pascalcase.yaml", "toolfetch_url-regex-mismatch_version_uppercase.yaml"
    })
    void testUrl(String configPath) {
        parseConfigPath("url/" + configPath);
        if (configPath.contains("-empty")) {
            testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppender.assertAnyMatch(Level.ERROR, "- must be at least 1 characters long");
            testLogListAppender.assertAnyMatch(Level.ERROR,
                    "- does not match the regex pattern ^https?://(?:(?!\\$\\{(?!version\\})).)*$");
        } else if (configPath.contains("-no-variable-with-version")) {
            testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppender.assertAnyMatch(Level.ERROR, "- must not be valid to the schema {\"required\":[\"version\"]}");
            testLogListAppender.assertAnyMatch(Level.ERROR, "- must not be valid to the schema {\"required\":[\"version\"]}");
        } else {
            testLogListAppender.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppender.assertAnyMatch(Level.ERROR,
                    "- does not match the regex pattern ^https?://(?:(?!\\$\\{(?!version\\})).)*$");
        }
    }

    private void parseConfigPath(String configPath) {
        Path testConfigPath = Path.of(OOC_CLASS_PATH, UNHAPPY_PATH, configPath);
        Optional<Configuration> configuration = configurationService.parse(testConfigPath);
        assertThat(configuration).isEmpty();
    }

}

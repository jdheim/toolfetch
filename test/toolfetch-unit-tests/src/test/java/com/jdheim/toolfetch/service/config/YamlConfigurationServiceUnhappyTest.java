/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Optional;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.config.parse.YamlParserService;
import com.jdheim.toolfetch.service.config.validation.JsonSchemaValidationService;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
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

    TestLogListAppenderSteps testLogListAppenderSteps;

    @BeforeEach
    void setUp() {
        configurationService = new YamlConfigurationService();
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start(YamlParserService.class, JsonSchemaValidationService.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            StringUtils.EMPTY, "toolfetch_not-exists.yaml"
    })
    void testConfigPath(String configPath) {
        parseConfigPath(configPath);
        testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Error occurred when parsing YAML configuration:");
    }

    @Test
    void testRequiredPropertyNotFound() {
        parseConfigPath("toolfetch_required-property-not-found.yaml");
        testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
        testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "- required property 'destination' not found");
    }

    @Test
    void testPropertyNotDefined() {
        parseConfigPath("toolfetch_property-not-defined.yaml");
        testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
        testLogListAppenderSteps.assertAnyMatch(Level.ERROR,
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
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "- null found, string expected");
        } else if (configPath.contains("-empty")) {
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "- must be at least 1 characters long");
        } else {
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "- does not match the regex pattern ^\\S+$");
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
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "- must be at least 1 characters long");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR,
                    "- does not match the regex pattern ^https?://(?:(?!\\$\\{(?!version\\})).)*$");
        } else if (configPath.contains("-no-variable-with-version")) {
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR,
                    "- must not be valid to the schema {\"required\":[\"version\"]}");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR,
                    "- must not be valid to the schema {\"required\":[\"version\"]}");
        } else {
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR, "Config does not conform to schema:");
            testLogListAppenderSteps.assertAnyMatch(Level.ERROR,
                    "- does not match the regex pattern ^https?://(?:(?!\\$\\{(?!version\\})).)*$");
        }
    }

    private void parseConfigPath(String configPath) {
        Path testConfigPath = Path.of(OOC_CLASS_PATH, UNHAPPY_PATH, configPath);
        Optional<Configuration> configuration = configurationService.parse(testConfigPath);
        assertThat(configuration).isEmpty();
    }

}

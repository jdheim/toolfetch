/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.http.Http;
import com.jdheim.toolfetch.model.http.ssl.Ssl;
import com.jdheim.toolfetch.model.http.ssl.truststore.TrustStore;
import com.jdheim.toolfetch.model.tool.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// OOC Tests for [YamlConfigurationService]
class YamlConfigurationServiceHappyTest {

    static final String OOC_CLASS_PATH = YamlConfigurationServiceHappyTest.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();

    static final String HAPPY_PATH = "config/happy";

    ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new YamlConfigurationService();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "toolfetch_latest-version.yaml", "toolfetch_only-mandatory-fields.yaml",
            "toolfetch_tool-destination-optional-field.yaml", "toolfetch_trustStore.yaml"
    })
    void testConfigPath(String configPath) {
        Path testConfigPath = Path.of(OOC_CLASS_PATH, HAPPY_PATH, configPath);
        Optional<Configuration> configuration = configurationService.parse(testConfigPath);
        assertThat(configuration).isNotEmpty();

        String destination = configuration.map(Configuration::destination).orElseThrow();
        assertThat(destination).isEqualTo("/tmp");

        if (configPath.contains("_trustStore")) {
            TrustStore trustStore = configuration.map(Configuration::http).map(Http::ssl).map(Ssl::trustStore).orElseThrow();
            TrustStore expectedTrustStore = new TrustStore("$JAVA_HOME/lib/security/cacerts", "PKCS12");
            assertThat(trustStore).isNotNull().isEqualTo(expectedTrustStore);
        }

        List<Tool> tools = configuration.map(Configuration::tools).orElseThrow();
        int expectedSize = configPath.contains("latest-version") ? 1 : 2;
        assertThat(tools).isNotNull().hasSize(expectedSize);

        assertFirstTool(configPath, tools.getFirst());
        if (!configPath.contains("latest-version")) {
            assertSecondTool(tools.get(1));
        }
    }

    private void assertFirstTool(String configPath, Tool firstTool) {
        String expectedDestination = null;
        if (configPath.contains("destination-optional-field")) {
            expectedDestination = "/tmp/test";
        }
        Tool expectedFirstTool;
        if (configPath.contains("latest-version")) {
            expectedFirstTool = new Tool("dbeaver", "https://dbeaver.io/files/dbeaver-ce-latest-linux.gtk.x86_64.tar.gz", null,
                    expectedDestination);
        } else {
            expectedFirstTool = new Tool("toolfetch",
                    "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz",
                    "0.0.3", expectedDestination);
        }
        assertThat(firstTool).isNotNull().isEqualTo(expectedFirstTool);
    }

    private void assertSecondTool(Tool secondTool) {
        Tool expectedSecondTool = new Tool("firefox",
                "https://ftp.mozilla.org/pub/firefox/releases/${version}/linux-x86_64/en-US/firefox-${version}.tar.xz",
                "146.0.1");
        assertThat(secondTool).isNotNull().isEqualTo(expectedSecondTool);
    }

}

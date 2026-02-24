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
import java.util.List;
import java.util.Optional;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
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
            "toolfetch_tool-destination-optional-field.yaml"
    })
    void testConfigPath(String configPath) {
        Path testConfigPath = Path.of(OOC_CLASS_PATH, HAPPY_PATH, configPath);
        Optional<Configuration> configuration = configurationService.parse(testConfigPath);
        assertThat(configuration).isNotEmpty();

        String destination = configuration.map(Configuration::destination).orElseThrow();
        assertThat(destination).isEqualTo("/tmp");

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
            expectedFirstTool = new Tool("dbeaver", null, "https://dbeaver.io/files/dbeaver-ce-latest-linux.gtk.x86_64.tar.gz",
                    expectedDestination);
        } else {
            expectedFirstTool = new Tool("kitty", "0.44.0",
                    "https://github.com/kovidgoyal/kitty/releases/download/v${version}/kitty-${version}-x86_64.txz",
                    expectedDestination);
        }
        assertThat(firstTool).isNotNull().isEqualTo(expectedFirstTool);
    }

    private void assertSecondTool(Tool secondTool) {
        Tool expectedSecondTool = new Tool("firefox", "146.0.1",
                "https://ftp.mozilla.org/pub/firefox/releases/${version}/linux-x86_64/en-US/firefox-${version}.tar.xz", null);
        assertThat(secondTool).isNotNull().isEqualTo(expectedSecondTool);
    }

}

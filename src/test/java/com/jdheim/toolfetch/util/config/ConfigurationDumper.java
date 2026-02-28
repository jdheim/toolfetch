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

package com.jdheim.toolfetch.util.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jdheim.toolfetch.model.Configuration;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.YamlOutputStreamWriter;
import org.snakeyaml.engine.v2.common.FlowStyle;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/// Utility class for serializing a [Configuration] object and saving it to a YAML configuration file
public final class ConfigurationDumper {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.NON_EMPTY)
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY))
            .build();

    private static final int DUMP_SETTINGS_INDICATOR_INDENT = 2;

    private ConfigurationDumper() {
        throw new AssertionError();
    }

    /// Serializes the given [Configuration] object into a YAML file and writes it to the specified file path
    public static void saveToConfigFile(Configuration config, Path configPath) throws IOException {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            DumpSettings dumpSettings = DumpSettings.builder()
                    .setDefaultFlowStyle(FlowStyle.BLOCK)
                    .setIndicatorIndent(DUMP_SETTINGS_INDICATOR_INDENT)
                    .setIndentWithIndicator(true)
                    .build();
            Dump dump = new Dump(dumpSettings);
            Map<String, Object> configMap = MAPPER.convertValue(config, new TypeReference<>() {});
            dump.dump(configMap, new YamlOutputStreamWriter(out, StandardCharsets.UTF_8));
        }
        assertThat(configPath).isRegularFile()
                .content()
                .containsOnlyOnce("destination:")
                .containsOnlyOnce("tools:")
                .contains("- id:", "url:")
                .doesNotContain("null");
    }

}

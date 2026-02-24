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

import java.nio.file.Path;
import java.util.Optional;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.config.parse.ParserService;
import com.jdheim.toolfetch.service.config.parse.YamlParserService;
import com.jdheim.toolfetch.service.config.validation.JsonSchemaValidationService;
import com.jdheim.toolfetch.service.config.validation.ValidationService;
import tools.jackson.databind.ObjectMapper;

public class YamlConfigurationService implements ConfigurationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ParserService parserService;

    private final ValidationService validationService;

    public YamlConfigurationService() {
        parserService = new YamlParserService();
        validationService = new JsonSchemaValidationService();
    }

    @Override
    public Optional<Configuration> parse(Path configPath) {
        return parserService.parse(configPath).filter(validationService::validateJsonSchema).map(this::toConfiguration);
    }

    private Configuration toConfiguration(Object rawConfiguration) {
        return MAPPER.convertValue(rawConfiguration, Configuration.class);
    }

}

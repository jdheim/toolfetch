/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
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

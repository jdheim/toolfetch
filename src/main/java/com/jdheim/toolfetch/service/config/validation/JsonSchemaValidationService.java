/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config.validation;

import java.util.List;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JsonSchemaValidationService implements ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(JsonSchemaValidationService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean validateJsonSchema(Object rawConfiguration) {
        JsonNode jsonConfiguration = toJson(rawConfiguration);
        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialectId(null, builder -> builder.schemaIdResolvers(
                schemaIdResolvers -> schemaIdResolvers.mapPrefix("https://jdheim.com/schema", "classpath:/schema")));
        Schema schema = schemaRegistry.getSchema(SchemaLocation.of("https://jdheim.com/schema/toolfetch.schema.json"));
        List<Error> errors = schema.validate(jsonConfiguration, executionContext -> executionContext.executionConfig(
                executionConfig -> executionConfig.formatAssertionsEnabled(true)));
        if (!errors.isEmpty()) {
            LOG.error("Config does not conform to schema:");
            errors.forEach(error -> LOG.error("- {}", error.getMessage()));
            return false;
        }
        return true;
    }

    private JsonNode toJson(Object rawConfiguration) {
        return MAPPER.valueToTree(rawConfiguration);
    }

}

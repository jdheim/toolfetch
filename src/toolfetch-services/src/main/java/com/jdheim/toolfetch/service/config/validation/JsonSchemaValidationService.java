/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config.validation;

import java.util.List;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JsonSchemaValidationService implements ValidationService {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(JsonSchemaValidationService.class);

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
            LOGGER.log("json-schema-validation.issue-header");
            errors.forEach(error -> LOGGER.log("json-schema-validation.issue", error.getMessage()));
            return false;
        }
        return true;
    }

    private JsonNode toJson(Object rawConfiguration) {
        return MAPPER.valueToTree(rawConfiguration);
    }

}

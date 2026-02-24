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

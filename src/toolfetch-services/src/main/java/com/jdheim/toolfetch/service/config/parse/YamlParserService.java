/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.config.parse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import org.apache.commons.lang3.StringUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

public class YamlParserService implements ParserService {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(YamlParserService.class);

    @Override
    public Optional<Object> parse(Path yamlConfigPath) {
        try (InputStream in = Files.newInputStream(yamlConfigPath)) {
            return loadFromInputStream(in);
        } catch (IOException | YamlEngineException e) {
            LOGGER.log("configuration-parser.exception", getExceptionMessage(e));
            return Optional.empty();
        }
    }

    private Optional<Object> loadFromInputStream(InputStream in) {
        LoadSettings loadSettings = LoadSettings.builder().build();
        Load load = new Load(loadSettings);
        return Optional.ofNullable(logIfNull(load.loadFromInputStream(in))).filter(this::validateConfig);
    }

    Object logIfNull(Object rawConfiguration) {
        if (rawConfiguration == null) {
            LOGGER.log("configuration-parser.empty");
        }
        return rawConfiguration;
    }

    boolean validateConfig(Object rawConfiguration) {
        if (!(rawConfiguration instanceof Map)) {
            LOGGER.log("configuration-parser.not-map");
            return false;
        }
        return true;
    }

    private String getExceptionMessage(Exception exception) {
        return Optional.of(exception)
                .filter(YamlEngineException.class::isInstance)
                .map(YamlEngineException.class::cast)
                .map(Throwable::getCause)
                .map(Throwable::getMessage)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(exception::getMessage);
    }

}

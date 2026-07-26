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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

public class YamlParserService implements ParserService {

    private static final Logger LOG = LoggerFactory.getLogger(YamlParserService.class);

    private static final String ERROR_MESSAGE_PREFIX = "Error occurred when parsing YAML configuration";

    @Override
    public Optional<Object> parse(Path yamlConfigPath) {
        try (InputStream in = Files.newInputStream(yamlConfigPath)) {
            return loadFromInputStream(in);
        } catch (IOException | YamlEngineException e) {
            LOG.error("{}: {}", ERROR_MESSAGE_PREFIX, getExceptionMessage(e));
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
            LOG.error("{}: should not be empty", ERROR_MESSAGE_PREFIX);
        }
        return rawConfiguration;
    }

    boolean validateConfig(Object rawConfiguration) {
        if (!(rawConfiguration instanceof Map)) {
            LOG.error("{}: should be a map object", ERROR_MESSAGE_PREFIX);
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

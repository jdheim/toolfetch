/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// OOC Tests for [ToolDestinationResolver]
class ToolDestinationResolverTest {

    DestinationResolver destinationResolver;

    @BeforeEach
    void setUp() {
        destinationResolver = new ToolDestinationResolver();
    }

    @ParameterizedTest
    @CsvSource({
            "'/tmp',''", "'/tmp','/opt'",
    })
    void testResolveDestination(String destination, String toolDestination) {
        String id = "toolfetch";
        Configuration configuration = new Configuration(destination, List.of());
        Tool tool = new Tool(id, null, StringUtils.EMPTY, toolDestination);
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        Path expectedDestination = StringUtils.isNotEmpty(toolDestination) ? Path.of(toolDestination) : Path.of(destination);
        assertThat(destinationPath).isEqualTo(expectedDestination.resolve(id));
    }

}

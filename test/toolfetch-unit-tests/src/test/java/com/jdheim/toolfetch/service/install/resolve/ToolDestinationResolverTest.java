/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Path;
import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
            "/tmp, ''", "/tmp, /opt", "/tmp, test", "$HOME/test1, ''", "$HOME/test1, $HOME/test2", "$NOT_EXISTS/test1, ''",
            "$NOT_EXISTS/test1, $NOT_EXISTS/test2"
    })
    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV",
            justification = "Test code intentionally uses System.getenv to validate environment variable handling")
    void testResolveDestination(String destination, String toolDestination) {
        String id = "toolfetch";
        Configuration configuration = new Configuration(destination, List.of());
        Tool tool = new Tool(id, StringUtils.EMPTY, null, toolDestination);
        String expectedDestination = StringUtils.isNotEmpty(toolDestination) ? toolDestination : destination;
        if (expectedDestination.startsWith("$HOME")) {
            expectedDestination = expectedDestination.replace("$HOME", System.getenv("HOME"));
        }
        Path expectedDestinationAsPath = Path.of(expectedDestination);
        if (StringUtils.isNotEmpty(toolDestination) && !expectedDestinationAsPath.isAbsolute()) {
            expectedDestinationAsPath = Path.of(destination).resolve(expectedDestinationAsPath);
        }
        if (expectedDestination.startsWith("$NOT_EXISTS")) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                            () -> destinationResolver.resolve(configuration, tool))
                    .withMessage(
                            "Environment variable \"NOT_EXISTS\" is not set (provided in \"%s\")".formatted(expectedDestination));
        } else {
            Path destinationPath = destinationResolver.resolve(configuration, tool);
            assertThat(destinationPath).isEqualTo(expectedDestinationAsPath.resolve(id));
        }
    }

}

/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.picocli.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

/// OOC Tests for [PathTrimConverter]
class PathTrimConverterTest {

    static final Path EMPTY_PATH = Path.of("");

    CommandLine.ITypeConverter<Path> converter;

    static Stream<Arguments> convertTestCases() {
        return Stream.of(Arguments.of(EMPTY_PATH, ""), Arguments.of(EMPTY_PATH, "   "), Arguments.of(Path.of("a"), " a "),
                Arguments.of(Path.of("b"), "  b  "));
    }

    @BeforeEach
    void setUp() {
        converter = new PathTrimConverter();
    }

    @ParameterizedTest
    @MethodSource("convertTestCases")
    void testValidateConfigPath(Path expected, String option) throws Exception {
        Path actual = converter.convert(option);
        assertThat(actual).isEqualTo(expected);
    }

}

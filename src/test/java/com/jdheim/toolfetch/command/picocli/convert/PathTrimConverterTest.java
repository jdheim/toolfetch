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

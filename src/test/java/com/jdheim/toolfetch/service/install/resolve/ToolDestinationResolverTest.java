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

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

import java.net.URI;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// OOC Tests for [ToolUriTransformer]
class ToolUriTransformerTest {

    UriTransformer uriTransformer;

    TestLogListAppender testLogListAppender;

    @BeforeEach
    void setUp() {
        uriTransformer = new ToolUriTransformer();
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(ToolUriTransformer.class);
    }

    @ParameterizedTest
    @CsvSource({
            ", http://localhost/download/toolfetch.zip, http://localhost/download/toolfetch.zip",
            "1.0, http://localhost/download/${version}/toolfetch.zip, http://localhost/download/1.0/toolfetch.zip",
            "2.0, https://localhost/download/${version}/toolfetch.zip, https://localhost/download/2.0/toolfetch.zip",
            "3.0.1, https://localhost/download/${version}/toolfetch-${version}.zip, https://localhost/download/3.0.1/toolfetch-3.0.1.zip"
    })
    void testTransform_Happy(String version, String url, String expectedURI) {
        Tool tool = new Tool("toolfetch", version, url, null);

        URI actualURI = uriTransformer.transform(tool);

        assertThat(actualURI).isEqualTo(URI.create(expectedURI));
    }

    @ParameterizedTest
    @CsvSource({
            "http://localhost/download/${version}/toolfetch.zip, 'Missing required parameter: version'",
            "ftp://localhost/download/toolfetch.zip, 'Forbidden scheme detected. Only http/https are allowed'"
    })
    void testTransform_Unhappy(String url, String logMessage) {
        Tool tool = new Tool("toolfetch", null, url, null);

        URI actualURI = uriTransformer.transform(tool);

        assertThat(actualURI).isNull();
        testLogListAppender.assertAnyMatch(Level.WARN, logMessage);
    }

}

/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
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

/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/// OOC Tests for [ToolFetchVersionProvider]
class ToolFetchVersionProviderTest {

    static final String SEPARATOR_PATTERN = "^-+$";

    static final String GRAALVM_VERSION_PATTERN = "\\d{2}(\\.\\d+){1,2}";

    static final String TOOLFETCH_VERSION_PATTERN = "\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?";

    static final String ISO_8601_UTC_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

    static final String GIT_REVISION_PATTERN = "[0-9a-f]{40}";

    ToolFetchVersionProvider provider;

    TestLogListAppender testLogListAppender;

    @BeforeEach
    void setUp() {
        provider = new ToolFetchVersionProvider();
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(ToolFetchInfo.class);
    }

    @AfterEach
    void tearDown() {
        ToolFetchInfo.LazyHolder.reloadTestHook("/version-info.properties");
    }

    @Test
    void testVersion() {
        String[] versionLines = provider.getVersion();
        int longestLineLength = getLongestLineLength(versionLines);
        String separator = provider.separator(versionLines);
        assertThat(separator).matches(SEPARATOR_PATTERN).hasSize(longestLineLength);
        assertThat(versionLines).hasSize(7)
                .satisfiesExactly(row -> assertThat(row).isEqualTo(separator),
                        row -> assertThat(row).matches("ToolFetch %s by JDHeim.com".formatted(TOOLFETCH_VERSION_PATTERN)),
                        row -> assertThat(row).isEqualTo(separator),
                        row -> assertThat(row).matches("Build Time:\\s{5}" + ISO_8601_UTC_PATTERN),
                        row -> assertThat(row).matches("Build Revision:\\s(%s|dev)".formatted(GIT_REVISION_PATTERN)),
                        row -> assertThat(row).matches("Build GraalVM:\\s{2}%s \\(.* %s.*\\)".formatted(GRAALVM_VERSION_PATTERN,
                                GRAALVM_VERSION_PATTERN)), row -> assertThat(row).isEqualTo(separator));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/version-info/version-info_no-filtering.properties", "/version-info/version-info_empty-filtering.properties",
            "/version-info/version-info_empty.properties"
    })
    void testVersion_Default(String versionInfoFilePath) {
        assertVersionWithVersionInfo(versionInfoFilePath);
        testLogListAppender.assertNoErrorNoWarn();
    }

    @Test
    void testVersion_ResourceNotFound() {
        assertVersionWithVersionInfo("/version-info/version-info_not-found.properties");
        testLogListAppender.assertAnyMatch(Level.WARN, "Resource not found: \"/version-info/version-info_not-found.properties\"");
    }

    @Test
    void testVersion_FailedToLoadResource() throws IOException {
        Properties propertiesMock = mock();
        doThrow(new IOException("I/O error occurred")).when(propertiesMock).load(any(InputStream.class));
        try (MockedStatic<ToolFetchInfo.LazyHolder> lazyHolder = mockStatic(CALLS_REAL_METHODS)) {
            lazyHolder.when(ToolFetchInfo.LazyHolder::getProperties).thenReturn(propertiesMock);
            assertVersionWithVersionInfo("/version-info.properties");
            lazyHolder.verify(ToolFetchInfo.LazyHolder::getProperties, times(1));
            verify(propertiesMock).load(any(InputStream.class));
            testLogListAppender.assertAnyMatch(Level.ERROR,
                    "Failed to load resource: \"/version-info.properties\" due to \"I/O error occurred\"");
        }
    }

    void assertVersionWithVersionInfo(String versionInfoFilePath) {
        ToolFetchInfo.LazyHolder.reloadTestHook(versionInfoFilePath);
        String[] versionLines = provider.getVersion();
        int longestLineLength = getLongestLineLength(versionLines);
        String separator = provider.separator(versionLines);
        assertThat(separator).matches(SEPARATOR_PATTERN).hasSize(longestLineLength);
        assertThat(versionLines).hasSize(7)
                .satisfiesExactly(row -> assertThat(row).isEqualTo(separator),
                        row -> assertThat(row).matches("ToolFetch X.X.X-DEV by JDHeim.com"),
                        row -> assertThat(row).isEqualTo(separator), row -> assertThat(row).matches("Build Time:\\s{5}-"),
                        row -> assertThat(row).matches("Build Revision:\\sdev"),
                        row -> assertThat(row).matches("Build GraalVM:\\s{2}-"), row -> assertThat(row).isEqualTo(separator));
    }

    private int getLongestLineLength(String[] versionLines) {
        int longestLineLength = Arrays.stream(versionLines)
                .filter(StringUtils::isNotBlank)
                .mapToInt(String::length)
                .max()
                .orElse(0);
        assertThat(longestLineLength).isGreaterThan(0);
        return longestLineLength;
    }

    @Test
    void testLazyHolder_NoInstance() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(ToolFetchInfo.LazyHolder::new);
    }

}

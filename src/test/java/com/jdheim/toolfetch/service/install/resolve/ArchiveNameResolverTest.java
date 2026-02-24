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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Optional;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// OOC Tests for [ArchiveNameResolver]
class ArchiveNameResolverTest {

    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    ArchiveNameResolver archiveNameResolver;

    TestLogListAppender testLogListAppender;

    @BeforeEach
    void setUp() {
        archiveNameResolver = spy(new ArchiveNameResolver());
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(ArchiveNameResolver.class);
    }

    @ParameterizedTest
    @CsvSource({
            "ATTACHMENT, ''", "attachment, ''", "attachment; filename*=''toolfetch.zip, toolfetch.zip",
            "ATTACHMENT; FILENAME*=''toolfetch.zip, toolfetch.zip", "attachment; filename*='en'toolfetch.zip, toolfetch.zip",
            "attachment;filename=toolfetch.zip, toolfetch.zip", "attachment;filename=\"tool fetch.zip\", 'tool fetch.zip'",
            "attachment;filename*=UTF-8''toolfetch.zip, toolfetch.zip",
            "attachment;filename*=UTF-8''tool%20fetch.zip, 'tool fetch.zip'",
            "attachment;filename*=UTF-8'en'toolfetch.zip, toolfetch.zip",
            "attachment;filename*=UTF-8'en'tool%20fetch.zip, 'tool fetch.zip'",
            "attachment;filename=toolfetch.zip;filename*=UTF-8'en'toolfetch.zip, 'toolfetch.zip'",
            "attachment;filename=\"tool fetch.zip\";filename*=UTF-8'en'tool%20fetch.zip, 'tool fetch.zip'",
            "attachment;filename*=UTF-8'en'toolfetch.zip;filename=toolfetch.zip;, 'toolfetch.zip'",
            "attachment;filename*=UTF-8'en'tool%20fetch.zip;filename=\"tool fetch.zip\", 'tool fetch.zip'",
            "attachment; filename = toolfetch.zip, toolfetch.zip", "attachment; filename=toolfetch.zip, toolfetch.zip",
            "attachment; filename=\"tool fetch.zip\", 'tool fetch.zip'",
            "attachment; filename* = UTF-8''toolfetch.zip, toolfetch.zip",
            "attachment; filename*=UTF-8''toolfetch.zip, toolfetch.zip",
            "attachment; filename*=UTFF-8''toolfetch.zip, toolfetch.zip",
            "attachment; filename*=utf-8''toolfetch.zip, toolfetch.zip",
            "attachment; filename*=UTF-8''tool%20fetch.zip, 'tool fetch.zip'",
            "attachment; filename*=UTF-8'en'toolfetch.zip, toolfetch.zip",
            "attachment; filename*=UTF-8'en'tool%20fetch.zip, 'tool fetch.zip'",
            "attachment; filename=toolfetch.zip; filename*=UTF-8'en'toolfetch.zip, 'toolfetch.zip'",
            "attachment; filename=\"tool fetch.zip\"; filename*=UTF-8'en'tool%20fetch.zip, 'tool fetch.zip'",
            "attachment; filename*=UTF-8'en'toolfetch.zip; filename=toolfetch.zip;, 'toolfetch.zip'",
            "attachment; filename*=UTF-8'en'tool%20fetch.zip; filename=\"tool fetch.zip\", 'tool fetch.zip'",
            "attachment; filename=this%20is%20tool%20fetch.zip, 'this is tool fetch.zip'",
            "attachment; filename*=UTF-8''this%20is%20tool%20fetch.zip, 'this is tool fetch.zip'",
            "attachment; filename=tool%20fetch(1).zip, 'tool fetch(1).zip'",
            "attachment; filename*=UTF-8''tool%20fetch(1).zip, 'tool fetch(1).zip'",
            "attachment; filename=tool%2Bfetch.zip, tool+fetch.zip",
            "attachment; filename*=UTF-8''tool%2Bfetch.zip, tool+fetch.zip",
            "attachment; filename=%C5%BC%C3%B3%C5%82%C4%87.zip, żółć.zip",
            "attachment; filename*=UTF-8''%C5%BC%C3%B3%C5%82%C4%87.zip, żółć.zip",
            "attachment; filename=tool+fetch.zip, tool+fetch.zip", "attachment; filename*=UTF-8''tool+fetch.zip, tool+fetch.zip",
            "ATTACHMENT; FILENAME=toolfetch.zip, toolfetch.zip", "ATTACHMENT; FILENAME*=UTF-8''toolfetch.zip, toolfetch.zip",
            "ATTACHMENT; FILENAME=toolfetch.zip; FILENAME*=UTF-8'en'toolfetch.zip, 'toolfetch.zip'",
            "ATTACHMENT; FILENAME*=UTF-8'en'toolfetch.zip; FILENAME=toolfetch.zip, 'toolfetch.zip'"
    })
    void testResolveFromHeader(String contentDisposition, String expectedName) {
        HttpResponse<InputStream> httpResponse = mockHttpResponse(contentDisposition);

        String actualName = archiveNameResolver.resolve(httpResponse);

        assertThat(actualName).isEqualTo(expectedName);
        verify(archiveNameResolver).resolveFromHeader(anyString());
        verify(archiveNameResolver).resolveFromFilenameRfc5987Pattern(anyString());
        if (Strings.CI.contains(contentDisposition, "filename*") && StringUtils.isNotEmpty(expectedName)) {
            verify(archiveNameResolver, never()).resolveFromFilenamePattern(anyString());
        } else {
            verify(archiveNameResolver).resolveFromFilenamePattern(anyString());
        }
        if ("attachment".equalsIgnoreCase(contentDisposition) ||
                (Strings.CI.contains(contentDisposition, "filename*") && StringUtils.isEmpty(expectedName))) {
            verify(archiveNameResolver).resolveFromUri(any());
        }
        if (contentDisposition.contains("UTFF-8")) {
            testLogListAppender.assertAnyMatch(Level.WARN,
                    "%s header contains unsupported charset: %s. Falling back to UTF-8".formatted(CONTENT_DISPOSITION_HEADER,
                            "UTFF-8"));
        }
    }

    @Test
    void testResolveFromHeader_ContentDispositionTooLong() {
        HttpResponse<InputStream> httpResponse = mockHttpResponse("attachment;filename=" + "a".repeat(8169) + ".zip");

        String actualName = archiveNameResolver.resolve(httpResponse);

        assertThat(actualName).isEqualTo(StringUtils.EMPTY);
        verify(archiveNameResolver).resolveFromHeader(anyString());
        verify(archiveNameResolver, never()).resolveFromFilenameRfc5987Pattern(anyString());
        verify(archiveNameResolver, never()).resolveFromFilenamePattern(anyString());
        verify(archiveNameResolver).resolveFromUri(any());
        testLogListAppender.assertAnyMatch(Level.WARN,
                "%s header longer than 8192 characters. Skipping archive name resolution from header".formatted(
                        CONTENT_DISPOSITION_HEADER));
    }

    @ParameterizedTest
    @CsvSource({
            "http://localhost, ''", "http://localhost/, ''", "http://localhost/download/, ''",
            "http://localhost/download/toolfetch.zip, toolfetch.zip",
            "http://localhost/download/v1/tool%20fetch.zip, 'tool fetch.zip'",
            "http://localhost/download/v2/this%20is%20tool%20fetch.zip?token=abc123&user=dev, 'this is tool fetch.zip'",
            "http://localhost/download/v3/this%20is%20tool%20fetch.zip#abc, 'this is tool fetch.zip'",
            "http://localhost/download/v4/tool%20fetch(1).zip, 'tool fetch(1).zip'",
            "http://localhost/download/v5/tool%2Bfetch.zip, tool+fetch.zip",
            "http://localhost/download/v6/tool+fetch.zip, tool+fetch.zip",
            "http://localhost/download/v7/%C5%BC%C3%B3%C5%82%C4%87.zip, żółć.zip"
    })
    void testResolveFromUri_NoContentDisposition(String uri, String expectedName) {
        HttpResponse<InputStream> httpResponse = mockHttpResponseNoContentDisposition(URI.create(uri));

        String actualName = archiveNameResolver.resolve(httpResponse);

        assertThat(actualName).isEqualTo(expectedName);
        verify(archiveNameResolver, never()).resolveFromHeader(anyString());
        verify(archiveNameResolver, never()).resolveFromFilenameRfc5987Pattern(anyString());
        verify(archiveNameResolver, never()).resolveFromFilenamePattern(anyString());
        verify(archiveNameResolver).resolveFromUri(any());
    }

    @ParameterizedTest
    @CsvSource({
            "http://localhost, ''", "http://localhost/, ''", "http://localhost/download/, ''",
            "http://localhost/download/toolfetch.zip, toolfetch.zip",
            "http://localhost/download/v1/tool%20fetch.zip, 'tool fetch.zip'",
            "http://localhost/download/v2/this%20is%20tool%20fetch.zip?token=abc123&user=dev, 'this is tool fetch.zip'",
            "http://localhost/download/v3/this%20is%20tool%20fetch.zip#abc, 'this is tool fetch.zip'",
            "http://localhost/download/v4/tool%20fetch(1).zip, 'tool fetch(1).zip'",
            "http://localhost/download/v5/tool%2Bfetch.zip, tool+fetch.zip",
            "http://localhost/download/v6/tool+fetch.zip, tool+fetch.zip",
            "http://localhost/download/v7/%C5%BC%C3%B3%C5%82%C4%87.zip, żółć.zip"
    })
    void testResolveFromUri_ContentDispositionNoFilename(String uri, String expectedName) {
        HttpResponse<InputStream> httpResponse = mockHttpResponseContentDispositionNoFilename(URI.create(uri));

        String actualName = archiveNameResolver.resolve(httpResponse);

        assertThat(actualName).isEqualTo(expectedName);
        verify(archiveNameResolver).resolveFromHeader(anyString());
        verify(archiveNameResolver).resolveFromFilenameRfc5987Pattern(anyString());
        verify(archiveNameResolver).resolveFromFilenamePattern(anyString());
        verify(archiveNameResolver).resolveFromUri(any());
    }

    private HttpResponse<InputStream> mockHttpResponse(String contentDisposition) {
        HttpResponse<InputStream> httpResponse = mock();
        HttpHeaders httpHeaders = mock();
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpHeaders.firstValue(CONTENT_DISPOSITION_HEADER)).thenReturn(Optional.of(contentDisposition));
        return httpResponse;
    }

    private HttpResponse<InputStream> mockHttpResponseNoContentDisposition(URI uri) {
        return mockHttpResponse(uri, StringUtils.EMPTY);
    }

    private HttpResponse<InputStream> mockHttpResponseContentDispositionNoFilename(URI uri) {
        return mockHttpResponse(uri, "attachment");
    }

    private HttpResponse<InputStream> mockHttpResponse(URI uri, String contentDisposition) {
        HttpResponse<InputStream> httpResponse = mock();
        when(httpResponse.uri()).thenReturn(uri);
        HttpHeaders httpHeaders = mock();
        when(httpResponse.headers()).thenReturn(httpHeaders);
        Optional<String> oContentDisposition =
                StringUtils.isEmpty(contentDisposition) ? Optional.empty() : Optional.of(contentDisposition);
        when(httpHeaders.firstValue(CONTENT_DISPOSITION_HEADER)).thenReturn(oContentDisposition);
        return httpResponse;
    }

}

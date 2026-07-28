/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jdheim.toolfetch.step.archive.ArchiveSteps.addZipEntryFile;
import static com.jdheim.toolfetch.step.archive.ArchiveSteps.createZip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.install.download.http.ToolFetchHttpClient;
import com.jdheim.toolfetch.service.install.resolve.ToolUriTransformer;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/// Integration Tests for [WebDownloadService]
@WireMockTest
class WebDownloadServiceIT {

    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    WebDownloadService webDownloadService;

    TestLogListAppenderSteps testLogListAppenderSteps;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        webDownloadService = new WebDownloadService();
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start(WebDownloadService.class, ToolUriTransformer.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/download/toolfetch.zip", "/download/v${version}/toolfetch.zip"})
    void testDownload(String mockUrl, WireMockRuntimeInfo wmRuntimeInfo) {
        String version = mockUrl.contains("${version}") ? "1.0.0" : null;
        Configuration configuration = buildConfiguration("http", mockUrl, version, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        String expectedUrl = tool.url();
        if (mockUrl.contains("${version}")) {
            String toolVersion = tool.version();
            assertThat(toolVersion).isNotNull();
            mockUrl = mockUrl.replace("${version}", toolVersion);
            expectedUrl = expectedUrl.replace("${version}", toolVersion);
        }
        stubWireMockToServeZip(mockUrl);

        Optional<Path> path = webDownloadService.download(configuration, tool);

        assertThat(getAllServeEvents()).hasSize(1);
        verify(1, getRequestedFor(urlEqualTo(mockUrl)));
        assertThat(path).isPresent()
                .hasValueSatisfying(
                        actualPath -> assertThat(actualPath).isEqualTo(tempDir.resolve(tool.id()).resolve("toolfetch.zip"))
                                .exists()
                                .isRegularFile());
        testLogListAppenderSteps.assertAnyMatch(Level.INFO, "=== Installing " + tool.id() + " ===");
        Path parentPath = path.get().getParent();
        testLogListAppenderSteps.assertAnyMatch(Level.INFO, "Creating " + parentPath);
        testLogListAppenderSteps.assertAnyMatch(Level.INFO, "Downloading %s to %s".formatted(expectedUrl, parentPath));
        testLogListAppenderSteps.assertAnyMatch(Level.INFO, "Download completed in ");
    }

    @Test
    void testDownload_ForbiddenScheme(WireMockRuntimeInfo wmRuntimeInfo) {
        String mockUrl = "/download/toolfetch.zip";
        Configuration configuration = buildConfiguration("ftp", mockUrl, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        stubWireMockToServeZip(mockUrl);

        Optional<Path> path = webDownloadService.download(configuration, tool);

        assertThat(getAllServeEvents()).isEmpty();
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN, "Forbidden scheme detected. Only http/https are allowed");
        testLogListAppenderSteps.assertAnyMatch(Level.WARN, "URI could not be resolved. Skipping " + tool.id());
    }

    @Test
    void testDownload_MissingVersion(WireMockRuntimeInfo wmRuntimeInfo) {
        String mockUrl = "/download/v${version}/toolfetch.zip";
        Configuration configuration = buildConfiguration("http", mockUrl, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        mockUrl = mockUrl.replace("${version}", "1.0.0");
        stubWireMockToServeZip(mockUrl);

        Optional<Path> path = webDownloadService.download(configuration, tool);

        assertThat(getAllServeEvents()).isEmpty();
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN, "Missing required parameter: version");
        testLogListAppenderSteps.assertAnyMatch(Level.WARN, "URI could not be resolved. Skipping " + tool.id());
    }

    @Test
    void testDownload_Non200Response(WireMockRuntimeInfo wmRuntimeInfo) {
        String mockUrl = "/download/toolfetch.zip";
        Configuration configuration = buildConfiguration("http", mockUrl, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        stubWireMockToServe404Response(mockUrl);

        Optional<Path> path = webDownloadService.download(configuration, tool);

        assertThat(getAllServeEvents()).hasSize(1);
        verify(1, getRequestedFor(urlEqualTo(mockUrl)));
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN, "Download failed: received HTTP 404 response. Skipping " + tool.id());
    }

    @Test
    void testDownload_UnknownArchiveName(WireMockRuntimeInfo wmRuntimeInfo) {
        String mockUrl = "/download/";
        Configuration configuration = buildConfiguration("http", mockUrl, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        stubWireMockToServeZip(mockUrl, "attachment");

        Optional<Path> path = webDownloadService.download(configuration, tool);

        assertThat(getAllServeEvents()).hasSize(1);
        verify(1, getRequestedFor(urlEqualTo(mockUrl)));
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN, "Archive name could not be resolved. Skipping " + tool.id());
    }

    @Test
    void testDownload_IOException(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String mockUrl = "/download/toolfetch.zip";
        Configuration configuration = buildConfiguration("http", mockUrl, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        stubWireMockToServeZip(mockUrl);
        webDownloadService = spy(webDownloadService);
        doThrow(new IOException("I/O error occurred")).when(webDownloadService).createDirectories(configuration, tool);

        Optional<Path> path = webDownloadService.download(configuration, tool);

        assertThat(getAllServeEvents()).hasSize(1);
        verify(1, getRequestedFor(urlEqualTo(mockUrl)));
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN,
                "Download failed due to exception: \"java.io.IOException: I/O error occurred\". Skipping " + tool.id());
    }

    @Test
    void testDownload_IOExceptionFromResponseBody() throws Exception {
        Configuration configuration = buildConfiguration("http", "/download/toolfetch.zip", 8080);
        Tool tool = configuration.tools().getFirst();
        webDownloadService = spy(webDownloadService);
        HttpClient mockHttpClient = mock();
        HttpResponse<Object> mockHttpResponse = mock();
        HttpHeaders mockHttpHeaders = mock();
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.headers()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.firstValue(CONTENT_DISPOSITION_HEADER)).thenReturn(
                Optional.of("attachment; filename=toolfetch.zip"));
        when(mockHttpResponse.body()).thenReturn(new IOExceptionInputStream());

        Optional<Path> path;
        try (MockedStatic<ToolFetchHttpClient> toolfetchHttpClient = mockStatic()) {
            toolfetchHttpClient.when(() -> ToolFetchHttpClient.getInstance(any(Configuration.class))).thenReturn(mockHttpClient);
            path = webDownloadService.download(configuration, tool);
        }

        assertThat(getAllServeEvents()).isEmpty();
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN,
                "Download failed due to exception: \"java.io.IOException: Boom\". Skipping " + tool.id());
    }

    @Test
    void testDownload_InterruptedException(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String mockUrl = "/download/toolfetch.zip";
        Configuration configuration = buildConfiguration("http", mockUrl, wmRuntimeInfo.getHttpPort());
        Tool tool = configuration.tools().getFirst();
        stubWireMockToServeZip(mockUrl);
        webDownloadService = spy(webDownloadService);
        HttpClient mockHttpClient = mock();
        when(mockHttpClient.send(any(), any())).thenThrow(new InterruptedException("Interrupted error occurred"));

        Optional<Path> path;
        try (MockedStatic<ToolFetchHttpClient> toolfetchHttpClient = mockStatic()) {
            toolfetchHttpClient.when(() -> ToolFetchHttpClient.getInstance(any(Configuration.class))).thenReturn(mockHttpClient);
            path = webDownloadService.download(configuration, tool);
        }

        assertThat(getAllServeEvents()).isEmpty();
        assertThat(path).isEmpty();
        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(Level.WARN,
                "Download failed due to exception: \"java.lang.InterruptedException: Interrupted error occurred\". Skipping "
                        + tool.id());
    }

    private Configuration buildConfiguration(String scheme, String mockUrl, int httpPort) {
        return buildConfiguration(scheme, mockUrl, null, httpPort);
    }

    private Configuration buildConfiguration(String scheme, String mockUrl, @Nullable String version, int httpPort) {
        String id = "toolfetch";
        String url = scheme + "://localhost:" + httpPort + mockUrl;
        Tool tool = new Tool(id, url, version);
        return new Configuration(tempDir.toString(), List.of(tool));
    }

    private void stubWireMockToServeZip(String mockUrl) {
        stubWireMockToServeZip(mockUrl, "attachment; filename=toolfetch.zip");
    }

    private void stubWireMockToServeZip(String mockUrl, String contentDisposition) {
        byte[] archiveBytes = createZip(zaos -> addZipEntryFile(zaos, "test1.txt", "Install Test 1"));
        stubFor(get(mockUrl).willReturn(aResponse().withStatus(200)
                .withHeader(ContentTypes.CONTENT_TYPE, ContentTypes.OCTET_STREAM)
                .withHeader(ContentTypes.CONTENT_LENGTH, String.valueOf(archiveBytes.length))
                .withHeader("Content-Disposition", contentDisposition)
                .withBody(archiveBytes)));
    }

    private void stubWireMockToServe404Response(String mockUrl) {
        stubFor(get(mockUrl).willReturn(aResponse().withStatus(404)));
    }

    static class IOExceptionInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return read(new byte[0], 0, 0);
        }

        @Override
        public int read(byte @NonNull [] b, int off, int len) throws IOException {
            throw new IOException("Boom");
        }

    }

}

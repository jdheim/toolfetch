/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download;

import static com.jdheim.toolfetch.command.version.ToolFetchInfo.TITLE;
import static com.jdheim.toolfetch.command.version.ToolFetchInfo.VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.service.install.resolve.ArchiveNameResolver;
import com.jdheim.toolfetch.service.install.resolve.DestinationResolver;
import com.jdheim.toolfetch.service.install.resolve.FileNameResolver;
import com.jdheim.toolfetch.service.install.resolve.ToolDestinationResolver;
import com.jdheim.toolfetch.service.install.resolve.ToolUriTransformer;
import com.jdheim.toolfetch.service.install.resolve.UriTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDownloadService implements DownloadService {

    private static final Logger LOG = LoggerFactory.getLogger(WebDownloadService.class);

    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(15);

    private final HttpClient httpClient;

    private final FileNameResolver fileNameResolver;

    private final DestinationResolver destinationResolver;

    private final UriTransformer uriTransformer;

    public WebDownloadService() {
        httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(CONNECT_TIMEOUT).build();
        fileNameResolver = new ArchiveNameResolver();
        destinationResolver = new ToolDestinationResolver();
        uriTransformer = new ToolUriTransformer();
    }

    HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public Optional<Path> download(Configuration configuration, Tool tool) {
        LOG.info("> Install {}...", tool.id());
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        if (Files.exists(destinationPath)) {
            LOG.warn("Destination Path already exists: \"{}\". Skipping {}", destinationPath, tool.id());
            return Optional.empty();
        }
        URI toolUri = uriTransformer.transform(tool);
        if (toolUri == null) {
            LOG.warn("URI could not be resolved. Skipping {}", tool.id());
            return Optional.empty();
        }
        try {
            return download(configuration, tool, toolUri);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logException(e, tool);
            cleanup(configuration, tool);
            return Optional.empty();
        }
    }

    private Optional<Path> download(Configuration configuration, Tool tool, URI toolUri) throws IOException,
            InterruptedException {
        HttpResponse<InputStream> httpResponse = getHttpClient().send(buildHttpRequest(toolUri),
                HttpResponse.BodyHandlers.ofInputStream());
        int statusCode = httpResponse.statusCode();
        if (isNotOK(statusCode)) {
            LOG.warn("Download failed: received HTTP {} response. Skipping {}", statusCode, tool.id());
            return Optional.empty();
        }
        String archiveName = fileNameResolver.resolve(httpResponse);
        if (StringUtils.isEmpty(archiveName)) {
            LOG.warn("Archive name could not be resolved. Skipping {}", tool.id());
            return Optional.empty();
        }
        Path destinationPath = createDirectories(configuration, tool);
        Path archivePath = destinationPath.resolve(archiveName);
        downloadFromResponseBody(httpResponse, toolUri, archivePath);
        return Optional.of(archivePath);
    }

    private HttpRequest buildHttpRequest(URI toolUri) {
        return HttpRequest.newBuilder(toolUri)
                .timeout(REQUEST_TIMEOUT)
                .header(USER_AGENT_HEADER, "%s/%s".formatted(TITLE.value(), VERSION.value()))
                .GET()
                .build();
    }

    private boolean isNotOK(int statusCode) {
        return statusCode != HttpURLConnection.HTTP_OK;
    }

    Path createDirectories(Configuration configuration, Tool tool) throws IOException {
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        LOG.info("Create {}", destinationPath);
        Files.createDirectories(destinationPath);
        return destinationPath;
    }

    private void downloadFromResponseBody(HttpResponse<InputStream> httpResponse, URI toolUri, Path archivePath) throws
            IOException {
        LOG.info("Download {} to {}", toolUri, archivePath);
        try (InputStream inputStream = httpResponse.body()) {
            Files.copy(inputStream, archivePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void logException(Exception exception, Tool tool) {
        LOG.warn("Download failed due to exception: \"{}\". Skipping {}", exception.getMessage(), tool.id());
    }

    private void cleanup(Configuration configuration, Tool tool) {
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        FileUtils.deleteQuietly(destinationPath.toFile());
    }

}

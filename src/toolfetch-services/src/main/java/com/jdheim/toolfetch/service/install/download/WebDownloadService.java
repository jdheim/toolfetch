/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download;

import static com.jdheim.toolfetch.service.info.ToolFetchInfo.TITLE;
import static com.jdheim.toolfetch.service.info.ToolFetchInfo.VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.http.Http;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.install.download.http.ToolFetchHttpClient;
import com.jdheim.toolfetch.service.install.resolve.ArchiveNameResolver;
import com.jdheim.toolfetch.service.install.resolve.DestinationResolver;
import com.jdheim.toolfetch.service.install.resolve.FileNameResolver;
import com.jdheim.toolfetch.service.install.resolve.ToolDestinationResolver;
import com.jdheim.toolfetch.service.install.resolve.ToolUriTransformer;
import com.jdheim.toolfetch.service.install.resolve.UriTransformer;
import com.jdheim.toolfetch.service.log.LogHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class WebDownloadService implements DownloadService {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(WebDownloadService.class);

    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(15);

    private final FileNameResolver fileNameResolver;

    private final DestinationResolver destinationResolver;

    private final UriTransformer uriTransformer;

    public WebDownloadService() {
        fileNameResolver = new ArchiveNameResolver();
        destinationResolver = new ToolDestinationResolver();
        uriTransformer = new ToolUriTransformer();
    }

    @Override
    public Optional<Path> download(Configuration configuration, Tool tool) {
        LOGGER.log("download.install-tool", tool.id());
        URI toolUri = uriTransformer.transform(tool);
        if (toolUri == null) {
            LOGGER.log("download.uri-not-resolved", tool.id());
            return Optional.empty();
        }
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        boolean isUpdateMode = Files.exists(destinationPath);
        try {
            return download(configuration, tool, toolUri);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logException(e, tool);
            cleanup(configuration, tool, isUpdateMode);
            return Optional.empty();
        }
    }

    private Optional<Path> download(Configuration configuration, Tool tool, URI toolUri) throws IOException,
            InterruptedException {
        HttpResponse<InputStream> httpResponse = ToolFetchHttpClient.getInstance(configuration)
                .send(buildHttpRequest(configuration, toolUri), HttpResponse.BodyHandlers.ofInputStream());
        int statusCode = httpResponse.statusCode();
        if (isNotOK(statusCode)) {
            LOGGER.log("download.response-failed", statusCode, tool.id());
            return Optional.empty();
        }
        String archiveName = fileNameResolver.resolve(httpResponse);
        if (StringUtils.isEmpty(archiveName)) {
            LOGGER.log("download.archive-name-failed", tool.id());
            return Optional.empty();
        }
        Path destinationPath = createDirectories(configuration, tool);
        Path archivePath = destinationPath.resolve(archiveName);
        downloadFromResponseBody(httpResponse, toolUri, archivePath);
        return Optional.of(archivePath);
    }

    private HttpRequest buildHttpRequest(Configuration configuration, URI toolUri) {
        return HttpRequest.newBuilder(toolUri)
                .timeout(requestTimeout(configuration))
                .header(USER_AGENT_HEADER, "%s/%s".formatted(TITLE.value(), VERSION.value()))
                .GET()
                .build();
    }

    private Duration requestTimeout(Configuration configuration) {
        Http http = configuration.http();
        if (http != null) {
            Integer requestTimeout = http.requestTimeout();
            if (requestTimeout != null) {
                return Duration.ofSeconds(requestTimeout);
            }
        }
        return DEFAULT_REQUEST_TIMEOUT;
    }

    private boolean isNotOK(int statusCode) {
        return statusCode != HttpURLConnection.HTTP_OK;
    }

    Path createDirectories(Configuration configuration, Tool tool) throws IOException {
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        if (Files.exists(destinationPath)) {
            Path backupPath = destinationPath.resolveSibling(destinationPath.getFileName() + ".bak");
            if (Files.exists(backupPath)) {
                LOGGER.log("download.backup-exists", backupPath);
                FileUtils.deleteQuietly(backupPath.toFile());
            }
            LOGGER.log("download.destination-exists", destinationPath, backupPath);
            Files.move(destinationPath, backupPath, StandardCopyOption.ATOMIC_MOVE);
        }
        LOGGER.log("path.create", destinationPath);
        Files.createDirectories(destinationPath);
        return destinationPath;
    }

    private void downloadFromResponseBody(HttpResponse<InputStream> httpResponse, URI toolUri, Path archivePath) throws
            IOException {
        LOGGER.log("download.started", toolUri, archivePath);
        long startTime = System.nanoTime();
        try (InputStream inputStream = httpResponse.body()) {
            Files.copy(inputStream, archivePath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            String elapsedTime = LogHelper.elapsedTime(startTime);
            LOGGER.log("download.completed", elapsedTime);
        }
    }

    private void logException(Exception exception, Tool tool) {
        LOGGER.log("download.exception", exception.getClass().getName(), StringUtils.trimToEmpty(exception.getMessage()),
                tool.id());
    }

    private void cleanup(Configuration configuration, Tool tool, boolean isUpdateMode) {
        Path destinationPath = destinationResolver.resolve(configuration, tool);
        Path backupPath = destinationPath.resolveSibling(destinationPath.getFileName() + ".bak");
        boolean isInstallMode = !isUpdateMode;
        if (isInstallMode || Files.exists(backupPath)) {
            LOGGER.log("path.remove", destinationPath);
            FileUtils.deleteQuietly(destinationPath.toFile());
        }
    }

}

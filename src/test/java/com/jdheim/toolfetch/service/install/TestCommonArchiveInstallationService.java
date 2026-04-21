/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.createZip;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.service.install.extract.scan.ArchiveScanner;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.io.TempDir;

public class TestCommonArchiveInstallationService {

    protected @Nullable TestLogListAppender testLogListAppender;

    @TempDir
    protected Path tempDir;

    @Nullable InstallationService installationService;

    protected TestLogListAppender getTestLogListAppender() {
        if (testLogListAppender == null) {
            throw new AssertionError("TestLogListAppender not initialized");
        }
        return testLogListAppender;
    }

    protected void testInstall(WireMockRuntimeInfo wmRuntimeInfo, Consumer<ZipArchiveOutputStream> zipEntries,
            Consumer<Path> assertions) {
        byte[] archiveBytes = createZip(zipEntries);
        testInstall(wmRuntimeInfo, archiveBytes, assertions);
    }

    protected void testInstall(WireMockRuntimeInfo wmRuntimeInfo, byte[] archiveBytes, Consumer<Path> assertions) {
        testInstall(wmRuntimeInfo, "toolfetch.zip", archiveBytes, assertions);
    }

    protected void testInstall(WireMockRuntimeInfo wmRuntimeInfo, String archiveName, byte[] archiveBytes,
            Consumer<Path> assertions) {
        installationService = new ArchiveInstallationService();
        testLogListAppender = new TestLogListAppender();
        getTestLogListAppender().start(ArchiveInstallationService.class, WebDownloadService.class, ArchiveExtractService.class,
                ArchiveScanner.class);

        stubFor(get("/download/" + archiveName).willReturn(aResponse().withStatus(200)
                .withHeader(ContentTypes.CONTENT_TYPE, ContentTypes.OCTET_STREAM)
                .withHeader(ContentTypes.CONTENT_LENGTH, String.valueOf(archiveBytes.length))
                .withHeader("Content-Disposition", "attachment; filename=" + archiveName)
                .withBody(archiveBytes)));

        String id = "toolfetch";
        String url = "http://localhost:%d/download/%s".formatted(wmRuntimeInfo.getHttpPort(), archiveName);
        Tool tool = new Tool(id, null, url, null);
        Configuration configuration = new Configuration(tempDir.toString(), List.of(tool));
        installationService.install(configuration);

        assertThat(getAllServeEvents()).hasSize(1);
        verify(1, getRequestedFor(urlEqualTo("/download/" + archiveName)));

        Path destinationPath = tempDir.resolve(id);
        assertions.accept(destinationPath);
        Path archivePath = destinationPath.resolve(archiveName);
        assertThat(archivePath).doesNotExist();

        getTestLogListAppender().assertAnyMatch(Level.INFO, "=== Installing " + id + " ===");
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Creating " + destinationPath);
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Downloading %s to %s".formatted(url, destinationPath));
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Download completed in ");
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Scanning " + archivePath);
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Scan completed in ");
        if (Files.exists(destinationPath)) {
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Extracting %s to %s".formatted(archivePath, destinationPath));
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Extract completed in ");
            if (getTestLogListAppender().list.stream()
                    .noneMatch(line -> line.getFormattedMessage().startsWith("Extract failed due to exception")))
                getTestLogListAppender().assertAnyMatch(Level.INFO, "Removing " + archivePath);
        }
    }

    protected void fillDestinationPath(String id) throws IOException {
        Path destinationPath = tempDir.resolve(id);
        Files.createDirectory(destinationPath);
        assertThat(destinationPath).isDirectory();
        Path cleanupTest = destinationPath.resolve("cleanupTest.txt");
        Files.createFile(cleanupTest);
        Files.writeString(cleanupTest, "Cleanup Test");
        assertThat(cleanupTest).isRegularFile().hasContent("Cleanup Test");
    }

    protected void fillBackupPath(String id) throws IOException {
        Path backupPath = tempDir.resolve(id + ".bak");
        Files.createDirectory(backupPath);
        assertThat(backupPath).isDirectory();
        Path backupTest = backupPath.resolve("backupTest.txt");
        Files.createFile(backupTest);
        Files.writeString(backupTest, "Backup Test");
        assertThat(backupTest).isRegularFile().hasContent("Backup Test");
    }

}

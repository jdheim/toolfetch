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
        getTestLogListAppender().start(WebDownloadService.class, ArchiveExtractService.class);

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
        assertThat(destinationPath.resolve(archiveName)).doesNotExist();

        getTestLogListAppender().assertAnyMatch(Level.INFO, "> Install " + id);
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Create " + destinationPath);
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Download %s to %s".formatted(url, destinationPath));
        getTestLogListAppender().assertAnyMatch(Level.INFO,
                "Extract %s to %s".formatted(destinationPath.resolve(archiveName), destinationPath));
        if (Files.exists(destinationPath)) {
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Removing " + destinationPath.resolve(archiveName));
        }
    }

}

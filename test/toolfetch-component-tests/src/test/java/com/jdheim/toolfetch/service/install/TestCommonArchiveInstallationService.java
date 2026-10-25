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
import static com.jdheim.toolfetch.builder.TestChecksumsBuilder.checksums;
import static com.jdheim.toolfetch.builder.TestConfigurationBuilder.configuration;
import static com.jdheim.toolfetch.builder.TestToolBuilder.tool;
import static com.jdheim.toolfetch.step.archive.ArchiveSteps.createZip;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Consumer;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.logging.LogMarker;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

public class TestCommonArchiveInstallationService {

    @RegisterExtension
    final TestLogListAppenderSteps testLogListAppenderSteps = new TestLogListAppenderSteps();

    @TempDir
    protected Path tempDir;

    @Nullable InstallationService installationService;

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

        stubFor(get("/download/" + archiveName).willReturn(aResponse().withStatus(200)
                .withHeader(ContentTypes.CONTENT_TYPE, ContentTypes.OCTET_STREAM)
                .withHeader(ContentTypes.CONTENT_LENGTH, String.valueOf(archiveBytes.length))
                .withHeader("Content-Disposition", "attachment; filename=" + archiveName)
                .withBody(archiveBytes)));

        String id = "toolfetch";
        String url = "http://localhost:%d/download/%s".formatted(wmRuntimeInfo.getHttpPort(), archiveName);
        Path destinationPath = tempDir.resolve(id);
        Path archivePath = destinationPath.resolve(archiveName);
        String sha256;
        try {
            sha256 = HexFormat.of().formatHex(MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256).digest(archiveBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().id(id).url(url).checksums(checksums().sha256(sha256)))
                .build();
        installationService.install(configuration);

        assertThat(getAllServeEvents()).hasSize(1);
        verify(1, getRequestedFor(urlEqualTo("/download/" + archiveName)));

        assertions.accept(destinationPath);
        assertThat(archivePath).doesNotExist();

        testLogListAppenderSteps.assertAnyMatch(LogMarker.STEP.toString(), "=== Installing " + id + " ===");
        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Creating " + destinationPath);
        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(),
                "Downloading %s to %s".formatted(url, destinationPath));
        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Download completed in ");
        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Scanning " + archivePath);
        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Scan completed in ");
        if (Files.exists(destinationPath)) {
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(),
                    "Extracting %s to %s".formatted(archivePath, destinationPath));
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Extraction completed in ");
            if (testLogListAppenderSteps.messages().stream()
                    .noneMatch(line -> line.startsWith("Extraction failed")))
                testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Removing " + archivePath);
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

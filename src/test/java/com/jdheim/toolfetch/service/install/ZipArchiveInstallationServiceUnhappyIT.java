/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.addZipEntryDir;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.addZipEntryFile;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.createZip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/// Integration Tests for [ArchiveInstallationService]
@WireMockTest
class ZipArchiveInstallationServiceUnhappyIT extends TestCommonArchiveInstallationService {

    static Stream<Arguments> zipTestCases_ZipSlip() {
        return Stream.of(oneDirWithOneFile_ZipSlip(), oneDirWithOneFile_DirAsEntry_ZipSlip());
    }

    static Arguments oneDirWithOneFile_ZipSlip() {
        return Arguments.of("One Dir with One File - Zip Slip", (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
                    addZipEntryFile(zaos, "../test2.txt", "Install Test 2");
                }, (Consumer<Path>) ZipArchiveInstallationServiceUnhappyIT::pathAsserts_OneDirWithOneFile,
                new String[]{"toolfetch", "../test2.txt", "toolfetch/../test2.txt"});
    }

    static Arguments oneDirWithOneFile_DirAsEntry_ZipSlip() {
        return Arguments.of("One Dir with One File - Dir as Entry - ZipSlip", (Consumer<ZipArchiveOutputStream>) zaos -> {
                    addZipEntryDir(zaos, "test1/");
                    addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
                    addZipEntryDir(zaos, "../");
                    addZipEntryFile(zaos, "../test2.txt", "Install Test 2");
                }, (Consumer<Path>) ZipArchiveInstallationServiceUnhappyIT::pathAsserts_OneDirWithOneFile,
                new String[]{"toolfetch", "../test2.txt", "toolfetch/../test2.txt"});
    }

    private static void pathAsserts_OneDirWithOneFile(Path destinationPath) {
        assertThat(destinationPath).doesNotExist();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zipTestCases_ZipSlip")
    void testInstall_ZipSlip(String testCase, Consumer<ZipArchiveOutputStream> zipEntries, Consumer<Path> assertions,
            String[] logArgs, WireMockRuntimeInfo wmRuntimeInfo) {
        testInstall(wmRuntimeInfo, zipEntries, assertions);
        assertThat(logArgs).hasSize(3);
        Path destinationPath = tempDir.resolve(logArgs[0]);
        Path archivePath = destinationPath.resolve("toolfetch.zip");
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Extracting %s to %s".formatted(archivePath, destinationPath));
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Extract completed in ");
        getTestLogListAppender().assertAnyMatch(Level.WARN,
                "Extract failed due to exception: \"java.lang.UnsupportedOperationException: Detected Zip Slip vulnerability: \"%s\" + \"%s\" = \"%s\"\"".formatted(
                        destinationPath, logArgs[1], tempDir.resolve(logArgs[2]).normalize()));
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @Test
    void testInstall_FilesAtRoot_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) {
        String id = "toolfetch";

        List<String> files = List.of("test1.txt", "test2.txt", "test3.txt");
        testInstall_PasswordProtected(1, files, wmRuntimeInfo);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).doesNotExist();
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @Test
    void testInstall_Strip_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) {
        String id = "toolfetch";

        List<String> files = List.of("test1/test11/test111/test1111/test11111/test11111.txt",
                "test1/test11/test111/test1111/test11111/test111111/test111111.txt", "test1/test11/test222/test222.txt",
                "test1/test11/test333/test3333/test3333-1.txt", "test1/test11/test333/test3333/test3333-2.txt",
                "test1/test11/test333/test3333/test3333-3.txt");
        testInstall_PasswordProtected(2, files, wmRuntimeInfo);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).doesNotExist();
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @Test
    void testInstall_FileAtRootNoStrip_PasswordProtected(WireMockRuntimeInfo wmRuntimeInfo) {
        String id = "toolfetch";

        List<String> files = List.of("test1/test11/test111/test1111/test11111/test11111.txt",
                "test1/test11/test111/test1111/test11111/test111111/test111111.txt", "test1/test11/test222/test222.txt",
                "test1/test11/test333/test3333/test3333-1.txt", "test1/test11/test333/test3333/test3333-2.txt",
                "test1/test11/test333/test3333/test3333-3.txt", "test4.txt");
        testInstall_PasswordProtected(3, files, wmRuntimeInfo);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInstall_Cleanup_ZipSlip(boolean backupPathExists, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String id = "toolfetch";
        fillDestinationPath(id);
        if (backupPathExists) {
            fillBackupPath(id);
        }
        Consumer<ZipArchiveOutputStream> zipEntries = zaos -> {
            addZipEntryFile(zaos, "test1/test1.txt", "Install Test 1");
            addZipEntryFile(zaos, "../test2.txt", "Install Test 2");
        };

        testInstall(wmRuntimeInfo, zipEntries, destinationPath -> {
            assertThat(destinationPath).isNotEmptyDirectory();
            Path cleanupTest = Path.of("cleanupTest.txt");
            assertThat(destinationPath.resolve(cleanupTest)).isRegularFile().hasContent("Cleanup Test");
            Path backupPath = tempDir.resolve(id + ".bak");
            assertThat(backupPath).doesNotExist();
            if (backupPathExists) {
                getTestLogListAppender().assertAnyMatch(Level.INFO,
                        "Backup Path already exists: %s. Removing".formatted(backupPath));
            }
            getTestLogListAppender().assertAnyMatch(Level.INFO,
                    "Destination Path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
            Path archivePath = destinationPath.resolve("toolfetch.zip");
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Extracting %s to %s".formatted(archivePath, destinationPath));
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Extract completed in ");
            getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Extract failed due to exception: \"java.lang.UnsupportedOperationException: Detected Zip Slip vulnerability: \"%s\" + \"../test2.txt\" = \"%s\"\"".formatted(
                            destinationPath, tempDir.resolve(id + "/../test2.txt").normalize()));
            getTestLogListAppender().assertNoMatch(Level.INFO, "Removing " + destinationPath);
            getTestLogListAppender().assertNoMatch(Level.INFO, "Removing " + backupPath);
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
        });
    }

    /// Password-protected ZIP file with password: "toolfetch"
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInstall_Cleanup_PasswordProtected(boolean backupPathExists, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String id = "toolfetch";
        fillDestinationPath(id);
        if (backupPathExists) {
            fillBackupPath(id);
        }

        List<String> files = List.of("test1.txt", "test2.txt", "test3.txt");
        testInstall_PasswordProtected(1, files, wmRuntimeInfo);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).isNotEmptyDirectory();
        Path cleanupTest = Path.of("cleanupTest.txt");
        assertThat(destinationPath.resolve(cleanupTest)).isRegularFile().hasContent("Cleanup Test");
        Path backupPath = tempDir.resolve(id + ".bak");
        assertThat(backupPath).doesNotExist();
        if (backupPathExists) {
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Backup Path already exists: %s. Removing".formatted(backupPath));
        }
        getTestLogListAppender().assertAnyMatch(Level.INFO,
                "Destination Path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
        getTestLogListAppender().assertNoMatch(Level.INFO, "Removing " + backupPath);
        getTestLogListAppender().assertAnyMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
    }

    private void testInstall_PasswordProtected(int index, List<String> files, WireMockRuntimeInfo wmRuntimeInfo) {
        byte[] archiveBytes = ArchiveUtils.readTestFile("/archive/zip/sample%d-password.zip".formatted(index));
        Path archivePath = tempDir.resolve("toolfetch/toolfetch.zip");

        testInstall(wmRuntimeInfo, archiveBytes, destinationPath -> {
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Extracting %s to %s".formatted(archivePath, destinationPath));
            files.forEach(file -> getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Couldn't read archive entry \"%s\". Skipping".formatted(file)));
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Extract completed in ");
            getTestLogListAppender().assertAnyMatch(Level.INFO, "Removing " + archivePath);
            getTestLogListAppender().assertAnyMatch(Level.WARN,
                    "Nothing has been extracted. Removing " + tempDir.resolve("toolfetch"));
        });
    }

    @Test
    void testInstall_Cleanup_RevertFailed(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String id = "toolfetch";
        String mockUrl = "/download/%s.zip".formatted(id);
        Configuration configuration = buildConfiguration(wmRuntimeInfo, id, mockUrl);
        fillBackupPath(id);

        Path destinationPath = tempDir.resolve(id);
        Path backupPath = tempDir.resolve(id + ".bak");
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.move(any(), any(), any())).thenThrow(new IOException("Boom"));
            files.when(() -> Files.exists(destinationPath)).thenReturn(false);
            files.when(() -> Files.exists(backupPath)).thenReturn(true);
            ArchiveInstallationService installationService = new ArchiveInstallationService();
            testLogListAppender = new TestLogListAppender();
            getTestLogListAppender().start(ArchiveInstallationService.class);
            installationService.cleanup(configuration, configuration.tools().getFirst());
        }

        assertThat(destinationPath).doesNotExist();
        assertThat(backupPath).isNotEmptyDirectory();
        Path backupTest = Path.of("backupTest.txt");
        assertThat(backupPath.resolve(backupTest)).isRegularFile().hasContent("Backup Test");
        testLogListAppender.assertNoMatch(Level.INFO, "Removing " + backupPath);
        testLogListAppender.assertAnyMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
        testLogListAppender.assertAnyMatch(Level.WARN, "Revert failed due to exception: \"java.io.IOException: Boom\"");
    }

    @Test
    void testInstall_Cleanup_InterruptedException(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String id = "toolfetch";
        String mockUrl = "/download/%s.zip".formatted(id);
        Configuration configuration = buildConfiguration(wmRuntimeInfo, id, mockUrl);
        stubWireMockToServeZip(mockUrl);
        HttpClient mockHttpClient = mock();
        when(mockHttpClient.send(any(), any())).thenThrow(new InterruptedException("Interrupted error occurred"));
        WebDownloadService webDownloadService = new WebDownloadService(mockHttpClient);
        fillDestinationPath(id);

        ArchiveInstallationService installationService = spy(new ArchiveInstallationService());
        doReturn(webDownloadService).when(installationService).downloadService();
        testLogListAppender = new TestLogListAppender();
        getTestLogListAppender().start(ArchiveInstallationService.class, WebDownloadService.class, ArchiveExtractService.class);
        installationService.install(configuration);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).isNotEmptyDirectory();
        Path cleanupTest = Path.of("cleanupTest.txt");
        assertThat(destinationPath.resolve(cleanupTest)).isRegularFile().hasContent("Cleanup Test");
        Path backupPath = tempDir.resolve(id + ".bak");
        assertThat(backupPath).doesNotExist();
        testLogListAppender.assertAnyMatch(Level.WARN,
                "Download failed due to exception: \"Interrupted error occurred\". Skipping " + id);
        testLogListAppender.assertNoMatch(Level.INFO, "Removing " + destinationPath);
        testLogListAppender.assertNoMatch(Level.INFO, "Removing " + backupPath);
        testLogListAppender.assertNoMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
    }

    @Test
    void testInstall_Cleanup_DownloadFailure(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        String id = "toolfetch";
        String mockUrl = "/download/%s.zip".formatted(id);
        Configuration configuration = buildConfiguration(wmRuntimeInfo, id, mockUrl);
        stubWireMockToServe404Response(mockUrl);
        fillDestinationPath(id);

        installationService = new ArchiveInstallationService();
        testLogListAppender = new TestLogListAppender();
        getTestLogListAppender().start(ArchiveInstallationService.class, WebDownloadService.class, ArchiveExtractService.class);
        installationService.install(configuration);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).isNotEmptyDirectory();
        Path cleanupTest = Path.of("cleanupTest.txt");
        assertThat(destinationPath.resolve(cleanupTest)).isRegularFile().hasContent("Cleanup Test");
        Path backupPath = tempDir.resolve(id + ".bak");
        assertThat(backupPath).doesNotExist();
        testLogListAppender.assertAnyMatch(Level.WARN, "Download failed: received HTTP 404 response. Skipping " + id);
        testLogListAppender.assertNoMatch(Level.INFO, "Removing " + destinationPath);
        testLogListAppender.assertNoMatch(Level.INFO, "Removing " + backupPath);
        testLogListAppender.assertNoMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInstall_Cleanup_IOExceptionFromResponseBody(boolean backupPathExists, WireMockRuntimeInfo wmRuntimeInfo) throws
            Exception {
        String id = "toolfetch";
        String mockUrl = "/download/%s.zip".formatted(id);
        Configuration configuration = buildConfiguration(wmRuntimeInfo, id, mockUrl);
        stubWireMockToServeZip(mockUrl);
        HttpClient mockHttpClient = mock();
        HttpResponse<Object> mockHttpResponse = mock();
        HttpHeaders mockHttpHeaders = mock();
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.headers()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.firstValue("Content-Disposition")).thenReturn(Optional.of("attachment; filename=toolfetch.zip"));
        when(mockHttpResponse.body()).thenReturn(new IOExceptionInputStream());
        WebDownloadService webDownloadService = new WebDownloadService(mockHttpClient);
        fillDestinationPath(id);
        if (backupPathExists) {
            fillBackupPath(id);
        }

        ArchiveInstallationService installationService = spy(new ArchiveInstallationService());
        doReturn(webDownloadService).when(installationService).downloadService();
        testLogListAppender = new TestLogListAppender();
        getTestLogListAppender().start(ArchiveInstallationService.class, WebDownloadService.class, ArchiveExtractService.class);
        installationService.install(configuration);

        Path destinationPath = tempDir.resolve(id);
        assertThat(destinationPath).isNotEmptyDirectory();
        Path cleanupTest = Path.of("cleanupTest.txt");
        assertThat(destinationPath.resolve(cleanupTest)).isRegularFile().hasContent("Cleanup Test");
        Path backupPath = tempDir.resolve(id + ".bak");
        assertThat(backupPath).doesNotExist();
        if (backupPathExists) {
            testLogListAppender.assertAnyMatch(Level.INFO, "Backup Path already exists: %s. Removing".formatted(backupPath));
        }
        testLogListAppender.assertAnyMatch(Level.INFO,
                "Destination Path already exists: %s. Moving to %s".formatted(destinationPath, backupPath));
        testLogListAppender.assertAnyMatch(Level.INFO, "Creating " + destinationPath);
        testLogListAppender.assertAnyMatch(Level.INFO,
                "Downloading http://localhost:%s%s to %s".formatted(wmRuntimeInfo.getHttpPort(), mockUrl,
                        destinationPath.resolve(id + ".zip")));
        testLogListAppender.assertAnyMatch(Level.INFO, "Download completed in ");
        testLogListAppender.assertAnyMatch(Level.WARN, "Download failed due to exception: \"Boom\". Skipping " + id);
        testLogListAppender.assertAnyMatch(Level.INFO, "Removing " + destinationPath);
        testLogListAppender.assertNoMatch(Level.INFO, "Removing " + backupPath);
        testLogListAppender.assertAnyMatch(Level.INFO, "Reverting %s to %s".formatted(backupPath, destinationPath));
    }

    private Configuration buildConfiguration(WireMockRuntimeInfo wmRuntimeInfo, String id, String mockUrl) {
        String url = "http" + "://localhost:" + wmRuntimeInfo.getHttpPort() + mockUrl;
        Tool tool = new Tool(id, url);
        return new Configuration(tempDir.toString(), List.of(tool));
    }

    private void stubWireMockToServe404Response(String mockUrl) {
        stubFor(get(mockUrl).willReturn(aResponse().withStatus(404)));
    }

    private void stubWireMockToServeZip(String mockUrl) {
        byte[] archiveBytes = createZip(zaos -> addZipEntryFile(zaos, "test1.txt", "Install Test 1"));
        stubFor(get(mockUrl).willReturn(aResponse().withStatus(200)
                .withHeader(ContentTypes.CONTENT_TYPE, ContentTypes.OCTET_STREAM)
                .withHeader(ContentTypes.CONTENT_LENGTH, String.valueOf(archiveBytes.length))
                .withHeader("Content-Disposition", "attachment; filename=toolfetch.zip")
                .withBody(archiveBytes)));
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

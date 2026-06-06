/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.addZipEntryFile;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.createZip;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.model.tool.checksums.Checksums;
import com.jdheim.toolfetch.service.install.resolve.ToolUriTransformer;
import com.jdheim.toolfetch.service.install.resolve.UriTransformer;
import com.jdheim.toolfetch.util.archive.ArchiveUtils;
import com.jdheim.toolfetch.util.assertion.AssertionUtils;
import com.jdheim.toolfetch.util.config.ConfigurationDumper;
import com.jdheim.toolfetch.util.wiremock.WireMockStubber;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [ToolFetch] -c/--config
@WireMockTest
class ToolFetchConfigHappySmokeTest extends ToolFetchTestBase {

    static final Path ARCHIVE_DIR = Path.of("target", "test-classes", "archive");

    static final List<String> EXCLUDED_ARCHIVERS = List.of(ArchiveStreamFactory.AR,
            ArchiveStreamFactory.ARJ,
            ArchiveStreamFactory.CPIO,
            ArchiveStreamFactory.DUMP);

    UriTransformer uriTransformer;

    @TempDir
    Path tempDir;

    Path configPath;

    @BeforeEach
    void setUp() {
        uriTransformer = new ToolUriTransformer();
        configPath = tempDir.resolve("toolfetch.yaml");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testSingleBinaryFile(String option, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        Configuration config = createConfigWithOneToolAsBinaryFile(wmRuntimeInfo.getHttpPort());
        ConfigurationDumper.saveToConfigFile(config, configPath);

        ExecResult execResult = execute(option, configPath.toString());

        assertNoErrorNoWarn(execResult);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
        assertThat(getAllServeEvents()).hasSize(config.tools().size());
        config.tools().forEach(tool -> {
            Path destinationPath = Path.of(config.destination()).resolve(tool.id());
            String binaryFileName = tool.id() + "_" + tool.version() + ".AppImage";
            Path binaryFilePath = destinationPath.resolve(binaryFileName);
            assertLogs(execResult, tool, destinationPath, binaryFilePath);
            assertAnyMatch(execResult, "[%s] Detected binary file. Skipping archive extraction".formatted(Level.INFO));
            assertAnyMatch(execResult, "[%s] Setting %s as executable".formatted(Level.INFO, binaryFilePath));
            Path renamedBinaryFilePath = destinationPath.resolve(tool.id() + ".AppImage");
            assertAnyMatch(execResult, "[%s] Renaming %s to %s".formatted(Level.INFO, binaryFilePath, renamedBinaryFilePath));
            verify(1, getRequestedFor(urlEqualTo("/download/%s/%s".formatted(tool.version(), binaryFileName))));
            assertThat(destinationPath).isDirectory();
            assertThat(binaryFilePath).doesNotExist();
            assertThat(renamedBinaryFilePath).isExecutable().hasContent("Hello ToolFetch!\nThis is a dummy AppImage.");
        });
    }

    private Configuration createConfigWithOneToolAsBinaryFile(int httpPort) {
        String id = "toolfetch";
        String version = "1.0.0";
        String url = "http://localhost:%d/download/${version}/%s_${version}.AppImage".formatted(httpPort, id);
        byte[] binaryFileBytes = createBinaryFileBytes();
        String sha256 = ArchiveUtils.computeMessageDigest(binaryFileBytes, MessageDigestAlgorithms.SHA_256);
        String sha384 = ArchiveUtils.computeMessageDigest(binaryFileBytes, MessageDigestAlgorithms.SHA_384);
        String sha512 = ArchiveUtils.computeMessageDigest(binaryFileBytes, MessageDigestAlgorithms.SHA_512);
        Map<String, String> checksumValues = Map.of("sha256", sha256, "sha384", sha384, "sha512", sha512);
        Checksums checksums = new Checksums(checksumValues);
        Tool tool = new Tool(id, url, version, null, checksums);
        Path binaryFileName = Path.of(getArchiveName(tool));
        WireMockStubber.stubFor(version, binaryFileName, binaryFileBytes, "text/plain; charset=UTF-8");
        List<Tool> tools = List.of(tool);
        return new Configuration(tempDir.toString(), tools);
    }

    private byte[] createBinaryFileBytes() {
        return "Hello ToolFetch!\nThis is a dummy AppImage.".getBytes(StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testSingleArchive(String option, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        Configuration config = createConfigWithOneToolAsArchive(wmRuntimeInfo.getHttpPort());
        ConfigurationDumper.saveToConfigFile(config, configPath);

        ExecResult execResult = execute(option, configPath.toString());

        assertNoErrorNoWarn(execResult);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
        assertThat(getAllServeEvents()).hasSize(config.tools().size());
        config.tools().forEach(tool -> {
            Path destinationPath = Path.of(config.destination()).resolve(tool.id());
            String archiveName = tool.id() + ".zip";
            Path archivePath = destinationPath.resolve(archiveName);
            assertLogs(execResult, tool, destinationPath, archivePath);
            verify(1, getRequestedFor(urlEqualTo("/download/%s/%s".formatted(tool.version(), archiveName))));
            assertSingleArchive(destinationPath);
            assertAnyMatch(execResult, "[%s] Removing %s".formatted(Level.INFO, archivePath));
            assertThat(archivePath).doesNotExist();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testAllArchives(String option, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        Configuration config = createConfigFromArchiveDir(wmRuntimeInfo.getHttpPort());
        ConfigurationDumper.saveToConfigFile(config, configPath);

        ExecResult execResult = execute(option, configPath.toString());

        assertNoError(execResult);
        assertThat(execResult.exitCode()).isEqualTo(CommandLine.ExitCode.OK);
        assertThat(getAllServeEvents()).hasSize(config.tools().size());
        config.tools().forEach(tool -> {
            Path destinationPath = Path.of(config.destination()).resolve(tool.id());
            String archiveName = getArchiveName(tool);
            Path archivePath = destinationPath.resolve(archiveName);
            assertLogs(execResult, tool, destinationPath, archivePath);
            verify(1, getRequestedFor(urlEqualTo("/download/" + archiveName)));
            if (isExcluded(archiveName)) {
                assertAnyMatch(execResult,
                        ("[%s] Extract failed due to exception: \"org.apache.commons.compress.archivers.ArchiveException: "
                                + "No Archiver found for the stream signature\". Skipping %s").formatted(Level.WARN, tool.id()));
            } else if (tool.url().contains(".7z")) {
                assertAnyMatch(execResult,
                        ("[%s] Extract failed due to exception: \"org.apache.commons.compress.archivers.StreamingNotSupportedException: "
                                + "The 7z doesn't support streaming.\". Skipping %s").formatted(Level.WARN, tool.id()));
            } else {
                assertAnyMatch(execResult, "[%s] Extracting %s to %s".formatted(Level.INFO, archivePath, destinationPath));
                assertAnyMatch(execResult, "[%s] Extract completed in ".formatted(Level.INFO));
                assertAnyMatch(execResult, "[%s] Removing %s".formatted(Level.INFO, archivePath));
                if (tool.url().contains("-password")) {
                    assertAnyMatch(execResult,
                            "[%s] Nothing has been extracted. Removing %s".formatted(Level.WARN, destinationPath));
                } else if (tool.id().contains("sample1")) {
                    AssertionUtils.assertSample1Archive(destinationPath);
                } else if (tool.id().contains("sample2")) {
                    AssertionUtils.assertSample2Archive(destinationPath);
                } else if (tool.id().contains("sample3")) {
                    AssertionUtils.assertSample3Archive(destinationPath);
                }
            }
            assertThat(archivePath).doesNotExist();
        });
    }

    private Configuration createConfigWithOneToolAsArchive(int httpPort) {
        String id = "toolfetch";
        String version = "1.0.0";
        String url = "http://localhost:%d/download/${version}/%s.zip".formatted(httpPort, id);
        byte[] archiveBytes = createArchiveBytes();
        String sha256 = ArchiveUtils.computeMessageDigest(archiveBytes, MessageDigestAlgorithms.SHA_256);
        String sha384 = ArchiveUtils.computeMessageDigest(archiveBytes, MessageDigestAlgorithms.SHA_384);
        String sha512 = ArchiveUtils.computeMessageDigest(archiveBytes, MessageDigestAlgorithms.SHA_512);
        Map<String, String> checksumValues = Map.of("sha256", sha256, "sha384", sha384, "sha512", sha512);
        Checksums checksums = new Checksums(checksumValues);
        Tool tool = new Tool(id, url, version, null, checksums);
        Path archiveName = Path.of(getArchiveName(tool));
        WireMockStubber.stubFor(version, archiveName, archiveBytes);
        List<Tool> tools = List.of(tool);
        return new Configuration(tempDir.toString(), tools);
    }

    private String getArchiveName(Tool tool) {
        return Optional.ofNullable(uriTransformer.transform(tool))
                .map(URI::getRawPath)
                .filter(path -> !path.endsWith("/"))
                .map(path -> path.substring(path.lastIndexOf('/') + 1))
                .orElseThrow();
    }

    private byte[] createArchiveBytes() {
        return createZip(zaos -> {
            for (int i = 1; i <= 100; i++) {
                addZipEntryFile(zaos, "test1/test2/test3/test4/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test%d3.txt".formatted(i), "Install Test %d3".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test4/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test%d2.txt".formatted(i), "Install Test %d2".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test4/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test3/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
                addZipEntryFile(zaos, "test1/test2/test%d1.txt".formatted(i), "Install Test %d1".formatted(i));
            }
        });
    }

    private Configuration createConfigFromArchiveDir(int httpPort) throws IOException {
        List<Tool> tools;
        try (Stream<Path> archivePaths = Files.walk(ARCHIVE_DIR)) {
            tools = archivePaths.filter(Files::isRegularFile).map(archivePath -> toTool(archivePath, httpPort)).toList();
        }
        assertThat(tools).isNotEmpty();
        return new Configuration(tempDir.toString(), tools);
    }

    private Tool toTool(Path archivePath, int httpPort) {
        Path archiveName = archivePath.getFileName();
        assertThat(archiveName).isNotNull();
        WireMockStubber.stubFor(archiveName, ArchiveUtils.readTestFile(archivePath));
        String id = archiveName.toString().replace('.', '-');
        String url = "http://localhost:%d/download/%s".formatted(httpPort, archiveName);
        String sha256 = ArchiveUtils.computeMessageDigest(archivePath, MessageDigestAlgorithms.SHA_256);
        String sha384 = ArchiveUtils.computeMessageDigest(archivePath, MessageDigestAlgorithms.SHA_384);
        String sha512 = ArchiveUtils.computeMessageDigest(archivePath, MessageDigestAlgorithms.SHA_512);
        Map<String, String> checksumValues = Map.of("sha256", sha256, "sha384", sha384, "sha512", sha512);
        Checksums checksums = new Checksums(checksumValues);
        return new Tool(id, url, null, null, checksums);
    }

    private boolean isExcluded(String archiveName) {
        return EXCLUDED_ARCHIVERS.stream().map(excludedArchiver -> "." + excludedArchiver).anyMatch(archiveName::contains);
    }

    private void assertLogs(ExecResult execResult, Tool tool, Path destinationPath, Path archivePath) {
        assertAnyMatch(execResult, "[%s] === Installing %s ===".formatted(Level.INFO, tool.id()));
        assertAnyMatch(execResult, "[%s] Creating %s".formatted(Level.INFO, destinationPath));
        assertAnyMatch(execResult,
                "[%s] Downloading %s to %s".formatted(Level.INFO, uriTransformer.transform(tool), destinationPath));
        assertAnyMatch(execResult, "[%s] Download completed in ".formatted(Level.INFO));
        assertAnyMatch(execResult, "[%s] Scanning %s".formatted(Level.INFO, archivePath));
        assertAnyMatch(execResult, "[%s] Scan completed in ".formatted(Level.INFO));
    }

    private void assertSingleArchive(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).doesNotExist();
        assertThat(destinationPath.resolve("test3")).isDirectory();
        assertThat(destinationPath.resolve("test3/test4")).isDirectory();
        for (int i = 1; i <= 100; i++) {
            assertThat(destinationPath.resolve("test%d1.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d1".formatted(i));
            assertThat(destinationPath.resolve("test%d2.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d2".formatted(i));
            assertThat(destinationPath.resolve("test%d3.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d3".formatted(i));
            assertThat(destinationPath.resolve("test3/test%d1.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d1".formatted(i));
            assertThat(destinationPath.resolve("test3/test%d2.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d2".formatted(i));
            assertThat(destinationPath.resolve("test3/test%d3.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d3".formatted(i));
            assertThat(destinationPath.resolve("test3/test4/test%d1.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d1".formatted(i));
            assertThat(destinationPath.resolve("test3/test4/test%d2.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d2".formatted(i));
            assertThat(destinationPath.resolve("test3/test4/test%d3.txt".formatted(i))).isRegularFile()
                    .hasContent("Install Test %d3".formatted(i));
        }
    }

}

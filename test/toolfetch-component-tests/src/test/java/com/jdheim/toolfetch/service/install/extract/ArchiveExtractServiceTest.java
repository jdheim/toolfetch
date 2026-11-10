/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract;

import static com.jdheim.toolfetch.builder.TestConfigurationBuilder.configuration;
import static com.jdheim.toolfetch.builder.TestToolBuilder.tool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.exception.SkippedArchiveException;
import com.jdheim.toolfetch.service.install.extract.model.ArchiveWithCompressorInputStream;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/// Component Tests for [ArchiveExtractService]
class ArchiveExtractServiceTest {

    private static final int OWNER_RWX_GROUP_RX_PERMISSIONS = 0750;

    private static final int OWNER_READ_ONLY_PERMISSIONS = 0400;

    ArchiveExtractService archiveExtractService;

    @RegisterExtension
    final TestLogListAppenderSteps testLogListAppenderSteps = new TestLogListAppenderSteps();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        archiveExtractService = new ArchiveExtractService();
    }

    @Test
    void testExtract_RestoresZipExecutablePermissions() throws IOException {
        Path executable = extractZipFileWithMode(UnixStat.FILE_FLAG | OWNER_RWX_GROUP_RX_PERMISSIONS);

        assertThat(Files.getPosixFilePermissions(executable))
                .contains(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE);
    }

    @Test
    void testExtract_KeepsDefaultPermissionsForNonExecutableZipFile() throws IOException {
        Path reference = Files.writeString(tempDir.resolve("default.txt"), "default permissions");

        Path extracted = extractZipFileWithMode(UnixStat.FILE_FLAG | OWNER_READ_ONLY_PERMISSIONS);

        assertThat(Files.getPosixFilePermissions(extracted)).isEqualTo(Files.getPosixFilePermissions(reference));
    }

    private Path extractZipFileWithMode(int mode) throws IOException {
        assumeTrue(tempDir.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Path archive = tempDir.resolve("toolfetch.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry entry = new ZipArchiveEntry("tool");
            entry.setUnixMode(mode);
            out.putArchiveEntry(entry);
            out.write(1);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/toolfetch.zip"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        return tempDir.resolve("tool");
    }

    @Test
    void testExtract_PreservesTarSymbolicLink() throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        testExtract_PreservesTarSymbolicLink(archive, new TarArchiveOutputStream(Files.newOutputStream(archive)));
    }

    @Test
    void testExtract_PreservesTarGzipSymbolicLink() throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar.gz");
        testExtract_PreservesTarSymbolicLink(archive,
                new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(archive))));
    }

    private void testExtract_PreservesTarSymbolicLink(Path archive, TarArchiveOutputStream out) throws IOException {
        byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
        try (out) {
            TarArchiveEntry fileEntry = new TarArchiveEntry("toolfetch/bin/tool-real");
            fileEntry.setSize(content.length);
            out.putArchiveEntry(fileEntry);
            out.write(content);
            out.closeArchiveEntry();

            TarArchiveEntry linkEntry = new TarArchiveEntry("toolfetch/bin/tool", TarConstants.LF_SYMLINK);
            linkEntry.setLinkName("tool-real");
            out.putArchiveEntry(linkEntry);
            out.closeArchiveEntry();
        }
        testExtract_PreservesSymbolicLink(archive, content);
    }

    @Test
    void testExtract_PreservesZipSymbolicLink() throws IOException {
        Path archive = tempDir.resolve("toolfetch.zip");
        testExtract_PreservesZipSymbolicLink(archive, new ZipArchiveOutputStream(Files.newOutputStream(archive)));
    }

    @Test
    void testExtract_PreservesJarSymbolicLink() throws IOException {
        Path archive = tempDir.resolve("toolfetch.jar");
        testExtract_PreservesZipSymbolicLink(archive, new JarArchiveOutputStream(Files.newOutputStream(archive)));
    }

    private void testExtract_PreservesZipSymbolicLink(Path archive, ZipArchiveOutputStream out) throws IOException {
        byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
        try (out) {
            ZipArchiveEntry fileEntry = new ZipArchiveEntry("toolfetch/bin/tool-real");
            out.putArchiveEntry(fileEntry);
            out.write(content);
            out.closeArchiveEntry();

            ZipArchiveEntry linkEntry = new ZipArchiveEntry("toolfetch/bin/tool");
            linkEntry.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
            out.putArchiveEntry(linkEntry);
            out.write("tool-real".getBytes(StandardCharsets.UTF_8));
            out.closeArchiveEntry();
        }
        try (MockedStatic<Files> files = mockStatic(CALLS_REAL_METHODS)) {
            testExtract_PreservesSymbolicLink(archive, content);
            files.verify(() -> {
                try (OutputStream _ = Files.newOutputStream(tempDir.resolve("tool"))) {
                    // Mockito verifies the call without opening a stream.
                }
            }, never());
        }
    }

    private void testExtract_PreservesSymbolicLink(Path archive, byte[] content) throws IOException {
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/" + archive.getFileName()))
                .build();
        Tool tool = configuration.tools().getFirst();

        archiveExtractService.extract(configuration, tool, archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("tool-real")).hasBinaryContent(content);
        Path link = tempDir.resolve("tool");
        assertThat(link).isSymbolicLink();
        assertThat(Files.readSymbolicLink(link)).isEqualTo(Path.of("tool-real"));
        assertThat(link).hasBinaryContent(content);
    }

    @Test
    void testExtract_RejectsTarSymbolicLinkOutsideDestination() throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            TarArchiveEntry linkEntry = new TarArchiveEntry("tool", TarConstants.LF_SYMLINK);
            linkEntry.setLinkName("../outside.txt");
            out.putArchiveEntry(linkEntry);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.tar"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        assertThat(outside).hasContent("unchanged");
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @Test
    void testExtract_RejectsZipSymbolicLinkOutsideDestination() throws IOException {
        testExtract_RejectsZipSymbolicLinkOutsideDestination(false);
    }

    @Test
    void testExtract_RejectsJarSymbolicLinkOutsideDestination() throws IOException {
        testExtract_RejectsZipSymbolicLinkOutsideDestination(true);
    }

    private void testExtract_RejectsZipSymbolicLinkOutsideDestination(boolean jar) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        String archiveName = jar ? "toolfetch.jar" : "toolfetch.zip";
        Path archive = destination.resolve(archiveName);
        try (ZipArchiveOutputStream out = jar
                ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry linkEntry = new ZipArchiveEntry("tool");
            linkEntry.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
            out.putArchiveEntry(linkEntry);
            out.write("../outside.txt".getBytes(StandardCharsets.UTF_8));
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/" + archiveName))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        assertThat(outside).hasContent("unchanged");
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @Test
    void testExtract_RejectsOversizedZipSymbolicLinkTarget() throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry linkEntry = new ZipArchiveEntry("tool");
            linkEntry.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
            out.putArchiveEntry(linkEntry);
            out.write("a".repeat(4097).getBytes(StandardCharsets.UTF_8));
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.zip"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({"a, 4097", "é, 2049"})
    void testExtract_RejectsOversizedTarSymbolicLinkTarget(String character, int repetitions) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            TarArchiveEntry linkEntry = new TarArchiveEntry("tool", TarConstants.LF_SYMLINK);
            linkEntry.setLinkName(character.repeat(repetitions));
            out.putArchiveEntry(linkEntry);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.tar"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
        assertThat(testLogListAppenderSteps.messages())
                .anyMatch(message -> message.contains("symbolic link target exceeds 4096 bytes"));
    }

    @Test
    void testExtract_PreservesTarDirectorySymbolicLinkBeforeTarget() throws IOException {
        testExtract_PreservesTarDirectorySymbolicLink("lib", "toolfetch/lib/tool");
    }

    @Test
    void testExtract_PreservesTarSymbolicLinkToCurrentDirectory() throws IOException {
        testExtract_PreservesTarDirectorySymbolicLink(".", "toolfetch/tool");
    }

    private void testExtract_PreservesTarDirectorySymbolicLink(String linkTarget, String fileName) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            TarArchiveEntry linkEntry = new TarArchiveEntry("toolfetch/bin", TarConstants.LF_SYMLINK);
            linkEntry.setLinkName(linkTarget);
            out.putArchiveEntry(linkEntry);
            out.closeArchiveEntry();
            TarArchiveEntry fileEntry = new TarArchiveEntry(fileName);
            fileEntry.setSize(content.length);
            out.putArchiveEntry(fileEntry);
            out.write(content);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/toolfetch.tar"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("bin")).isSymbolicLink();
        assertThat(tempDir.resolve("bin/tool")).hasBinaryContent(content);
    }

    @Test
    void testExtract_RemovesEmptyZipDestination() throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            out.finish();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.zip"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
    }

    @Test
    void testExtract_MissingDestinationPath() {
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("http://localhost/download/toolfetch.zip"))
                .build();
        Tool tool = configuration.tools().getFirst();
        archiveExtractService.extract(configuration, tool, Path.of("toolfetch.zip"));

        assertThat(tempDir.resolve(tool.id())).doesNotExist();
        testLogListAppenderSteps.assertAnyMatch(LogLevel.WARN.toString(),
                "Cannot determine the destination path. Skipping tool: " + tool.id());
    }

    @Test
    void testBinaryFileDetectionIsLoggedAtDebug() throws IOException {
        Path binary = Files.createFile(tempDir.resolve("download.bin"));
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/download.bin"))
                .build();
        Tool tool = configuration.tools().getFirst();

        archiveExtractService.extract(configuration, tool, binary);

        assertThat(tempDir.resolve("toolfetch.bin")).exists();
        testLogListAppenderSteps.assertExceptionLogged(SkippedArchiveException.class);
        testLogListAppenderSteps.assertNoErrorNoWarn();
    }

    @Test
    void testUnsupportedPermissionsAreLoggedAtDebug() throws IOException {
        Path binary = Files.createFile(tempDir.resolve("toolfetch.bin"));
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/toolfetch.bin"))
                .build();
        Tool tool = configuration.tools().getFirst();
        try (MockedStatic<Files> files = mockStatic(CALLS_REAL_METHODS)) {
            files.when(() -> Files.getPosixFilePermissions(binary)).thenThrow(new UnsupportedOperationException());

            archiveExtractService.extract(configuration, tool, binary);

            assertThat(binary).exists();
            testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
            testLogListAppenderSteps.assertNoErrorNoWarn();
        }
    }

    @Test
    void testBinaryRenameFailureIsLoggedAtDebug() throws IOException {
        Path binary = Files.createFile(tempDir.resolve("download.bin"));
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/download.bin"))
                .build();
        Tool tool = configuration.tools().getFirst();
        try (MockedStatic<Files> files = mockStatic(CALLS_REAL_METHODS)) {
            files.when(() -> Files.move(any(), any(), any())).thenThrow(new IOException("Cannot rename"));

            archiveExtractService.extract(configuration, tool, binary);

            assertThat(binary).exists();
            testLogListAppenderSteps.assertExceptionLogged(IOException.class);
            testLogListAppenderSteps.assertNoErrorNoWarn();
        }
    }

    @Test
    void testExtractEntry_MissingTargetParent_ZipSlip() throws IOException {
        Path target = Path.of("toolfetch.txt");
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            ArchiveEntry archiveEntry = mock();
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                            () -> archiveExtractService.extractEntry(acis, archiveEntry, target, 0))
                    .withMessage(
                            "Unsafe archive entry \"toolfetch.txt\" has no parent directory");
        }
    }

    @Test
    void testExtractEntry_TargetExists() throws IOException {
        Path target = tempDir.resolve("test1.txt");
        try (ArchiveInputStream<ArchiveEntry> ais = mock();
             ArchiveWithCompressorInputStream acis = new ArchiveWithCompressorInputStream(ais, null)) {
            ArchiveEntry archiveEntry = mock();
            archiveExtractService.extractEntry(acis, archiveEntry, target, 0);
            assertThat(target).isEmptyFile();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "test, '', test", "test/a/, 'test/a/b/', ''", "test/a/b/, 'test/a/', 'b/'", "test/a/b/c/d/e/, 'test/a/', 'b/c/d/e/'",
            "test/a/b/c/d/e/, 'a/b/', 'test/a/b/c/d/e/'"
    })
    void testStripEntryName(String actualEntryName, String topLevelDir, String expectedEntryName) {
        ArchiveEntry archiveEntry = new ZipArchiveEntry(actualEntryName);
        String strippedEntryName = archiveExtractService.stripEntryName(archiveEntry, topLevelDir);
        assertThat(strippedEntryName).isEqualTo(expectedEntryName);
    }

    @ParameterizedTest
    @CsvSource({
            ", ", "'', ''", "test, test/", "test/, test/"
    })
    void testNormalize(String actualEntryName, String expectedEntryName) {
        String normalizedEntryName = archiveExtractService.normalize(actualEntryName);
        assertThat(normalizedEntryName).isEqualTo(expectedEntryName);
    }

}

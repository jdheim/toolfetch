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
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemLoopException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
import org.apache.commons.compress.archivers.zip.UnrecognizedExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipShort;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_PreservesTarLink(byte linkType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        testExtract_PreservesTarLink(archive, new TarArchiveOutputStream(Files.newOutputStream(archive)), linkType, false);
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_PreservesTarGzipLink(byte linkType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar.gz");
        testExtract_PreservesTarLink(archive,
                new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(archive))), linkType, false);
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_PreservesTarLinkBeforeTarget(byte linkType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        testExtract_PreservesTarLink(archive, new TarArchiveOutputStream(Files.newOutputStream(archive)), linkType, true);
    }

    private void testExtract_PreservesTarLink(Path archive, TarArchiveOutputStream out, byte linkType,
            boolean linkBeforeTarget) throws IOException {
        byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
        try (out) {
            TarArchiveEntry linkEntry = new TarArchiveEntry("toolfetch/bin/tool", linkType);
            linkEntry.setLinkName(linkType == TarConstants.LF_LINK ? "toolfetch/bin/tool-real" : "tool-real");
            if (linkBeforeTarget) {
                out.putArchiveEntry(linkEntry);
                out.closeArchiveEntry();
            }

            TarArchiveEntry fileEntry = new TarArchiveEntry("toolfetch/bin/tool-real");
            fileEntry.setSize(content.length);
            out.putArchiveEntry(fileEntry);
            out.write(content);
            out.closeArchiveEntry();

            if (!linkBeforeTarget) {
                out.putArchiveEntry(linkEntry);
                out.closeArchiveEntry();
            }
        }
        testExtract_PreservesLink(archive, content, linkType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"tar", "zip", "jar"})
    void testExtract_ReplacesExistingHardLinkWithoutChangingOutsideAlias(String extension) throws IOException {
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path existing = Files.createLink(destination.resolve("tool-real"), outside);
        Path archive = destination.resolve("toolfetch." + extension);
        writeHardLinkArchive(archive, "toolfetch/bin/tool-real");

        extractTestArchive(archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(outside).hasContent("unchanged");
        assertThat(existing).hasContent("tool executable");
        assertThat(Files.isSameFile(existing, outside)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "tar, ./toolfetch/bin/tool-real", "zip, ./toolfetch/bin/tool-real", "jar, ./toolfetch/bin/tool-real",
            "tar, toolfetch/bin/../bin/tool-real", "zip, toolfetch/bin/../bin/tool-real", "jar, toolfetch/bin/../bin/tool-real"
    })
    void testExtract_PreservesEquivalentHardLinkTargetPaths(String extension, String linkTarget) throws IOException {
        Path archive = tempDir.resolve("toolfetch." + extension);
        writeHardLinkArchive(archive, linkTarget);

        testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), TarConstants.LF_LINK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"tar", "zip", "jar"})
    void testExtract_RejectsHardLinkTraversalBeforeStrippedArchiveRoot(String extension) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch." + extension);
        writeHardLinkArchive(archive, "../toolfetch/bin/tool-real");

        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    private void writeHardLinkArchive(Path archive, String linkTarget) throws IOException {
        byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
        String archiveName = Objects.requireNonNull(archive.getFileName()).toString();
        if (archiveName.endsWith(".tar")) {
            try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
                TarArchiveEntry file = new TarArchiveEntry("toolfetch/bin/tool-real");
                file.setSize(content.length);
                out.putArchiveEntry(file);
                out.write(content);
                out.closeArchiveEntry();
                TarArchiveEntry link = new TarArchiveEntry("toolfetch/bin/tool", TarConstants.LF_LINK);
                link.setLinkName(linkTarget);
                out.putArchiveEntry(link);
                out.closeArchiveEntry();
            }
        } else {
            try (ZipArchiveOutputStream out = archiveName.endsWith(".jar")
                    ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                    : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
                out.putArchiveEntry(new ZipArchiveEntry("toolfetch/bin/tool-real"));
                out.write(content);
                out.closeArchiveEntry();
                addZipLinkEntry(out, "toolfetch/bin/tool", linkTarget, true, true);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_ReplacesPendingTarLinkWithFile(byte linkType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        writeRepeatedTarEntryArchive(archive, linkType, TarConstants.LF_NORMAL);

        extractTestArchive(archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("tool")).isRegularFile().hasContent("replacement");
        assertThat(Files.isSymbolicLink(tempDir.resolve("tool"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_ReplacesTarFileWithLink(byte linkType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        writeRepeatedTarEntryArchive(archive, TarConstants.LF_NORMAL, linkType);

        testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), linkType);
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_ReplacesPendingTarLinkWithOtherLinkType(byte linkType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        byte earlierType = linkType == TarConstants.LF_LINK ? TarConstants.LF_SYMLINK : TarConstants.LF_LINK;
        writeRepeatedTarEntryArchive(archive, earlierType, linkType);

        testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), linkType);
    }

    private void writeRepeatedTarEntryArchive(Path archive, byte earlierType, byte laterType) throws IOException {
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            byte[] original = "tool executable".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry file = new TarArchiveEntry("tool-real");
            file.setSize(original.length);
            out.putArchiveEntry(file);
            out.write(original);
            out.closeArchiveEntry();
            for (byte type : new byte[] {earlierType, laterType}) {
                byte[] replacement = "replacement".getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry entry = new TarArchiveEntry("tool", type);
                if (type == TarConstants.LF_NORMAL) {
                    entry.setSize(replacement.length);
                } else {
                    entry.setLinkName("tool-real");
                }
                out.putArchiveEntry(entry);
                if (type == TarConstants.LF_NORMAL) {
                    out.write(replacement);
                }
                out.closeArchiveEntry();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"zip, false", "jar, false", "zip, true", "jar, true"})
    void testExtract_UsesEachZipEntryModeWhenReplacingSymbolicLink(String extension, boolean linkLast) throws IOException {
        Path archive = tempDir.resolve("toolfetch." + extension);
        try (ZipArchiveOutputStream out = extension.equals("jar")
                ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            addZipFile(out, "tool-real", "tool executable".getBytes(StandardCharsets.UTF_8));
            if (linkLast) {
                addZipFile(out, "tool", new byte[8192]);
                addZipLinkEntry(out, "tool", "tool-real", false, false);
            } else {
                addZipLinkEntry(out, "tool", "tool-real", false, false);
                addZipFile(out, "tool", "replacement".getBytes(StandardCharsets.UTF_8));
            }
        }
        if (linkLast) {
            testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), TarConstants.LF_SYMLINK);
        } else {
            extractTestArchive(archive);

            testLogListAppenderSteps.assertNoErrorNoWarn();
            assertThat(Files.isSymbolicLink(tempDir.resolve("tool"))).isFalse();
            assertThat(tempDir.resolve("tool")).hasContent("replacement");
        }
    }

    @ParameterizedTest
    @CsvSource({"tar, false", "zip, false", "jar, false", "tar, true", "zip, true", "jar, true"})
    void testExtract_PreservesHardLinkOriginalVersion(String extension, boolean forwardReference) throws IOException {
        Path archive = tempDir.resolve("toolfetch." + extension);
        writeHardLinkVersionArchive(archive, extension, forwardReference, false);

        extractTestArchive(archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("alias")).hasContent("original");
        assertThat(tempDir.resolve("data")).hasContent("replacement");
        assertThat(Files.isSameFile(tempDir.resolve("alias"), tempDir.resolve("data"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tar", "zip", "jar"})
    void testExtract_PreservesForwardHardLinkChainOriginalVersion(String extension) throws IOException {
        Path archive = tempDir.resolve("toolfetch." + extension);
        writeHardLinkVersionArchive(archive, extension, true, true);

        extractTestArchive(archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("alias")).hasContent("original");
        assertThat(tempDir.resolve("intermediate")).hasContent("original");
        assertThat(tempDir.resolve("data")).hasContent("replacement");
    }

    @Test
    void testExtract_RejectsHardLinkToPendingSymbolicReplacement() throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            addTarFile(out, "data", "original".getBytes(StandardCharsets.UTF_8));
            TarArchiveEntry symbolic = new TarArchiveEntry("data", TarConstants.LF_SYMLINK);
            symbolic.setLinkName("other");
            out.putArchiveEntry(symbolic);
            out.closeArchiveEntry();
            TarArchiveEntry hard = new TarArchiveEntry("alias", TarConstants.LF_LINK);
            hard.setLinkName("data");
            out.putArchiveEntry(hard);
            out.closeArchiveEntry();
            addTarFile(out, "other", "other".getBytes(StandardCharsets.UTF_8));
        }
        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(IOException.class);
    }

    private void writeHardLinkVersionArchive(Path archive, String extension, boolean forwardReference, boolean chain)
            throws IOException {
        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        byte[] replacement = "replacement".getBytes(StandardCharsets.UTF_8);
        Map<String, String> links = new LinkedHashMap<>();
        links.put("alias", chain ? "intermediate" : "data");
        if (chain) {
            links.put("intermediate", "data");
        }
        if (extension.equals("tar")) {
            try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
                if (!forwardReference) {
                    addTarFile(out, "data", original);
                }
                for (Map.Entry<String, String> link : links.entrySet()) {
                    TarArchiveEntry entry = new TarArchiveEntry(link.getKey(), TarConstants.LF_LINK);
                    entry.setLinkName(link.getValue());
                    out.putArchiveEntry(entry);
                    out.closeArchiveEntry();
                }
                if (forwardReference) {
                    addTarFile(out, "data", original);
                }
                addTarFile(out, "data", replacement);
            }
        } else {
            try (ZipArchiveOutputStream out = extension.equals("jar")
                    ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                    : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
                if (!forwardReference) {
                    addZipFile(out, "data", original);
                }
                for (Map.Entry<String, String> link : links.entrySet()) {
                    addZipLinkEntry(out, link.getKey(), link.getValue(), true, true);
                }
                if (forwardReference) {
                    addZipFile(out, "data", original);
                }
                addZipFile(out, "data", replacement);
            }
        }
    }

    private void addTarFile(TarArchiveOutputStream out, String name, byte[] content) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(content.length);
        out.putArchiveEntry(entry);
        out.write(content);
        out.closeArchiveEntry();
    }

    private void addZipFile(ZipArchiveOutputStream out, String name, byte[] content) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        entry.setUnixMode(UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
        out.putArchiveEntry(entry);
        out.write(content);
        out.closeArchiveEntry();
    }

    @Test
    void testExtract_PreservesTarHardLinkChainBeforeTarget() throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        Map<String, String> links = new LinkedHashMap<>();
        links.put("tool", "intermediate");
        links.put("intermediate", "tool-real");
        writeTarHardLinkArchive(archive, links);

        testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), TarConstants.LF_LINK);

        assertThat(Files.isSameFile(tempDir.resolve("intermediate"), tempDir.resolve("tool-real"))).isTrue();
    }

    @Test
    void testExtract_RejectsTarHardLinkCycle() throws IOException {
        testExtract_RejectsTarHardLinks(Map.of("tool", "other", "other", "tool"), IOException.class);
    }

    @Test
    void testExtract_RejectsTarHardLinkToMissingTarget() throws IOException {
        testExtract_RejectsTarHardLinks(Map.of("tool", "missing"), NoSuchFileException.class);
    }

    @Test
    void testExtract_RejectsTarHardLinkToDirectory() throws IOException {
        Files.createDirectories(tempDir.resolve("destination/directory"));

        testExtract_RejectsTarHardLinks(Map.of("tool", "directory"), IOException.class);
    }

    @Test
    void testExtract_RejectsTarHardLinkToSymbolicLink() throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.createSymbolicLink(destination.resolve("symbolic"), Path.of("tool-real"));

        testExtract_RejectsTarHardLinks(Map.of("tool", "symbolic"), IOException.class);
    }

    @Test
    void testExtract_RejectsTarHardLinkThroughSymbolicLinkOutsideDestination() throws IOException {
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.createSymbolicLink(destination.resolve("symbolic"), outside);

        testExtract_RejectsTarHardLinks(Map.of("tool", "symbolic"), UnsupportedOperationException.class);

        assertThat(outside).hasContent("unchanged");
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_RejectsLinkCreationThroughExistingSymbolicParent(byte linkType) throws IOException {
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.createSymbolicLink(destination.resolve("alias"), outside);
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            TarArchiveEntry link = new TarArchiveEntry("alias/link", linkType);
            link.setLinkName(linkType == TarConstants.LF_LINK ? "tool-real" : "../tool-real");
            out.putArchiveEntry(link);
            out.closeArchiveEntry();
            TarArchiveEntry file = new TarArchiveEntry("tool-real");
            file.setSize(1);
            out.putArchiveEntry(file);
            out.write(1);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.tar"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(outside.resolve("link")).doesNotExist();
        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @Test
    void testExtract_RejectsSymbolicLinkTargetThroughExistingSymbolicParent() throws IOException {
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.writeString(outside.resolve("secret"), "unchanged");
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.createSymbolicLink(destination.resolve("alias"), outside);
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            TarArchiveEntry link = new TarArchiveEntry("link", TarConstants.LF_SYMLINK);
            link.setLinkName("alias/secret");
            out.putArchiveEntry(link);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.tar"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        assertThat(outside.resolve("secret")).hasContent("unchanged");
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void testExtract_RejectsOversizedPkwareLinkPayload(boolean jar, boolean hardLink) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve(jar ? "toolfetch.jar" : "toolfetch.zip");
        try (ZipArchiveOutputStream out = jar
                ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry link = new ZipArchiveEntry("link");
            link.setUnixMode(hardLink ? UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM
                    : UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
            link.addExtraField(zipUnixExtraField("tool-real".getBytes(StandardCharsets.UTF_8)));
            out.putArchiveEntry(link);
            out.write(new byte[4097]);
            out.closeArchiveEntry();
            out.putArchiveEntry(new ZipArchiveEntry("tool-real"));
            out.write(1);
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/" + archive.getFileName()))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @Test
    void testExtract_PreservesSymbolicTargetWithParentAfterSymbolicComponent() throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        Map<String, String> links = new LinkedHashMap<>();
        links.put("alias", "real/sub");
        links.put("link", "alias/../tool-real");
        writeTarSymbolicLinkArchive(archive, links);
        extractTestArchive(archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(Files.readSymbolicLink(tempDir.resolve("link"))).isEqualTo(Path.of("alias/../tool-real"));
        assertThat(tempDir.resolve("link")).hasContent("real contents");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExtract_PreservesDanglingSymbolicLinkChain(boolean reverseOrder) throws IOException {
        Path archive = tempDir.resolve("toolfetch.tar");
        Map<String, String> links = new LinkedHashMap<>();
        if (reverseOrder) {
            links.put("second", "missing");
            links.put("first", "second");
        } else {
            links.put("first", "second");
            links.put("second", "missing");
        }
        writeTarSymbolicLinkArchive(archive, links);
        extractTestArchive(archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(Files.readSymbolicLink(tempDir.resolve("first"))).isEqualTo(Path.of("second"));
        assertThat(Files.readSymbolicLink(tempDir.resolve("second"))).isEqualTo(Path.of("missing"));
    }

    @Test
    void testExtract_RejectsEscapingSymbolicLinkChain() throws IOException {
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        Map<String, String> links = new LinkedHashMap<>();
        links.put("real/sub/up", "../..");
        links.put("link", "real/sub/up/../outside.txt");
        writeTarSymbolicLinkArchive(archive, links);
        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        assertThat(outside).hasContent("unchanged");
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @Test
    void testExtract_RejectsSymbolicLinkCycle() throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        writeTarSymbolicLinkArchive(archive, Map.of("first", "second", "second", "first"));
        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(FileSystemLoopException.class);
    }

    private void writeTarSymbolicLinkArchive(Path archive, Map<String, String> links) throws IOException {
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> link : links.entrySet()) {
                TarArchiveEntry entry = new TarArchiveEntry(link.getKey(), TarConstants.LF_SYMLINK);
                entry.setLinkName(link.getValue());
                out.putArchiveEntry(entry);
                out.closeArchiveEntry();
            }
            for (String name : new String[] {"real/sub/placeholder", "real/tool-real", "tool-real"}) {
                byte[] content = (name.equals("tool-real") ? "root contents" : "real contents").getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry entry = new TarArchiveEntry(name);
                entry.setSize(content.length);
                out.putArchiveEntry(entry);
                out.write(content);
                out.closeArchiveEntry();
            }
        }
    }

    private void extractTestArchive(Path archive) {
        Configuration configuration = configuration().destination(Objects.requireNonNull(archive.getParent()).toString())
                .addTool(tool().url("https://example.com/" + archive.getFileName()))
                .build();
        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);
    }

    @ParameterizedTest
    @ValueSource(strings = {"tar", "zip", "jar"})
    void testExtract_RejectsEmptySymbolicLinkTarget(String extension) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch." + extension);
        if (extension.equals("tar")) {
            try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
                TarArchiveEntry link = new TarArchiveEntry("link", TarConstants.LF_SYMLINK);
                link.setLinkName("");
                out.putArchiveEntry(link);
                out.closeArchiveEntry();
            }
        } else {
            try (ZipArchiveOutputStream out = extension.equals("jar")
                    ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                    : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
                addZipLinkEntry(out, "link", "", false, false);
            }
        }
        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_RejectsOversizedTarLinkPayload(byte linkType) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            TarArchiveEntry link = new TarArchiveEntry("link", linkType);
            link.setLinkName("tool-real");
            link.setSize(4097);
            out.putArchiveEntry(link);
            out.write(new byte[4097]);
            out.closeArchiveEntry();
            TarArchiveEntry file = new TarArchiveEntry("tool-real");
            file.setSize(1);
            out.putArchiveEntry(file);
            out.write(1);
            out.closeArchiveEntry();
        }
        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @Test
    void testExtract_RejectsDanglingSymbolicTargetEscapingAfterParentTraversal() throws IOException {
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.createSymbolicLink(destination.resolve("alias"), outside);
        Path archive = destination.resolve("toolfetch.tar");
        writeTarSymbolicLinkArchive(archive, Map.of("link", "missing/../alias/secret"));
        extractTestArchive(archive);

        assertThat(destination).doesNotExist();
        assertThat(outside).isEmptyDirectory();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    private void testExtract_RejectsTarHardLinks(Map<String, String> links, Class<? extends Throwable> exceptionType) throws
            IOException {
        Path destination = Files.createDirectories(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        writeTarHardLinkArchive(archive, links);
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.tar"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(exceptionType);
    }

    private void writeTarHardLinkArchive(Path archive, Map<String, String> links) throws IOException {
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> link : links.entrySet()) {
                TarArchiveEntry entry = new TarArchiveEntry(link.getKey(), TarConstants.LF_LINK);
                entry.setLinkName(link.getValue());
                out.putArchiveEntry(entry);
                out.closeArchiveEntry();
            }
            byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry("tool-real");
            entry.setSize(content.length);
            out.putArchiveEntry(entry);
            out.write(content);
            out.closeArchiveEntry();
        }
    }

    @Test
    void testExtract_PreservesZipSymbolicLinkInEntryContent() throws IOException {
        Path archive = tempDir.resolve("toolfetch.zip");
        testExtract_PreservesZipLink(archive, new ZipArchiveOutputStream(Files.newOutputStream(archive)), false, false);
    }

    @Test
    void testExtract_PreservesJarSymbolicLinkInEntryContent() throws IOException {
        Path archive = tempDir.resolve("toolfetch.jar");
        testExtract_PreservesZipLink(archive, new JarArchiveOutputStream(Files.newOutputStream(archive)), false, false);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"zip", "jar"})
    void testExtract_PreservesPkwareSymbolicLink(String extension) throws IOException {
        testExtract_PreservesPkwareLink(extension, false);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"zip", "jar"})
    void testExtract_PreservesPkwareHardLink(String extension) throws IOException {
        testExtract_PreservesPkwareLink(extension, true);
    }

    private void testExtract_PreservesPkwareLink(String extension, boolean hardLink) throws IOException {
        Path archive = tempDir.resolve("toolfetch." + extension);
        ZipArchiveOutputStream out = extension.equals("jar") ? new JarArchiveOutputStream(Files.newOutputStream(archive)) :
                new ZipArchiveOutputStream(Files.newOutputStream(archive));
        testExtract_PreservesZipLink(archive, out, hardLink, true);
    }

    private void testExtract_PreservesZipLink(Path archive, ZipArchiveOutputStream out, boolean hardLink,
            boolean extraField) throws IOException {
        byte[] content = "tool executable".getBytes(StandardCharsets.UTF_8);
        try (out) {
            ZipArchiveEntry fileEntry = new ZipArchiveEntry("toolfetch/bin/tool-real");
            out.putArchiveEntry(fileEntry);
            out.write(content);
            out.closeArchiveEntry();

            addZipLinkEntry(out, "toolfetch/bin/tool", hardLink ? "toolfetch/bin/tool-real" : "tool-real", hardLink, extraField);
        }
        try (MockedStatic<Files> files = mockStatic(CALLS_REAL_METHODS)) {
            testExtract_PreservesLink(archive, content, hardLink ? TarConstants.LF_LINK : TarConstants.LF_SYMLINK);
            files.verify(() -> {
                try (var _ = Files.newOutputStream(tempDir.resolve("tool"))) {
                    // Mockito verifies the call without opening a stream.
                }
            }, never());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExtract_PrefersUnixExtraFieldSymbolicLinkTarget(boolean jar) throws IOException {
        Path archive = tempDir.resolve(jar ? "toolfetch.jar" : "toolfetch.zip");
        writeZipUnixSymbolicLinkArchive(archive, jar, "tool-real".getBytes(StandardCharsets.UTF_8),
                "../outside.txt".getBytes(StandardCharsets.UTF_8));

        testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), TarConstants.LF_SYMLINK);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExtract_FallsBackToSymbolicLinkContentForUnixMetadata(boolean jar) throws IOException {
        Path archive = tempDir.resolve(jar ? "toolfetch.jar" : "toolfetch.zip");
        writeZipUnixSymbolicLinkArchive(archive, jar, new byte[0], "tool-real".getBytes(StandardCharsets.UTF_8));

        testExtract_PreservesLink(archive, "tool executable".getBytes(StandardCharsets.UTF_8), TarConstants.LF_SYMLINK);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExtract_RejectsMalformedUnixExtraFieldSymbolicLinkTarget(boolean jar) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve(jar ? "toolfetch.jar" : "toolfetch.zip");
        writeZipUnixSymbolicLinkArchive(archive, jar, new byte[]{(byte) 0xc3, 0x28},
                "tool-real".getBytes(StandardCharsets.UTF_8));
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/" + archive.getFileName()))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(MalformedInputException.class);
    }

    @ParameterizedTest
    @CsvSource({"false, a, 4097", "true, a, 4097", "false, é, 2049", "true, é, 2049"})
    void testExtract_RejectsOversizedUnixExtraFieldSymbolicLinkTarget(boolean jar, String character, int repetitions) throws
            IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve(jar ? "toolfetch.jar" : "toolfetch.zip");
        writeZipUnixSymbolicLinkArchive(archive, jar, character.repeat(repetitions).getBytes(StandardCharsets.UTF_8),
                new byte[0]);
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/" + archive.getFileName()))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
        assertThat(testLogListAppenderSteps.messages()).anyMatch(message -> message.contains("link target exceeds 4096 bytes"));
    }

    private void writeZipUnixSymbolicLinkArchive(Path archive, boolean jar, byte[] extraTarget, byte[] contentTarget) throws
            IOException {
        try (ZipArchiveOutputStream out = jar ? new JarArchiveOutputStream(Files.newOutputStream(archive)) :
                new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry linkEntry = new ZipArchiveEntry("toolfetch/bin/tool");
            linkEntry.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
            linkEntry.addExtraField(zipUnixExtraField(extraTarget));
            out.putArchiveEntry(linkEntry);
            out.write(contentTarget);
            out.closeArchiveEntry();
            ZipArchiveEntry fileEntry = new ZipArchiveEntry("toolfetch/bin/tool-real");
            out.putArchiveEntry(fileEntry);
            out.write("tool executable".getBytes(StandardCharsets.UTF_8));
            out.closeArchiveEntry();
        }
    }

    private UnrecognizedExtraField zipUnixExtraField(byte[] target) {
        // PKWARE Unix extra field: timestamps, UID and GID occupy the first 12 bytes.
        byte[] data = new byte[12 + target.length];
        System.arraycopy(target, 0, data, 12, target.length);
        UnrecognizedExtraField unixField = new UnrecognizedExtraField();
        unixField.setHeaderId(new ZipShort(0x000d));
        unixField.setLocalFileDataData(data);
        return unixField;
    }

    private void addZipLinkEntry(ZipArchiveOutputStream out, String name, String target, boolean hardLink,
            boolean extraField) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        byte[] targetBytes = target.getBytes(StandardCharsets.UTF_8);
        entry.setUnixMode(
                hardLink ? UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM : UnixStat.LINK_FLAG | UnixStat.DEFAULT_LINK_PERM);
        if (extraField) {
            entry.addExtraField(zipUnixExtraField(targetBytes));
        }
        out.putArchiveEntry(entry);
        if (!extraField) {
            out.write(targetBytes);
        }
        out.closeArchiveEntry();
    }

    private void testExtract_PreservesLink(Path archive, byte[] content, byte linkType) throws IOException {
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/" + archive.getFileName()))
                .build();
        Tool tool = configuration.tools().getFirst();

        archiveExtractService.extract(configuration, tool, archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("tool-real")).hasBinaryContent(content);
        Path link = tempDir.resolve("tool");
        if (linkType == TarConstants.LF_SYMLINK) {
            assertThat(link).isSymbolicLink();
            assertThat(Files.readSymbolicLink(link)).isEqualTo(Path.of("tool-real"));
        } else {
            assertThat(Files.isSymbolicLink(link)).isFalse();
            assertThat(Files.isSameFile(link, tempDir.resolve("tool-real"))).isTrue();
        }
        assertThat(link).hasBinaryContent(content);
    }

    @ParameterizedTest
    @ValueSource(bytes = {TarConstants.LF_SYMLINK, TarConstants.LF_LINK})
    void testExtract_RejectsTarLinkOutsideDestination(byte linkType) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            TarArchiveEntry linkEntry = new TarArchiveEntry("tool", linkType);
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

    @ParameterizedTest
    @CsvSource({"false, false", "true, true", "false, true"})
    void testExtract_RejectsZipLinkOutsideDestination(boolean hardLink, boolean extraField) throws IOException {
        testExtract_RejectsZipLinkOutsideDestination(false, hardLink, extraField);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, true", "false, true"})
    void testExtract_RejectsJarLinkOutsideDestination(boolean hardLink, boolean extraField) throws IOException {
        testExtract_RejectsZipLinkOutsideDestination(true, hardLink, extraField);
    }

    private void testExtract_RejectsZipLinkOutsideDestination(boolean jar, boolean hardLink, boolean extraField) throws
            IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "unchanged");
        String archiveName = jar ? "toolfetch.jar" : "toolfetch.zip";
        Path archive = destination.resolve(archiveName);
        try (ZipArchiveOutputStream out = jar
                ? new JarArchiveOutputStream(Files.newOutputStream(archive))
                : new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            addZipLinkEntry(out, "tool", "../outside.txt", hardLink, extraField);
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/" + archiveName))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        assertThat(outside).hasContent("unchanged");
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {UnixStat.FILE_FLAG, 0})
    void testExtract_KeepsZipUnixMetadataWithoutLinkTarget(int fileType) throws IOException {
        Path archive = tempDir.resolve("toolfetch.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            ZipArchiveEntry entry = new ZipArchiveEntry("tool");
            entry.setUnixMode(fileType | UnixStat.DEFAULT_FILE_PERM);
            entry.addExtraField(zipUnixExtraField(new byte[0]));
            out.putArchiveEntry(entry);
            out.write("ordinary file".getBytes(StandardCharsets.UTF_8));
            out.closeArchiveEntry();
        }
        Configuration configuration = configuration().destination(tempDir.toString())
                .addTool(tool().url("https://example.com/toolfetch.zip"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        testLogListAppenderSteps.assertNoErrorNoWarn();
        assertThat(tempDir.resolve("tool")).hasContent("ordinary file");
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, true", "false, true"})
    void testExtract_RejectsOversizedZipLinkTarget(boolean hardLink, boolean extraField) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(archive))) {
            addZipLinkEntry(out, "tool", "a".repeat(4097), hardLink, extraField);
        }
        Configuration configuration = configuration().destination(destination.toString())
                .addTool(tool().url("https://example.com/toolfetch.zip"))
                .build();

        archiveExtractService.extract(configuration, configuration.tools().getFirst(), archive);

        assertThat(destination).doesNotExist();
        testLogListAppenderSteps.assertExceptionLogged(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource({"a, 4097, false", "é, 2049, false", "a, 4097, true", "é, 2049, true"})
    void testExtract_RejectsOversizedTarLinkTarget(String character, int repetitions, boolean hardLink) throws IOException {
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path archive = destination.resolve("toolfetch.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            TarArchiveEntry linkEntry = new TarArchiveEntry("tool", hardLink ? TarConstants.LF_LINK : TarConstants.LF_SYMLINK);
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
        assertThat(testLogListAppenderSteps.messages()).anyMatch(message -> message.contains("link target exceeds 4096 bytes"));
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

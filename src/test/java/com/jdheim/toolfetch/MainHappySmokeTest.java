/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdheim.toolfetch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.addZipEntryFile;
import static com.jdheim.toolfetch.util.archive.ArchiveUtils.createZip;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.jdheim.toolfetch.service.config.parse.YamlParserService;
import com.jdheim.toolfetch.service.config.validation.JsonSchemaValidationService;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/// Smoke Tests for [Main]
@WireMockTest
class MainHappySmokeTest {

    CommandLine toolFetch;

    TestLogListAppender testLogListAppender;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        toolFetch = Main.commandLine();
        StringWriter log = new StringWriter();
        toolFetch.setOut(new PrintWriter(log));
        toolFetch.setErr(new PrintWriter(log));
        testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(YamlParserService.class, JsonSchemaValidationService.class, WebDownloadService.class,
                ArchiveExtractService.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-V", "--version", "-h", "--help"})
    void testToolFetch_VersionAndHelp(String option) {
        int exitCode = toolFetch.execute(option);
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-c", "--config"})
    void testInstall(String option, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        Path toolFetchConfigPath = tempDir.resolve("toolfetch.yaml");
        String id = "toolfetch";
        String url = "http://localhost:%d/download/toolfetch.zip".formatted(wmRuntimeInfo.getHttpPort());
        writeToolFetchConfig(toolFetchConfigPath, id, url);
        stubWireMockToServeZip();

        int exitCode = toolFetch.execute(option, toolFetchConfigPath.toString());
        testLogListAppender.assertNoErrorNoWarn();
        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);

        assertThat(getAllServeEvents()).hasSize(1);
        WireMock.verify(1, getRequestedFor(urlEqualTo("/download/toolfetch.zip")));

        Path destinationPath = tempDir.resolve(id);
        assertDestinationPath(destinationPath);
        assertLogs(destinationPath, id, url);
    }

    private void writeToolFetchConfig(Path toolfetchConfigPath, String id, String url) throws IOException {
        if (!Files.exists(toolfetchConfigPath)) {
            Files.writeString(toolfetchConfigPath, """
                    destination: %s
                    tools:
                      - id: %s
                        url: "%s"
                    """.formatted(tempDir, id, url));
        }
    }

    private void stubWireMockToServeZip() {
        byte[] archiveBytes = createZip(zaos -> {
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
        stubFor(get("/download/toolfetch.zip").willReturn(aResponse().withStatus(200)
                .withHeader(ContentTypes.CONTENT_TYPE, ContentTypes.OCTET_STREAM)
                .withHeader(ContentTypes.CONTENT_LENGTH, String.valueOf(archiveBytes.length))
                .withHeader("Content-Disposition", "attachment; filename=toolfetch.zip")
                .withBody(archiveBytes)));
    }

    private void assertDestinationPath(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test2")).doesNotExist();
        assertThat(destinationPath.resolve("test3")).isDirectory();
        assertThat(destinationPath.resolve("test3/test4")).isDirectory();
        for (int i = 1; i <= 100; i++) {
            assertThat(destinationPath.resolve("test%d1.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d1".formatted(i));
            assertThat(destinationPath.resolve("test%d2.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d2".formatted(i));
            assertThat(destinationPath.resolve("test%d3.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d3".formatted(i));
            assertThat(destinationPath.resolve("test3/test%d1.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d1".formatted(i));
            assertThat(destinationPath.resolve("test3/test%d2.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d2".formatted(i));
            assertThat(destinationPath.resolve("test3/test%d3.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d3".formatted(i));
            assertThat(destinationPath.resolve("test3/test4/test%d1.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d1".formatted(i));
            assertThat(destinationPath.resolve("test3/test4/test%d2.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d2".formatted(i));
            assertThat(destinationPath.resolve("test3/test4/test%d3.txt".formatted(i))).exists()
                    .isRegularFile()
                    .hasContent("Install Test %d3".formatted(i));
        }
        assertThat(destinationPath.resolve("toolfetch.zip")).doesNotExist();
    }

    private void assertLogs(Path destinationPath, String id, String url) {
        testLogListAppender.assertAnyMatch(Level.INFO, "> Install " + id);
        testLogListAppender.assertAnyMatch(Level.INFO, "Create " + destinationPath);
        testLogListAppender.assertAnyMatch(Level.INFO, "Download %s to %s".formatted(url, destinationPath));
        String archiveName = id + ".zip";
        testLogListAppender.assertAnyMatch(Level.INFO,
                "Extract %s to %s".formatted(destinationPath.resolve(archiveName), destinationPath));
        testLogListAppender.assertAnyMatch(Level.INFO, "Removing " + destinationPath.resolve(archiveName));
    }

}

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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import ch.qos.logback.classic.Level;
import com.jdheim.toolfetch.service.config.parse.YamlParserService;
import com.jdheim.toolfetch.service.config.validation.JsonSchemaValidationService;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.service.install.resolve.ArchiveNameResolver;
import com.jdheim.toolfetch.service.install.resolve.ToolUriTransformer;
import com.jdheim.toolfetch.util.log.TestLogListAppender;
import org.apache.commons.lang3.ArrayUtils;
import picocli.CommandLine;

public class ToolFetchTestBase {

    static final boolean IS_DEBUG_NATIVE_MODE_ENABLED = Boolean.getBoolean("native-debug");

    static final boolean IS_FORCE_NATIVE_MODE_ENABLED = Boolean.getBoolean("force-native");

    static final boolean IS_QUIET_MODE_ENABLED = Boolean.getBoolean("quiet");

    static final Path NATIVE_IMAGE_EXEC = Path.of("target/toolfetch");

    static final Path NATIVE_IMAGE_METADATA = Path.of("target/toolfetch.metadata");

    static final Path LIBSVMJDWP_LIBRARY = Path.of("target/libsvmjdwp.so");

    ExecResult execute(String... args) throws IOException, InterruptedException {
        if (IS_FORCE_NATIVE_MODE_ENABLED) {
            assertThat(NATIVE_IMAGE_EXEC).withFailMessage(
                    "File not found: %s. Run: ./build.sh --native".formatted(NATIVE_IMAGE_EXEC)).exists();
        }
        if (Files.exists(NATIVE_IMAGE_EXEC)) {
            return executeNativeImage(args);
        }
        return executeTest(args);
    }

    /// Run: `./build.sh --native-debug` and use `-Dnative-debug=true` in VM Options to enable debugging of Native Image
    private ExecResult executeNativeImage(String[] args) throws IOException, InterruptedException {
        args = enrichArgs(args);
        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        List<String> logs = readLogs(process);
        int exitCode = process.waitFor();
        return new ExecResult(exitCode, logs);
    }

    private String[] enrichArgs(String[] args) {
        if (IS_DEBUG_NATIVE_MODE_ENABLED) {
            assertThat(LIBSVMJDWP_LIBRARY).withFailMessage(
                    "File not found: %s. Run: ./build.sh --native-debug".formatted(LIBSVMJDWP_LIBRARY)).exists();
            assertThat(NATIVE_IMAGE_METADATA).withFailMessage(
                    "File not found: %s. Run: ./build.sh --native-debug".formatted(NATIVE_IMAGE_METADATA)).exists();
            String[] debugArgs = {NATIVE_IMAGE_EXEC.toString(), "-XX:JDWPOptions=transport=dt_socket,server=y,address=5005"};
            return ArrayUtils.addAll(debugArgs, args);
        } else {
            return ArrayUtils.addFirst(args, NATIVE_IMAGE_EXEC.toString());
        }
    }

    private List<String> readLogs(Process process) throws IOException {
        try (BufferedReader reader = process.inputReader()) {
            if (IS_QUIET_MODE_ENABLED) {
                return reader.readAllLines();
            }
            List<String> logs = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
                IO.println(line);
            }
            return logs;
        }
    }

    private ExecResult executeTest(String[] args) {
        CommandLine toolFetch = Main.commandLine();
        StringWriter log = new StringWriter();
        toolFetch.setOut(new PrintWriter(log));
        toolFetch.setErr(new PrintWriter(log));
        TestLogListAppender testLogListAppender = new TestLogListAppender();
        testLogListAppender.start(YamlParserService.class, JsonSchemaValidationService.class, WebDownloadService.class,
                ArchiveExtractService.class, ArchiveNameResolver.class, ToolUriTransformer.class);
        int exitCode = toolFetch.execute(args);
        List<String> writerLogs = log.toString().lines().toList();
        List<String> collectedLogs = testLogListAppender.list.stream()
                .map(event -> "[%s] %s".formatted(event.getLevel(), event.getFormattedMessage()))
                .toList();
        List<String> logs = Stream.concat(writerLogs.stream(), collectedLogs.stream()).toList();
        return new ExecResult(exitCode, logs);
    }

    void assertAnyMatch(ExecResult execResult, String message) {
        assertThat(execResult.logs()).anyMatch(line -> line.contains(message));
    }

    void assertNoErrorNoWarn(ExecResult execResult) {
        assertThat(execResult.logs()).noneMatch(
                line -> line.contains("Exception in thread") || line.contains("[%s]".formatted(Level.ERROR)) ||
                        line.contains("[%s]".formatted(Level.WARN)));
    }

    void assertNoError(ExecResult execResult) {
        assertThat(execResult.logs()).noneMatch(
                line -> line.contains("Exception in thread") || line.contains("[%s]".formatted(Level.ERROR)));
    }

    record ExecResult(int exitCode, List<String> logs) {

    }

}

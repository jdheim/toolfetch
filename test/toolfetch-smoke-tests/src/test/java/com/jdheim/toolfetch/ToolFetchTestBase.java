/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

public class ToolFetchTestBase {

    static final boolean IS_DEBUG_NATIVE_MODE_ENABLED = Boolean.getBoolean("native-debug");

    static final boolean IS_FORCE_NATIVE_MODE_ENABLED = Boolean.getBoolean("force-native");

    static final boolean IS_QUIET_MODE_ENABLED = Boolean.getBoolean("quiet");

    static final Path NATIVE_IMAGE_EXEC = Path.of(System.getProperty("maven.multiModuleProjectDirectory", "../../"), "target",
            "toolfetch");

    static final Path NATIVE_IMAGE_METADATA = Path.of("../../target", "toolfetch.metadata");

    static final Path LIBSVMJDWP_LIBRARY = Path.of("../../target", "libsvmjdwp.so");

    private static final Path TOOLFETCH_LOG = Path.of(System.getProperty("user.home"), ".toolfetch", "toolfetch.log");

    private static final Pattern DEBUG_LOGGER_NAME = Pattern.compile("(\\[DEBUG]) \\[[^]]+] ");

    ExecResult execute(String... args) throws IOException, InterruptedException {
        if (IS_FORCE_NATIVE_MODE_ENABLED) {
            assertThat(NATIVE_IMAGE_EXEC).withFailMessage(
                    "File not found: %s. Run: ./build.sh --native".formatted(NATIVE_IMAGE_EXEC)).exists();
        }
        if (runtimeModeNativeImage()) {
            return executeNativeImage(args);
        }
        return executeTest(args);
    }

    boolean runtimeModeNativeImage() {
        return Files.exists(NATIVE_IMAGE_EXEC);
    }

    /// Run: `./build.sh --native-debug` and use `-Dnative-debug=true` in VM Options to enable debugging of Native Image
    @SuppressFBWarnings(value = "COMMAND_INJECTION",
            justification = "Test-only code. Arguments are constructed internally for controlled Smoke Tests of Native Image and are not influenced by external or user input")
    private ExecResult executeNativeImage(String[] args) throws IOException, InterruptedException {
        args = enrichArgs(args);
        Files.deleteIfExists(TOOLFETCH_LOG);
        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        List<String> consoleLogs = readLogs(process);
        int exitCode = process.waitFor();
        List<String> fileLogs = Files.exists(TOOLFETCH_LOG) ? Files.readAllLines(TOOLFETCH_LOG) : List.of();
        List<String> debugLogs = fileLogs.stream().filter(line -> line.contains("[DEBUG]")).map(
                line -> DEBUG_LOGGER_NAME.matcher(line).replaceFirst("$1 ")).toList();
        List<String> logs = Stream.concat(consoleLogs.stream(), debugLogs.stream()).toList();
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
        CommandLine toolFetch = ToolFetch.commandLine();
        StringWriter log = new StringWriter();
        toolFetch.setOut(new PrintWriter(log));
        toolFetch.setErr(new PrintWriter(log));
        TestLogListAppenderSteps testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
        int exitCode = toolFetch.execute(args);
        List<String> writerLogs = log.toString().lines().toList();
        List<String> collectedLogs = testLogListAppenderSteps.logs();
        List<String> logs = Stream.concat(writerLogs.stream(), collectedLogs.stream()).toList();
        return new ExecResult(exitCode, logs);
    }

    void assertAnyMatch(ExecResult execResult, String message) {
        assertThat(execResult.logs()).anyMatch(line -> line.contains(message));
    }

    void assertOneMatch(ExecResult execResult, String message) {
        assertThat(execResult.logs()).filteredOn(line -> line.contains(message)).hasSize(1);
    }

    void assertNoMatch(ExecResult execResult, String message) {
        assertThat(execResult.logs()).noneMatch(line -> line.contains(message));
    }

    void assertNoErrorNoWarn(ExecResult execResult) {
        assertThat(execResult.logs()).noneMatch(
                line -> line.contains("Exception in thread") || line.contains("[%s]".formatted(LogLevel.ERROR)) || line.contains(
                        "[%s]".formatted(LogLevel.WARN)));
    }

    void assertNoError(ExecResult execResult) {
        assertThat(execResult.logs()).noneMatch(
                line -> line.contains("Exception in thread") || line.contains("[%s]".formatted(LogLevel.ERROR)));
    }

    void assertLogbackInit(ExecResult execResult) {
        assertThat(execResult.logs()).noneMatch(line -> line.contains("logback"));
    }

    String debug(String label) {
        return debug(label, null);
    }

    String debug(String label, @Nullable Object value) {
        return "[%s] %s%s".formatted(LogLevel.DEBUG, StringUtils.rightPad(label + ":", 19),
                value == null ? StringUtils.EMPTY : value);
    }

    record ExecResult(int exitCode, List<String> logs) {}

}

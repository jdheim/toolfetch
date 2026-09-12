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
import java.util.stream.Stream;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class ToolFetchTestBase {

    static final boolean IS_DEBUG_NATIVE_MODE_ENABLED = Boolean.getBoolean("native-debug");

    static final boolean IS_FORCE_NATIVE_MODE_ENABLED = Boolean.getBoolean("force-native");

    static final boolean IS_QUIET_MODE_ENABLED = Boolean.getBoolean("quiet");

    static final Path NATIVE_IMAGE_EXEC = Path.of(System.getProperty("maven.multiModuleProjectDirectory", "../../"), "target",
            "toolfetch");

    static final Path NATIVE_IMAGE_METADATA = Path.of("../../target", "toolfetch.metadata");

    static final Path LIBSVMJDWP_LIBRARY = Path.of("../../target", "libsvmjdwp.so");

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
    @SuppressFBWarnings(value = "COMMAND_INJECTION",
            justification = "Test-only code. Arguments are constructed internally for controlled Smoke Tests of Native Image and are not influenced by external or user input")
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
        CommandLine toolFetch = ToolFetch.commandLine();
        StringWriter log = new StringWriter();
        toolFetch.setOut(new PrintWriter(log));
        toolFetch.setErr(new PrintWriter(log));
        TestLogListAppenderSteps testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
        int exitCode = toolFetch.execute(args);
        List<String> writerLogs = log.toString().lines().toList();
        List<String> collectedLogs = formatLogs(testLogListAppenderSteps.list);
        List<String> logs = Stream.concat(writerLogs.stream(), collectedLogs.stream()).toList();
        return new ExecResult(exitCode, logs);
    }

    private List<String> formatLogs(List<ILoggingEvent> events) {
        PatternLayout layout = new PatternLayout();
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            layout.setContext(context);
            layout.setPattern("%toolFetchPlainLogLevel %toolFetchPlainLogMessage");
            layout.start();
            return events.stream().map(layout::doLayout).toList();
        } finally {
            layout.stop();
        }
    }

    void assertAnyMatch(ExecResult execResult, String message) {
        assertThat(execResult.logs()).anyMatch(line -> line.contains(message));
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

    record ExecResult(int exitCode, List<String> logs) {}

}

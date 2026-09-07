/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import com.jdheim.toolfetch.command.info.ToolFetchVersionInfoProvider;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import com.jdheim.toolfetch.service.info.JavaHome;
import com.jdheim.toolfetch.service.info.OsRelease;
import com.jdheim.toolfetch.service.util.OperatingSystemPredicates;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

public class DebugStrategy implements CommandLine.IExecutionStrategy {

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(DebugStrategy.class);

    private static final int LABEL_WIDTH = 19;

    private static final String GRAALVM_NATIVE_IMAGE_PROPERTY = "org.graalvm.nativeimage.imagecode";

    private static final String EXECUTABLE_MODE_NATIVE_IMAGE = "Native Image";

    private static final String EXECUTABLE_MODE_JVM = "JVM";

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException,
            CommandLine.ParameterException {
        ToolFetchVersionInfoProvider versionInfoProvider = new ToolFetchVersionInfoProvider();
        Arrays.stream(versionInfoProvider.getVersion()).forEach(line -> LOGGER.log("debug", line));
        debug("Executable", resolveExecutablePath());
        debug("Runtime Mode", runtimeMode());
        debug("Java Home", JavaHome.get());
        debug("User Home", SystemUtils.USER_HOME);
        debug("Working Directory", SystemUtils.USER_DIR);
        debug("Operating System", SystemUtils.OS_NAME);
        debug("OS Version", SystemUtils.OS_VERSION);
        debug("OS Architecture", SystemUtils.OS_ARCH);
        if (OperatingSystemPredicates.isLinux()) {
            Map<String, String> osRelease = OsRelease.get();
            debug("Distro", osRelease.get(OsRelease.NAME));
            debug("Distro Version", osRelease.get(OsRelease.VERSION_ID));
        }
        return CommandLine.ExitCode.OK;
    }

    private void debug(String label, @Nullable Object value) {
        if (value != null) {
            LOGGER.log("debug", StringUtils.rightPad(label + ":", LABEL_WIDTH) + value);
        }
    }

    private @Nullable Path resolveExecutablePath() {
        return ProcessHandle.current().info().command().map(this::resolveExecutablePath).orElse(null);
    }

    private Path resolveExecutablePath(String command) {
        Path path = Path.of(command);
        try {
            return path.toRealPath();
        } catch (IOException _) {
            return path.toAbsolutePath().normalize();
        }
    }

    private String runtimeMode() {
        return System.getProperty(GRAALVM_NATIVE_IMAGE_PROPERTY) != null ? EXECUTABLE_MODE_NATIVE_IMAGE : EXECUTABLE_MODE_JVM;
    }

}

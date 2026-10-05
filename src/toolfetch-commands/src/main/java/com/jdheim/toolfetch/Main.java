/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.command.execution.DebugStrategy;
import org.apache.commons.lang3.ArrayUtils;
import picocli.CommandLine;

public final class Main {

    private static final Object[] HELP_VERSION_OPTIONS = {"-h", "--help", "-v", "--version"};

    private Main() {
        throw new AssertionError();
    }

    static void main(String[] args) {
        System.exit(execute(System.nanoTime(), args));
    }

    private static int execute(long startTime, String[] args) {
        boolean containsHelpOrVersionArgs = ArrayUtils.containsAny(args, HELP_VERSION_OPTIONS);
        int exitCode = ToolFetch.execute(args);
        if (!containsHelpOrVersionArgs && exitCode != CommandLine.ExitCode.USAGE) {
            DebugStrategy.logCompletion(startTime, exitCode);
        }
        return exitCode;
    }

}

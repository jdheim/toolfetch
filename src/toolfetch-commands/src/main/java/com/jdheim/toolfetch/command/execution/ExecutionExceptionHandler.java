/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import com.jdheim.toolfetch.logging.ToolFetchLogger;
import picocli.CommandLine;

public class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception exception, CommandLine commandLine, CommandLine.ParseResult parseResult) {
        ToolFetchLogger.getLogger(ExecutionExceptionHandler.class).log(exception, "command.exception");
        return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }

}

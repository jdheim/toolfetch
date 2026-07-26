/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.command.execution.ValidatingExecutionStrategy;
import picocli.CommandLine;

public final class Main {

    private Main() {
        throw new AssertionError();
    }

    static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    static CommandLine commandLine() {
        return new CommandLine(new ToolFetch()).setExecutionStrategy(new ValidatingExecutionStrategy());
    }

}

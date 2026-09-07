/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import java.util.List;
import picocli.CommandLine;

public class CompositeStrategy implements CommandLine.IExecutionStrategy {

    private final List<CommandLine.IExecutionStrategy> executionStrategies;

    public CompositeStrategy(CommandLine.IExecutionStrategy... strategies) {
        executionStrategies = List.of(strategies);
    }

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException,
            CommandLine.ParameterException {
        int exitCode = CommandLine.ExitCode.OK;
        for (CommandLine.IExecutionStrategy executionStrategy : executionStrategies) {
            exitCode = executionStrategy.execute(parseResult);
            if (exitCode != CommandLine.ExitCode.OK) {
                break;
            }
        }
        return exitCode;
    }

}

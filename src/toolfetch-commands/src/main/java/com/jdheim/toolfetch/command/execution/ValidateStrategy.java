/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import com.jdheim.toolfetch.command.execution.option.ConfigPathValidationRule;
import com.jdheim.toolfetch.command.subcommand.Install;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import picocli.CommandLine;

public class ValidateStrategy implements CommandLine.IExecutionStrategy {

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException,
            CommandLine.ParameterException {
        if (!parseResult.hasSubcommand()) {
            throw new CommandLine.ParameterException(parseResult.commandSpec().commandLine(),
                    ToolFetchLogger.message("exception.command.missing-subcommand"));
        }
        validate(parseResult.subcommands().getLast());
        return CommandLine.ExitCode.OK;
    }

    private void validate(CommandLine.ParseResult parseResult) {
        CommandLine.Model.CommandSpec commandSpec = parseResult.commandSpec();
        CommandLine commandLine = commandSpec.commandLine();

        Object userObject = commandSpec.userObject();
        if (userObject instanceof Install install) {
            ConfigPathValidationRule.validateAll(commandLine, install.configPath());
        } else {
            throw new CommandLine.ParameterException(commandLine,
                    ToolFetchLogger.message("exception.command.unsupported", userObject.getClass().getSimpleName()));
        }
    }

}

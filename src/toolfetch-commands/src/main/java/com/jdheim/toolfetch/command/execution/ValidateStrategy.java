/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.execution;

import com.jdheim.toolfetch.command.execution.option.ConfigPathValidationRule;
import com.jdheim.toolfetch.command.subcommand.Install;
import picocli.CommandLine;

public class ValidateStrategy implements CommandLine.IExecutionStrategy {

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException,
            CommandLine.ParameterException {
        if (!parseResult.hasSubcommand()) {
            throw new CommandLine.ParameterException(parseResult.commandSpec().commandLine(), "Missing required subcommand");
        }
        validate(parseResult.subcommands().getLast());
        return CommandLine.ExitCode.OK;
    }

    private void validate(CommandLine.ParseResult parseResult) {
        CommandLine.Model.CommandSpec commandSpec = parseResult.commandSpec();
        CommandLine commandLine = commandSpec.commandLine();

        Object userObject = commandSpec.userObject();
        if (userObject instanceof Install install) {
            ConfigPathValidationRule.validateAll(commandLine, install.getConfigPath());
        } else {
            throw new CommandLine.ParameterException(commandLine,
                    "Command %s not supported".formatted(userObject.getClass().getSimpleName()));
        }
    }

}

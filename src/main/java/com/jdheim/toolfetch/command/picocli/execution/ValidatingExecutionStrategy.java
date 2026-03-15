/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.picocli.execution;

import java.util.Optional;
import com.jdheim.toolfetch.command.ToolFetch;
import com.jdheim.toolfetch.command.picocli.execution.option.ConfigPathValidationRule;
import picocli.CommandLine;

public class ValidatingExecutionStrategy implements CommandLine.IExecutionStrategy {

    @Override
    public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException,
            CommandLine.ParameterException {
        validate(parseResult);
        return new CommandLine.RunLast().execute(parseResult);
    }

    private void validate(CommandLine.ParseResult parseResult) {
        CommandLine.Model.CommandSpec commandSpec = parseResult.commandSpec();
        CommandLine commandLine = commandSpec.commandLine();

        Object userObject = commandSpec.userObject();
        if (userObject instanceof ToolFetch toolFetch) {
            Optional.of(toolFetch)
                    .map(ToolFetch::getConfigPath)
                    .ifPresent(configPath -> ConfigPathValidationRule.validateAll(commandLine, configPath));
        } else {
            throw new CommandLine.ParameterException(commandLine,
                    "Command %s not supported".formatted(userObject.getClass().getSimpleName()));
        }
    }

}

/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

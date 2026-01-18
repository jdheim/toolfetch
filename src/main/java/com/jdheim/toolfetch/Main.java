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

package com.jdheim.toolfetch;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import com.jdheim.toolfetch.version.VersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "toolfetch", versionProvider = VersionProvider.class, mixinStandardHelpOptions = true, description = "CLI for fetching and installing external tools from release URLs (e.g. GitHub releases) using a YAML configuration file")
public class Main implements Callable<Integer> {

    @CommandLine.Option(names = {"-c", "--config"}, required = true, description = "Path to toolfetch.yaml")
    private Path configPath;

    static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() {
        return 0;
    }

}

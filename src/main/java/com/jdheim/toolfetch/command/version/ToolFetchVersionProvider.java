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

package com.jdheim.toolfetch.command.version;

import static com.jdheim.toolfetch.command.version.ToolFetchInfo.BUILD_JVM;
import static com.jdheim.toolfetch.command.version.ToolFetchInfo.BUILD_REVISION;
import static com.jdheim.toolfetch.command.version.ToolFetchInfo.BUILD_TIME;
import static com.jdheim.toolfetch.command.version.ToolFetchInfo.TITLE;
import static com.jdheim.toolfetch.command.version.ToolFetchInfo.VERSION;

import picocli.CommandLine;

/// Provides version information for a command
public class ToolFetchVersionProvider implements CommandLine.IVersionProvider {

    private static final String VERSION_SEPARATOR = "---------------------------------------------------------------";

    @Override
    public String[] getVersion() {
        String titleWithVersion = "%s %s by JDHeim.com".formatted(TITLE.value(), VERSION.value());
        String buildTime = "Build Time:\t" + BUILD_TIME.value();
        String revision = "Revision:\t" + BUILD_REVISION.value();
        String jvm = "JVM:\t\t" + BUILD_JVM.value();

        return new String[]{
                VERSION_SEPARATOR, titleWithVersion, VERSION_SEPARATOR, buildTime, revision, jvm, VERSION_SEPARATOR
        };
    }

}

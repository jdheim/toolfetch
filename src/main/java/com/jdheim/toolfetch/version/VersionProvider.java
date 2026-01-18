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

package com.jdheim.toolfetch.version;

import static com.jdheim.toolfetch.version.VersionInfo.BUILD_JVM;
import static com.jdheim.toolfetch.version.VersionInfo.BUILD_REVISION;
import static com.jdheim.toolfetch.version.VersionInfo.BUILD_TIME;
import static com.jdheim.toolfetch.version.VersionInfo.TITLE;
import static com.jdheim.toolfetch.version.VersionInfo.VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import picocli.CommandLine;

/**
 * Provides version information for a command
 */
public class VersionProvider implements CommandLine.IVersionProvider {

    private static final String VERSION_INFO_FILE_PATH = "/version-info.properties";

    private static final String SEPARATOR = "-------------------------------------------------------------";

    @Override
    public String[] getVersion() {
        Properties props = loadProps();
        return buildVersion(props);
    }

    private Properties loadProps() {
        Properties props = new Properties();
        try (InputStream in = VersionProvider.class.getResourceAsStream(VERSION_INFO_FILE_PATH)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            return props;
        }
        return props;
    }

    private String[] buildVersion(Properties props) {
        return Optional.ofNullable(props).filter(p -> !p.isEmpty()).map(p -> {
            String titleWithVersion = "%s %s by JDHeim.com".formatted(TITLE.getProperty(p), VERSION.getProperty(p));
            String buildTime = "Build Time:\t" + BUILD_TIME.getProperty(p);
            String revision = "Revision:\t" + BUILD_REVISION.getProperty(p);
            String jvm = "JVM:\t\t" + BUILD_JVM.getProperty(p);

            return new String[]{SEPARATOR, titleWithVersion, SEPARATOR, buildTime, revision, jvm, SEPARATOR};
        }).orElseGet(() -> new String[]{"Version Information is not available"});
    }

}

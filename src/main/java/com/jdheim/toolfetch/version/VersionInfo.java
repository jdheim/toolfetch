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

import java.util.Optional;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

enum VersionInfo {

    TITLE("toolfetch.title", "ToolFetch"),
    VERSION("toolfetch.version", "dev"),
    BUILD_TIME("toolfetch.build.time"),
    BUILD_REVISION("toolfetch.build.revision", "dev"),
    BUILD_JVM("toolfetch.build.jvm");

    private static final String VARIABLE_PREFIX = "${";

    private final String propertyKey;

    private final String defaultValue;

    VersionInfo(String propertyKey) {
        this.propertyKey = propertyKey;
        this.defaultValue = StringUtils.EMPTY;
    }

    VersionInfo(String propertyKey, String defaultValue) {
        this.propertyKey = propertyKey;
        this.defaultValue = defaultValue;
    }

    String getProperty(Properties props) {
        return Optional.ofNullable(props)
                .map(p -> p.getProperty(propertyKey))
                .filter(value -> StringUtils.isNotBlank(value) && !value.startsWith(VARIABLE_PREFIX))
                .orElse(defaultValue);
    }

}

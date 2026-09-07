/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.info;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import com.jdheim.toolfetch.logging.ToolFetchLogger;
import org.apache.commons.lang3.StringUtils;

public enum ToolFetchInfo {

    TITLE("toolfetch.title", "ToolFetch"),
    VERSION("toolfetch.version", "X.X.X-DEV"),
    BUILD_TIME("toolfetch.build.time", "-"),
    BUILD_REVISION("toolfetch.build.revision", "dev"),
    BUILD_GRAALVM("toolfetch.build.graalvm", "-");

    private static final ToolFetchLogger LOGGER = ToolFetchLogger.getLogger(ToolFetchInfo.class);

    private static final String VERSION_INFO_FILE_PATH = "/version-info.properties";

    private static final String VARIABLE_PREFIX = "${";

    private final String propertyKey;

    private final String defaultValue;

    ToolFetchInfo(String propertyKey, String defaultValue) {
        this.propertyKey = propertyKey;
        this.defaultValue = defaultValue;
    }

    public String value() {
        String propertyValue = LazyHolder.versionInfoProps.getProperty(propertyKey);
        if (StringUtils.isBlank(propertyValue) || propertyValue.startsWith(VARIABLE_PREFIX)) {
            return defaultValue;
        }
        return propertyValue;
    }

    static final class LazyHolder {

        private static Properties versionInfoProps = load(VERSION_INFO_FILE_PATH);

        private LazyHolder() {
            throw new AssertionError();
        }

        static Properties load(String versionInfoFilePath) {
            Properties props = getProperties();
            try (InputStream in = ToolFetchInfo.class.getResourceAsStream(versionInfoFilePath)) {
                if (in != null) {
                    props.load(in);
                } else {
                    LOGGER.log("info.resource-not-found", versionInfoFilePath);
                }
            } catch (IOException e) {
                LOGGER.log("info.resource-load-failed", versionInfoFilePath, StringUtils.trimToEmpty(e.getMessage()));
            }
            return props;
        }

        static Properties getProperties() {
            return new Properties();
        }

        /// Never call this. Test hook!
        static void reloadTestHook(String versionInfoFilePath) {
            versionInfoProps = load(versionInfoFilePath);
        }

    }

}

/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.command.info;

import static com.jdheim.toolfetch.service.info.ToolFetchInfo.BUILD_GRAALVM;
import static com.jdheim.toolfetch.service.info.ToolFetchInfo.BUILD_REVISION;
import static com.jdheim.toolfetch.service.info.ToolFetchInfo.BUILD_TIME;
import static com.jdheim.toolfetch.service.info.ToolFetchInfo.TITLE;
import static com.jdheim.toolfetch.service.info.ToolFetchInfo.VERSION;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

/// Provides version information for a command
public class ToolFetchVersionInfoProvider implements CommandLine.IVersionProvider {

    private static final String SEPARATOR_CHAR = "-";

    private static final int DEFAULT_SEPARATOR_COUNT = 70;

    public static String separator(String... lines) {
        int separatorCharCount = Arrays.stream(lines)
                .filter(StringUtils::isNotBlank)
                .mapToInt(String::length)
                .max()
                .orElse(DEFAULT_SEPARATOR_COUNT);
        return SEPARATOR_CHAR.repeat(separatorCharCount);
    }

    @Override
    public String[] getVersion() {
        String titleWithVersion = "%s %s by JDHeim.com".formatted(TITLE.value(), VERSION.value());
        String buildTime = "Build Time:     " + BUILD_TIME.value();
        String revision = "Build Revision: " + BUILD_REVISION.value();
        String jvm = "Build GraalVM:  " + BUILD_GRAALVM.value();

        String separator = separator(titleWithVersion, buildTime, revision, jvm);

        return new String[]{
                separator, titleWithVersion, separator, buildTime, revision, jvm, separator
        };
    }

}

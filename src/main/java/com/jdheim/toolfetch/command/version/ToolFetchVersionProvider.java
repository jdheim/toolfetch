/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
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

    private static final String VERSION_SEPARATOR = "--------------------------------------------------------------------";

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

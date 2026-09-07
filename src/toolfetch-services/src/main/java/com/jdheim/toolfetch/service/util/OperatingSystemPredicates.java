/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.util;

import org.apache.commons.lang3.SystemUtils;

/// Predicate methods for detecting the current operating system.
public final class OperatingSystemPredicates {

    private OperatingSystemPredicates() {
        throw new AssertionError();
    }

    public static boolean isLinux() {
        return SystemUtils.IS_OS_LINUX;
    }

    public static boolean isMacOs() {
        return SystemUtils.IS_OS_MAC;
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

}

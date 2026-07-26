/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.log;

public final class LogHelper {

    private static final double TO_SECONDS = 1_000_000_000.0;

    LogHelper() {
        throw new AssertionError();
    }

    public static String elapsedTime(long startTime) {
        return String.format("%.2f", (System.nanoTime() - startTime) / TO_SECONDS);
    }

}

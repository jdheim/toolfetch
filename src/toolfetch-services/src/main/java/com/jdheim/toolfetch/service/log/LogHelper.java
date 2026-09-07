/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.log;

import java.util.Locale;

public final class LogHelper {

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private LogHelper() {
        throw new AssertionError();
    }

    public static String elapsedTime(long startTime) {
        double seconds = (System.nanoTime() - startTime) / NANOS_PER_SECOND;
        return String.format(Locale.ROOT, "%.2f", seconds);
    }

}
